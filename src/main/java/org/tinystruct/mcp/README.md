# Model Context Protocol (MCP) Module for Tinystruct

This module provides comprehensive integration with the Model Context Protocol (MCP) for the Tinystruct framework. It enables AI model interactions, tool discovery and execution, resource management, and prompt handling through a unified interface.

## Overview

The MCP module implements the Model Context Protocol specification, providing both client and server capabilities for AI model interactions. It supports JSON-RPC communication, Server-Sent Events (SSE), authentication, and a unified resource model.

## Components

The MCP module consists of the following core components:

### Core Classes
1. **MCPApplication**: Abstract base class for MCP applications with JSON-RPC handling
2. **MCPServerApplication**: Complete MCP server implementation with tool and resource management
3. **MCPClient**: Full-featured client for connecting to MCP servers
4. **MCPClientApplication**: Tinystruct application for CLI-based MCP interactions

### Resource Management
5. **MCPResource**: Unified interface for all MCP resources (tools, data sources, prompts)
6. **MCPTool**: Implementation for executable tools with method registration
7. **MCPDataResource**: Implementation for data resources
8. **MCPPrompt**: Implementation for prompt templates with parameter substitution

### Protocol Support
9. **MCPSpecification**: Protocol constants, methods, and error codes
10. **JsonRpcHandler**: JSON-RPC 2.0 protocol implementation
11. **AuthorizationHandler**: JWT-based authentication
12. **MCPPushManager**: Server-Sent Events management for real-time updates

### Supporting Classes
13. **MCPException**: Custom exception handling for MCP operations
14. **AbstractMCPResource**: Base implementation for MCP resources
15. **MCPLifecycle**: Lifecycle management utilities

## MCP Server Implementation

### Creating an MCP Server

```java
import org.tinystruct.mcp.MCPServerApplication;
import org.tinystruct.mcp.tools.CalculatorTool;

public class MyMCPServer extends MCPServerApplication {
    
    @Override
    public void init() {
        super.init();
        
        // Register tools
        CalculatorTool calculator = new CalculatorTool();
        this.registerToolMethods(calculator);
        
        // Register custom tools
        this.registerTool(new MyCustomTool());
        
        // Register prompts
        MCPPrompt greetingPrompt = new MCPPrompt(
            "greeting",
            "A greeting prompt",
            "Hello, {{name}}!",
            createPromptSchema(),
            null
        ) {
            @Override
            protected boolean supportsLocalExecution() {
                return true;
            }
        };
        this.registerPrompt(greetingPrompt);
    }
}
```

### Starting the Server

```bash
# Start with Netty HTTP Server
bin/dispatcher start --import org.tinystruct.mcp.examples.SampleMCPServerApplication

# Or programmatically
MCPServerApplication server = new MCPServerApplication();
ApplicationManager.install(server, settings);
ApplicationManager.call("mcp-server/start", context);
```

### Supported MCP Methods

The server implements the following MCP methods:

- **initialize**: Establish connection and return server capabilities
- **tools/list**: List available tools with schemas
- **tools/call**: Execute tools with parameters
- **resources/list**: List available data resources
- **resources/read**: Read data from resources
- **prompts/list**: List available prompt templates
- **prompts/get**: Retrieve prompt templates
- **get-capabilities**: Get server capabilities
- **get-status**: Get server status
- **shutdown**: Gracefully shutdown the server

## MCP Client Implementation

### Command Line Interface

The MCP module provides a comprehensive CLI through `MCPClientApplication`:

```bash
# Show help information
bin/dispatcher mcp --help

# Connect to an MCP server
bin/dispatcher mcp/connect --url http://localhost:8080 --token your-auth-token

# List all available resources
bin/dispatcher mcp/list

# List resources of a specific type
bin/dispatcher mcp/list/TOOL
bin/dispatcher mcp/list/DATA
bin/dispatcher mcp/list/PROMPT

# Execute a tool
bin/dispatcher mcp/execute --name calculator/add --params "a:10,b:5"

# Get information about a resource
bin/dispatcher mcp/info/calculator

# Disconnect from the MCP server
bin/dispatcher mcp/disconnect
```

### Programmatic Usage

