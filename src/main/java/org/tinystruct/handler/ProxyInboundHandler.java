/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.tinystruct.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ProxyInboundHandler extends ChannelInboundHandlerAdapter implements ProxyHandler {

    private static final Logger logger = Logger.getLogger(ProxyInboundHandler.class.getName());
    private Channel outboundChannel;

    String remoteHost = "localhost";
    final int remotePort;

    public ProxyInboundHandler(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    public ProxyInboundHandler(int remotePort) {
        this.remotePort = remotePort;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final Channel inboundChannel = ctx.channel();

        // Start the connection attempt.
        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new ProxyOutboundHandler(inboundChannel))
                .option(ChannelOption.AUTO_READ, false);
        ChannelFuture f = b.connect(remoteHost, remotePort);

        // Configure SSL.
        SslContext sslCtx = null;
        if (this.remotePort == 443) {
            try {
                sslCtx = SslContextBuilder.forClient().build();
            } catch (SSLException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        outboundChannel = f.channel();
        if (sslCtx != null)
            outboundChannel.pipeline().addLast(sslCtx.newHandler(outboundChannel.alloc()));
        outboundChannel.pipeline().addLast(this.initCodecs());
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    // connection complete start to read first data
                    inboundChannel.read();
                } else {
                    // Close the connection if the connection attempt has failed.
                    inboundChannel.close();
                }
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        logger.log(Level.INFO, msg.getClass().getName());

        if (outboundChannel.isActive() && outboundChannel.isWritable()) {
            outboundChannel.writeAndFlush(msg)
                    .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) {
                            if (future.isSuccess()) {
                                // was able to flush out data, start to read the next chunk
                                ctx.channel().read();
                            } else {
                                future.channel().close();
                            }
                        }
                    });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        logger.log(Level.SEVERE, "Exception caught!", e);
        closeOnFlush(ctx.channel());
    }

    @Override
    public void addCodec(ChannelPipeline pipeline, ChannelHandler... handler) {
        pipeline.addLast(handler);
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
