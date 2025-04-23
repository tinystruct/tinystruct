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
 * <p>
 * Resources are the primary way for clients to interact with MCP servers. They can be:
 * <ul>
 *   <li>Tools: Executable functions that perform operations</li>
 *   <li>Data Sources: Provide access to data</li>
 *   <li>Templates: URI templates for accessing resources</li>
 *   <li>Prompts: Templates for generating text</li>
 * </ul>
 * </p>
 */
public interface MCPResource {
    /**
     * Get the name of the resource.
     * <p>
     * The name should be unique within its resource type and should be a valid
     * identifier that can be used in URIs and method calls.
     * </p>
     *
     * @return The resource name (never null or empty)
     */
    String getName();

    /**
     * Get the description of the resource.
     * <p>
     * The description should provide a clear explanation of what the resource does
     * and how it should be used.
     * </p>
     *
     * @return The resource description (may be null or empty)
     */
    String getDescription();

    /**
     * Get the type of the resource.
     * <p>
     * The resource type determines how the resource is used and what operations
     * can be performed on it.
     * </p>
     *
     * @return The resource type (never null)
     */
    ResourceType getType();

    /**
     * Execute the resource with the given parameters.
     * <p>
     * This method is called when a client requests to use the resource. The parameters
     * are passed as a Builder object, which contains key-value pairs where the keys are
     * parameter names and the values are parameter values.
     * </p>
     * <p>
     * Implementations should validate the parameters before executing the resource
     * and throw an MCPException if the parameters are invalid.
     * </p>
     *
     * @param builder The parameters to use for execution (may be null or empty)
     * @return The result of the execution (may be null)
     * @throws MCPException If an error occurs during execution or if the parameters are invalid
     */
    Object execute(Builder builder) throws MCPException;

    /**
     * Enum representing the different types of resources.
     * <p>
     * Each resource type has different characteristics and usage patterns.
     * </p>
     */
    enum ResourceType {
        /**
         * An executable function (tool)
         * <p>
         * Tools are resources that perform operations when executed. They typically
         * take parameters and return results.
         * </p>
         */
        TOOL,

        /**
         * A data source
         * <p>
         * Data sources provide access to data. They typically take parameters to
         * filter or transform the data and return the requested data.
         * </p>
         */
        DATA,

        /**
         * A URI template
         * <p>
         * URI templates are resources that provide a way to access other resources
         * through URIs. They typically take parameters to construct the URI.
         * </p>
         */
        TEMPLATE,

        /**
         * A prompt template
         * <p>
         * Prompt templates are resources that provide a way to generate text. They
         * typically take parameters to customize the generated text.
         * </p>
         */
        PROMPT
    }
}
