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

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.analysis.TypeCheckResult
import org.apache.spark.sql.catalyst.analysis.TypeCheckResult.{TypeCheckFailure, TypeCheckSuccess}
import org.apache.spark.sql.catalyst.expressions.codegen.{CodegenContext, ExprCode}
import org.apache.spark.sql.catalyst.expressions.objects.LambdaVariable
import org.apache.spark.sql.types.DataType

/**
 * A special expression that evaluates [[BoundReference]]s by given expressions instead of the
 * input row.
 *
 * @param result The expression that contains [[BoundReference]] and produces the final output.
 * @param children The expressions that used as input values for [[BoundReference]].
 */
case class ReferenceToExpressions(result: Expression, children: Seq[Expression])
  extends Expression {

  override def nullable: Boolean = result.nullable
  override def dataType: DataType = result.dataType

  override def checkInputDataTypes(): TypeCheckResult = {
    if (result.references.nonEmpty) {
      return TypeCheckFailure("The result expression cannot reference to any attributes.")
    }

    var maxOrdinal = -1
    result foreach {
      case b: BoundReference if b.ordinal > maxOrdinal => maxOrdinal = b.ordinal
      case _ =>
    }
    if (maxOrdinal > children.length) {
      return TypeCheckFailure(s"The result expression need $maxOrdinal input expressions, but " +
        s"there are only ${children.length} inputs.")
    }

    TypeCheckSuccess
  }

  private lazy val projection = UnsafeProjection.create(children)

  override def eval(input: InternalRow): Any = {
    result.eval(projection(input))
  }

  override protected def doGenCode(ctx: CodegenContext, ev: ExprCode): ExprCode = {
    val childrenGen = children.map(_.genCode(ctx))
    val (classChildrenVars, initClassChildrenVars) = childrenGen.zip(children).map {
      case (childGen, child) =>
        // SPARK-18125: The children vars are local variables. If the result expression uses
        // splitExpression, those variables cannot be accessed so compilation fails.
        // To fix it, we use class variables to hold those local variables.
        val classChildVarName = ctx.freshName("classChildVar")
        val classChildVarIsNull = ctx.freshName("classChildVarIsNull")
        ctx.addMutableState(ctx.javaType(child.dataType), classChildVarName, "")
        ctx.addMutableState("boolean", classChildVarIsNull, "")

        val classChildVar =
          LambdaVariable(classChildVarName, classChildVarIsNull, child.dataType)

        val initCode = s"${classChildVar.value} = ${childGen.value};\n" +
          s"${classChildVar.isNull} = ${childGen.isNull};"

        (classChildVar, initCode)
    }.unzip

    val resultGen = result.transform {
      case b: BoundReference => classChildrenVars(b.ordinal)
    }.genCode(ctx)

    ExprCode(code = childrenGen.map(_.code).mkString("\n") + initClassChildrenVars.mkString("\n") +
      resultGen.code, isNull = resultGen.isNull, value = resultGen.value)
  }
}
