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

    private final Map<String, MCPTool> tools = new ConcurrentHashMap<>();
    private final Map<String, MCPDataResource> resources = new ConcurrentHashMap<>();
    private final Map<String, MCPPrompt> prompts = new ConcurrentHashMap<>();
    /**
     * Registry for custom RPC method handlers.
     */
    private final Map<String, RpcMethodHandler> rpcHandlers = new ConcurrentHashMap<>();

    @Override
    public void init() {
        super.init();
        // Register built-in handlers
        this.registerRpcHandler(Methods.LIST_TOOLS, (req, res, app) -> app.handleListTools(req, res));
        this.registerRpcHandler(Methods.CALL_TOOL, (req, res, app) -> app.handleCallTool(req, res));
        this.registerRpcHandler(Methods.LIST_RESOURCES, (req, res, app) -> app.handleListResources(req, res));
        this.registerRpcHandler(Methods.READ_RESOURCE, (req, res, app) -> app.handleReadResource(req, res));
        this.registerRpcHandler(Methods.LIST_PROMPTS, (req, res, app) -> app.handleListPrompts(req, res));
        this.registerRpcHandler(Methods.GET_PROMPT, (req, res, app) -> app.handleGetPrompt(req, res));
        LOGGER.info("MCPServerApplication initialized");
    }

    /**
     * Registers a tool with the server.
     *
     * @param tool The tool to register
     */
    public void registerTool(MCPTool tool) {
        // Generate schema for the tool using the SchemaGenerator utility
        Builder builder = SchemaGenerator.generateSchema(tool.getClass());
        tool.setSchema(builder);
        tools.put(tool.getName(), tool);
        LOGGER.info("Registered tool: " + tool.getName());
    }

    /**
     * Registers a resource with the server.
     *
     * @param resource The resource to register
     */
    public void registerResource(MCPDataResource resource) {
        resources.put(resource.getName(), resource);
        LOGGER.info("Registered resource: " + resource.getName());
    }

    /**
     * Registers a prompt with the server.
     *
     * @param prompt The prompt to register
     */
    public void registerPrompt(MCPPrompt prompt) {
        prompts.put(prompt.getName(), prompt);
        LOGGER.info("Registered prompt: " + prompt.getName());
    }

    /**
     * Registers a custom RPC method handler.
     *
     * @param method  the method name
     * @param handler the handler implementation
     */
    public void registerRpcHandler(String method, RpcMethodHandler handler) {
        rpcHandlers.put(method, handler);
        LOGGER.info("Registered RPC handler: " + method);
    }

    /**
     * Lists all registered tools.
     *
     * @return A formatted list of tools
     */
    @Action(value = "mcp-server/list-tools",
            description = "List all registered tools",
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String listTools() {
        StringBuilder sb = new StringBuilder();
        sb.append("Registered tools:\n");

        for (MCPTool tool : tools.values()) {
            sb.append(String.format("- %s: %s\n", tool.getName(), tool.getDescription()));
        }

        return sb.toString();
    }

    /**
     * Lists all registered prompts.
     *
     * @return A formatted list of prompts
     */
    @Action(value = "mcp-server/list-prompts",
            description = "List all registered prompts",
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String listPrompts() {
        StringBuilder sb = new StringBuilder();
        sb.append("Registered prompts:\n");

        for (MCPPrompt prompt : prompts.values()) {
            sb.append(String.format("- %s: %s\n", prompt.getName(), prompt.getDescription()));
        }

        return sb.toString();
    }

    @Action(Endpoints.RPC)
    @Override
    public String handleRpcRequest(Request request, Response response) throws ApplicationException {
        try {
            String body = request.body();
            JsonRpcRequest jsonRpcRequest = new JsonRpcRequest();
            jsonRpcRequest.parse(body);

            String method = jsonRpcRequest.getMethod();
            JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
            jsonRpcResponse.setId(jsonRpcRequest.getId());

            RpcMethodHandler handler = rpcHandlers.get(method);
            if (handler != null) {
                handler.handle(jsonRpcRequest, jsonRpcResponse, this);
            } else {
                // Let the parent class handle the standard methods
                return super.handleRpcRequest(request, response);
            }

            response.addHeader("Content-Type", Http.CONTENT_TYPE_JSON);
            return jsonRpcResponse.toString();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling RPC request", e);
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return jsonRpcHandler.createErrorResponse("Internal server error", ErrorCodes.INTERNAL_ERROR);
        }
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
    protected void handleGetCapabilities(JsonRpcRequest request, JsonRpcResponse response) {
        Builder result = new Builder();
        result.put("version", version());
        result.put("protocol", PROTOCOL_ID);

        // Add tools and resources capabilities
        List<String> featuresList = new ArrayList<>();
        for (String feature : Features.CORE_FEATURES) {
            featuresList.add(feature);
        }
        featuresList.add(Features.TOOLS);
        featuresList.add(Features.RESOURCES);
        featuresList.add(Features.PROMPTS);

        Builders features = new Builders();
        for (String feature : featuresList) {
            features.add(new Builder(feature));
        }
        result.put("features", features);

        response.setId(request.getId());
        response.setResult(result);
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