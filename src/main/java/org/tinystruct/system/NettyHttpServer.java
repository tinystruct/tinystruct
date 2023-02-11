/*******************************************************************************
 * Copyright  (c) 2013, 2023 James Mover Zhou
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
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.handler.HttpRequestHandler;
import org.tinystruct.handler.HttpStaticFileHandler;
import org.tinystruct.system.cli.CommandOption;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class NettyHttpServer extends AbstractApplication implements Bootstrap {
    private static final boolean SSL = System.getProperty("ssl") != null;
    private static final int MAX_CONTENT_LENGTH = 1024 * 100;
    private final EventLoopGroup bossgroup;
    private final EventLoopGroup workgroup;
    private final Logger logger = Logger.getLogger(NettyHttpServer.class.getName());
    private int port = 8080;

    public NettyHttpServer() {
        if (Epoll.isAvailable()) {
            this.bossgroup = new EpollEventLoopGroup(1);
            this.workgroup = new EpollEventLoopGroup();
        } else {
            this.bossgroup = new NioEventLoopGroup(1);
            this.workgroup = new NioEventLoopGroup();
        }
    }

    @Override
	public void init() {
        this.setAction("start", "start");

        List<CommandOption> options = new ArrayList<CommandOption>();
        CommandOption option = new CommandOption("server-port", "", "Server port");
        options.add(option);
        option = new CommandOption("http.proxyHost", "127.0.0.1", "Proxy host for http");
        options.add(option);
        option = new CommandOption("http.proxyPort", "3128", "Proxy port for http");
        options.add(option);
        option = new CommandOption("https.proxyHost", "127.0.0.1", "Proxy host for https");
        options.add(option);
        option = new CommandOption("https.proxyPort", "3128", "Proxy port for https");
        options.add(option);
        this.commandLines.get("start").setOptions(options);
    }

    @Override
    public String version() {
        return null;
    }

    @Override
    public void start() throws ApplicationException {

        if (this.context != null) {
            if (this.context.getAttribute("--server-port") != null) {
                this.port = Integer.parseInt(this.context.getAttribute("--server-port").toString());
            }

            if (this.context.getAttribute("--http.proxyHost") != null && this.context.getAttribute("--http.proxyPort") != null) {
                System.setProperty("http.proxyHost", this.context.getAttribute("--http.proxyHost").toString());
                System.setProperty("http.proxyPort", this.context.getAttribute("--http.proxyPort").toString());
            }

            if (this.context.getAttribute("--https.proxyHost") != null && this.context.getAttribute("--https.proxyPort") != null) {
                System.setProperty("https.proxyHost", this.context.getAttribute("--https.proxyHost").toString());
                System.setProperty("https.proxyPort", this.context.getAttribute("--https.proxyPort").toString());
            }
        }

        System.out.println(ApplicationManager.call("--logo", this.context));
        long start = System.currentTimeMillis();
        try {
            // Configure SSL.
            final SslContext sslCtx;
            if (SSL) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } else {
                sslCtx = null;
            }

            final int maxContentLength = "".equalsIgnoreCase(this.config.get("default.http.max_content_length")) ? MAX_CONTENT_LENGTH : Integer.parseInt(this.config.get("default.http.max_content_length"));
            ServerBootstrap bootstrap = new ServerBootstrap().group(bossgroup, workgroup)
                    .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class :
                            NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(new HttpServerCodec(), new HttpObjectAggregator(maxContentLength), new ChunkedWriteHandler(), new HttpStaticFileHandler(), new HttpRequestHandler(getConfiguration()));
                        }
                    }).option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                    .childOption(ChannelOption.SO_KEEPALIVE, false)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            // Bind and start to accept incoming connections.
            ChannelFuture future = bootstrap.bind(port).sync();
            logger.info("Netty server (" + port + ") startup in " + (System.currentTimeMillis() - start) + " ms");

            // Open the default browser
            this.context.setAttribute("--url", "http://localhost:" + this.port);
            ApplicationManager.call("open", this.context);

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
