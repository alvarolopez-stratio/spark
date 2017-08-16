/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.network.shuffle.protocol;

import com.google.common.base.Objects;
import io.netty.buffer.ByteBuf;

// Needed by ScalaDoc. See SPARK-7726
import static org.apache.spark.network.shuffle.protocol.BlockTransferMessage.Type;

/**
 * Identifier for a fixed number of chunks to read from a stream created by an "open blocks"
 * message. This is used by {@link org.apache.spark.network.shuffle.OneForOneBlockFetcher}.
 */
public class StreamHandle extends BlockTransferMessage {
  public final long streamId;
  public final int numChunks;

  public StreamHandle(long streamId, int numChunks) {
    this.streamId = streamId;
    this.numChunks = numChunks;
  }

  @Override
  protected Type type() { return Type.STREAM_HANDLE; }

  @Override
  public int hashCode() {
    return Objects.hashCode(streamId, numChunks);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("streamId", streamId)
      .add("numChunks", numChunks)
      .toString();
  }

  @Override
  public boolean equals(Object other) {
    if (other != null && other instanceof StreamHandle) {
      StreamHandle o = (StreamHandle) other;
      return Objects.equal(streamId, o.streamId)
        && Objects.equal(numChunks, o.numChunks);
    }
    return false;
  }

  @Override
  public int encodedLength() {
    return 8 + 4;
  }

  @Override
  public void encode(ByteBuf buf) {
    buf.writeLong(streamId);
    buf.writeInt(numChunks);
  }

  public static StreamHandle decode(ByteBuf buf) {
    long streamId = buf.readLong();
    int numChunks = buf.readInt();
    return new StreamHandle(streamId, numChunks);
  }
}
