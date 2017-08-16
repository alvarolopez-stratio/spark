/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.sql.catalyst.expressions

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql.catalyst.dsl.expressions._
import org.apache.spark.sql.types.IntegerType

class ExpressionSetSuite extends SparkFunSuite {

  val aUpper = AttributeReference("A", IntegerType)(exprId = ExprId(1))
  val aLower = AttributeReference("a", IntegerType)(exprId = ExprId(1))
  val fakeA = AttributeReference("a", IntegerType)(exprId = ExprId(3))

  val bUpper = AttributeReference("B", IntegerType)(exprId = ExprId(2))
  val bLower = AttributeReference("b", IntegerType)(exprId = ExprId(2))

  val aAndBSet = AttributeSet(aUpper :: bUpper :: Nil)

  def setTest(size: Int, exprs: Expression*): Unit = {
    test(s"expect $size: ${exprs.mkString(", ")}") {
      val set = ExpressionSet(exprs)
      if (set.size != size) {
        fail(set.toDebugString)
      }
    }
  }

  def setTestIgnore(size: Int, exprs: Expression*): Unit =
    ignore(s"expect $size: ${exprs.mkString(", ")}") {}

  // Commutative
  setTest(1, aUpper + 1, aLower + 1)
  setTest(2, aUpper + 1, aLower + 2)
  setTest(2, aUpper + 1, fakeA + 1)
  setTest(2, aUpper + 1, bUpper + 1)

  setTest(1, aUpper + aLower, aLower + aUpper)
  setTest(1, aUpper + bUpper, bUpper + aUpper)
  setTest(1,
    aUpper + bUpper + 3,
    bUpper + 3 + aUpper,
    bUpper + aUpper + 3,
    Literal(3) + aUpper + bUpper)
  setTest(1,
    aUpper * bUpper * 3,
    bUpper * 3 * aUpper,
    bUpper * aUpper * 3,
    Literal(3) * aUpper * bUpper)
  setTest(1, aUpper === bUpper, bUpper === aUpper)

  setTest(1, aUpper + 1 === bUpper, bUpper === Literal(1) + aUpper)


  // Not commutative
  setTest(2, aUpper - bUpper, bUpper - aUpper)

  // Reversible
  setTest(1, aUpper > bUpper, bUpper < aUpper)
  setTest(1, aUpper >= bUpper, bUpper <= aUpper)

  // `Not` canonicalization
  setTest(1, Not(aUpper > 1), aUpper <= 1, Not(Literal(1) < aUpper), Literal(1) >= aUpper)
  setTest(1, Not(aUpper < 1), aUpper >= 1, Not(Literal(1) > aUpper), Literal(1) <= aUpper)
  setTest(1, Not(aUpper >= 1), aUpper < 1, Not(Literal(1) <= aUpper), Literal(1) > aUpper)
  setTest(1, Not(aUpper <= 1), aUpper > 1, Not(Literal(1) >= aUpper), Literal(1) < aUpper)

  // Reordering AND/OR expressions
  setTest(1, aUpper > bUpper && aUpper <= 10, aUpper <= 10 && aUpper > bUpper)
  setTest(1,
    aUpper > bUpper && bUpper > 100 && aUpper <= 10,
    bUpper > 100 && aUpper <= 10 && aUpper > bUpper)

  setTest(1, aUpper > bUpper || aUpper <= 10, aUpper <= 10 || aUpper > bUpper)
  setTest(1,
    aUpper > bUpper || bUpper > 100 || aUpper <= 10,
    bUpper > 100 || aUpper <= 10 || aUpper > bUpper)

  setTest(1,
    (aUpper <= 10 && aUpper > bUpper) || bUpper > 100,
    bUpper > 100 || (aUpper <= 10 && aUpper > bUpper))

  setTest(1,
    aUpper >= bUpper || (aUpper > 10 && bUpper < 10),
    (bUpper < 10 && aUpper > 10) || aUpper >= bUpper)

  // More complicated cases mixing AND/OR
  // Three predicates in the following:
  //   (bUpper > 100)
  //   (aUpper < 100 && bUpper <= aUpper)
  //   (aUpper >= 10 && bUpper >= 50)
  // They can be reordered and the sub-predicates contained in each of them can be reordered too.
  setTest(1,
    (bUpper > 100) || (aUpper < 100 && bUpper <= aUpper) || (aUpper >= 10 && bUpper >= 50),
    (aUpper >= 10 && bUpper >= 50) || (bUpper > 100) || (aUpper < 100 && bUpper <= aUpper),
    (bUpper >= 50 && aUpper >= 10) || (bUpper <= aUpper && aUpper < 100) || (bUpper > 100))

