/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.mllib.feature

import org.apache.spark.SparkFunSuite
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.MLlibTestSparkContext
import org.apache.spark.util.Utils

class ChiSqSelectorSuite extends SparkFunSuite with MLlibTestSparkContext {

  /*
   *  Contingency tables
   *  feature0 = {8.0, 0.0}
   *  class  0 1 2
   *    8.0||1|0|1|
   *    0.0||0|2|0|
   *
   *  feature1 = {7.0, 9.0}
   *  class  0 1 2
   *    7.0||1|0|0|
   *    9.0||0|2|1|
   *
   *  feature2 = {0.0, 6.0, 8.0, 5.0}
   *  class  0 1 2
   *    0.0||1|0|0|
   *    6.0||0|1|0|
   *    8.0||0|1|0|
   *    5.0||0|0|1|
   *
   *  Use chi-squared calculator from Internet
   */

  test("ChiSqSelector transform test (sparse & dense vector)") {
    val labeledDiscreteData = sc.parallelize(
      Seq(LabeledPoint(0.0, Vectors.sparse(3, Array((0, 8.0), (1, 7.0)))),
        LabeledPoint(1.0, Vectors.sparse(3, Array((1, 9.0), (2, 6.0)))),
        LabeledPoint(1.0, Vectors.dense(Array(0.0, 9.0, 8.0))),
        LabeledPoint(2.0, Vectors.dense(Array(8.0, 9.0, 5.0)))), 2)
    val preFilteredData =
      Seq(LabeledPoint(0.0, Vectors.dense(Array(8.0))),
        LabeledPoint(1.0, Vectors.dense(Array(0.0))),
        LabeledPoint(1.0, Vectors.dense(Array(0.0))),
        LabeledPoint(2.0, Vectors.dense(Array(8.0))))
    val model = new ChiSqSelector(1).fit(labeledDiscreteData)
    val filteredData = labeledDiscreteData.map { lp =>
      LabeledPoint(lp.label, model.transform(lp.features))
    }.collect().toSeq
    assert(filteredData === preFilteredData)
  }

  test("ChiSqSelector by fpr transform test (sparse & dense vector)") {
    val labeledDiscreteData = sc.parallelize(
      Seq(LabeledPoint(0.0, Vectors.sparse(4, Array((0, 8.0), (1, 7.0)))),
        LabeledPoint(1.0, Vectors.sparse(4, Array((1, 9.0), (2, 6.0), (3, 4.0)))),
        LabeledPoint(1.0, Vectors.dense(Array(0.0, 9.0, 8.0, 4.0))),
        LabeledPoint(2.0, Vectors.dense(Array(8.0, 9.0, 5.0, 9.0)))), 2)
    val preFilteredData =
      Seq(LabeledPoint(0.0, Vectors.dense(Array(0.0))),
        LabeledPoint(1.0, Vectors.dense(Array(4.0))),
        LabeledPoint(1.0, Vectors.dense(Array(4.0))),
        LabeledPoint(2.0, Vectors.dense(Array(9.0))))
    val model: ChiSqSelectorModel = new ChiSqSelector().setSelectorType("fpr")
      .setFpr(0.1).fit(labeledDiscreteData)
    val filteredData = labeledDiscreteData.map { lp =>
      LabeledPoint(lp.label, model.transform(lp.features))
    }.collect().toSeq
    assert(filteredData === preFilteredData)
  }

  test("model load / save") {
    val model = ChiSqSelectorSuite.createModel()
    val tempDir = Utils.createTempDir()
    val path = tempDir.toURI.toString
    try {
      model.save(sc, path)
      val sameModel = ChiSqSelectorModel.load(sc, path)
      ChiSqSelectorSuite.checkEqual(model, sameModel)
    } finally {
      Utils.deleteRecursively(tempDir)
    }
  }
}

object ChiSqSelectorSuite extends SparkFunSuite {

  def createModel(): ChiSqSelectorModel = {
    val arr = Array(1, 2, 3, 4)
    new ChiSqSelectorModel(arr)
  }

  def checkEqual(a: ChiSqSelectorModel, b: ChiSqSelectorModel): Unit = {
    assert(a.selectedFeatures.deep == b.selectedFeatures.deep)
  }
}
