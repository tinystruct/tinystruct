package org.tinystruct.data;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Field;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;

public class Redis implements Repository {

    @Override
    public Type getType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean append(Field ready_fields, String table) throws ApplicationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean update(Field ready_fields, String table) throws ApplicationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean delete(Object Id, String table) throws ApplicationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Table find(String SQL, Object[] parameters) throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Row findOne(String SQL, Object[] parameters) throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }


}
