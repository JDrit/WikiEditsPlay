package models

import java.sql.Timestamp
import java.util.Date

import controllers.Log
import slick.backend.StaticDatabaseConfig
import slick.driver.PostgresDriver.api._
import slick.lifted.{TableQuery, Tag}

@StaticDatabaseConfig("file:conf/application.conf#tsql")
object Edit {

  val allEdits = tsql"""
        SELECT
         date_trunc('minute', timestamp) AS minute,
         count(*) AS count
        FROM log
        GROUP BY minute
        ORDER BY minute"""

  val listOfChannels = tsql"SELECT DISTINCT(channel) from log"

  val mostCommonIps = tsql"""
        SELECT 
          username, 
          count(*) AS count
        FROM log
        WHERE username ~ '^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{0,3}\.[0-9]{0,3}$$' 
        GROUP BY username 
        ORDER BY count DESC
        LIMIT 10"""

  def mostCommonIpsForDomain(domain: String) = tsql"""
        SELECT 
          username, 
          count(*) AS count
        FROM log
        WHERE 
          channel = $domain AND
          username ~ '^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{0,3}\.[0-9]{0,3}$$'
        GROUP BY username 
        ORDER BY count DESC
        LIMIT 10""" 

  def topDomains(start: Timestamp, end: Timestamp) = tsql"""
        SELECT
          channel, 
          count(*) AS count
        FROM log 
        WHERE 
          timestamp >= $start AND
          timestamp <= $end
        GROUP BY channel 
        ORDER BY count DESC
        LIMIT 10"""

  def domainEdits(domain: String) = 
    tsql"""
      SELECT
        date_trunc('minute', timestamp) AS minute,
        count(*) AS count
      FROM log
      WHERE channel = $domain
      GROUP BY minute
      ORDER BY minute"""

  def usersForPage(domain: String, page: String, start: Timestamp, end: Timestamp) =
    tsql"""
      SELECT
        username,
        count(*)
      FROM log
      WHERE
        channel = $domain AND
        page = $page AND
        timestamp >= $start AND
        timestamp <= $end
      GROUP BY username
      ORDER BY count(*) DESC
      LIMIT 20"""

  val topPages = Compiled((subDomain: ConstColumn[String], start: ConstColumn[Timestamp], end: ConstColumn[Timestamp]) =>
    TableQuery[Edit].filter(edit => edit.channel === subDomain && edit.timestamp >= start && edit.timestamp <= end)
      .groupBy(_.page)
      .map {
        case (page, seq) => (page, seq.length)
      }.sortBy(_._2.reverse)
      .take(20))

  val topUsers = Compiled((subDomain: ConstColumn[String], start: ConstColumn[Timestamp], end: ConstColumn[Timestamp]) =>
    TableQuery[Edit].filter(edit => edit.channel === subDomain && edit.timestamp >= start && edit.timestamp <= end)
      .groupBy(_.username)
      .map {
        case (username, seq) => (username, seq.length)
      }.sortBy(_._2.reverse)
      .take(20))

  def editsForPage(domain: String, page: String) =
    tsql"""SELECT
            date_trunc('minute', timestamp) AS minute,
            count(*) AS COUNT
           FROM log
           WHERE
            channel = $domain AND
            page = $page
           GROUP BY minute
           ORDER BY minute"""


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
