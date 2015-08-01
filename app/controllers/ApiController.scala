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

case class Log(channel: String, comment: String, diff: String, 
  page: String, timestamp: Long, username: String)
case class ChannelCount(channel: String, count: Long)
case class ChannelsPayload(timestamp: Long, channels: List[ChannelCount])

case class TopPage(channel: String, page: String, count: Long)
case class TopPageContainer(timestamp: Long, pages: List[TopPage])

case class TopUser(channel: String, username: String, count: Long)
case class TopUserContainer(timestamp: Long, users: List[TopUser])

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

  implicit val channelCountReads: Reads[ChannelCount] = (
    (JsPath \ "channel").read[String] and
      (JsPath \ "count").read[Long]
    )(ChannelCount.apply _)

  implicit val channelsPayloadReads: Reads[ChannelsPayload] = (
    (JsPath \ "timestamp").read[Long] and
      (JsPath \ "page_edits").read[List[ChannelCount]]
    )(ChannelsPayload.apply _)

  implicit val topPageContainerReads: Reads[TopPageContainer] = (
    (JsPath \ "timestamp").read[Long] and
      (JsPath \ "top_pages").read[List[TopPage]]
    )(TopPageContainer.apply _)

  implicit val topPageReads: Reads[TopPage] = (
    (JsPath \ "channel").read[String] and
      (JsPath \ "page").read[String] and
      (JsPath \ "count").read[Long]
    )(TopPage.apply _)

  implicit val topUserContainerReads: Reads[TopUserContainer] = (
    (JsPath \ "timestamp").read[Long] and
      (JsPath \ "top_users").read[List[TopUser]]
    )(TopUserContainer.apply _)

  implicit val topUserReads: Reads[TopUser] = (
    (JsPath \ "channel").read[String] and
      (JsPath \ "username").read[String] and
      (JsPath \ "count").read[Long]
    )(TopUser.apply _)


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
    request.body.asJson
      .map(json => json.validate[List[Log]])
      .map(edits => Edit.insert(edits.get))
      .map(action => dbConfig.db.run(action).map(q => Ok))
      .getOrElse(Future { BadRequest })
  }

  def addPageEdits() = Action.async { request =>
    request.body.asJson
      .map(json => json.validate[ChannelsPayload])
      .map(edits => ChannelEdits.insert(edits.get))
      .map(action => dbConfig.db.run(action).map(q => Ok))
      .getOrElse(Future { BadRequest })
  }

  def addTopUsers() = Action.async { request =>
    request.body.asJson
      .map(json => json.validate[TopUserContainer])
      .map(users => UserEdits.insert(users.get))
      .map(action => dbConfig.db.run(action).map(q => Ok))
      .getOrElse(Future { BadRequest })
  }

  def addTopPages() = Action.async { request =>
    request.body.asJson
      .map(json => json.validate[TopPageContainer])
      .map(pages => PageEdits.insert(pages.get))
      .map(action => dbConfig.db.run(action).map(q => Ok))
      .getOrElse(Future { BadRequest })
  }

  def addVandalism() = Action.async { request =>
    request.body.asJson
      .map(json => json.validate[Log])
      .map(logs => Vandalism.insert(logs))
      .map(action => dbConfig.db.run(action).map(q => Ok))
      .getOrElse(Future { BadRequest })
  }

  def addAnomalies() = Action.async { request =>
    request.body.asJson
      .map(json => json.validate[Log])
      .map(logs => Anomaly.insert(logs))
      .map(action => dbConfig.db.run(action).map(q => Ok))
      .getOrElse(Future { BadRequest })  }
}
