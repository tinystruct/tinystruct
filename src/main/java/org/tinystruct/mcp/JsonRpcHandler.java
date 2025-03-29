package org.tinystruct.mcp;

import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.http.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles JSON-RPC request processing and response formatting
 */
public class JsonRpcHandler {
    private static final Logger LOGGER = Logger.getLogger(JsonRpcHandler.class.getName());

    /**
     * Validates a JSON-RPC request
     * @param json JSON string to validate
     * @return true if valid, false otherwise
     */
    public boolean validateJsonRpcRequest(String json) {
        try {
            Builder jsonObject = new Builder();
            jsonObject.parse(json);
            
            // Validate required JSON-RPC 2.0 fields
            if (!jsonObject.containsKey(MCPSpecification.JsonRpc.VERSION_FIELD) || 
                !MCPSpecification.JsonRpc.VERSION.equals(jsonObject.get(MCPSpecification.JsonRpc.VERSION_FIELD)) ||
                !jsonObject.containsKey(MCPSpecification.JsonRpc.METHOD_FIELD)) {
                return false;
            }
            
            // id is optional for notifications
            if (jsonObject.containsKey(MCPSpecification.JsonRpc.ID_FIELD)) {
                Object id = jsonObject.get(MCPSpecification.JsonRpc.ID_FIELD);
                if (!(id instanceof String || id instanceof Number || id == null)) {
                    return false;
                }
            }
            
            // Validate params if present
            if (jsonObject.containsKey(MCPSpecification.JsonRpc.PARAMS_FIELD)) {
                Object params = jsonObject.get(MCPSpecification.JsonRpc.PARAMS_FIELD);
                if (!(params instanceof Map || params instanceof List)) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates an error response
     * @param message Error message
     * @param code Error code
     * @return JSON-RPC error response
     */
    public String createErrorResponse(String message, int code) {
        JsonRpcResponse errorResponse = new JsonRpcResponse();
        errorResponse.setId(UUID.randomUUID().toString());
        JsonRpcError error = new JsonRpcError(code, message);
        errorResponse.setError(error);
        return errorResponse.toString();
    }

    /**
     * Processes a batch of JSON-RPC requests
     * @param jsonStr JSON string containing batch requests
     * @param response HTTP response
     * @param methodHandler Handler for processing individual methods
     * @return JSON-RPC batch response
     */
    public String handleBatchRequest(String jsonStr, Response response, MethodHandler methodHandler) {
        try {
            Builders requests = new Builders();
            requests.parse(jsonStr);
            JsonRpcResponse[] responses = new JsonRpcResponse[requests.size()];
            
            for (int i = 0; i < requests.size(); i++) {
                JsonRpcRequest rpcRequest = new JsonRpcRequest();
                rpcRequest.parse(requests.get(i).toString());
                
                JsonRpcResponse jsonResponse = new JsonRpcResponse();
                methodHandler.handleMethod(rpcRequest, jsonResponse);
                responses[i] = jsonResponse;
            }
            
            return responses.toString();
        } catch (Exception e) {
            LOGGER.severe("Batch request processing failed");
            return createErrorResponse("Batch processing failed", MCPSpecification.ErrorCodes.INTERNAL_ERROR);
        }
    }

    /**
     * Interface for handling JSON-RPC methods
     */
    public interface MethodHandler {
        void handleMethod(JsonRpcRequest request, JsonRpcResponse response);
    }
} 