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
package org.tinystruct.data.component;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.DatabaseOperator;
import org.tinystruct.data.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MySQLServer implements Repository {

    public MySQLServer() {
    }

    public boolean append(Field ready_fields, String table) throws ApplicationException {
        boolean inserted = false;
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
                expressions.append("`").append(currentField.getColumnName()).append("`");
            else
                expressions.append(dot).append("`").append(currentField.getColumnName()).append("`");

            if (values.length() == 0)
                values.append('?');
            else
                values.append(dot).append('?');
        }

        String SQL = "INSERT INTO " + table + " (" + expressions + ") VALUES(" + values + ")";

        DatabaseOperator operator = new DatabaseOperator();
        PreparedStatement ps = operator.preparedStatement(SQL, new Object[]{});
        Iterator<String> iterator = fieldNames.iterator();
        try {
            int i = 1;
            while (iterator.hasNext()) {
                currentField = ready_fields.get(iterator.next());

                if (currentField.autoIncrement()) {
                    continue;
                }

                if (currentField.getType().getRealType().equalsIgnoreCase("int")) {
                    ps.setInt(i++, currentField.intValue());
                } else if (currentField.getType() == FieldType.TEXT) {
                    ps.setString(i++, currentField.stringValue());
                } else if (currentField.getType() == FieldType.DATE || currentField.getType() == FieldType.DATETIME) {
                    ps.setTimestamp(i++, new Timestamp(currentField.dateValue().getTime()));
                } else if (currentField.getType() == FieldType.BIT) {
                    ps.setBoolean(i++, currentField.booleanValue());
                } else {
                    ps.setObject(i++, currentField.value());
                }
            }

            if (operator.update() > 0) {
                inserted = true;
            }
        } catch (SQLException e) {

            throw new ApplicationException(e.getMessage(), e);
        } finally {
            operator.close();
        }

        return inserted;
    }

    public boolean update(Field ready_fields, String table) throws ApplicationException {
        String dot = ",", currentProperty;
        StringBuilder expressions = new StringBuilder();
        FieldInfo currentField;

        DatabaseOperator operator = new DatabaseOperator();

        Object Id = null;
        boolean edited = false;
        List<String> fieldNames = new ArrayList<String>();
        for (Enumeration<String> _field = ready_fields.keys(); _field.hasMoreElements(); ) {
            currentProperty = _field.nextElement();
            currentField = ready_fields.get(currentProperty);
            if ("Id".equalsIgnoreCase(currentField.getName())) {
                Id = currentField.value();

                continue;
            }

            if (currentField.value() != null) {
                fieldNames.add(currentProperty);

                if (expressions.length() == 0)
                    expressions.append("`").append(currentField.getColumnName()).append("`").append("=?");
                else
                    expressions.append(dot).append("`").append(currentField.getColumnName()).append("`").append("=?");
            }
        }

        String SQL = "UPDATE " + table + " SET " + expressions + " WHERE id=?";
        PreparedStatement ps = operator.preparedStatement(SQL, new Object[]{});
        Iterator<String> iterator = fieldNames.iterator();
        try {
            int i = 1;
            while (iterator.hasNext()) {
                currentField = ready_fields.get(iterator.next());

                // System.out.println("["+currentField.getType()+"]"+i+":"+currentField.value());
                if ("int".equalsIgnoreCase(currentField.getType().getRealType())) {
                    ps.setInt(i++, currentField.intValue());
                } else if (currentField.getType() == FieldType.TEXT) {
                    ps.setString(i++, currentField.stringValue());
                } else if (currentField.getType() == FieldType.DATE || currentField.getType() == FieldType.DATETIME) {
                    ps.setTimestamp(i++, new Timestamp(currentField.dateValue().getTime()));
                } else if (currentField.getType() == FieldType.BIT) {
                    ps.setBoolean(i++, currentField.booleanValue());
                } else {
                    ps.setObject(i++, currentField.value());
                }

            }

            // System.out.println(i+":"+Id);
            ps.setObject(i, Id);
            if (operator.update() > 0) {
                edited = true;
            }
        } catch (SQLException e) {

            throw new ApplicationException(e.getMessage(), e);
        } finally {
            operator.close();
        }

        return edited;
    }

    public boolean delete(Object Id, String table) throws ApplicationException {
        boolean deleted = false;
        String SQL = "DELETE FROM " + table + " WHERE id=?";

        DatabaseOperator operator = new DatabaseOperator();
        PreparedStatement ps = operator.preparedStatement(SQL, new Object[]{});
        try {
            ps.setObject(1, Id);
            if (operator.update() > 0)
                deleted = true;
        } catch (SQLException e) {

            throw new ApplicationException(e.getMessage(), e);
        } finally {
            operator.close();
        }

        return deleted;
    }

    public Type getType() {
        return Type.MySQL;
    }

    public Table find(String SQL, Object[] parameters) throws ApplicationException {

        Table table = new Table();
        Row row;
        FieldInfo field;
        Field fields;

        DatabaseOperator operator = new DatabaseOperator();
        operator.preparedStatement(SQL, parameters);

        try {
            operator.query();
            int cols = operator.getResultSet().getMetaData().getColumnCount();
            String[] fieldName = new String[cols];
            Object[] fieldValue = new Object[cols];

            for (int i = 0; i < cols; i++) {
                fieldName[i] = operator.getResultSet().getMetaData().getColumnName(i + 1);
            }

            Object v_field;
            while (operator.getResultSet().next()) {
                row = new Row();
                fields = new Field();
                for (int i = 0; i < fieldName.length; i++) {
                    v_field = operator.getResultSet().getObject(i + 1);

                    fieldValue[i] = (v_field == null ? "" : v_field);
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
        } finally {
            operator.close();
        }

        return table;

    }

    public Row findOne(String SQL, Object[] parameters) throws ApplicationException {

        Row row = new Row();
        FieldInfo fieldInfo;
        Field field = new Field();

        DatabaseOperator operator = new DatabaseOperator();
        operator.preparedStatement(SQL, parameters);

        try {
            operator.query();
            int cols = operator.getResultSet().getMetaData().getColumnCount();
            String[] fieldName = new String[cols];
            Object[] fieldValue = new Object[cols];

            for (int i = 0; i < cols; i++) {
                fieldName[i] = operator.getResultSet().getMetaData().getColumnName(i + 1);
            }

            Object v_field;
            if (operator.getResultSet().next()) {
                for (int i = 0; i < fieldName.length; i++) {
                    v_field = operator.getResultSet().getObject(i + 1);

                    fieldValue[i] = (v_field == null ? "" : v_field);
                    fieldInfo = new FieldInfo();
                    fieldInfo.append("name", fieldName[i]);
                    fieldInfo.append("value", fieldValue[i]);
                    fieldInfo.append("type", fieldInfo.typeOf(v_field));

                    field.append(fieldInfo.getName(), fieldInfo);
                }

                row.append(field);
            }
        } catch (Exception e) {
            throw new ApplicationException(e.getMessage(), e);
        } finally {
            operator.close();
        }

        return row;

    }
}
