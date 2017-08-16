/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.storage

import scala.collection.mutable
import scala.util.Random

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.internal.Logging

/**
 * ::DeveloperApi::
 * BlockReplicationPrioritization provides logic for prioritizing a sequence of peers for
 * replicating blocks. BlockManager will replicate to each peer returned in order until the
 * desired replication order is reached. If a replication fails, prioritize() will be called
 * again to get a fresh prioritization.
 */
@DeveloperApi
trait BlockReplicationPolicy {

  /**
   * Method to prioritize a bunch of candidate peers of a block
   *
   * @param blockManagerId Id of the current BlockManager for self identification
   * @param peers A list of peers of a BlockManager
   * @param peersReplicatedTo Set of peers already replicated to
   * @param blockId BlockId of the block being replicated. This can be used as a source of
   *                randomness if needed.
   * @param numReplicas Number of peers we need to replicate to
   * @return A prioritized list of peers. Lower the index of a peer, higher its priority.
   *         This returns a list of size at most `numPeersToReplicateTo`.
   */
  def prioritize(
      blockManagerId: BlockManagerId,
      peers: Seq[BlockManagerId],
      peersReplicatedTo: mutable.HashSet[BlockManagerId],
      blockId: BlockId,
      numReplicas: Int): List[BlockManagerId]
}

@DeveloperApi
class RandomBlockReplicationPolicy
  extends BlockReplicationPolicy
  with Logging {

  /**
   * Method to prioritize a bunch of candidate peers of a block. This is a basic implementation,
   * that just makes sure we put blocks on different hosts, if possible
   *
   * @param blockManagerId Id of the current BlockManager for self identification
   * @param peers A list of peers of a BlockManager
   * @param peersReplicatedTo Set of peers already replicated to
   * @param blockId BlockId of the block being replicated. This can be used as a source of
   *                randomness if needed.
   * @return A prioritized list of peers. Lower the index of a peer, higher its priority
   */
  override def prioritize(
      blockManagerId: BlockManagerId,
      peers: Seq[BlockManagerId],
      peersReplicatedTo: mutable.HashSet[BlockManagerId],
      blockId: BlockId,
      numReplicas: Int): List[BlockManagerId] = {
    val random = new Random(blockId.hashCode)
    logDebug(s"Input peers : ${peers.mkString(", ")}")
    val prioritizedPeers = if (peers.size > numReplicas) {
      getSampleIds(peers.size, numReplicas, random).map(peers(_))
    } else {
      if (peers.size < numReplicas) {
        logWarning(s"Expecting ${numReplicas} replicas with only ${peers.size} peer/s.")
      }
      random.shuffle(peers).toList
    }
    logDebug(s"Prioritized peers : ${prioritizedPeers.mkString(", ")}")
    prioritizedPeers
  }

  // scalastyle:off line.size.limit
  /**
   * Uses sampling algorithm by Robert Floyd. Finds a random sample in O(n) while
   * minimizing space usage. Please see <a href="http://math.stackexchange.com/questions/178690/whats-the-proof-of-correctness-for-robert-floyds-algorithm-for-selecting-a-sin">
   * here</a>.
   *
   * @param n total number of indices
   * @param m number of samples needed
   * @param r random number generator
   * @return list of m random unique indices
   */
  // scalastyle:on line.size.limit
  private def getSampleIds(n: Int, m: Int, r: Random): List[Int] = {
    val indices = (n - m + 1 to n).foldLeft(Set.empty[Int]) {case (set, i) =>
      val t = r.nextInt(i) + 1
      if (set.contains(t)) set + i else set + t
    }
    // we shuffle the result to ensure a random arrangement within the sample
    // to avoid any bias from set implementations
    r.shuffle(indices.map(_ - 1).toList)
  }
}
