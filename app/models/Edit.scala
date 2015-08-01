package models

import java.sql.Timestamp

import controllers.Log
import slick.driver.PostgresDriver.api._
import slick.lifted.{TableQuery, Tag}

object Edit {

  val editsPerPage = Compiled((subDomain: ConstColumn[String], page: ConstColumn[String]) =>
    TableQuery[Edit].filter(edit => edit.channel === subDomain && edit.page === page)
      .sortBy(_.timestamp.reverse))

  val activeUsersForPage = Compiled((subDomain: ConstColumn[String], page: ConstColumn[String]) =>
    TableQuery[Edit].filter(edit => edit.channel === subDomain && edit.page === page)
      .groupBy(_.username)
      .map { case (username, seq) => (username, seq.length) }
      .sortBy(_._2.reverse)
      .take(20))

  val activeUsersForDomain = Compiled((subDomain: ConstColumn[String]) =>
    TableQuery[Edit].filter(_.channel === subDomain)
      .groupBy(_.username)
      .map { case (username, seq) => (username, seq.length) }
      .sortBy(_._2.reverse)
      .take(20))

  val editsForUser = Compiled((subDomain: ConstColumn[String], user: ConstColumn[String]) =>
    TableQuery[Edit].filter(edit => edit.channel === subDomain && edit.username === user)
      .sortBy(_.timestamp.reverse))


  def insert(logs: List[Log]) = DBIO.seq(TableQuery[Edit] ++= logs map toEdit)

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
