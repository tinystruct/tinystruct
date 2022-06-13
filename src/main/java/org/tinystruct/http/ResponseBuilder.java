package org.tinystruct.http;

import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;
import java.util.Map;

public class ResponseBuilder extends ResponseWrapper<HttpResponse> {
    private final Headers headers = new ResponseHeaders(this);
    private ResponseStatus status;
    private Version version;

    public ResponseBuilder(HttpResponse response) {
        super(response);
    }

    public void setContentType(String contentType) {
        this.response.headers().add(Header.CONTENT_TYPE.name(), contentType);
    }

    public void addHeader(String header, Object value) {
        if (value instanceof Integer) {
            this.response.headers().addInt(header, (Integer) value);
        } else
            this.response.headers().add(header, value);
    }

    public String getHeader(String header) {
        return this.response.headers().get(header);
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
        for (Map.Entry<String, String> header : this.response.headers()) {
            this.headers.add(Header.valueOf(header.getKey()).set(header.getValue()));
        }
        return this.headers;
    }

    @Override
    public HttpResponse get() {
        return this.response;
    }

    @Override
    public void sendRedirect(String url) throws IOException {
    }
}
