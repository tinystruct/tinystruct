package org.tinystruct.mcp;

import org.tinystruct.data.component.Builder;

public class MCPLifecycle {
    public static final String METHOD_INITIALIZE = "initialize";
    public static final String METHOD_SHUTDOWN = "shutdown";
    public static final String METHOD_CAPABILITIES = "capabilities";

    public static JsonRpcRequest createInitializeRequest(String clientName, String version) {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod(METHOD_INITIALIZE);
        
        Builder params = new Builder();
        params.put("clientName", clientName);
        params.put("version", version);
        request.setParams(params);
        
        return request;
    }

    public static JsonRpcResponse createInitializeResponse(String serverId, String[] supportedFeatures) {
        JsonRpcResponse response = new JsonRpcResponse();
        
        Builder result = new Builder();
        result.put("serverId", serverId);
        result.put("version", "1.0.0");
        result.put("protocol", "MCP/1.0");
        
        Builder capabilities = new Builder();
        for (String feature : supportedFeatures) {
            capabilities.put(feature, true);
        }
        result.put("capabilities", capabilities);
        
        response.setResult(result);
        return response;
    }

    public static JsonRpcRequest createCapabilitiesRequest() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod(METHOD_CAPABILITIES);
        return request;
    }

    public static JsonRpcRequest createShutdownRequest() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod(METHOD_SHUTDOWN);
        return request;
    }

    public static JsonRpcResponse createShutdownResponse() {
        JsonRpcResponse response = new JsonRpcResponse();
        Builder result = new Builder();
        result.put("status", "shutdown");
        response.setResult(result);
        return response;
    }
} 