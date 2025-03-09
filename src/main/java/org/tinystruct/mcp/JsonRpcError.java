package org.tinystruct.mcp;

import org.tinystruct.data.component.Builder;

public class JsonRpcError {
    private int code;
    private String message;
    private Builder data;

    public JsonRpcError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Builder getData() {
        return data;
    }

    public void setData(Builder data) {
        this.data = data;
    }
} 