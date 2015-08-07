package controllers

import java.sql.Timestamp

import models.Edit
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Case classes representing the payloads from the stream to be loaded into the database
 */
case class Log(channel: String, comment: String, diff: String,
               page: String, timestamp: Long, username: String)
case class ChannelCount(channel: String, count: Long)
case class ChannelsPayload(timestamp: Long, channels: List[ChannelCount])

case class TopPage(channel: String, page: String, count: Long)
case class TopPageContainer(timestamp: Long, pages: List[TopPage])

case class TopUser(channel: String, username: String, count: Long)
case class TopUserContainer(timestamp: Long, users: List[TopUser])

case class AnomalyCase(channel: String, page: String, mean: Double, stdDev: Double, timestamp: Long, count: Long)


/**
 * Json reader and writers used to parse JSON fron the stream and to return results to
 * the web client
 */
object JSONConverters {

  implicit val writer1 = new Writes[(Timestamp, Long)] {
    def writes(t: (Timestamp, Long)): JsValue = Json.arr(t._1.getTime, t._2)
  }
  implicit val writer2 = new Writes[(String, Long)] {
    def writes(p: (String, Long)): JsValue = Json.arr(p._1, p._2)
  }
  implicit val writer3 = new Writes[(String, Int)] {
    def writes(p: (String, Int)): JsValue = Json.arr(p._1, p._2)
  }

  implicit val writer4 = new Writes[(String, String, Long)] {
    def writes(p: (String, String, Long)): JsValue = Json.arr(p._1, p._2, p._3)
  }

  implicit val anomalyReads: Reads[AnomalyCase] = (
    (JsPath \ "channel").read[String] and
      (JsPath \ "page").read[String] and
      (JsPath \ "mean").read[Double] and
      (JsPath \ "standard_deviation").read[Double] and
      (JsPath \ "timestamp").read[Long] and
      (JsPath \ "count").read[Long]
    )(AnomalyCase.apply _)

  implicit val logReads: Reads[Log] = (
    (JsPath \ "channel").read[String] and
      (JsPath \ "comment").read[String] and
      (JsPath \ "diff").read[String] and
      (JsPath \ "page").read[String] and
      (JsPath \ "timestamp").read[Long] and
      (JsPath \ "username").read[String]
    )(Log.apply _)

  implicit val channelCountReads: Reads[ChannelCount] = (
    (JsPath \ "channel").read[String] and
      (JsPath \ "count").read[Long]
    )(ChannelCount.apply _)

  implicit val channelsPayloadReads: Reads[ChannelsPayload] = (
    (JsPath \ "timestamp").read[Long] and
      (JsPath \ "page_edits").read[List[ChannelCount]]
    )(ChannelsPayload.apply _)

  implicit val topPageReads: Reads[TopPage] = (
    (JsPath \ "channel").read[String] and
      (JsPath \ "page").read[String] and
      (JsPath \ "count").read[Long]
    )(TopPage.apply _)

  implicit val topPageContainerReads: Reads[TopPageContainer] = (
    (JsPath \ "timestamp").read[Long] and
      (JsPath \ "top_pages").read[List[TopPage]]
    )(TopPageContainer.apply _)

  implicit val topUserReads: Reads[TopUser] = (
    (JsPath \ "channel").read[String] and
      (JsPath \ "username").read[String] and
      (JsPath \ "count").read[Long]
    )(TopUser.apply _)

  implicit val topUserContainerReads: Reads[TopUserContainer] = (
    (JsPath \ "timestamp").read[Long] and
      (JsPath \ "top_users").read[List[TopUser]]
    )(TopUserContainer.apply _)
}
