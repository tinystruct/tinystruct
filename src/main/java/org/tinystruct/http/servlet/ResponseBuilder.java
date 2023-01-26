package org.tinystruct.http.servlet;

import org.tinystruct.http.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ResponseBuilder extends ResponseWrapper<HttpServletResponse> {
    private final Headers headers = new ResponseHeaders(this);
    private ServletOutputStream outputStream;
    private ResponseStatus status;
    private Version version;

    public ResponseBuilder(HttpServletResponse response) {
        super(response);

        try {
            this.outputStream = response.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String headerName : this.response.getHeaderNames()) {
            this.headers.add(Header.value0f(headerName).set(this.response.getHeader(headerName)));
        }
    }

    public void setContentType(String contentType) {
        this.response.setContentType(contentType);
    }

    @Override
	public void addHeader(String header, Object value) {
        this.response.addHeader(header, value.toString());
    }

    public String getHeader(String header) {
        return this.response.getHeader(header);
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
    public Response setStatus(ResponseStatus status) {
        this.status = status;
        this.response.setStatus(status.code());
        return this;
    }

    @Override
    public Headers headers() {
        return this.headers;
    }

    @Override
    public ServletOutputStream get() {
        return this.outputStream;
    }

    @Override
    public void sendRedirect(String url) throws IOException {
        this.response.sendRedirect(url);
    }
}
