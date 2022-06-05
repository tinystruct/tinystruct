package org.tinystruct.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemorySession implements Session {

    private final Map<String, Object> storage = new ConcurrentHashMap<>();

    public MemorySession(String sessionId) {
        SessionManager.getInstance().setSession(sessionId, this);
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
}
