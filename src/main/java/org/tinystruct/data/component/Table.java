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

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class Table extends CopyOnWriteArrayList<Row> {
    private final static long serialVersionUID = 0;
    private String name;

    public Table() {
    }

    public Table(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void append(Row row) {
        this.add(row);
    }

    @Override
	public String toString() {
        StringBuilder to = new StringBuilder();

        Iterator<Row> iterator = this.iterator();
        Row row;
        while (iterator.hasNext()) {
            row = iterator.next();

            if (to.length() == 0)
                to.append("[").append(row.toString());
            else
                to.append(",").append(row.toString());
        }

        if (this.size() > 0)
            to.append("]\r\n");

        return to.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof Table))
            return false;
        Table other = (Table) obj;
        if (name == null) {
            return other.name == null;
        } else return name.equals(other.name);
    }
}
