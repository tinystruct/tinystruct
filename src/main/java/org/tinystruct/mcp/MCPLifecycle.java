package org.tinystruct.mcp;

import org.tinystruct.data.component.Builder;

import static org.tinystruct.mcp.MCPSpecification.Methods;

/**
 * Utility class for constructing standard JSON-RPC requests and responses for MCP lifecycle methods.
 */
public class MCPLifecycle {
    public static final String METHOD_INITIALIZE = "initialize";
    public static final String METHOD_SHUTDOWN = "shutdown";
    public static final String METHOD_CAPABILITIES = "capabilities";

    /**
     * Creates a JSON-RPC initialize request with protocol-compliant parameters.
     *
     * @param protocolVersion The protocol version supported by the client
     * @param capabilities    The client capabilities (structured Builder)
     * @param clientInfo      The client info (name, title, version, etc.)
     * @return The JSON-RPC initialize request
     */
    public static JsonRpcRequest createInitializeRequest(String protocolVersion, Builder capabilities, Builder clientInfo) {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod(METHOD_INITIALIZE);

        Builder params = new Builder();
        params.put("protocolVersion", protocolVersion);
        params.put("capabilities", capabilities);
        params.put("clientInfo", clientInfo);
        request.setParams(params);

        return request;
    }

    /**
     * Creates a JSON-RPC initialize response with MCP-compliant fields.
     *
     * @param protocolVersion   The protocol version to report
     * @param capabilities      The server capabilities (structured Builder)
     * @param serverInfo        The server info (name and version)
     * @return The JSON-RPC initialize response
     */
    public static JsonRpcResponse createInitializeResponse(String protocolVersion, Builder capabilities, Builder serverInfo) {
        JsonRpcResponse response = new JsonRpcResponse();

        Builder result = new Builder();
        result.put("protocolVersion", protocolVersion);
        result.put("capabilities", capabilities);
        result.put("serverInfo", serverInfo);

        response.setResult(result);
        return response;
    }

    /**
     * Creates a JSON-RPC capabilities request.
     *
     * @return The JSON-RPC capabilities request
     */
    public static JsonRpcRequest createCapabilitiesRequest() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod(METHOD_CAPABILITIES);
        return request;
    }

    /**
     * Creates a JSON-RPC shutdown request.
     *
     * @return The JSON-RPC shutdown request
     */
    public static JsonRpcRequest createShutdownRequest() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod(METHOD_SHUTDOWN);
        return request;
    }

    /**
     * Creates a JSON-RPC shutdown response.
     *
     * @return The JSON-RPC shutdown response
     */
    public static JsonRpcResponse createShutdownResponse() {
        JsonRpcResponse response = new JsonRpcResponse();
        Builder result = new Builder();
        result.put("status", "shutdown");
        response.setResult(result);
        return response;
    }

    /**
     * Creates a JSON-RPC notification for 'notifications/initialized'.
     *
     * @return The JSON-RPC notification request
     */
    public static JsonRpcRequest createInitializedNotification() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod(Methods.INITIALIZED_NOTIFICATION);
        return request;
    }
} 