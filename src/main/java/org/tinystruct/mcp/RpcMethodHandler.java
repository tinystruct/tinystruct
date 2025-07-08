package org.tinystruct.mcp;

/**
 * Functional interface for handling custom JSON-RPC methods in MCPServerApplication.
 * Implementations should process the request and populate the response.
 */
@FunctionalInterface
public interface RpcMethodHandler {
    /**
     * Handle a JSON-RPC request.
     *
     * @param request  the JSON-RPC request
     * @param response the JSON-RPC response to populate
     * @param app      the MCPServerApplication instance (for access to tools/resources)
     */
    void handle(JsonRpcRequest request, JsonRpcResponse response, MCPApplication app);
} 