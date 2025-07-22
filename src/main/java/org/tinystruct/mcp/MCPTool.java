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
package org.tinystruct.mcp;

import org.tinystruct.data.component.Builder;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.lang.reflect.Method;

/**
 * Implementation of MCPResource for tools.
 * Represents an executable function in the MCP protocol.
 * <p>
 * Tools are resources that perform operations when executed. They take parameters
 * according to their schema and return results. Tools can be executed locally or
 * remotely through an MCP client.
 * </p>
 */
public class MCPTool extends AbstractMCPResource {
    private static final Logger LOGGER = Logger.getLogger(MCPTool.class.getName());
    private Builder schema;
    private final boolean supportsLocalExecution;

    /**
     * Constructs a new MCPTool for local execution with no schema.
     *
     * @param name The name of the tool (must not be null or empty)
     * @param description The description of the tool
     */
    public MCPTool(String name, String description) {
        this(name, description, null, null, true);
    }

    /**
     * Constructs a new MCPTool for local execution with a schema.
     *
     * @param name The name of the tool (must not be null or empty)
     * @param description The description of the tool
     * @param schema The schema for the tool parameters
     */
    public MCPTool(String name, String description, Builder schema) {
        this(name, description, schema, null, true);
    }

    /**
     * Constructs a new MCPTool for remote or local execution, without schema.
     *
     * @param name The name of the tool (must not be null or empty)
     * @param description The description of the tool
     * @param client The MCP client to use for execution (maybe null for local tools)
     */
    public MCPTool(String name, String description, MCPClient client) {
        this(name, description, null, client, false);
    }

    /**
     * Constructs a new MCPTool for remote or local execution, with schema and explicit local execution flag.
     *
     * @param name The name of the tool (must not be null or empty)
     * @param description The description of the tool
     * @param schema The schema for the tool parameters (maybe null)
     * @param client The MCP client to use for execution (maybe null for local tools)
     */
    public MCPTool(String name, String description, Builder schema, MCPClient client) {
        super(name, description, client);
        this.schema = schema;
        this.supportsLocalExecution = true; // Default to true for test tools
    }

    /**
     * Constructs a new MCPTool for remote or local execution, with schema and explicit local execution flag.
     *
     * @param name The name of the tool (must not be null or empty)
     * @param description The description of the tool
     * @param schema The schema for the tool parameters (maybe null)
     * @param client The MCP client to use for execution (maybe null for local tools)
     * @param supportsLocalExecution Whether this tool supports local execution
     */
    public MCPTool(String name, String description, Builder schema, MCPClient client, boolean supportsLocalExecution) {
        super(name, description, client);
        this.schema = schema;
        this.supportsLocalExecution = supportsLocalExecution;
    }

    /**
     * Returns the type of this resource.
     * <p>
     * This method indicates that this resource is a tool.
     * </p>
     *
     * @return The resource type, which is always {@link ResourceType#TOOL}
     */
    @Override
    public ResourceType getType() {
        return ResourceType.TOOL;
    }

    /**
     * Get the JSON schema of the tool parameters.
     * <p>
     * The schema describes the parameters that the tool accepts, including their
     * types, constraints, and descriptions.
     * </p>
     *
     * @return The schema as a Builder object (maybe null)
     */
    public Builder getSchema() {
        return schema;
    }

    /**
     * Set the JSON schema of the tool parameters.
     * <p>
     * This method allows updating the schema after the tool has been created.
     * </p>
     *
     * @param schema The new schema to set (must not be null)
     * @throws IllegalArgumentException If the schema is null
     */
    public void setSchema(Builder schema) {
        if (schema == null) {
            throw new IllegalArgumentException("Schema cannot be null");
        }
        this.schema = schema;
    }

