package org.tinystruct.mcp;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.http.Header;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.http.ResponseStatus;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MCP (Model Context Protocol) Application Handler
 * Implements the MCP protocol for machine control and monitoring.
 * Designed to work with both Tomcat and Netty HTTP servers.
 */
public class MCPApplication extends AbstractApplication {
    private static final Logger LOGGER = Logger.getLogger(MCPApplication.class.getName());
    private final ConcurrentHashMap<String, SSESession> sseClients = new ConcurrentHashMap<>();
    
    // Base MCP constants
    private static final String MCP_PROTOCOL_VERSION = "1.0.0";
    private static final String[] SUPPORTED_FEATURES = {
        "base", "lifecycle", "resources", "sse", "json-rpc"
    };
    private boolean initialized = false;

    @Override
    public void init() {
        this.setTemplateRequired(false);
        // Generate a random auth token if not configured
        if (getConfiguration().get("mcp.auth.token") == null) {
            String token = UUID.randomUUID().toString();
            getConfiguration().set("mcp.auth.token", token);
            LOGGER.info("Generated new MCP auth token: " + token);
        }
    }

    /**
     * Initializes a new MCP session
     * @param request HTTP request containing client authentication
     * @param response HTTP response to set headers
     * @return JSON-RPC response with session details
     */
    @Action("mcp/initialize")
    public String initialize(Request request, Response response) throws ApplicationException {
        try {
            String clientId = validateAuthHeader(request);
            JsonRpcResponse jsonResponse = MCPLifecycle.createInitializeResponse(
                    clientId,
                    SUPPORTED_FEATURES
            );
            
            initialized = true;
            response.addHeader("Content-Type", "application/json");
            return jsonResponse.toString();
        } catch (SecurityException e) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return createErrorResponse("Unauthorized", -32001);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Initialize failed", e);
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return createErrorResponse("Internal server error", -32000);
        }
    }

    /**
     * Returns server capabilities
     */
    @Action("mcp/capabilities")
    public String capabilities(Response response) throws ApplicationException {
        try {
            Builder result = new Builder();
            result.put("version", version());
            result.put("protocol", "MCP/1.0");

            Builders features = new Builders();
            for (String feature : SUPPORTED_FEATURES) {
                features.add(new Builder(feature));
            }
            result.put("features", features);

            JsonRpcResponse jsonResponse = new JsonRpcResponse();
            jsonResponse.setId(UUID.randomUUID().toString());
            jsonResponse.setResult(result);

            response.addHeader("Content-Type", "application/json");
            return jsonResponse.toString();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Capabilities request failed", e);
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return createErrorResponse("Internal server error", -32000);
        }
    }

    /**
     * Gracefully shuts down the MCP session
     */
    @Action("mcp/shutdown")
    public String shutdown(Response response) throws ApplicationException {
        JsonRpcResponse jsonResponse = MCPLifecycle.createShutdownResponse();
        response.addHeader("Content-Type", "application/json");
        return jsonResponse.toString();
    }

    /**
     * Establishes Server-Sent Events connection
     */
    @Action(value = "mcp/events", options = {
            @Argument(key = "token", description = "Authentication token", optional = false)
    })
    public String events(Response response, Request request) throws ApplicationException {
        try {
            String token = request.getParameter("token");
            if (!authenticate(token)) {
                response.setStatus(ResponseStatus.UNAUTHORIZED);
                return createErrorResponse("Invalid token", -32001);
            }

            String clientId = UUID.randomUUID().toString();
            SSESession session = new SSESession(clientId);
            sseClients.put(clientId, session);

            // Set SSE headers
            response.addHeader("Content-Type", "text/event-stream");
            response.addHeader("Cache-Control", "no-cache");
            response.addHeader("Connection", "keep-alive");

            // Send initial connection event
            session.sendEvent("connected", "{\"clientId\":\"" + clientId + "\"}");
            return session.getOutput();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "SSE connection failed", e);
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return createErrorResponse("Internal server error", -32000);
        }
    }
    
    /**
     * Handles JSON-RPC requests for basic MCP operations
     */
    @Action("mcp/rpc")
    public String handleRpcRequest(Request request, Response response) throws ApplicationException {
        try {
            // Validate authentication
            validateAuthHeader(request);
            
            // Parse the JSON-RPC request
            String jsonStr = request.body();
            LOGGER.fine("Received JSON: " + jsonStr);
            
            Builder jsonObject = new Builder();
            jsonObject.parse(jsonStr);
            
            JsonRpcResponse jsonResponse = new JsonRpcResponse();
            
            if (jsonObject.containsKey("method")) {
                JsonRpcRequest rpcRequest = new JsonRpcRequest();
                rpcRequest.parse(jsonStr);
                
                String method = rpcRequest.getMethod();
                switch (method) {
                    case MCPLifecycle.METHOD_INITIALIZE:
                        handleInitialize(rpcRequest, jsonResponse);
                        break;
                    // Add other base MCP methods here
                    default:
                        jsonResponse.setError(new JsonRpcError(-32601, "Method not found: " + method));
                }
            } else {
                jsonResponse.setError(new JsonRpcError(-32600, "Invalid Request"));
            }
            
            response.addHeader("Content-Type", "application/json");
            return jsonResponse.toString();
            
        } catch (SecurityException e) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return createErrorResponse("Unauthorized", -32001);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "RPC request failed", e);
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return createErrorResponse("Internal server error: " + e.getMessage(), -32000);
        }
    }
    
    protected void handleInitialize(JsonRpcRequest request, JsonRpcResponse response) {
        if (!initialized) {
            JsonRpcResponse initResponse = MCPLifecycle.createInitializeResponse(
                UUID.randomUUID().toString(),
                SUPPORTED_FEATURES
            );
            response.setId(request.getId());
            response.setResult(initResponse.getResult());
            initialized = true;
        } else {
            response.setError(new JsonRpcError(-32600, "Server already initialized"));
        }
    }

    protected String validateAuthHeader(Request request) throws SecurityException {
        String auth = request.headers().get(Header.AUTHORIZATION).toString();
        if (auth == null || !authenticate(auth)) {
            throw new SecurityException("Invalid authorization");
        }
        return UUID.randomUUID().toString();
    }

    protected boolean authenticate(String token) {
        String validToken = getConfiguration().get("mcp.auth.token");
        return validToken != null && validToken.equals(token);
    }

    protected String createErrorResponse(String message, int code) {
        JsonRpcResponse errorResponse = new JsonRpcResponse();
        errorResponse.setId(UUID.randomUUID().toString());
        JsonRpcError error = new JsonRpcError(code, message);
        errorResponse.setError(error);
        return errorResponse.toString();
    }

    @Override
    public String version() {
        return MCP_PROTOCOL_VERSION;
    }

    /**
     * Represents a Server-Sent Events session
     */
    private static class SSESession {
        private final String id;
        private final StringBuilder output;

        public SSESession(String id) {
            this.id = id;
            this.output = new StringBuilder();
        }

        public String getOutput() {
            return output.toString();
        }

        public void sendEvent(String event, String data) {
            output.append("event: ").append(event).append("\n");
            output.append("data: ").append(data).append("\n\n");
        }
    }
}