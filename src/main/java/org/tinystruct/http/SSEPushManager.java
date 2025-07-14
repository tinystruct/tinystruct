package org.tinystruct.http;

import org.tinystruct.data.component.Builder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SSEPushManager manages all SSE client connections and message delivery.
 * It supports both Netty and Servlet/Tomcat environments.
 * Singleton pattern is used for global access.
 */
public class SSEPushManager {
    private static final Logger logger = Logger.getLogger(SSEPushManager.class.getName());
    // Map of sessionId to client (Response for Netty, SSEClient for Servlet)
    private final ConcurrentHashMap<String, Object> clients = new ConcurrentHashMap<>();
    // Executor for running SSEClient threads (Servlet/Tomcat only)
    private final ExecutorService executor;
    // Indicates if the manager is shutting down
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    // True if running in Netty environment
    private final boolean isNetty;
    // Identifier for push management, useful for distinguishing clients
    private String identifier = "";

    private static final SSEPushManager instance = new SSEPushManager();

    /**
     * Private constructor for singleton pattern.
     * Detects environment and initializes executor if needed.
     */
    protected SSEPushManager() {
        this.isNetty = isNettyEnvironment();
        this.executor = isNetty ? null : Executors.newCachedThreadPool();
        logger.info("SSEPushManager initialized for " + (isNetty ? "Netty" : "Servlet/Tomcat") + " environment");
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
        this.identifier = identifier;
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
     * @return The SSEClient instance (Servlet) or null (Netty)
     */
    public SSEClient register(String sessionId, Response out) {
        return register(sessionId, out, null);
    }

    /**
     * Register a new SSE client with a custom message queue (Servlet only).
     *
     * @param sessionId    Unique session identifier
     * @param out          Response object for sending data
     * @param messageQueue Custom message queue (optional)
     * @return The SSEClient instance (Servlet) or null (Netty)
     */
    public SSEClient register(String sessionId, Response out, BlockingQueue<Builder> messageQueue) {
        if (isShutdown.get()) {
            logger.log(Level.WARNING, "SSEPushManager is shutting down, cannot register new client");
            return null;
        }

        sessionId = identifier + sessionId;
        if (isNetty) {
            // Netty: just store the response directly
            clients.put(sessionId, out);
            logger.info("Registered Netty SSE client: " + sessionId);
            return null; // No SSEClient needed for Netty
        } else {
            // Servlet/Tomcat: use original thread-based approach
            SSEClient oldClient = (SSEClient) clients.get(sessionId);
            if (oldClient != null) {
                return oldClient;
            }

            SSEClient client = messageQueue != null ?
                    new SSEClient(out, messageQueue) :
                    new SSEClient(out);

            clients.put(sessionId, client);
            executor.submit(client);
            logger.info("Registered Servlet SSE client: " + sessionId);
            return client;
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

        sessionId = identifier + sessionId;
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
                logger.fine("Pushed message to Servlet client: " + sessionId);
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

        if (isNetty) {
            // Netty: broadcast to all clients
            String event = formatSSEMessage(message);
            clients.forEach((sessionId, clientObj) -> {
                Response out = (Response) clientObj;
                try {
                    out.writeAndFlush(event.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to broadcast to Netty client " + sessionId + ": " + e.getMessage());
                    clients.remove(sessionId, clientObj);
                }
            });
        } else {
            // Servlet/Tomcat: broadcast to all clients
            clients.forEach((sessionId, clientObj) -> {
                SSEClient client = (SSEClient) clientObj;
                if (client.isActive()) {
                    client.send(message);
                } else {
                    // Clean up inactive client
                    clients.remove(sessionId, client);
                }
            });
        }
    }

    /**
     * Remove and close a client connection by session ID.
     *
     * @param sessionId The client session ID to remove
     */
    public void remove(String sessionId) {
        sessionId = identifier + sessionId;
        Object clientObj = clients.remove(sessionId);
        if (clientObj != null) {
            if (isNetty) {
                // Netty: close the response
                Response out = (Response) clientObj;
                try {
                    out.close();
                } catch (IOException e) {
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
            // Close all clients
            if (isNetty) {
                clients.forEach((sessionId, clientObj) -> {
                    Response out = (Response) clientObj;
                    try {
                        out.close();
                    } catch (IOException e) {
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

            // Shutdown executor (only for Servlet/Tomcat)
            if (executor != null) {
                executor.shutdown();
            }

            logger.info("SSEPushManager shutdown complete");
        }
    }

    /**
     * Format a message as an SSE event string.
     *
     * @param message The message to format
     * @return The formatted SSE event string
     */
    private String formatSSEMessage(Builder message) {
        return "data: " + message.toString() + "\n\n";
    }
}