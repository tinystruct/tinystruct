# SSE Libraries in org.tinystruct.mcp Package

## Overview

The MCP package provides enhanced Server-Sent Events (SSE) functionality through two main components:

1. **SSEHandler** - Enhanced basic SSE management with validation, monitoring, and rate limiting
2. **SSEManager** - Advanced SSE management with connection pooling, event filtering, and replay capabilities

## SSEHandler

### Features

- **Connection Monitoring**: Tracks client connections with timestamps and activity monitoring
- **Event Validation**: Validates event types against MCP specification
- **Rate Limiting**: Prevents message flooding with configurable rate limits
- **Error Handling**: Comprehensive error handling with detailed logging
- **Metrics Collection**: Tracks connection statistics and message counts
- **Automatic Cleanup**: Removes inactive connections based on timeout

### Usage

```java
// Basic usage
SSEHandler handler = new SSEHandler();

// Register a client
boolean success = handler.registerClient("client123", response);
if (success) {
    // Send events
    handler.sendMCPEvent("client123", Events.CONNECTED, "{\"status\":\"ready\"}");
    handler.sendNotification("client123", "Welcome to MCP!");
}

// Broadcast to all clients
int recipients = handler.broadcast(message);

// Get statistics
Builder stats = handler.getConnectionStats();
```

### Configuration

```java
// Custom rate limiting
SSEHandler handler = new SSEHandler(30000L, 500); // 30s window, 500 messages max

// Setup SSE headers
handler.setupSSEHeaders(response);
```

### Methods

- `registerClient(String clientId, Response response)` - Register new client
- `broadcast(Builder message)` - Broadcast to all clients
- `pushToClient(String clientId, Builder message)` - Send to specific client
- `sendMCPEvent(String clientId, String eventType, String data)` - Send MCP event
- `sendConnectedEvent(String clientId, String data)` - Send connection event
- `sendErrorEvent(String clientId, String errorMessage, String errorCode)` - Send error event
- `sendNotification(String clientId, String notification)` - Send notification
- `closeClient(String clientId)` - Close specific client
- `closeAll()` - Close all clients
- `getConnectionStats()` - Get connection statistics
- `cleanupInactiveConnections()` - Clean up inactive connections

## SSEManager

### Features

- **Connection Pooling**: Manages clients in different pools for load balancing
- **Event Filtering**: Filters events using predicates before sending
- **Event Replay**: Replays recent events to new clients
- **Enhanced Monitoring**: Detailed metrics and health checks
- **Automatic Health Checks**: Periodic connection health monitoring
- **Event History**: Stores events for replay functionality

### Usage

```java
// Basic usage
SSEManager manager = new SSEManager();

// Register client in specific pool
manager.registerClient("client123", response, "admin-pool");

// Add event filter
manager.addEventFilter("admin-only", event -> 
    event.containsKey("admin") && (Boolean) event.get("admin"));

// Broadcast with filtering
int recipients = manager.broadcastEvent(event, "admin-only");

// Replay events to new client
int replayed = manager.replayEvents("client123", "notification", 10);

// Get pool statistics
Builder stats = manager.getPoolStatistics();
```

### Connection Pools

```java
// Register clients in different pools
manager.registerClient("admin1", response, "admin-pool");
manager.registerClient("user1", response, "user-pool");
manager.registerClient("guest1", response, "guest-pool");

// Each pool has independent connection limits and monitoring
```

### Event Filtering

```java
// Filter by event type
manager.addEventFilter("errors-only", event -> 
    "error".equals(event.get("event")));

// Filter by content
manager.addEventFilter("high-priority", event -> 
    event.containsKey("priority") && "high".equals(event.get("priority")));

// Use filter in broadcast
manager.broadcastEvent(event, "high-priority");
```

### Event Replay

```java
// Replay recent events of specific type
manager.replayEvents("client123", "notification", 5);

// Replay all recent events
manager.replayEvents("client123", null, 10);
```

### Methods

- `registerClient(String clientId, Response response, String poolName)` - Register in pool
- `broadcastEvent(Builder event, String filterName)` - Broadcast with filtering
- `sendEventToClient(String clientId, Builder event)` - Send to specific client
- `sendMCPEvent(String clientId, String eventType, String data)` - Send MCP event
- `broadcastMCPEvent(String eventType, String data)` - Broadcast MCP event
- `replayEvents(String clientId, String eventType, int maxEvents)` - Replay events
- `addEventFilter(String filterName, Predicate<Builder> filter)` - Add filter
- `removeEventFilter(String filterName)` - Remove filter
- `getPoolStatistics()` - Get pool statistics
- `closeClient(String clientId)` - Close client
- `shutdown()` - Shutdown manager

## MCP Event Types

The libraries support standard MCP event types defined in `MCPSpecification.Events`:

- `CONNECTED` - Connection established
- `STATE` - Server state change
- `ERROR` - Error event
- `CLOSE` - Connection close
- `NOTIFICATION` - General notification
- `RESOURCES_CHANGED` - Resources changed
- `TOOLS_CHANGED` - Tools changed
- `PROMPTS_CHANGED` - Prompts changed

