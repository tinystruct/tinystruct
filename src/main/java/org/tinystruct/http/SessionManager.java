package org.tinystruct.http;

import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;
import org.tinystruct.system.scheduling.Scheduler;
import org.tinystruct.system.scheduling.TimeIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager implements Monitor<SessionListener> {
    private static final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<String, Session>();
    private transient final List<SessionListener> listeners = new ArrayList<>();
    private SessionRepository repository;

    private SessionManager() {
        Configuration<String> settings = new Settings();
        String repositoryName = settings.get("default.session.repository");
        if (repositoryName != null && !repositoryName.isEmpty()) {
            try {
                this.repository = (SessionRepository) Class.forName(repositoryName).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                this.repository = new MemorySessionRepository();
            }
        } else {
            this.repository = new MemorySessionRepository();
        }

        Scheduler.getInstance().schedule(new ExpirationValidator(), new TimeIterator(0, 0, 0), 1000);
    }

    public static SessionManager getInstance() {
        return SingletonHolder.manager;
    }

    public void setSession(String sessionId, Session session) {
        if (this.repository instanceof MemorySessionRepository) {
            sessions.put(sessionId, session);
        }
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
        return this.repository.findById(sessionId);
    }

    public Session getSession(String sessionId, boolean generate) {
        Session session = this.repository.findById(sessionId);
        if (session == null && generate) {
            session = this.repository.create(sessionId);
        }
        return session;
    }

    public void removeSession(String sessionId) {
        Session session = this.getSession(sessionId);
        if (session != null) {
            this.repository.delete(sessionId);
            SessionEvent event = new SessionEvent(SessionEvent.Type.DESTROYED, session);
            listeners.forEach(listener -> listener.onSessionEvent(event));

            if (this.repository instanceof MemorySessionRepository) {
                sessions.remove(sessionId);
            }
        }
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