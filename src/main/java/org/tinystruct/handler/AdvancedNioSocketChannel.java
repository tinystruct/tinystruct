package org.tinystruct.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.channels.SocketChannel;

public class AdvancedNioSocketChannel extends NioSocketChannel {

    @SuppressWarnings("deprecation")
    private RecvByteBufAllocator.Handle allocHandle;

    public AdvancedNioSocketChannel(AdvancedNioServerSocketChannel advancedNioServerSocketChannel, SocketChannel ch) {
        super(advancedNioServerSocketChannel, ch);
        allocHandle = unsafe().recvBufAllocHandle();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected int doReadBytes(ByteBuf byteBuf) throws Exception {
        try {
            if (this.isActive()) {
                allocHandle.attemptedBytesRead(byteBuf.writableBytes());
                return byteBuf.writeBytes(javaChannel(), allocHandle.attemptedBytesRead());
            }
        } catch (Exception e) {
            this.disconnect();
        }
        return -1;
    }
}