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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Variables represents a collection of variables associated with a specific group.
 */
public class Variables {
    private final static String PREFIX_VARIABLE_NAME = "{%";
    private final static String SUFFIX_VARIABLE_NAME = "%}";
    private final static ConcurrentHashMap<String, Variables> group = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, Variable<?>> variableMap = new ConcurrentHashMap<>();
    private final static Logger logger = Logger.getLogger(Variables.class.getName());

    private Variables() {
    }

    /**
     * Get an instance of Variables for the specified group.
     *
     * @param group The group for which to get the Variables instance.
     * @return The Variables instance for the specified group.
     */
    public static Variables getInstance(String group) {
        return Variables.group.computeIfAbsent(group, k -> new Variables());
    }

    /**
     * Set a variable in the collection, optionally forcing an update.
     *
     * @param variable The variable to set.
     * @param force    Whether to force the update even if the variable already exists.
     */
    public void setVariable(Variable<?> variable, boolean force) {
        String variableName = PREFIX_VARIABLE_NAME + variable.getName() + SUFFIX_VARIABLE_NAME;

        saveVariable(variableName, variable, force);
    }

    /**
     * Save a variable to the collection, handling object variables.
     *
     * @param variableName The name of the variable.
     * @param variable     The variable to save.
     * @param force        Whether to force the update even if the variable already exists.
     */
    protected void saveVariable(String variableName, Variable<?> variable, boolean force) {
        // If the variable does not exist in the map, or force is true
        if (force || !variableMap.containsKey(variableName)) {
            if (variable.getType() == DataType.OBJECT) {
                processObjectVariable(variableName, variable);
            } else {
                variableMap.put(variableName, variable);
            }
        }
    }

    /**
     * Process an object variable and add its properties to the collection.
     *
     * @param variableName The name of the variable.
     * @param variable     The object variable to process.
     */
    private void processObjectVariable(String variableName, Variable<?> variable) {
        Builder builder = new Builder();
        try {
            builder.parse(variable.getValue().toString());
            for (Map.Entry<String, Object> entry : builder.entrySet()) {
                String newVariableName = PREFIX_VARIABLE_NAME + variable.getName() + "." + entry.getKey() + SUFFIX_VARIABLE_NAME;
                String value = entry.getValue().toString();
                variableMap.put(newVariableName, new StringVariable(newVariableName, value));
            }
        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Get a variable from the collection.
     *
     * @param variable The name of the variable to get.
     * @return The variable, or null if not found.
     */
    public Variable<?> getVariable(String variable) {
        return variableMap.get(PREFIX_VARIABLE_NAME + variable + SUFFIX_VARIABLE_NAME);
    }

    /**
     * Get all variables in the collection.
     *
     * @return The map of variables.
     */
    public Map<String, Variable<?>> getVariables() {
        return variableMap;
    }

}
