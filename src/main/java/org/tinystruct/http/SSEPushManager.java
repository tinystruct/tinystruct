package org.tinystruct.http;

import org.tinystruct.data.component.Builder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SSEPushManager manages all SSE client connections and message delivery.
 * It supports both Netty and Servlet/Tomcat environments.
 * Singleton pattern is used for global access.
 */
public class SSEPushManager {
    private static final Logger logger = Logger.getLogger(SSEPushManager.class.getName());

    /**
     * Seconds of inactivity after which a Servlet-mode SSE client is considered stale
     * and a WARNING is logged by the watchdog. Default: 60 seconds.
     */
    public static final int STALE_THRESHOLD_SEC = 60;

    // Map of sessionId -> client (Response for Netty, SSEClient for Servlet)
    private final ConcurrentHashMap<String, Object> clients = new ConcurrentHashMap<>();
    // Epoch-ms registration time per sessionId
    private final ConcurrentHashMap<String, Long> registrationTimes = new ConcurrentHashMap<>();
    // Executor for running SSEClient threads (Servlet/Tomcat only)
    private final ExecutorService executor;
    // Watchdog scheduler that logs stale connections
    private final ScheduledExecutorService watchdog;
    // Indicates if the manager is shutting down
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    // True if running in Netty environment
    private final boolean isNetty;
    // Identifier for push management, useful for distinguishing clients
    private final AtomicReference<String> identifier = new AtomicReference<>("");

    private static final SSEPushManager instance = new SSEPushManager();

