package org.tinystruct.mcp;

import org.tinystruct.data.component.Builder;
import org.tinystruct.http.Response;
import org.tinystruct.http.SSEPushManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.tinystruct.mcp.MCPSpecification.Events;

/**
 * Handles Server-Sent Events (SSE) connections and event broadcasting using SSEPushManager and SSEClient.
 * Provides enhanced features including connection monitoring, event validation, and metrics collection.
 */
public class SSEHandler {
    private static final Logger LOGGER = Logger.getLogger(SSEHandler.class.getName());
    
    // Constants for rate limiting and monitoring
    private static final long DEFAULT_RATE_LIMIT_WINDOW_MS = 60000L; // 1 minute
    private static final int DEFAULT_MAX_MESSAGES_PER_WINDOW = 1000;
    private static final long CONNECTION_TIMEOUT_MS = 300000L; // 5 minutes
    
    private final SSEPushManager pushManager = SSEPushManager.getInstance();
    private volatile long firstSessionCreatedAt = -1;
    
    // Connection monitoring and metrics
    private final Map<String, ClientInfo> clientInfoMap = new ConcurrentHashMap<>();
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong totalDisconnections = new AtomicLong(0);
    
    // Rate limiting
    private final long rateLimitWindowMs;
    private final int maxMessagesPerWindow;

    /**
     * Constructs SSEHandler with default rate limiting settings.
     */
    public SSEHandler() {
        this(DEFAULT_RATE_LIMIT_WINDOW_MS, DEFAULT_MAX_MESSAGES_PER_WINDOW);
    }

    /**
     * Constructs SSEHandler with custom rate limiting settings.
     * @param rateLimitWindowMs Rate limit window in milliseconds
     * @param maxMessagesPerWindow Maximum messages per window
     */
    public SSEHandler(long rateLimitWindowMs, int maxMessagesPerWindow) {
        this.rateLimitWindowMs = rateLimitWindowMs;
        this.maxMessagesPerWindow = maxMessagesPerWindow;
    }

