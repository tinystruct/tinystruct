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
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.annotation.Column;
import org.tinystruct.data.annotation.Id;
import org.tinystruct.data.component.Condition;
import org.tinystruct.data.component.Field;
import org.tinystruct.data.component.FieldInfo;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;
import org.tinystruct.data.repository.Type;
import org.tinystruct.system.Settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    public void testAnnotationsProduceTheSameMappingAsXml() throws ApplicationException {
        // "MappingCacheEntity" is described by MappingCacheEntity.map.xml; the annotated
        // fixture below declares the identical mapping.
        FakeData fromXml = new FakeData("MappingCacheEntity", repositoryOfType(Type.MySQL));
        AnnotatedWidget fromAnnotations = new AnnotatedWidget("AnnotatedWidget", repositoryOfType(Type.MySQL));

        Field xmlFields = Mapping.getMappedField(fromXml);
        Field annotatedFields = Mapping.getMappedField(fromAnnotations);

        assertEquals(fromXml.getTableName(), fromAnnotations.getTableName());
        assertEquals(xmlFields.keySet(), annotatedFields.keySet());
        for (String key : xmlFields.keySet()) {
            assertEquals(xmlFields.get(key).toString(), annotatedFields.get(key).toString(), "property " + key);
        }
        assertFalse(annotatedFields.containsKey("notMapped"), "fields without @Column must not be mapped");
    }

    @Test
    public void testXmlWithAutoAttributesMatchesTheOriginalAttributeNames() throws ApplicationException {
        // MappingCacheEntity.map.xml uses the original increment/generate attributes,
        // MappingAutoAttributes.map.xml the same mapping with autoIncrement/autoGenerated.
        FakeData original = new FakeData("MappingCacheEntity", repositoryOfType(Type.MySQL));
        FakeData renamed = new FakeData("MappingAutoAttributes", repositoryOfType(Type.MySQL));

        Field originalFields = Mapping.getMappedField(original);
        Field renamedFields = Mapping.getMappedField(renamed);

        assertEquals(originalFields.keySet(), renamedFields.keySet());
        for (String key : originalFields.keySet()) {
            assertEquals(originalFields.get(key).toString(), renamedFields.get(key).toString(), "property " + key);
        }
        assertTrue(renamedFields.get("Id").isAutoIncrement());
        assertFalse(renamedFields.get("Id").isAutoGenerated());
    }

    @Test
    public void testAutoGeneratedAttributeGivesAFreshIdPerInstance() throws ApplicationException {
        FakeData first = new FakeData("MappingAutoAttributesGenId", repositoryOfType(Type.PostgreSQL));
        FakeData second = new FakeData("MappingAutoAttributesGenId", repositoryOfType(Type.PostgreSQL));

        Field firstFields = Mapping.getMappedField(first);
        Mapping.getMappedField(second);

        assertTrue(firstFields.get("Id").isAutoGenerated());
        assertFalse(firstFields.get("Id").isAutoIncrement());
        assertNotNull(first.getId());
        assertNotEquals(first.getId(), second.getId(), "each instance must get its own generated id");
    }

    @Test
    public void testAutoAttributeWinsOverTheOriginalName() throws ApplicationException {
        FakeData data = new FakeData("MappingBothAttributes", repositoryOfType(Type.MySQL));

        Field fields = Mapping.getMappedField(data);

        assertTrue(fields.get("Id").isAutoIncrement(), "autoIncrement=\"true\" must win over increment=\"false\"");
    }

    @Test
    public void testAnnotationTakesPrecedenceOverXml() throws ApplicationException {
        // MappingPrecedence.map.xml maps table "from_xml"; the annotation says otherwise.
        AnnotatedWidget data = new AnnotatedWidget("MappingPrecedence", repositoryOfType(Type.MySQL));

        Mapping.getMappedField(data);

        assertEquals("`widgets`", data.getTableName());
    }

    @Test
    public void testAnnotatedTableNameQuotingPerDialectWithSchema() throws ApplicationException {
        AnnotatedProfile mysql = new AnnotatedProfile(repositoryOfType(Type.MySQL));
        AnnotatedProfile sqlServer = new AnnotatedProfile(repositoryOfType(Type.SQLServer));
        AnnotatedProfile postgres = new AnnotatedProfile(repositoryOfType(Type.PostgreSQL));

        Mapping.getMappedField(mysql);
        Mapping.getMappedField(sqlServer);
        Mapping.getMappedField(postgres);

        assertEquals("`public`.`profiles`", mysql.getTableName());
        assertEquals("[public].[profiles]", sqlServer.getTableName());
        assertEquals("\"public\".\"profiles\"", postgres.getTableName());
    }

    @Test
    public void testAnnotatedGeneratedIdIsFreshPerInstance() throws ApplicationException {
        AnnotatedGeneratedId first = new AnnotatedGeneratedId(repositoryOfType(Type.PostgreSQL));
        AnnotatedGeneratedId second = new AnnotatedGeneratedId(repositoryOfType(Type.PostgreSQL));

        Field firstFields = Mapping.getMappedField(first);
        Field secondFields = Mapping.getMappedField(second);

        assertNotNull(first.getId());
        assertNotEquals(first.getId(), second.getId(), "each instance must get its own generated id");
        assertEquals(first.getId(), firstFields.get("Id").value());
        assertEquals(second.getId(), secondFields.get("Id").value());
    }

    @Test
    public void testAnnotatedTableWithoutIdentifier() throws ApplicationException {
        AnnotatedLog data = new AnnotatedLog(repositoryOfType(Type.MySQL));

        Field fields = Mapping.getMappedField(data);

        assertEquals("`logs`", data.getTableName());
        assertEquals(1, fields.size());
        assertEquals("message", fields.get("message").getColumnName(), "an empty column name defaults to the field name");
        assertFalse(fields.containsKey("Id"));
    }

    @Test
    public void testSubclassInheritsMappingAndAddsColumns() throws ApplicationException {
        AnnotatedGadget data = new AnnotatedGadget(repositoryOfType(Type.MySQL));

        Field fields = Mapping.getMappedField(data);

        assertEquals("`widgets`", data.getTableName());
        assertTrue(fields.containsKey("Id"));
        assertTrue(fields.containsKey("name"), "columns of the superclass are mapped");
        assertTrue(fields.containsKey("email"), "columns of the superclass are mapped");
        assertEquals("serial_no", fields.get("serialNo").getColumnName(), "columns of the subclass are mapped");
    }

    @Test
    public void testDuplicateAnnotatedPropertyIsRejected() {
        DuplicateWidget data = new DuplicateWidget(repositoryOfType(Type.MySQL));

        assertThrows(ApplicationRuntimeException.class, () -> Mapping.getMappedField(data));
    }

    @Test
    public void testTableCreationFailureNeverBreaksInstantiation() throws ApplicationException {
        java.util.Properties settings = new Settings().getProperties();
        settings.setProperty("database.autocreate", "true");
        try {
            // Tables cannot be created for Redis, so the attempt fails; the mapping must still work.
            FakeData data = new FakeData("MappingCacheEntity", repositoryOfType(Type.Redis));

            Field fields = Mapping.getMappedField(data);

            assertEquals("widgets", data.getTableName());
            assertEquals(3, fields.size());
        } finally {
            settings.remove("database.autocreate");
        }
    }

    @org.tinystruct.data.annotation.Table(name = "widgets",
            id = @Id(name = "Id", column = "id", type = "INTEGER", autoIncrement = true))
    private static class AnnotatedWidget extends FakeData {
        @Column(name = "name", type = "VARCHAR", length = 255)
        private String name;
        @Column(name = "email", type = "VARCHAR", length = 255)
        private String email;
        private String notMapped;

        AnnotatedWidget(String className, Repository repository) {
            super(className, repository);
        }
    }

    private static final class AnnotatedGadget extends AnnotatedWidget {
        @Column(name = "serial_no", type = "VARCHAR", length = 32)
        private String serialNo;

        AnnotatedGadget(Repository repository) {
            super("AnnotatedGadget", repository);
        }
    }

    private static final class DuplicateWidget extends AnnotatedWidget {
        @Column(name = "other", type = "VARCHAR", length = 10)
        private String name;

        DuplicateWidget(Repository repository) {
            super("DuplicateWidget", repository);
        }
    }

    @org.tinystruct.data.annotation.Table(name = "profiles", schema = "public",
            id = @Id(column = "id", type = "INTEGER", autoIncrement = true))
    private static final class AnnotatedProfile extends FakeData {
        AnnotatedProfile(Repository repository) {
            super("AnnotatedProfile", repository);
        }
    }

    @org.tinystruct.data.annotation.Table(name = "sessions",
            id = @Id(column = "id", type = "VARCHAR", length = 36, autoGenerated = true))
    private static final class AnnotatedGeneratedId extends FakeData {
        AnnotatedGeneratedId(Repository repository) {
            super("AnnotatedGeneratedId", repository);
        }
    }

    @org.tinystruct.data.annotation.Table(name = "logs")
    private static final class AnnotatedLog extends FakeData {
        @Column(type = "TEXT")
        private String message;

        AnnotatedLog(Repository repository) {
            super("AnnotatedLog", repository);
        }
    }

    /**
     * Minimal {@link Data} implementation exercising only what {@link Mapping#getMappedField}
     * touches, so these tests don't need a fully wired {@link
     * org.tinystruct.data.component.AbstractData} subclass and its process-wide static
     * repository bootstrap.
     */
    private static class FakeData implements Data {
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
