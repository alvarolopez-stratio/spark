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
SELECT a, b, sum(c) FROM parquet_t2 GROUP BY a, b GROUPING SETS (a) ORDER BY a, b
--------------------------------------------------------------------------------
SELECT `gen_attr_0` AS `a`, `gen_attr_1` AS `b`, `gen_attr_3` AS `sum(c)` FROM (SELECT `gen_attr_5` AS `gen_attr_0`, `gen_attr_6` AS `gen_attr_1`, sum(`gen_attr_4`) AS `gen_attr_3` FROM (SELECT `a` AS `gen_attr_5`, `b` AS `gen_attr_6`, `c` AS `gen_attr_4`, `d` AS `gen_attr_7` FROM `default`.`parquet_t2`) AS gen_subquery_0 GROUP BY `gen_attr_5`, `gen_attr_6` GROUPING SETS((`gen_attr_5`)) ORDER BY `gen_attr_0` ASC NULLS FIRST, `gen_attr_1` ASC NULLS FIRST) AS gen_subquery_1