## Integration with MCP Applications

### In MCPApplication

```java
public class MCPApplication extends AbstractApplication {
    protected SSEHandler sseHandler;
    
    @Override
    public void init() {
        this.sseHandler = new SSEHandler();
    }
    
    @Action(Endpoints.EVENTS)
    public String events(Response response, Request request) throws ApplicationException {
        String clientId = UUID.randomUUID().toString();
        if (sseHandler.registerClient(clientId, response)) {
            sseHandler.setupSSEHeaders(response);
            sseHandler.sendConnectedEvent(clientId, "{\"clientId\":\"" + clientId + "\"}");
            return "";
        }
        throw new ApplicationException("Failed to register SSE client");
    }
}
```

### In MCPServerApplication

```java
public class MCPServerApplication extends MCPApplication {
    private SSEManager sseManager;
    
    @Override
    public void init() {
        super.init();
        this.sseManager = new SSEManager(sseHandler);
        
        // Add event filters
        sseManager.addEventFilter("admin-only", event -> 
            event.containsKey("admin") && (Boolean) event.get("admin"));
    }
    
    // Use sseManager for advanced features
    public void notifyAdmins(String message) {
        Builder event = new Builder();
        event.put("message", message);
        event.put("admin", true);
        event.put("timestamp", System.currentTimeMillis());
        
        sseManager.broadcastEvent(event, "admin-only");
    }
}
```

## Best Practices

### 1. Error Handling

```java
// Always check return values
boolean success = handler.registerClient(clientId, response);
if (!success) {
    // Handle registration failure
    LOGGER.warning("Failed to register client: " + clientId);
}

// Handle send failures
if (!handler.sendMCPEvent(clientId, Events.ERROR, errorData)) {
    // Handle send failure
    LOGGER.warning("Failed to send error event to client: " + clientId);
}
```

### 2. Resource Management

```java
// Always close connections properly
try {
    // Use SSE functionality
    handler.sendEvent(clientId, event);
} finally {
    handler.closeClient(clientId);
}

// Shutdown managers properly
sseManager.shutdown();
```

### 3. Rate Limiting

```java
// Configure appropriate rate limits
SSEHandler handler = new SSEHandler(60000L, 1000); // 1 minute, 1000 messages

// Monitor rate limit violations
if (!handler.pushToClient(clientId, message)) {
    // Check if rate limit exceeded
    LOGGER.warning("Rate limit exceeded for client: " + clientId);
}
```

### 4. Monitoring

```java
// Regular health checks
int inactiveConnections = handler.cleanupInactiveConnections();
if (inactiveConnections > 0) {
    LOGGER.info("Cleaned up " + inactiveConnections + " inactive connections");
}

// Monitor statistics
Builder stats = handler.getConnectionStats();
if (stats.get("activeConnections") != null) {
    int activeConnections = (Integer) stats.get("activeConnections");
    if (activeConnections > 1000) {
        LOGGER.warning("High number of active connections: " + activeConnections);
    }
}
```

### 5. Event Filtering

```java
// Use specific filters for different client types
sseManager.addEventFilter("user-events", event -> 
    !event.containsKey("admin") || !(Boolean) event.get("admin"));

sseManager.addEventFilter("system-events", event -> 
    event.containsKey("system") && (Boolean) event.get("system"));

// Broadcast to appropriate audiences
sseManager.broadcastEvent(userEvent, "user-events");
sseManager.broadcastEvent(systemEvent, "system-events");
```

## Performance Considerations

1. **Connection Limits**: Set appropriate connection limits per pool
2. **Event History Size**: Limit event history to prevent memory issues
3. **Rate Limiting**: Configure rate limits based on expected load
4. **Cleanup Intervals**: Adjust cleanup intervals based on connection patterns
5. **Health Check Frequency**: Balance monitoring overhead with responsiveness

## Security Considerations

1. **Client Validation**: Always validate client IDs and response objects
2. **Event Validation**: Validate event types against MCP specification
3. **Rate Limiting**: Prevent message flooding attacks
4. **Connection Timeouts**: Automatically close inactive connections
5. **Error Handling**: Don't expose sensitive information in error events

## Troubleshooting

### Common Issues

1. **Client Registration Fails**
   - Check if client ID is valid
   - Verify response object is not null
   - Check connection pool limits

2. **Events Not Received**
   - Verify client is still connected
   - Check rate limiting settings
   - Validate event format

3. **High Memory Usage**
   - Reduce event history size
   - Increase cleanup frequency
   - Monitor connection counts

4. **Performance Issues**
   - Adjust rate limiting settings
   - Use connection pooling
   - Implement event filtering

### Debugging

```java
// Enable fine logging
Logger.getLogger(SSEHandler.class.getName()).setLevel(Level.FINE);
Logger.getLogger(SSEManager.class.getName()).setLevel(Level.FINE);

// Check statistics
Builder stats = handler.getConnectionStats();
System.out.println("Connection stats: " + stats);

// Monitor specific client
ClientInfo clientInfo = handler.getClientInfo(clientId);
if (clientInfo != null) {
    System.out.println("Client activity: " + clientInfo.getLastActivity());
}
``` 