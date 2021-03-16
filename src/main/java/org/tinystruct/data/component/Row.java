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

import java.util.Iterator;
import java.util.Vector;

public class Row extends Vector<Field> {
    private final static long serialVersionUID = 1;

    public Row() {
    }

    public void append(Field field) {
        this.add(field);
    }

    public FieldInfo getFieldInfo(String fieldName) {
        return this.get(0).get(fieldName);
    }

    public String toString() {
        StringBuilder to = new StringBuilder();

        Iterator<Field> iterator = this.iterator();
        Field fields;
        while (iterator.hasNext()) {
            fields = iterator.next();

            if (to.length() == 0) {
                to.append("{").append(fields.toString());
            } else {
                to.append(",").append(fields.toString());
            }
        }

        if (this.size() > 0)
            to.append("}");

        return to.toString();
    }
}
