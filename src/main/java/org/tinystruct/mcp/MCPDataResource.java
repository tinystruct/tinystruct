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
 */
public class MCPDataResource implements MCPResource {
    private final String name;
    private final String description;
    private final String uriTemplate;
    private final MCPClient client;
    
    /**
     * Constructs a new MCPDataResource.
     *
     * @param name The name of the resource
     * @param description The description of the resource
     * @param uriTemplate The URI template for accessing the resource
     * @param client The MCP client to use for execution
     */
    public MCPDataResource(String name, String description, String uriTemplate, MCPClient client) {
        this.name = name;
        this.description = description;
        this.uriTemplate = uriTemplate;
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
        return ResourceType.DATA;
    }
    
    /**
     * Get the URI template for this resource.
     *
     * @return The URI template
     */
    public String getUriTemplate() {
        return uriTemplate;
    }
    
    @Override
    public Object execute(Builder builder) throws MCPException {
        try {
            String uri = resolveUriTemplate(uriTemplate, builder);
            return client.readResource(uri);
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
}
