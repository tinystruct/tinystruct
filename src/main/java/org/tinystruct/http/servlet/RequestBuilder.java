package org.tinystruct.http.servlet;

import org.tinystruct.http.*;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class RequestBuilder extends RequestWrapper<HttpServletRequest> {
    private final SessionManager manager = SessionManager.getInstance();
    private final Headers headers = new Headers();
    private final Cookie[] cookies;
    private Version version;
    private Method method;
    private String uri;

    public RequestBuilder(HttpServletRequest request) {
        super(request);
        this.request.getHeaderNames().asIterator().forEachRemaining(h -> {
            try {
                this.headers.add(Header.value0f(h).set(this.request.getHeader(h)));
            } catch (IllegalArgumentException ignored) {
                ;
            }
        });

        javax.servlet.http.Cookie[] _cookies = this.request.getCookies();
        if (_cookies != null) {
            int i = _cookies.length;

            this.cookies = new Cookie[i];
            for (javax.servlet.http.Cookie _cookie : _cookies) {
                Cookie cookie = new CookieImpl(_cookie.getName());
                cookie.setValue(_cookie.getValue());
                cookie.setDomain(_cookie.getDomain());
                cookie.setHttpOnly(_cookie.isHttpOnly());
                cookie.setMaxAge(_cookie.getMaxAge());
                cookie.setPath(_cookie.getPath());
                cookie.setSecure(_cookie.getSecure());
                cookies[--i] = cookie;
            }
        }
        else
            this.cookies = new Cookie[]{};
    }

    public Session getSession(String id) {
        return manager.getSession(id);
    }

    public Session getSession(String id, boolean generated) {
        if (manager.getSession(id) == null && generated) {
            manager.setSession(id, new MemorySession(id));
        }

        return manager.getSession(id);
    }

    @Override
    public Session getSession() {
        HttpSession session = this.request.getSession();
        String sessionId = session.getId();
        Session memorySession = getSession(sessionId, true);
        session.getAttributeNames().asIterator().forEachRemaining(s -> {
            memorySession.setAttribute(s, session.getAttribute(s));
        });

        return memorySession;
    }

    @Override
    public String query() {
        return this.request.getQueryString();
    }

    @Override
    public ServletInputStream stream() {
        try {
            return this.request.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
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
    public Headers headers() {
        return this.headers;
    }

    @Override
    public Method method() {
        this.setMethod(Method.valueOf(this.request.getMethod()));

        return this.method;
    }

    @Override
    public Request setMethod(Method method) {
        this.method = method;
        return this;
    }

    @Override
    public String uri() {
        this.setUri(this.request.getRequestURI());
        return this.uri;
    }

    @Override
    public Request setUri(String uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public String getParameter(String name) {
        return this.request.getParameter(name);
    }

    @Override
    public Cookie[] cookies() {
        return cookies;
    }
}

