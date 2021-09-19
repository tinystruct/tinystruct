/*******************************************************************************
 * Copyright  (c) 2017 James Mover Zhou
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
import org.tinystruct.data.Repository.Type;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;

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

    private final ConcurrentLinkedQueue<Connection> connections;
    private final static Logger logger = Logger.getLogger("ConnectionManager.class");
    private final String driverName;
    private final String url;
    private final String user;
    private final String password;
    private final int max;
    private Configuration<String> config;

    private String database;
    private Boolean pending;

    private static final class SingletonHolder {
        static final ConnectionManager manager = new ConnectionManager();
    }

    /**
     * Connection Manager Constructor.
     */
    private ConnectionManager() {

        this.connections = new ConcurrentLinkedQueue<Connection>();

        try {
            this.config = new Settings();
            this.driverName = this.config.get("driver");

            if (this.driverName.trim().length() == 0)
                throw new ApplicationRuntimeException("Database Connection Driver has not been set in application.properties!");

            Driver driver = (Driver) Class.forName(driverName).newInstance();
            DriverManager.registerDriver(driver);
        } catch (InstantiationException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        this.database = this.config.get("database");
        String dbUrl = this.config.get("database.url");
        String dbUser = this.config.get("database.user");
        String dbPassword = this.config.get("database.password");

        if (null != dbUrl) {
            try {
                URI dbUri;
                if (dbUrl.startsWith("jdbc:")) {
                    dbUri = new URI(dbUrl.substring("jdbc:".length()));
                } else
                    dbUri = new URI(dbUrl);

                if (dbUri.getUserInfo() != null) {
                    dbUser = dbUri.getUserInfo().split(":")[0];
                    dbPassword = dbUri.getUserInfo().split(":")[1];
                    dbUrl = "jdbc:mysql://" + dbUri.getHost() + dbUri.getPath();
                }
            } catch (URISyntaxException e) {
                logger.severe(e.getMessage());
            }
        }

        this.url = dbUrl;
        this.user = dbUser;
        this.password = dbPassword;
        this.max = this.config.get("database.connections.max").trim().length() > 0 ? Integer.parseInt(this.config.get(
                "database.connections.max")) : 0;
        this.pending = false;
    }

    public Type getConfiguredType() throws ApplicationException {

        int index = -1, length = Type.values().length;
        for (int i = 0; i < length; i++) {
            if (this.driverName.indexOf(Type.values()[i].name().toLowerCase()) != -1) {
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
            default: break;
        }
        return Type.MySQL;
    }

    public static ConnectionManager getInstance() {
        return SingletonHolder.manager;
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
        synchronized (ConnectionManager.class) {
            this.connections.add(connection);

            if (this.connections.size() > this.max && !this.pending) {
                this.pending = true;
                logger.severe("the current connection size("
                        + this.connections.size()
                        + ") is out of the max number.");
                new Thread(this).start();
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
        synchronized (ConnectionManager.class) {
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
                    if (null == this.user || this.user.trim().length() == 0)
                        connection = DriverManager.getConnection(this.url);
                    else
                        connection = DriverManager.getConnection(this.url, this.user, this.password);

                    logger.severe("system default database:" + connection.getCatalog());

                    if (this.database.trim().length() > 0)
                        connection.setCatalog(this.database);
                } catch (SQLException ex) {
                    throw new ApplicationException(ex.getMessage(), ex);
                }
            }
            return connection;
        }
    }

    public int size() {
        return this.connections.size();
    }

    public void run() {

        Connection current;
        synchronized (ConnectionManager.class) {

            while (!this.connections.isEmpty()) {
                current = this.connections.poll();

                if (current != null) {
                    try {
                        if (!current.isClosed())
                            current.close();
                    } catch (SQLException ex) {
                        logger.log(Level.WARNING, "关闭连接出错！信息：" + ex.getMessage(), ex);
                        continue;
                    }
                }
            }

            this.pending = false;
        }
    }

    public void clear() {
        Connection current;
        synchronized (ConnectionManager.class) {
            while (!this.connections.isEmpty()) {
                current = this.connections.poll();

                if (current != null) {
                    try {
                        if (!current.isClosed())
                            current.close();
                    } catch (SQLException ex) {
                        logger.log(Level.WARNING, "关闭连接出错！信息：" + ex.getMessage(), ex);
                        continue;
                    }
                }
            }

        }
    }
}