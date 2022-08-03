package org.tinystruct.http;

import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<String, Session>();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return SingletonHolder.manager;
    }

    public void setSession(String sessionId, Session value) {
        sessions.put(sessionId, value);
    }

    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    private static final class SingletonHolder {
        static final SessionManager manager = new SessionManager();
    }
}