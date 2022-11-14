package org.tinystruct.http;

import org.apache.catalina.SessionListener;

public interface Session {
    String getId();
    void setAttribute(String key, Object value);
    Object getAttribute(String key);
    void removeAttribute(String key);

    boolean isExpired();
}
