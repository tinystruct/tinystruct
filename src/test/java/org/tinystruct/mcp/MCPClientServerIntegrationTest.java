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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.mockito.Mockito;
import org.tinystruct.data.component.Builder;
import org.tinystruct.mcp.tools.CalculatorTool;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for MCP client-server interaction.
 * This test verifies the end-to-end flow from client to server, including tool execution.
 */
public class MCPClientServerIntegrationTest {

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
        MCPTool tool = new MCPTool("test-tool", "A test tool", createTestSchema(), null);

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

    // Client for remote server tests
    private MCPClient client;

    /**
     * Test a real client-server interaction with a running server.
     * This test requires a running server on port 8000.
     * It can be enabled for manual testing.
     */
    @Test
    public void testRealClientServerInteraction() throws Exception {
        // This test uses a real server running on port 8001
        // Start the server with: bin/dispatcher start --import org.tinystruct.system.TomcatServer --import org.tinystruct.mcp.MCPServerApplication --server-port 8001

        final String SERVER_URL = "http://localhost:8001";
        final Logger LOGGER = Logger.getLogger(MCPClientServerIntegrationTest.class.getName());

        try {
            // Connect to the server
            LOGGER.info("Connecting to server at " + SERVER_URL);
            client = new MCPClient(SERVER_URL, null);
            client.connect();
            LOGGER.info("Client connected to server at " + SERVER_URL);

            // List available resources
            LOGGER.info("Listing available resources...");
            List<MCPResource> resources = client.listResources();
            LOGGER.info("Found " + resources.size() + " resources");

            // Check if the calculator tool is available
            boolean foundCalculator = false;
            for (MCPResource resource : resources) {
                LOGGER.info("Found resource: " + resource.getName() + " (" + resource.getType() + ")");
                if ("calculator".equals(resource.getName()) && resource.getType() == MCPResource.ResourceType.TOOL) {
                    foundCalculator = true;
                    LOGGER.info("Found calculator tool: " + resource.getDescription());
                }
            }

            // Verify that the calculator tool is available
            assertTrue(foundCalculator, "Calculator tool should be available on the server");

            // Test remote execution of the calculator tool
            LOGGER.info("Testing remote execution of calculator tool");

            // Create a calculator tool with the client
            MCPTool remoteCalculator = new MCPTool("calculator", "A calculator tool", null, client);

            // Test addition with remote execution
            Builder remoteAddParams = new Builder();
            remoteAddParams.put("operation", "add");
            remoteAddParams.put("a", 5);
            remoteAddParams.put("b", 3);
            LOGGER.info("Executing remote calculator tool with parameters: " + remoteAddParams);

            // Log the request details
            LOGGER.info("Request details: tool name = 'calculator', parameters = " + remoteAddParams);

            try {
                Object remoteAddResult = remoteCalculator.execute(remoteAddParams);
                LOGGER.info("Remote result: " + remoteAddResult);

                // Extract the result value from the JSON object
                double actualResult;
                if (remoteAddResult instanceof Builder) {
                    Builder resultBuilder = (Builder) remoteAddResult;
                    actualResult = Double.parseDouble(resultBuilder.get("result").toString());
                } else {
                    actualResult = Double.parseDouble(remoteAddResult.toString());
                }

                assertEquals(8.0, actualResult, "Remote: 5 + 3 should equal 8");

                // Test subtraction with remote execution
                Builder remoteSubtractParams = new Builder();
                remoteSubtractParams.put("operation", "subtract");
                remoteSubtractParams.put("a", 10);
                remoteSubtractParams.put("b", 4);
                LOGGER.info("Executing remote calculator tool with parameters: " + remoteSubtractParams);

                Object remoteSubtractResult = remoteCalculator.execute(remoteSubtractParams);
                LOGGER.info("Remote result: " + remoteSubtractResult);

                // Extract the result value
                double subtractResult;
                if (remoteSubtractResult instanceof Builder) {
                    Builder resultBuilder = (Builder) remoteSubtractResult;
                    subtractResult = Double.parseDouble(resultBuilder.get("result").toString());
                } else {
                    subtractResult = Double.parseDouble(remoteSubtractResult.toString());
                }

                assertEquals(6.0, subtractResult, "Remote: 10 - 4 should equal 6");

                // Test multiplication with remote execution
                Builder remoteMultiplyParams = new Builder();
                remoteMultiplyParams.put("operation", "multiply");
                remoteMultiplyParams.put("a", 6);
                remoteMultiplyParams.put("b", 7);
                LOGGER.info("Executing remote calculator tool with parameters: " + remoteMultiplyParams);

                Object remoteMultiplyResult = remoteCalculator.execute(remoteMultiplyParams);
                LOGGER.info("Remote result: " + remoteMultiplyResult);

                // Extract the result value
                double multiplyResult;
                if (remoteMultiplyResult instanceof Builder) {
                    Builder resultBuilder = (Builder) remoteMultiplyResult;
                    multiplyResult = Double.parseDouble(resultBuilder.get("result").toString());
                } else {
                    multiplyResult = Double.parseDouble(remoteMultiplyResult.toString());
                }

                assertEquals(42.0, multiplyResult, "Remote: 6 * 7 should equal 42");

                // Test division with remote execution
                Builder remoteDivideParams = new Builder();
                remoteDivideParams.put("operation", "divide");
                remoteDivideParams.put("a", 20);
                remoteDivideParams.put("b", 4);
                LOGGER.info("Executing remote calculator tool with parameters: " + remoteDivideParams);

                Object remoteDivideResult = remoteCalculator.execute(remoteDivideParams);
                LOGGER.info("Remote result: " + remoteDivideResult);

                // Extract the result value
                double divideResult;
                if (remoteDivideResult instanceof Builder) {
                    Builder resultBuilder = (Builder) remoteDivideResult;
                    divideResult = Double.parseDouble(resultBuilder.get("result").toString());
                } else {
                    divideResult = Double.parseDouble(remoteDivideResult.toString());
                }

                assertEquals(5.0, divideResult, "Remote: 20 / 4 should equal 5");
            } catch (Exception e) {
                LOGGER.severe("Error executing remote calculator: " + e.getMessage());
                throw e;
            }

            // Now test local execution with our enhanced CalculatorTool
            LOGGER.info("Testing CalculatorTool with local execution");

            // Create a calculator tool with local execution support
            CalculatorTool calculator = new CalculatorTool();

            // Test addition
            Builder addParams = new Builder();
            addParams.put("operation", "add");
            addParams.put("a", 5);
            addParams.put("b", 3);
            LOGGER.info("Executing calculator tool with parameters: " + addParams);
            Object addResult = calculator.execute(addParams);
            LOGGER.info("Result: " + addResult);
            assertEquals(8.0, addResult, "5 + 3 should equal 8");

            // Test subtraction
            Builder subtractParams = new Builder();
            subtractParams.put("operation", "subtract");
            subtractParams.put("a", 10);
            subtractParams.put("b", 4);
            LOGGER.info("Executing calculator tool with parameters: " + subtractParams);
            Object subtractResult = calculator.execute(subtractParams);
            LOGGER.info("Result: " + subtractResult);
            assertEquals(6.0, subtractResult, "10 - 4 should equal 6");

            // Test multiplication
            Builder multiplyParams = new Builder();
            multiplyParams.put("operation", "multiply");
            multiplyParams.put("a", 6);
            multiplyParams.put("b", 7);
            LOGGER.info("Executing calculator tool with parameters: " + multiplyParams);
            Object multiplyResult = calculator.execute(multiplyParams);
            LOGGER.info("Result: " + multiplyResult);
            assertEquals(42.0, multiplyResult, "6 * 7 should equal 42");

            // Test division
            Builder divideParams = new Builder();
            divideParams.put("operation", "divide");
            divideParams.put("a", 20);
            divideParams.put("b", 4);
            LOGGER.info("Executing calculator tool with parameters: " + divideParams);
            Object divideResult = calculator.execute(divideParams);
            LOGGER.info("Result: " + divideResult);
            assertEquals(5.0, divideResult, "20 / 4 should equal 5");

            // Test power operation (custom operation)
            Builder powerParams = new Builder();
            powerParams.put("operation", "power");
            powerParams.put("a", 2);
            powerParams.put("b", 3);
            LOGGER.info("Executing calculator tool with parameters: " + powerParams);
            Object powerResult = calculator.execute(powerParams);
            LOGGER.info("Result: " + powerResult);
            assertEquals(8.0, powerResult, "2^3 should equal 8");

            // Test modulo operation (custom operation)
            Builder moduloParams = new Builder();
            moduloParams.put("operation", "modulo");
            moduloParams.put("a", 10);
            moduloParams.put("b", 3);
            LOGGER.info("Executing calculator tool with parameters: " + moduloParams);
            Object moduloResult = calculator.execute(moduloParams);
            LOGGER.info("Result: " + moduloResult);
            assertEquals(1.0, moduloResult, "10 % 3 should equal 1");

            // Test the greeting prompt
            // Create a mock prompt with the same functionality as the server's greeting prompt
            MCPPrompt greetingPrompt = new MCPPrompt(
                "greeting",
                "A simple greeting prompt",
                "Hello, {{name}}! Welcome to the MCP server.",
                null,
                null
            ) {
                @Override
                protected boolean supportsLocalExecution() {
                    return true;
                }

                @Override
                protected Object executeLocally(Builder builder) throws MCPException {
                    String name = builder.get("name").toString();
                    return getTemplate().replace("{{name}}", name);
                }
            };

            // Execute the greeting prompt
            Builder greetingParams = new Builder();
            greetingParams.put("name", "John");
            LOGGER.info("Executing greeting prompt with parameters: " + greetingParams);
            Object greetingResult = greetingPrompt.execute(greetingParams);
            LOGGER.info("Result: " + greetingResult);
            assertEquals("Hello, John! Welcome to the MCP server.", greetingResult, "Greeting should be personalized");

            LOGGER.info("All tests passed successfully");
        } catch (Exception e) {
            LOGGER.severe("Error during test: " + e.getMessage());
            throw e;
        } finally {
            // Disconnect from the server if we're connected
            try {
                if (client != null) {
                    client.disconnect();
                    LOGGER.info("Disconnected from server");
                }
            } catch (Exception e) {
                LOGGER.warning("Error disconnecting from server: " + e.getMessage());
            }
        }
    }
}
