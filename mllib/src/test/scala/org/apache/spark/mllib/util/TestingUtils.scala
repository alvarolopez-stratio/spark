/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.mllib.util

import org.scalatest.exceptions.TestFailedException

import org.apache.spark.mllib.linalg.{Matrix, Vector}

object TestingUtils {

  val ABS_TOL_MSG = " using absolute tolerance"
  val REL_TOL_MSG = " using relative tolerance"

  /**
   * Private helper function for comparing two values using relative tolerance.
   * Note that if x or y is extremely close to zero, i.e., smaller than Double.MinPositiveValue,
   * the relative tolerance is meaningless, so the exception will be raised to warn users.
   */
  private def RelativeErrorComparison(x: Double, y: Double, eps: Double): Boolean = {
    val absX = math.abs(x)
    val absY = math.abs(y)
    val diff = math.abs(x - y)
    if (x == y) {
      true
    } else if (absX < Double.MinPositiveValue || absY < Double.MinPositiveValue) {
      throw new TestFailedException(
        s"$x or $y is extremely close to zero, so the relative tolerance is meaningless.", 0)
    } else {
      diff < eps * math.min(absX, absY)
    }
  }

  /**
   * Private helper function for comparing two values using absolute tolerance.
   */
  private def AbsoluteErrorComparison(x: Double, y: Double, eps: Double): Boolean = {
    math.abs(x - y) < eps
  }

  case class CompareDoubleRightSide(
    fun: (Double, Double, Double) => Boolean, y: Double, eps: Double, method: String)

  /**
   * Implicit class for comparing two double values using relative tolerance or absolute tolerance.
   */
  implicit class DoubleWithAlmostEquals(val x: Double) {

    /**
     * When the difference of two values are within eps, returns true; otherwise, returns false.
     */
    def ~=(r: CompareDoubleRightSide): Boolean = r.fun(x, r.y, r.eps)

    /**
     * When the difference of two values are within eps, returns false; otherwise, returns true.
     */
    def !~=(r: CompareDoubleRightSide): Boolean = !r.fun(x, r.y, r.eps)

    /**
     * Throws exception when the difference of two values are NOT within eps;
     * otherwise, returns true.
     */
    def ~==(r: CompareDoubleRightSide): Boolean = {
      if (!r.fun(x, r.y, r.eps)) {
        throw new TestFailedException(
          s"Expected $x and ${r.y} to be within ${r.eps}${r.method}.", 0)
      }
      true
    }

    /**
     * Throws exception when the difference of two values are within eps; otherwise, returns true.
     */
    def !~==(r: CompareDoubleRightSide): Boolean = {
      if (r.fun(x, r.y, r.eps)) {
        throw new TestFailedException(
          s"Did not expect $x and ${r.y} to be within ${r.eps}${r.method}.", 0)
      }
      true
    }

    /**
     * Comparison using absolute tolerance.
     */
    def absTol(eps: Double): CompareDoubleRightSide =
      CompareDoubleRightSide(AbsoluteErrorComparison, x, eps, ABS_TOL_MSG)

    /**
     * Comparison using relative tolerance.
     */
    def relTol(eps: Double): CompareDoubleRightSide =
      CompareDoubleRightSide(RelativeErrorComparison, x, eps, REL_TOL_MSG)

    override def toString: String = x.toString
  }

  case class CompareVectorRightSide(
    fun: (Vector, Vector, Double) => Boolean, y: Vector, eps: Double, method: String)

