/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.streaming.kafka010;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

public class JavaKafkaRDDSuite implements Serializable {
  private transient JavaSparkContext sc = null;
  private transient KafkaTestUtils kafkaTestUtils = null;

  @Before
  public void setUp() {
    kafkaTestUtils = new KafkaTestUtils();
    kafkaTestUtils.setup();
    SparkConf sparkConf = new SparkConf()
      .setMaster("local[4]").setAppName(this.getClass().getSimpleName());
    sc = new JavaSparkContext(sparkConf);
  }

  @After
  public void tearDown() {
    if (sc != null) {
      sc.stop();
      sc = null;
    }

    if (kafkaTestUtils != null) {
      kafkaTestUtils.teardown();
      kafkaTestUtils = null;
    }
  }

  @Test
  public void testKafkaRDD() throws InterruptedException {
    String topic1 = "topic1";
    String topic2 = "topic2";

    Random random = new Random();

    createTopicAndSendData(topic1);
    createTopicAndSendData(topic2);

    Map<String, Object> kafkaParams = new HashMap<>();
    kafkaParams.put("bootstrap.servers", kafkaTestUtils.brokerAddress());
    kafkaParams.put("key.deserializer", StringDeserializer.class);
    kafkaParams.put("value.deserializer", StringDeserializer.class);
    kafkaParams.put("group.id", "java-test-consumer-" + random.nextInt() +
      "-" + System.currentTimeMillis());

    OffsetRange[] offsetRanges = {
      OffsetRange.create(topic1, 0, 0, 1),
      OffsetRange.create(topic2, 0, 0, 1)
    };

    Map<TopicPartition, String> leaders = new HashMap<>();
    String[] hostAndPort = kafkaTestUtils.brokerAddress().split(":");
    String broker = hostAndPort[0];
    leaders.put(offsetRanges[0].topicPartition(), broker);
    leaders.put(offsetRanges[1].topicPartition(), broker);

    Function<ConsumerRecord<String, String>, String> handler =
      new Function<ConsumerRecord<String, String>, String>() {
        @Override
        public String call(ConsumerRecord<String, String> r) {
          return r.value();
        }
      };

    JavaRDD<String> rdd1 = KafkaUtils.<String, String>createRDD(
        sc,
        kafkaParams,
        offsetRanges,
        LocationStrategies.PreferFixed(leaders)
    ).map(handler);

    JavaRDD<String> rdd2 = KafkaUtils.<String, String>createRDD(
        sc,
        kafkaParams,
        offsetRanges,
        LocationStrategies.PreferConsistent()
    ).map(handler);

    // just making sure the java user apis work; the scala tests handle logic corner cases
    long count1 = rdd1.count();
    long count2 = rdd2.count();
    Assert.assertTrue(count1 > 0);
    Assert.assertEquals(count1, count2);
  }

  private  String[] createTopicAndSendData(String topic) {
    String[] data = { topic + "-1", topic + "-2", topic + "-3"};
    kafkaTestUtils.createTopic(topic);
    kafkaTestUtils.sendMessages(topic, data);
    return data;
  }
}
