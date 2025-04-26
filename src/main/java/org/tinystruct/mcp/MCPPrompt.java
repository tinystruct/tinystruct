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

/**
 * Implementation of MCPResource for prompt templates.
 * Represents a template for generating text.
 * <p>
 * Prompts are resources that provide a way to generate text. They
 * typically take parameters to customize the generated text.
 * </p>
 */
public class MCPPrompt extends AbstractMCPResource {
    private final String template;
    private final Builder schema;

    /**
     * Constructs a new MCPPrompt.
     * <p>
     * The prompt can be executed locally if the client is null, or remotely
     * through the provided MCP client.
     * </p>
     *
     * @param name The name of the prompt (must not be null or empty)
     * @param description The description of the prompt
     * @param template The template text
     * @param schema The JSON schema of the prompt parameters
     * @param client The MCP client to use for execution (may be null for local prompts)
     * @throws IllegalArgumentException If name is null or empty
     */
    public MCPPrompt(String name, String description, String template, Builder schema, MCPClient client) {
        super(name, description, client);
        this.template = template;
        this.schema = schema;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.PROMPT;
    }

    /**
     * Get the template text.
     * <p>
     * The template text may contain placeholders that are replaced with parameter values.
     * </p>
     *
     * @return The template text
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Get the JSON schema of the prompt parameters.
     * <p>
     * The schema describes the parameters that the prompt accepts, including their
     * types, constraints, and descriptions.
     * </p>
     *
     * @return The schema as a Builder object (may be null)
     */
    public Builder getSchema() {
        return schema;
    }

    @Override
    public Object execute(Builder builder) throws MCPException {
        try {
            // Validate parameters
            validateParameters(builder);

            // Execute the prompt
            if (client != null) {
                // Remote execution through MCP client
                return client.executeResource(name, builder);
            } else if (supportsLocalExecution()) {
                // Local execution
                return executeLocally(builder);
            } else {
                throw new MCPException("Local execution not implemented for prompt: " + name);
            }
        } catch (MCPException e) {
            throw e; // Re-throw MCPException as is
        } catch (Exception e) {
            throw new MCPException("Error executing prompt: " + name, e);
        }
    }

    /**
     * Executes the prompt locally.
     * <p>
     * This method is called when the prompt is executed with a null client.
     * It should be overridden by subclasses to provide custom execution logic.
     * </p>
     *
     * @param builder The parameters to use for execution
     * @return The result of the execution
     * @throws MCPException If an error occurs during execution
     */
    protected Object executeLocally(Builder builder) throws MCPException {
        throw new MCPException("Local execution not implemented for prompt: " + name);
    }

    /**
     * Checks if the prompt supports local execution.
     * <p>
     * This method should be overridden by subclasses to indicate whether
     * they support local execution.
     * </p>
     *
     * @return true if the prompt supports local execution, false otherwise
     */
    protected boolean supportsLocalExecution() {
        return false;
    }

    /**
     * Validates the parameters against the schema.
     * <p>
     * This method checks that all required parameters are present and that
     * parameter types match the schema.
     * </p>
     *
     * @param builder The parameters to validate
     * @throws MCPException If the parameters are invalid
     */
    @Override
    protected void validateParameters(Builder builder) throws MCPException {
        // Default implementation does no validation
        // Subclasses should override this method to provide custom validation logic
    }
}
