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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of MCPResource for data resources.
 * Represents a data source accessed through a URI template.
 * <p>
 * Data resources provide access to data through URI templates. They take parameters
 * to filter or transform the data and return the requested data.
 * </p>
 */
public class MCPDataResource extends AbstractMCPResource {
    private final String uriTemplate;

    /**
     * Constructs a new MCPDataResource.
     * <p>
     * The data resource can be accessed locally if the client is null, or remotely
     * through the provided MCP client.
     * </p>
     *
     * @param name The name of the resource (must not be null or empty)
     * @param description The description of the resource
     * @param uriTemplate The URI template for accessing the resource
     * @param client The MCP client to use for execution (may be null for local resources)
     * @throws IllegalArgumentException If name is null or empty
     */
    public MCPDataResource(String name, String description, String uriTemplate, MCPClient client) {
        super(name, description, client);
        this.uriTemplate = uriTemplate;
    }

    // getName() and getDescription() are inherited from AbstractMCPResource

    @Override
    public ResourceType getType() {
        return ResourceType.DATA;
    }

    /**
     * Get the URI template for this resource.
     * <p>
     * The URI template is used to construct the URI for accessing the resource.
     * It may contain placeholders that are replaced with parameter values.
     * </p>
     *
     * @return The URI template (may be null or empty)
     */
    public String getUriTemplate() {
        return uriTemplate;
    }

    @Override
    public Object execute(Builder builder) throws MCPException {
        try {
            // Validate parameters
            validateParameters(builder);

            // Resolve the URI template with the parameters
            String uri = resolveUriTemplate(uriTemplate, builder);

            // Execute the resource
            if (client != null) {
                // Remote execution through MCP client
                return client.readResource(uri);
            } else if (supportsLocalExecution()) {
                // Local execution
                return executeLocally(builder);
            } else {
                throw new MCPException("Local execution not implemented for data resource: " + name);
            }
        } catch (MCPException e) {
            throw e; // Re-throw MCPException as is
        } catch (Exception e) {
            throw new MCPException("Error accessing resource: " + name, e);
        }
    }

    /**
     * Resolves a URI template with the given parameters.
     * Replaces {param} placeholders with actual values.
     *
     * @param template The URI template
     * @param parameters The parameters to use for resolution
     * @return The resolved URI
     */
    private String resolveUriTemplate(String template, Map<String, Object> parameters) {
        String result = template;
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(template);

        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (parameters.containsKey(paramName)) {
                String paramValue = String.valueOf(parameters.get(paramName));
                result = result.replace("{" + paramName + "}", paramValue);
            }
        }

        return result;
    }

    /**
     * @param builder The parameters to use for execution
     * @return
     * @throws MCPException
     */
    @Override
    protected Object executeLocally(Builder builder) throws MCPException {
        throw new MCPException("Local execution not implemented for data resource: " + name);
    }
}
