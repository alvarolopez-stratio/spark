/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.api.r

import java.io.{DataOutputStream, File, FileOutputStream, IOException}
import java.net.{InetAddress, InetSocketAddress, ServerSocket}
import java.util.concurrent.TimeUnit

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{ChannelFuture, ChannelInitializer, EventLoopGroup}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.bytes.{ByteArrayDecoder, ByteArrayEncoder}
import io.netty.handler.timeout.ReadTimeoutHandler

import org.apache.spark.SparkConf
import org.apache.spark.internal.Logging

/**
 * Netty-based backend server that is used to communicate between R and Java.
 */
private[spark] class RBackend {

  private[this] var channelFuture: ChannelFuture = null
  private[this] var bootstrap: ServerBootstrap = null
  private[this] var bossGroup: EventLoopGroup = null

  /** Tracks JVM objects returned to R for this RBackend instance. */
  private[r] val jvmObjectTracker = new JVMObjectTracker

  def init(): Int = {
    val conf = new SparkConf()
    val backendConnectionTimeout = conf.getInt(
      "spark.r.backendConnectionTimeout", SparkRDefaults.DEFAULT_CONNECTION_TIMEOUT)
    bossGroup = new NioEventLoopGroup(
      conf.getInt("spark.r.numRBackendThreads", SparkRDefaults.DEFAULT_NUM_RBACKEND_THREADS))
    val workerGroup = bossGroup
    val handler = new RBackendHandler(this)

    bootstrap = new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])

    bootstrap.childHandler(new ChannelInitializer[SocketChannel]() {
      def initChannel(ch: SocketChannel): Unit = {
        ch.pipeline()
          .addLast("encoder", new ByteArrayEncoder())
          .addLast("frameDecoder",
            // maxFrameLength = 2G
            // lengthFieldOffset = 0
            // lengthFieldLength = 4
            // lengthAdjustment = 0
            // initialBytesToStrip = 4, i.e. strip out the length field itself
            new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
          .addLast("decoder", new ByteArrayDecoder())
          .addLast("readTimeoutHandler", new ReadTimeoutHandler(backendConnectionTimeout))
          .addLast("handler", handler)
      }
    })

    channelFuture = bootstrap.bind(new InetSocketAddress("localhost", 0))
    channelFuture.syncUninterruptibly()
    channelFuture.channel().localAddress().asInstanceOf[InetSocketAddress].getPort()
  }

  def run(): Unit = {
    channelFuture.channel.closeFuture().syncUninterruptibly()
  }

  def close(): Unit = {
    if (channelFuture != null) {
      // close is a local operation and should finish within milliseconds; timeout just to be safe
      channelFuture.channel().close().awaitUninterruptibly(10, TimeUnit.SECONDS)
      channelFuture = null
    }
    if (bootstrap != null && bootstrap.group() != null) {
      bootstrap.group().shutdownGracefully()
    }
    if (bootstrap != null && bootstrap.childGroup() != null) {
      bootstrap.childGroup().shutdownGracefully()
    }
    bootstrap = null
    jvmObjectTracker.clear()
  }

}

private[spark] object RBackend extends Logging {
  initializeLogIfNecessary(true)

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      // scalastyle:off println
      System.err.println("Usage: RBackend <tempFilePath>")
      // scalastyle:on println
      System.exit(-1)
    }

    val sparkRBackend = new RBackend()
    try {
      // bind to random port
      val boundPort = sparkRBackend.init()
      val serverSocket = new ServerSocket(0, 1, InetAddress.getByName("localhost"))
      val listenPort = serverSocket.getLocalPort()
      // Connection timeout is set by socket client. To make it configurable we will pass the
      // timeout value to client inside the temp file
      val conf = new SparkConf()
      val backendConnectionTimeout = conf.getInt(
        "spark.r.backendConnectionTimeout", SparkRDefaults.DEFAULT_CONNECTION_TIMEOUT)

      // tell the R process via temporary file
      val path = args(0)
      val f = new File(path + ".tmp")
      val dos = new DataOutputStream(new FileOutputStream(f))
      dos.writeInt(boundPort)
      dos.writeInt(listenPort)
      SerDe.writeString(dos, RUtils.rPackages.getOrElse(""))
      dos.writeInt(backendConnectionTimeout)
      dos.close()
      f.renameTo(new File(path))

      // wait for the end of stdin, then exit
      new Thread("wait for socket to close") {
        setDaemon(true)
        override def run(): Unit = {
          // any un-catched exception will also shutdown JVM
          val buf = new Array[Byte](1024)
          // shutdown JVM if R does not connect back in 10 seconds
          serverSocket.setSoTimeout(10000)
          try {
            val inSocket = serverSocket.accept()
            serverSocket.close()
            // wait for the end of socket, closed if R process die
            inSocket.getInputStream().read(buf)
          } finally {
            sparkRBackend.close()
            System.exit(0)
          }
        }
      }.start()

      sparkRBackend.run()
    } catch {
      case e: IOException =>
        logError("Server shutting down: failed with exception ", e)
        sparkRBackend.close()
        System.exit(1)
    }
    System.exit(0)
  }
}
