package org.tinystruct.mcp;

import org.tinystruct.data.component.Builder;
import org.tinystruct.http.Response;
import org.tinystruct.http.SSEPushManager;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Handles Server-Sent Events (SSE) connections and event broadcasting using SSEPushManager and SSEClient.
 */
public class SSEHandler {
    private static final Logger LOGGER = Logger.getLogger(SSEHandler.class.getName());
    private final SSEPushManager pushManager = SSEPushManager.getInstance();
    private volatile long firstSessionCreatedAt = -1;

    /**
     * Registers a new SSE client.
     * @param clientId Unique identifier for the client
     * @param response HTTP response object
     */
    public void registerClient(String clientId, Response response) {
        if (firstSessionCreatedAt == -1) {
            firstSessionCreatedAt = System.currentTimeMillis();
        }
        pushManager.register(clientId, response);
    }

    /**
     * Broadcasts a message to all connected clients.
     * @param message Message content
     */
    public void broadcast(Builder message) {
        pushManager.broadcast(message);
    }

    /**
     * Sends a message to a specific client.
     * @param clientId Target client ID
     * @param message Message content
     */
    public void pushToClient(String clientId, Builder message) {
        pushManager.push(clientId, message);
    }

    /**
     * Sends an SSE event to a specific client.
     * @param clientId Target client ID
     * @param event Event name
     * @param data Event data
     */
    public void sendEvent(String clientId, String event, String data) {
        Builder builder = new Builder();
        builder.put("event", event);
        builder.put("data", data);
        pushToClient(clientId, builder);
    }

    /**
     * Closes and removes a specific client connection.
     * @param clientId Client ID to disconnect
     */
    public void closeClient(String clientId) {
        pushManager.remove(clientId);
    }

    /**
     * Closes all client connections.
     */
    public void closeAll() {
        Set<String> clientIds = pushManager.getClientIds();
        for (String clientId : clientIds.toArray(new String[0])) {
            pushManager.remove(clientId);
        }
    }

    /**
     * Sets up SSE headers for a response.
     * @param response HTTP response to configure
     */
    public void setupSSEHeaders(Response response) {
        response.addHeader("Content-Type", "text/event-stream");
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Connection", "keep-alive");
    }

    /**
     * Gets the timestamp of the first session creation.
     * @return Timestamp of the first session creation
     */
    public long getFirstSessionCreatedAt() {
        return firstSessionCreatedAt == -1 ? System.currentTimeMillis() : firstSessionCreatedAt;
    }
}

