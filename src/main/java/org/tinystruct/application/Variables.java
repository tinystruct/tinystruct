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
package org.tinystruct.application;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.system.template.variable.DataType;
import org.tinystruct.system.template.variable.StringVariable;
import org.tinystruct.system.template.variable.Variable;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Variables {
    private final static String PREFIX_VARIABLE_NAME = "{%";
    private final static String SUFFIX_VARIABLE_NAME = "%}";
    protected final ConcurrentHashMap<String, Variable<?>> variableMap = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String, Variables> group = new ConcurrentHashMap<>();

    private Variables() {
    }

    public static Variables getInstance(String group) {
        if (!Variables.group.containsKey(group)) {
            Variables.group.put(group, new Variables());
        }
        return Variables.group.get(group); // Return the singleton instance
    }

    public void setVariable(Variable<?> variable, boolean force) {
        String variableName = PREFIX_VARIABLE_NAME + variable.getName() + SUFFIX_VARIABLE_NAME;

        saveVariable(variableName, variable, force);
    }

    protected void saveVariable(String variableName, Variable<?> variable, boolean force) {

        if (force || !variableMap.containsKey(variableName)) {
            if (variable.getType() == DataType.OBJECT) {
                Builder builder = new Builder();
                try {
                    builder.parse(variable.getValue().toString());
                    Set<Map.Entry<String, Object>> elements = builder.entrySet();
                    Iterator<Map.Entry<String, Object>> list = elements.iterator();
                    Map.Entry<String, Object> entry;
                    while (list.hasNext()) {
                        entry = list.next();
                        variableMap.put(PREFIX_VARIABLE_NAME + variable.getName() + "."
                                + entry.getKey() + SUFFIX_VARIABLE_NAME, new StringVariable(PREFIX_VARIABLE_NAME
                                + variable.getName() + "." + entry.getKey() + SUFFIX_VARIABLE_NAME,
                                entry.getValue().toString()));
                    }
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }
            }
            variableMap.put(variableName, variable);
        }
    }

    public Variable<?> getVariable(String variable) {
        return variableMap.get(PREFIX_VARIABLE_NAME + variable + SUFFIX_VARIABLE_NAME);
    }

    public Map<String, Variable<?>> getVariables() {
        return variableMap;
    }

}
