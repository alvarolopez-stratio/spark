/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.network.server;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.spark.network.client.RpcResponseCallback;
import org.apache.spark.network.client.TransportClient;

/**
 * Handler for sendRPC() messages sent by {@link org.apache.spark.network.client.TransportClient}s.
 */
public abstract class RpcHandler {

  private static final RpcResponseCallback ONE_WAY_CALLBACK = new OneWayRpcCallback();

  /**
   * Receive a single RPC message. Any exception thrown while in this method will be sent back to
   * the client in string form as a standard RPC failure.
   *
   * This method will not be called in parallel for a single TransportClient (i.e., channel).
   *
   * @param client A channel client which enables the handler to make requests back to the sender
   *               of this RPC. This will always be the exact same object for a particular channel.
   * @param message The serialized bytes of the RPC.
   * @param callback Callback which should be invoked exactly once upon success or failure of the
   *                 RPC.
   */
  public abstract void receive(
      TransportClient client,
      ByteBuffer message,
      RpcResponseCallback callback);

  /**
   * Returns the StreamManager which contains the state about which streams are currently being
   * fetched by a TransportClient.
   */
  public abstract StreamManager getStreamManager();

  /**
   * Receives an RPC message that does not expect a reply. The default implementation will
   * call "{@link #receive(TransportClient, ByteBuffer, RpcResponseCallback)}" and log a warning if
   * any of the callback methods are called.
   *
   * @param client A channel client which enables the handler to make requests back to the sender
   *               of this RPC. This will always be the exact same object for a particular channel.
   * @param message The serialized bytes of the RPC.
   */
  public void receive(TransportClient client, ByteBuffer message) {
    receive(client, message, ONE_WAY_CALLBACK);
  }

  /**
   * Invoked when the channel associated with the given client is active.
   */
  public void channelActive(TransportClient client) { }

  /**
   * Invoked when the channel associated with the given client is inactive.
   * No further requests will come from this client.
   */
  public void channelInactive(TransportClient client) { }

  public void exceptionCaught(Throwable cause, TransportClient client) { }

  private static class OneWayRpcCallback implements RpcResponseCallback {

    private static final Logger logger = LoggerFactory.getLogger(OneWayRpcCallback.class);

    @Override
    public void onSuccess(ByteBuffer response) {
      logger.warn("Response provided for one-way RPC.");
    }

    @Override
    public void onFailure(Throwable e) {
      logger.error("Error response provided for one-way RPC.", e);
    }

  }

}
