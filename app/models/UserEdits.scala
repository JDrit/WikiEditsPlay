package models

import java.sql.Timestamp

import controllers.TopUserContainer
import slick.driver.PostgresDriver.api._
import slick.lifted.{TableQuery, Tag}

object UserEdits {

  def insert(users: TopUserContainer) = DBIO.seq(TableQuery[UserEdits] ++= toEdits(users))

  private def toEdits(users: TopUserContainer) = users.users.map { user =>
    (user.channel, user.username, user.count, new Timestamp(users.timestamp))   
  }
}

class UserEdits(tag: Tag) extends Table[(String, String, Long, Timestamp)](tag, "channel_top_users") {
  def channel   = column[String]("channel")
  def username  = column[String]("username")
  def count     = column[Long]("count")
  def timestamp = column[Timestamp]("timestamp")
  def * = (channel, username, count, timestamp)
}
