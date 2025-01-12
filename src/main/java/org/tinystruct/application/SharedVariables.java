package org.tinystruct.application;

import org.tinystruct.system.template.variable.Variable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SharedVariables represents a collection of shared variables used across the application.
 */
public final class SharedVariables {
    private final static String PREFIX_SHARED_VARIABLE_NAME = "[%";
    private final static String SUFFIX_SHARED_VARIABLE_NAME = "%]";
    private final static ConcurrentHashMap<String, SharedVariables> group = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Variable<?>> variableMap = new ConcurrentHashMap<>();

    /**
     * Get the singleton instance of SharedVariables.
     *
     * @return The singleton instance.
     */
    public static SharedVariables getInstance(String group) {
        return SharedVariables.group.computeIfAbsent(group, k -> new SharedVariables());
    }

    /**
     * Get a shared variable from the collection.
     *
     * @param variable The name of the shared variable to get.
     * @return The shared variable, or null if not found.
     */
    public Variable<?> getVariable(String variable) {
        return variableMap.get(PREFIX_SHARED_VARIABLE_NAME + variable + SUFFIX_SHARED_VARIABLE_NAME);
    }

    /**
     * Set a shared variable in the collection, optionally forcing an update.
     *
     * @param variable The shared variable to set.
     * @param force    Whether to force the update even if the variable already exists.
     */
    public void setVariable(Variable<?> variable, boolean force) {
        String variableName = PREFIX_SHARED_VARIABLE_NAME + variable.getName() + SUFFIX_SHARED_VARIABLE_NAME;
        saveVariable(variableName, variable, force);
    }

    /**
     * Save a shared variable to the collection, handling force updates.
     *
     * @param variableName The name of the shared variable.
     * @param variable     The shared variable to save.
     * @param force        Whether to force the update even if the variable already exists.
     */
    private void saveVariable(String variableName, Variable<?> variable, boolean force) {
        // If the variable does not exist in the map, or force is true
        if (force || !variableMap.containsKey(variableName)) {
            variableMap.put(variableName, variable);
        } else {
            variableMap.putIfAbsent(variableName, variable);
        }
    }

    /**
     * Get all shared variables in the collection.
     *
     * @return The map of shared variables.
     */
    public Map<String, Variable<?>> getVariables() {
        return variableMap;
    }
}
