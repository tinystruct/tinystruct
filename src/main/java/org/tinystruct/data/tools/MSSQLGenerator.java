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
package org.tinystruct.data.tools;


import org.tinystruct.ApplicationException;
import org.tinystruct.data.DatabaseOperator;
import org.tinystruct.data.component.*;
import org.tinystruct.dom.Document;
import org.tinystruct.dom.Element;
import org.tinystruct.system.util.FileGenerator;
import org.tinystruct.system.util.StringUtilities;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.logging.Logger;

public class MSSQLGenerator implements Generator {
    private String fileName;
    private String packageName;

    private final static Logger logger = Logger.getLogger(MSSQLGenerator.class.getName());
    private String[] packageList;

    public MSSQLGenerator() {
        this.fileName = "resources/org/tinystruct/customer/object/";
        this.packageList = new String[]{};
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void create(String className, String table) throws ApplicationException {
        StringBuilder java_resource = new StringBuilder();
        StringBuilder java_member_declaration = new StringBuilder();
        StringBuilder java_method_declaration = new StringBuilder();
        StringBuilder java_method_setdata = new StringBuilder();
        StringBuilder java_method_tostring = new StringBuilder();

        String spliter = "";

        this.fileName = this.fileName + className;

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
        java_resource.append("  private static final long serialVersionUID = ").append(new SecureRandom().nextLong()).append("L;\r\n");

        Element rootElement = new Element("mapping");
        Element classElement = rootElement.addElement("class");

        classElement.setAttribute("name", className);
        classElement.setAttribute("table", table);

        String command = "SELECT a.name, b.name AS type, a.length, a.typestat AS increment FROM syscolumns a LEFT OUTER JOIN systypes b ON a.xtype = b.xtype WHERE (a.id =(SELECT id FROM sysobjects WHERE name = '" + table + "'))";
        Table data = this.find(command);
        Iterator<Row> listRow = data.iterator();
        Row currentRow;

        String propertyName, propertyType;
        boolean increment = false;
        while (listRow.hasNext()) {
            currentRow = listRow.next();

            Iterator<Field> fields = currentRow.iterator();
            Field currentFields;

            while (fields.hasNext()) {
                currentFields = fields.next();

                propertyName = StringUtilities.setCharToUpper(currentFields.get("name").value().toString(), '_');
                propertyName = StringUtilities.remove(propertyName,'_');
                propertyType = FieldType.valueOf(currentFields.get("type").value().toString()).getRealType();

                String propertyNameOfMethod = StringUtilities.setCharToUpper(propertyName, 0);
                if (java_method_tostring.length() > 0) spliter = ",";

                if (currentFields.get("name").value().equals("id")) {
                    increment = Integer.parseInt(currentFields.get("increment").value().toString()) == 1;

                    if (propertyType.equalsIgnoreCase("String"))
                        java_method_tostring.append("\t\tbuffer.append(\"").append(spliter).append("\\\"").append(propertyNameOfMethod).append("\\\":\\\"\"+this.get").append(propertyNameOfMethod).append("()+\"\\\"\");\r\n");
                    else
                        java_method_tostring.append("\t\tbuffer.append(\"").append(spliter).append("\\\"").append(propertyNameOfMethod).append("\\\":\"+this.get").append(propertyNameOfMethod).append("());\r\n");

                    Element idElement = classElement.addElement("id");

                    idElement.setAttribute("name", propertyNameOfMethod);
                    idElement.setAttribute("column", currentFields.get("name").value().toString());
                    idElement.setAttribute("increment", String.valueOf(increment));
                    idElement.setAttribute("generate", String.valueOf(!increment));
                    idElement.setAttribute("length", currentFields.get("length").value().toString());
                    idElement.setAttribute("type", currentFields.get("type").value().toString());

                    if (propertyType.equalsIgnoreCase("String")) {
                        java_method_declaration.append("\tpublic ").append(propertyType).append(" get").append(propertyNameOfMethod).append("()\r\n");
                        java_method_declaration.append("\t{\r\n");
                        java_method_declaration.append("\t\treturn String.valueOf(this.").append(propertyNameOfMethod).append(");\r\n");
                    } else if (propertyType.equalsIgnoreCase("int")) {
                        java_method_declaration.append("\tpublic Integer get").append(propertyNameOfMethod).append("()\r\n");
                        java_method_declaration.append("\t{\r\n");
                        java_method_declaration.append("\t\treturn Integer.parseInt(this.").append(propertyNameOfMethod).append(".toString());\r\n");
                    }

                    java_method_declaration.append("\t}\r\n\r\n");
                } else {
                    java_member_declaration.append("\tprivate ").append(propertyType).append(" ").append(propertyName).append(";\r\n");

                    java_method_declaration.append("\tpublic void set").append(propertyNameOfMethod).append("(").append(propertyType).append(" ").append(propertyName).append(")\r\n");
                    java_method_declaration.append("\t{\r\n");
                    java_method_declaration.append("\t\tthis.").append(propertyName).append("=this.setFieldAs").append(StringUtilities.setCharToUpper(propertyType, 0)).append("(\"").append(propertyName).append("\",").append(propertyName).append(");\r\n");
                    java_method_declaration.append("\t}\r\n\r\n");

                    if (propertyType.equalsIgnoreCase("String") || propertyType.equalsIgnoreCase("Date"))
                        java_method_tostring.append("\t\tbuffer.append(\"").append(spliter).append("\\\"").append(propertyName).append("\\\":\\\"\"+this.get").append(propertyNameOfMethod).append("()+\"\\\"\");\r\n");
                    else
                        java_method_tostring.append("\t\tbuffer.append(\"").append(spliter).append("\\\"").append(propertyName).append("\\\":\"+this.get").append(propertyNameOfMethod).append("());\r\n");

                    Element propertyElement = classElement.addElement("property");

                    propertyElement.setAttribute("name", propertyName);
                    propertyElement.setAttribute("column", currentFields.get("name").value().toString());
                    propertyElement.setAttribute("length", currentFields.get("length").value().toString());
                    propertyElement.setAttribute("type", currentFields.get("type").value().toString());

                    java_method_declaration.append("\tpublic ").append(propertyType).append(" get").append(propertyNameOfMethod).append("()\r\n");
                    java_method_declaration.append("\t{\r\n");
                    java_method_declaration.append("\t\treturn this.").append(propertyName).append(";\r\n");
                    java_method_declaration.append("\t}\r\n\r\n");
                }

                java_method_setdata.append("\t\tif(row.getFieldInfo(\"").append(currentFields.get("name").value().toString()).append("\")!=null)");
                java_method_setdata.append("\tthis.set").append(propertyNameOfMethod).append("(row.getFieldInfo(\"").append(currentFields.get("name").value().toString()).append("\").").append(propertyType.toLowerCase()).append("Value());\r\n");

            }
        }

        Document Document = new Document(rootElement);
        try {
            Document.save(new FileOutputStream(this.fileName + ".map.xml"));
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
        java_resource.append("\t\tStringBuffer buffer=new StringBuffer();\r\n");
        java_resource.append("\t\tbuffer.append(\"{\");\r\n");
        java_resource.append(java_method_tostring);
        java_resource.append("\t\tbuffer.append(\"}\");\r\n");
        java_resource.append("\t\treturn buffer.toString();\r\n");
        java_resource.append("\t}\r\n\r\n");
        java_resource.append("}");

        FileGenerator generator = new FileGenerator(this.fileName + ".java", java_resource);
        generator.save();
    }

    public Table find(String SQL) throws ApplicationException {
        logger.severe("find:" + SQL);
        Table table = new Table();
        Row row;
        FieldInfo field;
        Field fields;

        try (DatabaseOperator Operator = new DatabaseOperator()) {
            Operator.createStatement(false);
            Operator.query(SQL);
            int cols = Operator.getResultSet().getMetaData().getColumnCount();
            String[] fieldName = new String[cols], fieldValue = new String[cols];

            for (int i = 0; i < cols; i++) {
                fieldName[i] = Operator.getResultSet().getMetaData().getColumnName(i + 1);
            }

            Object v_field;
            while (Operator.getResultSet().next()) {
                row = new Row();
                fields = new Field();
                for (int i = 0; i < fieldName.length; i++) {
                    v_field = Operator.getResultSet().getObject(i + 1);

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

    public void importPackages(String packageNameList) {

        this.packageList = packageNameList.split(";");
    }
}