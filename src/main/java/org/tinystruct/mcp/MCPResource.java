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
 * Unified interface for MCP resources, including tools, data sources, and templates.
 * This interface represents the concept that tools can be considered as a type of resource.
 */
public interface MCPResource {
    /**
     * Get the name of the resource.
     *
     * @return The resource name
     */
    String getName();

    /**
     * Get the description of the resource.
     *
     * @return The resource description
     */
    String getDescription();

    /**
     * Get the type of the resource.
     *
     * @return The resource type
     */
    ResourceType getType();

    /**
     * Execute the resource with the given parameters.
     *
     * @param builder The parameters to use for execution
     * @return The result of the execution
     * @throws MCPException If an error occurs during execution
     */
    Object execute(Builder builder) throws MCPException;

    /**
     * Enum representing the different types of resources.
     */
    enum ResourceType {
        /**
         * An executable function (tool)
         */
        TOOL,
        
        /**
         * A data source
         */
        DATA,
        
        /**
         * A URI template
         */
        TEMPLATE,
        
        /**
         * A prompt template
         */
        PROMPT
    }
}
