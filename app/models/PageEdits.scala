package models

import java.sql.Timestamp

import controllers.TopPageContainer
import slick.driver.PostgresDriver.api._
import slick.lifted.{TableQuery, Tag}

object PageEdits {
  val currentTopPages = Compiled((subDomain: ConstColumn[String]) => {
    val maxTime = TableQuery[PageEdits].filter(_.channel === subDomain)
      .map(_.timestamp).max
      
    TableQuery[PageEdits].filter(p => p.channel === subDomain && p.timestamp === maxTime)
      .sortBy(_.count.desc)
      .map(p => (p.page, p.count))
  })

  def insert(pages: TopPageContainer) =
    DBIO.seq(TableQuery[PageEdits].delete, TableQuery[PageEdits] ++= toEdits(pages))

  private def toEdits(pages: TopPageContainer) = pages.pages.map { page =>
    (page.channel, page.page, page.count, new Timestamp(pages.timestamp))
  }
}

class PageEdits(tag: Tag) extends Table[(String, String, Long, Timestamp)](tag, "channel_top_pages") {
  def channel   = column[String]("channel")
  def page      = column[String]("page")
  def count     = column[Long]("count")
  def timestamp = column[Timestamp]("timestamp")
  def * = (channel, page, count, timestamp)
}

