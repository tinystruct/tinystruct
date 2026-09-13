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
import org.tinystruct.data.component.Field;
import org.tinystruct.data.component.FieldInfo;
import org.tinystruct.dom.Attribute;
import org.tinystruct.dom.Document;
import org.tinystruct.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
     * resolved/quoted table name and, for every mapped column, the fixed attribute
     * pairs that a fresh {@link FieldInfo} needs. Every {@code new SomeData()} used to
     * re-walk the mapping XML DOM and rebuild this from scratch; the DOM shape and the
     * table name never change for a given class, so that work is computed once here and
     * only the per-instance {@link Field}/{@link FieldInfo} objects (and any
     * generated-id value) are still built fresh on every call.
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
            FieldInfo fieldInfo = new FieldInfo();
            for (int i = 0; i < template.attrNames.length; i++) {
                fieldInfo.append(template.attrNames[i], template.attrValues[i]);
            }

            if (template.needsGeneratedId) {
                data.setId(java.util.UUID.randomUUID().toString());
                fieldInfo.append("value", data.getId());
            }

            field.append(template.key, fieldInfo);
        }

        return field;
    }

    /**
     * Parses the mapping XML (via the cached {@link Document} in {@link MappingManager})
     * for the given class exactly once, capturing the table name and per-column
     * attribute templates so subsequent instantiations can skip the DOM walk.
     */
    private static ClassMetadata buildClassMetadata(Data data, String className) throws ApplicationException {
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
                switch (data.getRepository().getType().ordinal()) {
                    case 0: // MySQL
                        tableName = schema.isEmpty()
                                ? "`" + table + "`"
                                : "`" + schema + "`.`" + table + "`";
                        break;
                    case 1: // SQL Server
                    case 2: // SQLite
                        tableName = schema.isEmpty()
                                ? "[" + table + "]"
                                : "[" + schema + "].[" + table + "]";
                        break;
                    default: // H2, Redis, PostgreSQL
                        tableName = schema.isEmpty()
                                ? table
                                : "\"" + schema + "\".\"" + table + "\"";
                        break;
                }
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
                    String name = currentElement.getAttribute(NAME);
                    String generate = currentElement.getAttribute(GENERATE);
                    String type = currentElement.getAttribute(TYPE);

                    String[] attrNames = {ID, NAME, INCREMENT, GENERATE, TYPE, COLUMN, LENGTH};
                    String[] attrValues = {
                            name,
                            name,
                            currentElement.getAttribute(INCREMENT),
                            generate,
                            type,
                            currentElement.getAttribute(COLUMN),
                            currentElement.getAttribute(LENGTH)
                    };

                    boolean needsGeneratedId = Boolean.parseBoolean(generate) && !type.toLowerCase().startsWith("int");
                    templates.add(new FieldTemplate(name, attrNames, attrValues, needsGeneratedId));
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
                    templates.add(new FieldTemplate(key, attrNames, attrValues, false));
                }
            }
        }

        return new ClassMetadata(tableName, templates);
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
        final String[] attrNames;
        final String[] attrValues;
        final boolean needsGeneratedId;

        FieldTemplate(String key, String[] attrNames, String[] attrValues, boolean needsGeneratedId) {
            this.key = key;
            this.attrNames = attrNames;
            this.attrValues = attrValues;
            this.needsGeneratedId = needsGeneratedId;
        }
    }
}
