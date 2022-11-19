package org.tinystruct.http;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class ResponseBuilder extends ResponseWrapper<FullHttpResponse> {
    private final Headers headers = new ResponseHeaders(this);
    private ResponseStatus status;
    private Version version;

    public ResponseBuilder(FullHttpResponse response) {
        super(response);

        for (Map.Entry<String, String> map: response.headers()) {
            this.headers.add(Header.value0f(map.getKey().replace('-','_').toUpperCase(Locale.ROOT)).set(map.getValue()));
        }
    }

    public void setContentType(String contentType) {
        this.headers.add(Header.CONTENT_TYPE.set(contentType));
    }

    public void addHeader(String header, Object value) {
        if(!this.response.headers().contains(header) || !this.response.headers().get(header).equalsIgnoreCase(value.toString())) {
            if (value instanceof Integer) {
                this.response.headers().addInt(header, (Integer) value);
            } else
                this.response.headers().add(header, value);
        }
    }

    public void addCookie(Cookie cookie) {
        addHeader(Header.SET_COOKIE.toString(), cookie.toString());
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
    public Response setStatus(ResponseStatus status) {
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
    public void sendRedirect(String url) throws IOException {
        ResponseHeaders responseHeaders = new ResponseHeaders(this);
        responseHeaders.add(Header.CONTENT_LENGTH.setInt(0));
        responseHeaders.add(Header.LOCATION.set(url));
        this.response.setStatus(HttpResponseStatus.valueOf(307));
    }

}