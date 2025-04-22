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
 */
public class MCPTool implements MCPResource {
    private final String name;
    private final String description;
    private final Builder schema;
    private final MCPClient client;
    
    /**
     * Constructs a new MCPTool.
     *
     * @param name The name of the tool
     * @param description The description of the tool
     * @param schema The JSON schema of the tool parameters
     * @param client The MCP client to use for execution
     */
    public MCPTool(String name, String description, Builder schema, MCPClient client) {
        this.name = name;
        this.description = description;
        this.schema = schema;
        this.client = client;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public ResourceType getType() {
        return ResourceType.TOOL;
    }
    
    /**
     * Get the JSON schema of the tool parameters.
     *
     * @return The schema as a Map
     */
    public Builder getSchema() {
        return schema;
    }
    
    @Override
    public Object execute(Builder builder) throws MCPException {
        try {
            return client.callTool(name, builder);
        } catch (Exception e) {
            throw new MCPException("Error executing tool: " + name, e);
        }
    }
}
