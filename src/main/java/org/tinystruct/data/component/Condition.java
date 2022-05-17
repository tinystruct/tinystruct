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

import java.io.Serializable;

public class Condition implements Serializable {

    private static final long serialVersionUID = 7375286601225137163L;
    private StringBuilder SQL;
    private String table;
    private String fields;
    private String orders;

    public Condition() {
        this.SQL = new StringBuilder();
        this.fields = "*";
        this.orders = "";
    }

    public Condition select(String table) {
        this.table = table;
        this.SQL = new StringBuilder();
        this.SQL.append("SELECT ").append(this.fields).append(" FROM ").append(this.table).append(" ").append(this.orders);
        return this;
    }

    public Condition and(String condition) {
        String query = this.SQL.toString();
        if (query.toUpperCase().contains("WHERE"))
            this.SQL.append(" AND ");
        else
            this.SQL.append(" WHERE ");
        this.SQL.append(condition.toLowerCase());
        return this;
    }

    public Condition or(String condition) {
        String query = this.SQL.toString();
        if (query.toUpperCase().contains("WHERE"))
            this.SQL.append(" OR ");
        else
            this.SQL.append(" WHERE ");
        this.SQL.append(condition.toLowerCase());
        return this;
    }

    public Condition orderBy(String orders) {
        this.orders = " order by " + orders.toLowerCase();
        String query = this.SQL.toString();
        if (query.trim().length() > 0)
            this.SQL.append(this.orders);

        return this;
    }

    public Condition with(String sql) {
        this.SQL = new StringBuilder();
        this.SQL.append("SELECT ").append(this.fields).append(" FROM ").append(this.table).append(" ").append(sql).append(this.orders);
        return this;
    }

    public Condition setRequestFields(String fields) {
        this.fields = fields;
        return this;
    }

    public String toString() {
        return this.SQL.toString();
    }

}