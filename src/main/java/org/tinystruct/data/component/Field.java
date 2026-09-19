/*******************************************************************************
 * Copyright  (c) 2013, 2026 James M. ZHOU
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

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * Holds the per-row set of mapped columns for a single {@code AbstractData} instance or
 * result row. Instances are built fresh for every row/entity and never shared across
 * threads, so a plain {@link HashMap} is used instead of a concurrent map to avoid the
 * extra synchronization/memory overhead on this very hot allocation path. {@link #keys()}
 * is kept for source compatibility with the {@code Enumeration}-based iteration used
 * throughout the repository implementations.
 *
 * @author James Zhou
 */
public class Field extends HashMap<String, FieldInfo> {
    private final static long serialVersionUID = 1;

    /**
     * Appends a field mapping to this field set if the specified field name is not already present.
     *
     * @param fieldName the field name/key to associate with the field info; ignored if {@code null}
     * @param fieldInfo the {@link FieldInfo} describing the field
     */
    public void append(String fieldName, FieldInfo fieldInfo) {
        if (fieldName != null)
            this.putIfAbsent(fieldName, fieldInfo);
    }

    /**
     * Returns the {@link FieldInfo} associated with the given field name.
     *
     * @param fieldName the field name/key to look up
     * @return the {@link FieldInfo} mapped to the given name, or {@code null} if not found
     */
    public FieldInfo get(String fieldName) {
        return super.get(fieldName);
    }

    /**
     * Returns an {@link Enumeration} of the field names (keys) in this map.
     * <p>
     * Maintained for backward compatibility with callers and repository implementations
     * that rely on {@link Enumeration}-based iteration.
     * </p>
     *
     * @return an {@link Enumeration} of the keys contained in this map
     */
    public Enumeration<String> keys() {
        return Collections.enumeration(this.keySet());
    }

    /**
     * Returns a JSON-formatted string representation of all field mappings.
     *
     * @return a JSON fragment representing key-value pairs of field info
     */
    @Override
    public String toString() {
        StringBuilder to = new StringBuilder();
        FieldInfo fieldInfo;
        String key;
        Enumeration<String> f = this.keys();
        while (f.hasMoreElements()) {
            key = f.nextElement();
            fieldInfo = this.get(key);

            if (to.length() == 0)
                to.append("\"").append(key).append("\":{").append(fieldInfo.toString()).append("}");
            else
                to.append(", \"").append(key).append("\":{").append(fieldInfo.toString()).append("}");
        }
        return to.toString();
    }
}