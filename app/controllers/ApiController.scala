package controllers


import java.sql.Timestamp
import javax.inject.Inject

import play.Logger


import scala.concurrent.Future

import JSONConverters._
import models._

import play.api.db.slick.DatabaseConfigProvider
import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.Jsonp
import play.api.mvc.{Action, Controller}
import play.api.libs.json._
import play.api.Play.current
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._

class ApiController @Inject()(dbConfigProvider: DatabaseConfigProvider) extends Controller {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  /** Generates the graph data for the given subdomain */
  def channelEdits(subDomain: String) = Action.async { request =>
    dbConfig.db.run(ChannelEdits.allTimestamps(subDomain).result).map { seq => Ok(Json.toJson(seq)) }
  }

  /** generates the data for the graph on the front page */
  def totalEdits() = Action.async {
    dbConfig.db.run(Edit.allEdits).map { seq => Ok(Json.toJson(seq.toList)) }
  }

  /** Gets the most current channel edits / hr */
  def channelEditsUpdate(subDomain: String) = Action.async { request =>
    dbConfig.db.run(ChannelEdits.mostCurrent(subDomain).result).map { seq =>
      if (seq.isEmpty) BadRequest
      else request.getQueryString("callback") match {
        case Some(callback) => Ok(Jsonp(callback, Json.toJson(seq.head)))
        case None => Ok(Json.toJson(seq.head))
      }
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
      dbConfig.db.run(Edit.topPages(subDomain, start, end).result).map(seq => Ok(Json.toJson(seq)))
    }
  }

  /** Gets the list of the current most active pages for a subdomain */
  def topPages(subDomain: String) = Action.async {
    dbConfig.db.run(PageEdits.currentTopPages(subDomain).result).map(seq => Ok(Json.toJson(seq)))
  }

  /** Lists all of the domains that are being cataloged */
  def listDomains() = Cache.getOrElse("list-domains", 60 * 60) {
    Action.async {
      dbConfig.db.run(Edit.listOfChannels).map(seq => Ok(Json.toJson(seq)))
    }
  }

  /** Redirects the search box to the correct subdomain */
  def searchDomain() = Action { request =>
    request.getQueryString("domain") match {
      case Some(domain) => Redirect(s"/#/domain/$domain")
      case None => Redirect("/")
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
      dbConfig.db.run(Edit.topUsers(subDomain, start, end).result).map(seq => Ok(Json.toJson(seq)))
    }
  }

  /** Gets the current most active users for a channel */
  def topUsers(subDomain: String) = Action.async {
    dbConfig.db.run(UserEdits.currentTopUsers(subDomain).result).map(seq => Ok(Json.toJson(seq)))
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
