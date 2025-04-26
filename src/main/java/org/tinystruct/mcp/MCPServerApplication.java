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
import org.tinystruct.mcp.tools.CalculatorTool;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

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

    @Override
    public void init() {
        super.init();
        this.registerTool(new CalculatorTool());

        // Register a sample prompt
        Builder promptSchema = new Builder();
        Builder properties = new Builder();

        Builder nameParam = new Builder();
        nameParam.put("type", "string");
        nameParam.put("description", "The name to greet");

        properties.put("name", nameParam);
        promptSchema.put("type", "object");
        promptSchema.put("properties", properties);
        promptSchema.put("required", new String[]{"name"});

        MCPPrompt greetingPrompt = new MCPPrompt(
            "greeting",
            "A simple greeting prompt",
            "Hello, {{name}}! Welcome to the MCP server.",
            promptSchema,
            null
        ) {
            @Override
            protected boolean supportsLocalExecution() {
                return true;
            }

            @Override
            protected Object executeLocally(Builder builder) throws MCPException {
                String name = builder.get("name").toString();
                return getTemplate().replace("{{name}}", name);
            }
        };

        this.registerPrompt(greetingPrompt);

        LOGGER.info("MCPServerApplication initialized");
    }

    /**
     * Registers a tool with the server.
     *
     * @param tool The tool to register
     */
    public void registerTool(MCPTool tool) {
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
     * Starts the MCP server.
     *
     * @return Status message
     */
    @Action(value = "mcp-server/start",
            description = "Start the MCP server",
            options = {
                @Argument(key = "port", description = "Server port")
            },
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String start() {
        LOGGER.info("MCP server started");
        return "MCP server started";
    }

    /**
     * Stops the MCP server.
     *
     * @return Status message
     */
    @Action(value = "mcp-server/stop",
            description = "Stop the MCP server",
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String stop() {
        LOGGER.info("MCP server stopped");
        return "MCP server stopped";
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

    // We can't override the private handleMethod method, so we'll use a different approach
    // by overriding the handleRpcRequest method instead
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

            // Handle our custom methods
            if (Methods.LIST_TOOLS.equals(method)) {
                handleListTools(jsonRpcRequest, jsonRpcResponse);
            } else if (Methods.CALL_TOOL.equals(method)) {
                handleCallTool(jsonRpcRequest, jsonRpcResponse);
            } else if (Methods.LIST_RESOURCES.equals(method)) {
                handleListResources(jsonRpcRequest, jsonRpcResponse);
            } else if (Methods.READ_RESOURCE.equals(method)) {
                handleReadResource(jsonRpcRequest, jsonRpcResponse);
            } else if (Methods.LIST_PROMPTS.equals(method)) {
                handleListPrompts(jsonRpcRequest, jsonRpcResponse);
            } else if (Methods.GET_PROMPT.equals(method)) {
                handleGetPrompt(jsonRpcRequest, jsonRpcResponse);
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