    /**
     * Private constructor for singleton pattern.
     * Detects environment and initializes executor if needed.
     */
    protected SSEPushManager() {
        this.isNetty = isNettyEnvironment();
        this.executor = isNetty ? null : Executors.newCachedThreadPool();
        // Start watchdog: check every 30 s for stale connections
        this.watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-watchdog");
            t.setDaemon(true);
            return t;
        });
        this.watchdog.scheduleAtFixedRate(this::checkStaleConnections, 30, 30, TimeUnit.SECONDS);
        logger.info("SSEPushManager initialized");
    }

    /**
     * Get the singleton instance of SSEPushManager.
     *
     * @return The singleton instance
     */
    public static SSEPushManager getInstance() {
        return instance;
    }

    public void setIdentifier(String identifier) {
        this.identifier.set(identifier);
    }

    /**
     * Detect if running in Netty environment by checking for Netty classes in the classpath and stack trace.
     *
     * @return true if Netty is detected, false otherwise
     */
    private boolean isNettyEnvironment() {
        try {
            Class.forName("io.netty.channel.ChannelHandlerContext");
            // Additional check: see if we're actually using Netty
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                if (element.getClassName().contains("netty")) {
                    return true;
                }
            }
            return false;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Register a new SSE client with a session ID and response object.
     * For Netty, stores the response directly. For Servlet, creates and starts an SSEClient thread.
     *
     * @param sessionId Unique session identifier
     * @param out       Response object for sending data
     * @return The registration object (SSEClient for Servlet, Response for Netty) or null if already registered
     */
    public Object register(String sessionId, Response out) {
        return register(sessionId, out, null);
    }

    /**
     * Register a new SSE client with a custom message queue (Servlet only).
     *
     * @param sessionId    Unique session identifier
     * @param out          Response object for sending data
     * @param messageQueue Custom message queue (optional)
     * @return The registration object (SSEClient for Servlet, Response for Netty) or null if already registered
     */
    public Object register(String sessionId, Response out, BlockingQueue<Builder> messageQueue) {
        if (isShutdown.get()) {
            logger.log(Level.WARNING, "SSEPushManager is shutting down, cannot register new client");
            return null;
        }

        final String finalSessionId = identifier.get() + sessionId;
        if (isNetty) {
            // Netty: just store the response directly if not present
            if (clients.putIfAbsent(finalSessionId, out) == null) {
                registrationTimes.put(finalSessionId, System.currentTimeMillis());
                logger.info("Registered Netty SSE client: " + finalSessionId);
                return out;
            }
            return null; // Already registered
        } else {
            // Servlet/Tomcat: use original thread-based approach
            final AtomicBoolean isNew = new AtomicBoolean(false);
            SSEClient client = (SSEClient) clients.compute(finalSessionId, (id, existing) -> {
                if (existing != null && ((SSEClient) existing).isActive()) {
                    // keep existing active client
                    return existing;
                }
                isNew.set(true);
                SSEClient c = (messageQueue != null) ? new SSEClient(out, messageQueue) : new SSEClient(out);
                executor.submit(c);
                return c;
            });

            if (isNew.get()) {
                registrationTimes.put(finalSessionId, System.currentTimeMillis());
                logger.info("Registered a SSE client: " + finalSessionId);
                return client;
            }

            return null;
        }
    }

    /**
     * Push a message to a specific client by session ID.
     * For Netty, writes and flushes immediately. For Servlet, enqueues the message.
     *
     * @param sessionId The target client session ID
     * @param message   The message to send
     */
    public void push(String sessionId, Builder message) {
        if (isShutdown.get()) {
            logger.log(Level.WARNING, "SSEPushManager is shutting down, cannot push message");
            return;
        }

        sessionId = identifier.get() + sessionId;
        if (isNetty) {
            // Netty: write and flush immediately
            Response out = (Response) clients.get(sessionId);
            if (out != null) {
                try {
                    String event = formatSSEMessage(message);
                    out.writeAndFlush(event.getBytes(StandardCharsets.UTF_8));
                    logger.fine("Pushed message to Netty client: " + sessionId);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to push message to Netty client " + sessionId + ": " + e.getMessage());
                    clients.remove(sessionId, out);
                }
            } else {
                // Clean up inactive client
                clients.remove(sessionId);
            }
        } else {
            // Servlet/Tomcat: use original queue-based approach
            SSEClient client = (SSEClient) clients.get(sessionId);
            if (client != null && client.isActive()) {
                client.send(message);
                logger.fine("Pushed message to client: " + sessionId);
            } else {
                // Clean up inactive client
                clients.remove(sessionId, client);
            }
        }
    }

    /**
     * Broadcast a message to all connected clients.
     * For Netty, writes and flushes to all. For Servlet, enqueues for all active clients.
     *
     * @param message The message to broadcast
     */
    public void broadcast(Builder message) {
        if (isShutdown.get()) {
            logger.log(Level.WARNING, "SSEPushManager is shutting down, cannot broadcast message");
            return;
        }

        // Collect stale keys separately; never mutate clients inside forEach.
        List<String> staleKeys = new ArrayList<>();
        if (isNetty) {
            String event = formatSSEMessage(message);
            clients.forEach((sessionId, clientObj) -> {
                Response out = (Response) clientObj;
                try {
                    out.writeAndFlush(event.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to broadcast to Netty client " + sessionId + ": " + e.getMessage());
                    staleKeys.add(sessionId);
                }
            });
        } else {
            // Servlet/Tomcat: broadcast to all clients
            clients.forEach((sessionId, clientObj) -> {
                SSEClient client = (SSEClient) clientObj;
                if (client.isActive()) {
                    client.send(message);
                } else {
                    staleKeys.add(sessionId);
                }
            });
        }

        // Safe removal after iteration is complete
        for (String key : staleKeys) {
            clients.remove(key);
        }
    }

    /**
     * Remove and close a client connection by session ID.
     *
     * @param sessionId The client session ID to remove
     */
    public void remove(String sessionId) {
        sessionId = identifier.get() + sessionId;
        registrationTimes.remove(sessionId);
        Object clientObj = clients.remove(sessionId);
        if (clientObj != null) {
            if (isNetty) {
                // Netty: close the response
                Response out = (Response) clientObj;
                try {
                    out.close();
                } catch (org.tinystruct.ApplicationException e) {
                    throw new RuntimeException(e);
                }
            } else {
                // Servlet/Tomcat: close the client
                SSEClient client = (SSEClient) clientObj;
                client.close();
            }
            logger.info("Removed SSE client: " + sessionId);
        }
    }

    /**
     * Get the set of all connected client session IDs.
     *
     * @return Set of session IDs
     */
    public Set<String> getClientIds() {
        return clients.keySet();
    }

    /**
     * Shutdown the SSEPushManager, closing all clients and cleaning up resources.
     */
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            // Stop watchdog first
            watchdog.shutdownNow();

            // Close all clients
            if (isNetty) {
                clients.forEach((sessionId, clientObj) -> {
                    Response out = (Response) clientObj;
                    try {
                        out.close();
                    } catch (org.tinystruct.ApplicationException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                clients.forEach((sessionId, clientObj) -> {
                    SSEClient client = (SSEClient) clientObj;
                    client.close();
                });
            }
            clients.clear();
            registrationTimes.clear();

            // Shutdown executor (only for Servlet/Tomcat)
            if (executor != null) {
                executor.shutdown();
            }

            logger.info("SSEPushManager shutdown complete");
        }
    }

    /**
     * Returns a snapshot of connection statistics for monitoring purposes.
     * Each entry maps a session ID to a two-element long array:
     * {@code [registrationTimeMs, lastActivityTimeMs]}.
     * For Netty clients, {@code lastActivityTimeMs} is -1 (not tracked per-client).
     *
     * @return unmodifiable map of sessionId → [registeredAt, lastActivity]
     */
    public Map<String, long[]> getConnectionStats() {
        Map<String, long[]> stats = new LinkedHashMap<>();
        clients.forEach((sessionId, clientObj) -> {
            long registered = registrationTimes.getOrDefault(sessionId, -1L);
            long lastActivity;
            if (!isNetty && clientObj instanceof SSEClient) {
                lastActivity = ((SSEClient) clientObj).getLastActivityTime();
            } else {
                lastActivity = -1L;
            }
            stats.put(sessionId, new long[]{registered, lastActivity});
        });
        return Collections.unmodifiableMap(stats);
    }

    /**
     * Watchdog task: logs a WARNING for every Servlet-mode SSE client whose
     * last activity (message or heartbeat) is older than {@link #STALE_THRESHOLD_SEC}.
     * Also cleans up clients that are no longer active.
     */
    private void checkStaleConnections() {
        if (isNetty || isShutdown.get()) return;
        long now = System.currentTimeMillis();
        long staleMs = STALE_THRESHOLD_SEC * 1000L;
        List<String> staleKeys = new ArrayList<>();
        clients.forEach((sessionId, clientObj) -> {
            if (!(clientObj instanceof SSEClient)) return;
            SSEClient client = (SSEClient) clientObj;
            if (!client.isActive()) {
                staleKeys.add(sessionId);
                return;
            }
            long idle = now - client.getLastActivityTime();
            if (idle > staleMs) {
                logger.warning(String.format(
                        "SSEPushManager watchdog: client '%s' has been idle for %d s (threshold %d s). "
                        + "It may be stuck. Registered at: %s",
                        sessionId, idle / 1000, STALE_THRESHOLD_SEC,
                        new java.util.Date(registrationTimes.getOrDefault(sessionId, -1L))));
            }
        });
        // Remove inactive clients discovered during scan
        for (String key : staleKeys) {
            clients.remove(key);
            registrationTimes.remove(key);
            logger.info("SSEPushManager watchdog: removed inactive client '" + key + "'");
        }
    }

    /**
     * Formats a message as an SSE event string.
     *
     * @param message The message to format
     * @return The formatted SSE event string
     */
    public static String formatSSEMessage(Builder message) {
        String type = message.get("type") != null ? message.get("type").toString() : null;
        if ("connect".equals(type)) {
            return "event: connect\ndata: Connected\n\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("data: ").append(message).append("\n\n");
        return sb.toString();
    }
}