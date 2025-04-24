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
    @Action(value = "mcp/connect",
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
    @Action(value = "mcp/disconnect",
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
    @Action(value = "mcp/list",
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
    @Action(value = "mcp/list",
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
    @Action(value = "mcp/execute",
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
    @Action(value = "mcp/info",
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
        sb.append("  bin/dispatcher mcp/list/<type>                                        List resources of a specific type\n");
        sb.append("  bin/dispatcher mcp/execute --name <resource-name> [--params <json>]   Execute a resource\n");
        sb.append("  bin/dispatcher mcp/info/<resource-name>                               Get information about a resource\n");
        sb.append("  bin/dispatcher mcp --version                                          Show version information\n");
        sb.append("  bin/dispatcher mcp --help                                             Show this help information\n");
        return sb.toString();
    }
}
