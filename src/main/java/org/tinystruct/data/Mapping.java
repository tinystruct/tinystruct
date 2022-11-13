/*******************************************************************************
 * Copyright  (c) 2013, 2017 James Mover Zhou
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
import java.util.Iterator;
import java.util.List;

public class Mapping {
    private static final String PROPERTY = "property";
    private static final String NAME = "name";
    private static final String LENGTH = "length";
    private static final String COLUMN = "column";
    private static final String TYPE = "type";
    private static final String GENERATE = "generate";
    private static final String INCREMENT = "increment";
    private static final String ID = "id";

    public static Field getMappedField(Data data) throws ApplicationException {
        String className = data.getClassName();
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
                        throw new ApplicationRuntimeException("Failed to load mapping file: " + mapFile + ", Error: " + e.getMessage());
                    }
                } else
                    document = manager.get(className);
            }
        } else {
            document = manager.get(className);
        }

        Iterator<Element> iterator = document.getRoot().getElementsByTagName("class").iterator();

        List<Element> list = null;
        Element currentElement;
        while (iterator.hasNext()) {
            currentElement = iterator.next();
            if (className.equalsIgnoreCase(
                    currentElement.getAttribute(NAME))) {
                switch (data.getRepository().getType().ordinal()) {
                    case 0:
                        data.setTableName("`"
                                + currentElement.getAttribute("table") + "`");
                        break;
                    case 1:
                        data.setTableName("["
                                + currentElement.getAttribute("table") + "]");
                        break;
                    default:
                        data.setTableName(currentElement.getAttribute("table"));
                        break;
                }
                list = currentElement.getChildNodes();
                break;
            }
        }

        Field field = new Field();
        if (list != null && list.size() > 0) {
            iterator = list.iterator();

            FieldInfo fieldInfo;
            while (iterator.hasNext()) {
                currentElement = iterator.next();
                if (currentElement.getName().equalsIgnoreCase(ID)) {
                    fieldInfo = new FieldInfo();
                    fieldInfo.append(ID, currentElement.getAttribute(NAME));
                    fieldInfo.append(NAME, currentElement.getAttribute(NAME));
                    fieldInfo.append(INCREMENT, currentElement.getAttribute(INCREMENT));
                    fieldInfo.append(GENERATE, currentElement.getAttribute(GENERATE));
                    fieldInfo.append(TYPE, currentElement.getAttribute(TYPE));
                    fieldInfo.append(COLUMN, currentElement.getAttribute(COLUMN));
                    fieldInfo.append(LENGTH, currentElement.getAttribute(LENGTH));

                    if (Boolean.parseBoolean(currentElement.getAttribute(GENERATE))) {
                        data.setId(java.util.UUID.randomUUID().toString());
                        fieldInfo.append("value", data.getId());
                    }

                    field.append(fieldInfo.getName(), fieldInfo);
                }

                if (currentElement.getName().equalsIgnoreCase(PROPERTY)) {
                    fieldInfo = new FieldInfo();
                    List<Attribute> attributes = currentElement.getAttributes();

                    for (Attribute attribute : attributes) {
                        fieldInfo.append(attribute.name,
                                attribute.value);
                    }

                    field.append(fieldInfo.getName(), fieldInfo);
                }
            }
        }

        return field;
    }
}