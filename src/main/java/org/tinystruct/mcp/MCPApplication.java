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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.tinystruct.mcp.MCPSpecification.*;

/**
 * MCP (Model Context Protocol) Application Handler
 * Implements the MCP protocol for machine control and monitoring.
 * Designed to work with both Tomcat and Netty HTTP servers.
 */
public class MCPApplication extends AbstractApplication {
    private static final Logger LOGGER = Logger.getLogger(MCPApplication.class.getName());
    private final ConcurrentHashMap<String, SSESession> sseClients = new ConcurrentHashMap<>();
    
    private boolean initialized = false;
    private SessionState sessionState = SessionState.DISCONNECTED;
    private final String sessionId = UUID.randomUUID().toString();

    @Override
    public void init() {
        this.setTemplateRequired(false);
        // Generate a random auth token if not configured
        if (getConfiguration().get(Config.AUTH_TOKEN) == null) {
            String token = UUID.randomUUID().toString();
            getConfiguration().set(Config.AUTH_TOKEN, token);
            LOGGER.info("Generated new MCP auth token: " + token);
        }
    }

    /**
     * Initializes a new MCP session
     * @param request HTTP request containing client authentication
     * @param response HTTP response to set headers
     * @return JSON-RPC response with session details
     */
    @Action(Endpoints.INITIALIZE)
    public String initialize(Request request, Response response) throws ApplicationException {
        try {
            if (sessionState != SessionState.DISCONNECTED) {
                throw new IllegalStateException("Session already initialized");
            }
            
            sessionState = SessionState.INITIALIZING;
            String clientId = validateAuthHeader(request);
            
            JsonRpcResponse jsonResponse = MCPLifecycle.createInitializeResponse(
                    clientId,
                    Features.CORE_FEATURES
            );
            
            sessionState = SessionState.READY;
            response.addHeader("Content-Type", Http.CONTENT_TYPE_JSON);
            return jsonResponse.toString();
        } catch (Exception e) {
            sessionState = SessionState.ERROR;
            throw e;
        }
    }

    /**
     * Returns server capabilities
     */
    @Action(Endpoints.CAPABILITIES)
    public String capabilities(Response response) throws ApplicationException {
        try {
            Builder result = new Builder();
            result.put("version", version());
            result.put("protocol", PROTOCOL_ID);

            Builders features = new Builders();
            for (String feature : Features.CORE_FEATURES) {
                features.add(new Builder(feature));
            }
            result.put("features", features);

            JsonRpcResponse jsonResponse = new JsonRpcResponse();
            jsonResponse.setId(UUID.randomUUID().toString());
            jsonResponse.setResult(result);

            response.addHeader("Content-Type", Http.CONTENT_TYPE_JSON);
            return jsonResponse.toString();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Capabilities request failed", e);
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return createErrorResponse("Internal server error", ErrorCodes.INTERNAL_ERROR);
        }
    }

    /**
     * Gracefully shuts down the MCP session
     */
    @Action(Endpoints.SHUTDOWN)
    public String shutdown(Response response) throws ApplicationException {
        try {
            if (sessionState != SessionState.READY) {
                return createErrorResponse("Not in ready state", ErrorCodes.NOT_INITIALIZED);
            }

            // Close all SSE connections
            for (SSESession session : sseClients.values()) {
                session.close();
            }
            sseClients.clear();

            sessionState = SessionState.DISCONNECTED;
            
            JsonRpcResponse jsonResponse = new JsonRpcResponse();
            jsonResponse.setId(UUID.randomUUID().toString());
            jsonResponse.setResult(new Builder().put("status", "shutdown_complete"));
            
            response.addHeader("Content-Type", Http.CONTENT_TYPE_JSON);
            return jsonResponse.toString();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Shutdown failed", e);
            sessionState = SessionState.ERROR;
            return createErrorResponse("Shutdown failed", ErrorCodes.INTERNAL_ERROR);
        }
    }

