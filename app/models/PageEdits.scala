package models

import java.sql.Timestamp

import slick.driver.PostgresDriver.api._
import slick.lifted.{TableQuery, Tag}

object PageEdits {
  val currentTopPages = Compiled((subDomain: ConstColumn[String]) => TableQuery[PageEdits]
    .filter(p => p.channel === subDomain && p.timestamp === TableQuery[PageEdits].map(_.timestamp).max)
    .sortBy(_.count.desc)
    .map(p => (p.page, p.count)))
}

class PageEdits(tag: Tag) extends Table[(String, String, Long, Timestamp)](tag, "channel_top_pages") {
  def channel   = column[String]("channel")
  def page      = column[String]("page")
  def count     = column[Long]("count")
  def timestamp = column[Timestamp]("timestamp")
  def * = (channel, page, count, timestamp)
}

