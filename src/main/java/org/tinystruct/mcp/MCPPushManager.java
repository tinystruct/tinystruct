package org.tinystruct.mcp;

import org.tinystruct.http.SSEPushManager;

public class MCPPushManager extends SSEPushManager {
    private static final MCPPushManager instance = new MCPPushManager();

    private MCPPushManager() {
        super();
    }

    public static MCPPushManager getInstance() {
        return instance;
    }

    // Add MCP-specific overrides or methods here if needed
} 