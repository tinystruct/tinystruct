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
import org.tinystruct.ApplicationException;
import org.tinystruct.data.tools.PostgreSQLGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class PostgreSQLGeneratorTest {

    private static final String TEST_TABLE = "test_postgres_table";
    private static final String TEST_CLASS = "PostgresTestClass";
    private static final String TEST_OUTPUT_DIR = "target/test-classes/generated_pg";
    private static final String TEST_PACKAGE = "org.tinystruct.test.generated.pg";

    @Mock
    Connection connection;
    @Mock
    PreparedStatement preparedStatement;
    @Mock
    ResultSet resultSet;
    @Mock
    ResultSetMetaData resultSetMetaData;

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

        // Create test output directory
        Path testDir = Paths.get(TEST_OUTPUT_DIR);
        Files.createDirectories(testDir);

        // Mock JDBC behavior
        when(connection.isClosed()).thenReturn(false);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(connection.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(preparedStatement);
        
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);

        // We return 3 columns from metadata query: name, type, increment
        when(resultSetMetaData.getColumnCount()).thenReturn(3);
        when(resultSetMetaData.getColumnName(1)).thenReturn("name");
        when(resultSetMetaData.getColumnName(2)).thenReturn("type");
        when(resultSetMetaData.getColumnName(3)).thenReturn("increment");

        // Simulate 3 rows of column schema
        when(resultSet.next()).thenReturn(true, true, true, false);
        
        // Row 1: id, Row 2: name, Row 3: birth_date
        when(resultSet.getObject(1)).thenReturn("id", "name", "birth_date");
        when(resultSet.getObject(2)).thenReturn("INTEGER", "VARCHAR", "TIMESTAMP");
        when(resultSet.getObject(3)).thenReturn("1", "0", "0");

        // Load mock connection to connection manager
        resetConnectionPool();
        ConnectionManager.getInstance().flush(connection);
    }

    @Test
    public void testGeneratorCreatesCorrectFiles() throws ApplicationException, IOException {
        PostgreSQLGenerator generator = new PostgreSQLGenerator();
        generator.setPath(TEST_OUTPUT_DIR + "/");
        generator.setPackageName(TEST_PACKAGE);

        // Generate classes
        generator.create(TEST_CLASS, TEST_TABLE);

        // Check file existence
        File javaFile = new File(TEST_OUTPUT_DIR + "/" + TEST_CLASS + ".java");
        File xmlFile = new File(TEST_OUTPUT_DIR + "/" + TEST_CLASS + ".map.xml");

        assertTrue(javaFile.exists(), "Java class file should be generated");
        assertTrue(xmlFile.exists(), "XML mapping file should be generated");

        // Verify XML mapping file contents
        String xmlContent = new String(Files.readAllBytes(xmlFile.toPath()));
        assertTrue(xmlContent.contains("class name=\"" + TEST_CLASS + "\""), "XML should map to correct class");
        assertTrue(xmlContent.contains("table=\"" + TEST_TABLE + "\""), "XML should map to correct table");
        assertTrue(xmlContent.contains("increment=\"true\""), "ID should be mapped as increment=\"true\"");
        assertTrue(xmlContent.contains("column=\"birth_date\""), "birth_date column mapping should be generated");
    }

    @AfterEach
    public void tearDown() throws IOException {
        System.clearProperty("driver");
        System.clearProperty("database.url");
        System.clearProperty("database.user");
        System.clearProperty("database.password");
        System.clearProperty("database.connections.max");
        System.clearProperty("database");

        // Clean up generated files
        Path dir = Paths.get(TEST_OUTPUT_DIR);
        if (Files.exists(dir)) {
            Files.walk(dir)
                 .map(Path::toFile)
                 .forEach(File::delete);
            Files.deleteIfExists(dir);
        }
    }
}
