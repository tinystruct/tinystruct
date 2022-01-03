package org.tinystruct.system;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.tinystruct.ApplicationException;
import org.tinystruct.handler.HttpProxyHandler;

import java.util.logging.Logger;

public class HttpProxyServer extends ProxyServer implements Bootstrap {
    private int port = 81;
    private String remoteHost = "localhost";
    private int remotePort = 80;
    private ChannelFuture future;
    private EventLoopGroup bossgroup;
    private EventLoopGroup workgroup;
    private Logger logger = Logger.getLogger(HttpProxyServer.class.getName());

    public HttpProxyServer() {
        if (Epoll.isAvailable()) {
            this.bossgroup = new EpollEventLoopGroup(1);
            this.workgroup = new EpollEventLoopGroup();
        } else {
            this.bossgroup = new NioEventLoopGroup(1);
            this.workgroup = new NioEventLoopGroup();
        }
    }

    public void init() {
        this.setAction("--start-http-proxy", "start");

        this.setTemplateRequired(false);
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
                    .channel(Epoll.isAvailable()? EpollServerSocketChannel.class
                            : NioServerSocketChannel.class)
                    .handler(new LoggingHandler())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new LoggingHandler(LogLevel.TRACE));
                            ch.pipeline().addLast(new HttpRequestDecoder());
                            ch.pipeline().addLast(new HttpProxyHandler(remoteHost, remotePort));
                        }
                    })
                    .childOption(ChannelOption.AUTO_READ, false);

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