  // Two predicates in the following:
  //   (bUpper > 100 && aUpper < 100 && bUpper <= aUpper)
  //   (aUpper >= 10 && bUpper >= 50)
  setTest(1,
    (bUpper > 100 && aUpper < 100 && bUpper <= aUpper) || (aUpper >= 10 && bUpper >= 50),
    (aUpper >= 10 && bUpper >= 50) || (aUpper < 100 && bUpper > 100 && bUpper <= aUpper),
    (bUpper >= 50 && aUpper >= 10) || (bUpper <= aUpper && aUpper < 100 && bUpper > 100))

  // Three predicates in the following:
  //   (aUpper >= 10)
  //   (bUpper <= 10 && aUpper === bUpper && aUpper < 100)
  //   (bUpper >= 100)
  setTest(1,
    (aUpper >= 10) || (bUpper <= 10 && aUpper === bUpper && aUpper < 100) || (bUpper >= 100),
    (aUpper === bUpper && aUpper < 100 && bUpper <= 10) || (bUpper >= 100) || (aUpper >= 10),
    (aUpper < 100 && bUpper <= 10 && aUpper === bUpper) || (aUpper >= 10) || (bUpper >= 100),
    ((bUpper <= 10 && aUpper === bUpper) && aUpper < 100) || ((aUpper >= 10) || (bUpper >= 100)))

  // Don't reorder non-deterministic expression in AND/OR.
  setTest(2, Rand(1L) > aUpper && aUpper <= 10, aUpper <= 10 && Rand(1L) > aUpper)
  setTest(2,
    aUpper > bUpper && bUpper > 100 && Rand(1L) > aUpper,
    bUpper > 100 && Rand(1L) > aUpper && aUpper > bUpper)

  setTest(2, Rand(1L) > aUpper || aUpper <= 10, aUpper <= 10 || Rand(1L) > aUpper)
  setTest(2,
    aUpper > bUpper || aUpper <= Rand(1L) || aUpper <= 10,
    aUpper <= Rand(1L) || aUpper <= 10 || aUpper > bUpper)

  // Partial reorder case: we don't reorder non-deterministic expressions,
  // but we can reorder sub-expressions in deterministic AND/OR expressions.
  // There are two predicates:
  //   (aUpper > bUpper || bUpper > 100) => we can reorder sub-expressions in it.
  //   (aUpper === Rand(1L))
  setTest(1,
    (aUpper > bUpper || bUpper > 100) && aUpper === Rand(1L),
    (bUpper > 100 || aUpper > bUpper) && aUpper === Rand(1L))

  // There are three predicates:
  //   (Rand(1L) > aUpper)
  //   (aUpper <= Rand(1L) && aUpper > bUpper)
  //   (aUpper > 10 && bUpper > 10) => we can reorder sub-expressions in it.
  setTest(1,
    Rand(1L) > aUpper || (aUpper <= Rand(1L) && aUpper > bUpper) || (aUpper > 10 && bUpper > 10),
    Rand(1L) > aUpper || (aUpper <= Rand(1L) && aUpper > bUpper) || (bUpper > 10 && aUpper > 10))

  // Same predicates as above, but a negative case when we reorder non-deterministic
  // expression in (aUpper <= Rand(1L) && aUpper > bUpper).
  setTest(2,
    Rand(1L) > aUpper || (aUpper <= Rand(1L) && aUpper > bUpper) || (aUpper > 10 && bUpper > 10),
    Rand(1L) > aUpper || (aUpper > bUpper && aUpper <= Rand(1L)) || (aUpper > 10 && bUpper > 10))

  test("add to / remove from set") {
    val initialSet = ExpressionSet(aUpper + 1 :: Nil)

    assert((initialSet + (aUpper + 1)).size == 1)
    assert((initialSet + (aUpper + 2)).size == 2)
    assert((initialSet - (aUpper + 1)).size == 0)
    assert((initialSet - (aUpper + 2)).size == 1)

    assert((initialSet + (aLower + 1)).size == 1)
    assert((initialSet - (aLower + 1)).size == 0)

  }
}
