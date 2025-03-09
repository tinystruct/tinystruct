package org.tinystruct.mcp;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;

public class JsonRpcMessage {
    private final String jsonrpc = "2.0";
    private String id;

    public String getJsonrpc() {
        return jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void parse(String json) throws ApplicationException {
        Builder builder = new Builder();
        builder.parse(json);
        
        if (builder.containsKey("id")) {
            this.id = String.valueOf(builder.get("id"));
        }
    }

    @Override
    public String toString() {
        Builder builder = new Builder();
        builder.put("jsonrpc", jsonrpc);
        if (id != null) {
            builder.put("id", id);
        }
        return builder.toString();
    }
} 