/*******************************************************************************
 * Copyright  (c) 2013, 2025 James M. ZHOU
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
package org.tinystruct.data.tools;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.DatabaseOperator;
import org.tinystruct.system.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;

public class SQLiteGeneratorTest {

    private static final String TEST_TABLE = "test_autoincrement";
    private static final String TEST_CLASS = "TestAutoincrement";
    private static final String TEST_OUTPUT_DIR = "target/test-classes/generated";
    private static final String TEST_PACKAGE = "org.tinystruct.test.generated";

    @Before
    public void setUp() throws ApplicationException, SQLException {
        // Create test directory
        Path testDir = Paths.get(TEST_OUTPUT_DIR);
        try {
            Files.createDirectories(testDir);
        } catch (IOException e) {
            throw new ApplicationException("Failed to create test directory", e);
        }

        // Create test table with INTEGER PRIMARY KEY for autoincrement
        try (DatabaseOperator operator = new DatabaseOperator()) {
            // Drop table if exists
            try {
                operator.execute("DROP TABLE IF EXISTS " + TEST_TABLE);
            } catch (Exception e) {
                // Ignore if table doesn't exist
            }

            // Create table with INTEGER PRIMARY KEY for autoincrement
            String createTableSQL = "CREATE TABLE " + TEST_TABLE + " (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name TEXT, " +
                    "age INTEGER)";
            operator.execute(createTableSQL);
        }
    }

    @Test
    public void testAutoIncrementIdColumn() throws ApplicationException {
        // Create SQLiteGenerator
        SQLiteGenerator generator = new SQLiteGenerator();
        generator.setPath(TEST_OUTPUT_DIR + "/");
        generator.setPackageName(TEST_PACKAGE);

        // Generate class from test table
        generator.create(TEST_CLASS, TEST_TABLE);

        // Check if the generated files exist
        File javaFile = new File(TEST_OUTPUT_DIR + "/" + TEST_CLASS + ".java");
        File xmlFile = new File(TEST_OUTPUT_DIR + "/" + TEST_CLASS + ".map.xml");

        assertTrue("Java file should be generated", javaFile.exists());
        assertTrue("XML mapping file should be generated", xmlFile.exists());

        // Read the XML file content to verify autoincrement attribute
        try {
            String xmlContent = new String(Files.readAllBytes(xmlFile.toPath()));
            assertTrue("XML should contain increment=\"true\" for id column", 
                    xmlContent.contains("increment=\"true\""));
            assertTrue("XML should contain generate=\"false\" for id column", 
                    xmlContent.contains("generate=\"false\""));
        } catch (IOException e) {
            throw new ApplicationException("Failed to read XML file", e);
        }
    }

    @After
    public void tearDown() throws ApplicationException {
        // Drop test table
        try (DatabaseOperator operator = new DatabaseOperator()) {
            operator.execute("DROP TABLE IF EXISTS " + TEST_TABLE);
        } catch (SQLException e) {
            throw new ApplicationException("Failed to drop test table", e);
        }
    }
}
