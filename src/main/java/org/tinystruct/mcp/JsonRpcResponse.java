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
            if (builder.get("result") instanceof Builder) {
                this.result = (Builder) builder.get("result");
            } else {
                this.result = new Builder();
                this.result.parse(builder.get("result").toString());
            }
        }
        
        if (builder.containsKey("error")) {
            Builder errorBuilder = new Builder();
            errorBuilder.parse(builder.get("error").toString());
            
            int code = Integer.parseInt(String.valueOf(errorBuilder.get("code")));
            String message = String.valueOf(errorBuilder.get("message"));
            this.error = new JsonRpcError(code, message);
            
            if (errorBuilder.containsKey("data")) {
                Builder dataBuilder = new Builder();
                dataBuilder.parse(errorBuilder.get("data").toString());
                this.error.setData(dataBuilder);
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
} 