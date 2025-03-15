package org.tinystruct.mcp;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;

public class JsonRpcRequest extends JsonRpcMessage {
    private String method;
    private Builder params;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Builder getParams() {
        return params;
    }

    public void setParams(Builder params) {
        this.params = params;
    }

    @Override
    public void parse(String json) throws ApplicationException {
        Builder builder = new Builder();
        builder.parse(json);
        if (builder.containsKey("id")) {
            this.setId(String.valueOf(builder.get("id")));
        }

        if (builder.containsKey("method")) {
            this.method = String.valueOf(builder.get("method"));
        }
        
        if (builder.containsKey("params")) {
            if (builder.get("params") instanceof Builder) {
                this.params = (Builder) builder.get("params");
            } else {
                this.params = new Builder();
                this.params.parse(builder.get("params").toString());
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
        if (method != null) {
            builder.put("method", method);
        }
        if (params != null) {
            builder.put("params", params);
        }
        return builder.toString();
    }
} 