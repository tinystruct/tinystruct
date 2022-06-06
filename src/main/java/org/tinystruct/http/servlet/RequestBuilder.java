package org.tinystruct.http;

import javax.servlet.http.HttpServletRequest;

public class RequestBuilder extends RequestWrapper {
    private final SessionManager manager = SessionManager.getInstance();
    private final Headers headers = new Headers();
    private Version version;
    private Method method;
    private String uri;

    public RequestBuilder(HttpServletRequest request) {
        super(request);
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
        javax.servlet.http.Cookie[] cookies = this.request.getCookies();
        String sessionId = null;
        for (javax.servlet.http.Cookie cookie1 : cookies) {
            if (cookie1.getName().equalsIgnoreCase("jsessionid")) {
                sessionId = cookie1.getValue();
                break;
            }
        }

        if (sessionId != null) {
            return getSession(sessionId, true);
        }

        return null;
    }

    @Override
    public String query() {
        return this.request.getQueryString();
    }

    private void setHeader(Header header, Object value) {
        this.headers.add(header.set(value));
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
}

class CookieImpl implements Cookie {
    private final String name;

    public CookieImpl(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public String value() {
        return null;
    }

    @Override
    public void setValue(String value) {

    }

    @Override
    public boolean wrap() {
        return false;
    }

    @Override
    public void setWrap(boolean wrap) {

    }

    @Override
    public String domain() {
        return null;
    }

    @Override
    public void setDomain(String domain) {

    }

    @Override
    public String path() {
        return null;
    }

    @Override
    public void setPath(String path) {

    }

    @Override
    public long maxAge() {
        return 0;
    }

    @Override
    public void setMaxAge(long maxAge) {

    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public void setSecure(boolean secure) {

    }

    @Override
    public boolean isHttpOnly() {
        return false;
    }

    @Override
    public void setHttpOnly(boolean httpOnly) {

    }
}
