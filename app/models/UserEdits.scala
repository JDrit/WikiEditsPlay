package models

import java.sql.Timestamp

import slick.driver.PostgresDriver.api._
import slick.lifted.{TableQuery, Tag}

object UserEdits {
  val totalMostActiveUsers = Compiled((subDomain: ConstColumn[String]) => TableQuery[Edit]
    .filter(_.channel === subDomain)
    .groupBy(_.username)
    .map { case (username, seq) => (username, seq.length) }
    .sortBy(_._2.reverse)
    .take(20))
}

class UserEdits(tag: Tag) extends Table[(String, String, Long, Timestamp)](tag, "channel_top_users") {
  def channel   = column[String]("channel")
  def username  = column[String]("username")
  def count     = column[Long]("count")
  def timestamp = column[Timestamp]("timestamp")
  def * = (channel, username, count, timestamp)
}
