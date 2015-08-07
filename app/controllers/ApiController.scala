package controllers

import javax.inject.Inject
import java.sql.Timestamp

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import play.api.db.slick.DatabaseConfigProvider
import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.libs.Jsonp
import play.api.libs.ws._
import play.api.mvc.{Action, Controller}
import play.api.Play.current
import play.Logger
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._

import JSONConverters._
import models._

class ApiController @Inject()(dbConfigProvider: DatabaseConfigProvider) extends Controller {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  final val MINUTE = 60000L

  def insertGaps(data: List[(Timestamp, Long)], current: Long): List[(Timestamp, Long)] = data match {
    case Nil => Nil
    case l @ (time, count) :: xs => if (time.getTime() - current <= MINUTE) {
      (time, count) :: insertGaps(xs, time.getTime() + MINUTE)
    } else {
      (new Timestamp(current), 0L) :: insertGaps(l, current + MINUTE)
    }
  }

  /** Generates the graph data for the given subdomain */
  def channelEdits(subDomain: String) = Action.async { request =>
    Logger.info(s"Getting the domain edits for $subDomain")
    dbConfig.db.run(Edit.domainEdits(subDomain)).map { seq => 
      val format = insertGaps(seq.toList, seq.head._1.getTime)
      Ok(Json.toJson(format))
    }
  }

  def getIpCity(ip: String): Future[(String, String)] = Cache.getOrElse(s"ip-info-$ip", 60 * 60) {
    WS.url(s"http://ip-api.com/json/$ip").withRequestTimeout(1000).get().map { response =>
      Logger.info(s"Getting infomation for $ip")
      (ip, (response.json \ "city").as[String] + " " + (response.json \ "country").as[String])
    }
  }

  def ipAddrs(domain: String) = Action.async { 
    dbConfig.db.run(Edit.mostCommonIpsForDomain(domain)).map { seq =>
      val ipInfo = Future.sequence(seq.map { case (ip, count) => getIpCity(ip) })
      val ipResult = Await.result(ipInfo, 1 second).toList 
      val output = seq.zip(ipResult).map { case ((ip, count), (ip2, city)) => 
        (ip, city, count) 
      }
      Ok(Json.toJson(output))
     }
  }

  /** generates the data for the graph on the front page */
  def totalEdits() = Action.async {
    Logger.info(s"Getting the total page edits")
    dbConfig.db.run(Edit.allEdits).map { seq => 
      val format = insertGaps(seq.toList, seq.head._1.getTime)
      Ok(Json.toJson(format)) 
    }
  }

  /** Gets the most current channel edits / hr */
  def channelEditsUpdate(subDomain: String) = Action.async { request =>
    dbConfig.db.run(ChannelEdits.mostCurrent(subDomain).result).map { seq => 
      Ok(Json.toJson(seq.toList)) 
    }
  }

  /** Gets all the edits for a given page in a subdomain */
  def editsForPage(subDomain: String, page: String) = Action.async {
    Logger.info(s"Geting page views for $subDomain : $page")
    dbConfig.db.run(Edit.editsForPage(subDomain, page)).map { seq =>
      val format = insertGaps(seq.toList, seq.head._1.getTime)
      Ok(Json.toJson(format))
    }
  }

  /** Gets all the edits that a user has performed in a given subdomain */
  def editsForUser(subDomain: String, username: String) = Action.async {
    dbConfig.db.run(Edit.editsForUser(subDomain, username).result).map { seq =>
      BadRequest
      //Ok(Json.toJson(seq.toList))
     }
  }
 
  /** Lists all of the domains that are being cataloged */
  def listDomains() = Cache.getOrElse("list-domains", 60 * 60) {
    Action.async {
      dbConfig.db.run(Edit.listOfChannels).map(seq => Ok(Json.toJson(seq)))
    }
  }

  def topDomains() = Cache.getOrElse("top-domains", 60 * 60) {
    Action.async { request =>
      dbConfig.db.run(Edit.topDomains).map(seq => Ok(Json.toJson(seq)))
     }
  }

  /** Redirects the search box to the correct subdomain */
  def searchDomain() = Action { request =>
    request.getQueryString("domain") match {
      case Some(domain) => Redirect(s"/#/domain/$domain")
      case None => Redirect("/")
    }
  }

