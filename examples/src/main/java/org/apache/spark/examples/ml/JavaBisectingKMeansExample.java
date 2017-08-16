/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.examples.ml;

// $example on$
import org.apache.spark.ml.clustering.BisectingKMeans;
import org.apache.spark.ml.clustering.BisectingKMeansModel;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
// $example off$
import org.apache.spark.sql.SparkSession;


/**
 * An example demonstrating bisecting k-means clustering.
 * Run with
 * <pre>
 * bin/run-example ml.JavaBisectingKMeansExample
 * </pre>
 */
public class JavaBisectingKMeansExample {

  public static void main(String[] args) {
    SparkSession spark = SparkSession
      .builder()
      .appName("JavaBisectingKMeansExample")
      .getOrCreate();

    // $example on$
    // Loads data.
    Dataset<Row> dataset = spark.read().format("libsvm").load("data/mllib/sample_kmeans_data.txt");

    // Trains a bisecting k-means model.
    BisectingKMeans bkm = new BisectingKMeans().setK(2).setSeed(1);
    BisectingKMeansModel model = bkm.fit(dataset);

    // Evaluate clustering.
    double cost = model.computeCost(dataset);
    System.out.println("Within Set Sum of Squared Errors = " + cost);

    // Shows the result.
    System.out.println("Cluster Centers: ");
    Vector[] centers = model.clusterCenters();
    for (Vector center : centers) {
      System.out.println(center);
    }
    // $example off$

    spark.stop();
  }
}
