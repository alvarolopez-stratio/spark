/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.sql.internal

case class HiveSerDe(
  inputFormat: Option[String] = None,
  outputFormat: Option[String] = None,
  serde: Option[String] = None)

object HiveSerDe {
  /**
   * Get the Hive SerDe information from the data source abbreviation string or classname.
   *
   * @param source Currently the source abbreviation can be one of the following:
   *               SequenceFile, RCFile, ORC, PARQUET, and case insensitive.
   * @return HiveSerDe associated with the specified source
   */
  def sourceToSerDe(source: String): Option[HiveSerDe] = {
    val serdeMap = Map(
      "sequencefile" ->
        HiveSerDe(
          inputFormat = Option("org.apache.hadoop.mapred.SequenceFileInputFormat"),
          outputFormat = Option("org.apache.hadoop.mapred.SequenceFileOutputFormat")),

      "rcfile" ->
        HiveSerDe(
          inputFormat = Option("org.apache.hadoop.hive.ql.io.RCFileInputFormat"),
          outputFormat = Option("org.apache.hadoop.hive.ql.io.RCFileOutputFormat"),
          serde = Option("org.apache.hadoop.hive.serde2.columnar.LazyBinaryColumnarSerDe")),

      "orc" ->
        HiveSerDe(
          inputFormat = Option("org.apache.hadoop.hive.ql.io.orc.OrcInputFormat"),
          outputFormat = Option("org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat"),
          serde = Option("org.apache.hadoop.hive.ql.io.orc.OrcSerde")),

      "parquet" ->
        HiveSerDe(
          inputFormat = Option("org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat"),
          outputFormat = Option("org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat"),
          serde = Option("org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe")),

      "textfile" ->
        HiveSerDe(
          inputFormat = Option("org.apache.hadoop.mapred.TextInputFormat"),
          outputFormat = Option("org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat")),

      "avro" ->
        HiveSerDe(
          inputFormat = Option("org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat"),
          outputFormat = Option("org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat"),
          serde = Option("org.apache.hadoop.hive.serde2.avro.AvroSerDe")))

    val key = source.toLowerCase match {
      case s if s.startsWith("org.apache.spark.sql.parquet") => "parquet"
      case s if s.startsWith("org.apache.spark.sql.orc") => "orc"
      case s if s.equals("orcfile") => "orc"
      case s if s.equals("parquetfile") => "parquet"
      case s if s.equals("avrofile") => "avro"
      case s => s
    }

    serdeMap.get(key)
  }
}
