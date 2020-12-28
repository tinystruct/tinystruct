package org.tinystruct.system;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.handler.AdvancedNioServerSocketChannel;
import org.tinystruct.handler.ProxyFrontendHandler;

import java.util.logging.Logger;

public class ProxyServer extends AbstractApplication implements Bootstrap {
    private int port = 6379;
    private String remoteHost = "localhost";
    private int remotePort = 9001;
    private ChannelFuture future;
    private EventLoopGroup bossgroup;
    private EventLoopGroup workgroup;
    private Logger logger = Logger.getLogger(ProxyServer.class.getName());

    public ProxyServer() {
        if (Epoll.isAvailable()) {
            this.bossgroup = new EpollEventLoopGroup(1);
            this.workgroup = new EpollEventLoopGroup();
        } else {
            this.bossgroup = new NioEventLoopGroup(1);
            this.workgroup = new NioEventLoopGroup();
        }
    }

    public void init() {
        this.setAction("--start-server-proxy", "start");
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

            if (this.context.getAttribute("--remote-server-port") != null) {
                this.remotePort = Integer.parseInt(this.context.getAttribute("--remote-server-port").toString());
            }

            if (this.context.getAttribute("--remote-server-host") != null) {
                this.remoteHost = this.context.getAttribute("--remote-server-host").toString();
            }
        }

        try {
            ServerBootstrap bootstrap = new ServerBootstrap().group(bossgroup, workgroup)
                    .channel(AdvancedNioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ProxyFrontendHandler proxy = new ProxyFrontendHandler(remoteHost, remotePort) {
                                @Override
                                public ChannelHandler[] initCodecs() {
                                    return new ChannelHandler[]{};
                                }
                            };
                            ChannelPipeline p = ch.pipeline();

                            ChannelHandler[] handlers;
                            if ((handlers = proxy.initCodecs()) != null && handlers.length > 0)
                                p.addLast(handlers);
                            p.addLast(new LoggingHandler(LogLevel.TRACE), proxy);
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128) // (5)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000).childOption(ChannelOption.SO_KEEPALIVE, true); // (6);

            // Bind and start to accept incoming connections.
            future = bootstrap.bind(port).sync();
            logger.info("Proxy server(" + port + ") started.");

            // Wait until the server socket is closed.
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
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
