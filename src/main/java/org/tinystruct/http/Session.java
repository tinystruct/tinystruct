package org.tinystruct.http;

public interface Session {
    void setAttribute(String key, Object value);
    Object getAttribute(String key);
    void removeAttribute(String key);
}
