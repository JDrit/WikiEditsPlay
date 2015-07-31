package controllers

import java.sql.Timestamp
import javax.inject.Inject

import play.Logger

import scala.concurrent.Future

import models._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.Jsonp
import play.api.libs.functional.syntax._
import play.api.mvc.{Action, Controller}
import play.api.libs.json._
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._
import play.api.libs.concurrent.Execution.Implicits._

case class Log(channel: String, comment: String, diff: String, page: String, timestamp: Long, username: String)

class ApiController @Inject()(dbConfigProvider: DatabaseConfigProvider) extends Controller {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  implicit val writer1 = new Writes[(Timestamp, Long)] {
    def writes(t: (Timestamp, Long)): JsValue = Json.arr(t._1.getTime, t._2)
  }
  implicit val writer2 = new Writes[(String, Long)] {
    def writes(p: (String, Long)): JsValue = Json.arr(p._1, p._2)
  }
  implicit val writer3 = new Writes[(String, Int)] {
    def writes(p: (String, Int)): JsValue = Json.arr(p._1, p._2)
  }

  implicit val logReads: Reads[Log] = (
    (JsPath \ "channel").read[String] and
      (JsPath \ "comment").read[String] and
      (JsPath \ "diff").read[String] and
      (JsPath \ "page").read[String] and
      (JsPath \ "timestamp").read[Long] and
      (JsPath \ "username").read[String]
    )(Log.apply _)


  def channelEdits(subDomain: String) = Action.async { request =>
    dbConfig.db.run(ChannelEdits.allTimestamps(subDomain).result).map { seq =>
      request.getQueryString("callback") match {
        case Some(callback) => Ok(Jsonp(callback, Json.toJson(seq)))
        case None => Ok(Json.toJson(seq))
      }
    }
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

  def topPages(subDomain: String) = Action.async {
    dbConfig.db.run(PageEdits.currentTopPages(subDomain).result).map(seq => Ok(Json.toJson(seq)))
  }

  def topUsers(subDomain: String) = Action.async {
    dbConfig.db.run(UserEdits.totalMostActiveUsers(subDomain).result).map(seq => Ok(Json.toJson(seq)))
  }



  /** --------------- UPDATE DATABASE API ----------------------------------------------- */
  def addLog() = Action.async { request =>
    dbConfig.db.run {
      (request.body.asJson match {
        case None =>
          Logger.error("payload body is not json")
          DBIO.seq()
        case Some(json) =>
          json.validate[List[Log]] match {
            case logs: JsSuccess[List[Log]] =>
              Logger.info("inserting logs")
              Edit.insert(logs.get)
            case e: JsError =>
              Logger.error("could not parse JSON")
              DBIO.seq()
          }
      }).map(q => Ok)
    }
  }

  def addPageEdits() = Action { Ok("") }

  def addTopUsers() = Action { Ok("") }

  def addTopPages() = Action { Ok("") }

  def addVandalism() = Action { Ok("") }

  def addAnomalies() = Action { Ok("") }
}