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
     private final StringBuilder sqlBuilder;
     private String table;
     private String fields;
     private String orders;

     /**
      * Constructs a new Condition object with default settings.
      */
     public Condition() {
         this.sqlBuilder = new StringBuilder();
         this.fields = "*";
         this.orders = "";
     }

     /**
      * Sets the table for the SQL query.
      *
      * @param table The table name (non-null and non-empty).
      * @return The updated Condition object.
      * @throws IllegalArgumentException if the table name is null or empty.
      */
     public Condition select(String table) {
         if (table == null || table.trim().isEmpty()) {
             throw new IllegalArgumentException("Table name must not be null or empty.");
         }
         this.table = table;
         this.sqlBuilder.setLength(0); // Clear the current SQL builder
         this.sqlBuilder.append("SELECT ").append(this.fields).append(" FROM ").append(this.table).append(this.orders);
         return this;
     }

     /**
      * Adds an AND condition to the SQL query.
      *
      * @param condition The condition to add (non-null and non-empty).
      * @return The updated Condition object.
      * @throws IllegalArgumentException if the condition is null or empty.
      */
     public Condition and(String condition) {
         validateCondition(condition);
         appendCondition("AND", condition);
         return this;
     }

     /**
      * Adds an OR condition to the SQL query.
      *
      * @param condition The condition to add (non-null and non-empty).
      * @return The updated Condition object.
      * @throws IllegalArgumentException if the condition is null or empty.
      */
     public Condition or(String condition) {
         validateCondition(condition);
         appendCondition("OR", condition);
         return this;
     }

     private void appendCondition(String connector, String condition) {
         String query = this.sqlBuilder.toString();
         if (query.toUpperCase().contains("WHERE")) {
             this.sqlBuilder.append(" ").append(connector).append(" ");
         } else {
             this.sqlBuilder.append(" WHERE ");
         }
         this.sqlBuilder.append(condition.toLowerCase());
     }

     /**
      * Sets the order for the SQL query.
      *
      * @param orders The order clause (non-null and non-empty).
      * @return The updated Condition object.
      * @throws IllegalArgumentException if the order clause is null or empty.
      */
     public Condition orderBy(String orders) {
         if (orders == null || orders.trim().isEmpty()) {
             throw new IllegalArgumentException("Order clause must not be null or empty.");
         }
         this.orders = " ORDER BY " + orders.toLowerCase();
         this.sqlBuilder.append(this.orders);
         return this;
     }

     /**
      * Constructs a new SQL query using custom SQL.
      *
      * @param sql The custom SQL string (non-null and non-empty).
      * @return The updated Condition object.
      * @throws IllegalArgumentException if the SQL is null or empty.
      */
     public Condition with(String sql) {
         if (sql == null || sql.trim().isEmpty()) {
             throw new IllegalArgumentException("SQL must not be null or empty.");
         }
         this.sqlBuilder.setLength(0);
         this.sqlBuilder.append("SELECT ").append(this.fields).append(" FROM ").append(this.table).append(" ").append(sql).append(this.orders);
         return this;
     }

     /**
      * Sets the fields to be retrieved in the SQL query.
      *
      * @param fields The fields to retrieve (non-null and non-empty).
      * @return The updated Condition object.
      * @throws IllegalArgumentException if the fields string is null or empty.
      */
     public Condition setRequestFields(String fields) {
         if (fields == null || fields.trim().isEmpty()) {
             throw new IllegalArgumentException("Fields must not be null or empty.");
         }
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
         return this.sqlBuilder.toString().trim();
     }

     private void validateCondition(String condition) {
         if (condition == null || condition.trim().isEmpty()) {
             throw new IllegalArgumentException("Condition must not be null or empty.");
         }
     }
 }
