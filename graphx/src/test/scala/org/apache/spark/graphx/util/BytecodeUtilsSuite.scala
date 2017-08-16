/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.graphx.util

import org.apache.spark.SparkFunSuite


// scalastyle:off println
class BytecodeUtilsSuite extends SparkFunSuite {

  import BytecodeUtilsSuite.TestClass

  test("closure invokes a method") {
    val c1 = {e: TestClass => println(e.foo); println(e.bar); println(e.baz); }
    assert(BytecodeUtils.invokedMethod(c1, classOf[TestClass], "foo"))
    assert(BytecodeUtils.invokedMethod(c1, classOf[TestClass], "bar"))
    assert(BytecodeUtils.invokedMethod(c1, classOf[TestClass], "baz"))

    val c2 = {e: TestClass => println(e.foo); println(e.bar); }
    assert(BytecodeUtils.invokedMethod(c2, classOf[TestClass], "foo"))
    assert(BytecodeUtils.invokedMethod(c2, classOf[TestClass], "bar"))
    assert(!BytecodeUtils.invokedMethod(c2, classOf[TestClass], "baz"))

    val c3 = {e: TestClass => println(e.foo); }
    assert(BytecodeUtils.invokedMethod(c3, classOf[TestClass], "foo"))
    assert(!BytecodeUtils.invokedMethod(c3, classOf[TestClass], "bar"))
    assert(!BytecodeUtils.invokedMethod(c3, classOf[TestClass], "baz"))
  }

  test("closure inside a closure invokes a method") {
    val c1 = {e: TestClass => println(e.foo); println(e.bar); println(e.baz); }
    val c2 = {e: TestClass => c1(e); println(e.foo); }
    assert(BytecodeUtils.invokedMethod(c2, classOf[TestClass], "foo"))
    assert(BytecodeUtils.invokedMethod(c2, classOf[TestClass], "bar"))
    assert(BytecodeUtils.invokedMethod(c2, classOf[TestClass], "baz"))
  }

  test("closure inside a closure inside a closure invokes a method") {
    val c1 = {e: TestClass => println(e.baz); }
    val c2 = {e: TestClass => c1(e); println(e.foo); }
    val c3 = {e: TestClass => c2(e) }
    assert(BytecodeUtils.invokedMethod(c3, classOf[TestClass], "foo"))
    assert(!BytecodeUtils.invokedMethod(c3, classOf[TestClass], "bar"))
    assert(BytecodeUtils.invokedMethod(c3, classOf[TestClass], "baz"))
  }

  test("closure calling a function that invokes a method") {
    def zoo(e: TestClass) {
      println(e.baz)
    }
    val c1 = {e: TestClass => zoo(e)}
    assert(!BytecodeUtils.invokedMethod(c1, classOf[TestClass], "foo"))
    assert(!BytecodeUtils.invokedMethod(c1, classOf[TestClass], "bar"))
    assert(BytecodeUtils.invokedMethod(c1, classOf[TestClass], "baz"))
  }

  test("closure calling a function that invokes a method which uses another closure") {
    val c2 = {e: TestClass => println(e.baz)}
    def zoo(e: TestClass) {
      c2(e)
    }
    val c1 = {e: TestClass => zoo(e)}
    assert(!BytecodeUtils.invokedMethod(c1, classOf[TestClass], "foo"))
    assert(!BytecodeUtils.invokedMethod(c1, classOf[TestClass], "bar"))
    assert(BytecodeUtils.invokedMethod(c1, classOf[TestClass], "baz"))
  }

  test("nested closure") {
    val c2 = {e: TestClass => println(e.baz)}
    def zoo(e: TestClass, c: TestClass => Unit) {
      c(e)
    }
    val c1 = {e: TestClass => zoo(e, c2)}
    assert(!BytecodeUtils.invokedMethod(c1, classOf[TestClass], "foo"))
    assert(!BytecodeUtils.invokedMethod(c1, classOf[TestClass], "bar"))
    assert(BytecodeUtils.invokedMethod(c1, classOf[TestClass], "baz"))
  }

  // The following doesn't work yet, because the byte code doesn't contain any information
  // about what exactly "c" is.
//  test("invoke interface") {
//    val c1 = {e: TestClass => c(e)}
//    assert(!BytecodeUtils.invokedMethod(c1, classOf[TestClass], "foo"))
//    assert(!BytecodeUtils.invokedMethod(c1, classOf[TestClass], "bar"))
//    assert(BytecodeUtils.invokedMethod(c1, classOf[TestClass], "baz"))
//  }

  private val c = {e: TestClass => println(e.baz)}
}

// scalastyle:on println

object BytecodeUtilsSuite {
  class TestClass(val foo: Int, val bar: Long) {
    def baz: Boolean = false
  }
}
