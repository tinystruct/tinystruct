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
 * Implementation of MCPResource for tools.
 * Represents an executable function in the MCP protocol.
 * <p>
 * Tools are resources that perform operations when executed. They take parameters
 * according to their schema and return results. Tools can be executed locally or
 * remotely through an MCP client.
 * </p>
 */
public class MCPTool extends AbstractMCPResource {
    private final Builder schema;

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
        super(name, description, client);
        this.schema = schema;
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
            throw new MCPException("Error executing tool: " + name, e);
        }
    }
}