```java
import org.tinystruct.mcp.MCPClient;
import org.tinystruct.mcp.MCPResource;
import org.tinystruct.mcp.MCPException;

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
    parameters.put("a", 10);
    parameters.put("b", 5);
    Object result = client.executeResource("calculator/add", parameters);
    System.out.println("Result: " + result);
    
} catch (MCPException e) {
    e.printStackTrace();
} finally {
    client.disconnect();
}
```

### Using with ApplicationManager

```java
import org.tinystruct.ApplicationException;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Settings;

// Register the MCPClientApplication
Settings settings = new Settings();
ApplicationManager.install("org.tinystruct.mcp.MCPClientApplication", settings);

// Connect to an MCP server
ApplicationManager.call("mcp/connect", new Object[] {"http://localhost:8080", "your-auth-token"});

// List available resources
String resources = (String) ApplicationManager.call("mcp/list", null);
System.out.println(resources);

// Execute a resource
String result = (String) ApplicationManager.call("mcp/execute", new Object[] {"calculator/add", "a:10,b:5"});
System.out.println(result);
```

## Creating Custom Tools

### Simple Tool Implementation

```java
import org.tinystruct.mcp.MCPTool;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

public class MyCustomTool extends MCPTool {
    
    public MyCustomTool() {
        super("my-tool", "A custom tool", null, null, true);
    }
    
    @Action(value = "my-tool/process", 
            description = "Process data", 
            arguments = {
                @Argument(key = "input", description = "Input data", type = "string")
            })
    public String processData(String input) {
        return "Processed: " + input.toUpperCase();
    }
}
```

### Advanced Tool with Schema

```java
import org.tinystruct.data.component.Builder;
import org.tinystruct.mcp.MCPTool;
import org.tinystruct.mcp.MCPException;

public class AdvancedTool extends MCPTool {
    
    public AdvancedTool() {
        super("advanced", "Advanced processing tool", createSchema(), null, true);
    }
    
    private static Builder createSchema() {
        Builder schema = new Builder();
        Builder properties = new Builder();
        
        Builder inputParam = new Builder();
        inputParam.put("type", "string");
        inputParam.put("description", "Input data to process");
        
        properties.put("input", inputParam);
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new String[]{"input"});
        
        return schema;
    }
    
    @Override
    protected Object executeLocally(Builder builder) throws MCPException {
        String input = builder.get("input").toString();
        // Custom processing logic
        return "Advanced processing result: " + input;
    }
}
```

## Creating Custom Prompts

```java
import org.tinystruct.mcp.MCPPrompt;
import org.tinystruct.data.component.Builder;

public class CustomPrompt extends MCPPrompt {
    
    public CustomPrompt() {
        super(
            "custom-prompt",
            "A custom prompt template",
            "Hello {{name}}, your score is {{score}}!",
            createPromptSchema(),
            null
        );
    }
    
    private static Builder createPromptSchema() {
        Builder schema = new Builder();
        Builder properties = new Builder();
        
        Builder nameParam = new Builder();
        nameParam.put("type", "string");
        nameParam.put("description", "User's name");
        
        Builder scoreParam = new Builder();
        scoreParam.put("type", "number");
        scoreParam.put("description", "User's score");
        
        properties.put("name", nameParam);
        properties.put("score", scoreParam);
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new String[]{"name", "score"});
        
        return schema;
    }
    
    @Override
    protected boolean supportsLocalExecution() {
        return true;
    }
    
    @Override
    protected Object executeLocally(Builder builder) throws MCPException {
        String name = builder.get("name").toString();
        String score = builder.get("score").toString();
        return template.replace("{{name}}", name).replace("{{score}}", score);
    }
}
```

## Server-Sent Events (SSE) Support

The MCP module includes built-in SSE support for real-time updates:

```java
// Server-side: Push events to clients
MCPPushManager.getInstance().push(sessionId, eventData);

// Client-side: Receive SSE events
// The MCPClient automatically handles SSE connections
client.connect(); // Establishes both JSON-RPC and SSE connections
```

## Authentication and Security

### JWT Authentication

The MCP module supports JWT-based authentication:

```java
// Server configuration
getConfiguration().set("mcp.auth.token", "your-secret-token");

// Client connection with token
MCPClient client = new MCPClient("http://localhost:8080", "your-secret-token");
```

