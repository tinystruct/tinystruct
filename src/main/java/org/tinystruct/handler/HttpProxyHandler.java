package org.tinystruct.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class HttpProxyHandler extends ProxyInboundHandler {

    public HttpProxyHandler(String remoteHost, int remotePort) {
        super(remoteHost, remotePort);
    }

    @Override
    public ChannelHandler[] initCodecs() {
        return new ChannelHandler[]{new HttpClientCodec()};
    }

}
