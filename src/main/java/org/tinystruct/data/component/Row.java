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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Row extends CopyOnWriteArrayList<Field> {
    private final static long serialVersionUID = 1;

    public Row() {
    }

    public void append(Field field) {
        this.add(field);
    }

    public FieldInfo getFieldInfo(String fieldName) {
        if (this.size()>0) {
            return this.get(0).get(fieldName);
        }
        return null; // or throw an exception if index is out of bounds
    }

    @Override
    public String toString() {
        StringBuilder to = new StringBuilder();
        to.append("{");

        Iterator<Field> iterator = this.iterator();
        while (iterator.hasNext()) {
            Field field = iterator.next();
            to.append(field.toString());
            if (iterator.hasNext()) {
                to.append(",");
            }
        }

        to.append("}");
        return to.toString();
    }
}
