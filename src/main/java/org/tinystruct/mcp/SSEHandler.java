package org.tinystruct.mcp;

import org.tinystruct.http.Response;
import org.tinystruct.data.component.Builder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Handles Server-Sent Events (SSE) connections and event broadcasting
 */
public class SSEHandler {
    private static final Logger LOGGER = Logger.getLogger(SSEHandler.class.getName());
    private final ConcurrentHashMap<String, SSESession> sseClients = new ConcurrentHashMap<>();

    /**
     * Creates a new SSE session for a client
     * @param clientId Unique identifier for the client
     * @return The created SSE session
     */
    public SSESession createSession(String clientId) {
        SSESession session = new SSESession(clientId);
        sseClients.put(clientId, session);
        return session;
    }

    /**
     * Broadcasts an event to all connected clients
     * @param event Event type
     * @param data Event data
     */
    public void broadcastEvent(String event, String data) {
        sseClients.values().forEach(session -> session.sendEvent(event, data));
    }

    /**
     * Broadcasts an event to a specific client
     * @param clientId Target client ID
     * @param event Event type
     * @param data Event data
     */
    public void sendEventToClient(String clientId, String event, String data) {
        SSESession session = sseClients.get(clientId);
        if (session != null) {
            session.sendEvent(event, data);
        }
    }

    /**
     * Closes all SSE connections
     */
    public void closeAllConnections() {
        sseClients.values().forEach(SSESession::close);
        sseClients.clear();
    }

    /**
     * Closes a specific client's SSE connection
     * @param clientId Client ID to disconnect
     */
    public void closeConnection(String clientId) {
        SSESession session = sseClients.remove(clientId);
        if (session != null) {
            session.close();
        }
    }

    /**
     * Sets up SSE headers for a response
     * @param response HTTP response to configure
     */
    public void setupSSEHeaders(Response response) {
        response.addHeader("Content-Type", MCPSpecification.Http.CONTENT_TYPE_SSE);
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Connection", "keep-alive");
    }

    /**
     * Formats an SSE response message
     * @param event Event type
     * @param data Event data
     * @param id Optional event ID
     * @return Formatted SSE message
     */
    public String formatSSEResponse(String event, String data, String id) {
        StringBuilder response = new StringBuilder();
        if (event != null) {
            response.append("event: ").append(event).append("\n");
        }
        if (id != null) {
            response.append("id: ").append(id).append("\n");
        }
        response.append("data: ").append(data).append("\n\n");
        return response.toString();
    }

    /**
     * Gets the creation timestamp of the first active session
     * @return Creation timestamp in milliseconds, or current time if no sessions exist
     */
    public long getFirstSessionCreatedAt() {
        return sseClients.values().stream()
                .findFirst()
                .map(s -> s.createdAt)
                .orElse(System.currentTimeMillis());
    }

    /**
     * Represents a Server-Sent Events session
     */
    public static class SSESession {
        private final String id;
        private final StringBuilder output;
        private boolean isActive = true;
        private final long createdAt = System.currentTimeMillis();
        private long lastActivityAt = System.currentTimeMillis();

        public SSESession(String id) {
            this.id = id;
            this.output = new StringBuilder();
        }

        public String getOutput() {
            return output.toString();
        }

        public void sendEvent(String event, String data) {
            if (isActive) {
                output.append("event: ").append(event).append("\n");
                output.append("data: ").append(data).append("\n\n");
            }
        }

        public void sendStateChange(MCPSpecification.SessionState newState) {
            Builder data = new Builder();
            data.put("type", "state_change");
            data.put("state", newState.toString());
            sendEvent(MCPSpecification.Events.STATE, data.toString());
        }

        public void sendError(String errorMessage, int errorCode) {
            Builder data = new Builder();
            data.put("type", "error");
            data.put("code", errorCode);
            data.put("message", errorMessage);
            sendEvent(MCPSpecification.Events.ERROR, data.toString());
        }

        public void close() {
            if (isActive) {
                sendEvent(MCPSpecification.Events.CLOSE, "{\"reason\":\"session_closed\"}");
                isActive = false;
            }
        }

        public void updateActivity() {
            this.lastActivityAt = System.currentTimeMillis();
        }
        
        public boolean isExpired(long timeoutMs) {
            return System.currentTimeMillis() - lastActivityAt > timeoutMs;
        }
    }
} 