    /**
     * Registers a new SSE client with validation and monitoring.
     * @param clientId Unique identifier for the client
     * @param response HTTP response object
     * @return true if registration successful, false otherwise
     */
    public boolean registerClient(String clientId, Response response) {
        if (clientId == null || clientId.trim().isEmpty()) {
            LOGGER.warning("Invalid client ID provided for registration");
            return false;
        }
        
        if (response == null) {
            LOGGER.warning("Null response object provided for client: " + clientId);
            return false;
        }
        
        if (firstSessionCreatedAt == -1) {
            firstSessionCreatedAt = System.currentTimeMillis();
        }
        
        try {
            pushManager.register(clientId, response);
            
            // Track client information
            ClientInfo clientInfo = new ClientInfo(clientId, System.currentTimeMillis());
            clientInfoMap.put(clientId, clientInfo);
            totalConnections.incrementAndGet();
            
            LOGGER.info("Successfully registered SSE client: " + clientId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to register SSE client: " + clientId, e);
            return false;
        }
    }

    /**
     * Broadcasts a message to all connected clients with rate limiting.
     * @param message Message content
     * @return Number of clients that received the message
     */
    public int broadcast(Builder message) {
        if (message == null) {
            LOGGER.warning("Attempted to broadcast null message");
            return 0;
        }
        
        try {
            pushManager.broadcast(message);
            int activeClients = getActiveClientCount();
            totalMessagesSent.addAndGet(activeClients);
            LOGGER.fine("Broadcasted message to " + activeClients + " clients");
            return activeClients;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to broadcast message", e);
            return 0;
        }
    }

    /**
     * Sends a message to a specific client with rate limiting and validation.
     * @param clientId Target client ID
     * @param message Message content
     * @return true if message sent successfully, false otherwise
     */
    public boolean pushToClient(String clientId, Builder message) {
        if (!validateClientAndMessage(clientId, message)) {
            return false;
        }
        
        ClientInfo clientInfo = clientInfoMap.get(clientId);
        if (clientInfo == null) {
            LOGGER.warning("Client not found: " + clientId);
            return false;
        }
        
        if (!checkRateLimit(clientInfo)) {
            LOGGER.warning("Rate limit exceeded for client: " + clientId);
            return false;
        }
        
        try {
            pushManager.push(clientId, message);
            clientInfo.incrementMessageCount();
            totalMessagesSent.incrementAndGet();
            LOGGER.fine("Sent message to client: " + clientId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send message to client: " + clientId, e);
            return false;
        }
    }

    /**
     * Sends an SSE event to a specific client with event validation.
     * @param clientId Target client ID
     * @param event Event name (must be a valid MCP event type)
     * @param data Event data
     * @return true if event sent successfully, false otherwise
     */
    public boolean sendEvent(String clientId, String event, String data) {
        if (!validateEventType(event)) {
            LOGGER.warning("Invalid event type: " + event);
            return false;
        }
        
        Builder builder = new Builder();
        builder.put("event", event);
        builder.put("data", data);
        builder.put("timestamp", System.currentTimeMillis());
        
        return pushToClient(clientId, builder);
    }

    /**
     * Sends a standard MCP event to a specific client.
     * @param clientId Target client ID
     * @param eventType Standard MCP event type from MCPSpecification.Events
     * @param data Event data as JSON string
     * @return true if event sent successfully, false otherwise
     */
    public boolean sendMCPEvent(String clientId, String eventType, String data) {
        return sendEvent(clientId, eventType, data);
    }

    /**
     * Sends a connection event to a client.
     * @param clientId Target client ID
     * @param data Additional connection data
     * @return true if event sent successfully, false otherwise
     */
    public boolean sendConnectedEvent(String clientId, String data) {
        return sendMCPEvent(clientId, Events.CONNECTED, data);
    }

    /**
     * Sends an error event to a client.
     * @param clientId Target client ID
     * @param errorMessage Error message
     * @param errorCode Optional error code
     * @return true if event sent successfully, false otherwise
     */
    public boolean sendErrorEvent(String clientId, String errorMessage, String errorCode) {
        Builder errorData = new Builder();
        errorData.put("message", errorMessage);
        if (errorCode != null) {
            errorData.put("code", errorCode);
        }
        errorData.put("timestamp", System.currentTimeMillis());
        
        return sendMCPEvent(clientId, Events.ERROR, errorData.toString());
    }

    /**
     * Sends a notification event to a client.
     * @param clientId Target client ID
     * @param notification Notification message
     * @return true if event sent successfully, false otherwise
     */
    public boolean sendNotification(String clientId, String notification) {
        return sendMCPEvent(clientId, Events.NOTIFICATION, notification);
    }

    /**
     * Closes and removes a specific client connection with cleanup.
     * @param clientId Client ID to disconnect
     * @return true if client was found and removed, false otherwise
     */
    public boolean closeClient(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            return false;
        }
        
        try {
            pushManager.remove(clientId);
            ClientInfo removed = clientInfoMap.remove(clientId);
            if (removed != null) {
                totalDisconnections.incrementAndGet();
                LOGGER.info("Closed client connection: " + clientId);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error closing client: " + clientId, e);
            return false;
        }
    }

    /**
     * Closes all client connections with cleanup.
     * @return Number of clients closed
     */
    public int closeAll() {
        Set<String> clientIds = pushManager.getClientIds();
        int closedCount = 0;
        
        for (String clientId : clientIds.toArray(new String[0])) {
            if (closeClient(clientId)) {
                closedCount++;
            }
        }
        
        LOGGER.info("Closed " + closedCount + " client connections");
        return closedCount;
    }

    /**
     * Sets up SSE headers for a response with additional security headers.
     * @param response HTTP response to configure
     */
    public void setupSSEHeaders(Response response) {
        if (response == null) {
            LOGGER.warning("Attempted to setup headers for null response");
            return;
        }
        
        response.addHeader("Content-Type", "text/event-stream");
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Connection", "keep-alive");
        response.addHeader("X-Accel-Buffering", "no"); // Disable proxy buffering
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers", "Cache-Control");
    }

    /**
     * Gets the timestamp of the first session creation.
     * @return Timestamp of the first session creation
     */
    public long getFirstSessionCreatedAt() {
        return firstSessionCreatedAt == -1 ? System.currentTimeMillis() : firstSessionCreatedAt;
    }

    /**
     * Gets the number of currently active clients.
     * @return Number of active clients
     */
    public int getActiveClientCount() {
        return pushManager.getClientIds().size();
    }

    /**
     * Gets connection statistics.
     * @return Builder containing connection statistics
     */
    public Builder getConnectionStats() {
        Builder stats = new Builder();
        stats.put("activeConnections", getActiveClientCount());
        stats.put("totalConnections", totalConnections.get());
        stats.put("totalDisconnections", totalDisconnections.get());
        stats.put("totalMessagesSent", totalMessagesSent.get());
        stats.put("uptime", System.currentTimeMillis() - getFirstSessionCreatedAt());
        stats.put("firstSessionCreatedAt", getFirstSessionCreatedAt());
        return stats;
    }

    /**
     * Cleans up inactive connections based on timeout.
     * @return Number of connections cleaned up
     */
    public int cleanupInactiveConnections() {
        long currentTime = System.currentTimeMillis();
        int cleanedCount = 0;
        
        for (Map.Entry<String, ClientInfo> entry : clientInfoMap.entrySet()) {
            String clientId = entry.getKey();
            ClientInfo clientInfo = entry.getValue();
            
            if (currentTime - clientInfo.getLastActivity() > CONNECTION_TIMEOUT_MS) {
                if (closeClient(clientId)) {
                    cleanedCount++;
                }
            }
        }
        
        if (cleanedCount > 0) {
            LOGGER.info("Cleaned up " + cleanedCount + " inactive connections");
        }
        
        return cleanedCount;
    }

    /**
     * Validates client ID and message.
     */
    private boolean validateClientAndMessage(String clientId, Builder message) {
        if (clientId == null || clientId.trim().isEmpty()) {
            LOGGER.warning("Invalid client ID: " + clientId);
            return false;
        }
        
        if (message == null) {
            LOGGER.warning("Null message provided for client: " + clientId);
            return false;
        }
        
        return true;
    }

    /**
     * Validates event type against MCP specification.
     */
    private boolean validateEventType(String eventType) {
        if (eventType == null || eventType.trim().isEmpty()) {
            return false;
        }
        
        // Check against standard MCP events
        return eventType.equals(Events.CONNECTED) ||
               eventType.equals(Events.STATE) ||
               eventType.equals(Events.ERROR) ||
               eventType.equals(Events.CLOSE) ||
               eventType.equals(Events.NOTIFICATION) ||
               eventType.equals(Events.RESOURCES_CHANGED) ||
               eventType.equals(Events.TOOLS_CHANGED) ||
               eventType.equals(Events.PROMPTS_CHANGED);
    }

    /**
     * Checks rate limit for a client.
     */
    private boolean checkRateLimit(ClientInfo clientInfo) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - rateLimitWindowMs;
        
        // Remove old message counts outside the window
        clientInfo.removeOldMessages(windowStart);
        
        return clientInfo.getMessageCount() < maxMessagesPerWindow;
    }

    /**
     * Client information for monitoring and rate limiting.
     */
    private static class ClientInfo {
        private final String clientId;
        private final long createdAt;
        private volatile long lastActivity;
        private final Map<Long, Integer> messageCounts = new ConcurrentHashMap<>();

        public ClientInfo(String clientId, long createdAt) {
            this.clientId = clientId;
            this.createdAt = createdAt;
            this.lastActivity = createdAt;
        }

        public void incrementMessageCount() {
            long currentTime = System.currentTimeMillis();
            this.lastActivity = currentTime;
            
            long windowKey = currentTime / 1000; // Round to second
            messageCounts.merge(windowKey, 1, Integer::sum);
        }

        public int getMessageCount() {
            return messageCounts.values().stream().mapToInt(Integer::intValue).sum();
        }

        public void removeOldMessages(long cutoffTime) {
            long cutoffKey = cutoffTime / 1000;
            messageCounts.entrySet().removeIf(entry -> entry.getKey() < cutoffKey);
        }

        public long getLastActivity() {
            return lastActivity;
        }

        public String getClientId() {
            return clientId;
        }

        public long getCreatedAt() {
            return createdAt;
        }
    }
}

