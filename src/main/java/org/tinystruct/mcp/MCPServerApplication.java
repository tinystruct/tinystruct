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

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.http.ResponseStatus;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.tinystruct.mcp.MCPSpecification.*;

/**
 * MCP Server Application that extends the base MCPApplication
 * and adds support for tools and resources.
 */
public class MCPServerApplication extends MCPApplication {
    private static final Logger LOGGER = Logger.getLogger(MCPServerApplication.class.getName());

    @Override
    public void init() {
        super.init();

        LOGGER.info("MCPServerApplication initialized");
    }

    /**
     * Handles a list tools request.
     *
     * @param request The JSON-RPC request
     * @param response The JSON-RPC response
     */
    protected void handleListTools(JsonRpcRequest request, JsonRpcResponse response) {
        Builder result = new Builder();
        Builders toolsList = new Builders();

        for (MCPTool tool : tools.values()) {
            Builder toolInfo = new Builder();
            toolInfo.put("name", tool.getName());
            toolInfo.put("description", tool.getDescription());
            toolInfo.put("schema", tool.getSchema());
            toolsList.add(toolInfo);
        }

        result.put("tools", toolsList);

        response.setId(request.getId());
        response.setResult(result);
    }

    /**
     * Handles a call tool request.
     *
     * @param request The JSON-RPC request
     * @param response The JSON-RPC response
     */
    protected void handleCallTool(JsonRpcRequest request, JsonRpcResponse response) {
        try {
            Builder params = request.getParams();
            String toolName = params.get("name").toString();
            Builder toolParams = (Builder) params.get("parameters");

            MCPTool tool = tools.get(toolName);
            if (tool == null) {
                response.setError(new JsonRpcError(ErrorCodes.RESOURCE_NOT_FOUND, "Tool not found: " + toolName));
                return;
            }

            Object result = tool.execute(toolParams);

            response.setId(request.getId());
            response.setResult(new Builder().put("result", result));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calling tool", e);
            response.setError(new JsonRpcError(ErrorCodes.INTERNAL_ERROR, "Error calling tool: " + e.getMessage()));
        }
    }

    /**
     * Handles a list resources request.
     *
     * @param request The JSON-RPC request
     * @param response The JSON-RPC response
     */
    protected void handleListResources(JsonRpcRequest request, JsonRpcResponse response) {
        Builder result = new Builder();
        List<Map<String, Object>> resourcesList = new ArrayList<>();

        for (MCPDataResource resource : resources.values()) {
            Map<String, Object> resourceInfo = new HashMap<>();
            resourceInfo.put("name", resource.getName());
            resourceInfo.put("description", resource.getDescription());
            resourceInfo.put("uriTemplate", resource.getUriTemplate());
            resourcesList.add(resourceInfo);
        }

        result.put("resources", resourcesList);

        response.setId(request.getId());
        response.setResult(result);
    }

    /**
     * Handles a read resource request.
     *
     * @param request The JSON-RPC request
     * @param response The JSON-RPC response
     */
    protected void handleReadResource(JsonRpcRequest request, JsonRpcResponse response) {
        try {
            Builder params = request.getParams();
            String uri = params.get("uri").toString();

            // Parse the URI to extract the resource name and parameters
            // This is a simplified implementation
            String resourceName = uri.substring(0, uri.indexOf("?"));
            String queryString = uri.substring(uri.indexOf("?") + 1);

            Builder resourceParams = new Builder();
            for (String param : queryString.split("&")) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    resourceParams.put(keyValue[0], keyValue[1]);
                }
            }

            MCPDataResource resource = resources.get(resourceName);
            if (resource == null) {
                response.setError(new JsonRpcError(ErrorCodes.RESOURCE_NOT_FOUND, "Resource not found: " + resourceName));
                return;
            }

            Object result = resource.execute(resourceParams);

            response.setId(request.getId());
            response.setResult(new Builder().put("result", result));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading resource", e);
            response.setError(new JsonRpcError(ErrorCodes.INTERNAL_ERROR, "Error reading resource: " + e.getMessage()));
        }
    }

    /**
     * Handles a list prompts request.
     *
     * @param request The JSON-RPC request
     * @param response The JSON-RPC response
     */
    protected void handleListPrompts(JsonRpcRequest request, JsonRpcResponse response) {
        Builder result = new Builder();
        Builders promptsList = new Builders();

        for (MCPPrompt prompt : prompts.values()) {
            Builder promptInfo = new Builder();
            promptInfo.put("name", prompt.getName());
            promptInfo.put("description", prompt.getDescription());
            promptInfo.put("schema", prompt.getSchema());
            promptsList.add(promptInfo);
        }

        result.put("prompts", promptsList);

        response.setId(request.getId());
        response.setResult(result);
    }

    /**
     * Handles a get prompt request.
     *
     * @param request The JSON-RPC request
     * @param response The JSON-RPC response
     */
    protected void handleGetPrompt(JsonRpcRequest request, JsonRpcResponse response) {
        try {
            Builder params = request.getParams();
            String promptName = params.get("name").toString();

            MCPPrompt prompt = prompts.get(promptName);
            if (prompt == null) {
                response.setError(new JsonRpcError(ErrorCodes.RESOURCE_NOT_FOUND, "Prompt not found: " + promptName));
                return;
            }

            Builder result = new Builder();
            result.put("name", prompt.getName());
            result.put("description", prompt.getDescription());
            result.put("template", prompt.getTemplate());
            result.put("schema", prompt.getSchema());

            response.setId(request.getId());
            response.setResult(result);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting prompt", e);
            response.setError(new JsonRpcError(ErrorCodes.INTERNAL_ERROR, "Error getting prompt: " + e.getMessage()));
        }
    }

    @Override
    protected String[] getFeatures() {
        // Example: server supports core, tools, resources, prompts
        return new String[] { "core", "tools", "resources", "prompts" };
    }
}

class SchemaGenerator {
    /**
     * Generates a schema Builder for a tool class by inspecting @Action and @Argument annotations.
     *
     * @param toolClass The tool class to inspect
     * @return The generated schema as a Builder
     */
    public static Builder generateSchema(Class<?> toolClass) {
        Builder schema = new Builder();
        Builder properties = new Builder();
        List<String> required = new ArrayList<>();

        for (Method method : toolClass.getDeclaredMethods()) {
            Action action = method.getAnnotation(Action.class);
            if (action != null) {
                for (Argument arg : action.arguments()) {
                    Builder argSchema = new Builder();
                    argSchema.put("type", arg.type());
                    argSchema.put("description", arg.description());
                    properties.put(arg.key(), argSchema);
                    if (!arg.optional()) {
                        required.add(arg.key());
                    }
                }
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