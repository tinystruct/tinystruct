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
import org.tinystruct.data.component.Builders;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.tinystruct.mcp.MCPSpecification.ErrorCodes;

/**
 * MCP Server Application that extends the base MCPApplication
 * and adds support for tools and resources.
 */
public class MCPServer extends MCPApplication {
    private static final Logger LOGGER = Logger.getLogger(MCPServer.class.getName());

    @Override
    public void init() {
        super.init();

        LOGGER.info("MCPServerApplication initialized");
    }

    /**
     * Handles a list tools request.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response
     */
    protected void handleListTools(JsonRpcRequest request, JsonRpcResponse response) {
        Builder result = new Builder();
        Builders toolsList = new Builders();

        // Add individual tool methods (which may be overloaded)
        for (List<MCPTool.ToolMethod> toolMethodList : toolMethods.values()) {
            if (toolMethodList.isEmpty()) continue;
            
            Builder mergedSchema = new Builder();
            mergedSchema.put("type", "object");
            Builder mergedProperties = new Builder();
            java.util.Set<String> allRequired = null;
            
            for (MCPTool.ToolMethod tm : toolMethodList) {
                Builder schema = tm.getSchema();
                if (schema.containsKey("properties")) {
                    Builder props = (Builder) schema.get("properties");
                    for (String key : props.keySet()) {
                        mergedProperties.put(key, props.get(key));
                    }
                }
                
                java.util.Set<String> currentRequired = new java.util.HashSet<>();
                if (schema.containsKey("required")) {
                    Object req = schema.get("required");
                    if (req instanceof String[]) {
                        for (String r : (String[]) req) {
                            currentRequired.add(r);
                        }
                    }
                }
                
                if (allRequired == null) {
                    allRequired = currentRequired;
                } else {
                    allRequired.retainAll(currentRequired); // Only parameters required by ALL overloads remain required
                }
            }
            
            mergedSchema.put("properties", mergedProperties);
            if (allRequired != null && !allRequired.isEmpty()) {
                mergedSchema.put("required", allRequired.toArray(new String[0]));
            }

            MCPTool.ToolMethod first = toolMethodList.get(0);
            Builder toolInfo = new Builder();
            toolInfo.put("name", first.getName());
            toolInfo.put("description", first.getDescription());
            toolInfo.put("inputSchema", mergedSchema);
            toolsList.add(toolInfo);
        }

        // Add registered tool classes
        for (MCPTool tool : tools.values()) {
            boolean hasSubMethods = false;
            String prefix = tool.getName() + "/";
            for (String methodName : toolMethods.keySet()) {
                if (methodName.startsWith(prefix)) {
                    hasSubMethods = true;
                    break;
                }
            }
            if (!hasSubMethods) {
                Builder toolInfo = new Builder();
                toolInfo.put("name", tool.getName());
                toolInfo.put("description", tool.getDescription());
                toolInfo.put("inputSchema", tool.getSchema());
                toolsList.add(toolInfo);
            }
        }

        result.put("tools", toolsList);

        response.setId(request.getId());
        response.setResult(result);
    }

