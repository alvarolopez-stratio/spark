/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.ml.source.libsvm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Files;

import org.junit.Assert;
import org.junit.Test;

import org.apache.spark.SharedSparkSession;
import org.apache.spark.ml.linalg.DenseVector;
import org.apache.spark.ml.linalg.Vectors;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.util.Utils;


/**
 * Test LibSVMRelation in Java.
 */
public class JavaLibSVMRelationSuite extends SharedSparkSession {

  private File tempDir;
  private String path;

  @Override
  public void setUp() throws IOException {
    super.setUp();
    tempDir = Utils.createTempDir(System.getProperty("java.io.tmpdir"), "datasource");
    File file = new File(tempDir, "part-00000");
    String s = "1 1:1.0 3:2.0 5:3.0\n0\n0 2:4.0 4:5.0 6:6.0";
    Files.write(s, file, StandardCharsets.UTF_8);
    path = tempDir.toURI().toString();
  }

  @Override
  public void tearDown() {
    super.tearDown();
    Utils.deleteRecursively(tempDir);
  }

  @Test
  public void verifyLibSVMDF() {
    Dataset<Row> dataset = spark.read().format("libsvm").option("vectorType", "dense")
      .load(path);
    Assert.assertEquals("label", dataset.columns()[0]);
    Assert.assertEquals("features", dataset.columns()[1]);
    Row r = dataset.first();
    Assert.assertEquals(1.0, r.getDouble(0), 1e-15);
    DenseVector v = r.getAs(1);
    Assert.assertEquals(Vectors.dense(1.0, 0.0, 2.0, 0.0, 3.0, 0.0), v);
  }
}
