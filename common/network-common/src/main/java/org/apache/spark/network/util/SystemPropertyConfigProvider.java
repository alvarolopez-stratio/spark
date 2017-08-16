/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.network.util;

import java.util.NoSuchElementException;

/** Uses System properties to obtain config values. */
public class SystemPropertyConfigProvider extends ConfigProvider {
  @Override
  public String get(String name) {
    String value = System.getProperty(name);
    if (value == null) {
      throw new NoSuchElementException(name);
    }
    return value;
  }
}