  /** Gets the most active pages for a given subdomain in the time interval */
  def topPagesRange(subDomain: String) = Action.async { request =>
    val start = request.getQueryString("start") match {
      case Some(num) => new Timestamp(num.toDouble.toLong)
      case None => new Timestamp(0)
    }
    val end = request.getQueryString("end") match {
      case Some(num) => new Timestamp(num.toDouble.toLong)
      case None => new Timestamp(System.currentTimeMillis())
    }
    Cache.getOrElse(s"top-pages-$subDomain-$start-$end", 60 * 60) {
      dbConfig.db.run(Edit.topPages(subDomain, start, end).result).map { seq => 
        Ok(Json.toJson(seq))
      }
    }
  }

  /** Gets the list of the current most active pages for a subdomain */
  def topPages(subDomain: String) = Action.async {
    dbConfig.db.run(PageEdits.currentTopPages(subDomain).result).map { seq => 
      Ok(Json.toJson(seq))
    }
  }

  /** Gets the most active users in the given time range */
  def topUsersRange(subDomain: String) = Action.async { request =>
    val start = request.getQueryString("start") match {
      case Some(num) => new Timestamp(num.toDouble.toLong)
      case None => new Timestamp(0)
    }
    val end = request.getQueryString("end") match {
      case Some(num) => new Timestamp(num.toDouble.toLong)
      case None => new Timestamp(System.currentTimeMillis())
    }
    Cache.getOrElse(s"top-users-$subDomain-$start-$end", 60 * 60) {
      dbConfig.db.run(Edit.topUsers(subDomain, start, end).result).map { seq => 
        Ok(Json.toJson(seq))
      }
    }
  }

  /** Gets the current most active users for a channel */
  def topUsers(subDomain: String) = Action.async {
    dbConfig.db.run(UserEdits.currentTopUsers(subDomain).result).map { seq => 
      Ok(Json.toJson(seq))
    }
  }

  /** ----------------------------------------------------------------------------------- */
  /** ---------------------- UPDATE DATABASE API ---------------------------------------- */
  /** ----------------------------------------------------------------------------------- */

  def addLog() = Action.async { request =>
    request.body.asJson
      .map(json => json.validate[List[Log]])
      .map {
        case JsSuccess(edits, path) => dbConfig.db.run(Edit.insert(edits)).map(q => Ok)
        case _ =>
          Logger.error("could not parse JSON for logs")
          Future { BadRequest }
      }.get
  }

  def addPageEdits() = Action.async { request =>
    request.body.asJson
      .map(json => json.validate[ChannelsPayload])
      .map {
        case JsSuccess(edits, path) => dbConfig.db.run(ChannelEdits.insert(edits)).map(q => Ok)
        case _ =>
          Logger.error("could not parse JSON for channel edits")
          Future { BadRequest }
      }.get
  }

  def addTopUsers() = Action.async { request =>
    request.body.asJson
      .map(json => json.validate[TopUserContainer])
      .map {
        case JsSuccess(users, path) => dbConfig.db.run(UserEdits.insert(users)).map(q => Ok)
        case _ =>
          Logger.error("could not parse JSON for top users")
          Future { BadRequest }
      }.get
  }

  def addTopPages() = Action.async { request =>
    request.body.asJson
      .map(json => json.validate[TopPageContainer])
      .map {
        case JsSuccess(pages, path) => dbConfig.db.run(PageEdits.insert(pages)).map(q => Ok)
        case _ =>
          Logger.error("could not parse JSON for top pages")
          Future { BadRequest }
      }.get
  }

  def addVandalism() = Action.async { request =>
    request.body.asJson
      .map(json => json.validate[List[Log]])
      .map {
        case JsSuccess(logs, path) => dbConfig.db.run(Vandalism.insert(logs)).map(q => Ok)
        case _ =>
          Logger.error("could not parse JSON for vandalism")
          Future { BadRequest }
      }.get
  }

  def addAnomalies() = Action.async { request =>
    request.body.asJson
      .map(json => json.validate[List[AnomalyCase]])
      .map {
        case JsSuccess(anomalies, path) => dbConfig.db.run(Anomaly.insert(anomalies)).map(q => Ok)
        case _ =>
          Logger.error("could not parse JSON for anomalies")
          Future { BadRequest }
      }.get
  }

}
