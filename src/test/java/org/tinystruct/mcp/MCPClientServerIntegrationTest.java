/*******************************************************************************
 * Copyright  (c) 2013, 2025 James M. ZHOU
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.tinystruct.mcp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationContext;
import org.tinystruct.application.Context;
import org.tinystruct.data.component.Builder;
import org.tinystruct.mcp.tools.CalculatorTool;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Dispatcher;
import org.tinystruct.system.HttpServer;
import org.tinystruct.system.Settings;
import org.tinystruct.system.annotation.Action;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for MCP client-server interaction.
 * This test verifies the end-to-end flow from client to server, including tool execution.
 */
public class MCPClientServerIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(MCPClientServerIntegrationTest.class.getName());
    private static final String SERVER_URL = "http://localhost:8001";
    private static MCPServerApplication serverApp;
    private static Thread serverThread;
    
    @BeforeAll
    public static void setUp() throws Exception {
        serverThread = new Thread(() -> {
            try {
                Settings settings = new Settings();
                settings.set("default.base_url", "/?q=");
                settings.set("default.language", "en_US");
                settings.set("charset", "utf-8");

                serverApp = new MCPServerApplication();
                ApplicationManager.install(serverApp, settings);
                serverApp.registerTool(new CalculatorTool());

                Context serverContext = new ApplicationContext();
                serverContext.setAttribute("--server-port", "8001");
                ApplicationManager.install(new Dispatcher());
                ApplicationManager.install(new HttpServer());
                ApplicationManager.call("start", serverContext, Action.Mode.CLI);

                // Keep the thread alive as long as the server is running
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for the server to be ready (poll the port)
        boolean started = false;
        for (int i = 0; i < 30; i++) { // wait up to 30 seconds
            try (Socket socket = new Socket("localhost", 8001)) {
                started = true;
                break;
            } catch (IOException e) {
                Thread.sleep(1000);
            }
        }
        if (!started) {
            throw new RuntimeException("Server did not start in time");
        }
    }
    
    @AfterAll
    public static void tearDown() throws Exception {
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }
        // Optionally, call your server's shutdown logic here as well
        Context serverContext = new ApplicationContext();
        ApplicationManager.call("stop", serverContext);
    }

    /**
     * Test that a calculator tool with local execution support works correctly.
     */
    @Test
    public void testLocalCalculatorTool() throws MCPException {
        // Create a calculator tool with local execution support
        CalculatorTool calculator = new CalculatorTool(null) {
            @Override
            protected boolean supportsLocalExecution() {
                return true;
            }

            @Override
            protected Object executeLocally(Builder builder) throws MCPException {
                String operation = builder.get("operation").toString();

                double a = Double.parseDouble(builder.get("a").toString());
                double b = Double.parseDouble(builder.get("b").toString());

                switch (operation) {
                    case "add":
                        return a + b;
                    case "subtract":
                        return a - b;
                    case "multiply":
                        return a * b;
                    case "divide":
                        if (b == 0) {
                            throw new MCPException("Division by zero");
                        }
                        return a / b;
                    default:
                        throw new MCPException("Unknown operation: " + operation);
                }
            }
        };

        // Test addition
        Builder addParams = new Builder();
        addParams.put("operation", "add");
        addParams.put("a", 5);
        addParams.put("b", 3);
        Object addResult = calculator.execute(addParams);
        assertEquals(8.0, addResult, "5 + 3 should equal 8");

        // Test subtraction
        Builder subtractParams = new Builder();
        subtractParams.put("operation", "subtract");
        subtractParams.put("a", 10);
        subtractParams.put("b", 4);
        Object subtractResult = calculator.execute(subtractParams);
        assertEquals(6.0, subtractResult, "10 - 4 should equal 6");

        // Test multiplication
        Builder multiplyParams = new Builder();
        multiplyParams.put("operation", "multiply");
        multiplyParams.put("a", 6);
        multiplyParams.put("b", 7);
        Object multiplyResult = calculator.execute(multiplyParams);
        assertEquals(42.0, multiplyResult, "6 * 7 should equal 42");

        // Test division
        Builder divideParams = new Builder();
        divideParams.put("operation", "divide");
        divideParams.put("a", 20);
        divideParams.put("b", 4);
        Object divideResult = calculator.execute(divideParams);
        assertEquals(5.0, divideResult, "20 / 4 should equal 5");
    }

    /**
     * Test that the calculator tool with local execution support works correctly.
     */
    @Test
    public void testCalculatorTool() throws MCPException {
        // Create a calculator tool with local execution support
        CalculatorTool calculator = new CalculatorTool();

        // Test power operation (custom operation)
        Builder powerParams = new Builder();
        powerParams.put("operation", "power");
        powerParams.put("a", 2);
        powerParams.put("b", 3);
        Object powerResult = calculator.execute(powerParams);
        assertEquals(8.0, powerResult, "2^3 should equal 8");

        // Test modulo operation (custom operation)
        Builder moduloParams = new Builder();
        moduloParams.put("operation", "modulo");
        moduloParams.put("a", 10);
        moduloParams.put("b", 3);
        Object moduloResult = calculator.execute(moduloParams);
        assertEquals(1.0, moduloResult, "10 % 3 should equal 1");
    }

    /**
     * Test that parameter validation works correctly when executing a tool.
     */
    @Test
    public void testParameterValidation() {
        // Create a direct instance of MCPTool for validation testing
        MCPTool tool = new MCPTool("test-tool", "A test tool", createTestSchema(), null, false);

        // Test with missing required parameter
        Builder invalidParams = new Builder();
        invalidParams.put("operation", "add");
        invalidParams.put("a", 5);
        // Missing "b" parameter

        MCPException exception = assertThrows(MCPException.class, () -> {
            tool.execute(invalidParams);
        });

        // Verify that the exception message mentions the missing parameter
        assertTrue(exception.getMessage().contains("Missing required parameter") ||
                   exception.getMessage().contains("Parameter validation failed"),
                   "Exception should mention missing parameter");

        // Test with invalid enum value
        Builder invalidEnumParams = new Builder();
        invalidEnumParams.put("operation", "invalid");
        invalidEnumParams.put("a", 5);
        invalidEnumParams.put("b", 3);

        exception = assertThrows(MCPException.class, () -> {
            tool.execute(invalidEnumParams);
        });

        // Verify that the exception message mentions the invalid enum value
        assertTrue(exception.getMessage().contains("invalid value") ||
                   exception.getMessage().contains("Parameter validation failed"),
                   "Exception should mention invalid enum value");
    }

    /**
     * Test remote execution of a tool through a mocked client.
     * This test simulates the client-server interaction without actually starting a server.
     */
    @Test
    public void testRemoteExecution() throws MCPException, IOException {
        // Create a mock client
        MCPClient mockClient = mock(MCPClient.class);

        // Create a calculator tool with the mock client
        MCPTool calculator = new MCPTool("calculator", "A test calculator", createTestSchema(), mockClient);

        // Set up the mock client to return expected results
        when(mockClient.callTool(eq("calculator"), any(Builder.class))).thenAnswer(invocation -> {
            Builder params = invocation.getArgument(1);
            String operation = params.get("operation").toString();
            double a = Double.parseDouble(params.get("a").toString());
            double b = Double.parseDouble(params.get("b").toString());

            switch (operation) {
                case "add":
                    return a + b;
                case "subtract":
                    return a - b;
                case "multiply":
                    return a * b;
                case "divide":
                    if (b == 0) {
                        throw new MCPException("Division by zero");
                    }
                    return a / b;
                default:
                    throw new MCPException("Unknown operation: " + operation);
            }
        });

        // Test addition
        Builder addParams = new Builder();
        addParams.put("operation", "add");
        addParams.put("a", 5);
        addParams.put("b", 3);
        Object addResult = calculator.execute(addParams);
        assertEquals(8.0, addResult, "5 + 3 should equal 8");

        // Test subtraction
        Builder subtractParams = new Builder();
        subtractParams.put("operation", "subtract");
        subtractParams.put("a", 10);
        subtractParams.put("b", 4);
        Object subtractResult = calculator.execute(subtractParams);
        assertEquals(6.0, subtractResult, "10 - 4 should equal 6");

        // Test multiplication
        Builder multiplyParams = new Builder();
        multiplyParams.put("operation", "multiply");
        multiplyParams.put("a", 6);
        multiplyParams.put("b", 7);
        Object multiplyResult = calculator.execute(multiplyParams);
        assertEquals(42.0, multiplyResult, "6 * 7 should equal 42");

        // Test division
        Builder divideParams = new Builder();
        divideParams.put("operation", "divide");
        divideParams.put("a", 20);
        divideParams.put("b", 4);
        Object divideResult = calculator.execute(divideParams);
        assertEquals(5.0, divideResult, "20 / 4 should equal 5");

        // Verify that the client's callTool method was called the expected number of times
        verify(mockClient, times(4)).callTool(eq("calculator"), any(Builder.class));
    }

    /**
     * Test that parameter validation works correctly when executing a tool remotely.
     */
    @Test
    public void testRemoteParameterValidation() throws MCPException, IOException {
        // Create a mock client
        MCPClient mockClient = mock(MCPClient.class);

        // Set up the mock client to throw an exception for invalid parameters
        doThrow(new IOException("Tool execution failed: Parameter validation failed: Missing required parameter: b"))
            .when(mockClient).callTool(eq("calculator"), any(Builder.class));

        // Create a calculator tool with the mock client
        MCPTool calculator = new MCPTool("calculator", "A test calculator", createTestSchema(), mockClient);

        // Create parameters with a missing required parameter
        Builder invalidParams = new Builder();
        invalidParams.put("operation", "add");
        invalidParams.put("a", 5);
        // Missing "b" parameter

        // Execution should throw an exception
        MCPException exception = assertThrows(MCPException.class, () -> {
            calculator.execute(invalidParams);
        });

        // Verify that the exception message mentions the missing parameter
        assertTrue(exception.getMessage().contains("Missing required parameter") ||
                   exception.getMessage().contains("Parameter validation failed"),
                   "Exception should mention missing parameter");
    }

    /**
     * Creates a schema for testing.
     *
     * @return A Builder object representing a schema
     */
    private Builder createTestSchema() {
        Builder schema = new Builder();

        Builder properties = new Builder();

        Builder operation = new Builder();
        operation.put("type", "string");
        operation.put("description", "The operation to perform");
        operation.put("enum", new String[]{"add", "subtract", "multiply", "divide"});

        Builder a = new Builder();
        a.put("type", "number");
        a.put("description", "The first operand");

        Builder b = new Builder();
        b.put("type", "number");
        b.put("description", "The second operand");

        properties.put("operation", operation);
        properties.put("a", a);
        properties.put("b", b);

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new String[]{"operation", "a", "b"});

        return schema;
    }

    /**
     * Test that a tool without local execution support throws the correct exception.
     */
    @Test
    public void testNoLocalExecution() {
        // Create a tool without local execution support
        MCPTool tool = new MCPTool("test-tool", "A test tool", createTestSchema(), null);

        // Create valid parameters
        Builder params = new Builder();
        params.put("operation", "add");
        params.put("a", 5);
        params.put("b", 3);

        // Execution should throw an exception
        MCPException exception = assertThrows(MCPException.class, () -> {
            tool.execute(params);
        });

        // Verify that the exception message mentions local execution not implemented
        assertTrue(exception.getMessage().contains("Local execution not implemented"),
                   "Exception should mention local execution not implemented");
    }

    @Test
    public void testRealClientServerInteraction() throws Exception {
        LOGGER.info("Connecting to server at " + SERVER_URL);
        
        // Create a client and connect to the server
        MCPClient client = new MCPClient(SERVER_URL, null);
        client.connect();
        
        // List available resources
        List<MCPResource> resources = client.listResources();
        assertFalse(resources.isEmpty(), "Should have at least one resource");
        
        // Find the calculator tool
        MCPResource calculator = null;
        for (MCPResource resource : resources) {
            if (resource.getName().equals("calculator")) {
                calculator = resource;
                break;
            }
        }
        assertNotNull(calculator, "Should find the calculator tool");
        
        // Test addition
        Builder addParams = new Builder();
        addParams.put("operation", "add");
        addParams.put("a", 5);
        addParams.put("b", 3);
        Object addResult = client.executeResource("calculator", addParams);
        assertEquals(8.0, addResult, "5 + 3 should equal 8");
        
        // Test subtraction
        Builder subtractParams = new Builder();
        subtractParams.put("operation", "subtract");
        subtractParams.put("a", 10);
        subtractParams.put("b", 4);
        Object subtractResult = client.executeResource("calculator", subtractParams);
        assertEquals(6.0, subtractResult, "10 - 4 should equal 6");
        
        // Test multiplication
        Builder multiplyParams = new Builder();
        multiplyParams.put("operation", "multiply");
        multiplyParams.put("a", 6);
        multiplyParams.put("b", 7);
        Object multiplyResult = client.executeResource("calculator", multiplyParams);
        assertEquals(42.0, multiplyResult, "6 * 7 should equal 42");
        
        // Test division
        Builder divideParams = new Builder();
        divideParams.put("operation", "divide");
        divideParams.put("a", 20);
        divideParams.put("b", 4);
        Object divideResult = client.executeResource("calculator", divideParams);
        assertEquals(5.0, divideResult, "20 / 4 should equal 5");
        
        // Disconnect from the server
        client.disconnect();
    }
}