  /**
   * Implicit class for comparing two vectors using relative tolerance or absolute tolerance.
   */
  implicit class VectorWithAlmostEquals(val x: Vector) {

    /**
     * When the difference of two vectors are within eps, returns true; otherwise, returns false.
     */
    def ~=(r: CompareVectorRightSide): Boolean = r.fun(x, r.y, r.eps)

    /**
     * When the difference of two vectors are within eps, returns false; otherwise, returns true.
     */
    def !~=(r: CompareVectorRightSide): Boolean = !r.fun(x, r.y, r.eps)

    /**
     * Throws exception when the difference of two vectors are NOT within eps;
     * otherwise, returns true.
     */
    def ~==(r: CompareVectorRightSide): Boolean = {
      if (!r.fun(x, r.y, r.eps)) {
        throw new TestFailedException(
          s"Expected $x and ${r.y} to be within ${r.eps}${r.method} for all elements.", 0)
      }
      true
    }

    /**
     * Throws exception when the difference of two vectors are within eps; otherwise, returns true.
     */
    def !~==(r: CompareVectorRightSide): Boolean = {
      if (r.fun(x, r.y, r.eps)) {
        throw new TestFailedException(
          s"Did not expect $x and ${r.y} to be within ${r.eps}${r.method} for all elements.", 0)
      }
      true
    }

    /**
     * Comparison using absolute tolerance.
     */
    def absTol(eps: Double): CompareVectorRightSide = CompareVectorRightSide(
      (x: Vector, y: Vector, eps: Double) => {
        x.size == y.size && x.toArray.zip(y.toArray).forall(x => x._1 ~= x._2 absTol eps)
      }, x, eps, ABS_TOL_MSG)

    /**
     * Comparison using relative tolerance. Note that comparing against sparse vector
     * with elements having value of zero will raise exception because it involves with
     * comparing against zero.
     */
    def relTol(eps: Double): CompareVectorRightSide = CompareVectorRightSide(
      (x: Vector, y: Vector, eps: Double) => {
        x.size == y.size && x.toArray.zip(y.toArray).forall(x => x._1 ~= x._2 relTol eps)
      }, x, eps, REL_TOL_MSG)

    override def toString: String = x.toString
  }

  case class CompareMatrixRightSide(
     fun: (Matrix, Matrix, Double) => Boolean, y: Matrix, eps: Double, method: String)

  /**
   * Implicit class for comparing two matrices using relative tolerance or absolute tolerance.
   */
  implicit class MatrixWithAlmostEquals(val x: Matrix) {

    /**
     * When the difference of two matrices are within eps, returns true; otherwise, returns false.
     */
    def ~=(r: CompareMatrixRightSide): Boolean = r.fun(x, r.y, r.eps)

    /**
     * When the difference of two matrices are within eps, returns false; otherwise, returns true.
     */
    def !~=(r: CompareMatrixRightSide): Boolean = !r.fun(x, r.y, r.eps)

    /**
     * Throws exception when the difference of two matrices are NOT within eps;
     * otherwise, returns true.
     */
    def ~==(r: CompareMatrixRightSide): Boolean = {
      if (!r.fun(x, r.y, r.eps)) {
        throw new TestFailedException(
          s"Expected \n$x\n and \n${r.y}\n to be within ${r.eps}${r.method} for all elements.", 0)
      }
      true
    }

    /**
     * Throws exception when the difference of two matrices are within eps; otherwise, returns true.
     */
    def !~==(r: CompareMatrixRightSide): Boolean = {
      if (r.fun(x, r.y, r.eps)) {
        throw new TestFailedException(
          s"Did not expect \n$x\n and \n${r.y}\n to be within " +
            "${r.eps}${r.method} for all elements.", 0)
      }
      true
    }

    /**
     * Comparison using absolute tolerance.
     */
    def absTol(eps: Double): CompareMatrixRightSide = CompareMatrixRightSide(
      (x: Matrix, y: Matrix, eps: Double) => {
        x.numRows == y.numRows && x.numCols == y.numCols &&
          x.toArray.zip(y.toArray).forall(x => x._1 ~= x._2 absTol eps)
      }, x, eps, ABS_TOL_MSG)

    /**
     * Comparison using relative tolerance. Note that comparing against sparse vector
     * with elements having value of zero will raise exception because it involves with
     * comparing against zero.
     */
    def relTol(eps: Double): CompareMatrixRightSide = CompareMatrixRightSide(
      (x: Matrix, y: Matrix, eps: Double) => {
        x.numRows == y.numRows && x.numCols == y.numCols &&
          x.toArray.zip(y.toArray).forall(x => x._1 ~= x._2 relTol eps)
      }, x, eps, REL_TOL_MSG)

    override def toString: String = x.toString
  }

}
