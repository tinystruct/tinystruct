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
package org.tinystruct.mail;

import org.tinystruct.ApplicationException;
import org.tinystruct.mail.Connection.PROTOCOL;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;

import javax.mail.MessagingException;
import java.util.Vector;

public final class ConnectionManager implements Runnable {
    private final Vector<Connection> list;
    private boolean pending;

    private static final class SingletonHolder {
        static final ConnectionManager manager = new ConnectionManager();
    }

    private ConnectionManager() {
        this.list = new Vector<Connection>();
        this.pending = false;
    }

    public static ConnectionManager getInstance() {
        return SingletonHolder.manager;
    }

    public void run() {

        Connection current;
        synchronized (ConnectionManager.class) {
            int i = 0;
            while (i < this.list.size()) {
                current = this.list.get(i);

                if (current != null && !current.available()) {
                    try {
                        current.close();
                    } catch (MessagingException e) {

                        e.printStackTrace();
                    }

                    this.list.remove(i);
                }
                i++;
            }

            this.pending = false;
        }

    }

    /**
     * When connection is finished work,then put it into collection.
     *
     * @param connection connection
     */
    public void flush(Connection connection) {
        synchronized (ConnectionManager.class) {
            this.list.add(connection);

            if (this.list.size() > 3 && !this.pending) {
                this.pending = true;

                new Thread(this).start();
            }
        }
    }

    public Connection getConnection(Configuration<String> config, PROTOCOL protocol) throws ApplicationException {
        Connection connection;
        synchronized (ConnectionManager.class) {
            if (this.list.size() > 0) {
                connection = this.list.firstElement();// 从连接向量中提取第一个空闲的连接。由于是提取，所以要把它从连接向量中删除
                this.list.remove(connection);
                if (!connection.available())// 对提取出来的连接进行判断，如果关闭了，那么提取下一个连接，否则直接获取一个新的连接
                {
                    connection = this.getConnection(protocol);
                }
            } else {
                if (protocol == PROTOCOL.SMTP)
                    connection = new SMTPConnection(config);
                else
                    connection = new POP3Connection(config);
            }

            return connection;
        }
    }

    public Connection getConnection(PROTOCOL protocol) throws ApplicationException {
        return this.getConnection(new Settings("/application.properties"), protocol);
    }

    public int size() {
        return list.size();
    }

}

