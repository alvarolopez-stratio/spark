/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.examples.mllib;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

// $example on$
import scala.Tuple2;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.clustering.DistributedLDAModel;
import org.apache.spark.mllib.clustering.LDA;
import org.apache.spark.mllib.clustering.LDAModel;
import org.apache.spark.mllib.linalg.Matrix;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
// $example off$

public class JavaLatentDirichletAllocationExample {
  public static void main(String[] args) {

    SparkConf conf = new SparkConf().setAppName("JavaKLatentDirichletAllocationExample");
    JavaSparkContext jsc = new JavaSparkContext(conf);

    // $example on$
    // Load and parse the data
    String path = "data/mllib/sample_lda_data.txt";
    JavaRDD<String> data = jsc.textFile(path);
    JavaRDD<Vector> parsedData = data.map(
      new Function<String, Vector>() {
        public Vector call(String s) {
          String[] sarray = s.trim().split(" ");
          double[] values = new double[sarray.length];
          for (int i = 0; i < sarray.length; i++) {
            values[i] = Double.parseDouble(sarray[i]);
          }
          return Vectors.dense(values);
        }
      }
    );
    // Index documents with unique IDs
    JavaPairRDD<Long, Vector> corpus =
      JavaPairRDD.fromJavaRDD(parsedData.zipWithIndex().map(
        new Function<Tuple2<Vector, Long>, Tuple2<Long, Vector>>() {
          public Tuple2<Long, Vector> call(Tuple2<Vector, Long> doc_id) {
            return doc_id.swap();
          }
        }
      )
    );
    corpus.cache();

    // Cluster the documents into three topics using LDA
    LDAModel ldaModel = new LDA().setK(3).run(corpus);

    // Output topics. Each is a distribution over words (matching word count vectors)
    System.out.println("Learned topics (as distributions over vocab of " + ldaModel.vocabSize()
      + " words):");
    Matrix topics = ldaModel.topicsMatrix();
    for (int topic = 0; topic < 3; topic++) {
      System.out.print("Topic " + topic + ":");
      for (int word = 0; word < ldaModel.vocabSize(); word++) {
        System.out.print(" " + topics.apply(word, topic));
      }
      System.out.println();
    }

    ldaModel.save(jsc.sc(),
      "target/org/apache/spark/JavaLatentDirichletAllocationExample/LDAModel");
    DistributedLDAModel sameModel = DistributedLDAModel.load(jsc.sc(),
      "target/org/apache/spark/JavaLatentDirichletAllocationExample/LDAModel");
    // $example off$

    jsc.stop();
  }
}
