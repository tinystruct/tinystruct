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

 import java.io.Serializable;

 /**
 * Represents a SQL condition for building dynamic SQL queries.
 */
public class Condition implements Serializable {

    private static final long serialVersionUID = 7375286601225137163L;
    private StringBuilder SQL;
    private String table;
    private String fields;
    private String orders;

    /**
     * Constructs a new Condition object with default settings.
     */
    public Condition() {
        this.SQL = new StringBuilder();
        this.fields = "*";
        this.orders = "";
    }

    /**
     * Sets the table for the SQL query.
     *
     * @param table The table name.
     * @return The Condition object.
     */
    public Condition select(String table) {
        this.table = table;
        this.SQL = new StringBuilder();
        this.SQL.append("SELECT ").append(this.fields).append(" FROM ").append(this.table).append(" ").append(this.orders);
        return this;
    }

    /**
     * Adds an AND condition to the SQL query.
     *
     * @param condition The condition to add.
     * @return The Condition object.
     */
    public Condition and(String condition) {
        String query = this.SQL.toString();
        if (query.toUpperCase().contains("WHERE"))
            this.SQL.append(" AND ");
        else
            this.SQL.append(" WHERE ");
        this.SQL.append(condition.toLowerCase());
        return this;
    }

    /**
     * Adds an OR condition to the SQL query.
     *
     * @param condition The condition to add.
     * @return The Condition object.
     */
    public Condition or(String condition) {
        String query = this.SQL.toString();
        if (query.toUpperCase().contains("WHERE"))
            this.SQL.append(" OR ");
        else
            this.SQL.append(" WHERE ");
        this.SQL.append(condition.toLowerCase());
        return this;
    }

    /**
     * Sets the order for the SQL query.
     *
     * @param orders The order clause.
     * @return The Condition object.
     */
    public Condition orderBy(String orders) {
        this.orders = " order by " + orders.toLowerCase();
        String query = this.SQL.toString();
        if (query.trim().length() > 0)
            this.SQL.append(this.orders);

        return this;
    }

    /**
     * Constructs a new SQL query using custom SQL.
     *
     * @param sql The custom SQL string.
     * @return The Condition object.
     */
    public Condition with(String sql) {
        this.SQL = new StringBuilder();
        this.SQL.append("SELECT ").append(this.fields).append(" FROM ").append(this.table).append(" ").append(sql).append(this.orders);
        return this;
    }

    /**
     * Sets the fields to be retrieved in the SQL query.
     *
     * @param fields The fields to retrieve.
     * @return The Condition object.
     */
    public Condition setRequestFields(String fields) {
        this.fields = fields;
        return this;
    }

    /**
     * Converts the Condition object to a string representation of the SQL query.
     *
     * @return The SQL query string.
     */
    @Override
    public String toString() {
        return this.SQL.toString();
    }
}