package org.tinystruct.handler;

import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.channels.SocketChannel;
import java.util.List;

public class AdvancedNioServerSocketChannel extends NioServerSocketChannel {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AdvancedNioServerSocketChannel.class);

    @Override
    protected int doReadMessages(List<Object> buf) throws Exception {

        try (SocketChannel ch = SocketUtils.accept(javaChannel())) {
            if (ch != null) {
                buf.add(new AdvancedNioSocketChannel(this, ch));
                return 1;
            }
        } catch (Exception t) {
            logger.warn("Failed to create a new channel from an accepted socket.", t);
        }

        return 0;
    }
}