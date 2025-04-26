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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
    private final Builder schema;
    private final boolean supportsLocalExecution;

    /**
     * Constructs a new MCPTool.
     * <p>
     * The tool can be executed locally if the client is null, or remotely through
     * the provided MCP client.
     * </p>
     *
     * @param name The name of the tool (must not be null or empty)
     * @param description The description of the tool
     * @param schema The JSON schema of the tool parameters
     * @param client The MCP client to use for execution (may be null for local tools)
     * @throws IllegalArgumentException If name is null or empty
     */
    public MCPTool(String name, String description, Builder schema, MCPClient client) {
        this(name, description, schema, client, false);
    }

    /**
     * Constructs a new MCPTool with explicit local execution support.
     * <p>
     * The tool can be executed locally if the client is null and supportsLocalExecution is true,
     * or remotely through the provided MCP client.
     * </p>
     *
     * @param name The name of the tool (must not be null or empty)
     * @param description The description of the tool
     * @param schema The JSON schema of the tool parameters
     * @param client The MCP client to use for execution (may be null for local tools)
     * @param supportsLocalExecution Whether this tool supports local execution
     * @throws IllegalArgumentException If name is null or empty
     */
    public MCPTool(String name, String description, Builder schema, MCPClient client, boolean supportsLocalExecution) {
        super(name, description, client);
        this.schema = schema;
        this.supportsLocalExecution = supportsLocalExecution;
    }

    // getName() and getDescription() are inherited from AbstractMCPResource

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
     * @return The schema as a Builder object (may be null)
     */
    public Builder getSchema() {
        return schema;
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
            if (client != null) {
                // Remote execution through MCP client
                return client.callTool(name, builder);
            } else if (supportsLocalExecution()) {
                // Local execution
                return executeLocally(builder);
            } else {
                throw new MCPException("Local execution not implemented for tool: " + name);
            }
        } catch (MCPException e) {
            throw e; // Re-throw MCPException as is
        } catch (Exception e) {
            throw new MCPException("Error executing tool: " + name + " - " + e.getMessage(), e);
        }
    }
}
