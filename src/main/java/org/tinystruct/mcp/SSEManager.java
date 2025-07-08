package org.tinystruct.mcp;

import org.tinystruct.data.component.Builder;
import org.tinystruct.http.Response;
import org.tinystruct.http.SSEPushManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.tinystruct.mcp.MCPSpecification.Events;

/**
 * Advanced SSE Manager for MCP applications providing connection pooling,
 * event filtering, and enhanced monitoring capabilities.
 * 
 * This class extends the basic SSEHandler functionality with:
 * - Connection pooling and load balancing
 * - Event filtering and routing
 * - Enhanced monitoring and metrics
 * - Automatic cleanup and health checks
 * - Event replay capabilities
 */
public class SSEManager {
    private static final Logger LOGGER = Logger.getLogger(SSEManager.class.getName());
    
    // Constants for connection management
    private static final long HEALTH_CHECK_INTERVAL_MS = 30000L; // 30 seconds
    private static final long CONNECTION_CLEANUP_INTERVAL_MS = 60000L; // 1 minute
    private static final int MAX_CONNECTIONS_PER_POOL = 100;
    private static final int MAX_EVENT_HISTORY_SIZE = 1000;
    
    private final SSEHandler sseHandler;
    private final SSEPushManager pushManager;
    private final ScheduledExecutorService scheduler;
    
    // Connection pools by client type/category
    private final Map<String, ConnectionPool> connectionPools = new ConcurrentHashMap<>();
    
    // Event history for replay
    private final Map<String, EventHistory> eventHistory = new ConcurrentHashMap<>();
    
    // Event filters
    private final Map<String, Predicate<Builder>> eventFilters = new ConcurrentHashMap<>();
    
    // Metrics
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsFiltered = new AtomicLong(0);
    private final AtomicLong totalEventsReplayed = new AtomicLong(0);
    
    private volatile boolean shutdown = false;

    /**
     * Constructs SSEManager with default settings.
     */
    public SSEManager() {
        this(new SSEHandler());
    }

    /**
     * Constructs SSEManager with custom SSEHandler.
     * @param sseHandler Custom SSEHandler instance
     */
    public SSEManager(SSEHandler sseHandler) {
        this.sseHandler = sseHandler;
        this.pushManager = SSEPushManager.getInstance();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        startHealthChecks();
        LOGGER.info("SSEManager initialized");
    }

    /**
     * Registers a client in a specific connection pool.
     * @param clientId Unique client identifier
     * @param response HTTP response object
     * @param poolName Connection pool name
     * @return true if registration successful
     */
    public boolean registerClient(String clientId, Response response, String poolName) {
        if (shutdown) {
            LOGGER.warning("SSEManager is shutdown, cannot register client");
            return false;
        }
        
        ConnectionPool pool = connectionPools.computeIfAbsent(poolName, 
            k -> new ConnectionPool(k, MAX_CONNECTIONS_PER_POOL));
        
        if (pool.addClient(clientId, response)) {
            boolean success = sseHandler.registerClient(clientId, response);
            if (success) {
                // Send welcome event
                sendWelcomeEvent(clientId, poolName);
                LOGGER.info("Registered client " + clientId + " in pool " + poolName);
            }
            return success;
        }
        
        LOGGER.warning("Connection pool " + poolName + " is full");
        return false;
    }

    /**
     * Registers a client in the default pool.
     * @param clientId Unique client identifier
     * @param response HTTP response object
     * @return true if registration successful
     */
    public boolean registerClient(String clientId, Response response) {
        return registerClient(clientId, response, "default");
    }

    /**
     * Broadcasts an event to all clients with optional filtering.
     * @param event Event to broadcast
     * @param filterName Optional filter name to apply
     * @return Number of clients that received the event
     */
    public int broadcastEvent(Builder event, String filterName) {
        if (shutdown || event == null) {
            return 0;
        }
        
        totalEventsProcessed.incrementAndGet();
        
        // Apply filter if specified
        if (filterName != null) {
            Predicate<Builder> filter = eventFilters.get(filterName);
            if (filter != null && !filter.test(event)) {
                totalEventsFiltered.incrementAndGet();
                LOGGER.fine("Event filtered out by filter: " + filterName);
                return 0;
            }
        }
        
        // Store in history for replay
        storeEventInHistory(event);
        
        return sseHandler.broadcast(event);
    }

