package controllers

import java.sql.Timestamp
import javax.inject.Inject

import models.{PageTops, Edits, ChannelTops}
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.Jsonp
import play.api.mvc.{Action, Controller}
import play.api.libs.json.{JsValue, Writes, Json}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._
import play.api.libs.concurrent.Execution.Implicits._

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

  private val channelQuery = Compiled((subDomain: ConstColumn[String]) => TableQuery[ChannelTops]
    .filter(_.channel === subDomain)
    .sortBy(_.timestamp)
    .map(t => (t.timestamp, t.count)))

  private val pagesQuery = Compiled((subDomain: ConstColumn[String]) => TableQuery[PageTops]
    .filter(p => p.channel === subDomain && p.timestamp === TableQuery[PageTops].map(_.timestamp).max)
    .sortBy(_.count.desc)
    .map(p => (p.page, p.count)))

  private val usersQuery = Compiled((subDomain: ConstColumn[String]) => TableQuery[Edits]
    .filter(_.channel === subDomain)
    .groupBy(_.username)
    .map { case (username, seq) => (username, seq.length) }
    .sortBy(_._2.reverse)
    .take(20))

  private val channelUpdateQuery = Compiled((subDomain: ConstColumn[String]) => TableQuery[ChannelTops]
    .filter(r => r.channel === subDomain && r.timestamp === TableQuery[ChannelTops].filter(_.channel === subDomain).map(_.timestamp).max)
    .map(r => (r.timestamp, r.count))
    .take(1))

  def channelEdits(subDomain: String) = Action.async { request =>
    dbConfig.db.run(channelQuery(subDomain).result).map { seq =>
      request.getQueryString("callback") match {
        case Some(callback) => Ok(Jsonp(callback, Json.toJson(seq)))
        case None => Ok(Json.toJson(seq))
      }
    }
  }

  def channelEditsUpdate(subDomain: String) = Action.async { request =>
    dbConfig.db.run(channelUpdateQuery(subDomain).result).map { seq =>
      if (seq.isEmpty) {
        BadRequest
      } else {
        request.getQueryString("callback") match {
          case Some(callback) => Ok(Jsonp(callback, Json.toJson(seq.head)))
          case None => Ok(Json.toJson(seq.head))
        }
      }
    }
  }

  def topPages(subDomain: String) = Action.async {
    dbConfig.db.run(pagesQuery(subDomain).result).map(seq => Ok(Json.toJson(seq)))
  }

  def topUsers(subDomain: String) = Action.async {
    dbConfig.db.run(usersQuery(subDomain).result).map(seq => Ok(Json.toJson(seq)))
  }
}