package org.tinystruct.http;

import org.tinystruct.data.component.Builder;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SSEPushManager {
    private static final Logger logger = Logger.getLogger(SSEPushManager.class.getName());
    private final ConcurrentHashMap<String, SSEClient> clients = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    private static final SSEPushManager instance = new SSEPushManager();

    private SSEPushManager() {}

    public static SSEPushManager getInstance() {
        return instance;
    }

    public SSEClient register(String sessionId, Response out) {
        return register(sessionId, out, null);
    }

    public SSEClient register(String sessionId, Response out, BlockingQueue<Builder> messageQueue) {
        if (isShutdown.get()) {
            logger.log(Level.WARNING, "SSEPushManager is shutting down, cannot register new client");
            return null;
        }

        SSEClient oldClient = clients.get(sessionId);
        if (oldClient != null) {
            return oldClient;
        }

        SSEClient client = messageQueue != null ?
                new SSEClient(out, messageQueue) :
                new SSEClient(out);

        clients.put(sessionId, client);
        executor.submit(client);
        return client;
    }

    public void push(String sessionId, Builder message) {
        if (isShutdown.get()) {
            logger.log(Level.WARNING, "SSEPushManager is shutting down, cannot push message");
            return;
        }

        SSEClient client = clients.get(sessionId);
        if (client != null && client.isActive()) {
            client.send(message);
        } else {
            // Clean up inactive client
            clients.remove(sessionId, client);
        }
    }

    public void broadcast(Builder message) {
        if (isShutdown.get()) {
            logger.log(Level.WARNING, "SSEPushManager is shutting down, cannot broadcast message");
            return;
        }

        clients.forEach((sessionId, client) -> {
            if (client.isActive()) {
                client.send(message);
            } else {
                // Clean up inactive client
                clients.remove(sessionId, client);
            }
        });
    }

    public void remove(String sessionId) {
        SSEClient client = clients.remove(sessionId);
        if (client != null) {
            client.close();
        }
    }

    public Set<String> getClientIds() {
        return clients.keySet();
    }

    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            // Close all clients
            clients.forEach((sessionId, client) -> client.close());
            clients.clear();

            // Shutdown executor
            executor.shutdown();
        }
    }
}
