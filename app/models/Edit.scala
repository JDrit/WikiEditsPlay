package models

import java.sql.Timestamp

import controllers.Log
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag

object Edit {

  def insert(logs: List[Log]) = DBIO.seq(TableQuery[Edit] ++= logs.map(toEdit))

  private def toEdit(log: Log) =
    (log.channel, log.page, log.diff, log.username, log.comment, new Timestamp((log.timestamp)))
}


class Edit(tag: Tag) extends Table[(String, String, String, String, String, Timestamp)](tag, "log") {
  def channel   = column[String]("channel")
  def page      = column[String]("page")
  def diff      = column[String]("diff")
  def username  = column[String]("username")
  def comment   = column[String]("comment")
  def timestamp = column[Timestamp]("timestamp")
  def * = (channel, page, diff, username, comment, timestamp)
}
