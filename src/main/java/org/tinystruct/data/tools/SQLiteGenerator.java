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

public class SQLiteGenerator implements Generator {
    private String fileName;
    private String packageName;

    private final static Logger logger = Logger.getLogger("SQLiteGenerator.class");
    ;
    private String[] packageList;

    public SQLiteGenerator() {
        this.fileName = "resources/org/tinystruct/customer/object/";
        this.packageList = new String[]{};
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public static void main(String[] args) {
        System.out.println();
		
/*		String[] props="varchar(100)".split("\\(");
		System.out.println(props[0]);
		System.out.println(props[1].split("\\)")[0]);*/

        String[] props = "varchar".split("\\(");
        System.out.println(props[0]);
        System.out.println(props[1].split("\\)")[0]);
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
            java_resource.append("package " + this.packageName + ";\r\n");
        } else {
            java_resource.append("package org.tinystruct.customer.object;\r\n");
        }

        java_resource.append("import java.io.Serializable;\r\n");

        if (this.packageList.length > 0) {
            java_resource.append("\r\n");
            for (int i = 0; i < this.packageList.length; i++) {
                java_resource.append("import " + this.packageList[i] + ";\r\n");
            }
        }

        java_resource.append("\r\n");
        java_resource.append("import org.tinystruct.data.component.Row;\r\n");
        java_resource.append("import org.tinystruct.data.component.AbstractData;\r\n\r\n");
        java_resource.append("public class " + className + " extends AbstractData implements Serializable {\r\n");
        java_resource.append("	/**\r\n");
        java_resource.append("   * Auto Generated Serial Version UID\r\n");
        java_resource.append("   */\r\n");
        java_resource.append("  private static final long serialVersionUID = " + new SecureRandom().nextLong() + "L;\r\n");

        Element rootElement = new Element("mapping");
        Element classElement = rootElement.addElement("class");

        classElement.setAttribute("name", className);
        classElement.setAttribute("table", table);

        String command = "PRAGMA table_info (" + table + ")";
        Table data = this.find(command);
        Iterator<Row> listRow = data.iterator();
        Row currentRow;

        String propertyName, propertyType, propertyTypeValue;
        boolean increment = false;
        while (listRow.hasNext()) {
            currentRow = listRow.next();

            Iterator<Field> fields = currentRow.iterator();
            Field currentFields;

            System.out.println(currentRow);
            while (fields.hasNext()) {
                currentFields = fields.next();

                System.out.println(currentFields);
                propertyName = StringUtilities.setCharToUpper(currentFields.get("name").value().toString(), '_');
                propertyName = new StringUtilities(propertyName).remove('_');

                String propertyNameOfMethod = StringUtilities.setCharToUpper(propertyName, 0);

                propertyTypeValue = currentFields.get("type").value().toString();

                String[] props = propertyTypeValue.split("\\(");
                propertyType = FieldType.valueOf(props[0]).getRealType();

                if (java_method_tostring.length() > 0) spliter = ",";

                if (currentFields.get("name").value().equals("id")) {
//					increment=currentFields.get("Extra").stringValue().indexOf("auto_increment")!=-1;

                    if (propertyType.equalsIgnoreCase("String"))
                        java_method_tostring.append("\t\tbuffer.append(\"" + spliter + "\\\"" + propertyNameOfMethod + "\\\":\\\"\"+this.get" + propertyNameOfMethod + "()+\"\\\"\");\r\n");
                    else
                        java_method_tostring.append("\t\tbuffer.append(\"" + spliter + "\\\"" + propertyNameOfMethod + "\\\":\"+this.get" + propertyNameOfMethod + "());\r\n");

                    Element idElement = classElement.addElement("id");

                    idElement.setAttribute("name", propertyNameOfMethod);
                    idElement.setAttribute("column", currentFields.get("name").value().toString());
                    idElement.setAttribute("increment", String.valueOf(increment));
                    idElement.setAttribute("generate", String.valueOf(!increment));
                    idElement.setAttribute("length", props.length > 1 ? props[1].split("\\)")[0] : "0");
                    idElement.setAttribute("type", props[0]);

                    if (propertyType.equalsIgnoreCase("String")) {
                        java_method_declaration.append("\tpublic " + propertyType + " get" + propertyNameOfMethod + "()\r\n");
                        java_method_declaration.append("\t{\r\n");
                        java_method_declaration.append("\t\treturn String.valueOf(this." + propertyNameOfMethod + ");\r\n");
                    } else if (propertyType.equalsIgnoreCase("int")) {
                        java_method_declaration.append("\tpublic Integer get" + propertyNameOfMethod + "()\r\n");
                        java_method_declaration.append("\t{\r\n");
                        java_method_declaration.append("\t\treturn Integer.parseInt(this." + propertyNameOfMethod + ".toString());\r\n");
                    }

                    java_method_declaration.append("\t}\r\n\r\n");
                } else {
                    java_member_declaration.append("\tprivate " + propertyType + " " + propertyName + ";\r\n");

                    java_method_declaration.append("\tpublic void set" + propertyNameOfMethod + "(" + propertyType + " " + propertyName + ")\r\n");
                    java_method_declaration.append("\t{\r\n");
                    java_method_declaration.append("\t\tthis." + propertyName + "=this.setFieldAs" + StringUtilities.setCharToUpper(propertyType, 0) + "(\"" + propertyName + "\"," + propertyName + ");\r\n");
                    java_method_declaration.append("\t}\r\n\r\n");

                    if (propertyType.equalsIgnoreCase("String") || propertyType.equalsIgnoreCase("Date"))
                        java_method_tostring.append("\t\tbuffer.append(\"" + spliter + "\\\"" + propertyName + "\\\":\\\"\"+this.get" + propertyNameOfMethod + "()+\"\\\"\");\r\n");
                    else
                        java_method_tostring.append("\t\tbuffer.append(\"" + spliter + "\\\"" + propertyName + "\\\":\"+this.get" + propertyNameOfMethod + "());\r\n");

                    Element propertyElement = classElement.addElement("property");

                    propertyElement.setAttribute("name", propertyName);
                    propertyElement.setAttribute("column", currentFields.get("name").value().toString());
                    propertyElement.setAttribute("length", props.length > 1 ? props[1].split("\\)")[0] : "0");
                    propertyElement.setAttribute("type", props[0]);

                    java_method_declaration.append("\tpublic " + propertyType + " get" + propertyNameOfMethod + "()\r\n");
                    java_method_declaration.append("\t{\r\n");
                    java_method_declaration.append("\t\treturn this." + propertyName + ";\r\n");
                    java_method_declaration.append("\t}\r\n\r\n");
                }

                java_method_setdata.append("\t\tif(row.getFieldInfo(\"" + currentFields.get("name").value().toString() + "\")!=null)");
                java_method_setdata.append("\tthis.set" + propertyNameOfMethod + "(row.getFieldInfo(\"" + currentFields.get("name").value().toString() + "\")." + propertyType.toLowerCase() + "Value());\r\n");

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