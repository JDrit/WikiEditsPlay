package models

import java.sql.Timestamp

import controllers.ChannelsPayload
import slick.driver.PostgresDriver.api._
import slick.lifted.{TableQuery, Tag}

object ChannelEdits {
  val allTimestamps = Compiled((subDomain: ConstColumn[String]) => TableQuery[ChannelEdits]
    .filter(_.channel === subDomain)
    .sortBy(_.timestamp)
    .map(t => (t.timestamp, t.count)))

  val mostCurrent = Compiled((subDomain: ConstColumn[String]) => {
    val maxTime = TableQuery[ChannelEdits].filter(_.channel === subDomain)
      .map(_.timestamp).max

    TableQuery[ChannelEdits]
      .filter(r => r.channel === subDomain && r.timestamp === maxTime)
      .map(r => (r.timestamp, r.count))
      .take(1)
  })

  def insert(edits: ChannelsPayload) = 
    DBIO.seq(TableQuery[ChannelEdits] ++= toEdits(edits))


  private def toEdits(edits: ChannelsPayload) = edits.channels.map { channel => 
    (channel.channel, channel.count, new Timestamp(edits.timestamp))
  }
}


class ChannelEdits(tag: Tag) extends Table[(String, Long, Timestamp)](tag, "channel_edit_count") {
  def channel   = column[String]("channel")
  def count     = column[Long]("count")
  def timestamp = column[Timestamp]("timestamp")
  def * = (channel, count, timestamp)
}

