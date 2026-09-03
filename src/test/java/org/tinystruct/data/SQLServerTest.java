/*******************************************************************************
 * Copyright  (c) 2013, 2026 James M. ZHOU
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.tinystruct.data.repository.SQLServer;
import org.tinystruct.data.repository.Type;
import org.tinystruct.data.component.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SQLServerTest {

    @Mock
    Connection connection;
    @Mock
    PreparedStatement preparedStatement;
    @Mock
    ResultSet resultSet;

    SQLServer server;

    private void resetConnectionPool() throws Exception {
        java.lang.reflect.Field field = ConnectionManager.getInstance().getClass().getDeclaredField("connections");
        field.setAccessible(true);
        java.util.concurrent.ConcurrentLinkedQueue<?> queue = (java.util.concurrent.ConcurrentLinkedQueue<?>) field.get(ConnectionManager.getInstance());
        queue.clear();
    }

    @BeforeEach
    public void setUp() throws Exception {
        System.setProperty("driver", "org.h2.Driver");
        System.setProperty("database.url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        System.setProperty("database.user", "sa");
        System.setProperty("database.password", "");
        System.setProperty("database.connections.max", "10");
        System.setProperty("database", "test");

        MockitoAnnotations.openMocks(this);
        server = new SQLServer();

        when(connection.isClosed()).thenReturn(false);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        // Put the mocked connection into ConnectionManager's pool
        resetConnectionPool();
        ConnectionManager.getInstance().flush(connection);
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("driver");
        System.clearProperty("database.url");
        System.clearProperty("database.user");
        System.clearProperty("database.password");
        System.clearProperty("database.connections.max");
        System.clearProperty("database");
    }

    @Test
    public void testGetType() {
        assertEquals(Type.SQLServer, server.getType());
    }

    @Test
    public void testUpdate() throws Exception {
        Field readyFields = new Field();

        FieldInfo idField = new FieldInfo();
        idField.append("name", "Id");
        idField.append("column", "id");
        idField.append("value", 100L);
        idField.append("type", "BIGINT");
        readyFields.append("Id", idField);

        FieldInfo nameField = new FieldInfo();
        nameField.append("name", "name");
        nameField.append("column", "name");
        nameField.append("value", "John");
        nameField.append("type", "VARCHAR");
        readyFields.append("name", nameField);

        when(preparedStatement.executeUpdate()).thenReturn(1);

        boolean result = server.update(readyFields, "users");

        assertTrue(result);
        verify(connection).prepareStatement(contains("UPDATE [users] SET"));
    }
}
