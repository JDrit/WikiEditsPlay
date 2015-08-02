package controllers


import javax.inject.Inject

import play.Logger

import scala.concurrent.Future

import JSONConverters._
import models._

import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.Jsonp
import play.api.mvc.{Action, Controller}
import play.api.libs.json._
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._

class ApiController @Inject()(dbConfigProvider: DatabaseConfigProvider) extends Controller {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  def channelEdits(subDomain: String) = Action.async { request =>
    dbConfig.db.run(ChannelEdits.allTimestamps(subDomain).result).map { seq =>
      request.getQueryString("callback") match {
        case Some(callback) => Ok(Jsonp(callback, Json.toJson(seq)))
        case None => Ok(Json.toJson(seq))
      }
    }
  }

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

  def topPages(subDomain: String) = Action.async {
    dbConfig.db.run(PageEdits.currentTopPages(subDomain).result).map(seq => Ok(Json.toJson(seq)))
  }

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
