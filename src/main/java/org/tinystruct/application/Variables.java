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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Variables {
    private final static String PREFIX_VARIABLE_NAME = "{%";
    private final static String SUFFIX_VARIABLE_NAME = "%}";
    private final static ConcurrentHashMap<String, Variables> group = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, Variable<?>> variableMap = new ConcurrentHashMap<>();

    private Variables() {
    }

    public static Variables getInstance(String group) {
        return Variables.group.computeIfAbsent(group, k -> new Variables());
    }

    public void setVariable(Variable<?> variable, boolean force) {
        String variableName = PREFIX_VARIABLE_NAME + variable.getName() + SUFFIX_VARIABLE_NAME;

        saveVariable(variableName, variable, force);
    }

    protected void saveVariable(String variableName, Variable<?> variable, boolean force) {
        variableMap.compute(variableName, (key, existingVariable) -> {
            if (existingVariable == null || force) {
                if (variable.getType() == DataType.OBJECT) {
                    processObjectVariable(variableName, variable);
                }
                return variable;
            }
            return existingVariable;
        });
    }

    private void processObjectVariable(String variableName, Variable<?> variable) {
        Builder builder = new Builder();
        try {
            builder.parse(variable.getValue().toString());
            for (Map.Entry<String, Object> entry : builder.entrySet()) {
                String newVariableName = PREFIX_VARIABLE_NAME + variable.getName() + "."
                        + entry.getKey() + SUFFIX_VARIABLE_NAME;
                String value = entry.getValue().toString();
                variableMap.put(newVariableName, new StringVariable(newVariableName, value));
            }
        } catch (ApplicationException e) {
            e.printStackTrace();
        }
    }

    public Variable<?> getVariable(String variable) {
        return variableMap.get(PREFIX_VARIABLE_NAME + variable + SUFFIX_VARIABLE_NAME);
    }

    public Map<String, Variable<?>> getVariables() {
        return variableMap;
    }

}
