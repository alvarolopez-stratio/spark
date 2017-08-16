/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.input

import com.google.common.io.{ByteStreams, Closeables}
import org.apache.hadoop.conf.{Configurable => HConfigurable, Configuration}
import org.apache.hadoop.io.Text
import org.apache.hadoop.io.compress.CompressionCodecFactory
import org.apache.hadoop.mapreduce.InputSplit
import org.apache.hadoop.mapreduce.RecordReader
import org.apache.hadoop.mapreduce.TaskAttemptContext
import org.apache.hadoop.mapreduce.lib.input.{CombineFileRecordReader, CombineFileSplit}

/**
 * A trait to implement [[org.apache.hadoop.conf.Configurable Configurable]] interface.
 */
private[spark] trait Configurable extends HConfigurable {
  private var conf: Configuration = _
  def setConf(c: Configuration) {
    conf = c
  }
  def getConf: Configuration = conf
}

/**
 * A [[org.apache.hadoop.mapreduce.RecordReader RecordReader]] for reading a single whole text file
 * out in a key-value pair, where the key is the file path and the value is the entire content of
 * the file.
 */
private[spark] class WholeTextFileRecordReader(
    split: CombineFileSplit,
    context: TaskAttemptContext,
    index: Integer)
  extends RecordReader[Text, Text] with Configurable {

  private[this] val path = split.getPath(index)
  private[this] val fs = path.getFileSystem(context.getConfiguration)

  // True means the current file has been processed, then skip it.
  private[this] var processed = false

  private[this] val key: Text = new Text(path.toString)
  private[this] var value: Text = null

  override def initialize(split: InputSplit, context: TaskAttemptContext): Unit = {}

  override def close(): Unit = {}

  override def getProgress: Float = if (processed) 1.0f else 0.0f

  override def getCurrentKey: Text = key

  override def getCurrentValue: Text = value

  override def nextKeyValue(): Boolean = {
    if (!processed) {
      val conf = new Configuration
      val factory = new CompressionCodecFactory(conf)
      val codec = factory.getCodec(path)  // infers from file ext.
      val fileIn = fs.open(path)
      val innerBuffer = if (codec != null) {
        ByteStreams.toByteArray(codec.createInputStream(fileIn))
      } else {
        ByteStreams.toByteArray(fileIn)
      }

      value = new Text(innerBuffer)
      Closeables.close(fileIn, false)
      processed = true
      true
    } else {
      false
    }
  }
}


/**
 * A [[org.apache.hadoop.mapreduce.lib.input.CombineFileRecordReader CombineFileRecordReader]]
 * that can pass Hadoop Configuration to [[org.apache.hadoop.conf.Configurable Configurable]]
 * RecordReaders.
 */
private[spark] class ConfigurableCombineFileRecordReader[K, V](
    split: InputSplit,
    context: TaskAttemptContext,
    recordReaderClass: Class[_ <: RecordReader[K, V] with HConfigurable])
  extends CombineFileRecordReader[K, V](
    split.asInstanceOf[CombineFileSplit],
    context,
    recordReaderClass
  ) with Configurable {

  override def initNextRecordReader(): Boolean = {
    val r = super.initNextRecordReader()
    if (r) {
      this.curReader.asInstanceOf[HConfigurable].setConf(getConf)
    }
    r
  }
}
