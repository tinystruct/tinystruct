package org.tinystruct.http.servlet;

import org.tinystruct.http.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class ResponseBuilder extends ResponseWrapper<HttpServletResponse> {
    private final Headers headers = new ResponseHeaders(this);
    private PrintWriter printWriter;
    private ResponseStatus status;
    private Version version;

    public ResponseBuilder(HttpServletResponse response) {
        super(response);

        try {
            this.printWriter = response.getWriter();
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

    public void addHeader(String header, Object value) {
        this.response.addHeader(header, value.toString());
    }

    public String getHeader(String header) {
        return this.response.getHeader(header);
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
        this.response.setStatus(status.code());
        return this;
    }

    @Override
    public Headers headers() {
        return this.headers;
    }

    @Override
    public PrintWriter get() {
        return this.printWriter;
    }

    @Override
    public void sendRedirect(String url) throws IOException {
        this.response.sendRedirect(url);
    }
}
