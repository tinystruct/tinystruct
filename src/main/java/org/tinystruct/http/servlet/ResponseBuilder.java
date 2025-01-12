package org.tinystruct.http.servlet;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.tinystruct.http.*;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResponseBuilder extends ResponseWrapper<HttpServletResponse, ServletOutputStream> {
    private final Headers headers = new Headers();
    private ServletOutputStream outputStream;
    private ResponseStatus status;
    private Version version;
    private static final Logger logger = Logger.getLogger(ResponseBuilder.class.getName());

    public ResponseBuilder(HttpServletResponse response) {
        super(response);

        try {
            this.outputStream = response.getOutputStream();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
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
        if (!this.response.containsHeader(header)) {
            this.headers.add(new Header(header).set(value));
            if (value instanceof Integer) {
                this.response.addIntHeader(header, (Integer) value);
            } else if (value instanceof Long) {
                this.response.addDateHeader(header, (Long) value);
            } else
                this.response.addHeader(header, value.toString());
        } else {
            if (this.response.getHeader(header) == null || !this.response.getHeader(header).equalsIgnoreCase(value.toString())) {
                if (value instanceof Integer) {
                    this.response.addIntHeader(header, (Integer) value);
                } else if (value instanceof Long) {
                    this.response.addDateHeader(header, (Long) value);
                } else
                    this.response.addHeader(header, value.toString());
            }
        }
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
    public Response<HttpServletResponse, ServletOutputStream> setStatus(ResponseStatus status) {
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
        if (!this.response.isCommitted())
            this.response.sendRedirect(url);
    }
}
