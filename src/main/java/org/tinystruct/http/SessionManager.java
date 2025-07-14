package org.tinystruct.mcp;

import org.tinystruct.system.scheduling.Scheduler;
import org.tinystruct.system.scheduling.TimeIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager implements Monitor<SessionListener> {
    private static final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<String, Session>();
    private transient final List<SessionListener> listeners = new ArrayList<>();

    private SessionManager() {
        Scheduler.getInstance().schedule(new ExpirationValidator(), new TimeIterator(0, 0, 0), 1000);
    }

    public static SessionManager getInstance() {
        return SingletonHolder.manager;
    }

    public void setSession(String sessionId, Session session) {
        sessions.put(sessionId, session);
        SessionEvent event = new SessionEvent(SessionEvent.Type.CREATED, session);
        fireEvent(event);
    }

    private void fireEvent(SessionEvent event) {
        if (listeners.isEmpty()) {
            return;
        }
        listeners.forEach(listener -> listener.onSessionEvent(event));
    }

    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public void removeSession(String sessionId) {
        Session session = sessions.remove(sessionId);
        SessionEvent event = new SessionEvent(SessionEvent.Type.DESTROYED, session);
        listeners.forEach(listener -> listener.onSessionEvent(event));
    }

    @Override
    public void addListener(SessionListener listener) {
        this.listeners.add(listener);
    }

    private static final class ExpirationValidator extends TimerTask {
        @Override
        public void run() {
            sessions.forEach((sessionId, session) -> {
                if(session.isExpired()) {
                    getInstance().removeSession(sessionId);
                }
            });
        }
    }

    private static final class SingletonHolder {
        static final SessionManager manager = new SessionManager();
    }
}