package org.tinystruct.system;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.handler.MQTTServerHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MQTTServer extends AbstractApplication implements Bootstrap {
    private final static Logger logger = Logger.getLogger(MQTTServer.class.getName());

    @Override
    public void init() {

    }

    @Override
    public String version() {
        return null;
    }

    @Override
    public void start() throws ApplicationException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup);
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.channel(NioServerSocketChannel.class);
            b.childHandler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast("decoder", new MqttDecoder());
                    ch.pipeline().addLast("encoder", MqttEncoder.INSTANCE);
                    ch.pipeline().addLast("handler", MQTTServerHandler.INSTANCE);
                }
            });

            ChannelFuture f = b.bind(1883).sync();
            System.out.println("MQTT Broker initiated...");

            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }

    }

    @Override
    public void stop() {

    }
}