### Session Management

```java
// Automatic session management
String sessionId = request.getSession().getId();
sessionMap.put(sessionId, System.currentTimeMillis());
```

## Error Handling

The MCP module provides comprehensive error handling:

```java
try {
    Object result = client.executeResource("tool-name", parameters);
} catch (MCPException e) {
    // Handle MCP-specific errors
    System.err.println("MCP Error: " + e.getMessage());
} catch (IOException e) {
    // Handle connection errors
    System.err.println("Connection Error: " + e.getMessage());
}
```

## Protocol Features

### JSON-RPC 2.0 Support
- Full JSON-RPC 2.0 protocol implementation
- Batch request support
- Error handling with standard error codes
- Method validation and routing

### Resource Types
- **TOOL**: Executable functions with parameters
- **DATA**: Data sources and repositories
- **PROMPT**: Template-based text generation
- **TEMPLATE**: URI templates for resource access

### Capabilities
- **logging**: Log level management
- **prompts**: Prompt template support with list change notifications
- **resources**: Resource subscription and list change notifications
- **tools**: Tool discovery with list change notifications

## Examples

### Complete Server Example

```java
import org.tinystruct.mcp.MCPServerApplication;
import org.tinystruct.mcp.tools.CalculatorTool;

public class CompleteMCPServer extends MCPServerApplication {
    
    @Override
    public void init() {
        super.init();
        
        // Register calculator tool
        CalculatorTool calculator = new CalculatorTool();
        this.registerToolMethods(calculator);
        
        // Register custom tools
        this.registerTool(new FileProcessorTool());
        this.registerTool(new DataAnalysisTool());
        
        // Register prompts
        this.registerPrompt(new GreetingPrompt());
        this.registerPrompt(new ReportPrompt());
        
        // Register data resources
        this.registerResource(new DatabaseResource());
        this.registerResource(new FileSystemResource());
    }
}
```

### Complete Client Example

```java
import org.tinystruct.mcp.MCPClient;
import java.util.HashMap;
import java.util.Map;

public class MCPClientExample {
    
    public static void main(String[] args) {
        MCPClient client = new MCPClient("http://localhost:8080", "auth-token");
        
        try {
            // Connect to server
            client.connect();
            System.out.println("Connected to MCP server");
            
            // List available tools
            List<MCPResource> tools = client.listResources();
            System.out.println("Available tools:");
            for (MCPResource tool : tools) {
                System.out.println("- " + tool.getName() + ": " + tool.getDescription());
            }
            
            // Execute calculator tool
            Map<String, Object> params = new HashMap<>();
            params.put("a", 15);
            params.put("b", 3);
            Object result = client.executeResource("calculator/add", params);
            System.out.println("15 + 3 = " + result);
            
            // Execute custom tool
            Map<String, Object> fileParams = new HashMap<>();
            fileParams.put("filename", "data.txt");
            Object fileResult = client.executeResource("file-processor/read", fileParams);
            System.out.println("File content: " + fileResult);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            client.disconnect();
        }
    }
}
```

## Configuration

### Server Configuration

```properties
# MCP Server Configuration
mcp.auth.token=your-secret-token
mcp.server.port=8080
mcp.server.host=localhost
mcp.sse.enabled=true
mcp.jsonrpc.enabled=true
```

### Client Configuration

```properties
# MCP Client Configuration
mcp.client.timeout=30000
mcp.client.retry.attempts=3
mcp.client.retry.delay=1000
mcp.sse.reconnect.enabled=true
```

## Extending the MCP Module

### Creating Custom Resource Types

```java
public class CustomResource extends AbstractMCPResource {
    
    public CustomResource(String name, String description) {
        super(name, description, ResourceType.CUSTOM);
    }
    
    @Override
    public Object execute(Builder builder) throws MCPException {
        // Custom execution logic
        return "Custom resource executed";
    }
}
```

### Adding Custom RPC Handlers

```java
// Register custom RPC method handler
this.registerRpcHandler("custom/method", (req, res, app) -> {
    // Custom method implementation
    res.setResult("Custom method result");
});
```

## License

This module is licensed under the Apache License 2.0. See the LICENSE file for details.
