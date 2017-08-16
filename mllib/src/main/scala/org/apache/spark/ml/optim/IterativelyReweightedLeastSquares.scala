/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.ml.optim

import org.apache.spark.internal.Logging
import org.apache.spark.ml.feature.Instance
import org.apache.spark.ml.linalg._
import org.apache.spark.rdd.RDD

/**
 * Model fitted by [[IterativelyReweightedLeastSquares]].
 * @param coefficients model coefficients
 * @param intercept model intercept
 * @param diagInvAtWA diagonal of matrix (A^T * W * A)^-1 in the last iteration
 * @param numIterations number of iterations
 */
private[ml] class IterativelyReweightedLeastSquaresModel(
    val coefficients: DenseVector,
    val intercept: Double,
    val diagInvAtWA: DenseVector,
    val numIterations: Int) extends Serializable

/**
 * Implements the method of iteratively reweighted least squares (IRLS) which is used to solve
 * certain optimization problems by an iterative method. In each step of the iterations, it
 * involves solving a weighted least squares (WLS) problem by [[WeightedLeastSquares]].
 * It can be used to find maximum likelihood estimates of a generalized linear model (GLM),
 * find M-estimator in robust regression and other optimization problems.
 *
 * @param initialModel the initial guess model.
 * @param reweightFunc the reweight function which is used to update offsets and weights
 *                     at each iteration.
 * @param fitIntercept whether to fit intercept.
 * @param regParam L2 regularization parameter used by WLS.
 * @param maxIter maximum number of iterations.
 * @param tol the convergence tolerance.
 *
 * @see <a href="http://www.jstor.org/stable/2345503">P. J. Green, Iteratively
 * Reweighted Least Squares for Maximum Likelihood Estimation, and some Robust
 * and Resistant Alternatives, Journal of the Royal Statistical Society.
 * Series B, 1984.</a>
 */
private[ml] class IterativelyReweightedLeastSquares(
    val initialModel: WeightedLeastSquaresModel,
    val reweightFunc: (Instance, WeightedLeastSquaresModel) => (Double, Double),
    val fitIntercept: Boolean,
    val regParam: Double,
    val maxIter: Int,
    val tol: Double) extends Logging with Serializable {

  def fit(instances: RDD[Instance]): IterativelyReweightedLeastSquaresModel = {

    var converged = false
    var iter = 0

    var model: WeightedLeastSquaresModel = initialModel
    var oldModel: WeightedLeastSquaresModel = null

    while (iter < maxIter && !converged) {

      oldModel = model

      // Update offsets and weights using reweightFunc
      val newInstances = instances.map { instance =>
        val (newOffset, newWeight) = reweightFunc(instance, oldModel)
        Instance(newOffset, newWeight, instance.features)
      }

      // Estimate new model
      model = new WeightedLeastSquares(fitIntercept, regParam, elasticNetParam = 0.0,
        standardizeFeatures = false, standardizeLabel = false).fit(newInstances)

      // Check convergence
      val oldCoefficients = oldModel.coefficients
      val coefficients = model.coefficients
      BLAS.axpy(-1.0, coefficients, oldCoefficients)
      val maxTolOfCoefficients = oldCoefficients.toArray.reduce { (x, y) =>
        math.max(math.abs(x), math.abs(y))
      }
      val maxTol = math.max(maxTolOfCoefficients, math.abs(oldModel.intercept - model.intercept))

      if (maxTol < tol) {
        converged = true
        logInfo(s"IRLS converged in $iter iterations.")
      }

      logInfo(s"Iteration $iter : relative tolerance = $maxTol")
      iter = iter + 1

      if (iter == maxIter) {
        logInfo(s"IRLS reached the max number of iterations: $maxIter.")
      }

    }

    new IterativelyReweightedLeastSquaresModel(
      model.coefficients, model.intercept, model.diagInvAtWA, iter)
  }
}
