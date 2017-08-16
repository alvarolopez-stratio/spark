/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.sql.types

import org.apache.spark.annotation.InterfaceStability

/**
 * The data type representing calendar time intervals. The calendar time interval is stored
 * internally in two components: number of months the number of microseconds.
 *
 * Please use the singleton `DataTypes.CalendarIntervalType`.
 *
 * @note Calendar intervals are not comparable.
 *
 * @since 1.5.0
 */
@InterfaceStability.Stable
class CalendarIntervalType private() extends DataType {

  override def defaultSize: Int = 16

  private[spark] override def asNullable: CalendarIntervalType = this
}

/**
 * @since 1.5.0
 */
@InterfaceStability.Stable
case object CalendarIntervalType extends CalendarIntervalType
