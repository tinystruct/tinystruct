package org.tinystruct.http;

public interface Session {
    String getId();
    void setAttribute(String key, Object value);
    Object getAttribute(String key);
    void removeAttribute(String key);
}
