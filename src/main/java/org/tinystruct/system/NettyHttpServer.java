/*******************************************************************************
 * Copyright  (c) 2013, 2017 James Mover Zhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.tinystruct.system;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.handler.HttpRequestHandler;
import org.tinystruct.handler.HttpStaticFileHandler;
import org.tinystruct.system.cli.CommandOption;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class NettyHttpServer extends AbstractApplication implements Bootstrap {
    private static final boolean SSL = System.getProperty("ssl") != null;
    private int port = 8080;
    private static final int MAX_CONTENT_LENGTH = 1024 * 100;
    private final EventLoopGroup bossgroup;
    private final EventLoopGroup workgroup;
    private final Logger logger = Logger.getLogger(NettyHttpServer.class.getName());

    public NettyHttpServer() {
        if (Epoll.isAvailable()) {
            this.bossgroup = new EpollEventLoopGroup(1);
            this.workgroup = new EpollEventLoopGroup();
        } else {
            this.bossgroup = new NioEventLoopGroup(1);
            this.workgroup = new NioEventLoopGroup();
        }
    }

    public void init() {
        this.setAction("start", "start");

        List<CommandOption> options = new ArrayList<CommandOption>();
        options.add(new CommandOption("server-port", "", "Server port"));
        this.commandLines.get("start").setOptions(options);
    }

    @Override
    public String version() {
        return null;
    }

    @Override
    public void start() throws ApplicationException {
        if (this.context != null && this.context.getAttribute("--server-port") != null) {
            this.port = Integer.parseInt(this.context.getAttribute("--server-port").toString());
        }
        try {
            // Configure SSL.
            final SslContext sslCtx;
            if (SSL) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } else {
                sslCtx = null;
            }

            ServerBootstrap bootstrap = new ServerBootstrap().group(bossgroup, workgroup)
                    .channel(Epoll.isAvailable()? EpollServerSocketChannel.class:
                            NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(new HttpServerCodec(), new HttpObjectAggregator(MAX_CONTENT_LENGTH), new ChunkedWriteHandler(), new HttpStaticFileHandler(), new HttpRequestHandler(getConfiguration(), getContext()));
                        }
                    }).option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            // Bind and start to accept incoming connections.
            ChannelFuture future = bootstrap.bind(port).sync();
            logger.info("Netty server(" + port + ") started.");

            // Wait until the server socket is closed.
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        } finally {
            this.stop();
        }
    }

    @Override
    public void stop() {
        bossgroup.shutdownGracefully();
        workgroup.shutdownGracefully();
    }
}