    /**
     * Broadcasts an event to all clients.
     * @param event Event to broadcast
     * @return Number of clients that received the event
     */
    public int broadcastEvent(Builder event) {
        return broadcastEvent(event, null);
    }

    /**
     * Sends an event to a specific client.
     * @param clientId Target client ID
     * @param event Event to send
     * @return true if event sent successfully
     */
    public boolean sendEventToClient(String clientId, Builder event) {
        if (shutdown || event == null) {
            return false;
        }
        
        totalEventsProcessed.incrementAndGet();
        storeEventInHistory(event);
        
        return sseHandler.pushToClient(clientId, event);
    }

    /**
     * Sends a standard MCP event to a client.
     * @param clientId Target client ID
     * @param eventType MCP event type
     * @param data Event data
     * @return true if event sent successfully
     */
    public boolean sendMCPEvent(String clientId, String eventType, String data) {
        return sseHandler.sendMCPEvent(clientId, eventType, data);
    }

    /**
     * Broadcasts a standard MCP event to all clients.
     * @param eventType MCP event type
     * @param data Event data
     * @return Number of clients that received the event
     */
    public int broadcastMCPEvent(String eventType, String data) {
        Builder event = new Builder();
        event.put("event", eventType);
        event.put("data", data);
        event.put("timestamp", System.currentTimeMillis());
        
        return broadcastEvent(event);
    }

    /**
     * Replays recent events to a client.
     * @param clientId Target client ID
     * @param eventType Optional event type filter
     * @param maxEvents Maximum number of events to replay
     * @return Number of events replayed
     */
    public int replayEvents(String clientId, String eventType, int maxEvents) {
        if (shutdown) {
            return 0;
        }
        
        EventHistory history = eventHistory.get(eventType != null ? eventType : "all");
        if (history == null) {
            return 0;
        }
        
        int replayedCount = 0;
        for (Builder event : history.getRecentEvents(maxEvents)) {
            if (sseHandler.pushToClient(clientId, event)) {
                replayedCount++;
                totalEventsReplayed.incrementAndGet();
            }
        }
        
        LOGGER.info("Replayed " + replayedCount + " events to client " + clientId);
        return replayedCount;
    }

    /**
     * Adds an event filter.
     * @param filterName Filter name
     * @param filter Filter predicate
     */
    public void addEventFilter(String filterName, Predicate<Builder> filter) {
        eventFilters.put(filterName, filter);
        LOGGER.info("Added event filter: " + filterName);
    }

    /**
     * Removes an event filter.
     * @param filterName Filter name to remove
     */
    public void removeEventFilter(String filterName) {
        eventFilters.remove(filterName);
        LOGGER.info("Removed event filter: " + filterName);
    }

    /**
     * Gets connection statistics for all pools.
     * @return Statistics builder
     */
    public Builder getPoolStatistics() {
        Builder stats = new Builder();
        stats.put("totalPools", connectionPools.size());
        stats.put("totalConnections", getTotalConnectionCount());
        stats.put("totalEventsProcessed", totalEventsProcessed.get());
        stats.put("totalEventsFiltered", totalEventsFiltered.get());
        stats.put("totalEventsReplayed", totalEventsReplayed.get());
        
        Builder poolStats = new Builder();
        for (Map.Entry<String, ConnectionPool> entry : connectionPools.entrySet()) {
            poolStats.put(entry.getKey(), entry.getValue().getStatistics());
        }
        stats.put("pools", poolStats);
        
        return stats;
    }

    /**
     * Gets the number of active connections across all pools.
     * @return Total connection count
     */
    public int getTotalConnectionCount() {
        return connectionPools.values().stream()
                .mapToInt(ConnectionPool::getActiveConnectionCount)
                .sum();
    }

    /**
     * Closes a specific client connection.
     * @param clientId Client ID to close
     * @return true if client was found and closed
     */
    public boolean closeClient(String clientId) {
        // Remove from all pools
        for (ConnectionPool pool : connectionPools.values()) {
            if (pool.removeClient(clientId)) {
                break;
            }
        }
        
        return sseHandler.closeClient(clientId);
    }

    /**
     * Closes all connections and shuts down the manager.
     */
    public void shutdown() {
        if (shutdown) {
            return;
        }
        
        shutdown = true;
        
        // Close all connections
        int closedCount = sseHandler.closeAll();
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LOGGER.info("SSEManager shutdown complete. Closed " + closedCount + " connections");
    }

