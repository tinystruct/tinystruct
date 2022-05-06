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
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.Data;
import org.tinystruct.data.Mapping;
import org.tinystruct.data.Repository;
import org.tinystruct.data.Repository.Type;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;
import org.tinystruct.system.util.ClassInfo;

import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author James
 */
public abstract class AbstractData implements Data {

    private final static Logger logger = Logger.getLogger(AbstractData.class.getName());
    ;
    private String classPath;
    private String className;
    protected Object Id;
    private String table;
    private Field readyFields;
    private Condition condition;
    private static Repository repository;

    static {
        try {
            repository = getDefaultServer();
        } catch (ApplicationException e) {
            logger.severe(e.getMessage());
        }
    }

    protected AbstractData() {
        this.className = this.getClass().getSimpleName();

        try {
            this.classPath = new ClassInfo(this).getClassPath();
        } catch (ApplicationException e) {
            logger.severe(e.getMessage());
        }

        this.readyFields = new Field();
        try {
            this.readyFields = new Mapping().getMappedField(this);
        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private static Repository getDefaultServer() throws ApplicationException {
        Configuration<String> properties = new Settings("/application.properties");
        String driver = properties.get("driver");

        if (driver.trim().length() == 0)
            throw new ApplicationRuntimeException("Database Connection Driver has not been set in application.properties!");

        Repository repository;
        int index = -1, length = Type.values().length;
        for (int i = 0; i < length; i++) {
            if (driver.indexOf(Type.values()[i].name().toLowerCase()) != -1) {
                index = i;
                break;
            }
        }

        switch (index) {
            case 1:
                repository = new SQLServer();
                break;
            case 2:
                repository = new SQLiteServer();
                break;
            case 3:
                repository = new H2Server();
                break;
            default:
                repository = new MySQLServer();
                break;
        }

        return repository;
    }

    public String getClassPath() {
        return this.classPath;
    }

    public Object setId(Object id) {
        this.Id = id;

        if (this.readyFields.containsKey("Id")) {
            this.readyFields.get("Id").set("value", this.Id);
        }

        return this.Id;
    }

    protected Object setField(String fieldName, Object fieldValue) {
        if (this.readyFields.containsKey(fieldName)) {
            this.readyFields.get(fieldName).set("value", fieldValue);

            return fieldValue;
        }

        return null;
    }

    protected Timestamp setFieldAsTimestamp(String fieldName, Timestamp fieldValue) {
        Object t = this.setField(fieldName, fieldValue);
        if (t != null) {
            return fieldValue;
        }
        return Timestamp.valueOf("2009-03-20");
    }

    protected Date setFieldAsDate(String fieldName, Date fieldValue) {
        Object t = this.setField(fieldName, fieldValue);
        if (t != null) {
            return fieldValue;
        }
        return new Date();
    }

    protected int setFieldAsInt(String fieldName, int fieldValue) {
        Object t = this.setField(fieldName, fieldValue);
        if (t != null) {
            return Integer.parseInt(t.toString());
        }

        return -1;
    }

    protected String setFieldAsString(String fieldName, String fieldValue) {
        Object t = this.setField(fieldName, fieldValue);
        if (t != null) {
            return t.toString();
        }

        return null;
    }

    protected boolean setFieldAsBoolean(String fieldName, boolean fieldValue) {
        Object t = this.setField(fieldName, fieldValue);
        if (t != null) {
            return Boolean.parseBoolean(t.toString());
        }

        return false;
    }

    public boolean append() throws ApplicationException {
        return repository.append(this.readyFields, this.table);
    }

    public boolean update() throws ApplicationException {
        return repository.update(readyFields, this.table);
    }

    public boolean delete() throws ApplicationException {
        return repository.delete(this.Id, this.table);
    }

    protected int countAll(String SQL) {
        return 0;
    }

    public Data setRequestFields(String fields) {
        this.condition = new Condition();
        this.condition.setRequestFields(fields);

        return this;
    }

    public Data orderBy(String[] fieldNames) {
        StringBuilder orders = new StringBuilder();
        for (String fields : fieldNames) {
            if (orders.length() > 0)
                orders.append("," + fields);
            else
                orders.append(fields);
        }

        if (this.condition == null)
            this.condition = new Condition();
        this.condition.orderBy(orders.toString());

        return this;
    }

    public Table find(String SQL, Object[] parameters) throws ApplicationException {
        return repository.find(SQL, parameters);
    }

    public Table find(Condition condition, Object[] parameters) throws ApplicationException {
        return this.find(condition.toString(), parameters);
    }

    public Table findWith(String where, Object[] parameters) throws ApplicationException {
        if (this.condition == null) {
            return this.find(new Condition().select(this.table).with(where), parameters);
        } else {
            return this.find(this.condition.select(this.table).with(where), parameters);
        }
    }

    public Row findOne(String SQL, Object[] parameters) throws ApplicationException {
        return repository.findOne(SQL, parameters);
    }

    public Row findOneById() throws ApplicationException {
        Row row = this.findOne(new Condition().select(this.table).and(
                "id=?").toString(), new Object[]{this.Id});

        if (row.size() > 0)
            this.setData(row);

        return row;
    }

    public Row findOneByKey(String PK, String value) throws ApplicationException {
        Row row = this.findOne(new Condition().select(this.table).and(
                PK + "=?").toString(), new Object[]{value});

        if (row.size() > 0)
            this.setData(row);

        return row;
    }

    public Table findAll() throws ApplicationException {
        if (this.condition == null) {
            return this.find(new Condition().select(this.table), new Object[]{});
        } else {
            return this.find(this.condition.select(this.table), new Object[]{});
        }
    }

    public abstract void setData(Row row);

    public abstract String toString();

    protected void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public Object getId() {
        return this.Id;
    }

    public String getTableName() {
        return this.table;
    }

    public void setTableName(String table) {
        this.table = table;
    }

    public Repository getRepository() {
        return repository;
    }
}