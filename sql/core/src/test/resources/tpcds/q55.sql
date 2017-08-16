--
-- © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
--
-- This software is a modification of the original software Apache Spark licensed under the Apache 2.0
-- license, a copy of which is below. This software contains proprietary information of
-- Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
-- otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
-- without express written authorization from Stratio Big Data Inc., Sucursal en España.
--

SELECT
  i_brand_id brand_id,
  i_brand brand,
  sum(ss_ext_sales_price) ext_price
FROM date_dim, store_sales, item
WHERE d_date_sk = ss_sold_date_sk
  AND ss_item_sk = i_item_sk
  AND i_manager_id = 28
  AND d_moy = 11
  AND d_year = 1999
GROUP BY i_brand, i_brand_id
ORDER BY ext_price DESC, brand_id
LIMIT 100
