package org.tinystruct.mcp;

import org.junit.jupiter.api.Test;
import org.tinystruct.data.component.Builder;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the MCPClient using a real server.
 * Extends BaseMCPTest to use the centralized server setup.
 */
public class MCPClientTest extends BaseMCPTest {

    @Test
    public void testClientConnect() throws IOException {
        MCPClient client = new MCPClient(SERVER_URL, authToken);
        client.connect();
        assertEquals(MCPSpecification.SessionState.READY, client.getSessionState());
        client.disconnect();
    }

    @Test
    public void testClientDisconnection() throws IOException {
        MCPClient client = new MCPClient(SERVER_URL, authToken);
        client.connect();
        client.disconnect();
        assertEquals(MCPSpecification.SessionState.DISCONNECTED, client.getSessionState());
    }

    @Test
    public void testClientStateManagement() throws IOException {
        MCPClient client = new MCPClient(SERVER_URL, authToken);
        assertEquals(MCPSpecification.SessionState.DISCONNECTED, client.getSessionState());

        client.connect();
        assertEquals(MCPSpecification.SessionState.READY, client.getSessionState());

        client.disconnect();
        assertEquals(MCPSpecification.SessionState.DISCONNECTED, client.getSessionState());
    }

    @Test
    public void testListResources() throws IOException, MCPException {
        MCPClient client = new MCPClient(SERVER_URL, authToken);
        client.connect();

        List<MCPResource> resources = client.listResources();
        assertNotNull(resources);
        assertTrue(resources.size() > 0, "Should have at least one resource (the calculator tool)");

        client.disconnect();
    }

    @Test
    public void testExecuteResource() throws IOException, MCPException {
        MCPClient client = new MCPClient(SERVER_URL, authToken);
        client.connect();

        Builder params = new Builder();
        params.put("operation", "add");
        params.put("a", 10.0);
        params.put("b", 20.0);

        Object result = client.executeResource("calculator", params);
        assertNotNull(result);

        // The result should be "30.0" as per current implementation of formatToolResult
        assertEquals("30.0", result.toString());

        client.disconnect();
    }

    @Test
    public void testCallTool() throws IOException {
        MCPClient client = new MCPClient(SERVER_URL, authToken);
        client.connect();

        Builder params = new Builder();
        params.put("operation", "multiply");
        params.put("a", 5.0);
        params.put("b", 6.0);

        Object result = client.callTool("calculator", params);
        assertNotNull(result);
        assertEquals("30.0", result.toString());

        client.disconnect();
    }

    @Test
    public void testAuthentication() {
        // Test with invalid token
        MCPClient client = new MCPClient(SERVER_URL, "Bearer invalid-token");
        assertThrows(IOException.class, client::connect, "Should fail with invalid token");
    }

    @Test
    public void testResourceNotFound() throws IOException {
        MCPClient client = new MCPClient(SERVER_URL, authToken);
        client.connect();

        MCPException exception = assertThrows(MCPException.class, () -> {
            client.executeResource("non-existent-tool", new Builder());
        });

        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("404"),
                "Error message should indicate resource not found");

        client.disconnect();
    }
}
