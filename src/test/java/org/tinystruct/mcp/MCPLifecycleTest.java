package org.tinystruct.mcp;

import org.junit.jupiter.api.Test;
import org.tinystruct.data.component.Builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MCPLifecycleTest {
    @Test
    public void testCreateInitializeRequest() {
        Builder capabilities = new Builder();
        capabilities.put("feature", true);
        Builder clientInfo = new Builder();
        clientInfo.put("name", "test-client");
        var req = MCPLifecycle.createInitializeRequest("1.0", capabilities, clientInfo);
        assertEquals("initialize", req.getMethod());
        assertEquals("1.0", ((Builder)req.getParams()).get("protocolVersion"));
    }

    @Test
    public void testCreateInitializeResponse() {
        Builder capabilities = new Builder();
        Builder serverInfo = new Builder();
        serverInfo.put("name", "server");
        var resp = MCPLifecycle.createInitializeResponse("1.0", capabilities, serverInfo);
        assertNotNull(resp.getResult());
        assertEquals("1.0", ((Builder)resp.getResult()).get("protocolVersion"));
    }

    @Test
    public void testCreateCapabilitiesRequest() {
        var req = MCPLifecycle.createCapabilitiesRequest();
        assertEquals("capabilities", req.getMethod());
    }

    @Test
    public void testCreateShutdownRequest() {
        var req = MCPLifecycle.createShutdownRequest();
        assertEquals("shutdown", req.getMethod());
    }

    @Test
    public void testCreateShutdownResponse() {
        var resp = MCPLifecycle.createShutdownResponse();
        assertEquals("shutdown", ((Builder)resp.getResult()).get("status"));
    }

    @Test
    public void testCreateInitializedNotification() {
        var req = MCPLifecycle.createInitializedNotification();
        assertEquals(org.tinystruct.mcp.MCPSpecification.Methods.INITIALIZED_NOTIFICATION, req.getMethod());
    }
} 