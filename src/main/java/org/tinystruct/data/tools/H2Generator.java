package org.tinystruct.data.tools;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Field;
import org.tinystruct.data.component.FieldType;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;
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
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class H2Generator extends MySQLGenerator {
    private final static Logger logger = Logger.getLogger(MSSQLGenerator.class.getName());
    private String path;
    private String packageName;
    private String[] packageList;

    public H2Generator() {
        this.path = "src/main/java/org/tinystruct/customer/object";
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
        java_resource.append("   /**\r\n");
        java_resource.append("   * Auto Generated Serial Version UID\r\n");
        java_resource.append("   */\r\n");
        try {
            java_resource.append("  private static final long serialVersionUID = ").append(SecureRandom.getInstance("SHA1PRNG").nextLong()).append("L;\r\n");
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        Element rootElement = new Element("mapping");
        Element classElement = rootElement.addElement("class");

        classElement.setAttribute("name", className);
        classElement.setAttribute("table", table);

        String command = "show columns from " + table + "";
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

                propertyName = StringUtilities.setCharToUpper(currentFields.get("COLUMN_NAME").value().toString().toLowerCase(Locale.ROOT), '_');
                propertyName = StringUtilities.remove(propertyName, '_');

                String propertyNameOfMethod = StringUtilities.setCharToUpper(propertyName, 0);

                propertyTypeValue = currentFields.get("TYPE").value().toString();

                String[] props = propertyTypeValue.split("\\(");
                propertyType = FieldType.valueOf(props[0]).getRealType();

                if (java_method_tostring.length() > 0) spliter = ",";

                if (currentFields.get("COLUMN_NAME").value().toString().equalsIgnoreCase("id")) {
                    increment = propertyTypeValue.indexOf("BIGINT") != -1 && currentFields.get("KEY").stringValue().indexOf("PRI") != -1 && currentFields.get("IS_NULLABLE").stringValue().indexOf("NO") != -1;

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
                    if (!propertyType.endsWith("[]")) {
                        java_method_declaration.append("\t\tthis.").append(propertyName).append(" = this.setFieldAs").append(StringUtilities.setCharToUpper(propertyType, 0)).append("(\"").append(propertyName).append("\",").append(propertyName).append(");\r\n");
                    } else {
                        java_method_declaration.append("\t\tthis.").append(propertyName).append(" = this.setFieldAs").append(StringUtilities.setCharToUpper(propertyType.replace("[]", "Array"), 0)).append("(\"").append(propertyName).append("\",").append(propertyName).append(");\r\n");
                    }
                    java_method_declaration.append("\t}\r\n\r\n");

                    if ("String".equalsIgnoreCase(propertyType) || "Date".equalsIgnoreCase(propertyType)) {
                        java_method_tostring.append("\t\tbuffer.append(\"").append(spliter).append("\\\"").append(propertyName).append("\\\":\\\"\"+this.get").append(propertyNameOfMethod).append("()+\"\\\"\");\r\n");
                    } else if ("byte[]".equalsIgnoreCase(propertyType)) {
                        java_method_tostring.append("\t\tif (this.get").append(propertyNameOfMethod).append("() != null) {\r\n");
                        java_method_tostring.append("\t\t\tbuffer.append(\"").append(spliter).append("\\\"").append(propertyName).append("\\\":\\\"binary-data\\\"\");\r\n");
                        java_method_tostring.append("\t\t} else {\r\n");
                        java_method_tostring.append("\t\t\tbuffer.append(\"").append(spliter).append("\\\"").append(propertyName).append("\\\":null\");\r\n");
                        java_method_tostring.append("\t\t}\r\n");
                    } else {
                        java_method_tostring.append("\t\tbuffer.append(\"").append(spliter).append("\\\"").append(propertyName).append("\\\":\"+this.get").append(propertyNameOfMethod).append("());\r\n");
                    }

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
                if (propertyType.endsWith("[]")) {
                    java_method_setdata.append("\tthis.set").append(propertyNameOfMethod).append("(row.getFieldInfo(\"").append(currentFields.get("COLUMN_NAME").value().toString()).append("\").").append(propertyType.replace("[]", "Array")).append("Value());\r\n");
                } else {
                    java_method_setdata.append("\tthis.set").append(propertyNameOfMethod).append("(row.getFieldInfo(\"").append(currentFields.get("COLUMN_NAME").value().toString()).append("\").").append(StringUtilities.setCharToLower(propertyType, 0)).append("Value());\r\n");
                }
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

    @Override
    public void importPackages(String packageNameList) {
        this.packageList = packageNameList.split(";");
    }
}