    /**
     * Validates the parameters against the schema.
     * <p>
     * This method checks that:
     * <ul>
     *   <li>All required parameters are present</li>
     *   <li>Parameter types match the schema</li>
     *   <li>Enum values are valid</li>
     * </ul>
     * </p>
     *
     * @param builder The parameters to validate
     * @throws MCPException If the parameters are invalid
     */
    @Override
    protected void validateParameters(Builder builder) throws MCPException {
        if (schema == null) {
            // No schema to validate against
            return;
        }

        List<String> validationErrors = new ArrayList<>();

        // Check required parameters
        if (schema.containsKey("required")) {
            Object requiredObj = schema.get("required");
            if (requiredObj instanceof String[]) {
                String[] required = (String[]) requiredObj;
                for (String param : required) {
                    if (!builder.containsKey(param) || builder.get(param) == null) {
                        validationErrors.add("Missing required parameter: " + param);
                    }
                }
            }
        }

        // Check parameter types and constraints
        if (schema.containsKey("properties") && !builder.isEmpty()) {
            Builder properties = (Builder) schema.get("properties");

            for (String paramName : builder.keySet()) {
                if (properties.containsKey(paramName)) {
                    Builder paramSchema = (Builder) properties.get(paramName);
                    Object paramValue = builder.get(paramName);

                    // Skip null values (already checked in required validation)
                    if (paramValue == null) {
                        continue;
                    }

                    // Type validation
                    if (paramSchema.containsKey("type")) {
                        String expectedType = paramSchema.get("type").toString();
                        if (!validateType(paramValue, expectedType)) {
                            validationErrors.add("Parameter '" + paramName + "' has invalid type. Expected: " +
                                                expectedType + ", Got: " + paramValue.getClass().getSimpleName());
                        }
                    }

                    // Enum validation
                    if (paramSchema.containsKey("enum")) {
                        Object enumObj = paramSchema.get("enum");
                        if (enumObj instanceof String[]) {
                            String[] enumValues = (String[]) enumObj;
                            boolean valid = false;
                            for (String enumValue : enumValues) {
                                if (enumValue.equals(paramValue.toString())) {
                                    valid = true;
                                    break;
                                }
                            }
                            if (!valid) {
                                validationErrors.add("Parameter '" + paramName + "' has invalid value. Expected one of: " +
                                                    String.join(", ", enumValues) + ", Got: " + paramValue);
                            }
                        }
                    }
                }
            }
        }

        // If there are validation errors, throw an exception
        if (!validationErrors.isEmpty()) {
            throw new MCPException("Parameter validation failed for tool '" + name + "': " +
                                  String.join("; ", validationErrors));
        }
    }

    /**
     * Validates that a parameter value matches the expected type.
     *
     * @param value The parameter value
     * @param expectedType The expected type from the schema
     * @return true if the value matches the expected type, false otherwise
     */
    private boolean validateType(Object value, String expectedType) {
        switch (expectedType) {
            case "string":
                return value instanceof String;
            case "number":
                return value instanceof Number;
            case "integer":
                return value instanceof Integer || value instanceof Long;
            case "boolean":
                return value instanceof Boolean;
            case "array":
                return value instanceof List || value instanceof Object[] || value.getClass().isArray();
            case "object":
                return value instanceof Builder;
            default:
                LOGGER.warning("Unknown type in schema: " + expectedType);
                return true; // Unknown type, assume valid
        }
    }

    /**
     * Checks if the tool supports local execution.
     *
     * @return true if the tool supports local execution, false otherwise
     */
    @Override
    protected boolean supportsLocalExecution() {
        return supportsLocalExecution;
    }

    @Override
    public Object execute(Builder builder) throws MCPException {
        try {
            // Validate parameters
            validateParameters(builder);

            // Execute the tool
            if (supportsLocalExecution()) {
                // Local execution
                return executeLocally(builder);
            } else if (client != null) {
                // Remote execution through MCP client
                return client.callTool(name, builder);
            } else {
                throw new MCPException("Local execution not implemented for tool: " + name);
            }
        } catch (MCPException e) {
            throw e; // Re-throw MCPException as is
        } catch (Exception e) {
            throw new MCPException("Error executing tool: " + name + " - " + e.getMessage(), e);
        }
    }

    /**
     * Inner static class representing an individual tool method for MCP.
     */
    public static class MCPToolMethod {
        private static final Logger LOGGER = Logger.getLogger(MCPToolMethod.class.getName());
        
        private final String name;
        private final String description;
        private final Builder schema;
        private final Method method;
        private final Object toolInstance;
        private final List<ParameterInfo> parameters;

        /**
         * Information about a tool method parameter.
         */
        public static class ParameterInfo {
            private final String name;
            private final String type;
            private final String description;
            private final boolean required;
            private final String[] enumValues;

