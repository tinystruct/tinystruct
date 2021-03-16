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
    private String SQL;
    private String table;
    private String fields;
    private String orders;

    public Condition() {
        this.SQL = "";
        this.fields = "*";
        this.orders = "";
    }

    public Condition select(String table) {
        this.table = table;
        this.SQL = "SELECT " + this.fields + " FROM " + this.table + " "
                + this.orders;
        return this;
    }

    public Condition and(String condition) {
        condition = condition.toLowerCase();

        if (this.SQL.toUpperCase().contains("WHERE"))
            this.SQL += " AND " + condition;
        else
            this.SQL += " WHERE " + condition;

        return this;
    }

    public Condition or(String condition) {
        condition = condition.toLowerCase();

        if (this.SQL.toUpperCase().contains("WHERE"))
            this.SQL += " OR " + condition.toLowerCase();
        else
            this.SQL += " WHERE " + condition.toLowerCase();
        return this;
    }

    public Condition orderBy(String orders) {
        orders = orders.toLowerCase();
        this.orders = " order by " + orders;
        if (this.SQL.trim().length() > 0)
            this.SQL += this.orders;
        return this;
    }

    public Condition with(String sql) {

        this.SQL = "SELECT " + this.fields + " FROM " + this.table + " " + sql
                + this.orders;
        return this;
    }

    public Condition setRequestFields(String fields) {

        this.fields = fields;
        return this;
    }

    public String toString() {
        return this.SQL;
    }

}
