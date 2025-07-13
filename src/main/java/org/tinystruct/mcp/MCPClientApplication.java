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

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.tinystruct.mcp.MCPSpecification.Methods;

/**
 * MCPClientApplication is a tinystruct-based module for interacting with MCP servers.
 * It provides a command-line interface for discovering and executing tools,
 * accessing resources, and managing the MCP client.
 */
public class MCPClientApplication extends AbstractApplication {
    private static final Logger LOGGER = Logger.getLogger(MCPClientApplication.class.getName());

    private MCPClient client;

    @Override
    public void init() {
        // Set template not required for CLI operations
        this.setTemplateRequired(false);

        // Register actions with the ActionRegistry
        // This is handled automatically by the AnnotationProcessor in AbstractApplication
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    /**
     * Connect to an MCP server.
     *
     * @return Connection status message
     */
    @Action(value = Methods.INITIALIZE,
            description = "Connect to an MCP server",
            options = {
                @Argument(key = "url", description = "Server URL"),
                @Argument(key = "token", description = "Authentication token")
            },
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String connect() {
        String url = getContext().getAttribute("--url") != null ? getContext().getAttribute("--url").toString() : null;
        String token = getContext().getAttribute("--token") != null ? getContext().getAttribute("--token").toString() : null;

        if (url == null) {
            return "Error: URL is required";
        }
        try {
            this.client = new MCPClient(url, token);
            this.client.connect();
            return "Connected to MCP server at " + url;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to MCP server", e);
            return "Connection failed: " + e.getMessage();
        }
    }

    /**
     * Disconnect from the MCP server.
     *
     * @return Disconnection status message
     */
    @Action(value = Methods.SHUTDOWN,
            description = "Disconnect from the MCP server",
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String disconnect() {
        if (client == null) {
            return "Not connected to any MCP server";
        }

        try {
            client.disconnect();
            return "Disconnected from MCP server";
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to disconnect from MCP server", e);
            return "Disconnection failed: " + e.getMessage();
        }
    }

    /**
     * List all available resources.
     *
     * @return A formatted list of resources
     */
    @Action(value = Methods.LIST_RESOURCES,
            description = "List all available resources",
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String listResources() {
        if (client == null) {
            return "Not connected to any MCP server";
        }

        try {
            List<MCPResource> resources = client.listResources();
            StringBuilder sb = new StringBuilder();
            sb.append("Available resources:\n");

            for (MCPResource resource : resources) {
                sb.append(String.format("- %s (%s): %s\n",
                        resource.getName(),
                        resource.getType(),
                        resource.getDescription()));
            }

            return sb.toString();
        } catch (MCPException e) {
            LOGGER.log(Level.SEVERE, "Failed to list resources", e);
            return "Failed to list resources: " + e.getMessage();
        }
    }

    /**
     * List resources of a specific type.
     *
     * @param type The resource type to filter by
     * @return A formatted list of resources of the specified type
     */
    @Action(value = Methods.LIST_RESOURCES,
            description = "List resources of a specific type",
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String listResourcesByType(String type) {
        if (client == null) {
            return "Not connected to any MCP server";
        }

        try {
            MCPResource.ResourceType resourceType = MCPResource.ResourceType.valueOf(type.toUpperCase());
            List<MCPResource> resources = client.listResourcesByType(resourceType);
            StringBuilder sb = new StringBuilder();
            sb.append("Available " + type + " resources:\n");

            for (MCPResource resource : resources) {
                sb.append(String.format("- %s: %s\n",
                        resource.getName(),
                        resource.getDescription()));
            }

            return sb.toString();
        } catch (IllegalArgumentException e) {
            return "Invalid resource type: " + type;
        } catch (MCPException e) {
            LOGGER.log(Level.SEVERE, "Failed to list resources", e);
            return "Failed to list resources: " + e.getMessage();
        }
    }

    /**
     * Execute a resource with the given parameters.
     *
     * @return The result of the execution
     */
    @Action(value = "execute",
            description = "Execute a resource",
            options = {
                @Argument(key = "name", description = "Resource name"),
                @Argument(key = "params", description = "Parameters as JSON")
            },
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String executeResource() {
        String name = getContext().getAttribute("--name") != null ? getContext().getAttribute("--name").toString() : null;
        String params = getContext().getAttribute("--params") != null ? getContext().getAttribute("--params").toString() : null;

        if (name == null) {
            return "Error: Resource name is required";
        }
        if (client == null) {
            return "Not connected to any MCP server";
        }

        try {
            Builder parameters = new Builder();
            if (params != null && !params.isEmpty()) {
                // Parse JSON parameters (simplified for example)
                // In a real implementation, use a proper JSON parser
                String[] pairs = params.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        parameters.put(keyValue[0].trim(), keyValue[1].trim());
                    }
                }
            }

            Object result = client.executeResource(name, parameters);
            return "Result: " + result;
        } catch (MCPException e) {
            LOGGER.log(Level.SEVERE, "Failed to execute resource", e);
            return "Execution failed: " + e.getMessage();
        }
    }

    /**
     * Get information about a specific resource.
     *
     * @param name The name of the resource
     * @return Information about the resource
     */
    @Action(value = "info",
            description = "Get information about a specific resource",
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String getResourceInfo(String name) {
        if (client == null) {
            return "Not connected to any MCP server";
        }

        try {
            MCPResource resource = client.getResource(name);
            if (resource == null) {
                return "Resource not found: " + name;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Resource Information:\n");
            sb.append("Name: ").append(resource.getName()).append("\n");
            sb.append("Type: ").append(resource.getType()).append("\n");
            sb.append("Description: ").append(resource.getDescription()).append("\n");

            if (resource instanceof MCPTool) {
                MCPTool tool = (MCPTool) resource;
                sb.append("Schema: ").append(tool.getSchema()).append("\n");
            }

            return sb.toString();
        } catch (MCPException e) {
            LOGGER.log(Level.SEVERE, "Failed to get resource info", e);
            return "Failed to get resource info: " + e.getMessage();
        }
    }

    /**
     * List all available tools.
     *
     * @return A formatted list of tools
     */
    @Action(value = Methods.LIST_TOOLS,
            description = "List all available tools",
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String listTools() {
        if (client == null) {
            return "Not connected to any MCP server";
        }

        try {
            List<MCPResource> resources = client.listResourcesByType(MCPResource.ResourceType.TOOL);
            StringBuilder sb = new StringBuilder();
            sb.append("Available tools:\n");

            for (MCPResource resource : resources) {
                sb.append(String.format("- %s: %s\n",
                        resource.getName(),
                        resource.getDescription()));
            }

            return sb.toString();
        } catch (MCPException e) {
            LOGGER.log(Level.SEVERE, "Failed to list tools", e);
            return "Failed to list tools: " + e.getMessage();
        }
    }

    /**
     * Call a tool with the given parameters.
     *
     * @return The result of the tool execution
     */
    @Action(value = Methods.CALL_TOOL,
            description = "Call a tool",
            options = {
                @Argument(key = "name", description = "Tool name"),
                @Argument(key = "arguments", description = "Tool arguments as key:value pairs")
            },
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String callTool() {
        String name = getContext().getAttribute("--name") != null ? getContext().getAttribute("--name").toString() : null;
        String arguments = getContext().getAttribute("--arguments") != null ? getContext().getAttribute("--arguments").toString() : null;

        if (name == null) {
            return "Error: Tool name is required";
        }
        if (client == null) {
            return "Not connected to any MCP server";
        }

        try {
            Builder parameters = new Builder();
            if (arguments != null && !arguments.isEmpty()) {
                // Parse arguments as key:value pairs
                String[] pairs = arguments.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        
                        // Try to parse as number if possible
                        try {
                            if (value.contains(".")) {
                                parameters.put(key, Double.parseDouble(value));
                            } else {
                                parameters.put(key, Integer.parseInt(value));
                            }
                        } catch (NumberFormatException e) {
                            parameters.put(key, value);
                        }
                    }
                }
            }

            Object result = client.callTool(name, parameters);
            return "Result: " + result;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to call tool", e);
            return "Tool call failed: " + e.getMessage();
        }
    }

    /**
     * List all available prompts.
     *
     * @return A formatted list of prompts
     */
    @Action(value = Methods.LIST_PROMPTS,
            description = "List all available prompts",
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String listPrompts() {
        if (client == null) {
            return "Not connected to any MCP server";
        }

        try {
            List<MCPResource> resources = client.listResourcesByType(MCPResource.ResourceType.PROMPT);
            StringBuilder sb = new StringBuilder();
            sb.append("Available prompts:\n");

            for (MCPResource resource : resources) {
                sb.append(String.format("- %s: %s\n",
                        resource.getName(),
                        resource.getDescription()));
            }

            return sb.toString();
        } catch (MCPException e) {
            LOGGER.log(Level.SEVERE, "Failed to list prompts", e);
            return "Failed to list prompts: " + e.getMessage();
        }
    }

    /**
     * Get a prompt template by name.
     *
     * @param name The name of the prompt
     * @return The prompt template and messages
     */
    @Action(value = Methods.GET_PROMPT,
            description = "Get a prompt template by name",
            options = {
                @Argument(key = "name", description = "Prompt name"),
                @Argument(key = "arguments", description = "Prompt arguments as JSON")
            },
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String getPrompt(String name) {
        String arguments = getContext().getAttribute("--arguments") != null ? getContext().getAttribute("--arguments").toString() : null;

        if (name == null) {
            return "Error: Prompt name is required";
        }
        if (client == null) {
            return "Not connected to any MCP server";
        }

        try {
            Builder params = new Builder();
            params.put("name", name);
            
            if (arguments != null && !arguments.isEmpty()) {
                Builder args = new Builder();
                // Parse arguments as key:value pairs
                String[] pairs = arguments.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        args.put(keyValue[0].trim(), keyValue[1].trim());
                    }
                }
                params.put("arguments", args);
            }

            JsonRpcRequest request = new JsonRpcRequest();
            request.setMethod(Methods.GET_PROMPT);
            request.setParams(params);

            // For now, return a simple message since we need to implement the actual request
            return "Prompt '" + name + "' requested. Arguments: " + arguments;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get prompt", e);
            return "Failed to get prompt: " + e.getMessage();
        }
    }

    /**
     * Main CLI entry point.
     *
     * @return Help text
     * @throws ApplicationException If an error occurs
     */
    @Action(value = "mcp",
            description = "Model Context Protocol Tool",
            options = {
                @Argument(key = "help", description = "Show help information"),
                @Argument(key = "version", description = "Show version information"),
                @Argument(key = "import", description = "Import application")
            },
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String main() throws ApplicationException {
        if (getContext().getAttribute("--help") != null) {
            return getHelpText();
        } else if (getContext().getAttribute("--version") != null) {
            return "MCPTool version " + version();
        }

        return getHelpText();
    }

    /**
     * Get help text for the tool.
     *
     * @return Help text
     */
    private String getHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("MCPTool - Model Context Protocol Tool\n");
        sb.append("Version: ").append(version()).append("\n\n");
        sb.append("Usage:\n");
        sb.append("  bin/dispatcher mcp/connect --url <server-url> [--token <auth-token>]  Connect to an MCP server\n");
        sb.append("  bin/dispatcher mcp/disconnect                                         Disconnect from the MCP server\n");
        sb.append("  bin/dispatcher mcp/list                                               List all available resources\n");
        sb.append("  bin/dispatcher mcp/list/tools                                         List all available tools\n");
        sb.append("  bin/dispatcher mcp/list/prompts                                       List all available prompts\n");
        sb.append("  bin/dispatcher mcp/list/<type>                                        List resources of a specific type\n");
        sb.append("  bin/dispatcher mcp/call --name <tool-name> --arguments <args>          Call a tool\n");
        sb.append("  bin/dispatcher mcp/execute --name <resource-name> [--params <json>]   Execute a resource\n");
        sb.append("  bin/dispatcher mcp/info/<resource-name>                               Get information about a resource\n");
        sb.append("  bin/dispatcher mcp/prompts/get --name <prompt-name> [--arguments <args>] Get a prompt template\n");
        sb.append("  bin/dispatcher mcp --version                                          Show version information\n");
        sb.append("  bin/dispatcher mcp --help                                             Show this help information\n\n");
        sb.append("Examples:\n");
        sb.append("  bin/dispatcher mcp/call --name calculator/add --arguments \"a:10,b:5\"\n");
        sb.append("  bin/dispatcher mcp/call --name calculator --arguments \"operation:add,a:1,b:2\"\n");
        sb.append("  bin/dispatcher mcp/prompts/get --name greeting --arguments \"name:John\"\n");
        return sb.toString();
    }
}
