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
SELECT COUNT(value) FROM parquet_t1 GROUP BY key ORDER BY key, MAX(key)
--------------------------------------------------------------------------------
SELECT `gen_attr_0` AS `count(value)` FROM (SELECT `gen_attr_0` FROM (SELECT count(`gen_attr_4`) AS `gen_attr_0`, `gen_attr_3` AS `gen_attr_1`, max(`gen_attr_3`) AS `gen_attr_2` FROM (SELECT `key` AS `gen_attr_3`, `value` AS `gen_attr_4` FROM `default`.`parquet_t1`) AS gen_subquery_0 GROUP BY `gen_attr_3` ORDER BY `gen_attr_1` ASC NULLS FIRST, `gen_attr_2` ASC NULLS FIRST) AS gen_subquery_1) AS gen_subquery_2
