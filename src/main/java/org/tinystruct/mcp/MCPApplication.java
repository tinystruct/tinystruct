package org.tinystruct.mcp;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.http.ResponseStatus;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

import java.util.UUID;
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
    
    protected SSEHandler sseHandler;
    protected JsonRpcHandler jsonRpcHandler;
    protected AuthorizationHandler authHandler;
    
    private boolean initialized = false;
    private SessionState sessionState = SessionState.DISCONNECTED;
    private final String sessionId = UUID.randomUUID().toString();

    @Override
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
    }

    @Action(Endpoints.INITIALIZE)
    public String initialize(Request request, Response response) throws ApplicationException {
        try {
            if (sessionState != SessionState.DISCONNECTED) {
                throw new IllegalStateException("Session already initialized");
            }
            
            sessionState = SessionState.INITIALIZING;
            String clientId = authHandler.validateAuthHeader(request);
            
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
            return jsonRpcHandler.createErrorResponse("Internal server error", ErrorCodes.INTERNAL_ERROR);
        }
    }

    @Action(Endpoints.SHUTDOWN)
    public String shutdown(Response response) throws ApplicationException {
        try {
            if (sessionState != SessionState.READY) {
                return jsonRpcHandler.createErrorResponse("Not in ready state", ErrorCodes.NOT_INITIALIZED);
            }

            sseHandler.closeAll();
            sessionState = SessionState.DISCONNECTED;
            
            JsonRpcResponse jsonResponse = new JsonRpcResponse();
            jsonResponse.setId(UUID.randomUUID().toString());
            jsonResponse.setResult(new Builder().put("status", "shutdown_complete"));
            
            response.addHeader("Content-Type", Http.CONTENT_TYPE_JSON);
            return jsonResponse.toString();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Shutdown failed", e);
            sessionState = SessionState.ERROR;
            return jsonRpcHandler.createErrorResponse("Shutdown failed", ErrorCodes.INTERNAL_ERROR);
        }
    }

    @Action(value = Endpoints.EVENTS, options = {
            @Argument(key = Http.TOKEN_PARAM, description = "Authentication token", optional = false)
    })
    public String events(Response response, Request request) throws ApplicationException {
        try {
            String token = request.getParameter(Http.TOKEN_PARAM);
            if (!authHandler.authenticate(token)) {
                response.setStatus(ResponseStatus.UNAUTHORIZED);
                return jsonRpcHandler.createErrorResponse("Invalid token", ErrorCodes.UNAUTHORIZED);
            }

            String clientId = UUID.randomUUID().toString();
            sseHandler.registerClient(clientId, response);
            sseHandler.setupSSEHeaders(response);
            // 发送初始连接事件
            sseHandler.sendEvent(clientId, Events.CONNECTED, "{\"clientId\":\"" + clientId + "\"}");
            // SSE 连接应保持打开，返回空字符串即可
            return "";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "SSE connection failed", e);
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return jsonRpcHandler.createErrorResponse("Internal server error", ErrorCodes.INTERNAL_ERROR);
        }
    }
    
    @Action(Endpoints.RPC)
    public String handleRpcRequest(Request request, Response response) throws ApplicationException {
        try {
            // Validate authentication
            authHandler.validateAuthHeader(request);
            
            // Parse the JSON-RPC request
            String jsonStr = request.body();
            LOGGER.fine("Received JSON: " + jsonStr);
            
            // Add batch request support
            if (jsonStr.trim().startsWith("[")) {
                return jsonRpcHandler.handleBatchRequest(jsonStr, response, this::handleMethod);
            }
            
            if (!jsonRpcHandler.validateJsonRpcRequest(jsonStr)) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return jsonRpcHandler.createErrorResponse("Invalid JSON-RPC request", ErrorCodes.INVALID_REQUEST);
            }
            
            JsonRpcRequest rpcRequest = new JsonRpcRequest();
            rpcRequest.parse(jsonStr);
            
            JsonRpcResponse jsonResponse = new JsonRpcResponse();
            handleMethod(rpcRequest, jsonResponse);
            
            response.addHeader("Content-Type", Http.CONTENT_TYPE_JSON);
            return jsonResponse.toString();
            
        } catch (SecurityException e) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return jsonRpcHandler.createErrorResponse("Unauthorized", ErrorCodes.UNAUTHORIZED);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "RPC request failed", e);
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return jsonRpcHandler.createErrorResponse("Internal server error: " + e.getMessage(), ErrorCodes.INTERNAL_ERROR);
        }
    }
    
    private void handleMethod(JsonRpcRequest request, JsonRpcResponse response) {
        String method = request.getMethod();
        switch (method) {
            case Methods.INITIALIZE:
                handleInitialize(request, response);
                break;
            case Methods.GET_CAPABILITIES:
                handleGetCapabilities(request, response);
                break;
            case Methods.SHUTDOWN:
                handleShutdown(request, response);
                break;
            case Methods.GET_STATUS:
                handleGetStatus(request, response);
                break;
            default:
                response.setError(new JsonRpcError(ErrorCodes.METHOD_NOT_FOUND, "Method not found: " + method));
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

        sseHandler.closeAll();
        sessionState = SessionState.DISCONNECTED;
        
        response.setId(request.getId());
        response.setResult(new Builder().put("status", "shutdown_complete"));
    }
    
    protected void handleGetStatus(JsonRpcRequest request, JsonRpcResponse response) {
        Builder result = new Builder();
        result.put("state", sessionState.toString());
        result.put("sessionId", sessionId);
        result.put("uptime", System.currentTimeMillis() - sseHandler.getFirstSessionCreatedAt());
        
        response.setId(request.getId());
        response.setResult(result);
    }

    @Override
    public String version() {
        return PROTOCOL_VERSION;
    }
}