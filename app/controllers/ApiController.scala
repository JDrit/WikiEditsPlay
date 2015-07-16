package controllers

import java.sql.Timestamp
import javax.inject.Inject

import models.{PageTops, Edits, ChannelTops}
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.{Action, Controller}
import play.api.libs.json.{JsValue, Writes, Json}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._
import play.api.libs.concurrent.Execution.Implicits._

class ApiController @Inject()(dbConfigProvider: DatabaseConfigProvider) extends Controller {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  implicit val writer1 = new Writes[(Timestamp, Long)] {
    def writes(t: (Timestamp, Long)): JsValue = Json.arr(t._1.getTime * 1000, t._2)
  }
  implicit val writer2 = new Writes[(String, Long)] {
    def writes(p: (String, Long)): JsValue = Json.arr(p._1, p._2)
  }
  implicit val writer3 = new Writes[(String, Int)] {
    def writes(p: (String, Int)): JsValue = Json.arr(p._1, p._2)
  }

  def channelEdits(subDomain: String) = Action.async { request =>
    val query = TableQuery[ChannelTops]
      .filter(_.channel === subDomain)
      .sortBy(_.timestamp)
      .map(t => (t.timestamp, t.count))
      .result
    dbConfig.db.run(query).map(seq => Ok(Json.toJson(seq)))

  }

  def topPages(subDomain: String) = Action.async { request =>
    val timestamp = TableQuery[PageTops].map(_.timestamp).max
    val query = TableQuery[PageTops]
      .filter(p => p.channel === subDomain && p.timestamp === timestamp)
      .sortBy(_.count.desc)
      .map(p => (p.page, p.count))
      .result
    dbConfig.db.run(query).map(seq => Ok(Json.toJson(seq)))
  }

  def topUsers(subDomain: String) = Action.async { request =>
    val query = TableQuery[Edits]
      .filter(_.channel === subDomain)
      .groupBy(_.username)
      .map { case (username, seq) => (username, seq.length) }
      .sortBy(_._2.reverse)
      .take(20)
      .result

    dbConfig.db.run(query).map(seq => Ok(Json.toJson(seq)))
  }
}