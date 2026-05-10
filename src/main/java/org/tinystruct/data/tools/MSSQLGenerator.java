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
 ********************************************************************************/
package org.tinystruct.data.tools;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.DatabaseOperator;
import org.tinystruct.data.component.*;
import org.tinystruct.dom.Document;
import org.tinystruct.dom.Element;
import org.tinystruct.system.util.FileGenerator;
import org.tinystruct.system.util.StringUtilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MSSQLGenerator implements Generator {
    private final static Logger logger = Logger.getLogger(MSSQLGenerator.class.getName());
    private String path;
    private String packageName;

    public MSSQLGenerator() {
        this.path = "src/main/java/org/tinystruct/custom/object";
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public void create(String className, String table) throws ApplicationException {
        StringBuilder java_member_declaration = new StringBuilder();
        StringBuilder java_method_declaration = new StringBuilder();
        StringBuilder java_method_setdata = new StringBuilder();
        StringBuilder java_method_tostring = new StringBuilder();

        String spliter = "";
        String lineSeparator = "\r\n";

        Element rootElement = new Element("mapping");
        Element classElement = rootElement.addElement("class");

        classElement.setAttribute("name", className);
        classElement.setAttribute("table", table);

        String command = "SELECT a.name, b.name AS type, a.length, a.typestat AS increment FROM syscolumns a LEFT OUTER JOIN systypes b ON a.xtype = b.xtype WHERE (a.id =(SELECT id FROM sysobjects WHERE name = '"
                + table + "'))";
        Table data = this.find(command);
        Iterator<Row> listRow = data.iterator();
        Row currentRow;

        Set<String> imports = new TreeSet<>();
        imports.add("java.io.Serializable");
        imports.add("org.tinystruct.data.component.Row");
        imports.add("org.tinystruct.data.component.AbstractData");


        String propertyName, propertyType;
        boolean increment;
        while (listRow.hasNext()) {
            currentRow = listRow.next();

            Iterator<Field> fields = currentRow.iterator();
            Field currentFields;

            while (fields.hasNext()) {
                currentFields = fields.next();

                propertyName = StringUtilities.setCharToUpper(currentFields.get("name").value().toString(), '_');
                propertyName = StringUtilities.remove(propertyName, '_');
                propertyType = FieldType.valueOf(currentFields.get("type").value().toString()).getRealType();

                String propertyNameOfMethod = StringUtilities.setCharToUpper(propertyName, 0);

                // Add automatic imports based on propertyType
                switch (propertyType) {
                    case "LocalDateTime" -> imports.add("java.time.LocalDateTime");
                    case "Date" -> imports.add("java.util.Date");
                    case "Timestamp" -> imports.add("java.sql.Timestamp");
                    case "Time" -> imports.add("java.sql.Time");
                }

                if (currentFields.get("name").value().toString().equalsIgnoreCase("id")) {
                    increment = Integer.parseInt(currentFields.get("increment").value().toString()) == 1;

                    if ("String".equalsIgnoreCase(propertyType))
                        java_method_tostring.append("\t\tbuffer.append(\"").append(spliter).append("\\\"")
                                .append(propertyNameOfMethod).append("\\\":\\\"\"+this.get")
                                .append(propertyNameOfMethod).append("()+\"\\\"\");").append(lineSeparator);
                    else
                        java_method_tostring.append("\t\tbuffer.append(\"").append(spliter).append("\\\"")
                                .append(propertyNameOfMethod).append("\\\":\"+this.get").append(propertyNameOfMethod)
                                .append("());").append(lineSeparator);
                    spliter = ",";

                    Element idElement = classElement.addElement("id");

                    idElement.setAttribute("name", propertyNameOfMethod);
                    idElement.setAttribute("column", currentFields.get("name").value().toString());
                    idElement.setAttribute("increment", String.valueOf(increment));
                    idElement.setAttribute("generate", String.valueOf(!increment));
                    idElement.setAttribute("length", currentFields.get("length").value().toString());
                    idElement.setAttribute("type", currentFields.get("type").value().toString());

                    if ("String".equalsIgnoreCase(propertyType)) {
                        java_method_declaration.append("\tpublic ").append(propertyType).append(" get")
                                .append(propertyNameOfMethod).append("()").append(lineSeparator);
                        java_method_declaration.append("\t{").append(lineSeparator);
                        java_method_declaration.append("\t\treturn String.valueOf(this.").append(propertyNameOfMethod)
                                .append(");").append(lineSeparator);
                    } else if ("int".equalsIgnoreCase(propertyType)) {
                        java_method_declaration.append("\tpublic Integer get").append(propertyNameOfMethod)
                                .append("()").append(lineSeparator);
                        java_method_declaration.append("\t{").append(lineSeparator);
                        java_method_declaration.append("\t\treturn Integer.parseInt(this.").append(propertyNameOfMethod)
                                .append(".toString());").append(lineSeparator);
                    } else if ("long".equalsIgnoreCase(propertyType)) {
                        java_method_declaration.append("\tpublic Long get").append(propertyNameOfMethod)
                                .append("()").append(lineSeparator);
                        java_method_declaration.append("\t{").append(lineSeparator);
                        java_method_declaration.append("\t\treturn Long.parseLong(this.").append(propertyNameOfMethod)
                                .append(".toString());").append(lineSeparator);
                    }

                    java_method_declaration.append("\t}").append(lineSeparator).append(lineSeparator);
                } else {
                    java_member_declaration.append("\tprivate ").append(propertyType).append(" ").append(propertyName)
                            .append(";").append(lineSeparator);

                    java_method_declaration.append("\tpublic void set").append(propertyNameOfMethod).append("(")
                            .append(propertyType).append(" ").append(propertyName).append(")").append(lineSeparator);
                    java_method_declaration.append("\t{").append(lineSeparator);
                    if (!propertyType.endsWith("[]")) {
                        java_method_declaration.append("\t\tthis.").append(propertyName).append(" = this.setFieldAs")
                                .append(StringUtilities.setCharToUpper(propertyType, 0)).append("(\"")
                                .append(propertyName).append("\",").append(propertyName).append(");").append(lineSeparator);
                    } else {
                        java_method_declaration.append("\t\tthis.").append(propertyName).append(" = this.setFieldAs")
                                .append(StringUtilities.setCharToUpper(propertyType.replace("[]", "Array"), 0))
                                .append("(\"").append(propertyName).append("\",").append(propertyName).append(");").append(lineSeparator);
                    }
                    java_method_declaration.append("\t}").append(lineSeparator).append(lineSeparator);

                    if ("String".equalsIgnoreCase(propertyType) || "Date".equalsIgnoreCase(propertyType)
                            || "LocalDateTime".equalsIgnoreCase(propertyType)) {
                        java_method_tostring.append("\t\tbuffer.append(\"").append(spliter).append("\\\"")
                                .append(propertyName).append("\\\":\\\"\"+this.get").append(propertyNameOfMethod)
                                .append("()+\"\\\"\");").append(lineSeparator);
                    } else if ("byte[]".equalsIgnoreCase(propertyType)) {
                        java_method_tostring.append("\t\tif (this.get").append(propertyNameOfMethod)
                                .append("() != null) {").append(lineSeparator);
                        java_method_tostring.append("\t\t\tbuffer.append(\"").append(spliter).append("\\\"")
                                .append(propertyName).append("\\\":\\\"binary-data\\\"\");").append(lineSeparator);
                        java_method_tostring.append("\t\t} else {").append(lineSeparator);
                        java_method_tostring.append("\t\t\tbuffer.append(\"").append(spliter).append("\\\"")
                                .append(propertyName).append("\\\":null\");").append(lineSeparator);
                        java_method_tostring.append("\t\t}").append(lineSeparator);
                    } else {
                        java_method_tostring.append("\t\tbuffer.append(\"").append(spliter).append("\\\"")
                                .append(propertyName).append("\\\":\"+this.get").append(propertyNameOfMethod)
                                .append("());").append(lineSeparator);
                    }
                    spliter = ",";

                    Element propertyElement = classElement.addElement("property");

                    propertyElement.setAttribute("name", propertyName);
                    propertyElement.setAttribute("column", currentFields.get("name").value().toString());
                    propertyElement.setAttribute("length", currentFields.get("length").value().toString());
                    propertyElement.setAttribute("type", currentFields.get("type").value().toString());

                    java_method_declaration.append("\tpublic ").append(propertyType).append(" get")
                            .append(propertyNameOfMethod).append("()").append(lineSeparator);
                    java_method_declaration.append("\t{").append(lineSeparator);
                    java_method_declaration.append("\t\treturn this.").append(propertyName).append(";").append(lineSeparator);
                    java_method_declaration.append("\t}").append(lineSeparator).append(lineSeparator);
                }

                java_method_setdata.append("\t\tif(row.getFieldInfo(\"")
                        .append(currentFields.get("name").value().toString()).append("\")!=null)");
                if (propertyType.endsWith("[]")) {
                    java_method_setdata.append("\tthis.set").append(propertyNameOfMethod).append("(row.getFieldInfo(\"")
                            .append(currentFields.get("name").value().toString()).append("\").")
                            .append(propertyType.replace("[]", "Array")).append("Value());").append(lineSeparator);
                } else {
                    java_method_setdata.append("\tthis.set").append(propertyNameOfMethod).append("(row.getFieldInfo(\"")
                            .append(currentFields.get("name").value().toString()).append("\").")
                            .append(StringUtilities.setCharToLower(propertyType, 0)).append("Value());").append(lineSeparator);
                }
            }
        }

        StringBuilder java_resource = new StringBuilder();
        if (this.packageName != null) {
            java_resource.append("package ").append(this.packageName).append(";").append(lineSeparator);
        } else {
            java_resource.append("package org.tinystruct.custom.object;").append(lineSeparator);
        }

        java_resource.append(lineSeparator);
        for (String pkg : imports) {
            java_resource.append("import ").append(pkg).append(";").append(lineSeparator);
        }

        java_resource.append(lineSeparator);
        java_resource.append("public class ").append(className)
                .append(" extends AbstractData implements Serializable {").append(lineSeparator);
        java_resource.append("	/**").append(lineSeparator);
        java_resource.append("   * Auto Generated Serial Version UID").append(lineSeparator);
        java_resource.append("   */").append(lineSeparator);
        try {
            java_resource.append("  private static final long serialVersionUID = ")
                    .append(SecureRandom.getInstance("SHA1PRNG").nextLong()).append("L;").append(lineSeparator);
        } catch (NoSuchAlgorithmException e) {
            java_resource.append("  private static final long serialVersionUID = 1L;").append(lineSeparator);
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        java_resource.append(java_member_declaration);
        java_resource.append(lineSeparator);
        java_resource.append(java_method_declaration);
        java_resource.append(lineSeparator);
        java_resource.append("\t@Override").append(lineSeparator);
        java_resource.append("\tpublic void setData(Row row) {").append(lineSeparator);
        java_resource.append(java_method_setdata);
        java_resource.append("\t}").append(lineSeparator).append(lineSeparator);

        java_resource.append("\t@Override").append(lineSeparator);
        java_resource.append("\tpublic String toString() {").append(lineSeparator);
        java_resource.append("\t\tStringBuilder buffer = new StringBuilder();").append(lineSeparator);
        java_resource.append("\t\tbuffer.append(\"{\");").append(lineSeparator);
        java_resource.append(java_method_tostring);
        java_resource.append("\t\tbuffer.append(\"}\");").append(lineSeparator);
        java_resource.append("\t\treturn buffer.toString();").append(lineSeparator);
        java_resource.append("\t}").append(lineSeparator).append(lineSeparator);
        java_resource.append("}");

        String fullPath;
        if (this.path.endsWith("/"))
            fullPath = this.path + className;
        else
            fullPath = this.path + File.separator + className;

        Path java_src_path = Paths.get(fullPath + ".java");

        // Replace path separators to handle Windows paths
        String normalizedPath = (fullPath + ".java").replace("\\", "/");
        String resourcePath = normalizedPath.replace("main/java", "main/resources");
        Path java_resource_path = Paths.get(resourcePath.replace(".java", ".map.xml"));

        try {
            Path parent = java_src_path.getParent();
            if (parent != null)
                Files.createDirectories(parent);

            parent = java_resource_path.getParent();
            if (parent != null)
                Files.createDirectories(parent);
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }

        Document document = new Document(rootElement);
        try (FileOutputStream out = new FileOutputStream(java_resource_path.toString())) {
            document.save(out);
        } catch (IOException IO) {
            logger.severe(IO.getMessage());
        }

        FileGenerator generator = new FileGenerator(fullPath + ".java", java_resource);
        generator.save();
    }

    public Table find(String SQL) throws ApplicationException {
        logger.severe("find:" + SQL);
        Table table = new Table();
        Row row;
        FieldInfo field;
        Field fields;

        try (DatabaseOperator operator = new DatabaseOperator()) {
            ResultSet set = operator.query(SQL);
            int cols = set.getMetaData().getColumnCount();
            String[] fieldName = new String[cols], fieldValue = new String[cols];

            for (int i = 0; i < cols; i++) {
                fieldName[i] = set.getMetaData().getColumnName(i + 1);
            }

            Object v_field;
            while (set.next()) {
                row = new Row();
                fields = new Field();
                for (int i = 0; i < fieldName.length; i++) {
                    v_field = set.getObject(i + 1);

                    fieldValue[i] = (v_field == null ? "" : v_field.toString());
                    field = new FieldInfo();
                    field.append("name", fieldName[i]);
                    field.append("value", fieldValue[i]);
                    field.append("type", field.typeOf(v_field).getTypeName());

                    fields.append(field.getName(), field);
                }
                row.append(fields);
                table.append(row);
            }
        } catch (Exception e) {
            throw new ApplicationException(e.getMessage(), e);
        }

        return table;
    }

}