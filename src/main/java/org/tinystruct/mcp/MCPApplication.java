package org.tinystruct.mcp;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.http.ResponseStatus;
import org.tinystruct.http.SSEPushManager;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.tinystruct.mcp.MCPSpecification.*;

/**
 * MCP (Model Context Protocol) Application Handler
 * Implements the MCP protocol for machine control and monitoring.
 * Designed to work with both Tomcat and Netty HTTP servers.
 */
public abstract class MCPApplication extends AbstractApplication {
    private static final Logger LOGGER = Logger.getLogger(MCPApplication.class.getName());
    
    protected SSEHandler sseHandler;
    protected JsonRpcHandler jsonRpcHandler;
    protected AuthorizationHandler authHandler;
    
    private boolean initialized = false;
    protected SessionState sessionState = SessionState.DISCONNECTED;
    private final String sessionId = UUID.randomUUID().toString();

    // Generic registries for tools, resources, prompts, and custom RPC handlers
    protected final Map<String, MCPTool> tools = new java.util.concurrent.ConcurrentHashMap<>();
    protected final Map<String, MCPDataResource> resources = new java.util.concurrent.ConcurrentHashMap<>();
    protected final Map<String, MCPPrompt> prompts = new java.util.concurrent.ConcurrentHashMap<>();
    protected final Map<String, RpcMethodHandler> rpcHandlers = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    /**
     * Initializes the MCP application, setting up authentication, SSE, JSON-RPC handler,
     * and registering core protocol handlers. Subclasses should call super.init() and
     * may register additional handlers for custom protocol extensions.
     */
    public void init() {
        this.setTemplateRequired(false);
        // Generate a random auth token if not configured
        if (getConfiguration().get(Config.AUTH_TOKEN) == null) {
            String token = AuthorizationHandler.generateAuthToken();
            getConfiguration().set(Config.AUTH_TOKEN, token);
            LOGGER.info("Generated new MCP auth token: " + token);
        }
        this.authHandler = new AuthorizationHandler(getConfiguration().get(Config.AUTH_TOKEN));
        this.sseHandler = new SSEHandler();
        this.jsonRpcHandler = new JsonRpcHandler();

        // Register core protocol handlers
        this.registerRpcHandler(Methods.INITIALIZE, (req, res, app) -> app.handleInitialize(req, res));
        this.registerRpcHandler(Methods.LIST_TOOLS, (req, res, app) -> app.handleListTools(req, res));
        this.registerRpcHandler(Methods.CALL_TOOL, (req, res, app) -> app.handleCallTool(req, res));
        this.registerRpcHandler(Methods.LIST_RESOURCES, (req, res, app) -> app.handleListResources(req, res));
        this.registerRpcHandler(Methods.READ_RESOURCE, (req, res, app) -> app.handleReadResource(req, res));
        this.registerRpcHandler(Methods.LIST_PROMPTS, (req, res, app) -> app.handleListPrompts(req, res));
        this.registerRpcHandler(Methods.GET_PROMPT, (req, res, app) -> app.handleGetPrompt(req, res));
        this.registerRpcHandler(Methods.GET_CAPABILITIES, (req, res, app) -> app.handleGetCapabilities(req, res));
        this.registerRpcHandler(Methods.SHUTDOWN, (req, res, app) -> app.handleShutdown(req, res));
        this.registerRpcHandler(Methods.GET_STATUS, (req, res, app) -> app.handleGetStatus(req, res));
        this.registerRpcHandler(Methods.INITIALIZED_NOTIFICATION, (req, res, app) -> {
            if (app.sessionState == SessionState.INITIALIZING) {
                app.sessionState = SessionState.READY;
                res.setResult(null);
            } else {
                res.setError(new JsonRpcError(ErrorCodes.INVALID_REQUEST, "Not in initializing state"));
            }
        });
    }

    /**
     * Checks if a method is allowed before initialization is complete.
     * Only 'initialize' and 'ping' are allowed before the session is READY.
     *
     * @param method The JSON-RPC method name
     * @return true if allowed, false otherwise
     */
    private boolean isAllowedPreInitialization(String method) {
        return Methods.INITIALIZE.equals(method) || "ping".equals(method);
    }

