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
import java.util.Enumeration;

public class MySQLServer extends AbstractDataRepository {

    public static final String COMMA = ",";

    public MySQLServer() {
    }

    /**
     * Append new records to the MySQL database table.
     *
     * @param ready_fields The fields ready for insertion.
     * @param table        The table name.
     * @return True if the record is successfully appended, otherwise false.
     * @throws ApplicationException if there is an error appending the record.
     */
    @Override
    public boolean append(Field ready_fields, String table) throws ApplicationException {

        int i = 0, cols = ready_fields.size();
        String[] columns = new String[cols];
        FieldInfo[] fields = new FieldInfo[cols];

        String key;
        StringBuilder expressions = new StringBuilder(), values = new StringBuilder();
        Enumeration<String> keys = ready_fields.keys();
        while (keys.hasMoreElements()) {
            key = keys.nextElement();
            if (!ready_fields.get(key).autoIncrement()) {
                columns[i] = ready_fields.get(key).getColumnName();
                fields[i] = ready_fields.get(key);

                if (expressions.length() == 0)
                    expressions.append("`").append(columns[i]).append("`");
                else
                    expressions.append(COMMA).append("`").append(columns[i]).append("`");

                if (values.length() == 0)
                    values.append('?');
                else
                    values.append(COMMA).append('?');

                i++;
            }
        }

        String SQL = "INSERT INTO " + table + " (" + expressions + ") VALUES(" + values + ")";
        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement ps = operator.preparedStatement(SQL, new Object[]{});
            setParameters(ps, fields);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ApplicationException("Error appending record: " + e.getMessage(), e);
        }
    }

    /**
     * Append a new record to the database and return the generated ID.
     *
     * @param ready_fields The fields ready for insertion.
     * @param table        The table name.
     * @return The generated ID if the operation is successful, null otherwise.
     * @throws ApplicationException if there is an error appending the record.
     */
    @Override
    public Object appendAndGetId(Field ready_fields, String table) throws ApplicationException {
        int i = 0, cols = ready_fields.size();
        String[] columns = new String[cols];
        FieldInfo[] fields = new FieldInfo[cols];

        String key;
        StringBuilder expressions = new StringBuilder(), values = new StringBuilder();
        Enumeration<String> keys = ready_fields.keys();
        while (keys.hasMoreElements()) {
            key = keys.nextElement();
            if (!ready_fields.get(key).autoIncrement()) {
                columns[i] = ready_fields.get(key).getColumnName();
                fields[i] = ready_fields.get(key);

                if (expressions.length() == 0)
                    expressions.append("`").append(columns[i]).append("`");
                else
                    expressions.append(COMMA).append("`").append(columns[i]).append("`");

                if (values.length() == 0)
                    values.append('?');
                else
                    values.append(COMMA).append('?');

                i++;
            }
        }

        String SQL = "INSERT INTO " + table + " (" + expressions + ") VALUES(" + values + ")";
        try (DatabaseOperator operator = new DatabaseOperator()) {
            // Create a prepared statement that returns generated keys
            PreparedStatement ps = operator.createPreparedStatement(SQL, false, true);
            setParameters(ps, fields);

            // Execute the update and get the generated ID
            return operator.executeUpdateAndGetGeneratedId(ps);
        } catch (SQLException e) {
            throw new ApplicationException("Error appending record: " + e.getMessage(), e);
        }
    }

    /**
     * Update existing records in the MySQL database table.
     *
     * @param ready_fields The fields ready for update.
     * @param table        The table name.
     * @return True if the record is successfully updated, otherwise false.
     * @throws ApplicationException if there is an error updating the record.
     */
    @Override
    public boolean update(Field ready_fields, String table) throws ApplicationException {
        String key, id = "id";
        Object Id = null;
        StringBuilder expressions = new StringBuilder();

        int i = 0, cols = ready_fields.size();
        String[] columns = new String[cols];
        FieldInfo[] values = new FieldInfo[cols];
        Enumeration<String> keys = ready_fields.keys();
        while (keys.hasMoreElements()) {
            key = keys.nextElement();
            if (key.equalsIgnoreCase("Id")) {
                Id = ready_fields.get(key).value();
                id = ready_fields.get(key).getColumnName();
                continue;
            }

            columns[i] = ready_fields.get(key).getColumnName();
            values[i] = ready_fields.get(key);

            if (expressions.length() == 0)
                expressions.append("`").append(columns[i]).append("`").append("=?");
            else
                expressions.append(COMMA).append("`").append(columns[i]).append("`").append("=?");

            i++;
        }

        String SQL = "UPDATE " + table + " SET " + expressions + " WHERE " + id + "=?";
        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement ps = operator.preparedStatement(SQL, new Object[]{});
            setParameters(ps, values);
            ps.setObject(ready_fields.size(), Id); // Set Id parameter

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ApplicationException("Error updating record: " + e.getMessage(), e);
        }
    }

    /**
     * Get the type of the repository, which is MySQL in this case.
     *
     * @return The repository type.
     */
    @Override
    public Type getType() {
        return Type.MySQL;
    }

    /**
     * Retrieve records from the MySQL database table based on the provided SQL query.
     *
     * @param SQL        The SQL query to retrieve records.
     * @param parameters The parameters to be used in the SQL query.
     * @return A table containing the retrieved records.
     * @throws ApplicationException if there is an error retrieving records.
     */
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
            String fieldName;
            String fieldTypeName;
            Object fieldValue;
            while (resultSet.next()) {
                row = new Row();
                fields = new Field();
                for (int i = 0; i < cols; i++) {
                    fieldName = resultSet.getMetaData().getColumnName(i + 1);
                    fieldTypeName = resultSet.getMetaData().getColumnTypeName(i + 1);

                    // First check if the value is NULL
                    if (resultSet.getObject(i + 1) == null) {
                        fieldValue = null;
                    } else {
                        // Get the appropriate data type based on fieldTypeName
                        // MySQL data types: https://dev.mysql.com/doc/refman/8.0/en/data-types.html
                        String type = fieldTypeName.toUpperCase();

                        try {
                            if (type.contains("INT") || type.equals("YEAR")) {
                                // Handle all integer types (TINYINT, SMALLINT, MEDIUMINT, INT, BIGINT, YEAR)
                                if (type.equals("BIGINT") || type.equals("MEDIUMINT")) {
                                    fieldValue = resultSet.getLong(i + 1);
                                } else {
                                    fieldValue = resultSet.getInt(i + 1);
                                }
                            } else if (type.equals("DECIMAL") || type.equals("NUMERIC") ||
                                      type.contains("FLOAT") || type.contains("DOUBLE")) {
                                // Handle all floating-point types
                                if (type.contains("FLOAT")) {
                                    fieldValue = resultSet.getFloat(i + 1);
                                } else {
                                    fieldValue = resultSet.getDouble(i + 1);
                                }
                            } else if (type.equals("BIT") || type.equals("BOOLEAN") || type.equals("BOOL")) {
                                // Handle boolean types
                                fieldValue = resultSet.getBoolean(i + 1);
                            } else if (type.contains("DATE") || type.contains("TIME")) {
                                // Handle date/time types (DATE, DATETIME, TIMESTAMP, TIME)
                                if (type.equals("DATE")) {
                                    fieldValue = resultSet.getDate(i + 1);
                                } else if (type.equals("TIME")) {
                                    fieldValue = resultSet.getTime(i + 1);
                                } else {
                                    fieldValue = resultSet.getTimestamp(i + 1);
                                }
                            } else if (type.equals("BLOB") || type.equals("BINARY") ||
                                      type.equals("VARBINARY") || type.equals("TINYBLOB") ||
                                      type.equals("MEDIUMBLOB") || type.equals("LONGBLOB")) {
                                // Handle binary data
                                fieldValue = resultSet.getBytes(i + 1);
                            } else {
                                // Default to getString for TEXT, VARCHAR, CHAR, etc.
                                fieldValue = resultSet.getString(i + 1);
                            }
                        } catch (SQLException e) {
                            // Fallback to getObject if specific getter fails
                            fieldValue = resultSet.getObject(i + 1);
                        }
                    }

                    field = new FieldInfo();
                    field.append("name", fieldName);
                    field.append("value", fieldValue);
                    field.append("type", fieldTypeName);

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

    /**
     * Retrieve a single record from the MySQL database table based on the provided SQL query.
     *
     * @param SQL        The SQL query to retrieve the record.
     * @param parameters The parameters to be used in the SQL query.
     * @return A row containing the retrieved record.
     * @throws ApplicationException if there is an error retrieving the record.
     */
    @Override
    public Row findOne(String SQL, Object[] parameters) throws ApplicationException {
        Row row = new Row();
        FieldInfo fieldInfo;
        Field field = new Field();

        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement preparedStatement = operator.preparedStatement(SQL, parameters);
            ResultSet resultSet = operator.executeQuery(preparedStatement);
            int cols = resultSet.getMetaData().getColumnCount();
            String fieldName;
            String fieldTypeName;
            Object fieldValue;
            if (resultSet.next()) {
                for (int i = 0; i < cols; i++) {
                    fieldName = resultSet.getMetaData().getColumnName(i + 1);
                    fieldTypeName = resultSet.getMetaData().getColumnTypeName(i + 1);

                    // First check if the value is NULL
                    if (resultSet.getObject(i + 1) == null) {
                        fieldValue = null;
                    } else {
                        // Get the appropriate data type based on fieldTypeName
                        // MySQL data types: https://dev.mysql.com/doc/refman/8.0/en/data-types.html
                        String type = fieldTypeName.toUpperCase();

                        try {
                            if (type.contains("INT") || type.equals("YEAR")) {
                                // Handle all integer types (TINYINT, SMALLINT, MEDIUMINT, INT, BIGINT, YEAR)
                                if (type.equals("BIGINT") || type.equals("MEDIUMINT")) {
                                    fieldValue = resultSet.getLong(i + 1);
                                } else {
                                    fieldValue = resultSet.getInt(i + 1);
                                }
                            } else if (type.equals("DECIMAL") || type.equals("NUMERIC") ||
                                      type.contains("FLOAT") || type.contains("DOUBLE")) {
                                // Handle all floating-point types
                                if (type.contains("FLOAT")) {
                                    fieldValue = resultSet.getFloat(i + 1);
                                } else {
                                    fieldValue = resultSet.getDouble(i + 1);
                                }
                            } else if (type.equals("BIT") || type.equals("BOOLEAN") || type.equals("BOOL")) {
                                // Handle boolean types
                                fieldValue = resultSet.getBoolean(i + 1);
                            } else if (type.contains("DATE") || type.contains("TIME")) {
                                // Handle date/time types (DATE, DATETIME, TIMESTAMP, TIME)
                                if (type.equals("DATE")) {
                                    fieldValue = resultSet.getDate(i + 1);
                                } else if (type.equals("TIME")) {
                                    fieldValue = resultSet.getTime(i + 1);
                                } else {
                                    fieldValue = resultSet.getTimestamp(i + 1);
                                }
                            } else if (type.equals("BLOB") || type.equals("BINARY") ||
                                      type.equals("VARBINARY") || type.equals("TINYBLOB") ||
                                      type.equals("MEDIUMBLOB") || type.equals("LONGBLOB")) {
                                // Handle binary data
                                fieldValue = resultSet.getBytes(i + 1);
                            } else {
                                // Default to getString for TEXT, VARCHAR, CHAR, etc.
                                fieldValue = resultSet.getString(i + 1);
                            }
                        } catch (SQLException e) {
                            // Fallback to getObject if specific getter fails
                            fieldValue = resultSet.getObject(i + 1);
                        }
                    }

                    fieldInfo = new FieldInfo();
                    fieldInfo.append("name", fieldName);
                    fieldInfo.append("value", fieldValue);
                    fieldInfo.append("type", fieldTypeName);

                    field.append(fieldInfo.getName(), fieldInfo);
                }

                row.append(field);
            }
        } catch (Exception e) {
            throw new ApplicationException(e.getMessage(), e);
        }

        return row;
    }

    private String generateInsertSQL(String[] columns, String table) {
        StringBuilder _columns = new StringBuilder();
        StringBuilder values = new StringBuilder();

        for (String fieldName : columns) {
            if (_columns.length() > 0) {
                _columns.append(", ");
                values.append(", ");
            }
            _columns.append("`").append(fieldName).append("`");
            values.append("?");
        }

        return "INSERT INTO " + table + " (" + _columns + ") VALUES (" + values + ")";
    }

    private void setParameters(PreparedStatement ps, FieldInfo[] values) throws SQLException {
        int i = 1;
        for (FieldInfo fieldInfo : values) {
            if (fieldInfo != null && !fieldInfo.autoIncrement()) {
                Object value = fieldInfo.value();
                if ("int".equalsIgnoreCase(fieldInfo.getType().getRealType())) {
                    ps.setInt(i++, fieldInfo.intValue());
                } else if (fieldInfo.getType() == FieldType.TEXT) {
                    ps.setString(i++, fieldInfo.stringValue());
                } else if (fieldInfo.getType() == FieldType.DATE || fieldInfo.getType() == FieldType.DATETIME) {
                    ps.setDate(i++, new Date(fieldInfo.dateValue().getTime()));
                } else if (fieldInfo.getType() == FieldType.BIT) {
                    ps.setBoolean(i++, fieldInfo.booleanValue());
                } else {
                    ps.setObject(i++, value);
                }
            }
        }
    }
}
