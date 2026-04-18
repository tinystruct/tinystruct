package org.tinystruct.http;

import org.tinystruct.system.scheduling.Scheduler;
import org.tinystruct.system.scheduling.TimeIterator;

import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process {@link SessionRepository} backed by a {@link ConcurrentHashMap}.
 */
public class MemorySessionRepository implements SessionRepository {

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final SessionManager manager;

    public MemorySessionRepository(SessionManager sessionManager) {
        this.manager = sessionManager;
        Scheduler.getInstance().schedule(new ExpirationValidator(), new TimeIterator(0, 0, 0), 1000);
    }

    @Override
    public Session findById(String sessionId) {
        return sessions.get(sessionId);
    }

    @Override
    public Session create(String sessionId) {
        Session session = new MemorySession(sessionId);
        sessions.put(sessionId, session);
        manager.fireEvent(new SessionEvent(SessionEvent.Type.CREATED, session));
        return session;
    }

    @Override
    public void delete(String sessionId) {
        Session session = findById(sessionId);
        sessions.remove(sessionId);
        manager.fireEvent(new SessionEvent(SessionEvent.Type.DESTROYED, session));
    }

    private final class ExpirationValidator extends TimerTask {
        @Override
        public void run() {
            sessions.forEach((sessionId, session) -> {
                if (session.isExpired()) {
                    delete(sessionId);
                }
            });
        }
    }
}