            public ParameterInfo(String name, String type, String description, boolean required, String[] enumValues) {
                this.name = name;
                this.type = type;
                this.description = description;
                this.required = required;
                this.enumValues = enumValues;
            }

            public String getName() { return name; }
            public String getType() { return type; }
            public String getDescription() { return description; }
            public boolean isRequired() { return required; }
            public String[] getEnumValues() { return enumValues; }
        }

        /**
         * Constructs a new MCPToolMethod from a method and its Action annotation.
         *
         * @param method The Java method
         * @param action The Action annotation
         * @param toolInstance The tool instance that contains this method
         */
        public MCPToolMethod(Method method, Action action, Object toolInstance) {
            this.method = method;
            this.toolInstance = toolInstance;
            this.name = action.value();
            this.description = action.description();
            this.parameters = new ArrayList<>();
            
            // Extract parameter information from the Action annotation
            for (Argument arg : action.arguments()) {
                ParameterInfo paramInfo = new ParameterInfo(
                    arg.key(),
                    arg.type(),
                    arg.description(),
                    !arg.optional(),
                    null // No enum values support in current Argument annotation
                );
                this.parameters.add(paramInfo);
            }
            
            // Generate schema from parameters
            this.schema = generateSchema();
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public Builder getSchema() { return schema; }
        public List<ParameterInfo> getParameters() { return parameters; }

        public Object execute(Builder parameters) throws MCPException {
            try {
                Object[] args = convertParametersToArguments(parameters);
                return method.invoke(toolInstance, args);
            } catch (Exception e) {
                LOGGER.severe("Error executing tool method " + name + ": " + e.getCause().getMessage());
                throw new MCPException("Error executing tool method " + name + ": " + e.getCause().getMessage(), e);
            }
        }

        private Object[] convertParametersToArguments(Builder parameters) throws MCPException {
            Class<?>[] paramTypes = method.getParameterTypes();
            Object[] args = new Object[paramTypes.length];
            for (int i = 0; i < this.parameters.size(); i++) {
                ParameterInfo paramInfo = this.parameters.get(i);
                String paramName = paramInfo.getName();
                Object value = parameters.get(paramName);
                if (value == null && paramInfo.isRequired()) {
                    throw new MCPException("Missing required parameter: " + paramName);
                }
                if (i < paramTypes.length) {
                    Class<?> paramType = paramTypes[i];
                    args[i] = convertValue(value, paramType);
                }
            }
            return args;
        }

        private Object convertValue(Object value, Class<?> targetType) throws MCPException {
            try {
                if (targetType == String.class) {
                    return value.toString();
                } else if (targetType == int.class || targetType == Integer.class) {
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }
                    return Integer.parseInt(value.toString());
                } else if (targetType == long.class || targetType == Long.class) {
                    if (value instanceof Number) {
                        return ((Number) value).longValue();
                    }
                    return Long.parseLong(value.toString());
                } else if (targetType == double.class || targetType == Double.class) {
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                    return Double.parseDouble(value.toString());
                } else if (targetType == float.class || targetType == Float.class) {
                    if (value instanceof Number) {
                        return ((Number) value).floatValue();
                    }
                    return Float.parseFloat(value.toString());
                } else if (targetType == boolean.class || targetType == Boolean.class) {
                    if (value instanceof Boolean) {
                        return value;
                    }
                    return Boolean.parseBoolean(value.toString());
                } else {
                    throw new MCPException("Unsupported parameter type: " + targetType.getName());
                }
            } catch (NumberFormatException e) {
                throw new MCPException("Invalid number format for parameter: " + e.getMessage());
            }
        }

        private Builder generateSchema() {
            Builder schema = new Builder();
            Builder properties = new Builder();
            List<String> required = new ArrayList<>();
            for (ParameterInfo param : parameters) {
                Builder paramSchema = new Builder();
                paramSchema.put("type", param.getType());
                paramSchema.put("description", param.getDescription());
                if (param.getEnumValues() != null && param.getEnumValues().length > 0) {
                    paramSchema.put("enum", param.getEnumValues());
                }
                properties.put(param.getName(), paramSchema);
                if (param.isRequired()) {
                    required.add(param.getName());
                }
            }
            schema.put("type", "object");
            schema.put("properties", properties);
            if (!required.isEmpty()) {
                schema.put("required", required.toArray(new String[0]));
            }
            return schema;
        }
    }
}
