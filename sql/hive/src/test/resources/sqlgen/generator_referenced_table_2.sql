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
SELECT EXPLODE(arr) AS val FROM parquet_t3
--------------------------------------------------------------------------------
SELECT `gen_attr_0` AS `val` FROM (SELECT `gen_attr_0` FROM (SELECT `arr` AS `gen_attr_1`, `arr2` AS `gen_attr_2`, `json` AS `gen_attr_3`, `id` AS `gen_attr_4` FROM `default`.`parquet_t3`) AS gen_subquery_0 LATERAL VIEW explode(`gen_attr_1`) gen_subquery_2 AS `gen_attr_0`) AS gen_subquery_1
