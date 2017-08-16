/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.streaming.kafka

import java.util.Properties

import scala.collection.Map
import scala.reflect.{classTag, ClassTag}

import kafka.consumer.{Consumer, ConsumerConfig, ConsumerConnector, KafkaStream}
import kafka.serializer.Decoder
import kafka.utils.VerifiableProperties

import org.apache.spark.internal.Logging
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream._
import org.apache.spark.streaming.receiver.Receiver
import org.apache.spark.util.ThreadUtils

/**
 * Input stream that pulls messages from a Kafka Broker.
 *
 * @param kafkaParams Map of kafka configuration parameters.
 *                    See: http://kafka.apache.org/configuration.html
 * @param topics Map of (topic_name to numPartitions) to consume. Each partition is consumed
 * in its own thread.
 * @param storageLevel RDD storage level.
 */
private[streaming]
class KafkaInputDStream[
  K: ClassTag,
  V: ClassTag,
  U <: Decoder[_]: ClassTag,
  T <: Decoder[_]: ClassTag](
    _ssc: StreamingContext,
    kafkaParams: Map[String, String],
    topics: Map[String, Int],
    useReliableReceiver: Boolean,
    storageLevel: StorageLevel
  ) extends ReceiverInputDStream[(K, V)](_ssc) with Logging {

  def getReceiver(): Receiver[(K, V)] = {
    if (!useReliableReceiver) {
      new KafkaReceiver[K, V, U, T](kafkaParams, topics, storageLevel)
    } else {
      new ReliableKafkaReceiver[K, V, U, T](kafkaParams, topics, storageLevel)
    }
  }
}

private[streaming]
class KafkaReceiver[
  K: ClassTag,
  V: ClassTag,
  U <: Decoder[_]: ClassTag,
  T <: Decoder[_]: ClassTag](
    kafkaParams: Map[String, String],
    topics: Map[String, Int],
    storageLevel: StorageLevel
  ) extends Receiver[(K, V)](storageLevel) with Logging {

  // Connection to Kafka
  var consumerConnector: ConsumerConnector = null

  def onStop() {
    if (consumerConnector != null) {
      consumerConnector.shutdown()
      consumerConnector = null
    }
  }

  def onStart() {

    logInfo("Starting Kafka Consumer Stream with group: " + kafkaParams("group.id"))

    // Kafka connection properties
    val props = new Properties()
    kafkaParams.foreach(param => props.put(param._1, param._2))

    val zkConnect = kafkaParams("zookeeper.connect")
    // Create the connection to the cluster
    logInfo("Connecting to Zookeeper: " + zkConnect)
    val consumerConfig = new ConsumerConfig(props)
    consumerConnector = Consumer.create(consumerConfig)
    logInfo("Connected to " + zkConnect)

    val keyDecoder = classTag[U].runtimeClass.getConstructor(classOf[VerifiableProperties])
      .newInstance(consumerConfig.props)
      .asInstanceOf[Decoder[K]]
    val valueDecoder = classTag[T].runtimeClass.getConstructor(classOf[VerifiableProperties])
      .newInstance(consumerConfig.props)
      .asInstanceOf[Decoder[V]]

    // Create threads for each topic/message Stream we are listening
    val topicMessageStreams = consumerConnector.createMessageStreams(
      topics, keyDecoder, valueDecoder)

    val executorPool =
      ThreadUtils.newDaemonFixedThreadPool(topics.values.sum, "KafkaMessageHandler")
    try {
      // Start the messages handler for each partition
      topicMessageStreams.values.foreach { streams =>
        streams.foreach { stream => executorPool.submit(new MessageHandler(stream)) }
      }
    } finally {
      executorPool.shutdown() // Just causes threads to terminate after work is done
    }
  }

  // Handles Kafka messages
  private class MessageHandler(stream: KafkaStream[K, V])
    extends Runnable {
    def run() {
      logInfo("Starting MessageHandler.")
      try {
        val streamIterator = stream.iterator()
        while (streamIterator.hasNext()) {
          val msgAndMetadata = streamIterator.next()
          store((msgAndMetadata.key, msgAndMetadata.message))
        }
      } catch {
        case e: Throwable => reportError("Error handling message; exiting", e)
      }
    }
  }
}
