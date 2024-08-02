package org.tinystruct.system;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.tinystruct.ApplicationException;
import org.tinystruct.handler.HttpProxyHandler;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpProxyServer extends ProxyServer implements Bootstrap {
    private final Logger logger = Logger.getLogger(HttpProxyServer.class.getName());
    private int port = 81;
    private String remoteHost = "localhost";
    private int remotePort = 80;

    public HttpProxyServer() {
    }

    @Override
    public void init() {
        this.setTemplateRequired(false);
    }

    @Override
    public String version() {
        return null;
    }

    @Action(value = "start", description = "Start a Tomcat server.", options = {
            @Argument(key = "server-port", description = "Server port"),
            @Argument(key = "remote-server-host", description = "Remote Server host"),
            @Argument(key = "remote-server-port", description = "Remote Server port"),
    }, example = "bin/dispatcher start --import org.tinystruct.system.RedisProxyServer --server-port 8080", mode = org.tinystruct.application.Action.Mode.CLI)
    @Override
    public void start() throws ApplicationException {
        if (this.context != null) {
            if (this.context.getAttribute("--server-port") != null) {
                this.port = Integer.parseInt(this.context.getAttribute("--server-port").toString());
            }

            if (this.context.getAttribute("--remote-server-port") != null) {
                this.remotePort = Integer.parseInt(this.context.getAttribute("--remote-server-port").toString());
            }

            if (this.context.getAttribute("--remote-server-host") != null) {
                this.remoteHost = this.context.getAttribute("--remote-server-host").toString();
            }
        }

        try {
            // Configure SSL.
            final SslContext sslCtx;
            if (this.remotePort == 443) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } else {
                sslCtx = null;
            }

            ServerBootstrap bootstrap = new ServerBootstrap().group(bossgroup, workgroup)
                    .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class
                            : NioServerSocketChannel.class)
                    .handler(new LoggingHandler())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            if (sslCtx != null) {
                                ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            ch.pipeline().addLast(new LoggingHandler());
                            ch.pipeline().addLast(new HttpRequestDecoder());
                            ch.pipeline().addLast(new HttpProxyHandler(remoteHost, remotePort));
                        }
                    })
                    .childOption(ChannelOption.AUTO_READ, false);

            // Bind and start to accept incoming connections.
            ChannelFuture future = bootstrap.bind(port).sync();
            logger.info("Proxy server(" + port + ") started.");

            // Wait until the server socket is closed.
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
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