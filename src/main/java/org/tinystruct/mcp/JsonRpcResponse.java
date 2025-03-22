package org.tinystruct.mcp;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;

public class JsonRpcResponse extends JsonRpcMessage {
    private Builder result;
    private JsonRpcError error;

    public Builder getResult() {
        return result;
    }

    public void setResult(Builder result) {
        this.result = result;
    }

    public JsonRpcError getError() {
        return error;
    }

    public void setError(JsonRpcError error) {
        this.error = error;
    }

    @Override
    public void parse(String json) throws ApplicationException {
        Builder builder = new Builder();
        builder.parse(json);
        if (builder.containsKey("id")) {
            this.setId(String.valueOf(builder.get("id")));
        }

        if (builder.containsKey("result")) {
            Object resultObj = builder.get("result");
            if (resultObj instanceof Builder) {
                this.result = (Builder) resultObj;
            } else if (resultObj != null) {
                this.result = new Builder();
                this.result.parse(resultObj.toString());
            }
        }
        
        if (builder.containsKey("error")) {
            Object errorObj = builder.get("error");
            Builder errorBuilder;
            
            if (errorObj instanceof Builder) {
                errorBuilder = (Builder) errorObj;
            } else if (errorObj != null) {
                errorBuilder = new Builder();
                errorBuilder.parse(errorObj.toString());
            } else {
                return; // Skip if error is null
            }
            
            int code = 0;
            String message = "";
            
            if (errorBuilder.containsKey("code")) {
                Object codeObj = errorBuilder.get("code");
                if (codeObj instanceof Number) {
                    code = ((Number) codeObj).intValue();
                } else if (codeObj != null) {
                    code = Integer.parseInt(codeObj.toString());
                }
            }
            
            if (errorBuilder.containsKey("message")) {
                Object msgObj = errorBuilder.get("message");
                message = msgObj != null ? msgObj.toString() : "";
            }
            
            this.error = new JsonRpcError(code, message);
            
            if (errorBuilder.containsKey("data")) {
                Object dataObj = errorBuilder.get("data");
                if (dataObj instanceof Builder) {
                    this.error.setData((Builder) dataObj);
                } else if (dataObj != null) {
                    Builder dataBuilder = new Builder();
                    dataBuilder.parse(dataObj.toString());
                    this.error.setData(dataBuilder);
                }
            }
        }
    }

    @Override
    public String toString() {
        Builder builder = new Builder();
        builder.put("jsonrpc", getJsonrpc());
        if (getId() != null) {
            builder.put("id", getId());
        }
        if (result != null) {
            builder.put("result", result);
        }
        if (error != null) {
            Builder errorBuilder = new Builder();
            errorBuilder.put("code", error.getCode());
            errorBuilder.put("message", error.getMessage());
            if (error.getData() != null) {
                errorBuilder.put("data", error.getData());
            }
            builder.put("error", errorBuilder);
        }
        return builder.toString();
    }

    public boolean hasError() {
        return error != null;
    }
}