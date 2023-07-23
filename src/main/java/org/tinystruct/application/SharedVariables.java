package org.tinystruct.application;

import org.tinystruct.system.template.variable.Variable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SharedVariables {
    private final static String PREFIX_SHARED_VARIABLE_NAME = "[%";
    private final static String SUFFIX_SHARED_VARIABLE_NAME = "%]";
    private final static ConcurrentHashMap<String, Variable<?>> variableMap = new ConcurrentHashMap<>();

    private final static SharedVariables INSTANCE = new SharedVariables();
    public static SharedVariables getInstance() {
        return INSTANCE; // Return the singleton instance
    }

    public Variable<?> getVariable(String variable) {
        return variableMap.get(PREFIX_SHARED_VARIABLE_NAME + variable + SUFFIX_SHARED_VARIABLE_NAME);
    }

    public void setVariable(Variable<?> variable, boolean force) {
        String variableName = PREFIX_SHARED_VARIABLE_NAME + variable.getName() + SUFFIX_SHARED_VARIABLE_NAME;
        saveVariable(variableName, variable, force);
    }

    private void saveVariable(String variableName, Variable<?> variable, boolean force) {
        if (force || !variableMap.containsKey(variableName)) {
            variableMap.put(variableName, variable);
        }
    }

    public Map<String, Variable<?>> getVariables() {
        return variableMap;
    }
}
