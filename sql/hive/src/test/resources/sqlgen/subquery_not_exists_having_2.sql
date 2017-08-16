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
select *
from src b
group by key, value
having not exists (select distinct a.key
                   from src a
                   where b.value = a.value and a.value > 'val_12')
--------------------------------------------------------------------------------
SELECT `gen_attr_2` AS `key`, `gen_attr_0` AS `value` FROM (SELECT `gen_attr_2`, `gen_attr_0` FROM (SELECT `key` AS `gen_attr_2`, `value` AS `gen_attr_0` FROM `default`.`src`) AS gen_subquery_0 GROUP BY `gen_attr_2`, `gen_attr_0` HAVING (NOT EXISTS(SELECT `gen_attr_3` AS `1` FROM (SELECT 1 AS `gen_attr_3` FROM (SELECT DISTINCT `gen_attr_4`, `gen_attr_1` FROM (SELECT `key` AS `gen_attr_4`, `value` AS `gen_attr_1` FROM `default`.`src`) AS gen_subquery_2 WHERE (`gen_attr_1` > 'val_12')) AS gen_subquery_1 WHERE (`gen_attr_0` = `gen_attr_1`)) AS gen_subquery_3))) AS b
