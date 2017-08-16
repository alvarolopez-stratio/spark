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
SELECT x.key, COUNT(*) FROM parquet_t1 x JOIN parquet_t1 y ON x.key = y.key group by x.key
--------------------------------------------------------------------------------
SELECT `gen_attr_0` AS `key`, `gen_attr_1` AS `count(1)` FROM (SELECT `gen_attr_0`, count(1) AS `gen_attr_1` FROM (SELECT `key` AS `gen_attr_0`, `value` AS `gen_attr_3` FROM `default`.`parquet_t1`) AS gen_subquery_0 INNER JOIN (SELECT `key` AS `gen_attr_2`, `value` AS `gen_attr_4` FROM `default`.`parquet_t1`) AS gen_subquery_1 ON (`gen_attr_0` = `gen_attr_2`) GROUP BY `gen_attr_0`) AS x
