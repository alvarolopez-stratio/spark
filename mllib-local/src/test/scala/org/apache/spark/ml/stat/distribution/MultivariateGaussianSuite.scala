/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.ml.stat.distribution

import org.apache.spark.ml.SparkMLFunSuite
import org.apache.spark.ml.linalg.{Matrices, Vectors}
import org.apache.spark.ml.util.TestingUtils._


class MultivariateGaussianSuite extends SparkMLFunSuite {

  test("univariate") {
    val x1 = Vectors.dense(0.0)
    val x2 = Vectors.dense(1.5)

    val mu = Vectors.dense(0.0)
    val sigma1 = Matrices.dense(1, 1, Array(1.0))
    val dist1 = new MultivariateGaussian(mu, sigma1)
    assert(dist1.pdf(x1) ~== 0.39894 absTol 1E-5)
    assert(dist1.pdf(x2) ~== 0.12952 absTol 1E-5)

    val sigma2 = Matrices.dense(1, 1, Array(4.0))
    val dist2 = new MultivariateGaussian(mu, sigma2)
    assert(dist2.pdf(x1) ~== 0.19947 absTol 1E-5)
    assert(dist2.pdf(x2) ~== 0.15057 absTol 1E-5)
  }

  test("multivariate") {
    val x1 = Vectors.dense(0.0, 0.0)
    val x2 = Vectors.dense(1.0, 1.0)

    val mu = Vectors.dense(0.0, 0.0)
    val sigma1 = Matrices.dense(2, 2, Array(1.0, 0.0, 0.0, 1.0))
    val dist1 = new MultivariateGaussian(mu, sigma1)
    assert(dist1.pdf(x1) ~== 0.15915 absTol 1E-5)
    assert(dist1.pdf(x2) ~== 0.05855 absTol 1E-5)

    val sigma2 = Matrices.dense(2, 2, Array(4.0, -1.0, -1.0, 2.0))
    val dist2 = new MultivariateGaussian(mu, sigma2)
    assert(dist2.pdf(x1) ~== 0.060155 absTol 1E-5)
    assert(dist2.pdf(x2) ~== 0.033971 absTol 1E-5)
  }

  test("multivariate degenerate") {
    val x1 = Vectors.dense(0.0, 0.0)
    val x2 = Vectors.dense(1.0, 1.0)

    val mu = Vectors.dense(0.0, 0.0)
    val sigma = Matrices.dense(2, 2, Array(1.0, 1.0, 1.0, 1.0))
    val dist = new MultivariateGaussian(mu, sigma)
    assert(dist.pdf(x1) ~== 0.11254 absTol 1E-5)
    assert(dist.pdf(x2) ~== 0.068259 absTol 1E-5)
  }

  test("SPARK-11302") {
    val x = Vectors.dense(629, 640, 1.7188, 618.19)
    val mu = Vectors.dense(
      1055.3910505836575, 1070.489299610895, 1.39020554474708, 1040.5907503867697)
    val sigma = Matrices.dense(4, 4, Array(
      166769.00466698944, 169336.6705268059, 12.820670788921873, 164243.93314092053,
      169336.6705268059, 172041.5670061245, 21.62590020524533, 166678.01075856484,
      12.820670788921873, 21.62590020524533, 0.872524191943962, 4.283255814732373,
      164243.93314092053, 166678.01075856484, 4.283255814732373, 161848.9196719207))
    val dist = new MultivariateGaussian(mu, sigma)
    // Agrees with R's dmvnorm: 7.154782e-05
    assert(dist.pdf(x) ~== 7.154782224045512E-5 absTol 1E-9)
  }
}
