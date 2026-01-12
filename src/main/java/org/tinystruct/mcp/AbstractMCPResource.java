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
 * Abstract base class for MCP resources.
 * <p>
 * This class provides common functionality for all MCP resources, including
 * name, description, and client handling.
 * </p>
 */
public abstract class AbstractMCPResource implements MCPResource {
    protected final String name;
    protected final String description;
    protected final MCPClient client;
    
    /**
     * Constructs a new AbstractMCPResource.
     *
     * @param name The name of the resource (must not be null or empty)
     * @param description The description of the resource
     * @param client The MCP client to use for execution (may be null for local resources)
     * @throws IllegalArgumentException If name is null or empty
     */
    protected AbstractMCPResource(String name, String description, MCPClient client) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource name must not be null or empty");
        }
        this.name = name;
        this.description = description;
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
    
    /**
     * Validates the parameters for the resource.
     * <p>
     * This method should be overridden by subclasses to provide custom validation
     * logic. The default implementation does no validation.
     * </p>
     *
     * @param builder The parameters to validate
     * @throws MCPException If the parameters are invalid
     */
    protected void validateParameters(Builder builder) throws MCPException {
        // Default implementation does no validation
        // Subclasses should override this method to provide custom validation logic
    }
    
    /**
     * Checks if the resource can be executed locally.
     * <p>
     * This method should be overridden by subclasses that support local execution.
     * The default implementation returns false.
     * </p>
     *
     * @return true if the resource can be executed locally, false otherwise
     */
    protected boolean supportsLocalExecution() {
        return false;
    }
    
    /**
     * Executes the resource locally.
     * <p>
     * This method should be overridden by subclasses that support local execution.
     * The default implementation throws an MCPException.
     * </p>
     *
     * @param builder The parameters to use for execution
     * @return The result of the execution
     * @throws MCPException If an error occurs during execution or if local execution is not supported
     */
    abstract protected Object executeLocally(Builder builder) throws MCPException;
}
