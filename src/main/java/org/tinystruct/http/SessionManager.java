package org.tinystruct.http;

import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;

import java.util.ArrayList;
import java.util.List;

public final class SessionManager implements Monitor<SessionListener> {
    private transient final List<SessionListener> listeners = new ArrayList<>();
    private final SessionRepository repository;

    private SessionManager() {
        SessionRepository sessionRepository;
        Configuration<String> settings = new Settings();
        String repositoryName = settings.get("default.session.repository");
        if (repositoryName != null && !repositoryName.isEmpty()) {
            try {
                sessionRepository = (SessionRepository) Class.forName(repositoryName)
                        .getDeclaredConstructor(SessionManager.class)
                        .newInstance(this);
            } catch (Exception e) {
                sessionRepository = new MemorySessionRepository(this);
            }
        } else {
            sessionRepository = new MemorySessionRepository(this);
        }

        this.repository = sessionRepository;
    }

    public static SessionManager getInstance() {
        return SingletonHolder.manager;
    }

    public void fireEvent(SessionEvent event) {
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
        this.repository.delete(sessionId);
    }

    @Override
    public void addListener(SessionListener listener) {
        this.listeners.add(listener);
    }

    static final class SingletonHolder {
        static final SessionManager manager = new SessionManager();
    }
}
