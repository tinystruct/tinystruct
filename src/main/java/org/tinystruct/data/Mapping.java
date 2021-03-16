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
import org.tinystruct.data.component.Field;
import org.tinystruct.data.component.FieldInfo;
import org.tinystruct.dom.Attribute;
import org.tinystruct.dom.Document;
import org.tinystruct.dom.Element;

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
    private Field fields;

    public Mapping() {
        this.fields = new Field();
    }

    public Field getMappedField(Data data) throws ApplicationException {
        String className = data.getClassName();
        String mapFile = data.getClassPath() + className + ".map.xml";

        MappingManager manager = MappingManager.getInstance();
        synchronized (Mapping.class) {
            Document document;

            if (manager.get(className) == null) {
                InputStream in = data.getClass().getResourceAsStream("/" + mapFile);
                document = new Document();
                boolean loaded = document.load(in);
                if (!loaded) {
                    System.out.println(mapFile);
                    return null;
                }
                manager.set(className, document);
            } else
                document = manager.get(className);

            List<Element> list = null;
            Iterator<Element> iterator = document.getRoot().getElementsByTagName("class").iterator();

            Element currentElement = null;
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
                            data.setTableName("`"
                                    + currentElement.getAttribute("table") + "`");
                            break;
                    }
                    list = currentElement.getChildNodes();
                    break;
                }
            }

            if (list != null && list.size() > 0) {
                iterator = list.iterator();

                FieldInfo field;
                while (iterator.hasNext()) {
                    currentElement = iterator.next();
                    if (currentElement.getName().equalsIgnoreCase(ID)) {
                        field = new FieldInfo();
                        field.append(ID, currentElement.getAttribute(NAME));
                        field.append(NAME, currentElement.getAttribute(NAME));
                        field.append(INCREMENT, currentElement.getAttribute(INCREMENT));
                        field.append(GENERATE, currentElement.getAttribute(GENERATE));
                        field.append(TYPE, currentElement.getAttribute(TYPE));
                        field.append(COLUMN, currentElement.getAttribute(COLUMN));
                        field.append(LENGTH, currentElement.getAttribute(LENGTH));

                        if (Boolean.parseBoolean(currentElement.getAttribute(GENERATE))) {
                            data.setId(java.util.UUID.randomUUID().toString());
                            field.append("value", data.getId());
                        }

                        this.fields.append(field.getName(), field);
                    }

                    if (currentElement.getName().equalsIgnoreCase(PROPERTY)) {
                        field = new FieldInfo();
                        List<Attribute> attributes = currentElement.getAttributes();
                        Attribute currentAttribute;

                        for (Attribute attribute : attributes) {
                            currentAttribute = attribute;
                            field.append(currentAttribute.name,
                                    currentAttribute.value);
                        }

                        this.fields.append(field.getName(), field);
                    }
                }
            }

            return this.fields;
        }
    }
}
