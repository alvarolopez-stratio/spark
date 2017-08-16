/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.util.logging

import java.text.SimpleDateFormat
import java.util.{Calendar, Locale}

import org.apache.spark.internal.Logging

/**
 * Defines the policy based on which [[org.apache.spark.util.logging.RollingFileAppender]] will
 * generate rolling files.
 */
private[spark] trait RollingPolicy {

  /** Whether rollover should be initiated at this moment */
  def shouldRollover(bytesToBeWritten: Long): Boolean

  /** Notify that rollover has occurred */
  def rolledOver(): Unit

  /** Notify that bytes have been written */
  def bytesWritten(bytes: Long): Unit

  /** Get the desired name of the rollover file */
  def generateRolledOverFileSuffix(): String
}

/**
 * Defines a [[org.apache.spark.util.logging.RollingPolicy]] by which files will be rolled
 * over at a fixed interval.
 */
private[spark] class TimeBasedRollingPolicy(
    var rolloverIntervalMillis: Long,
    rollingFileSuffixPattern: String,
    checkIntervalConstraint: Boolean = true   // set to false while testing
  ) extends RollingPolicy with Logging {

  import TimeBasedRollingPolicy._
  if (checkIntervalConstraint && rolloverIntervalMillis < MINIMUM_INTERVAL_SECONDS * 1000L) {
    logWarning(s"Rolling interval [${rolloverIntervalMillis/1000L} seconds] is too small. " +
      s"Setting the interval to the acceptable minimum of $MINIMUM_INTERVAL_SECONDS seconds.")
    rolloverIntervalMillis = MINIMUM_INTERVAL_SECONDS * 1000L
  }

  @volatile private var nextRolloverTime = calculateNextRolloverTime()
  private val formatter = new SimpleDateFormat(rollingFileSuffixPattern, Locale.US)

  /** Should rollover if current time has exceeded next rollover time */
  def shouldRollover(bytesToBeWritten: Long): Boolean = {
    System.currentTimeMillis > nextRolloverTime
  }

  /** Rollover has occurred, so find the next time to rollover */
  def rolledOver() {
    nextRolloverTime = calculateNextRolloverTime()
    logDebug(s"Current time: ${System.currentTimeMillis}, next rollover time: " + nextRolloverTime)
  }

  def bytesWritten(bytes: Long) { }  // nothing to do

  private def calculateNextRolloverTime(): Long = {
    val now = System.currentTimeMillis()
    val targetTime = (
      math.ceil(now.toDouble / rolloverIntervalMillis) * rolloverIntervalMillis
    ).toLong
    logDebug(s"Next rollover time is $targetTime")
    targetTime
  }

  def generateRolledOverFileSuffix(): String = {
    formatter.format(Calendar.getInstance.getTime)
  }
}

private[spark] object TimeBasedRollingPolicy {
  val MINIMUM_INTERVAL_SECONDS = 60L  // 1 minute
}

/**
 * Defines a [[org.apache.spark.util.logging.RollingPolicy]] by which files will be rolled
 * over after reaching a particular size.
 */
private[spark] class SizeBasedRollingPolicy(
    var rolloverSizeBytes: Long,
    checkSizeConstraint: Boolean = true     // set to false while testing
  ) extends RollingPolicy with Logging {

  import SizeBasedRollingPolicy._
  if (checkSizeConstraint && rolloverSizeBytes < MINIMUM_SIZE_BYTES) {
    logWarning(s"Rolling size [$rolloverSizeBytes bytes] is too small. " +
      s"Setting the size to the acceptable minimum of $MINIMUM_SIZE_BYTES bytes.")
    rolloverSizeBytes = MINIMUM_SIZE_BYTES
  }

  @volatile private var bytesWrittenSinceRollover = 0L
  val formatter = new SimpleDateFormat("--yyyy-MM-dd--HH-mm-ss--SSSS", Locale.US)

  /** Should rollover if the next set of bytes is going to exceed the size limit */
  def shouldRollover(bytesToBeWritten: Long): Boolean = {
    logDebug(s"$bytesToBeWritten + $bytesWrittenSinceRollover > $rolloverSizeBytes")
    bytesToBeWritten + bytesWrittenSinceRollover > rolloverSizeBytes
  }

  /** Rollover has occurred, so reset the counter */
  def rolledOver() {
    bytesWrittenSinceRollover = 0
  }

  /** Increment the bytes that have been written in the current file */
  def bytesWritten(bytes: Long) {
    bytesWrittenSinceRollover += bytes
  }

  /** Get the desired name of the rollover file */
  def generateRolledOverFileSuffix(): String = {
    formatter.format(Calendar.getInstance.getTime)
  }
}

private[spark] object SizeBasedRollingPolicy {
  val MINIMUM_SIZE_BYTES = RollingFileAppender.DEFAULT_BUFFER_SIZE * 10
}

