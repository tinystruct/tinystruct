package org.tinystruct.mcp;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.mcp.tools.CalculatorTool;
import org.tinystruct.system.Settings;

import static org.junit.jupiter.api.Assertions.*;


public class MCPServerApplicationTest {
    private MCPServerApplication app;

    @BeforeEach
    public void setUp() {
        app = new MCPServerApplication();
        app.setConfiguration(new Settings());
        app.init();
        
        // Register calculator tool methods
        CalculatorTool calculator = new CalculatorTool();
        app.registerToolMethods(calculator);
        
        // Register a mock tool
        app.registerTool(new MockTool());
    }

    @Test
    public void testListTools() {
        JsonRpcRequest req = new JsonRpcRequest();
        req.setId("1");
        JsonRpcResponse resp = new JsonRpcResponse();
        app.handleListTools(req, resp);
        assertNull(resp.getError());
        assertNotNull(resp.getResult());
        
        // Check that tool methods are included
        String resultStr = resp.getResult().toString();
        assertTrue(resultStr.contains("calculator/add"), "Should include calculator/add");
        assertTrue(resultStr.contains("calculator/subtract"), "Should include calculator/subtract");
        assertTrue(resultStr.contains("calculator/multiply"), "Should include calculator/multiply");
        assertTrue(resultStr.contains("calculator/divide"), "Should include calculator/divide");
        assertTrue(resultStr.contains("mock-tool"), "Should include mock-tool");
    }

    @Test
    public void testCallToolMethodSuccess() {
        JsonRpcRequest req = new JsonRpcRequest();
        req.setId("2");
        Builder params = new Builder();
        params.put("name", "calculator/add");
        Builder arguments = new Builder();
        arguments.put("a", 5);
        arguments.put("b", 3);
        params.put("arguments", arguments);
        req.setParams(params);
        JsonRpcResponse resp = new JsonRpcResponse();
        app.handleCallTool(req, resp);
        assertNull(resp.getError());
        assertNotNull(resp.getResult());
        
        // Check that the response has the correct MCP format
        Builder result = resp.getResult();
        assertTrue(result.containsKey("content"), "Response should contain content array");
        Builders contentArray = (Builders) result.get("content");
        assertFalse(contentArray.isEmpty(), "Content array should not be empty");
        
        // Check the first content item
        Builder contentItem = contentArray.get(0);
        assertTrue(contentItem.containsKey("type"), "Content item should have type");
        assertTrue(contentItem.containsKey("text"), "Content item should have text");
        assertTrue(contentItem.get("text").toString().contains("8.0"), "Content text should contain result");
    }

    @Test
    public void testCallToolMethodNotFound() {
        JsonRpcRequest req = new JsonRpcRequest();
        req.setId("3");
        Builder params = new Builder();
        params.put("name", "calculator/nonexistent");
        params.put("arguments", new Builder());
        req.setParams(params);
        JsonRpcResponse resp = new JsonRpcResponse();
        app.handleCallTool(req, resp);
        assertNotNull(resp.getError());
        assertEquals(MCPSpecification.ErrorCodes.RESOURCE_NOT_FOUND, resp.getError().getCode());
    }

    @Test
    public void testCallLegacyToolSuccess() {
        JsonRpcRequest req = new JsonRpcRequest();
        req.setId("4");
        Builder params = new Builder();
        params.put("name", "mock-tool");
        params.put("arguments", new Builder());
        req.setParams(params);
        JsonRpcResponse resp = new JsonRpcResponse();
        app.handleCallTool(req, resp);
        assertNull(resp.getError());
        assertNotNull(resp.getResult());
        
        // Check that the response has the correct MCP format
        Builder result = resp.getResult();
        assertTrue(result.containsKey("content"), "Response should contain content array");
        Builders contentArray = (Builders) result.get("content");
        assertFalse(contentArray.isEmpty(), "Content array should not be empty");
        
        // Check the first content item
        Builder contentItem = contentArray.get(0);
        assertTrue(contentItem.containsKey("type"), "Content item should have type");
        assertTrue(contentItem.containsKey("text"), "Content item should have text");
        assertTrue(contentItem.get("text").toString().contains("mock-result"), "Content text should contain result");
    }

    @Test
    public void testCallLegacyToolNotFound() {
        JsonRpcRequest req = new JsonRpcRequest();
        req.setId("5");
        Builder params = new Builder();
        params.put("name", "not-exist");
        params.put("arguments", new Builder());
        req.setParams(params);
        JsonRpcResponse resp = new JsonRpcResponse();
        app.handleCallTool(req, resp);
        assertNotNull(resp.getError());
        assertEquals(MCPSpecification.ErrorCodes.RESOURCE_NOT_FOUND, resp.getError().getCode());
    }

    // Simple mock tool for testing
    static class MockTool extends MCPTool {
        public MockTool(String name, String description, MCPClient client) {
            super(name, description, client);
        }

        public MockTool() {
            this("mock-tool", "A mock tool for testing", null);
        }

        @Override
        public String getName() { return "mock-tool"; }
        @Override
        public String getDescription() { return "A mock tool for testing."; }
        @Override
        public Object execute(Builder parameters) { return "mock-result"; }
    }
} 