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

import java.nio.charset.StandardCharsets

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.CatalystTypeConverters
import org.apache.spark.sql.catalyst.util.DateTimeUtils
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.CalendarInterval


class LiteralExpressionSuite extends SparkFunSuite with ExpressionEvalHelper {

  test("null") {
    checkEvaluation(Literal.create(null, BooleanType), null)
    checkEvaluation(Literal.create(null, ByteType), null)
    checkEvaluation(Literal.create(null, ShortType), null)
    checkEvaluation(Literal.create(null, IntegerType), null)
    checkEvaluation(Literal.create(null, LongType), null)
    checkEvaluation(Literal.create(null, FloatType), null)
    checkEvaluation(Literal.create(null, DoubleType), null)
    checkEvaluation(Literal.create(null, StringType), null)
    checkEvaluation(Literal.create(null, BinaryType), null)
    checkEvaluation(Literal.create(null, DecimalType.USER_DEFAULT), null)
    checkEvaluation(Literal.create(null, DateType), null)
    checkEvaluation(Literal.create(null, TimestampType), null)
    checkEvaluation(Literal.create(null, CalendarIntervalType), null)
    checkEvaluation(Literal.create(null, ArrayType(ByteType, true)), null)
    checkEvaluation(Literal.create(null, ArrayType(StringType, true)), null)
    checkEvaluation(Literal.create(null, MapType(StringType, IntegerType)), null)
    checkEvaluation(Literal.create(null, StructType(Seq.empty)), null)
  }

  test("default") {
    checkEvaluation(Literal.default(BooleanType), false)
    checkEvaluation(Literal.default(ByteType), 0.toByte)
    checkEvaluation(Literal.default(ShortType), 0.toShort)
    checkEvaluation(Literal.default(IntegerType), 0)
    checkEvaluation(Literal.default(LongType), 0L)
    checkEvaluation(Literal.default(FloatType), 0.0f)
    checkEvaluation(Literal.default(DoubleType), 0.0)
    checkEvaluation(Literal.default(StringType), "")
    checkEvaluation(Literal.default(BinaryType), "".getBytes(StandardCharsets.UTF_8))
    checkEvaluation(Literal.default(DecimalType.USER_DEFAULT), Decimal(0))
    checkEvaluation(Literal.default(DecimalType.SYSTEM_DEFAULT), Decimal(0))
    checkEvaluation(Literal.default(DateType), DateTimeUtils.toJavaDate(0))
    checkEvaluation(Literal.default(TimestampType), DateTimeUtils.toJavaTimestamp(0L))
    checkEvaluation(Literal.default(CalendarIntervalType), new CalendarInterval(0, 0L))
    checkEvaluation(Literal.default(ArrayType(StringType)), Array())
    checkEvaluation(Literal.default(MapType(IntegerType, StringType)), Map())
    checkEvaluation(Literal.default(StructType(StructField("a", StringType) :: Nil)), Row(""))
  }

  test("boolean literals") {
    checkEvaluation(Literal(true), true)
    checkEvaluation(Literal(false), false)
  }

  test("int literals") {
    List(0, 1, Int.MinValue, Int.MaxValue).foreach { d =>
      checkEvaluation(Literal(d), d)
      checkEvaluation(Literal(d.toLong), d.toLong)
      checkEvaluation(Literal(d.toShort), d.toShort)
      checkEvaluation(Literal(d.toByte), d.toByte)
    }
    checkEvaluation(Literal(Long.MinValue), Long.MinValue)
    checkEvaluation(Literal(Long.MaxValue), Long.MaxValue)
  }

  test("double literals") {
    List(0.0, -0.0, Double.NegativeInfinity, Double.PositiveInfinity).foreach { d =>
      checkEvaluation(Literal(d), d)
      checkEvaluation(Literal(d.toFloat), d.toFloat)
    }
    checkEvaluation(Literal(Double.MinValue), Double.MinValue)
    checkEvaluation(Literal(Double.MaxValue), Double.MaxValue)
    checkEvaluation(Literal(Float.MinValue), Float.MinValue)
    checkEvaluation(Literal(Float.MaxValue), Float.MaxValue)

  }

  test("string literals") {
    checkEvaluation(Literal(""), "")
    checkEvaluation(Literal("test"), "test")
    checkEvaluation(Literal("\u0000"), "\u0000")
  }

  test("sum two literals") {
    checkEvaluation(Add(Literal(1), Literal(1)), 2)
  }

  test("binary literals") {
    checkEvaluation(Literal.create(new Array[Byte](0), BinaryType), new Array[Byte](0))
    checkEvaluation(Literal.create(new Array[Byte](2), BinaryType), new Array[Byte](2))
  }

  test("decimal") {
    List(-0.0001, 0.0, 0.001, 1.2, 1.1111, 5).foreach { d =>
      checkEvaluation(Literal(Decimal(d)), Decimal(d))
      checkEvaluation(Literal(Decimal(d.toInt)), Decimal(d.toInt))
      checkEvaluation(Literal(Decimal(d.toLong)), Decimal(d.toLong))
      checkEvaluation(Literal(Decimal((d * 1000L).toLong, 10, 3)),
        Decimal((d * 1000L).toLong, 10, 3))
      checkEvaluation(Literal(BigDecimal(d.toString)), Decimal(d))
      checkEvaluation(Literal(new java.math.BigDecimal(d.toString)), Decimal(d))
    }
  }

  test("array") {
    def checkArrayLiteral(a: Array[_], elementType: DataType): Unit = {
      val toCatalyst = (a: Array[_], elementType: DataType) => {
        CatalystTypeConverters.createToCatalystConverter(ArrayType(elementType))(a)
      }
      checkEvaluation(Literal(a), toCatalyst(a, elementType))
    }
    checkArrayLiteral(Array(1, 2, 3), IntegerType)
    checkArrayLiteral(Array("a", "b", "c"), StringType)
    checkArrayLiteral(Array(1.0, 4.0), DoubleType)
    checkArrayLiteral(Array(CalendarInterval.MICROS_PER_DAY, CalendarInterval.MICROS_PER_HOUR),
      CalendarIntervalType)
  }

  test("unsupported types (map and struct) in literals") {
    def checkUnsupportedTypeInLiteral(v: Any): Unit = {
      val errMsgMap = intercept[RuntimeException] {
        Literal(v)
      }
      assert(errMsgMap.getMessage.startsWith("Unsupported literal type"))
    }
    checkUnsupportedTypeInLiteral(Map("key1" -> 1, "key2" -> 2))
    checkUnsupportedTypeInLiteral(("mike", 29, 1.0))
  }
}
