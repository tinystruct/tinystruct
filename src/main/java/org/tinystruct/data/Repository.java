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
import org.tinystruct.data.component.Field;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;
import org.tinystruct.data.repository.Type;

import java.util.Set;

/**
 * Interface for database repository.
 */
public interface Repository {

    /**
     * Get the type of the repository.
     *
     * @return the type of the repository.
     */
    Type getType();

    /**
     * Append a new record to the database.
     *
     * @param ready_fields the fields ready for insertion.
     * @param table        the table to append the record to.
     * @return true if the operation is successful, false otherwise.
     * @throws ApplicationException if an application-specific error occurs.
     */
    boolean append(Field ready_fields, String table) throws ApplicationException;

    /**
     * Append a new record to the database and return the generated ID.
     *
     * @param ready_fields the fields ready for insertion.
     * @param table        the table to append the record to.
     * @return the generated ID if the operation is successful, null otherwise.
     * @throws ApplicationException if an application-specific error occurs.
     */
    Object appendAndGetId(Field ready_fields, String table) throws ApplicationException;

    /**
     * Update an existing record in the database.
     *
     * @param ready_fields the fields ready for update.
     * @param table        the table to update the record in.
     * @return true if the operation is successful, false otherwise.
     * @throws ApplicationException if an application-specific error occurs.
     */
    boolean update(Field ready_fields, String table) throws ApplicationException;

    /**
     * Update only the specified fields of an existing record in the database table.
     * <p>
     * Only columns whose property name or column name appears in {@code onlyFields} will be
     * included in the {@code SET} clause, allowing a fully-populated domain object to be passed
     * in without accidentally overwriting unrelated columns.
     * </p>
     * <p>Example:</p>
     * <pre>
     *     server.update(readyFields, "users", new HashSet&lt;&gt;(Arrays.asList("name", "email")));
     * </pre>
     *
     * @param ready_fields The fields ready for update (must include the Id field).
     * @param table        The table name.
     * @param onlyFields   Whitelist of field/column names to update.
     * @return True if the record is successfully updated, otherwise false.
     * @throws ApplicationException if there is an error updating the record.
     */
    boolean update(Field ready_fields, String table, Set<String> onlyFields) throws ApplicationException;

    /**
     * Delete a record from the database.
     *
     * @param Id    the identifier of the record to delete.
     * @param table the table to delete the record from.
     * @return true if the operation is successful, false otherwise.
     * @throws ApplicationException if an application-specific error occurs.
     */
    boolean delete(Object Id, String table) throws ApplicationException;

    /**
     * Find records in the database based on the given SQL query and parameters.
     *
     * @param SQL        the SQL query.
     * @param parameters the parameters for the query.
     * @return a Table containing the query result.
     * @throws ApplicationException if an application-specific error occurs.
     */
    Table find(String SQL, Object[] parameters) throws ApplicationException;

    /**
     * Find a single record in the database based on the given SQL query and parameters.
     *
     * @param SQL        the SQL query.
     * @param parameters the parameters for the query.
     * @return a Row containing the query result.
     * @throws ApplicationException if an application-specific error occurs.
     */
    Row findOne(String SQL, Object[] parameters) throws ApplicationException;
}
