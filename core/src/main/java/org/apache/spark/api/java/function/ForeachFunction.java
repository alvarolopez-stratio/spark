/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.api.java.function;

import java.io.Serializable;

/**
 * Base interface for a function used in Dataset's foreach function.
 *
 * Spark will invoke the call function on each element in the input Dataset.
 */
public interface ForeachFunction<T> extends Serializable {
  void call(T t) throws Exception;
}
