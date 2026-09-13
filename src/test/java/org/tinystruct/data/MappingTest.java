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

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Condition;
import org.tinystruct.data.component.Field;
import org.tinystruct.data.component.FieldInfo;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;
import org.tinystruct.data.repository.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for {@link Mapping}'s per-(class, dialect) metadata cache, added when
 * the DOM walk that used to run on every {@code new SomeData()} was hoisted out into a
 * one-time-per-class computation. These tests exercise {@link Mapping#getMappedField}
 * directly (via a minimal {@link Data} stub) rather than through {@link
 * org.tinystruct.data.component.AbstractData}, so they don't depend on the shared,
 * process-wide database driver bootstrap that {@code AbstractData}'s static repository
 * field requires.
 */
public class MappingTest {

    private static Repository repositoryOfType(Type type) {
        Repository repository = mock(Repository.class);
        when(repository.getType()).thenReturn(type);
        return repository;
    }

    @Test
    public void testFieldsAreIndependentAcrossInstances() throws ApplicationException {
        FakeData first = new FakeData("MappingCacheEntity", repositoryOfType(Type.MySQL));
        FakeData second = new FakeData("MappingCacheEntity", repositoryOfType(Type.MySQL));

        Field firstFields = Mapping.getMappedField(first);
        Field secondFields = Mapping.getMappedField(second);

        // Cached DOM-derived metadata must still yield a fresh Field/FieldInfo per call.
        assertNotSame(firstFields, secondFields);
        assertNotSame(firstFields.get("name"), secondFields.get("name"));

        assertEquals("`widgets`", first.getTableName());
        assertEquals("`widgets`", second.getTableName());

        assertEquals("id", firstFields.get("Id").getColumnName());
        assertEquals("name", firstFields.get("name").getColumnName());
        assertEquals("email", firstFields.get("email").getColumnName());
        assertEquals(255, firstFields.get("name").getLength());
        assertEquals("VARCHAR", firstFields.get("name").getType().toString());

        // Mutating one instance's field value must not leak into the other's.
        firstFields.get("name").set("value", "Alice");
        secondFields.get("name").set("value", "Bob");
        assertEquals("Alice", firstFields.get("name").stringValue());
        assertEquals("Bob", secondFields.get("name").stringValue());
    }

    @Test
    public void testGeneratedIdIsFreshPerInstance() throws ApplicationException {
        FakeData first = new FakeData("MappingCacheEntityGenId", repositoryOfType(Type.PostgreSQL));
        FakeData second = new FakeData("MappingCacheEntityGenId", repositoryOfType(Type.PostgreSQL));

        Field firstFields = Mapping.getMappedField(first);
        Field secondFields = Mapping.getMappedField(second);

        assertNotNull(first.getId());
        assertNotNull(second.getId());
        assertNotEquals(first.getId(), second.getId(), "each instance must get its own generated id");
        assertEquals(first.getId(), firstFields.get("Id").value());
        assertEquals(second.getId(), secondFields.get("Id").value());
    }

    @Test
    public void testTableNameQuotingPerDialectWithSchema() throws ApplicationException {
        FakeData mysql = new FakeData("MappingCacheEntitySchema", repositoryOfType(Type.MySQL));
        FakeData sqlServer = new FakeData("MappingCacheEntitySchema", repositoryOfType(Type.SQLServer));
        FakeData postgres = new FakeData("MappingCacheEntitySchema", repositoryOfType(Type.PostgreSQL));

        Mapping.getMappedField(mysql);
        Mapping.getMappedField(sqlServer);
        Mapping.getMappedField(postgres);

        assertEquals("`public`.`profiles`", mysql.getTableName());
        assertEquals("[public].[profiles]", sqlServer.getTableName());
        assertEquals("\"public\".\"profiles\"", postgres.getTableName());
    }

    /**
     * Minimal {@link Data} implementation exercising only what {@link Mapping#getMappedField}
     * touches, so these tests don't need a fully wired {@link
     * org.tinystruct.data.component.AbstractData} subclass and its process-wide static
     * repository bootstrap.
     */
    private static final class FakeData implements Data {
        private final String className;
        private final Repository repository;
        private String tableName;
        private Object id;

        FakeData(String className, Repository repository) {
            this.className = className;
            this.repository = repository;
        }

        String getTableName() {
            return tableName;
        }

        @Override
        public String getClassPath() {
            return "";
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public void setTableName(String attribute) {
            this.tableName = attribute;
        }

        @Override
        public Object setId(Object Id) {
            this.id = Id;
            return this.id;
        }

        @Override
        public Object getId() {
            return id;
        }

        @Override
        public Repository getRepository() {
            return repository;
        }

        @Override
        public boolean append() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object appendAndGetId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean update() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean delete() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Data setRequestFields(String fields) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Data orderBy(String[] fieldNames) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Table find(String SQL, Object[] parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Table find(Condition condition, Object[] parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Table findWith(String where, Object[] parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Row findOne(String SQL, Object[] parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Row findOneById() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Row findOneByKey(String PK, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Table findAll() {
            throw new UnsupportedOperationException();
        }
    }
}
