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
package org.tinystruct.data.component;

import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.Data;
import org.tinystruct.data.Mapping;
import org.tinystruct.data.Repository;
import org.tinystruct.data.repository.Type;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;
import org.tinystruct.system.util.ClassInfo;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides a skeletal implementation of the Data interface.
 * It implements common methods for database operations and data manipulation.
 */
public abstract class AbstractData implements Data {

    // Logger for logging messages
    private static final Logger logger = Logger.getLogger(AbstractData.class.getName());

    // Repository for database operations, initialized once for all instances
    private static Repository repository;

    // Static block to initialize the repository during class loading
    static {
        try {
            repository = initializeRepository();
        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, "Failed to initialize repository: {0}", e.getMessage());
        }
    }

    // Identifier for the data object
    protected Object Id;

    // Class path of the data object
    private String classPath;

    // Class name of the data object
    private String className;

    // Database table associated with the data object
    private String table;

    // Fields prepared for database operations
    private Field readyFields;

    // All field names for the current object
    private final StringBuilder allFields = new StringBuilder();

    // Comma-separated field names for querying
    private String fields;

    // Order by clause for SQL queries
    private String orderBy;

    /**
     * Constructor to initialize classPath, className, and mapped fields.
     */
    public AbstractData() {
        this.className = this.getClass().getSimpleName();

        try {
            // Get the fully qualified class path
            this.classPath = new ClassInfo(this).getClassPath();
        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, "Failed to get class path: {0}", e.getMessage());
        }

        initializeFields();
    }

    /**
     * Initializes the repository based on the database configuration in application properties.
     *
     * @return The repository instance.
     * @throws ApplicationException If the repository cannot be initialized.
     */
    private static Repository initializeRepository() throws ApplicationException {
        Configuration<String> properties = new Settings("/application.properties");
        String driver = properties.get("driver");

        if (driver == null || driver.trim().isEmpty()) {
            throw new ApplicationRuntimeException("Database connection driver is not configured in application.properties.");
        }

        for (Type type : Type.values()) {
            if (driver.toLowerCase().contains(type.name().toLowerCase())) {
                return type.createRepository();
            }
        }

        throw new ApplicationException("Unsupported database driver: " + driver);
    }

    /**
     * Initialize the mapped fields for the current object and prepare field names.
     */
    private void initializeFields() {
        try {
            this.readyFields = Mapping.getMappedField(this);
            for (Map.Entry<String, FieldInfo> entry : this.readyFields.entrySet()) {
                if (allFields.length() > 0) allFields.append(",");
                allFields.append(entry.getValue().getColumnName());
            }
            this.fields = allFields.toString();
        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, "Failed to initialize fields: {0}", e.getMessage());
        }
    }

    /**
     * Get the class path of the data object.
     *
     * @return Fully qualified class path.
     */
    @Override
    public String getClassPath() {
        return this.classPath;
    }

    /**
     * Set the identifier for the data object.
     *
     * @param id Identifier value.
     * @return Updated identifier.
     */
    @Override
    public Object setId(Object id) {
        this.Id = id;

        if (this.readyFields != null && this.readyFields.containsKey("Id")) {
            this.readyFields.get("Id").set("value", this.Id);
        }

        return this.Id;
    }

    /**
     * Get the identifier of the data object.
     *
     * @return Identifier value.
     */
    @Override
    public Object getId() {
        return this.Id;
    }

    /**
     * Set field value as an Object.
     *
     * @param fieldName name of field.
     * @param fieldValue value of field.
     * @return field value
     */
    protected Object setField(String fieldName, Object fieldValue) {
        if (this.readyFields.containsKey(fieldName)) {
            this.readyFields.get(fieldName).set("value", fieldValue);

            return fieldValue;
        }

        return null;
    }

    /**
     * Set field value as Timestamp type
     *
     * @param fieldName name of field.
     * @param fieldValue value of field.
     * @return field value
     */
    protected Timestamp setFieldAsTimestamp(String fieldName, Timestamp fieldValue) {
        Object t = this.setField(fieldName, fieldValue);
        if (t != null) {
            return fieldValue;
        }
        return Timestamp.valueOf("2009-03-20");
    }

    /**
     * Set field value as Date type.
     *
     * @param fieldName name of field.
     * @param fieldValue value of field.
     * @return field value
     */
    protected Date setFieldAsDate(String fieldName, Date fieldValue) {
        Object t = this.setField(fieldName, fieldValue);
        if (t != null) {
            return fieldValue;
        }
        return new Date();
    }

    /**
     * Set field value as local date time
     *
     * @param fieldName name of field.
     * @param fieldValue value of field.
     * @return field value
     */
    protected LocalDateTime setFieldAsLocalDateTime(String fieldName, LocalDateTime fieldValue) {
        Object t = this.setField(fieldName, fieldValue);
        if (t != null) {
            return fieldValue;
        }
        return LocalDateTime.now();
    }

    /**
     * Set field value as Integer type
     *
     * @param fieldName name of field.
     * @param fieldValue value of field.
     * @return field value
     */
    protected int setFieldAsInt(String fieldName, int fieldValue) {
        Object t = this.setField(fieldName, fieldValue);
        if (t != null) {
            return Integer.parseInt(t.toString());
        }

        return -1;
    }

    /**
     * Set field value as String type.
     *
     * @param fieldName name of field.
     * @param fieldValue value of field.
     * @return field value
     */
    protected String setFieldAsString(String fieldName, String fieldValue) {
        Object t = this.setField(fieldName, fieldValue);
        if (t != null) {
            return t.toString();
        }

        return null;
    }

    /**
     * Set field value as boolean type.
     *
     * @param fieldName name of field.
     * @param fieldValue value of field.
     * @return field value
     */
    protected boolean setFieldAsBoolean(String fieldName, boolean fieldValue) {
        Object t = this.setField(fieldName, fieldValue);
        if (t != null) {
            return Boolean.parseBoolean(t.toString());
        }

        return false;
    }

    /**
     * Validate if the table name is set, and throw an exception if not.
     *
     * @throws ApplicationException If the table name is missing.
     */
    private void validateTableName() throws ApplicationException {
        if (this.table == null || this.table.trim().isEmpty()) {
            throw new ApplicationException("Table name is not set.");
        }
    }

    /**
     * Append a new record to the database.
     *
     * @return true if the operation succeeds, false otherwise.
     * @throws ApplicationException If any error occurs during the operation.
     */
    @Override
    public boolean append() throws ApplicationException {
        validateTableName();
        return repository.append(this.readyFields, this.table);
    }

    /**
     * Update an existing record in the database.
     *
     * @return true if the operation succeeds, false otherwise.
     * @throws ApplicationException If any error occurs during the operation.
     */
    @Override
    public boolean update() throws ApplicationException {
        validateTableName();
        return repository.update(this.readyFields, this.table);
    }

    /**
     * Delete a record from the database.
     *
     * @return true if the operation succeeds, false otherwise.
     * @throws ApplicationException If any error occurs during the operation.
     */
    @Override
    public boolean delete() throws ApplicationException {
        validateTableName();
        if (this.Id == null) {
            throw new ApplicationException("Cannot delete a record without an ID.");
        }
        return repository.delete(this.Id, this.table);
    }

    /**
     * Set the request fields for querying data.
     */
    @Override
    public Data setRequestFields(String fields) {
        if (fields.equalsIgnoreCase("*")) {
            this.fields = this.allFields.toString();
        } else {
            this.fields = fields;
        }
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

        this.orderBy = orders.toString();
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
        if (this.orderBy != null) condition.orderBy(this.orderBy);
        return this.find(condition.toString(), parameters);
    }

    /**
     * Find records in the database based on the given where clause and parameters.
     */
    @Override
    public Table findWith(String where, Object[] parameters) throws ApplicationException {
        Condition condition = new Condition();
        condition.setRequestFields(fields);
        return this.find(condition.select(this.table).with(where), parameters);
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
        Condition condition = new Condition();
        condition.setRequestFields(fields);
        if (this.orderBy != null) condition.orderBy(this.orderBy);
        Row row = this.findOne(condition.select(this.table).and(
                "id=?").toString(), new Object[]{this.Id});

        if (!row.isEmpty())
            this.setData(row);

        return row;
    }

    /**
     * Find a single record in the database by the given primary key and value.
     */
    @Override
    public Row findOneByKey(String PK, String value) throws ApplicationException {
        Condition condition = new Condition();
        condition.setRequestFields(fields);
        if (this.orderBy != null) condition.orderBy(this.orderBy);

        Row row = this.findOne(condition.select(this.table).and(
                PK + "=?").toString(), new Object[]{value});

        if (!row.isEmpty())
            this.setData(row);

        return row;
    }

    /**
     * Find all records in the database.
     */
    @Override
    public Table findAll() throws ApplicationException {
        Condition condition = new Condition();
        condition.setRequestFields(fields);

        return this.find(condition.select(this.table), new Object[]{});
    }

    /**
     * Get the repository associated with the data object.
     */
    @Override
    public Repository getRepository() {
        return repository;
    }

    /**
     * Abstract method to set data for the object from a database row.
     * Subclasses must implement this method.
     *
     * @param row The database row containing data.
     */
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
     * Get the table name associated with the data object.
     *
     * @return Table name.
     */
    public String getTableName() {
        return this.table;
    }

    /**
     * Set the table name for the data object.
     *
     * @param table Table name.
     */
    @Override
    public void setTableName(String table) {
        this.table = table;
    }
}
