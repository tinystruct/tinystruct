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
package org.tinystruct.data;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Condition;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;

/**
 * This interface defines methods for data operations.
 */
public interface Data {

    /**
     * Get the class path of the data object.
     */
    String getClassPath();

    /**
     * Get the class name of the data object.
     */
    String getClassName();

    /**
     * Set the database table associated with the data object.
     */
    void setTableName(String attribute);

    /**
     * Set the identifier of the data object.
     */
    Object setId(Object Id);

    /**
     * Get the identifier of the data object.
     */
    Object getId();

    /**
     * Append a new record to the database.
     * @return true if the operation is successful, false otherwise.
     * @throws ApplicationException if an application-specific error occurs.
     */
    boolean append() throws ApplicationException;

    /**
     * Update an existing record in the database.
     * @return true if the operation is successful, false otherwise.
     * @throws ApplicationException if an application-specific error occurs.
     */
    boolean update() throws ApplicationException;

    /**
     * Delete a record from the database.
     * @return true if the operation is successful, false otherwise.
     * @throws ApplicationException if an application-specific error occurs.
     */
    boolean delete() throws ApplicationException;

    /**
     * Set the request fields for querying data.
     */
    Data setRequestFields(String fields);

    /**
     * Set the order by clause for querying data.
     */
    Data orderBy(String[] fieldNames);

    /**
     * Find records in the database based on the given SQL query and parameters.
     * @param SQL the SQL query.
     * @param parameters the parameters for the query.
     * @return a Table containing the query result.
     * @throws ApplicationException if an application-specific error occurs.
     */
    Table find(String SQL, Object[] parameters) throws ApplicationException;

    /**
     * Find records in the database based on the given condition and parameters.
     * @param condition the condition for the query.
     * @param parameters the parameters for the query.
     * @return a Table containing the query result.
     * @throws ApplicationException if an application-specific error occurs.
     */
    Table find(Condition condition, Object[] parameters) throws ApplicationException;

    /**
     * Find records in the database based on the given where clause and parameters.
     * @param where the where clause for the query.
     * @param parameters the parameters for the query.
     * @return a Table containing the query result.
     * @throws ApplicationException if an application-specific error occurs.
     */
    Table findWith(String where, Object[] parameters) throws ApplicationException;

    /**
     * Find a single record in the database based on the given SQL query and parameters.
     * @param SQL the SQL query.
     * @param parameters the parameters for the query.
     * @return a Row containing the query result.
     * @throws ApplicationException if an application-specific error occurs.
     */
    Row findOne(String SQL, Object[] parameters) throws ApplicationException;

    /**
     * Find a single record in the database by its identifier.
     * @return a Row containing the query result.
     * @throws ApplicationException if an application-specific error occurs.
     */
    Row findOneById() throws ApplicationException;

    /**
     * Find a single record in the database by the given primary key and value.
     * @param PK the primary key.
     * @param value the value of the primary key.
     * @return a Row containing the query result.
     * @throws ApplicationException if an application-specific error occurs.
     */
    Row findOneByKey(String PK, String value) throws ApplicationException;

    /**
     * Find all records in the database.
     * @return a Table containing all records.
     * @throws ApplicationException if an application-specific error occurs.
     */
    Table findAll() throws ApplicationException;

    /**
     * Get the repository associated with the data object.
     */
    Repository getRepository();
}