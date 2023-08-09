/*******************************************************************************
 * Copyright  (c) 2013, 2023 James Mover Zhou
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
import java.util.logging.Logger;

public class MySQLGenerator implements Generator {
    private final static Logger logger = Logger.getLogger(MSSQLGenerator.class.getName());
    private String path;
    private String packageName;
    private String[] packageList;

    public MySQLGenerator() {
        this.path = "src/main/java/org/tinystruct/custom/object";
        this.packageList = new String[]{};
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
        StringBuilder java_resource = new StringBuilder();
        StringBuilder java_member_declaration = new StringBuilder();
        StringBuilder java_method_declaration = new StringBuilder();
        StringBuilder java_method_setdata = new StringBuilder();
        StringBuilder java_method_tostring = new StringBuilder();

        String spliter = "";

        if (this.path.endsWith("/"))
            this.path = this.path + className;
        else
            this.path = this.path + File.separator + className;

        if (this.packageName != null) {
            java_resource.append("package ").append(this.packageName).append(";\r\n");
        } else {
            java_resource.append("package org.tinystruct.customer.object;\r\n");
        }

        java_resource.append("import java.io.Serializable;\r\n");

        if (this.packageList.length > 0) {
            java_resource.append("\r\n");
            for (int i = 0; i < this.packageList.length; i++) {
                java_resource.append("import ").append(this.packageList[i]).append(";\r\n");
            }
        }

        java_resource.append("\r\n");
        java_resource.append("import org.tinystruct.data.component.Row;\r\n");
        java_resource.append("import org.tinystruct.data.component.AbstractData;\r\n\r\n");
        java_resource.append("public class ").append(className).append(" extends AbstractData implements Serializable {\r\n");
        java_resource.append("	/**\r\n");
        java_resource.append("   * Auto Generated Serial Version UID\r\n");
        java_resource.append("   */\r\n");
        try {
            java_resource.append("  private static final long serialVersionUID = ").append(SecureRandom.getInstance("NativePRNG").nextLong()).append("L;\r\n");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        Element rootElement = new Element("mapping");
        Element classElement = rootElement.addElement("class");

        classElement.setAttribute("name", className);
        classElement.setAttribute("table", table);

        String command = "desc `" + table + "`";
        Table data = this.find(command);
        Iterator<Row> listRow = data.iterator();
        Row currentRow;

        String propertyName, propertyType, propertyTypeValue;
        boolean increment;
        while (listRow.hasNext()) {
            currentRow = listRow.next();

            Iterator<Field> fields = currentRow.iterator();
            Field currentFields;

            while (fields.hasNext()) {
                currentFields = fields.next();

                propertyName = StringUtilities.setCharToUpper(currentFields.get("COLUMN_NAME").value().toString(), '_');
                propertyName = StringUtilities.remove(propertyName, '_');

                String propertyNameOfMethod = StringUtilities.setCharToUpper(propertyName, 0);

                propertyTypeValue = currentFields.get("COLUMN_TYPE").value().toString();

                String[] props = propertyTypeValue.split("\\(");
                propertyType = FieldType.valueOf(props[0]).getRealType();

                if (java_method_tostring.length() > 0) spliter = ",";

                if (currentFields.get("COLUMN_NAME").value().toString().equalsIgnoreCase("id")) {
                    increment = currentFields.get("EXTRA").stringValue().indexOf("auto_increment") != -1;

                    if ("String".equalsIgnoreCase(propertyType))
                        java_method_tostring.append("\t\tbuffer.append(\"").append(spliter).append("\\\"").append(propertyNameOfMethod).append("\\\":\\\"\"+this.get").append(propertyNameOfMethod).append("()+\"\\\"\");\r\n");
                    else
                        java_method_tostring.append("\t\tbuffer.append(\"").append(spliter).append("\\\"").append(propertyNameOfMethod).append("\\\":\"+this.get").append(propertyNameOfMethod).append("());\r\n");

                    Element idElement = classElement.addElement("id");

                    idElement.setAttribute("name", propertyNameOfMethod);
                    idElement.setAttribute("column", currentFields.get("COLUMN_NAME").value().toString());
                    idElement.setAttribute("increment", String.valueOf(increment));
                    idElement.setAttribute("generate", String.valueOf(!increment));
                    idElement.setAttribute("length", props.length > 1 ? props[1].split("\\)")[0] : "0");
                    idElement.setAttribute("type", props[0]);

                    if ("String".equalsIgnoreCase(propertyType)) {
                        java_method_declaration.append("\tpublic ").append(propertyType).append(" get").append(propertyNameOfMethod).append("()\r\n");
                        java_method_declaration.append("\t{\r\n");
                        java_method_declaration.append("\t\treturn String.valueOf(this.").append(propertyNameOfMethod).append(");\r\n");
                    } else if ("int".equalsIgnoreCase(propertyType)) {
                        java_method_declaration.append("\tpublic Integer get").append(propertyNameOfMethod).append("()\r\n");
                        java_method_declaration.append("\t{\r\n");
                        java_method_declaration.append("\t\treturn Integer.parseInt(this.").append(propertyNameOfMethod).append(".toString());\r\n");
                    }

                    java_method_declaration.append("\t}\r\n\r\n");
                } else {
                    java_member_declaration.append("\tprivate ").append(propertyType).append(" ").append(propertyName).append(";\r\n");

                    java_method_declaration.append("\tpublic void set").append(propertyNameOfMethod).append("(").append(propertyType).append(" ").append(propertyName).append(")\r\n");
                    java_method_declaration.append("\t{\r\n");
                    java_method_declaration.append("\t\tthis.").append(propertyName).append(" = this.setFieldAs").append(StringUtilities.setCharToUpper(propertyType, 0)).append("(\"").append(propertyName).append("\",").append(propertyName).append(");\r\n");
                    java_method_declaration.append("\t}\r\n\r\n");

                    if ("String".equalsIgnoreCase(propertyType) || "Date".equalsIgnoreCase(propertyType))
                        java_method_tostring.append("\t\tbuffer.append(\"").append(spliter).append("\\\"").append(propertyName).append("\\\":\\\"\"+this.get").append(propertyNameOfMethod).append("()+\"\\\"\");\r\n");
                    else
                        java_method_tostring.append("\t\tbuffer.append(\"").append(spliter).append("\\\"").append(propertyName).append("\\\":\"+this.get").append(propertyNameOfMethod).append("());\r\n");

                    Element propertyElement = classElement.addElement("property");

                    propertyElement.setAttribute("name", propertyName);
                    propertyElement.setAttribute("column", currentFields.get("COLUMN_NAME").value().toString());
                    propertyElement.setAttribute("length", props.length > 1 ? props[1].split("\\)")[0] : "0");
                    propertyElement.setAttribute("type", props[0]);

                    java_method_declaration.append("\tpublic ").append(propertyType).append(" get").append(propertyNameOfMethod).append("()\r\n");
                    java_method_declaration.append("\t{\r\n");
                    java_method_declaration.append("\t\treturn this.").append(propertyName).append(";\r\n");
                    java_method_declaration.append("\t}\r\n\r\n");
                }

                java_method_setdata.append("\t\tif(row.getFieldInfo(\"").append(currentFields.get("COLUMN_NAME").value().toString()).append("\")!=null)");
                java_method_setdata.append("\tthis.set").append(propertyNameOfMethod).append("(row.getFieldInfo(\"").append(currentFields.get("COLUMN_NAME").value().toString()).append("\").").append(StringUtilities.setCharToLower(propertyType, 0)).append("Value());\r\n");
            }
        }

        Path java_src_path = Paths.get(this.path);
        Path java_resource_path = Paths.get(this.path.replace("main" + File.separator + "java", "main" + File.separator + "resources") + ".map.xml");
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
        try (FileOutputStream out = new FileOutputStream(this.path.replace("main" + File.separator + "java", "main" + File.separator + "resources") + ".map.xml")) {
            document.save(out);
        } catch (IOException IO) {
            logger.severe(IO.getMessage());
        }

        java_resource.append(java_member_declaration);
        java_resource.append("\r\n");
        java_resource.append(java_method_declaration);
        java_resource.append("\r\n");
        java_resource.append("\t@Override\r\n");
        java_resource.append("\tpublic void setData(Row row) {\r\n");
        java_resource.append(java_method_setdata);
        java_resource.append("\t}\r\n\r\n");

        java_resource.append("\t@Override\r\n");
        java_resource.append("\tpublic String toString() {\r\n");
        java_resource.append("\t\tStringBuilder buffer = new StringBuilder();\r\n");
        java_resource.append("\t\tbuffer.append(\"{\");\r\n");
        java_resource.append(java_method_tostring);
        java_resource.append("\t\tbuffer.append(\"}\");\r\n");
        java_resource.append("\t\treturn buffer.toString();\r\n");
        java_resource.append("\t}\r\n\r\n");
        java_resource.append("}");

        FileGenerator generator = new FileGenerator(this.path + ".java", java_resource);
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
            String[] fieldName = new String[cols];
			String[] fieldValue = new String[cols];

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

    @Override
	public void importPackages(String packageNameList) {
        this.packageList = packageNameList.split(";");
    }
}