package org.tinystruct.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.tinystruct.ApplicationException;

public class ResponseBuilder extends ResponseWrapper<FullHttpResponse, FullHttpResponse> {
    private ResponseStatus status;
    private Version version;
    private final Headers headers = new Headers();
    private final ChannelHandlerContext ctx; // Netty context for immediate flush

    public ResponseBuilder(FullHttpResponse response, ChannelHandlerContext ctx) {
        super(response);
        this.ctx = ctx;
        HttpHeaders headers = response.headers();
        Object[] names = headers.names().toArray();
        for (Object o : names) {
            String name = o.toString();
            this.headers.add(Header.value0f(name).set(headers.get(name)));
        }
        this.status = ResponseStatus.valueOf(response.status().code());
    }

    public void setContentType(String contentType) {
        this.response.headers().add(Header.CONTENT_TYPE.name(), contentType);
    }

    @Override
    public void addHeader(String header, Object value) {
        if (!this.response.headers().contains(header)) {
            this.headers.add(new Header(header).set(value));
            if (value instanceof Integer) {
                this.response.headers().addInt(header, (Integer) value);
            } else
                this.response.headers().add(header, value);
        } else {
            if (this.response.headers().get(header) == null || !this.response.headers().get(header).equalsIgnoreCase(value.toString())) {
                if (value instanceof Integer) {
                    this.response.headers().addInt(header, (Integer) value);
                } else
                    this.response.headers().add(header, value);
            }
        }
    }

    public void addCookie(Cookie cookie) {
        addHeader(Header.SET_COOKIE.name(), cookie.toString());
    }

    @Override
    public Version version() {
        return this.version;
    }

    @Override
    public void setVersion(Version version) {
        this.version = version;
    }

    @Override
    public ResponseStatus status() {
        return this.status;
    }

    @Override
    public Response<FullHttpResponse, FullHttpResponse> setStatus(ResponseStatus status) {
        this.status = status;
        this.response.setStatus(HttpResponseStatus.valueOf(status.code()));
        return this;
    }

    @Override
    public Headers headers() {
        return this.headers;
    }

    @Override
    public FullHttpResponse get() {
        return this.response;
    }

    @Override
    public void sendRedirect(String url) throws ApplicationException {
        this.response.content().clear();

        ResponseHeaders responseHeaders = new ResponseHeaders(this);
        responseHeaders.add(Header.CONTENT_LENGTH.setInt(0));
        responseHeaders.add(Header.LOCATION.set(url));
        this.setStatus(ResponseStatus.TEMPORARY_REDIRECT);
    }

    /**
     * Write and flush bytes immediately to the client using Netty's channel.
     * This is critical for SSE streaming.
     */
    @Override
    public void writeAndFlush(byte[] bytes) throws ApplicationException {
        // Netty: Write as a new chunk and flush immediately for SSE
        ctx.writeAndFlush(new DefaultHttpContent(Unpooled.wrappedBuffer(bytes)));
    }

    @Override
    public void close() throws ApplicationException {
        ctx.close();
    }
}