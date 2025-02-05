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
package org.tinystruct.data.component;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

public class Field extends ConcurrentHashMap<String, FieldInfo> {
    private final static long serialVersionUID = 1;

    public void append(String fieldName, FieldInfo fieldInfo) {
        if (fieldName != null)
            this.putIfAbsent(fieldName, fieldInfo);
    }

    public FieldInfo get(String fieldName) {
        return super.get(fieldName);
    }

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