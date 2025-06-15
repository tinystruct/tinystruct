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
package org.tinystruct.data.component;

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.Repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Example demonstrating how to use the appendAndGetId method to insert a record and get the generated ID.
 */
public class AppendAndGetIdTest {

    /**
     * Example data class for demonstration.
     */
    static class User extends AbstractData {

        private String name;
        private String email;

        public User() {
            super();
            this.setTableName("users");
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;

            // Create a field for the name
            FieldInfo field = new FieldInfo();
            field.append("name", "name");
            field.append("value", name);
            field.append("type", "VARCHAR");

            // Use the setField method to add the field to the ready fields
            this.setFieldAsString("name", name);
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;

            // Create a field for the email
            FieldInfo field = new FieldInfo();
            field.append("name", "email");
            field.append("value", email);
            field.append("type", "VARCHAR");

            // Use the setField method to add the field to the ready fields
            this.setFieldAsString("email", email);
        }

        @Override
        public void setData(Row row) {
            Field fields = row.get(0);

            if (fields.containsKey("id")) {
                this.setId(fields.get("id").value());
            }

            if (fields.containsKey("name")) {
                this.name = fields.get("name").stringValue();
            }

            if (fields.containsKey("email")) {
                this.email = fields.get("email").stringValue();
            }
        }
    }

    /**
     * Test demonstrating how to use appendAndGetId to insert a record and get the generated ID.
     * This test uses a mock repository to simulate the database interaction.
     */
    @Test
    public void testAppendAndGetId() throws ApplicationException {
        // Create a mock repository
        Repository mockRepository = mock(Repository.class);

        // Set up the mock repository to return a generated ID
        Long expectedId = 123L;
        when(mockRepository.appendAndGetId(any(), eq("users"))).thenReturn(expectedId);

        // Create a test user class that uses the mock repository
        TestUser user = new TestUser(mockRepository);
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");

        // Insert the user and get the generated ID
        Object generatedId = user.appendAndGetId();

        // Print the generated ID
        System.out.println("Generated ID: " + generatedId);

        // Verify that the ID was generated
        assertNotNull(generatedId, "Generated ID should not be null");
        assertEquals(expectedId, generatedId, "Generated ID should match the expected ID");

        // Verify that the ID was set in the user object
        assertNotNull(user.getId(), "User ID should not be null");
        assertEquals(expectedId, user.getId(), "User ID should match the expected ID");
        System.out.println("User ID: " + user.getId());

        // Verify that the repository's appendAndGetId method was called
        verify(mockRepository).appendAndGetId(any(), eq("users"));
    }

    /**
     * Test user class that uses a mock repository.
     */
    static class TestUser {
        private String name;
        private String email;
        private Object id;
        private final Repository repository;
        private final Field readyFields;
        private final String table;

        public TestUser(Repository repository) {
            this.repository = repository;
            this.readyFields = new Field();
            this.table = "users";
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;

            // Create a field for the name
            FieldInfo field = new FieldInfo();
            field.append("name", "name");
            field.append("value", name);
            field.append("type", "VARCHAR");

            // Add the field to the ready fields
            this.readyFields.append("name", field);
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;

            // Create a field for the email
            FieldInfo field = new FieldInfo();
            field.append("name", "email");
            field.append("value", email);
            field.append("type", "VARCHAR");

            // Add the field to the ready fields
            this.readyFields.append("email", field);
        }

        public Object getId() {
            return id;
        }

        public Object appendAndGetId() throws ApplicationException {
            // Call the repository's appendAndGetId method
            Object generatedId = repository.appendAndGetId(this.readyFields, this.table);
            if (generatedId != null) {
                // Update the Id field with the generated ID
                this.id = generatedId;
            }
            return generatedId;
        }
    }
}
