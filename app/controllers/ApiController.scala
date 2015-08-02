package controllers


import javax.inject.Inject

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
    dbConfig.db.run(Edit.activeUsersForDomain(subDomain).result).map(seq => Ok(Json.toJson(seq)))
  }



  /** ----------------------------------------------------------------------------------- */
  /** ---------------------- UPDATE DATABASE API ---------------------------------------- */
  /** ----------------------------------------------------------------------------------- */

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
      .map(json => json.validate[List[Log]])
      .map(logs => Vandalism.insert(logs.get))
      .map(action => dbConfig.db.run(action).map(q => Ok))
      .getOrElse(Future { BadRequest })
  }

  def addAnomalies() = Action.async { request =>
    request.body.asJson
      .map(json => json.validate[List[Log]])
      .map(logs => Anomaly.insert(logs.get))
      .map(action => dbConfig.db.run(action).map(q => Ok))
      .getOrElse(Future { BadRequest })  }
}
