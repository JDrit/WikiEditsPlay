package models

import java.sql.Timestamp

import controllers.TopUserContainer
import slick.driver.PostgresDriver.api._
import slick.lifted.{TableQuery, Tag}

object UserEdits {

  val currentTopUsers = Compiled((subDomain: ConstColumn[String]) => {
    val maxTime = TableQuery[UserEdits].filter(_.channel === subDomain)
      .map(_.timestamp).max

    TableQuery[UserEdits].filter(p => p.channel === subDomain && p.timestamp === maxTime)
      .sortBy(_.count.desc)
      .map(p => (p.username, p.count))
  })

  def insert(users: TopUserContainer) =
    DBIO.seq(TableQuery[UserEdits].delete, TableQuery[UserEdits] ++= toEdits(users))

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
