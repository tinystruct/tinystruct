/*******************************************************************************
 * Copyright  (c) 2013, 2026 James M. ZHOU
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

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class PostgreSQLServer extends AbstractDataRepository {

    public PostgreSQLServer() {
    }

    /**
     * Append new records to the PostgreSQL database table.
     *
     * @param ready_fields The fields ready for insertion.
     * @param table        The table name.
     * @return True if the record is successfully appended, otherwise false.
     * @throws ApplicationException if there is an error appending the record.
     */
    @Override
    public boolean append(Field ready_fields, String table) throws ApplicationException {
        List<String> fieldNames = new ArrayList<>();
        StringBuilder expressions = new StringBuilder();
        StringBuilder values = new StringBuilder();
        buildInsertFragments(ready_fields, fieldNames, expressions, values);

        String SQL = "INSERT INTO " + table + " (" + expressions + ") VALUES(" + values + ")";

        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement ps = operator.preparedStatement(SQL, new Object[]{});
            setParameters(ps, ready_fields, fieldNames);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Append a new record to the PostgreSQL database table and return the generated ID.
     * <p>
     * If a field's "generate" property is set to true, its value will be used as
     * the returned ID.
     * </p>
     *
     * @param ready_fields The fields ready for insertion.
     * @param table        The table name.
     * @return The generated ID if the operation is successful, null otherwise.
     * @throws ApplicationException if there is an error appending the record.
     */
    @Override
    public Object appendAndGetId(Field ready_fields, String table) throws ApplicationException {
        List<String> fieldNames = new ArrayList<>();
        StringBuilder expressions = new StringBuilder();
        StringBuilder values = new StringBuilder();
        buildInsertFragments(ready_fields, fieldNames, expressions, values);

        Object Id = null;
        String SQL = "INSERT INTO " + table + " (" + expressions + ") VALUES(" + values + ")";
        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement ps = operator.createPreparedStatement(SQL, false, true);
            setParameters(ps, ready_fields, fieldNames);

            for (String fieldName : fieldNames) {
                FieldInfo currentField = ready_fields.get(fieldName);
                if (Id == null && currentField.get("generate") != null
                        && Boolean.parseBoolean(currentField.get("generate").toString())) {
                    Id = currentField.value();
                }
            }

            if (Id == null) {
                return operator.executeUpdateAndGetGeneratedId(ps);
            } else {
                operator.executeUpdate(ps);
                return Id;
            }
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Builds the column-name and placeholder fragments shared by {@code append}
     * and {@code appendAndGetId}, skipping auto-increment fields.
     *
     * @param ready_fields The fields to inspect.
     * @param fieldNames   Populated with the property keys of non-auto-increment fields, in order.
     * @param expressions  Populated with quoted column names, comma-separated.
     * @param values       Populated with '?' placeholders, comma-separated.
     */
    private void buildInsertFragments(Field ready_fields,
                                      List<String> fieldNames,
                                      StringBuilder expressions,
                                      StringBuilder values) {
        String dot = ",";
        for (Enumeration<String> _field = ready_fields.keys(); _field.hasMoreElements(); ) {
            String currentProperty = _field.nextElement();
            FieldInfo currentField = ready_fields.get(currentProperty);
            if (currentField.autoIncrement()) {
                continue;
            }

            fieldNames.add(currentProperty);

            if (expressions.length() == 0)
                expressions.append("\"").append(currentField.getColumnName()).append("\"");
            else
                expressions.append(dot).append("\"").append(currentField.getColumnName()).append("\"");

            if (values.length() == 0)
                values.append('?');
            else
                values.append(dot).append('?');
        }
    }

    /**
     * Update all non-Id fields of an existing record in the PostgreSQL database table.
     *
     * @param ready_fields The fields ready for update.
     * @param table        The table name.
     * @return True if the record is successfully updated, otherwise false.
     * @throws ApplicationException if there is an error updating the record.
     */
    @Override
    public boolean update(Field ready_fields, String table) throws ApplicationException {
        String dot = ",";
        StringBuilder expressions = new StringBuilder();
        FieldInfo currentField;

        Object Id = null;
        List<String> fieldNames = new ArrayList<>();
        for (Enumeration<String> _field = ready_fields.keys(); _field.hasMoreElements();) {
            String currentProperty = _field.nextElement();
            currentField = ready_fields.get(currentProperty);

            if ("Id".equalsIgnoreCase(currentField.getName())) {
                Id = currentField.value();
                continue;
            }

            if (currentField.value() != null) {
                fieldNames.add(currentProperty);
                if (expressions.length() == 0)
                    expressions.append("\"").append(currentField.getColumnName()).append("\"").append("=?");
                else
                    expressions.append(dot).append("\"").append(currentField.getColumnName()).append("\"").append("=?");
            }
        }

        String SQL = "UPDATE " + table + " SET " + expressions + " WHERE \"id\"=?";
        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement ps = operator.preparedStatement(SQL, new Object[] {});
            setParameters(ps, ready_fields, fieldNames);
            ps.setObject(fieldNames.size() + 1, Id);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ApplicationException("Error updating record: " + e.getMessage(), e);
        }
    }

    /**
     * Get the type of this repository.
     *
     * @return {@link Type#PostgreSQL}
     */
    @Override
    public Type getType() {
        return Type.PostgreSQL;
    }

    /**
     * Retrieve records from the PostgreSQL database table based on the provided SQL query.
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
            String[] fieldName = new String[cols];
            String[] fieldType = new String[cols];

            for (int i = 0; i < cols; i++) {
                fieldName[i] = resultSet.getMetaData().getColumnName(i + 1);
                fieldType[i] = resultSet.getMetaData().getColumnTypeName(i + 1);
            }

            while (resultSet.next()) {
                row = new Row();
                fields = new Field();
                for (int i = 0; i < fieldName.length; i++) {
                    // Cache getObject result to avoid a second driver call for the typed getter
                    Object raw = resultSet.getObject(i + 1);
                    Object v_field;
                    if (raw == null) {
                        v_field = null;
                    } else {
                        String type = fieldType[i].toUpperCase();

                        if (type.contains("INT") || type.equals("SERIAL") || type.equals("BIGSERIAL")) {
                            try {
                                if (type.equals("BIGINT") || type.equals("INT8") || type.equals("BIGSERIAL")) {
                                    v_field = resultSet.getLong(i + 1);
                                } else {
                                    v_field = resultSet.getInt(i + 1);
                                }
                            } catch (SQLException e) {
                                v_field = resultSet.getLong(i + 1);
                            }
                        } else if (type.equals("REAL") ||
                                type.contains("FLOAT") ||
                                type.contains("DOUBLE") ||
                                type.contains("NUMERIC") ||
                                type.contains("DECIMAL")) {
                            try {
                                v_field = resultSet.getDouble(i + 1);
                            } catch (SQLException e) {
                                v_field = resultSet.getFloat(i + 1);
                            }
                        } else if (type.contains("BOOL") || type.equals("BIT")) {
                            // Use the already-fetched raw object to avoid getBoolean returning
                            // false for NULL; raw is non-null here so the cast is safe
                            v_field = (Boolean) raw;
                        } else if (type.contains("DATE") || type.contains("TIME") || type.contains("TIMESTAMP")) {
                            try {
                                v_field = resultSet.getTimestamp(i + 1);
                            } catch (SQLException e) {
                                v_field = resultSet.getString(i + 1);
                            }
                        } else if (type.equals("BLOB") || type.contains("BINARY") || type.equals("BYTEA")) {
                            try {
                                v_field = resultSet.getBytes(i + 1);
                            } catch (SQLException e) {
                                v_field = raw;
                            }
                        } else {
                            v_field = resultSet.getString(i + 1);
                        }
                    }

                    field = new FieldInfo();
                    field.append("name", fieldName[i]);
                    field.append("value", v_field);
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