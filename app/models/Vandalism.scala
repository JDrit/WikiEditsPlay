package models

import java.sql.Timestamp

import controllers.Log
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag

object Vandalism {

  val vandalismForPage = Compiled((subDomain: ConstColumn[String], page: ConstColumn[String]) =>
    TableQuery[Vandalism].filter(edit => edit.channel === subDomain && edit.page === page)
      .sortBy(_.timestamp.reverse))

  val vandalismForUser = Compiled((subDomain: ConstColumn[String], user: ConstColumn[String]) =>
    TableQuery[Vandalism].filter(edit => edit.channel === subDomain && edit.username === user)
      .sortBy(_.timestamp.reverse))

  def insert(logs: List[Log]) = DBIO.seq(TableQuery[Vandalism] ++= logs map toEdit)

  private def toEdit(log: Log) =
    (log.channel, log.page, log.diff, log.username, log.comment, new Timestamp(log.timestamp))
}


class Vandalism(tag: Tag) extends Table[(String, String, String, String, String, Timestamp)](tag, "vandalism") {
  def channel   = column[String]("channel")
  def page      = column[String]("page")
  def diff      = column[String]("diff")
  def username  = column[String]("username")
  def comment   = column[String]("comment")
  def timestamp = column[Timestamp]("timestamp")
  def * = (channel, page, diff, username, comment, timestamp)
}
