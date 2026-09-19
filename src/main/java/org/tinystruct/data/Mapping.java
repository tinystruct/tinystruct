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
package org.tinystruct.data;

import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.annotation.Column;
import org.tinystruct.data.annotation.Id;
import org.tinystruct.data.annotation.Table;
import org.tinystruct.data.component.Field;
import org.tinystruct.data.component.FieldInfo;
import org.tinystruct.dom.Attribute;
import org.tinystruct.dom.Document;
import org.tinystruct.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Mapping {
    private static final String PROPERTY = "property";
    private static final String NAME = "name";
    private static final String LENGTH = "length";
    private static final String COLUMN = "column";
    private static final String TYPE = "type";
    private static final String GENERATE = "generate";
    private static final String INCREMENT = "increment";
    private static final String ID = "id";
    private static final String SCHEMA = "schema";

    /**
     * Per (class, repository-dialect) cache of the immutable parts of a mapping: the
     * resolved/quoted table name and, for every mapped column, a prototype
     * {@link FieldInfo} whose type, length and flags are already parsed and resolved.
     * Every {@code new SomeData()} used to re-walk the mapping XML DOM, and then
     * re-parse every attribute string (type lookup, integer and boolean parsing) for
     * every column. None of that varies for a given class, so it is done once here and
     * each instance only copies the prototypes into its own {@link Field}/{@link FieldInfo}
     * objects (plus any generated-id value).
     */
    private static final ConcurrentHashMap<String, ClassMetadata> METADATA_CACHE = new ConcurrentHashMap<>();

    public static Field getMappedField(Data data) throws ApplicationException {
        String className = data.getClassName();
        String cacheKey = className + ':' + data.getRepository().getType().ordinal();

        ClassMetadata metadata = METADATA_CACHE.get(cacheKey);
        if (metadata == null) {
            metadata = buildClassMetadata(data, className);
            ClassMetadata existing = METADATA_CACHE.putIfAbsent(cacheKey, metadata);
            if (existing != null) {
                metadata = existing;
            }
        }

        data.setTableName(metadata.tableName);

        Field field = new Field();
        for (FieldTemplate template : metadata.fields) {
            FieldInfo fieldInfo = new FieldInfo(template.prototype);

            if (template.needsGeneratedId) {
                data.setId(java.util.UUID.randomUUID().toString());
                fieldInfo.append("value", data.getId());
            }

            field.append(template.key, fieldInfo);
        }

        return field;
    }

    /**
     * Builds the metadata for the given class exactly once. A {@link Table} annotation
     * on the class takes precedence; otherwise the {@code .map.xml} file is used. Both
     * sources produce the same table name and per-column prototype {@link FieldInfo},
     * so instantiation is identical whichever one a class uses.
     */
    private static ClassMetadata buildClassMetadata(Data data, String className) throws ApplicationException {
        Table table = data.getClass().getAnnotation(Table.class);
        return table != null
                ? buildAnnotatedMetadata(data, table)
                : buildXmlMetadata(data, className);
    }

    /**
     * Reads the {@link Table}, {@link Id} and {@link Column} annotations of the data
     * class (including its superclasses) into the table name and prototypes.
     */
    private static ClassMetadata buildAnnotatedMetadata(Data data, Table table) {
        String tableName = quoteTable(data.getRepository().getType().ordinal(), table.name(), table.schema());

        List<FieldTemplate> templates = new ArrayList<>();
        Id id = table.id();
        if (!id.column().isEmpty()) {
            templates.add(idTemplate(id.name(), String.valueOf(id.increment()), String.valueOf(id.generate()),
                    id.type(), id.column(), String.valueOf(id.length())));
        }

        Set<String> keys = new HashSet<>();
        for (Class<?> type = data.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (java.lang.reflect.Field member : type.getDeclaredFields()) {
                Column column = member.getAnnotation(Column.class);
                if (column == null) {
                    continue;
                }

                String key = member.getName();
                if (!keys.add(key)) {
                    throw new ApplicationRuntimeException("Duplicate mapped property '" + key + "' in " + data.getClass().getName());
                }

                String[] attrNames = {NAME, COLUMN, TYPE, LENGTH};
                String[] attrValues = {
                        key,
                        column.name().isEmpty() ? key : column.name(),
                        column.type(),
                        String.valueOf(column.length())
                };
                templates.add(new FieldTemplate(key, prototypeOf(attrNames, attrValues), false));
            }
        }

        return new ClassMetadata(tableName, templates);
    }

    /**
     * Parses the mapping XML (via the cached {@link Document} in {@link MappingManager})
     * for the given class exactly once, capturing the table name and a resolved
     * prototype {@link FieldInfo} per column so subsequent instantiations can skip
     * both the DOM walk and the attribute parsing.
     */
    private static ClassMetadata buildXmlMetadata(Data data, String className) throws ApplicationException {
        String mapFile = data.getClassPath() + className + ".map.xml";

        MappingManager manager = MappingManager.getInstance();
        Document document;
        if (manager.get(className) == null) {
            synchronized (Mapping.class) {
                if (manager.get(className) == null) {
                    document = new Document();
                    try (InputStream in = data.getClass().getResourceAsStream("/" + mapFile)) {
                        boolean loaded = document.load(in);
                        if (!loaded) {
                            throw new ApplicationRuntimeException("Failed to load mapping file: " + mapFile);
                        }
                        manager.set(className, document);
                    } catch (IOException e) {
                        throw new ApplicationRuntimeException("Failed to load mapping file: " + mapFile + ", Error: " + e.getMessage(), e);
                    }
                } else
                    document = manager.get(className);
            }
        } else {
            document = manager.get(className);
        }

        Iterator<Element> iterator = document.getRoot().getElementsByTagName("class").iterator();

        String tableName = null;
        List<Element> list = null;
        Element currentElement;
        while (iterator.hasNext()) {
            currentElement = iterator.next();
            if (className.equalsIgnoreCase(
                    currentElement.getAttribute(NAME))) {
                String table = currentElement.getAttribute("table");
                String schema = currentElement.getAttribute(SCHEMA);
                tableName = quoteTable(data.getRepository().getType().ordinal(), table, schema);
                list = currentElement.getChildNodes();
                break;
            }
        }

        List<FieldTemplate> templates = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            iterator = list.iterator();

            while (iterator.hasNext()) {
                currentElement = iterator.next();
                if (currentElement.getName().equalsIgnoreCase(ID)) {
                    templates.add(idTemplate(
                            currentElement.getAttribute(NAME),
                            currentElement.getAttribute(INCREMENT),
                            currentElement.getAttribute(GENERATE),
                            currentElement.getAttribute(TYPE),
                            currentElement.getAttribute(COLUMN),
                            currentElement.getAttribute(LENGTH)));
                }

                if (currentElement.getName().equalsIgnoreCase(PROPERTY)) {
                    List<Attribute> attributes = currentElement.getAttributes();
                    String[] attrNames = new String[attributes.size()];
                    String[] attrValues = new String[attributes.size()];
                    String key = null;
                    for (int i = 0; i < attributes.size(); i++) {
                        Attribute attribute = attributes.get(i);
                        attrNames[i] = attribute.name;
                        attrValues[i] = attribute.value;
                        if (NAME.equals(attribute.name)) {
                            key = attribute.value;
                        }
                    }
                    templates.add(new FieldTemplate(key, prototypeOf(attrNames, attrValues), false));
                }
            }
        }

        return new ClassMetadata(tableName, templates);
    }

    /**
     * Quotes the table (and optional schema) for the repository dialect.
     */
    private static String quoteTable(int dialect, String table, String schema) {
        switch (dialect) {
            case 0: // MySQL
                return schema.isEmpty()
                        ? "`" + table + "`"
                        : "`" + schema + "`.`" + table + "`";
            case 1: // SQL Server
            case 2: // SQLite
                return schema.isEmpty()
                        ? "[" + table + "]"
                        : "[" + schema + "].[" + table + "]";
            default: // H2, Redis, PostgreSQL
                return schema.isEmpty()
                        ? table
                        : "\"" + schema + "\".\"" + table + "\"";
        }
    }

    /**
     * Builds the template of the identifier column. Only a non-integer id that the
     * framework is asked to generate needs a fresh UUID per instance.
     */
    private static FieldTemplate idTemplate(String name, String increment, String generate,
                                            String type, String column, String length) {
        String[] attrNames = {ID, NAME, INCREMENT, GENERATE, TYPE, COLUMN, LENGTH};
        String[] attrValues = {name, name, increment, generate, type, column, length};

        boolean needsGeneratedId = Boolean.parseBoolean(generate) && !type.toLowerCase().startsWith("int");
        return new FieldTemplate(name, prototypeOf(attrNames, attrValues), needsGeneratedId);
    }

    /**
     * Resolves the attribute pairs of one mapping element into a {@link FieldInfo}
     * once, through the same {@link FieldInfo#append} route the per-instance code used
     * to take, so parsing semantics are unchanged.
     */
    private static FieldInfo prototypeOf(String[] attrNames, String[] attrValues) {
        FieldInfo prototype = new FieldInfo();
        for (int i = 0; i < attrNames.length; i++) {
            prototype.append(attrNames[i], attrValues[i]);
        }
        return prototype;
    }

    private static final class ClassMetadata {
        final String tableName;
        final List<FieldTemplate> fields;

        ClassMetadata(String tableName, List<FieldTemplate> fields) {
            this.tableName = tableName;
            this.fields = fields;
        }
    }

    private static final class FieldTemplate {
        final String key;
        final FieldInfo prototype;
        final boolean needsGeneratedId;

        FieldTemplate(String key, FieldInfo prototype, boolean needsGeneratedId) {
            this.key = key;
            this.prototype = prototype;
            this.needsGeneratedId = needsGeneratedId;
        }
    }
}
