# Model Context Protocol (MCP) Module for Tinystruct

This module provides integration with the Model Context Protocol (MCP) for the Tinystruct framework. It allows you to interact with MCP servers, discover and execute tools, and access resources through a unified interface.

## Components

The MCP module consists of the following components:

1. **MCPClient**: Core client for connecting to MCP servers
2. **MCPResource**: Interface representing a resource (tools, data sources, etc.)
3. **MCPTool**: Implementation of MCPResource for tools
4. **MCPDataResource**: Implementation of MCPResource for data resources
5. **MCPClientApplication**: Tinystruct application for interacting with MCP servers via CLI

## Using the MCP Module

### Command Line Interface

The MCP module provides a command-line interface through the `MCPClientApplication` class. You can use it as follows:

```bash
# Show help information
bin/dispatcher mcp --help

# Connect to an MCP server
bin/dispatcher mcp/connect --url http://localhost:8080 --token your-auth-token

# List all available resources
bin/dispatcher mcp/list

# List resources of a specific type (TOOL, DATA, TEMPLATE, PROMPT)
bin/dispatcher mcp/list/TOOL

# Execute a resource
bin/dispatcher mcp/execute --name calculator --params "operation:add,a:1,b:2"

# Get information about a resource
bin/dispatcher mcp/info/calculator

# Disconnect from the MCP server
bin/dispatcher mcp/disconnect
```

### Programmatic Usage

You can also use the MCP module programmatically in your Java code:

```java
import org.tinystruct.ApplicationException;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Settings;
import org.tinystruct.mcp.MCPClient;
import org.tinystruct.mcp.MCPResource;

// Register the MCPClientApplication
Settings settings = new Settings();
ApplicationManager.install("org.tinystruct.mcp.MCPClientApplication", settings);

// Connect to an MCP server
ApplicationManager.call("mcp/connect", new Object[] {"http://localhost:8080", "your-auth-token"});

// List available resources
String resources = (String) ApplicationManager.call("mcp/list", null);
System.out.println(resources);

// Execute a resource
String result = (String) ApplicationManager.call("mcp/execute", new Object[] {"calculator", "operation:add,a:1,b:2"});
System.out.println(result);

// Disconnect from the MCP server
ApplicationManager.call("mcp/disconnect", null);
```

### Direct API Usage

You can also use the MCP client API directly:

```java
import org.tinystruct.mcp.MCPClient;
import org.tinystruct.mcp.MCPResource;
import org.tinystruct.mcp.MCPException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Create and connect the client
MCPClient client = new MCPClient("http://localhost:8080", "your-auth-token");
client.connect();

try {
    // List all resources
    List<MCPResource> resources = client.listResources();
    for (MCPResource resource : resources) {
        System.out.println(resource.getName() + ": " + resource.getDescription());
    }
    
    // Execute a tool
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("operation", "add");
    parameters.put("a", 1);
    parameters.put("b", 2);
    Object result = client.executeResource("calculator", parameters);
    System.out.println("Result: " + result);
    
} catch (MCPException e) {
    e.printStackTrace();
} finally {
    // Disconnect
    client.disconnect();
}
```

## Unified Resource Model

The MCP module uses a unified resource model where tools are treated as a type of resource. This provides several benefits:

1. **Conceptual Clarity**: Treats tools as a type of resource, which aligns with their nature
2. **Simplified Client Code**: Provides a unified interface for all server capabilities
3. **Polymorphic Execution**: Allows executing any resource regardless of its type
4. **Extensibility**: Makes it easy to add new resource types in the future
5. **Improved Discoverability**: All resources can be discovered through a single interface

## Extending the MCP Module

You can extend the MCP module by:

1. **Creating Custom Resource Types**: Implement the MCPResource interface for new resource types
2. **Adding New Actions**: Extend the MCPClientApplication class to add new actions
3. **Implementing Custom Transports**: Create new transport mechanisms for the MCPClient

## License

This module is licensed under the Apache License 2.0. See the LICENSE file for details.
