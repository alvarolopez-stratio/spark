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

import org.apache.spark.sql.catalyst.dsl.expressions._
import org.apache.spark.sql.catalyst.dsl.plans._
import org.apache.spark.sql.catalyst.plans.PlanTest
import org.apache.spark.sql.catalyst.plans.logical.{LocalRelation, LogicalPlan}
import org.apache.spark.sql.catalyst.rules.RuleExecutor

class CollapseRepartitionSuite extends PlanTest {
  object Optimize extends RuleExecutor[LogicalPlan] {
    val batches =
      Batch("CollapseRepartition", FixedPoint(10),
        CollapseRepartition) :: Nil
  }

  val testRelation = LocalRelation('a.int, 'b.int)

  test("collapse two adjacent repartitions into one") {
    val query = testRelation
      .repartition(10)
      .repartition(20)

    val optimized = Optimize.execute(query.analyze)
    val correctAnswer = testRelation.repartition(20).analyze

    comparePlans(optimized, correctAnswer)
  }

  test("collapse repartition and repartitionBy into one") {
    val query = testRelation
      .repartition(10)
      .distribute('a)(20)

    val optimized = Optimize.execute(query.analyze)
    val correctAnswer = testRelation.distribute('a)(20).analyze

    comparePlans(optimized, correctAnswer)
  }

  test("collapse repartitionBy and repartition into one") {
    val query = testRelation
      .distribute('a)(20)
      .repartition(10)

    val optimized = Optimize.execute(query.analyze)
    val correctAnswer = testRelation.distribute('a)(10).analyze

    comparePlans(optimized, correctAnswer)
  }

  test("collapse two adjacent repartitionBys into one") {
    val query = testRelation
      .distribute('b)(10)
      .distribute('a)(20)

    val optimized = Optimize.execute(query.analyze)
    val correctAnswer = testRelation.distribute('a)(20).analyze

    comparePlans(optimized, correctAnswer)
  }
}
