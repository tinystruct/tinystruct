/*******************************************************************************
 * Copyright  (c) 2023 James Mover Zhou
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
package org.tinystruct.data.repository;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.DatabaseOperator;
import org.tinystruct.data.component.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;

public class SQLServer extends AbstractDataRepository {

    public SQLServer() {
    }

    @Override
    public boolean append(Field ready_fields, String table)
            throws ApplicationException {
        StringBuilder keys = new StringBuilder();
        StringBuilder dataKeys = new StringBuilder();
        StringBuilder values = new StringBuilder();
        StringBuilder parameters = new StringBuilder();
        String dot = ",";
        String currentProperty;
        FieldInfo currentField;

        for (Enumeration<String> _field = ready_fields.keys(); _field
                .hasMoreElements(); ) {
            currentProperty = _field.nextElement();
            currentField = ready_fields.get(currentProperty);

            if (currentField.autoIncrement()) {
                continue;
            }

            if ("int".equalsIgnoreCase(currentField.getType().getRealType())
                    || currentField.getType() == FieldType.TEXT
                    || currentField.getType() == FieldType.BIT
                    || currentField.getType() == FieldType.DATE
                    || currentField.getType() == FieldType.DATETIME) {
                parameters.append("@").append(currentField.getName()).append(" ").append(currentField.get("type")).append(dot);

                if (currentField.getType() == FieldType.TEXT) {
                    values.append("'").append(currentField.stringValue().replaceAll("'", "''")).append("'").append(dot);
                } else if (currentField.getType() == FieldType.DATE
                        || currentField.getType() == FieldType.DATETIME) {
                    SimpleDateFormat format = new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss");
                    values.append("'").append(format.format(currentField.value())).append("'").append(dot);
                } else if (currentField.getType() == FieldType.BIT) {
                    // 对null未作处理，考虑后面要进行处理
                    if (currentField.value() != null)
                        values.append("true".equals(
                                currentField.value().toString()) ? 1 : 0).append(dot);
                    else
                        values.append("0").append(dot);
                } else
                    values.append(currentField.value()).append(dot);
            } else {
                parameters.append("@").append(currentField.getName()).append(" ").append(currentField.get("type")).append("(").append(currentField.getLength()).append(")").append(dot);
                values.append("'").append(currentField.stringValue().replaceAll("'", "''")).append("'").append(dot);
            }

            dataKeys.append(currentField.getColumnName()).append(dot);
            keys.append("@").append(currentField.getName()).append(dot);
        }

        dataKeys = new StringBuilder(dataKeys.substring(0, dataKeys.length() - 1));
        keys = new StringBuilder(keys.substring(0, keys.length() - 1));
        values = new StringBuilder(values.substring(0, values.length() - 1));
        parameters = new StringBuilder(parameters.substring(0, parameters.length() - 1));

        table = table.replaceAll("\\[", "").replaceAll("\\]", "");
        String SQL = "if not exists (select * from dbo.sysobjects where id = object_id(N'[dbo].["
                + table
                + "_APPEND]') and OBJECTPROPERTY(id, N'IsProcedure') = 1)"
                + "BEGIN exec('CREATE PROCEDURE [dbo].[" + table + "_APPEND] "
                + parameters + " AS INSERT INTO [" + table + "](" + dataKeys
                + ") VALUES(" + keys + ")')"
                + " {call " + table + "_APPEND(" + values + ")} END"
                + " else {call " + table + "_APPEND(" + values + ")}";

        try (DatabaseOperator operator = new DatabaseOperator()) {
            return operator.update(SQL) > 0;
        }
    }

    @Override
    public boolean delete(Object Id, String table) throws ApplicationException {
        String SQL = "DELETE FROM [" + table + "] WHERE id=?";

        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement ps = operator.preparedStatement(SQL, new Object[]{});
            ps.setObject(1, Id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Field ready_fields, String table)
            throws ApplicationException {
        StringBuilder parameters = new StringBuilder();
        StringBuilder values = new StringBuilder();
        StringBuilder expressions = new StringBuilder();

        StringBuilder sql = new StringBuilder();
        StringBuilder keys = new StringBuilder();
        final String dot = ",";
        String currentProperty;
        FieldInfo currentField;

        for (Enumeration<String> _field = ready_fields.keys(); _field
                .hasMoreElements(); ) {
            currentProperty = _field.nextElement();
            currentField = ready_fields.get(currentProperty);

            if (currentField.autoIncrement()) {
                parameters.append("@").append(currentField.getName()).append(" ")
                        .append(currentField.get("type")).append(dot);
                values.append(currentField.value()).append(dot);

                continue;
            }

            if ("int".equalsIgnoreCase(currentField.getType().getRealType())
                    || currentField.getType() == FieldType.TEXT
                    || currentField.getType() == FieldType.BIT
                    || currentField.getType() == FieldType.DATE
                    || currentField.getType() == FieldType.DATETIME) {
                parameters.append("@").append(currentField.getName()).append(" ")
                        .append(currentField.get("type")).append(dot);
                if (currentField.getType() == FieldType.TEXT) {
                    values.append("'")
                            .append(currentField.stringValue().replaceAll("'", "''"))
                            .append("'").append(dot);
                } else if (currentField.getType() == FieldType.DATE
                        || currentField.getType() == FieldType.DATETIME) {
                    SimpleDateFormat format = new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss");
                    values.append("'").append(format.format(currentField.value())).append("'")
                            .append(dot);
                } else if (currentField.getType() == FieldType.BIT)
                    values.append("true".equals(currentField.value().toString()) ? 1
                            : 0).append(dot);

                else
                    values.append(currentField.value()).append(dot);
            } else {
                parameters.append("@").append(currentField.getName()).append(" ")
                        .append(currentField.get("type")).append("(")
                        .append(currentField.getLength())
                        .append(")").append(dot);
                values.append("'")
                        .append(currentField.stringValue().replaceAll("'", "''"))
                        .append("'").append(dot);
            }

            keys.append("@").append(currentField.getName());
            expressions.append("[").append(currentField.getColumnName()).append("]=").append(keys)
                    .append(dot);
        }

        values.setLength(values.length() - 1);
        expressions.setLength(expressions.length() - 1);
        parameters.setLength(parameters.length() - 1);
        table = table.replaceAll("\\[", "").replaceAll("\\]", "");

        sql.append("if not exists (select * from dbo.sysobjects where id = object_id(N'[dbo].[")
                .append(table)
                .append("_EDIT]') and OBJECTPROPERTY(id, N'IsProcedure') = 1)");
        sql.append("BEGIN exec('CREATE PROCEDURE [dbo].[").append(table).append("_EDIT] ")
                .append(parameters).append(" AS UPDATE [").append(table).append("] SET ").append(expressions)
                .append(" WHERE id=@Id')");
        sql.append(" {call ").append(table).append("_EDIT(").append(values).append(")} END");
        sql.append(" else {call ").append(table).append("_EDIT(").append(values).append(")}");

        try (DatabaseOperator operator = new DatabaseOperator()) {
            return operator.update(sql.toString()) > 0;
        }
    }

    @Override
    public Type getType() {
        return Type.SQLServer;
    }

    @Override
    public Table find(String SQL, Object[] parameters) throws ApplicationException {

        Table table = new Table();
        Row row;
        FieldInfo field;
        Field fields;

        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement preparedStatement = operator.preparedStatement(SQL, parameters);
            ResultSet resultSet = operator.executeQuery(preparedStatement);
            int cols = resultSet.getMetaData().getColumnCount();
            String[] fieldName = new String[cols];
            String[] fieldType = new String[cols];
            Object[] fieldValue = new Object[cols];

            for (int i = 0; i < cols; i++) {
                fieldName[i] = resultSet.getMetaData().getColumnName(i + 1);
                fieldType[i] = resultSet.getMetaData().getColumnTypeName(i + 1);
            }

            Object v_field;
            while (resultSet.next()) {
                row = new Row();
                fields = new Field();
                for (int i = 0; i < fieldName.length; i++) {
                    // First check if the value is NULL
                    if (resultSet.getObject(i + 1) == null) {
                        v_field = null;
                    } else {
                        // Get the appropriate data type based on fieldType[i]
                        // SQL Server data types: https://docs.microsoft.com/en-us/sql/t-sql/data-types/data-types-transact-sql
                        String type = fieldType[i].toUpperCase();

                        try {
                            if (type.contains("INT") || type.equals("SMALLINT") || type.equals("TINYINT")) {
                                // Handle all integer types (INT, SMALLINT, TINYINT, BIGINT)
                                if (type.equals("BIGINT")) {
                                    v_field = resultSet.getLong(i + 1);
                                } else {
                                    v_field = resultSet.getInt(i + 1);
                                }
                            } else if (type.equals("DECIMAL") || type.equals("NUMERIC") ||
                                      type.equals("MONEY") || type.equals("SMALLMONEY")) {
                                // Handle decimal types
                                v_field = resultSet.getBigDecimal(i + 1);
                            } else if (type.equals("FLOAT") || type.equals("REAL")) {
                                // Handle floating-point types
                                if (type.equals("REAL")) {
                                    v_field = resultSet.getFloat(i + 1);
                                } else {
                                    v_field = resultSet.getDouble(i + 1);
                                }
                            } else if (type.equals("BIT")) {
                                // Handle boolean types
                                v_field = resultSet.getBoolean(i + 1);
                            } else if (type.equals("DATE") || type.equals("DATETIME") ||
                                      type.equals("DATETIME2") || type.equals("SMALLDATETIME") ||
                                      type.equals("DATETIMEOFFSET")) {
                                // Handle date/time types
                                v_field = resultSet.getTimestamp(i + 1);
                            } else if (type.equals("TIME")) {
                                // Handle time type
                                v_field = resultSet.getTime(i + 1);
                            } else if (type.equals("BINARY") || type.equals("VARBINARY") ||
                                      type.equals("IMAGE") || type.contains("BLOB")) {
                                // Handle binary data
                                v_field = resultSet.getBytes(i + 1);
                            } else if (type.equals("UNIQUEIDENTIFIER")) {
                                // Handle GUID/UUID
                                v_field = resultSet.getString(i + 1);
                            } else if (type.equals("XML")) {
                                // Handle XML data
                                v_field = resultSet.getString(i + 1);
                            } else {
                                // Default to getString for CHAR, VARCHAR, NCHAR, NVARCHAR, TEXT, NTEXT, etc.
                                v_field = resultSet.getString(i + 1);
                            }
                        } catch (SQLException e) {
                            // Fallback to getObject if specific getter fails
                            v_field = resultSet.getObject(i + 1);
                        }
                    }

                    // Keep null values as null instead of converting to empty string
                    fieldValue[i] = v_field;
                    field = new FieldInfo();
                    field.append("name", fieldName[i]);
                    field.append("value", fieldValue[i]);
                    field.append("type", fieldType[i]);

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
