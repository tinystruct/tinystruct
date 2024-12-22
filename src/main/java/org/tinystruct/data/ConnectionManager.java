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
package org.tinystruct.data;

import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.repository.Type;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages database connections and provides thread-safe access to them.
 */
final class ConnectionManager implements Runnable {

    private final static Logger logger = Logger.getLogger(ConnectionManager.class.getName());
    private final ConcurrentLinkedQueue<Connection> connections;
    private final String driverName;
    private String url;
    private String user;
    private String password;
    private final int maxConnections;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String database;
    private volatile boolean pending;

    // Private constructor to ensure singleton pattern
    private ConnectionManager() {
        this.connections = new ConcurrentLinkedQueue<>();
        Configuration<String> config = new Settings();
        this.driverName = config.get("driver");
        loadDatabaseDriver();
        loadDatabaseConfig(config);
        this.maxConnections = !config.get("database.connections.max").trim().isEmpty() ?
                Integer.parseInt(config.get("database.connections.max")) : 0;
        this.pending = false;
    }

    /**
     * Get the singleton instance of ConnectionManager.
     *
     * @return The ConnectionManager instance.
     */
    public static ConnectionManager getInstance() {
        return SingletonHolder.manager;
    }

    private void loadDatabaseDriver() {
        try {
            assert driverName != null && !driverName.isEmpty();
            Class.forName(driverName).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ApplicationRuntimeException(e.getMessage(), e);
        }
    }

    private void loadDatabaseConfig(Configuration<String> config) {
        // Load and process database configuration here
        this.database = config.get("database").trim();
        String dbUrl = config.get("database.url");
        String dbUser = config.get("database.user");
        String dbPassword = config.get("database.password").trim();

        String dbType = getConfiguredType().name().toLowerCase();
        if (null != dbUrl && !"h2".equalsIgnoreCase(dbType) && !"sqlite".equalsIgnoreCase(dbType)) {
            try {
                URI dbUri;
                if (dbUrl.startsWith("jdbc:" + dbType + "://")) {
                    dbUri = new URI(dbUrl.substring("jdbc:".length()));
                } else {
                    if (!dbUrl.startsWith(dbType + "://")) {
                        dbUri = new URI(dbType + "://" + dbUrl);
                    } else {
                        dbUri = new URI(dbUrl);
                    }
                }
                if (dbUri.getUserInfo() != null) {
                    dbUser = dbUri.getUserInfo().split(":")[0];
                    dbPassword = dbUri.getUserInfo().split(":")[1];
                }
                StringBuilder builder = new StringBuilder();
                builder.append("jdbc:").append(dbType).append("://");
                builder.append(dbUri.getHost());
                builder.append(":");

                if (dbUri.getPort() != -1) {
                    builder.append(dbUri.getPort());
                } else {
                    builder.append(3306);
                }

                if (dbUri.getPath() != null) {
                    builder.append(dbUri.getPath().replaceAll("//", "/"));
                }

                if (dbUri.getQuery() != null) {
                    builder.append("?");
                    builder.append(dbUri.getQuery());
                }

                dbUrl = builder.toString();
            } catch (URISyntaxException e) {
                logger.severe(e.getMessage());
            }
        }

        this.url = dbUrl;
        this.user = dbUser;
        this.password = dbPassword;
    }

    private Type getConfiguredType() {
        int index = -1, length = Type.values().length;
        for (int i = 0; i < length; i++) {
            if (this.driverName.contains(Type.values()[i].name().toLowerCase())) {
                index = i;
                break;
            }
        }

        switch (index) {
            case 0:
                return Type.MySQL;
            case 1:
                return Type.SQLServer;
            case 2:
                return Type.SQLite;
            case 3:
                return Type.H2;
            default:
                break;
        }
        return Type.MySQL;
    }

    /**
     * Get the current database name.
     *
     * @return The database name.
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Set the database name.
     *
     * @param database The new database name.
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * Adds a connection to the queue for reuse.
     *
     * @param connection The connection to be added.
     */
    public void flush(Connection connection) {
        synchronized (ConnectionManager.class) {
            connections.add(connection);
            if (connections.size() == 1) return;

            if (connections.size() > maxConnections && !pending) {
                pending = true;
                logger.severe("The current connection size (" + connections.size() + ") is out of the max number.");
                executor.submit(this);
            }
        }
    }

    /**
     * Gets a connection from the queue if available, or creates a new one.
     *
     * @return A connection object.
     * @throws ApplicationException If there is an error creating a new connection.
     */
    public Connection getConnection() throws ApplicationException {
        Connection connection;
        if (!connections.isEmpty()) {
            connection = connections.poll();
            try {
                if (connection.isClosed()) {
                    logger.severe("Found an invalid connection, removing it.");
                    connection = getConnection();
                }
            } catch (SQLException ex) {
                handleSQLException("Error while checking connection status.", ex);
                connection = getConnection();
            }
        } else {
            connection = createNewConnection();
        }

        return connection;
    }

    private Connection createNewConnection() throws ApplicationException {
        try {
            Connection connection = user == null || user.trim().isEmpty() ?
                    DriverManager.getConnection(url) :
                    DriverManager.getConnection(url, user, password);
            logger.log(Level.INFO, "System default database: " + connection.getCatalog());

            if (!this.database.isEmpty() && !connection.getCatalog().equalsIgnoreCase(this.database))
                connection.setCatalog(this.database);

            return connection;
        } catch (SQLException ex) {
            throw new ApplicationException("Error while creating a new connection.", ex);
        }
    }

    /**
     * Get the size of the connection queue.
     *
     * @return The size of the connection queue.
     */
    public int size() {
        return connections.size();
    }

    @Override
    public void run() {
        clear();
    }

    /**
     * Clears excess connections from the queue, maintaining the maximum allowed connections.
     */
    public void clear() {
        synchronized (ConnectionManager.class) {
            Connection current;
            while (connections.size() > maxConnections && (current = connections.poll()) != null) {
                try {
                    if (!current.isClosed()) {
                        current.close();
                    }
                } catch (SQLException ex) {
                    handleSQLException("Error while closing connection.", ex);
                }
            }
            pending = false;
        }
    }

    public void shutdownExecutor() throws InterruptedException {
        logger.info("Shutting down executor...");
        executor.shutdown(); // Stop accepting new tasks
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) { // Wait for tasks to complete
            logger.warning("Executor did not terminate in the specified time. Forcing shutdown...");
            executor.shutdownNow(); // Force shutdown
        }
        logger.info("Executor shut down successfully.");
    }

    private void handleSQLException(String message, SQLException ex) {
        logger.log(Level.WARNING, message + " Message: " + ex.getMessage(), ex);
    }

    private enum DatabaseType {
        MYSQL, SQLSERVER, SQLITE, H2
    }

    private static final class SingletonHolder {
        static final ConnectionManager manager = new ConnectionManager();
    }
}
