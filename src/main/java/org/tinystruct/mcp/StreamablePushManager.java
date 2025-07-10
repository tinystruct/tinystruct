package org.tinystruct.mcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * StreamablePushManager manages server-to-client streaming (SSE-style) for Streamable HTTP MCP.
 * Supports event IDs, resumability, and per-session event queues.
 */
public class StreamablePushManager {
    // Singleton instance
    private static final StreamablePushManager instance = new StreamablePushManager();
    public static StreamablePushManager getInstance() { return instance; }

    // Session ID -> SessionInfo
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    // Global event ID counter
    private final AtomicLong globalEventId = new AtomicLong(1);

    private StreamablePushManager() {}

    /** Register a new session for streaming. */
    public void registerSession(String sessionId) {
        sessions.putIfAbsent(sessionId, new SessionInfo(sessionId));
    }

    /** Unregister a session. */
    public void unregisterSession(String sessionId) {
        sessions.remove(sessionId);
    }

    /** Send an event to a specific session. */
    public void sendEvent(String sessionId, String event, String data) {
        SessionInfo info = sessions.get(sessionId);
        if (info != null) {
            long eventId = globalEventId.getAndIncrement();
            info.addEvent(new StreamableEvent(eventId, event, data));
        }
    }

    /** Broadcast an event to all sessions. */
    public void broadcastEvent(String event, String data) {
        long eventId = globalEventId.getAndIncrement();
        StreamableEvent evt = new StreamableEvent(eventId, event, data);
        for (SessionInfo info : sessions.values()) {
            info.addEvent(evt);
        }
    }

    /** Get missed events for a session since a given event ID. */
    public List<StreamableEvent> getMissedEvents(String sessionId, long lastEventId) {
        SessionInfo info = sessions.get(sessionId);
        if (info == null) return new ArrayList<>();
        return info.getEventsSince(lastEventId);
    }

    /** Get and clear all pending events for a session. */
    public List<StreamableEvent> drainEvents(String sessionId) {
        SessionInfo info = sessions.get(sessionId);
        if (info == null) return new ArrayList<>();
        return info.drainEvents();
    }

    /** Session info with event queue and last event ID. */
    private static class SessionInfo {
        private final String sessionId;
        private final List<StreamableEvent> eventQueue = new ArrayList<>();
        private long lastEventId = 0;
        private final ReentrantLock lock = new ReentrantLock();
        public SessionInfo(String sessionId) { this.sessionId = sessionId; }
        public void addEvent(StreamableEvent event) {
            lock.lock();
            try {
                eventQueue.add(event);
                lastEventId = event.id;
            } finally {
                lock.unlock();
            }
        }
        public List<StreamableEvent> getEventsSince(long eventId) {
            lock.lock();
            try {
                List<StreamableEvent> result = new ArrayList<>();
                for (StreamableEvent evt : eventQueue) {
                    if (evt.id > eventId) result.add(evt);
                }
                return result;
            } finally {
                lock.unlock();
            }
        }
        public List<StreamableEvent> drainEvents() {
            lock.lock();
            try {
                List<StreamableEvent> result = new ArrayList<>(eventQueue);
                eventQueue.clear();
                return result;
            } finally {
                lock.unlock();
            }
        }
    }

    /** Event structure for streaming. */
    public static class StreamableEvent {
        public final long id;
        public final String event;
        public final String data;
        public StreamableEvent(long id, String event, String data) {
            this.id = id;
            this.event = event;
            this.data = data;
        }
    }
} 