    /**
     * Establishes Server-Sent Events connection
     */
    @Action(value = Endpoints.EVENTS, options = {
            @Argument(key = Http.TOKEN_PARAM, description = "Authentication token", optional = false)
    })
    public String events(Response response, Request request) throws ApplicationException {
        try {
            String token = request.getParameter(Http.TOKEN_PARAM);
            if (!authenticate(token)) {
                response.setStatus(ResponseStatus.UNAUTHORIZED);
                return createErrorResponse("Invalid token", ErrorCodes.UNAUTHORIZED);
            }

            String clientId = UUID.randomUUID().toString();
            SSESession session = new SSESession(clientId);
            sseClients.put(clientId, session);

            // Set SSE headers
            response.addHeader("Content-Type", Http.CONTENT_TYPE_SSE);
            response.addHeader("Cache-Control", "no-cache");
            response.addHeader("Connection", "keep-alive");

            // Send initial connection event
            session.sendEvent(Events.CONNECTED, "{\"clientId\":\"" + clientId + "\"}");
            return session.getOutput();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "SSE connection failed", e);
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return createErrorResponse("Internal server error", ErrorCodes.INTERNAL_ERROR);
        }
    }
    
    /**
     * Handles JSON-RPC requests for basic MCP operations
     */
    @Action(Endpoints.RPC)
    public String handleRpcRequest(Request request, Response response) throws ApplicationException {
        try {
            // Validate authentication
            validateAuthHeader(request);
            
            // Parse the JSON-RPC request
            String jsonStr = request.body();
            LOGGER.fine("Received JSON: " + jsonStr);
            
            // Add batch request support
            if (jsonStr.trim().startsWith("[")) {
                return handleBatchRequest(jsonStr, response);
            }
            
            if (!validateJsonRpcRequest(jsonStr)) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return createErrorResponse("Invalid JSON-RPC request", ErrorCodes.INVALID_REQUEST);
            }
            
            Builder jsonObject = new Builder();
            jsonObject.parse(jsonStr);
            
            JsonRpcResponse jsonResponse = new JsonRpcResponse();
            
            if (jsonObject.containsKey(JsonRpc.METHOD_FIELD)) {
                JsonRpcRequest rpcRequest = new JsonRpcRequest();
                rpcRequest.parse(jsonStr);
                
                String method = rpcRequest.getMethod();
                switch (method) {
                    case Methods.INITIALIZE:
                        handleInitialize(rpcRequest, jsonResponse);
                        break;
                    case Methods.GET_CAPABILITIES:
                        handleGetCapabilities(rpcRequest, jsonResponse);
                        break;
                    case Methods.SHUTDOWN:
                        handleShutdown(rpcRequest, jsonResponse);
                        break;
                    case Methods.GET_STATUS:
                        handleGetStatus(rpcRequest, jsonResponse);
                        break;
                    default:
                        jsonResponse.setError(new JsonRpcError(ErrorCodes.METHOD_NOT_FOUND, "Method not found: " + method));
                }
            } else {
                jsonResponse.setError(new JsonRpcError(ErrorCodes.INVALID_REQUEST, "Invalid Request"));
            }
            
            response.addHeader("Content-Type", Http.CONTENT_TYPE_JSON);
            return jsonResponse.toString();
            
        } catch (SecurityException e) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return createErrorResponse("Unauthorized", ErrorCodes.UNAUTHORIZED);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "RPC request failed", e);
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return createErrorResponse("Internal server error: " + e.getMessage(), ErrorCodes.INTERNAL_ERROR);
        }
    }
    
    protected void handleInitialize(JsonRpcRequest request, JsonRpcResponse response) {
        if (!initialized) {
            JsonRpcResponse initResponse = MCPLifecycle.createInitializeResponse(
                UUID.randomUUID().toString(),
                Features.CORE_FEATURES
            );
            response.setId(request.getId());
            response.setResult(initResponse.getResult());
            initialized = true;
        } else {
            response.setError(new JsonRpcError(ErrorCodes.ALREADY_INITIALIZED, "Server already initialized"));
        }
    }
    
    protected void handleGetCapabilities(JsonRpcRequest request, JsonRpcResponse response) {
        Builder result = new Builder();
        result.put("version", version());
        result.put("protocol", PROTOCOL_ID);

        Builders features = new Builders();
        for (String feature : Features.CORE_FEATURES) {
            features.add(new Builder(feature));
        }
        result.put("features", features);
        
        response.setId(request.getId());
        response.setResult(result);
    }
    
    protected void handleShutdown(JsonRpcRequest request, JsonRpcResponse response) {
        if (sessionState != SessionState.READY) {
            response.setError(new JsonRpcError(ErrorCodes.NOT_INITIALIZED, "Not in ready state"));
            return;
        }

        // Close all SSE connections
        for (SSESession session : sseClients.values()) {
            session.close();
        }
        sseClients.clear();

        sessionState = SessionState.DISCONNECTED;
        
        response.setId(request.getId());
        response.setResult(new Builder().put("status", "shutdown_complete"));
    }
    
    protected void handleGetStatus(JsonRpcRequest request, JsonRpcResponse response) {
        Builder result = new Builder();
        result.put("state", sessionState.toString());
        result.put("sessionId", sessionId);
        result.put("uptime", System.currentTimeMillis() - sseClients.values().stream()
                .findFirst()
                .map(s -> s.createdAt)
                .orElse(System.currentTimeMillis()));
        
        response.setId(request.getId());
        response.setResult(result);
    }

    protected String validateAuthHeader(Request request) throws SecurityException {
        String auth = request.headers().get(Header.AUTHORIZATION).toString();
        if (auth == null || !authenticate(auth)) {
            throw new SecurityException("Invalid authorization");
        }
        return UUID.randomUUID().toString();
    }

    protected boolean authenticate(String token) {
        String validToken = getConfiguration().get(Config.AUTH_TOKEN);
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
        return PROTOCOL_VERSION;
    }

    private boolean validateJsonRpcRequest(String json) {
        try {
            Builder jsonObject = new Builder();
            jsonObject.parse(json);
            
            // Validate required JSON-RPC 2.0 fields
            if (!jsonObject.containsKey(JsonRpc.VERSION_FIELD) || 
                !JsonRpc.VERSION.equals(jsonObject.get(JsonRpc.VERSION_FIELD)) ||
                !jsonObject.containsKey(JsonRpc.METHOD_FIELD)) {
                return false;
            }
            
            // id is optional for notifications
            if (jsonObject.containsKey(JsonRpc.ID_FIELD)) {
                Object id = jsonObject.get(JsonRpc.ID_FIELD);
                if (!(id instanceof String || id instanceof Number || id == null)) {
                    return false;
                }
            }
            
            // Validate params if present
            if (jsonObject.containsKey(JsonRpc.PARAMS_FIELD)) {
                Object params = jsonObject.get(JsonRpc.PARAMS_FIELD);
                if (!(params instanceof Map || params instanceof List)) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Add batch request handling
    private String handleBatchRequest(String jsonStr, Response response) {
        try {
            Builders requests = new Builders();
            requests.parse(jsonStr);
            JsonRpcResponse[] responses = new JsonRpcResponse[requests.size()];
            
            for (int i = 0; i < requests.size(); i++) {
                // Process each request in the batch
                JsonRpcRequest rpcRequest = new JsonRpcRequest();
                rpcRequest.parse(requests.get(i).toString());
                
                JsonRpcResponse jsonResponse = new JsonRpcResponse();
                String method = rpcRequest.getMethod();
                
                switch (method) {
                    case Methods.INITIALIZE:
                        handleInitialize(rpcRequest, jsonResponse);
                        break;
                    case Methods.GET_CAPABILITIES:
                        handleGetCapabilities(rpcRequest, jsonResponse);
                        break;
                    case Methods.SHUTDOWN:
                        handleShutdown(rpcRequest, jsonResponse);
                        break;
                    case Methods.GET_STATUS:
                        handleGetStatus(rpcRequest, jsonResponse);
                        break;
                    default:
                        jsonResponse.setError(new JsonRpcError(ErrorCodes.METHOD_NOT_FOUND, 
                            "Method not found: " + method));
                }
                
                responses[i] = jsonResponse;
            }
            
            return Arrays.toString(responses);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Batch request processing failed", e);
            return createErrorResponse("Batch processing failed", ErrorCodes.INTERNAL_ERROR);
        }
    }

    /**
     * Represents a Server-Sent Events session
     */
    private static class SSESession {
        private final String id;
        private final StringBuilder output;
        private boolean isActive = true;
        private final long createdAt = System.currentTimeMillis();
        private long lastActivityAt = System.currentTimeMillis();

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

        public void sendStateChange(SessionState newState) {
            Builder data = new Builder();
            data.put("type", "state_change");
            data.put("state", newState.toString());
            sendEvent(Events.STATE, data.toString());
        }

        public void sendError(String errorMessage, int errorCode) {
            Builder data = new Builder();
            data.put("type", "error");
            data.put("code", errorCode);
            data.put("message", errorMessage);
            sendEvent(Events.ERROR, data.toString());
        }

        public void close() {
            if (isActive) {
                sendEvent(Events.CLOSE, "{\"reason\":\"session_closed\"}");
                isActive = false;
            }
        }

        public void updateActivity() {
            this.lastActivityAt = System.currentTimeMillis();
        }
        
        public boolean isExpired(long timeoutMs) {
            return System.currentTimeMillis() - lastActivityAt > timeoutMs;
        }
    }
}