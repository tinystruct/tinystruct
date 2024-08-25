package org.tinystruct.data.component;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.DatabaseOperator;
import org.tinystruct.data.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class AbstractDataRepository implements Repository {

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
}
