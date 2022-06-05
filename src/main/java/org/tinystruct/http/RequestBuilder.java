package org.tinystruct.http;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class RequestBuilder extends RequestWrapper {
    private final SessionManager manager = SessionManager.getInstance();
    private final HashSet<String> headerNames = new HashSet<>();
    private final HashMap<String, String> headerValues = new HashMap<>();

    public Session getSession(String id) {
        return manager.getSession(id);
    }

    public Session getSession(String id, boolean generated) {
        if (manager.getSession(id) == null && generated) {
            manager.setSession(id, new MemorySession(id));
        }

        return manager.getSession(id);
    }

    public void setHeader(String header, String value) {
        if (!headerNames.contains(header)) {
            return;
        }

        this.headerValues.put(header, value);
    }

    public String getHeader(String header) {
        return this.headerValues.get(header);
    }

    @Override
    public Version protocolVersion() {
        return null;
    }

    @Override
    public void setProtocolVersion(Version version) {
    }

    @Override
    public List<Header> headers() {
        return null;
    }

    @Override
    public Method method() {
        return null;
    }

    @Override
    public Request setMethod(Method method) {
        return null;
    }

    @Override
    public String uri() {
        return null;
    }

    @Override
    public Request setUri(String uri) {
        return null;
    }
}