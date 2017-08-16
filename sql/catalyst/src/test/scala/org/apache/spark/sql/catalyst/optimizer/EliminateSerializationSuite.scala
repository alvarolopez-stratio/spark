/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.sql.catalyst.optimizer

import scala.reflect.runtime.universe.TypeTag

import org.apache.spark.sql.catalyst.dsl.expressions._
import org.apache.spark.sql.catalyst.dsl.plans._
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.plans.PlanTest
import org.apache.spark.sql.catalyst.rules.RuleExecutor

case class OtherTuple(_1: Int, _2: Int)

class EliminateSerializationSuite extends PlanTest {
  private object Optimize extends RuleExecutor[LogicalPlan] {
    val batches =
      Batch("Serialization", FixedPoint(100),
        EliminateSerialization) :: Nil
  }

  implicit private def productEncoder[T <: Product : TypeTag] = ExpressionEncoder[T]()
  implicit private def intEncoder = ExpressionEncoder[Int]()

  test("back to back serialization") {
    val input = LocalRelation('obj.obj(classOf[(Int, Int)]))
    val plan = input.serialize[(Int, Int)].deserialize[(Int, Int)].analyze
    val optimized = Optimize.execute(plan)
    val expected = input.select('obj.as("obj")).analyze
    comparePlans(optimized, expected)
  }

  test("back to back serialization with object change") {
    val input = LocalRelation('obj.obj(classOf[OtherTuple]))
    val plan = input.serialize[OtherTuple].deserialize[(Int, Int)].analyze
    val optimized = Optimize.execute(plan)
    comparePlans(optimized, plan)
  }

  test("back to back serialization in AppendColumns") {
    val input = LocalRelation('obj.obj(classOf[(Int, Int)]))
    val func = (item: (Int, Int)) => item._1
    val plan = AppendColumns(func, input.serialize[(Int, Int)]).analyze

    val optimized = Optimize.execute(plan)

    val expected = AppendColumnsWithObject(
      func.asInstanceOf[Any => Any],
      productEncoder[(Int, Int)].namedExpressions,
      intEncoder.namedExpressions,
      input).analyze

    comparePlans(optimized, expected)
  }

  test("back to back serialization in AppendColumns with object change") {
    val input = LocalRelation('obj.obj(classOf[OtherTuple]))
    val func = (item: (Int, Int)) => item._1
    val plan = AppendColumns(func, input.serialize[OtherTuple]).analyze

    val optimized = Optimize.execute(plan)
    comparePlans(optimized, plan)
  }
}
