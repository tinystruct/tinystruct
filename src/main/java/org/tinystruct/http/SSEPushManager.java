package org.tinystruct.http;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SSEPushManager {
    private final ConcurrentHashMap<String, SSEClient> clients = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static final SSEPushManager instance = new SSEPushManager();

    private SSEPushManager() {}

    public static SSEPushManager getInstance() {
        return instance;
    }

    public void register(String sessionId, Response out) {
        SSEClient client = new SSEClient(out);
        clients.put(sessionId, client);
        executor.submit(client);
    }

    public void push(String sessionId, String message) {
        SSEClient client = clients.get(sessionId);
        if (client != null && client.isActive()) {
            client.send(message);
        }
    }

    public void broadcast(String message) {
        for (SSEClient client : clients.values()) {
            if (client.isActive()) {
                client.send(message);
            }
        }
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
}
