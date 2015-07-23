package models

import java.sql.Timestamp

import slick.driver.PostgresDriver.api._
import slick.lifted.Tag

class Edits(tag: Tag) extends Table[(String, String, String, String, String, Timestamp)](tag, "log") {
  def channel   = column[String]("channel")
  def page      = column[String]("page")
  def diff      = column[String]("diff")
  def username  = column[String]("username")
  def comment   = column[String]("comment")
  def timestamp = column[Timestamp]("timestamp")
  def * = (channel, page, diff, username, comment, timestamp)
}

class ChannelTops(tag: Tag) extends Table[(String, Long, Timestamp)](tag, "channel_edit_count") {
  def channel   = column[String]("channel")
  def count     = column[Long]("count")
  def timestamp = column[Timestamp]("timestamp")
  def * = (channel, count, timestamp)
}

class PageTops(tag: Tag) extends Table[(String, String, Long, Timestamp)](tag, "channel_top_pages") {
  def channel   = column[String]("channel")
  def page      = column[String]("page")
  def count     = column[Long]("count")
  def timestamp = column[Timestamp]("timestamp")
  def * = (channel, page, count, timestamp)
}

class UserTops(tag: Tag) extends Table[(String, String, Long, Timestamp)](tag, "channel_top_users") {
  def channel   = column[String]("channel")
  def username  = column[String]("username")
  def count     = column[Long]("count")
  def timestamp = column[Timestamp]("timestamp")
  def * = (channel, username, count, timestamp)
}