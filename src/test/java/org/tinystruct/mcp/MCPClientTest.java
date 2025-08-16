package org.tinystruct.mcp;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.mcp.tools.CalculatorTool;
import org.tinystruct.system.Settings;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;


public class MCPClientTest {
    private MCPServerApplication serverApp;
    private MCPClient client;
    private static final String SERVER_URL = "http://localhost:8004";

    @BeforeEach
    public void setUp() {
        // Set up server
        serverApp = new MCPServerApplication();
        serverApp.setConfiguration(new Settings());
        serverApp.init();
        
        // Register calculator tool methods
        CalculatorTool calculator = new CalculatorTool();
        serverApp.registerToolMethods(calculator);
        
        // Set up client and initialize it for testing
        client = new MCPClient(SERVER_URL, null);
        
        // Use reflection to set the session state for testing
        try {
            Field sessionStateField = MCPClient.class.getDeclaredField("sessionState");
            sessionStateField.setAccessible(true);
            sessionStateField.set(client, MCPSpecification.SessionState.READY);
        } catch (Exception e) {
            fail("Failed to set session state for testing: " + e.getMessage());
        }
    }

    @Test
    public void testClientInitialization() {
        // Test that the client is properly initialized
        assertNotNull(client);
        assertEquals(MCPSpecification.SessionState.READY, client.getSessionState());
    }

    @Test
    public void testClientDisconnection() {
        // Test that the client can be disconnected
        try {
            client.disconnect();
            assertEquals(MCPSpecification.SessionState.DISCONNECTED, client.getSessionState());
        } catch (IOException e) {
            // Expected since there's no actual server
            assertTrue(e.getMessage().contains("Connection failed"));
        }
    }

    @Test
    public void testClientStateManagement() {
        // Test client state management
        assertNotNull(client.getSessionState());
        assertTrue(client.getSessionState() == MCPSpecification.SessionState.READY);
    }

    @Test
    public void testClientConfiguration() {
        // Test that client configuration is correct
        assertNotNull(client);
        // The client should be configured with the test URL
        assertTrue(client.getSessionState() == MCPSpecification.SessionState.READY);
    }

    @Test
    public void testListResourcesAndGetResource() {
        // Should return empty or throw since no real server
        assertThrows(MCPException.class, () -> client.listResources());
        assertThrows(MCPException.class, () -> client.getResource("nonexistent"));
    }

    @Test
    public void testExecuteResourceAndCallTool() {
        // Should throw since no real server
        assertThrows(MCPException.class, () -> client.executeResource("nonexistent", new org.tinystruct.data.component.Builder()));
        assertThrows(IOException.class, () -> client.callTool("nonexistent", new org.tinystruct.data.component.Builder()));
    }

    @Test
    public void testConnectWrongState() {
        // Set state to READY and try to connect again
        try {
            java.lang.reflect.Field sessionStateField = MCPClient.class.getDeclaredField("sessionState");
            sessionStateField.setAccessible(true);
            sessionStateField.set(client, org.tinystruct.mcp.MCPSpecification.SessionState.READY);
            assertThrows(IllegalStateException.class, () -> client.connect());
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    public void testDisconnectWrongState() {
        // Set state to DISCONNECTED and try to disconnect
        try {
            java.lang.reflect.Field sessionStateField = MCPClient.class.getDeclaredField("sessionState");
            sessionStateField.setAccessible(true);
            sessionStateField.set(client, org.tinystruct.mcp.MCPSpecification.SessionState.DISCONNECTED);
            assertThrows(IllegalStateException.class, () -> client.disconnect());
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }
} 