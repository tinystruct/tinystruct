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
package org.tinystruct.data.component;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.DatabaseOperator;
import org.tinystruct.data.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class MySQLServer implements Repository {

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
        String SQL = generateInsertSQL(ready_fields, table);

        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement ps = operator.preparedStatement(SQL, new Object[]{});
            setParameters(ps, ready_fields);

            return ps.executeUpdate() > 0;
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
        String SQL = generateUpdateSQL(ready_fields, table);

        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement ps = operator.preparedStatement(SQL, new Object[]{});
            setParameters(ps, ready_fields);
            ps.setObject(ready_fields.size() + 1, ready_fields.get("Id").value()); // Set Id parameter

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ApplicationException("Error updating record: " + e.getMessage(), e);
        }
    }

    /**
     * Delete records from the MySQL database table.
     *
     * @param Id    The identifier of the record to be deleted.
     * @param table The table name.
     * @return True if the record is successfully deleted, otherwise false.
     * @throws ApplicationException if there is an error deleting the record.
     */
    @Override
    public boolean delete(Object Id, String table) throws ApplicationException {
        boolean deleted = false;
        String SQL = "DELETE FROM " + table + " WHERE id=?";

        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement ps = operator.preparedStatement(SQL, new Object[]{});
            ps.setObject(1, Id);
            if (ps.execute())
                deleted = true;
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }

        return deleted;
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
            int cols = operator.executeQuery(preparedStatement).getMetaData().getColumnCount();
            String fieldName;
            Object fieldValue;
            while (operator.getResultSet().next()) {
                row = new Row();
                fields = new Field();
                for (int i = 0; i < cols; i++) {
                    fieldValue = operator.getResultSet().getObject(i + 1);
                    fieldName = operator.getResultSet().getMetaData().getColumnName(i + 1);

                    field = new FieldInfo();
                    field.append("name", fieldName);
                    field.append("value", fieldValue);
                    field.append("type", field.typeOf(fieldValue).getTypeName());

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
            int cols = operator.executeQuery(preparedStatement).getMetaData().getColumnCount();
            String fieldName;
            Object fieldValue;
            if (operator.getResultSet().next()) {
                for (int i = 0; i < cols; i++) {
                    fieldValue = operator.getResultSet().getObject(i + 1);
                    fieldName = operator.getResultSet().getMetaData().getColumnName(i + 1);

                    fieldInfo = new FieldInfo();
                    fieldInfo.append("name", fieldName);
                    fieldInfo.append("value", fieldValue);
                    fieldInfo.append("type", fieldInfo.typeOf(fieldValue));

                    field.append(fieldInfo.getName(), fieldInfo);
                }

                row.append(field);
            }
        } catch (Exception e) {
            throw new ApplicationException(e.getMessage(), e);
        }

        return row;
    }

    private String generateInsertSQL(Field ready_fields, String table) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();

        for (String fieldName : ready_fields.keySet()) {
            if (!ready_fields.get(fieldName).autoIncrement()) {
                if (columns.length() > 0) {
                    columns.append(", ");
                    values.append(", ");
                }
                columns.append("`").append(ready_fields.get(fieldName).getColumnName()).append("`");
                values.append("?");
            }
        }

        return "INSERT INTO " + table + " (" + columns + ") VALUES (" + values + ")";
    }

    private String generateUpdateSQL(Field ready_fields, String table) {
        StringBuilder expressions = new StringBuilder();

        for (String fieldName : ready_fields.keySet()) {
            if (!fieldName.equalsIgnoreCase("Id")) {
                if (expressions.length() > 0) {
                    expressions.append(", ");
                }
                expressions.append("`").append(ready_fields.get(fieldName).getColumnName()).append("`").append("=?");
            }
        }

        return "UPDATE " + table + " SET " + expressions + " WHERE id=?";
    }

    private void setParameters(PreparedStatement ps, Field ready_fields) throws SQLException {
        int i = 1;
        for (FieldInfo fieldInfo : ready_fields.values()) {
            if (!fieldInfo.autoIncrement()) {
                Object value = fieldInfo.value();
                if ("int".equalsIgnoreCase(fieldInfo.getType().getRealType())) {
                    ps.setInt(i++, fieldInfo.intValue());
                } else if (fieldInfo.getType() == FieldType.TEXT) {
                    ps.setString(i++, fieldInfo.stringValue());
                } else if (fieldInfo.getType() == FieldType.DATE || fieldInfo.getType() == FieldType.DATETIME) {
                    ps.setTimestamp(i++, new Timestamp(fieldInfo.dateValue().getTime()));
                } else if (fieldInfo.getType() == FieldType.BIT) {
                    ps.setBoolean(i++, fieldInfo.booleanValue());
                } else {
                    ps.setObject(i++, value);
                }
            }
        }
    }
}
