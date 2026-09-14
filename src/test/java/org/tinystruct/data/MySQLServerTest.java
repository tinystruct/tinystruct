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
import org.tinystruct.data.repository.MySQLServer;
import org.tinystruct.data.repository.Type;
import org.tinystruct.data.component.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class MySQLServerTest {

    @Mock
    Connection connection;
    @Mock
    PreparedStatement preparedStatement;
    @Mock
    ResultSet resultSet;
    @Mock
    ResultSetMetaData resultSetMetaData;

    MySQLServer server;

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
        server = new MySQLServer();

        when(connection.isClosed()).thenReturn(false);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        // Put the mocked connection into ConnectionManager's pool
        resetConnectionPool();
        ConnectionManager.getInstance().flush(connection);
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Prevent the mocked connection from leaking into the shared ConnectionManager
        // singleton's pool, where it would be handed out to unrelated tests run afterwards.
        resetConnectionPool();

        System.clearProperty("driver");
        System.clearProperty("database.url");
        System.clearProperty("database.user");
        System.clearProperty("database.password");
        System.clearProperty("database.connections.max");
        System.clearProperty("database");
    }

    @Test
    public void testGetType() {
        assertEquals(Type.MySQL, server.getType());
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
        verify(connection).prepareStatement(contains("UPDATE users SET"));
    }

    /**
     * Regression test covering two fixes in {@code MySQLServer.find()} at once: (1)
     * {@code ResultSetMetaData} used to be re-fetched for every column of every row instead
     * of once up front, and (2) rows used to be appended one at a time directly onto the
     * {@code CopyOnWriteArrayList}-backed {@code Table} (quadratic in the row count) instead
     * of being collected and added in one batch. This verifies row order and content are
     * correct across several rows after both fixes.
     */
    @Test
    public void testFindMultipleRowsPreservesOrderAndContent() throws Exception {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSetMetaData.getColumnCount()).thenReturn(2);
        when(resultSetMetaData.getColumnName(1)).thenReturn("id");
        when(resultSetMetaData.getColumnTypeName(1)).thenReturn("INT");
        when(resultSetMetaData.getColumnName(2)).thenReturn("name");
        when(resultSetMetaData.getColumnTypeName(2)).thenReturn("VARCHAR");

        int rowCount = 5;
        when(resultSet.next()).thenReturn(true, true, true, true, true, false);
        when(resultSet.getObject(1)).thenReturn(1, 2, 3, 4, 5);
        when(resultSet.getObject(2)).thenReturn("Row1", "Row2", "Row3", "Row4", "Row5");
        when(resultSet.getInt(1)).thenReturn(1, 2, 3, 4, 5);
        when(resultSet.getString(2)).thenReturn("Row1", "Row2", "Row3", "Row4", "Row5");

        Table table = server.find("SELECT id, name FROM users", new Object[]{});

        assertNotNull(table);
        assertEquals(rowCount, table.size());
        for (int i = 0; i < rowCount; i++) {
            Row row = table.get(i);
            assertEquals(i + 1, row.get(0).get("id").value());
            assertEquals("Row" + (i + 1), row.get(0).get("name").value());
        }

        // The fix hoists getMetaData()/getColumnName()/getColumnTypeName() out of the row
        // loop: they must be called exactly once per column, not once per cell.
        verify(resultSetMetaData, times(1)).getColumnName(1);
        verify(resultSetMetaData, times(1)).getColumnTypeName(1);
        verify(resultSetMetaData, times(1)).getColumnName(2);
        verify(resultSetMetaData, times(1)).getColumnTypeName(2);
    }
}
