/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.sql.execution.vectorized;

import java.util.Arrays;

import com.google.common.annotations.VisibleForTesting;

import org.apache.spark.memory.MemoryMode;
import org.apache.spark.sql.types.StructType;

import static org.apache.spark.sql.types.DataTypes.LongType;

/**
 * This is an illustrative implementation of an append-only single-key/single value aggregate hash
 * map that can act as a 'cache' for extremely fast key-value lookups while evaluating aggregates
 * (and fall back to the `BytesToBytesMap` if a given key isn't found). This can be potentially
 * 'codegened' in HashAggregate to speed up aggregates w/ key.
 *
 * It is backed by a power-of-2-sized array for index lookups and a columnar batch that stores the
 * key-value pairs. The index lookups in the array rely on linear probing (with a small number of
 * maximum tries) and use an inexpensive hash function which makes it really efficient for a
 * majority of lookups. However, using linear probing and an inexpensive hash function also makes it
 * less robust as compared to the `BytesToBytesMap` (especially for a large number of keys or even
 * for certain distribution of keys) and requires us to fall back on the latter for correctness.
 */
public class AggregateHashMap {

  private ColumnarBatch batch;
  private int[] buckets;
  private int numBuckets;
  private int numRows = 0;
  private int maxSteps = 3;

  private static int DEFAULT_CAPACITY = 1 << 16;
  private static double DEFAULT_LOAD_FACTOR = 0.25;
  private static int DEFAULT_MAX_STEPS = 3;

  public AggregateHashMap(StructType schema, int capacity, double loadFactor, int maxSteps) {

    // We currently only support single key-value pair that are both longs
    assert (schema.size() == 2 && schema.fields()[0].dataType() == LongType &&
        schema.fields()[1].dataType() == LongType);

    // capacity should be a power of 2
    assert (capacity > 0 && ((capacity & (capacity - 1)) == 0));

    this.maxSteps = maxSteps;
    numBuckets = (int) (capacity / loadFactor);
    batch = ColumnarBatch.allocate(schema, MemoryMode.ON_HEAP, capacity);
    buckets = new int[numBuckets];
    Arrays.fill(buckets, -1);
  }

  public AggregateHashMap(StructType schema) {
    this(schema, DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_MAX_STEPS);
  }

  public ColumnarBatch.Row findOrInsert(long key) {
    int idx = find(key);
    if (idx != -1 && buckets[idx] == -1) {
      batch.column(0).putLong(numRows, key);
      batch.column(1).putLong(numRows, 0);
      buckets[idx] = numRows++;
    }
    return batch.getRow(buckets[idx]);
  }

  @VisibleForTesting
  public int find(long key) {
    long h = hash(key);
    int step = 0;
    int idx = (int) h & (numBuckets - 1);
    while (step < maxSteps) {
      // Return bucket index if it's either an empty slot or already contains the key
      if (buckets[idx] == -1) {
        return idx;
      } else if (equals(idx, key)) {
        return idx;
      }
      idx = (idx + 1) & (numBuckets - 1);
      step++;
    }
    // Didn't find it
    return -1;
  }

  private long hash(long key) {
    return key;
  }

  private boolean equals(int idx, long key1) {
    return batch.column(0).getLong(buckets[idx]) == key1;
  }
}
