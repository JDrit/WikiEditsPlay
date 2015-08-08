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

  implicit def tuple2Writes[A, B](implicit aWrites: Writes[A], bWrites: Writes[B]): Writes[Tuple2[A, B]] = new Writes[Tuple2[A, B]] {
    def writes(tuple: Tuple2[A, B]) = JsArray(Seq(aWrites.writes(tuple._1), bWrites.writes(tuple._2)))
  }

  implicit def tuple3Writes[A, B, C](implicit aWrites: Writes[A], bWrites: Writes[B], cWrites: Writes[C]): Writes[Tuple3[A, B, C]] = new Writes[Tuple3[A, B, C]] {
    def writes(tuple: Tuple3[A, B, C]) = JsArray(Seq(aWrites.writes(tuple._1), bWrites.writes(tuple._2), cWrites.writes(tuple._3)))
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
