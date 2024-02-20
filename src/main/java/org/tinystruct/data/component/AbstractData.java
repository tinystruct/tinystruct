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
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.Data;
import org.tinystruct.data.Mapping;
import org.tinystruct.data.Repository;
import org.tinystruct.data.Repository.Type;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;
import org.tinystruct.system.util.ClassInfo;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides a skeletal implementation of the Data interface.
 * It implements common methods for database operations and data manipulation.
 */
public abstract class AbstractData implements Data {

    // Logger for logging messages
    private final static Logger logger = Logger.getLogger(AbstractData.class.getName());

    // Repository for database operations
    private static Repository repository;

    static {
        try {
            repository = getDefaultServer();
        } catch (ApplicationException e) {
            logger.severe(e.getMessage());
        }
    }

    // Identifier for the data
    protected Object Id;

    // Class path of the data object
    private String classPath;

    // Class name of the data object
    private String className;

    // Database table associated with the data object
    private String table;

    // Fields ready for database operations
    private Field readyFields;

    // Condition for querying data
    private Condition condition;

    /**
     * Constructor to initialize classPath, className, and readyFields.
     */
    public AbstractData() {
        this.className = this.getClass().getSimpleName();

        try {
            this.classPath = new ClassInfo(this).getClassPath();
        } catch (ApplicationException e) {
            logger.severe(e.getMessage());
        }

        try {
            this.readyFields = Mapping.getMappedField(this); // Get mapped fields of the data object
        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    // Method to get the default server based on the database driver specified in the properties file
    private static Repository getDefaultServer() throws ApplicationException {
        Configuration<String> properties = new Settings("/application.properties");
        String driver = properties.get("driver");

        if (driver.trim().isEmpty())
            throw new ApplicationRuntimeException("Database Connection Driver has not been set in application.properties!");

        Repository repository;
        int index = -1, length = Type.values().length;
        for (int i = 0; i < length; i++) {
            if (driver.contains(Type.values()[i].name().toLowerCase())) {
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

    /**
     * Get the class path of the data object.
     */
    @Override
    public String getClassPath() {
        return this.classPath;
    }

    /**
     * Set the identifier of the data object.
     */
    @Override
    public Object setId(Object id) {
        this.Id = id;

        if (this.readyFields != null && this.readyFields.containsKey("Id")) {
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

    protected LocalDateTime setFieldAsLocalDateTime(String fieldName, LocalDateTime fieldValue) {
        Object t = this.setField(fieldName, fieldValue);
        if (t != null) {
            return fieldValue;
        }
        return LocalDateTime.now();
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

    /**
     * Append a new record to the database.
     */
    @Override
    public boolean append() throws ApplicationException {
        return repository.append(this.readyFields, this.table);
    }

    /**
     * Update an existing record in the database.
     */
    @Override
    public boolean update() throws ApplicationException {
        return repository.update(readyFields, this.table);
    }

    /**
     * Delete a record from the database.
     */
    @Override
    public boolean delete() throws ApplicationException {
        return repository.delete(this.Id, this.table);
    }

    /**
     * Set the request fields for querying data.
     */
    @Override
    public Data setRequestFields(String fields) {
        this.condition = new Condition();
        this.condition.setRequestFields(fields);

        return this;
    }

    /**
     * Set the order by clause for querying data.
     */
    @Override
    public Data orderBy(String[] fieldNames) {
        StringBuilder orders = new StringBuilder();
        for (String fields : fieldNames) {
            if (orders.length() > 0)
                orders.append(",").append(fields);
            else
                orders.append(fields);
        }

        if (this.condition == null)
            this.condition = new Condition();
        this.condition.orderBy(orders.toString());

        return this;
    }

    /**
     * Find records in the database based on the given SQL query and parameters.
     */
    @Override
    public Table find(String SQL, Object[] parameters) throws ApplicationException {
        return repository.find(SQL, parameters);
    }

    /**
     * Find records in the database based on the given condition and parameters.
     */
    @Override
    public Table find(Condition condition, Object[] parameters) throws ApplicationException {
        return this.find(condition.toString(), parameters);
    }

    /**
     * Find records in the database based on the given where clause and parameters.
     */
    @Override
    public Table findWith(String where, Object[] parameters) throws ApplicationException {
        return this.find(Objects.requireNonNullElseGet(this.condition, Condition::new).select(this.table).with(where), parameters);
    }

    /**
     * Find a single record in the database based on the given SQL query and parameters.
     */
    @Override
    public Row findOne(String SQL, Object[] parameters) throws ApplicationException {
        return repository.findOne(SQL, parameters);
    }

    /**
     * Find a single record in the database by its identifier.
     */
    @Override
    public Row findOneById() throws ApplicationException {
        Row row = this.findOne(new Condition().select(this.table).and(
                "id=?").toString(), new Object[]{this.Id});

        if (row.size() > 0)
            this.setData(row);

        return row;
    }

    /**
     * Find a single record in the database by the given primary key and value.
     */
    @Override
    public Row findOneByKey(String PK, String value) throws ApplicationException {
        Row row = this.findOne(new Condition().select(this.table).and(
                PK + "=?").toString(), new Object[]{value});

        if (row.size() > 0)
            this.setData(row);

        return row;
    }

    /**
     * Find all records in the database.
     */
    @Override
    public Table findAll() throws ApplicationException {
        return this.find(Objects.requireNonNullElseGet(this.condition, Condition::new).select(this.table), new Object[]{});
    }

    /**
     * Get the repository associated with the data object.
     */
    @Override
    public Repository getRepository() {
        return repository;
    }

    // Abstract method to be implemented by subclasses
    public abstract void setData(Row row);

    /**
     * Get the class name of the data object.
     */
    @Override
    public String getClassName() {
        return className;
    }

    /**
     * Set the class name of the data object.
     */
    protected void setClassName(String className) {
        this.className = className;
    }

    /**
     * Get the identifier of the data object.
     */
    @Override
    public Object getId() {
        return this.Id;
    }

    /**
     * Get the database table associated with the data object.
     */
    public String getTableName() {
        return this.table;
    }

    /**
     * Set the database table associated with the data object.
     */
    @Override
    public void setTableName(String table) {
        this.table = table;
    }
}