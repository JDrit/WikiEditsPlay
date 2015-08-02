package models

import java.sql.Timestamp

import controllers.{AnomalyCase, Log}
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag

object Anomaly {

  val anomaliesForPage = Compiled((subDomain: ConstColumn[String], page: ConstColumn[String]) =>
    TableQuery[Anomaly].filter(edit => edit.channel === subDomain && edit.page === page)
      .sortBy(_.timestamp.reverse))

  def insert(anomalies: List[AnomalyCase]) = DBIO.seq(TableQuery[Anomaly] ++= anomalies map toEdit)

  private def toEdit(log: AnomalyCase) =
    (log.channel, log.page, log.mean, log.stdDev, new Timestamp(log.timestamp), log.count)
}


class Anomaly(tag: Tag) extends Table[(String, String, Double, Double, Timestamp, Long)](tag, "anomalies") {
  def channel   = column[String]("channel")
  def page      = column[String]("page")
  def mean      = column[Double]("mean")
  def stddev    = column[Double]("stddev")
  def timestamp = column[Timestamp]("timestamp")
  def count     = column[Long]("count")
  def * = (channel, page, mean, stddev, timestamp, count)
}
