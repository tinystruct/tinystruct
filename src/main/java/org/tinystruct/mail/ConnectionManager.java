/*******************************************************************************
 * Copyright  (c) 2023 James Mover Zhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.tinystruct.mail;

import org.tinystruct.ApplicationException;
import org.tinystruct.mail.Connection.PROTOCOL;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Message;
import jakarta.mail.Address;

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.Duration;
import java.time.Instant;

/**
 * Manages a pool of mail connections with automatic cleanup and monitoring.
 * Implements connection pooling pattern with thread-safe operations.
 */
public final class ConnectionManager implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(ConnectionManager.class.getName());
    
    private final ConcurrentLinkedQueue<PooledConnection> idleConnections;
    private final ConcurrentHashMap<Connection, Instant> activeConnections;
    private final ScheduledExecutorService cleanupExecutor;
    private final int maxPoolSize;
    private final Duration maxIdleTime;
    private final Duration maxLifetime;
    private volatile boolean isShutdown;

    private static final class SingletonHolder {
        static final ConnectionManager INSTANCE = new ConnectionManager();
    }

    private ConnectionManager() {
        this.idleConnections = new ConcurrentLinkedQueue<>();
        this.activeConnections = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ConnectionManager-Cleanup");
            t.setDaemon(true);
            return t;
        });
        this.maxPoolSize = Integer.parseInt(System.getProperty("mail.pool.size", "10"));
        this.maxIdleTime = Duration.ofMinutes(Integer.parseInt(System.getProperty("mail.pool.idle.minutes", "5")));
        this.maxLifetime = Duration.ofMinutes(Integer.parseInt(System.getProperty("mail.pool.lifetime.minutes", "30")));
        this.isShutdown = false;

        // Schedule periodic cleanup
        this.cleanupExecutor.scheduleAtFixedRate(
            this::cleanup,
            1, 1, TimeUnit.MINUTES
        );
    }

    public static ConnectionManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Gets a connection from the pool or creates a new one if needed.
     *
     * @param config Configuration for creating new connections
     * @param protocol Mail protocol to use
     * @return A connection from the pool
     * @throws ApplicationException if connection creation fails
     */
    public Connection getConnection(Configuration<String> config, PROTOCOL protocol) throws ApplicationException {
        if (isShutdown) {
            throw new ApplicationException("Connection manager is shut down");
        }

        // First try to get an idle connection
        PooledConnection connection = idleConnections.poll();
        if (connection != null) {
            if (isConnectionValid(connection)) {
                markConnectionActive(connection);
                return connection;
            } else {
                closeConnection(connection);
            }
        }

        // Create new connection if pool is not full
        if (activeConnections.size() < maxPoolSize) {
            connection = createNewConnection(config, protocol);
            markConnectionActive(connection);
            return connection;
        }

        // Wait for a connection to become available
        try {
            return waitForConnection(config, protocol);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApplicationException("Interrupted while waiting for connection", e);
        }
    }

    /**
     * Returns a connection to the pool.
     *
     * @param connection The connection to return
     */
    public void releaseConnection(Connection connection) {
        if (connection instanceof PooledConnection) {
            PooledConnection pooledConnection = (PooledConnection) connection;
            activeConnections.remove(connection);
            
            if (isConnectionValid(pooledConnection)) {
                idleConnections.offer(pooledConnection);
            } else {
                closeConnection(pooledConnection);
            }
        }
    }

    @Override
    public void close() {
        isShutdown = true;
        cleanupExecutor.shutdown();
        
        try {
            if (!cleanupExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cleanupExecutor.shutdownNow();
        }

        // Close all connections
        idleConnections.forEach(this::closeConnection);
        idleConnections.clear();
        activeConnections.keySet().forEach(this::closeConnection);
        activeConnections.clear();
    }

    private void cleanup() {
        Instant now = Instant.now();

        // Cleanup idle connections
        idleConnections.removeIf(conn -> {
            if (isConnectionExpired(conn, now)) {
                closeConnection(conn);
                return true;
            }
            return false;
        });

        // Cleanup active connections
        activeConnections.entrySet().removeIf(entry -> {
            if (Duration.between(entry.getValue(), now).compareTo(maxLifetime) > 0) {
                closeConnection(entry.getKey());
                return true;
            }
            return false;
        });
    }

    private boolean isConnectionValid(PooledConnection connection) {
        return connection != null && 
               connection.getCreationTime() != null && 
               connection.getLastUsedTime() != null &&
               Duration.between(connection.getCreationTime(), Instant.now()).compareTo(maxLifetime) <= 0 &&
               Duration.between(connection.getLastUsedTime(), Instant.now()).compareTo(maxIdleTime) <= 0;
    }

    private boolean isConnectionExpired(PooledConnection connection, Instant now) {
        return Duration.between(connection.getLastUsedTime(), now).compareTo(maxIdleTime) > 0 ||
               Duration.between(connection.getCreationTime(), now).compareTo(maxLifetime) > 0;
    }

    private void closeConnection(Connection connection) {
        try {
            connection.close();
        } catch (MessagingException e) {
            logger.log(Level.WARNING, "Error closing connection", e);
        }
    }

    private void markConnectionActive(PooledConnection connection) {
        connection.setLastUsedTime(Instant.now());
        activeConnections.put(connection, Instant.now());
    }

    private PooledConnection createNewConnection(Configuration<String> config, PROTOCOL protocol) 
            throws ApplicationException {
        Connection baseConnection = protocol == PROTOCOL.SMTP ? 
            new SMTPConnection(config) : new POP3Connection(config);
        return new PooledConnection(baseConnection);
    }

    private Connection waitForConnection(Configuration<String> config, PROTOCOL protocol) 
            throws InterruptedException, ApplicationException {
        int attempts = 0;
        while (attempts < 3) {
            PooledConnection connection = idleConnections.poll();
            if (connection != null && isConnectionValid(connection)) {
                markConnectionActive(connection);
                return connection;
            }
            
            Thread.sleep(1000);
            attempts++;
        }
        throw new ApplicationException("Connection pool exhausted");
    }

    /**
     * Wraps a Connection with pooling metadata.
     */
    private static class PooledConnection implements Connection {
        private final Connection delegate;
        private final Instant creationTime;
        private volatile Instant lastUsedTime;

        PooledConnection(Connection delegate) {
            this.delegate = delegate;
            this.creationTime = Instant.now();
            this.lastUsedTime = Instant.now();
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public PROTOCOL getProtocol() {
            return delegate.getProtocol();
        }

        @Override
        public Session getSession() {
            return delegate.getSession();
        }

        @Override
        public boolean available() {
            return delegate.available();
        }

        @Override
        public void send(Message message, Address[] recipients) throws MessagingException {
            try {
                delegate.send(message, recipients);
                lastUsedTime = Instant.now();
            } catch (MessagingException e) {
                // Log the error and rethrow
                logger.log(Level.WARNING, "Failed to send message using connection: " + getId(), e);
                throw e;
            }
        }

        @Override
        public void close() throws MessagingException {
            try {
                delegate.close();
            } catch (MessagingException e) {
                logger.log(Level.WARNING, "Error closing connection: " + getId(), e);
                throw e;
            }
        }

        Instant getCreationTime() {
            return creationTime;
        }

        Instant getLastUsedTime() {
            return lastUsedTime;
        }

        void setLastUsedTime(Instant time) {
            this.lastUsedTime = time;
        }
    }
}