    /**
     * Main entry point for handling JSON-RPC requests.
     * Authenticates, parses, dispatches, and returns the response.
     *
     * @param request  The HTTP request
     * @param response The HTTP response
     * @return The JSON-RPC response as a string
     * @throws ApplicationException if an error occurs
     */
    @Action(Endpoints.SSE)
    public String handleRpcRequest(Request request, Response response) throws ApplicationException {
        try {
            // If HTTP method is GET, return initialize handler response
            if (request.method().name().equalsIgnoreCase("GET")) {
                JsonRpcRequest initReq = new JsonRpcRequest();
                initReq.setId("1");
                JsonRpcResponse initRes = new JsonRpcResponse();
                handleInitialize(initReq, initRes);

                return initRes.toString();
            }

            // Validate authentication
            authHandler.validateAuthHeader(request);
            
            // Parse the JSON-RPC request
            String jsonStr = request.body();
            LOGGER.fine("Received JSON: " + jsonStr);
            
            // Add batch request support
            if (jsonStr.trim().startsWith("[")) {
                return jsonRpcHandler.handleBatchRequest(jsonStr, (rpcReq, rpcRes) -> {
                    RpcMethodHandler handler = rpcHandlers.get(rpcReq.getMethod());
                    if (handler != null) {
                        handler.handle(rpcReq, rpcRes, this);
                    } else {
                        rpcRes.setError(new JsonRpcError(ErrorCodes.METHOD_NOT_FOUND, "Method not found: " + rpcReq.getMethod()));
                    }
                });
            }
            
            if (!jsonRpcHandler.validateJsonRpcRequest(jsonStr)) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return jsonRpcHandler.createErrorResponse("Invalid JSON-RPC request", ErrorCodes.INVALID_REQUEST);
            }
            
            JsonRpcRequest rpcRequest = new JsonRpcRequest();
            rpcRequest.parse(jsonStr);
            
            JsonRpcResponse jsonResponse = new JsonRpcResponse();
            // Restrict methods before READY
            String method = rpcRequest.getMethod();
            if (sessionState != SessionState.READY && !isAllowedPreInitialization(method)) {
                jsonResponse.setError(new JsonRpcError(ErrorCodes.NOT_INITIALIZED, "Server not initialized. Only 'initialize' and 'ping' allowed."));
            } else {
                RpcMethodHandler handler = rpcHandlers.get(method);
                if (handler != null) {
                    handler.handle(rpcRequest, jsonResponse, this);
                } else {
                    jsonResponse.setError(new JsonRpcError(ErrorCodes.METHOD_NOT_FOUND, "Method not found: " + method));
                }
            }
            String sessionId = request.getSession().getId();
            String rpcResponse = jsonResponse.toString();
            Builder message = new Builder();
            message.parse(rpcResponse);
            SSEPushManager.getInstance().push(sessionId,message);

            return rpcResponse;
        } catch (SecurityException e) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return jsonRpcHandler.createErrorResponse("Unauthorized", ErrorCodes.UNAUTHORIZED);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "RPC request failed", e);
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return jsonRpcHandler.createErrorResponse("Internal server error: " + e.getMessage(), ErrorCodes.INTERNAL_ERROR);
        }
    }
    
    /**
     * Handles the 'initialize' JSON-RPC method.
     * Subclasses may override to customize initialization behavior.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response to populate
     */
    protected void handleInitialize(JsonRpcRequest request, JsonRpcResponse response) {
        // Parse protocolVersion from request if present, else use MCP spec version
        String protocolVersion = MCPSpecification.PROTOCOL_VERSION; // e.g., "2025-06-18"

        // Build capabilities object as required by MCP
        Builder capabilities = new Builder();
        for (String feature : getFeatures()) {
            capabilities.put(feature, new Builder());
        }

        // Build serverInfo with only name and version
        Builder serverInfo = new Builder()
            .put("name", "TinyStructMCP")
            .put("version", version());

        // Build result object
        Builder result = new Builder()
            .put("protocolVersion", protocolVersion)
            .put("capabilities", capabilities)
            .put("serverInfo", serverInfo);

        response.setId(request.getId());
        response.setResult(result);
    }
    
    /**
     * Handles the 'get-capabilities' JSON-RPC method.
     * Returns the protocol version, protocol ID, and supported features.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response to populate
     */
    protected void handleGetCapabilities(JsonRpcRequest request, JsonRpcResponse response) {
        Builder result = new Builder();
        result.put("version", version());
        result.put("protocol", PROTOCOL_ID);

        Builders features = new Builders();
        for (String feature : getFeatures()) {
            features.add(new Builder(feature));
        }
        result.put("features", features);
        
        response.setId(request.getId());
        response.setResult(result);
    }
    
    /**
     * Handles the 'shutdown' JSON-RPC method.
     * Closes all SSE connections and sets the session state to DISCONNECTED.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response to populate
     */
    protected void handleShutdown(JsonRpcRequest request, JsonRpcResponse response) {
        if (sessionState != SessionState.READY) {
            response.setError(new JsonRpcError(ErrorCodes.NOT_INITIALIZED, "Not in ready state"));
            return;
        }

        sseHandler.closeAll();
        sessionState = SessionState.DISCONNECTED;
        
        response.setId(request.getId());
        response.setResult(new Builder().put("status", "shutdown_complete"));
    }
    
    /**
     * Handles the 'get-status' JSON-RPC method.
     * Returns the current session state, session ID, and uptime.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response to populate
     */
    protected void handleGetStatus(JsonRpcRequest request, JsonRpcResponse response) {
        Builder result = new Builder();
        result.put("state", sessionState.toString());
        result.put("sessionId", sessionId);
        result.put("uptime", System.currentTimeMillis() - sseHandler.getFirstSessionCreatedAt());
        
        response.setId(request.getId());
        response.setResult(result);
    }

    @Override
    /**
     * Returns the protocol version implemented by this application.
     *
     * @return The protocol version string
     */
    public String version() {
        return PROTOCOL_VERSION;
    }

    /**
     * Returns the features/capabilities supported by this application.
     * Subclasses can override to provide dynamic features.
     *
     * @return An array of feature names
     */
    protected String[] getFeatures() {
        return Features.CORE_FEATURES;
    }

    /**
     * Registers a tool with the application.
     * Generates a schema for the tool and adds it to the registry.
     *
     * @param tool The tool to register
     */
    public void registerTool(MCPTool tool) {
        Builder builder = SchemaGenerator.generateSchema(tool.getClass());
        tool.setSchema(builder);
        tools.put(tool.getName(), tool);
        LOGGER.info("Registered tool: " + tool.getName());
    }

    /**
     * Registers a resource with the application.
     * Adds the resource to the registry.
     *
     * @param resource The resource to register
     */
    public void registerResource(MCPDataResource resource) {
        resources.put(resource.getName(), resource);
        LOGGER.info("Registered resource: " + resource.getName());
    }

    /**
     * Registers a prompt with the application.
     * Adds the prompt to the registry.
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
     * @param method  The JSON-RPC method name
     * @param handler The handler implementation
     */
    public void registerRpcHandler(String method, RpcMethodHandler handler) {
        rpcHandlers.put(method, handler);
        LOGGER.info("Registered RPC handler: " + method);
    }

    /**
     * Handles the 'list-tools' JSON-RPC method.
     * Subclasses should override to provide tool listing functionality.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response to populate
     */
    abstract void handleListTools(JsonRpcRequest request, JsonRpcResponse response);

    /**
     * Handles the 'call-tool' JSON-RPC method.
     * Subclasses should override to provide tool invocation functionality.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response to populate
     */
    abstract void handleCallTool(JsonRpcRequest request, JsonRpcResponse response);

    /**
     * Handles the 'list-resources' JSON-RPC method.
     * Subclasses should override to provide resource listing functionality.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response to populate
     */
    abstract void handleListResources(JsonRpcRequest request, JsonRpcResponse response);

    /**
     * Handles the 'read-resource' JSON-RPC method.
     * Subclasses should override to provide resource reading functionality.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response to populate
     */
    abstract void handleReadResource(JsonRpcRequest request, JsonRpcResponse response);

    /**
     * Handles the 'list-prompts' JSON-RPC method.
     * Subclasses should override to provide prompt listing functionality.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response to populate
     */
    abstract void handleListPrompts(JsonRpcRequest request, JsonRpcResponse response);
    
    /**
     * Handles the 'get-prompt' JSON-RPC method.
     * Subclasses should override to provide prompt retrieval functionality.
     *
     * @param request  The JSON-RPC request
     * @param response The JSON-RPC response to populate
     */
    abstract void handleGetPrompt(JsonRpcRequest request, JsonRpcResponse response);
}