package org.tinystruct.data.repository;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.DatabaseOperator;
import org.tinystruct.data.Repository;
import org.tinystruct.data.component.Field;
import org.tinystruct.data.component.FieldInfo;
import org.tinystruct.data.component.FieldType;
import org.tinystruct.data.component.Row;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

public abstract class AbstractDataRepository implements Repository {

    /**
     * Append a new record to the database and return the generated ID.
     * This default implementation should be overridden by database-specific implementations.
     *
     * @param ready_fields the fields ready for insertion.
     * @param table        the table to append the record to.
     * @return the generated ID if the operation is successful, null otherwise.
     * @throws ApplicationException if an application-specific error occurs.
     */
    @Override
    public Object appendAndGetId(Field ready_fields, String table) throws ApplicationException {
        throw new ApplicationException("appendAndGetId not implemented for this database type");
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
        String SQL = "DELETE FROM " + table + " WHERE id=?";

        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement ps = operator.preparedStatement(SQL, new Object[]{});
            ps.setObject(1, Id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    @Override
    public Row findOne(String SQL, Object[] parameters) throws ApplicationException {
        Row row = new Row();
        FieldInfo fieldInfo;
        Field field = new Field();

        try (DatabaseOperator operator = new DatabaseOperator()) {
            PreparedStatement preparedStatement = operator.preparedStatement(SQL, parameters);
            ResultSet resultSet = operator.executeQuery(preparedStatement);
            int cols = resultSet.getMetaData().getColumnCount();
            String[] fieldName = new String[cols];
            Object[] fieldValue = new Object[cols];

            for (int i = 0; i < cols; i++) {
                fieldName[i] = resultSet.getMetaData()
                        .getColumnName(i + 1);
            }

            Object v_field;
            if (resultSet.next()) {
                for (int i = 0; i < fieldName.length; i++) {
                    v_field = resultSet.getObject(i + 1);

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
        }

        return row;
    }

    @Override
    public boolean update(Field ready_fields, String table, Set<String> onlyFields) throws ApplicationException {
        if (onlyFields == null || onlyFields.isEmpty()) {
            return this.update(ready_fields, table);
        }

        Field filteredFields = new Field();
        Enumeration<String> keys = ready_fields.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            FieldInfo fieldInfo = ready_fields.get(key);
            if ("Id".equalsIgnoreCase(fieldInfo.getName()) || onlyFields.contains(fieldInfo.getName()) || onlyFields.contains(fieldInfo.getColumnName())) {
                filteredFields.append(key, fieldInfo);
            }
        }

        return this.update(filteredFields, table);
    }

    protected void setParameters(PreparedStatement ps, Field ready_fields, List<String> fieldNames) throws SQLException {
        int i = 1;
        for (String fieldName : fieldNames) {
            FieldInfo currentField = ready_fields.get(fieldName);
            if ("int".equalsIgnoreCase(currentField.getType().getRealType())) {
                ps.setInt(i++, currentField.intValue());
            } else if ("long".equalsIgnoreCase(currentField.getType().getRealType())) {
                ps.setLong(i++, currentField.longValue());
            } else if (currentField.getType() == FieldType.TEXT || currentField.getType() == FieldType.LONGTEXT) {
                ps.setString(i++, currentField.stringValue());
            } else if (currentField.getType() == FieldType.DATE || currentField.getType() == FieldType.DATETIME) {
                ps.setDate(i++, new java.sql.Date(currentField.dateValue().getTime()));
            } else if (currentField.getType() == FieldType.BIT || currentField.getType() == FieldType.BOOLEAN) {
                ps.setBoolean(i++, currentField.booleanValue());
            } else {
                ps.setObject(i++, currentField.value());
            }
        }
    }

    protected void setParameters(PreparedStatement ps, FieldInfo[] fields) throws SQLException {
        int i = 1;
        for (FieldInfo fieldInfo : fields) {
            if (fieldInfo != null) {
                Object value = fieldInfo.value();
                if ("int".equalsIgnoreCase(fieldInfo.getType().getRealType())) {
                    ps.setInt(i++, fieldInfo.intValue());
                } else if ("long".equalsIgnoreCase(fieldInfo.getType().getRealType())) {
                    ps.setLong(i++, fieldInfo.longValue());
                } else if (fieldInfo.getType() == FieldType.TEXT || fieldInfo.getType() == FieldType.LONGTEXT) {
                    ps.setString(i++, fieldInfo.stringValue());
                } else if (fieldInfo.getType() == FieldType.DATE || fieldInfo.getType() == FieldType.DATETIME) {
                    ps.setDate(i++, new java.sql.Date(fieldInfo.dateValue().getTime()));
                } else if (fieldInfo.getType() == FieldType.BIT || fieldInfo.getType() == FieldType.BOOLEAN) {
                    ps.setBoolean(i++, fieldInfo.booleanValue());
                } else {
                    ps.setObject(i++, value);
                }
            }
        }
    }
}
