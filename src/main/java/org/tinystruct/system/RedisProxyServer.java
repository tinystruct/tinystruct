package org.tinystruct.system;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.logging.LoggingHandler;
import org.tinystruct.ApplicationException;
import org.tinystruct.handler.RedisProxyHandler;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;
import org.tinystruct.system.cli.CommandOption;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class RedisProxyServer extends ProxyServer implements Bootstrap {
    private int port = 6380;
    private String remoteHost = "localhost";
    private int remotePort = 6379;

    private final Logger logger = Logger.getLogger(RedisProxyServer.class.getName());

    public RedisProxyServer() {

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
    }, example = "bin/dispatcher start --import org.tinystruct.system.RedisProxyServer --server-port 8080")
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
                            ch.pipeline().addLast(new RedisDecoder(true));
                            ch.pipeline().addLast(new RedisProxyHandler(remoteHost, remotePort));
                        }
                    })
                    .childOption(ChannelOption.AUTO_READ, false);

            // Bind and start to accept incoming connections.
            ChannelFuture future = bootstrap.bind(port).sync();
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
