package org.tinystruct.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemorySession implements Session {

    private final Map<String, Object> storage = new ConcurrentHashMap<>();
    private final String sessionId;
    private final long expiry;

    public MemorySession(String sessionId) {
        this.sessionId = sessionId;
        this.expiry = System.currentTimeMillis() + 1800000L;
    }

    @Override
    public String getId() {
        return this.sessionId;
    }

    @Override
    public void setAttribute(String key, Object value) {
        storage.put(key, value);
    }

    @Override
    public Object getAttribute(String key) {
        return storage.get(key);
    }

    @Override
    public void removeAttribute(String key) {
        storage.remove(key);
    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() > expiry;
    }
}
