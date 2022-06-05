package org.tinystruct.http;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResponseBuilder extends ResponseWrapper {
    private final Map<String, List<String>> headers = new HashMap<String, List<String>>();
    private ResponseStatus status;

    public ResponseBuilder(OutputStream stream) {
        super(stream);
    }

    public void setContentType(String contentType) {

    }

    public void addHeader(String header, String value) {

    }

    public String getHeader(String header) {
        return null;
    }

    public void addCookie(Cookie cookie) {
        addHeader("Set-Cookie", cookie.toString());
    }

    @Override
    public Version protocolVersion() {
        return null;
    }

    @Override
    public void setProtocolVersion(Version version) {

    }

    @Override
    public ResponseStatus status() {
        return this.status;
    }

    @Override
    public Response setStatus(ResponseStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public List<Header> headers() {
        return null;
    }
}