    /**
     * Handles a call tool request.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response
     */
    protected void handleCallTool(JsonRpcRequest request, JsonRpcResponse response) {
        try {
            Builder params = request.getParams();
            String toolName = params.get("name").toString();
            Builder toolParams = (Builder) params.get("arguments");

            // First try to find the tool method
            List<MCPTool.ToolMethod> methods = toolMethods.get(toolName);
            if (methods != null && !methods.isEmpty()) {
                Object result = null;
                boolean executed = false;
                MCPException lastException = null;

                for (MCPTool.ToolMethod toolMethod : methods) {
                    try {
                        // Attempt to execute. The ToolMethod.execute will perform schema validation first.
                        result = toolMethod.execute(toolParams);
                        executed = true;
                        break;
                    } catch (MCPException e) {
                        lastException = e;
                    }
                }

                if (executed) {
                    response.setId(request.getId());
                    response.setResult(formatToolResult(result));
                } else if (lastException != null) {
                    throw lastException;
                } else {
                    throw new MCPException("No matching overloaded method found for tool: " + toolName);
                }
            } else {
                // Try to find the tool in the tools map
                MCPTool tool = tools.get(toolName);
                if (tool != null) {
                    Object result = tool.execute(toolParams);
                    response.setId(request.getId());
                    response.setResult(formatToolResult(result));
                } else {
                    response.setError(new JsonRpcError(ErrorCodes.METHOD_NOT_FOUND, "Tool not exists: " + toolName));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calling tool", e);
            response.setId(request.getId());
            
            // Extract the most relevant error message, unwrapping InvocationTargetException if necessary
            String errorMessage = e.getMessage();
            Throwable cause = e.getCause();
            
            while (cause != null) {
                if (cause instanceof java.lang.reflect.InvocationTargetException) {
                    Throwable target = ((java.lang.reflect.InvocationTargetException) cause).getTargetException();
                    if (target != null) {
                        errorMessage = target.getMessage();
                        break;
                    }
                }
                cause = cause.getCause();
            }
            
            response.setResult(formatToolErrorResult(errorMessage));
        }
    }

    /**
     * Handles a list resources request.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response
     */
    protected void handleListResources(JsonRpcRequest request, JsonRpcResponse response) {
        Builder result = new Builder();
        Builders resourcesList = new Builders();

        for (MCPDataResource resource : resources.values()) {
            Builder resourceInfo = new Builder();
            resourceInfo.put("uri", resource.getName()); // Use name as URI for now
            resourceInfo.put("name", resource.getName());
            resourceInfo.put("description", resource.getDescription());
            resourceInfo.put("mimeType", "text/plain"); // Default MIME type
            // annotations field is optional, so we can omit it
            resourcesList.add(resourceInfo);
        }

        result.put("resources", resourcesList);

        response.setId(request.getId());
        response.setResult(result);
    }

    /**
     * Handles a read resource request.
     *
     * @param request  The JSON-RPC request
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
                response.setError(
                        new JsonRpcError(ErrorCodes.RESOURCE_NOT_FOUND, "Resource not found: " + resourceName));
                return;
            }

            Object result = resource.execute(resourceParams);

            // Create the correct MCP response format with contents array
            Builder responseResult = new Builder();
            Builders contents = new Builders();
            Builder content = new Builder();
            content.put("uri", uri);
            content.put("mimeType", "text/plain");
            content.put("text", String.valueOf(result));
            contents.add(content);
            responseResult.put("contents", contents);

            response.setId(request.getId());
            response.setResult(responseResult);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading resource", e);
            response.setError(new JsonRpcError(ErrorCodes.INTERNAL_ERROR, "Error reading resource: " + e.getMessage()));
        }
    }

    /**
     * Handles a list prompts request.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response
     */
    protected void handleListPrompts(JsonRpcRequest request, JsonRpcResponse response) {
        Builder result = new Builder();
        Builders promptsList = new Builders();

        for (MCPPrompt prompt : prompts.values()) {
            Builder promptInfo = new Builder();
            promptInfo.put("name", prompt.getName());
            promptInfo.put("description", prompt.getDescription());

            // Create arguments array as required by MCP Prompt schema
            Builders arguments = new Builders();
            if (prompt.getSchema() != null && prompt.getSchema().containsKey("properties")) {
                Builder properties = (Builder) prompt.getSchema().get("properties");
                for (String propName : properties.keySet()) {
                    Builder property = (Builder) properties.get(propName);
                    Builder argument = new Builder();
                    argument.put("name", propName);
                    argument.put("description", property.get("description"));

                    // Check if this argument is required
                    boolean required = false;
                    if (prompt.getSchema().containsKey("required")) {
                        Object requiredObj = prompt.getSchema().get("required");
                        if (requiredObj instanceof String[]) {
                            String[] requiredArray = (String[]) requiredObj;
                            for (String req : requiredArray) {
                                if (req.equals(propName)) {
                                    required = true;
                                    break;
                                }
                            }
                        }
                    }
                    argument.put("required", required);
                    arguments.add(argument);
                }
            }
            promptInfo.put("arguments", arguments);
            promptsList.add(promptInfo);
        }

        result.put("prompts", promptsList);

        response.setId(request.getId());
        response.setResult(result);
    }

    /**
     * Handles a get prompt request.
     *
     * @param request  The JSON-RPC request
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

            // Create the correct MCP response format according to GetPromptResult
            Builder result = new Builder();
            result.put("description", prompt.getDescription());

            // Create messages array as required by MCP specification
            Builders messages = new Builders();
            Builder message = new Builder();
            message.put("role", "user"); // Default role for prompt messages
            Builder content = new Builder();
            content.put("type", "text");
            content.put("text", prompt.getTemplate());
            message.put("content", content);
            messages.add(message);

            result.put("messages", messages);

            response.setId(request.getId());
            response.setResult(result);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting prompt", e);
            response.setError(new JsonRpcError(ErrorCodes.INTERNAL_ERROR, "Error getting prompt: " + e.getMessage()));
        }
    }

    /**
     * Handles the 'logging/setLevel' JSON-RPC method.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response to populate
     */
    @Override
    protected void handleLoggingSetLevel(JsonRpcRequest request, JsonRpcResponse response) {
        Builder params = request.getParams();
        if (params == null || params.get("level") == null) {
            response.setError(new JsonRpcError(ErrorCodes.INVALID_PARAMS, "Missing level parameter"));
            return;
        }

        String levelStr = params.get("level").toString().toUpperCase();
        Level level;
        try {
            // Map common debug levels to JUL levels
            switch (levelStr) {
                case "DEBUG":
                    level = Level.FINE;
                    break;
                case "TRACE":
                    level = Level.FINER;
                    break;
                case "ERROR":
                    level = Level.SEVERE;
                    break;
                case "WARN":
                case "WARNING":
                    level = Level.WARNING;
                    break;
                case "INFO":
                    level = Level.INFO;
                    break;
                default:
                    level = Level.parse(levelStr);
                    break;
            }
            Logger.getLogger("").setLevel(level);
            // Also set for MCP package specifically
            Logger.getLogger("org.tinystruct.mcp").setLevel(level);
            LOGGER.info("Log level set to " + level);

            // Empty result object for success
            response.setId(request.getId());
            response.setResult(new Builder());
        } catch (IllegalArgumentException e) {
            response.setError(new JsonRpcError(ErrorCodes.INVALID_PARAMS, "Invalid log level: " + levelStr));
        }
    }

    @Override
    protected String[] getFeatures() {
        // Example: server supports core, tools, resources, prompts
        return new String[] { "core", "tools", "resources", "prompts" };
    }

    /**
     * Formats the tool execution result into the MCP-compliant JSON-RPC response
     * format.
     *
     * @param result The result of the tool execution
     * @return A Builder containing the formatted result
     */
    private Builder formatToolResult(Object result) {
        Builder resultBuilder = new Builder();
        Builders contentArray = new Builders();
        Builder contentItem = new Builder();
        contentItem.put("type", "text");
        contentItem.put("text", String.valueOf(result));
        contentArray.add(contentItem);
        resultBuilder.put("content", contentArray);
        resultBuilder.put("isError", false);

        return resultBuilder;
    }

    /**
     * Formats a tool execution error into the MCP-compliant JSON-RPC response
     * format.
     *
     * @param errorMessage The error message
     * @return A Builder containing the formatted error result
     */
    private Builder formatToolErrorResult(String errorMessage) {
        Builder resultBuilder = new Builder();
        Builders contentArray = new Builders();
        Builder contentItem = new Builder();
        contentItem.put("type", "text");
        contentItem.put("text", errorMessage != null ? errorMessage : "Unknown error executing tool");
        contentArray.add(contentItem);
        resultBuilder.put("content", contentArray);
        resultBuilder.put("isError", true);

        return resultBuilder;
    }
}

class SchemaGenerator {
    /**
     * Generates a schema Builder for a tool class by inspecting @Action
     * and @Argument annotations.
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