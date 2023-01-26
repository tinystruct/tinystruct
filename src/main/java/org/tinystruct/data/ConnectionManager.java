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
import org.tinystruct.data.Repository.Type;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ConnectionManager implements Runnable {

    private final static Logger logger = Logger.getLogger(ConnectionManager.class.getName());
    private final ConcurrentLinkedQueue<Connection> connections;
    private final String driverName;
    private final String url;
    private final String user;
    private final String password;
    private final int max;

    private String database;
    private boolean pending;

    /**
     * Connection Manager Constructor.
     */
    private ConnectionManager() {

        this.connections = new ConcurrentLinkedQueue<Connection>();

        Configuration<String> config = new Settings();
        this.driverName = config.get("driver");
        try {
            assert this.driverName != null;
            Driver driver = (Driver) Class.forName(driverName).getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(driver);
        } catch (InstantiationException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        this.database = config.get("database");
        String dbUrl = config.get("database.url");
        String dbUser = config.get("database.user");
        String dbPassword = config.get("database.password");

        String dbType = getConfiguredType().name().toLowerCase();
        if (null != dbUrl && !"h2".equalsIgnoreCase(dbType)) {
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
        this.max = config.get("database.connections.max").trim().length() > 0 ? Integer.parseInt(config.get(
                "database.connections.max")) : 0;
        this.pending = false;
    }

    public static ConnectionManager getInstance() {
        return SingletonHolder.manager;
    }

    public Type getConfiguredType() {

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

    public String getDatabase() {
        return this.database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * When the connection is idle,then push it into connection pool.
     *
     * @param connection a connection
     */
    public void flush(Connection connection)// 从外面获取连接并放入连接向量中
    {
        this.connections.add(connection);
        if (this.connections.size() > this.max) {
            synchronized (ConnectionManager.class) {
                if (this.connections.size() > this.max && !this.pending) {
                    this.pending = true;
                    logger.severe("the current connection size("
                            + this.connections.size()
                            + ") is out of the max number.");
                    Thread thread = new Thread(this);
                    thread.start();
                }
            }
        }
    }

    /**
     * Get connection,then remove it from collection.
     *
     * @return a available connection
     * @throws ApplicationException application exception
     */
    public Connection getConnection() throws ApplicationException// 从里面获取可用连接并返回到外部
    {
        Connection connection;
        if (!this.connections.isEmpty()) {
            connection = this.connections.poll();// 从连接向量中提取第一个空闲的连接。由于是提取，所以要把它从连接向量中删除
            try {
                if (connection.isClosed())// 对提取出来的连接进行判断，如果关闭了，那么提取下一个连接，否则直接获取一个新的连接
                {
                    logger.severe("发现了无效连接，系统已把它删除掉了！");
                    connection = this.getConnection();
                }
            } catch (SQLException ex) {
                logger.log(Level.WARNING, "获取连接出错！信息：" + ex.getMessage(), ex);
                connection = this.getConnection();
            }
        } else {
            try {
                if (null == this.user || this.user.trim().isEmpty())
                    connection = DriverManager.getConnection(this.url);
                else
                    connection = DriverManager.getConnection(this.url, this.user, this.password);

                logger.log(Level.INFO, "System default database: " + connection.getCatalog());

                if (this.database.trim().length() > 0)
                    connection.setCatalog(this.database);
            } catch (SQLException ex) {
                throw new ApplicationException(ex.getMessage(), ex);
            }
        }
        return connection;
    }

    public int size() {
        return this.connections.size();
    }

    @Override
	public void run() {
        this.clear();
    }

    public void clear() {
        synchronized (ConnectionManager.class) {
            Connection current;
            if (!this.connections.isEmpty()) {
                while (this.connections.size() > this.max && (current = this.connections.poll()) != null) {
                    try {
                        if (!current.isClosed())
                            current.close();
                    } catch (SQLException ex) {
                        logger.log(Level.WARNING, "关闭连接出错！信息：" + ex.getMessage(), ex);
                    }
                }
            }

            this.pending = false;
        }
    }

    private static final class SingletonHolder {
        static final ConnectionManager manager = new ConnectionManager();
    }
}