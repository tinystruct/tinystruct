/*******************************************************************************
 * Copyright  (c) 2017 James Mover Zhou
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
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLServer implements Repository {

    private final static Logger logger = Logger.getLogger("SQLServer.class");
    ;

    public SQLServer() {
    }

    public boolean append(Field ready_fields, String table)
            throws ApplicationException {
        boolean inserted = false;
        String SQL = "", KEYS = "", DATA_KEYS = "", VALUES = "", Parameters = "", dot = ",", currentProperty;
        FieldInfo currentField;

        for (Enumeration<String> _field = ready_fields.keys(); _field
                .hasMoreElements(); ) {
            currentProperty = _field.nextElement();
            currentField = ready_fields.get(currentProperty);

            if (currentField.autoIncrement()) {
                continue;
            }

            if (currentField.getType().getRealType().equalsIgnoreCase("int")
                    || currentField.getType() == FieldType.TEXT
                    || currentField.getType() == FieldType.BIT
                    || currentField.getType() == FieldType.DATE
                    || currentField.getType() == FieldType.DATETIME) {
                Parameters += "@" + currentField.getName() + " "
                        + currentField.get("type") + dot;

                if (currentField.getType() == FieldType.TEXT) {
                    VALUES += "'"
                            + currentField.stringValue().replaceAll("'", "''")
                            + "'" + dot;
                } else if (currentField.getType() == FieldType.DATE
                        || currentField.getType() == FieldType.DATETIME) {
                    SimpleDateFormat format = new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss");
                    VALUES += "'" + format.format(currentField.value()) + "'"
                            + dot;
                } else if (currentField.getType() == FieldType.BIT) {
                    // 对null未作处理，考虑后面要进行处理
                    if (currentField.value() != null)
                        VALUES += (currentField.value().toString().equals(
                                "true") ? 1 : 0)
                                + dot;
                    else
                        VALUES += "0" + dot;
                } else
                    VALUES += currentField.value() + dot;
            } else {
                Parameters += "@" + currentField.getName() + " "
                        + currentField.get("type") + "("
                        + currentField.getLength() + ")" + dot;
                VALUES += "'"
                        + currentField.stringValue().replaceAll("'", "''")
                        + "'" + dot;
            }

            DATA_KEYS += currentField.getColumnName() + dot;
            KEYS += "@" + currentField.getName() + dot;
        }

        DATA_KEYS = DATA_KEYS.substring(0, DATA_KEYS.length() - 1);
        KEYS = KEYS.substring(0, KEYS.length() - 1);
        VALUES = VALUES.substring(0, VALUES.length() - 1);
        Parameters = Parameters.substring(0, Parameters.length() - 1);

        table = table.replaceAll("\\[", "").replaceAll("\\]", "");
        SQL = "if not exists (select * from dbo.sysobjects where id = object_id(N'[dbo].["
                + table
                + "_APPEND]') and OBJECTPROPERTY(id, N'IsProcedure') = 1)";
        SQL += "BEGIN exec('CREATE PROCEDURE [dbo].[" + table + "_APPEND] "
                + Parameters + " AS INSERT INTO [" + table + "](" + DATA_KEYS
                + ") VALUES(" + KEYS + ")')";
        SQL += " {call " + table + "_APPEND(" + VALUES + ")} END";
        SQL += " else {call " + table + "_APPEND(" + VALUES + ")}";

        DatabaseOperator operator = new DatabaseOperator();
        operator.createStatement(false);
        logger.log(Level.INFO, SQL);
        if (operator.update(SQL) > 0) {
            inserted = true;
        }

        operator.close();
        return inserted;
    }

    public boolean delete(Object Id, String table) throws ApplicationException {
        boolean deleted = false;
        String SQL = "DELETE FROM [" + table + "] WHERE id=?";

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

    public boolean update(Field ready_fields, String table)
            throws ApplicationException {
        String Parameters = "", SQL = "", KEYS = "", VALUES = "", Expressions = "", dot = ",", currentProperty;
        FieldInfo currentField;

        boolean edited = false;

        for (Enumeration<String> _field = ready_fields.keys(); _field
                .hasMoreElements(); ) {
            currentProperty = _field.nextElement();
            currentField = ready_fields.get(currentProperty);

            if (currentField.autoIncrement()) {
                Parameters += "@" + currentField.getName() + " "
                        + currentField.get("type") + dot;
                VALUES += currentField.value() + dot;

                continue;
            }

            if (currentField.getType().getRealType().equalsIgnoreCase("int")
                    || currentField.getType() == FieldType.TEXT
                    || currentField.getType() == FieldType.BIT
                    || currentField.getType() == FieldType.DATE
                    || currentField.getType() == FieldType.DATETIME) {
                Parameters += "@" + currentField.getName() + " "
                        + currentField.get("type") + dot;
                if (currentField.getType() == FieldType.TEXT) {
                    VALUES += "'"
                            + currentField.stringValue().replaceAll("'", "''")
                            + "'" + dot;
                } else if (currentField.getType() == FieldType.DATE
                        || currentField.getType() == FieldType.DATETIME) {
                    SimpleDateFormat format = new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss");
                    VALUES += "'" + format.format(currentField.value()) + "'"
                            + dot;
                } else if (currentField.getType() == FieldType.BIT)
                    VALUES += (currentField.value().toString().equals("true") ? 1
                            : 0)
                            + dot;
                else
                    VALUES += currentField.value() + dot;
            } else {
                Parameters += "@" + currentField.getName() + " "
                        + currentField.get("type") + "("
                        + currentField.getLength() + ")" + dot;
                VALUES += "'"
                        + currentField.stringValue().replaceAll("'", "''")
                        + "'" + dot;
            }

            KEYS = "@" + currentField.getName();
            Expressions += "[" + currentField.getColumnName() + "]=" + KEYS
                    + dot;
        }

        VALUES = VALUES.substring(0, VALUES.length() - 1);
        Expressions = Expressions.substring(0, Expressions.length() - 1);
        Parameters = Parameters.substring(0, Parameters.length() - 1);

        table = table.replaceAll("\\[", "").replaceAll("\\]", "");
        SQL = "if not exists (select * from dbo.sysobjects where id = object_id(N'[dbo].["
                + table
                + "_EDIT]') and OBJECTPROPERTY(id, N'IsProcedure') = 1)";
        SQL += "BEGIN exec('CREATE PROCEDURE [dbo].[" + table + "_EDIT] "
                + Parameters + " AS UPDATE [" + table + "] SET " + Expressions
                + " WHERE id=@Id')";
        SQL += " {call " + table + "_EDIT(" + VALUES + ")} END";
        SQL += " else {call " + table + "_EDIT(" + VALUES + ")}";

        DatabaseOperator operator = new DatabaseOperator();
        operator.createStatement(false);

        logger.log(Level.INFO, SQL);
        if (operator.update(SQL) > 0) {
            edited = true;
        }
        operator.close();

        return edited;
    }

    public Type getType() {

        return Type.SQLServer;
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
                fieldName[i] = operator.getResultSet().getMetaData()
                        .getColumnName(i + 1);
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
                fieldName[i] = operator.getResultSet().getMetaData()
                        .getColumnName(i + 1);
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
