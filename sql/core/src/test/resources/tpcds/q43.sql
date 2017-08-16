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
  s_store_name,
  s_store_id,
  sum(CASE WHEN (d_day_name = 'Sunday')
    THEN ss_sales_price
      ELSE NULL END) sun_sales,
  sum(CASE WHEN (d_day_name = 'Monday')
    THEN ss_sales_price
      ELSE NULL END) mon_sales,
  sum(CASE WHEN (d_day_name = 'Tuesday')
    THEN ss_sales_price
      ELSE NULL END) tue_sales,
  sum(CASE WHEN (d_day_name = 'Wednesday')
    THEN ss_sales_price
      ELSE NULL END) wed_sales,
  sum(CASE WHEN (d_day_name = 'Thursday')
    THEN ss_sales_price
      ELSE NULL END) thu_sales,
  sum(CASE WHEN (d_day_name = 'Friday')
    THEN ss_sales_price
      ELSE NULL END) fri_sales,
  sum(CASE WHEN (d_day_name = 'Saturday')
    THEN ss_sales_price
      ELSE NULL END) sat_sales
FROM date_dim, store_sales, store
WHERE d_date_sk = ss_sold_date_sk AND
  s_store_sk = ss_store_sk AND
  s_gmt_offset = -5 AND
  d_year = 2000
GROUP BY s_store_name, s_store_id
ORDER BY s_store_name, s_store_id, sun_sales, mon_sales, tue_sales, wed_sales,
  thu_sales, fri_sales, sat_sales
LIMIT 100
