package org.tinystruct.http;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

import java.util.Collections;
import java.util.Set;

public class RequestBuilder extends RequestWrapper<HttpRequest> {
    private final SessionManager manager = SessionManager.getInstance();
    private final Headers headers = new Headers();
    private final Cookie[] cookies;
    private Version version;
    private Method method;
    private String uri;

    public RequestBuilder(HttpRequest request) {
        super(request);

        HttpHeaders headers = request.headers();
        headers.forEach(h-> this.headers.add(Header.value0f(h.getKey()).set(h.getValue())));

        Set<io.netty.handler.codec.http.cookie.Cookie> _cookies;
        String value = request.headers().get(HttpHeaderNames.COOKIE);
        if (value == null) {
            _cookies = Collections.emptySet();
        } else {
            _cookies = ServerCookieDecoder.STRICT.decode(value);
        }

        int i = _cookies.size();
        this.cookies = new Cookie[i];
        for (io.netty.handler.codec.http.cookie.Cookie _cookie : _cookies) {
            Cookie cookie = new CookieImpl(_cookie.name());
            cookie.setValue(_cookie.value());
            cookie.setDomain(_cookie.domain());
            cookie.setHttpOnly(_cookie.isHttpOnly());
            cookie.setMaxAge(_cookie.maxAge());
            cookie.setPath(_cookie.path());
            cookie.setSecure(_cookie.isSecure());
            cookies[--i] = cookie;
        }
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
        String[] cookies = this.request.headers().get(Header.COOKIE.name()).split(";");
        String sessionId = null;
        for (String cookie1 : cookies) {
            String[] _cookie = cookie1.split(":");
            if (_cookie[0].equalsIgnoreCase("jsessionid")) {
                sessionId = _cookie[1];
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
        return null;
    }

    @Override
    public Object stream() {
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
        this.setMethod(Method.valueOf(this.request.method().name()));

        return this.method;
    }

    @Override
    public Request setMethod(Method method) {
        this.method = method;
        return this;
    }

    @Override
    public String uri() {
        this.setUri(this.request.uri());
        return this.uri;
    }

    @Override
    public Request setUri(String uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public String getParameter(String name) {
        return null;
    }

    @Override
    public Cookie[] cookies() {
        return cookies;
    }
}

