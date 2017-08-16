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
SELECT MAX(key) OVER (PARTITION BY key % 3) + key FROM parquet_t1
--------------------------------------------------------------------------------
SELECT `gen_attr_0` AS `(max(key) OVER (PARTITION BY (key % CAST(3 AS BIGINT)) ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) + key)` FROM (SELECT (`gen_attr_1` + `gen_attr_2`) AS `gen_attr_0` FROM (SELECT gen_subquery_1.`gen_attr_2`, gen_subquery_1.`gen_attr_3`, max(`gen_attr_2`) OVER (PARTITION BY `gen_attr_3` ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS `gen_attr_1` FROM (SELECT `gen_attr_2`, (`gen_attr_2` % CAST(3 AS BIGINT)) AS `gen_attr_3` FROM (SELECT `key` AS `gen_attr_2`, `value` AS `gen_attr_4` FROM `default`.`parquet_t1`) AS gen_subquery_0) AS gen_subquery_1) AS gen_subquery_2) AS gen_subquery_3
