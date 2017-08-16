--
-- © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
--
-- This software is a modification of the original software Apache Spark licensed under the Apache 2.0
-- license, a copy of which is below. This software contains proprietary information of
-- Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
-- otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
-- without express written authorization from Stratio Big Data Inc., Sucursal en España.
--

-- This file is automatically generated by LogicalPlanToSQLSuite.
SELECT CASE id WHEN 0 THEN 'foo' WHEN 1 THEN 'bar' ELSE 'baz' END FROM parquet_t0
--------------------------------------------------------------------------------
SELECT `gen_attr_0` AS `CASE WHEN (id = CAST(0 AS BIGINT)) THEN foo WHEN (id = CAST(1 AS BIGINT)) THEN bar ELSE baz END` FROM (SELECT CASE WHEN (`gen_attr_1` = CAST(0 AS BIGINT)) THEN 'foo' WHEN (`gen_attr_1` = CAST(1 AS BIGINT)) THEN 'bar' ELSE 'baz' END AS `gen_attr_0` FROM (SELECT `id` AS `gen_attr_1` FROM `default`.`parquet_t0`) AS gen_subquery_0) AS gen_subquery_1
