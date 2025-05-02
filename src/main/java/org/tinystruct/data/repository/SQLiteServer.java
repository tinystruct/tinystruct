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
package org.tinystruct.data.repository;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.DatabaseOperator;
import org.tinystruct.data.component.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public class SQLiteServer extends AbstractDataRepository {

    public SQLiteServer() {
    }

    @Override
    public boolean append(Field ready_fields, String table)
            throws ApplicationException {
        String dot = ",", currentProperty;
        StringBuilder expressions = new StringBuilder(), values = new StringBuilder();
        FieldInfo currentField;

        List<String> fieldNames = new ArrayList<String>();
        for (Enumeration<String> _field = ready_fields.keys(); _field
                .hasMoreElements(); ) {
            currentProperty = _field.nextElement();

            currentField = ready_fields.get(currentProperty);
            if (currentField.autoIncrement()) {
                continue;
            }

            fieldNames.add(currentProperty);

            if (expressions.length() == 0)
                expressions.append("[").append(currentField.getColumnName()).append("]");
            else
                expressions.append(dot).append("[").append(currentField.getColumnName()).append("]");

            if (values.length() == 0)
                values.append('?');
            else
                values.append(dot).append('?');
        }

        String SQL = "INSERT INTO " + table + " (" + expressions + ") VALUES("
                + values + ")";

        Iterator<String> iterator = fieldNames.iterator();
        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement ps = operator.preparedStatement(SQL, new Object[]{});
            int i = 1;
            while (iterator.hasNext()) {
                currentField = ready_fields.get(iterator.next());

                if (currentField.autoIncrement()) {
                    continue;
                }

                if ("int"
                        .equalsIgnoreCase(currentField.getType().getRealType())) {
                    ps.setInt(i++, currentField.intValue());
                } else if (currentField.getType() == FieldType.TEXT || currentField.getType() == FieldType.LONGTEXT) {
                    ps.setString(i++, currentField.stringValue());
                } else if (currentField.getType() == FieldType.DATE
                        || currentField.getType() == FieldType.DATETIME) {
                    ps.setDate(i++, new Date(currentField.dateValue()
                            .getTime()));
                } else if (currentField.getType() == FieldType.BIT || currentField.getType() == FieldType.BOOLEAN) {
                    ps.setBoolean(i++, currentField.booleanValue());
                } else {
                    ps.setObject(i++, currentField.value());
                }
            }

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Append a new record to the database and return the generated ID.
     *
     * @param ready_fields the fields ready for insertion.
     * @param table        the table to append the record to.
     * @return the generated ID if the operation is successful, null otherwise.
     * @throws ApplicationException if an application-specific error occurs.
     */
    @Override
    public Object appendAndGetId(Field ready_fields, String table) throws ApplicationException {
        String dot = ",", currentProperty;
        StringBuilder expressions = new StringBuilder(), values = new StringBuilder();
        FieldInfo currentField;

        List<String> fieldNames = new ArrayList<String>();
        for (Enumeration<String> _field = ready_fields.keys(); _field.hasMoreElements(); ) {
            currentProperty = _field.nextElement();

            currentField = ready_fields.get(currentProperty);
            if (currentField.autoIncrement()) {
                continue;
            }

            fieldNames.add(currentProperty);

            if (expressions.length() == 0)
                expressions.append("[").append(currentField.getColumnName()).append("]");
            else
                expressions.append(dot).append("[").append(currentField.getColumnName()).append("]");

            if (values.length() == 0)
                values.append('?');
            else
                values.append(dot).append('?');
        }

        String SQL = "INSERT INTO " + table + " (" + expressions + ") VALUES("
                + values + ")";

        try (DatabaseOperator operator = new DatabaseOperator()) {
            // Create a prepared statement that returns generated keys
            PreparedStatement ps = operator.createPreparedStatement(SQL, false, true);

            Iterator<String> iterator = fieldNames.iterator();
            int i = 1;
            while (iterator.hasNext()) {
                currentField = ready_fields.get(iterator.next());

                if (currentField.autoIncrement()) {
                    continue;
                }

                if ("int".equalsIgnoreCase(currentField.getType().getRealType())) {
                    ps.setInt(i++, currentField.intValue());
                } else if (currentField.getType() == FieldType.TEXT || currentField.getType() == FieldType.LONGTEXT) {
                    ps.setString(i++, currentField.stringValue());
                } else if (currentField.getType() == FieldType.DATE || currentField.getType() == FieldType.DATETIME) {
                    ps.setDate(i++, new Date(currentField.dateValue().getTime()));
                } else if (currentField.getType() == FieldType.BIT || currentField.getType() == FieldType.BOOLEAN) {
                    ps.setBoolean(i++, currentField.booleanValue());
                } else {
                    ps.setObject(i++, currentField.value());
                }
            }

            // Execute the update and get the generated ID
            return operator.executeUpdateAndGetGeneratedId(ps);
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Field ready_fields, String table)
            throws ApplicationException {
        String dot = ",", currentProperty;
        StringBuilder expressions = new StringBuilder();
        FieldInfo currentField;

        Object Id = null;
        List<String> fieldNames = new ArrayList<String>();
        for (Enumeration<String> _field = ready_fields.keys(); _field
                .hasMoreElements(); ) {
            currentProperty = _field.nextElement();
            currentField = ready_fields.get(currentProperty);
            if ("Id".equalsIgnoreCase(currentField.getName())) {
                Id = currentField.value();

                continue;
            }

            if (currentField.value() != null) {
                fieldNames.add(currentProperty);

                if (expressions.length() == 0)
                    expressions.append("[").append(currentField.getColumnName()).append("]").append("=?");
                else
                    expressions.append(dot).append("[").append(currentField.getColumnName()).append("]").append("=?");
            }
        }

        String SQL = "UPDATE " + table + " SET " + expressions + " WHERE id=?";
        Iterator<String> iterator = fieldNames.iterator();
        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement ps = operator.preparedStatement(SQL, new Object[]{});
            int i = 1;
            while (iterator.hasNext()) {
                currentField = ready_fields.get(iterator.next());

                if ("int".equalsIgnoreCase(currentField.getType().getRealType())) {
                    ps.setInt(i++, currentField.intValue());
                } else if (currentField.getType() == FieldType.TEXT || currentField.getType() == FieldType.LONGTEXT) {
                    ps.setString(i++, currentField.stringValue());
                } else if (currentField.getType() == FieldType.DATE
                        || currentField.getType() == FieldType.DATETIME) {
                    ps.setDate(i++, new Date(currentField.dateValue()
                            .getTime()));
                } else if (currentField.getType() == FieldType.BIT || currentField.getType() == FieldType.BOOLEAN) {
                    ps.setBoolean(i++, currentField.booleanValue());
                } else {
                    ps.setObject(i++, currentField.value());
                }
            }

            ps.setObject(i, Id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    @Override
    public Type getType() {
        return Type.SQLite;
    }

    @Override
    public Table find(String SQL, Object[] parameters)
            throws ApplicationException {
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
                fieldName[i] = resultSet.getMetaData()
                        .getColumnName(i + 1);
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
                        // SQLite has 5 main storage classes: NULL, INTEGER, REAL, TEXT, and BLOB
                        String type = fieldType[i].toUpperCase();

                        if (type.contains("INT")) {
                            // Handle all integer types (INTEGER, INT, SMALLINT, TINYINT, BIGINT)
                            try {
                                // For larger integers, use getLong to avoid overflow
                                if (type.equals("BIGINT")) {
                                    v_field = resultSet.getLong(i + 1);
                                } else {
                                    v_field = resultSet.getInt(i + 1);
                                }
                            } catch (SQLException e) {
                                // Fallback to getLong if getInt fails
                                v_field = resultSet.getLong(i + 1);
                            }
                        } else if (type.equals("REAL") ||
                                  type.contains("FLOAT") ||
                                  type.contains("DOUBLE") ||
                                  type.contains("NUMERIC") ||
                                  type.contains("DECIMAL")) {
                            // Handle all floating-point types
                            try {
                                v_field = resultSet.getDouble(i + 1);
                            } catch (SQLException e) {
                                // Fallback to getFloat if getDouble fails
                                v_field = resultSet.getFloat(i + 1);
                            }
                        } else if (type.contains("BOOL") || type.equals("BIT")) {
                            // Handle boolean types
                            v_field = resultSet.getBoolean(i + 1);
                        } else if (type.contains("DATE") ||
                                  type.contains("TIME")) {
                            // Handle date/time types
                            try {
                                v_field = resultSet.getTimestamp(i + 1);
                            } catch (SQLException e) {
                                // Fallback to getString if getTimestamp fails
                                v_field = resultSet.getString(i + 1);
                            }
                        } else if (type.equals("BLOB") ||
                                  type.contains("BINARY")) {
                            // Handle binary data
                            try {
                                v_field = resultSet.getBytes(i + 1);
                            } catch (SQLException e) {
                                // Fallback to getObject if getBytes fails
                                v_field = resultSet.getObject(i + 1);
                            }
                        } else {
                            // Default to getString for TEXT, VARCHAR, CHAR, etc.
                            v_field = resultSet.getString(i + 1);
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