    /**
     * Starts health check and cleanup tasks.
     */
    private void startHealthChecks() {
        // Health check task
        scheduler.scheduleAtFixedRate(() -> {
            if (!shutdown) {
                performHealthCheck();
            }
        }, HEALTH_CHECK_INTERVAL_MS, HEALTH_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
        
        // Cleanup task
        scheduler.scheduleAtFixedRate(() -> {
            if (!shutdown) {
                performCleanup();
            }
        }, CONNECTION_CLEANUP_INTERVAL_MS, CONNECTION_CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Performs health check on all connections.
     */
    private void performHealthCheck() {
        try {
            int inactiveConnections = sseHandler.cleanupInactiveConnections();
            if (inactiveConnections > 0) {
                LOGGER.info("Health check: cleaned up " + inactiveConnections + " inactive connections");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during health check", e);
        }
    }

    /**
     * Performs cleanup tasks.
     */
    private void performCleanup() {
        try {
            // Clean up old event history
            for (EventHistory history : eventHistory.values()) {
                history.cleanup();
            }
            
            // Clean up empty pools
            connectionPools.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during cleanup", e);
        }
    }

    /**
     * Stores an event in history for replay.
     */
    private void storeEventInHistory(Builder event) {
        String eventType = (String) event.get("event");
        if (eventType != null) {
            eventHistory.computeIfAbsent(eventType, k -> new EventHistory(MAX_EVENT_HISTORY_SIZE))
                    .addEvent(event);
        }
        
        // Also store in "all" history
        eventHistory.computeIfAbsent("all", k -> new EventHistory(MAX_EVENT_HISTORY_SIZE))
                .addEvent(event);
    }

    /**
     * Sends a welcome event to a newly connected client.
     */
    private void sendWelcomeEvent(String clientId, String poolName) {
        Builder welcomeData = new Builder();
        welcomeData.put("clientId", clientId);
        welcomeData.put("pool", poolName);
        welcomeData.put("timestamp", System.currentTimeMillis());
        
        sseHandler.sendMCPEvent(clientId, Events.CONNECTED, welcomeData.toString());
    }

    /**
     * Connection pool for managing groups of clients.
     */
    private static class ConnectionPool {
        private final String name;
        private final int maxConnections;
        private final Map<String, Long> clients = new ConcurrentHashMap<>();
        private final AtomicLong totalConnections = new AtomicLong(0);
        private final AtomicLong totalDisconnections = new AtomicLong(0);

        public ConnectionPool(String name, int maxConnections) {
            this.name = name;
            this.maxConnections = maxConnections;
        }

        public boolean addClient(String clientId, Response response) {
            if (clients.size() >= maxConnections) {
                return false;
            }
            
            clients.put(clientId, System.currentTimeMillis());
            totalConnections.incrementAndGet();
            return true;
        }

        public boolean removeClient(String clientId) {
            if (clients.remove(clientId) != null) {
                totalDisconnections.incrementAndGet();
                return true;
            }
            return false;
        }

        public int getActiveConnectionCount() {
            return clients.size();
        }

        public boolean isEmpty() {
            return clients.isEmpty();
        }

        public Builder getStatistics() {
            Builder stats = new Builder();
            stats.put("name", name);
            stats.put("activeConnections", getActiveConnectionCount());
            stats.put("maxConnections", maxConnections);
            stats.put("totalConnections", totalConnections.get());
            stats.put("totalDisconnections", totalDisconnections.get());
            return stats;
        }
    }

    /**
     * Event history for replay functionality.
     */
    private static class EventHistory {
        private final int maxSize;
        private final java.util.Queue<Builder> events = new java.util.concurrent.ConcurrentLinkedQueue<>();

        public EventHistory(int maxSize) {
            this.maxSize = maxSize;
        }

        public void addEvent(Builder event) {
            events.offer(event);
            
            // Remove oldest events if we exceed max size
            while (events.size() > maxSize) {
                events.poll();
            }
        }

        public java.util.List<Builder> getRecentEvents(int maxEvents) {
            return events.stream()
                    .limit(Math.min(maxEvents, events.size()))
                    .collect(java.util.stream.Collectors.toList());
        }

        public void cleanup() {
            // Remove events older than 1 hour
            long cutoffTime = System.currentTimeMillis() - 3600000L;
            events.removeIf(event -> {
                Long timestamp = (Long) event.get("timestamp");
                return timestamp != null && timestamp < cutoffTime;
            });
        }
    }
}
