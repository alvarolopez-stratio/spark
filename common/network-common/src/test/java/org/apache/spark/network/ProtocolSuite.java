/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.network;

import java.util.List;

import com.google.common.primitives.Ints;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.FileRegion;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import org.apache.spark.network.protocol.ChunkFetchFailure;
import org.apache.spark.network.protocol.ChunkFetchRequest;
import org.apache.spark.network.protocol.ChunkFetchSuccess;
import org.apache.spark.network.protocol.Message;
import org.apache.spark.network.protocol.MessageDecoder;
import org.apache.spark.network.protocol.MessageEncoder;
import org.apache.spark.network.protocol.OneWayMessage;
import org.apache.spark.network.protocol.RpcFailure;
import org.apache.spark.network.protocol.RpcRequest;
import org.apache.spark.network.protocol.RpcResponse;
import org.apache.spark.network.protocol.StreamChunkId;
import org.apache.spark.network.protocol.StreamFailure;
import org.apache.spark.network.protocol.StreamRequest;
import org.apache.spark.network.protocol.StreamResponse;
import org.apache.spark.network.util.ByteArrayWritableChannel;
import org.apache.spark.network.util.NettyUtils;

public class ProtocolSuite {
  private void testServerToClient(Message msg) {
    EmbeddedChannel serverChannel = new EmbeddedChannel(new FileRegionEncoder(),
      new MessageEncoder());
    serverChannel.writeOutbound(msg);

    EmbeddedChannel clientChannel = new EmbeddedChannel(
        NettyUtils.createFrameDecoder(), new MessageDecoder());

    while (!serverChannel.outboundMessages().isEmpty()) {
      clientChannel.writeInbound(serverChannel.readOutbound());
    }

    assertEquals(1, clientChannel.inboundMessages().size());
    assertEquals(msg, clientChannel.readInbound());
  }

  private void testClientToServer(Message msg) {
    EmbeddedChannel clientChannel = new EmbeddedChannel(new FileRegionEncoder(),
      new MessageEncoder());
    clientChannel.writeOutbound(msg);

    EmbeddedChannel serverChannel = new EmbeddedChannel(
        NettyUtils.createFrameDecoder(), new MessageDecoder());

    while (!clientChannel.outboundMessages().isEmpty()) {
      serverChannel.writeInbound(clientChannel.readOutbound());
    }

    assertEquals(1, serverChannel.inboundMessages().size());
    assertEquals(msg, serverChannel.readInbound());
  }

  @Test
  public void requests() {
    testClientToServer(new ChunkFetchRequest(new StreamChunkId(1, 2)));
    testClientToServer(new RpcRequest(12345, new TestManagedBuffer(0)));
    testClientToServer(new RpcRequest(12345, new TestManagedBuffer(10)));
    testClientToServer(new StreamRequest("abcde"));
    testClientToServer(new OneWayMessage(new TestManagedBuffer(10)));
  }

  @Test
  public void responses() {
    testServerToClient(new ChunkFetchSuccess(new StreamChunkId(1, 2), new TestManagedBuffer(10)));
    testServerToClient(new ChunkFetchSuccess(new StreamChunkId(1, 2), new TestManagedBuffer(0)));
    testServerToClient(new ChunkFetchFailure(new StreamChunkId(1, 2), "this is an error"));
    testServerToClient(new ChunkFetchFailure(new StreamChunkId(1, 2), ""));
    testServerToClient(new RpcResponse(12345, new TestManagedBuffer(0)));
    testServerToClient(new RpcResponse(12345, new TestManagedBuffer(100)));
    testServerToClient(new RpcFailure(0, "this is an error"));
    testServerToClient(new RpcFailure(0, ""));
    // Note: buffer size must be "0" since StreamResponse's buffer is written differently to the
    // channel and cannot be tested like this.
    testServerToClient(new StreamResponse("anId", 12345L, new TestManagedBuffer(0)));
    testServerToClient(new StreamFailure("anId", "this is an error"));
  }

  /**
   * Handler to transform a FileRegion into a byte buffer. EmbeddedChannel doesn't actually transfer
   * bytes, but messages, so this is needed so that the frame decoder on the receiving side can
   * understand what MessageWithHeader actually contains.
   */
  private static class FileRegionEncoder extends MessageToMessageEncoder<FileRegion> {

    @Override
    public void encode(ChannelHandlerContext ctx, FileRegion in, List<Object> out)
      throws Exception {

      ByteArrayWritableChannel channel = new ByteArrayWritableChannel(Ints.checkedCast(in.count()));
      while (in.transfered() < in.count()) {
        in.transferTo(channel, in.transfered());
      }
      out.add(Unpooled.wrappedBuffer(channel.getData()));
    }

  }

}
