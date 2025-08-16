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

import org.junit.jupiter.api.*;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Context;
import org.tinystruct.data.component.Builder;
import org.tinystruct.mcp.examples.SampleMCPServerApplication;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Settings;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test version of the MCPServerTest example.
 * This test demonstrates how to set up an MCP server, register tools, and interact with it using a client.
 * 
 * Note: This test is disabled by default because it requires setting up the application manager.
 * It can be enabled for manual testing.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("This test requires setting up the application manager")
public class MCPServerTestJUnit {
    private static final Logger LOGGER = Logger.getLogger(MCPServerTestJUnit.class.getName());
    private static final String SERVER_URL = "http://localhost:8003";
    
    private MCPClient client;
    private SampleMCPServerApplication serverApp;
    
    /**
     * Set up the test environment before all tests.
     * This includes initializing the application manager, registering the server application,
     * starting the server, and connecting the client.
     */
    @BeforeAll
    public void setUp() throws ApplicationException, IOException {
        // Initialize the application manager with default settings
        Settings settings = new Settings();
        settings.set("default.base_url", "/?q=");
        settings.set("default.language", "en_US");
        settings.set("charset", "utf-8");
        settings.set("server.port", "8003");
        
        // Create and register the MCP server application
        serverApp = new SampleMCPServerApplication();
        ApplicationManager.install(serverApp, settings);
        
        LOGGER.info("Registered sample MCP server application with tools and prompts");
        
        // Start the server
        LOGGER.info("Starting MCP server...");
        Context serverContext = new ApplicationContext();
        String startResult = (String) ApplicationManager.call("mcp-server/start", serverContext);
        LOGGER.info("Server start result: " + startResult);
        
        // Connect to the server
        LOGGER.info("Connecting to MCP server...");
        client = new MCPClient(SERVER_URL, null);
        client.connect();
        LOGGER.info("Connected to MCP server at " + SERVER_URL);
    }
    
    /**
     * Clean up the test environment after all tests.
     * This includes disconnecting the client and stopping the server.
     */
    @AfterAll
    public void tearDown() throws ApplicationException, IOException {
        // Disconnect from the server
        if (client != null) {
            try {
                LOGGER.info("Disconnecting from MCP server...");
                client.disconnect();
                LOGGER.info("Disconnected from MCP server");
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error disconnecting from server", e);
            }
        }
        
        // Stop the server
        Context serverContext = new ApplicationContext();
        String stopResult = (String) ApplicationManager.call("mcp-server/stop", serverContext);
        LOGGER.info("Server stop result: " + stopResult);
    }
    
    /**
     * Test that the server has the calculator tool registered.
     */
    @Test
    public void testCalculatorToolRegistered() {
        // List tools
        try {
            Context serverContext = new ApplicationContext();
            String toolsResult = (String) ApplicationManager.call("mcp-server/list-tools", serverContext);
            LOGGER.info("Tools: " + toolsResult);
            
            // Verify that the calculator tool is registered
            assertTrue(toolsResult.contains("calculator"), "Calculator tool should be registered");
        } catch (ApplicationException e) {
            fail("Failed to list tools: " + e.getMessage());
        }
    }
    
    /**
     * Test that the client can execute the calculator tool on the server.
     */
    @Test
    public void testCalculatorTool() throws MCPException, IOException {
        // Test addition
        Builder addParams = new Builder();
        addParams.put("operation", "add");
        addParams.put("a", 5);
        addParams.put("b", 3);
        Object addResult = client.executeResource("calculator", addParams);
        LOGGER.info("5 + 3 = " + addResult);
        
        // Extract the result value from the JSON object
        double actualAddResult;
        if (addResult instanceof Builder) {
            Builder resultBuilder = (Builder) addResult;
            actualAddResult = Double.parseDouble(resultBuilder.get("result").toString());
        } else {
            actualAddResult = Double.parseDouble(addResult.toString());
        }
        
        assertEquals(8.0, actualAddResult, "5 + 3 should equal 8");
        
        // Test subtraction
        Builder subtractParams = new Builder();
        subtractParams.put("operation", "subtract");
        subtractParams.put("a", 10);
        subtractParams.put("b", 4);
        Object subtractResult = client.executeResource("calculator", subtractParams);
        LOGGER.info("10 - 4 = " + subtractResult);
        
        // Extract the result value
        double actualSubtractResult;
        if (subtractResult instanceof Builder) {
            Builder resultBuilder = (Builder) subtractResult;
            actualSubtractResult = Double.parseDouble(resultBuilder.get("result").toString());
        } else {
            actualSubtractResult = Double.parseDouble(subtractResult.toString());
        }
        
        assertEquals(6.0, actualSubtractResult, "10 - 4 should equal 6");
        
        // Test multiplication
        Builder multiplyParams = new Builder();
        multiplyParams.put("operation", "multiply");
        multiplyParams.put("a", 6);
        multiplyParams.put("b", 7);
        Object multiplyResult = client.executeResource("calculator", multiplyParams);
        LOGGER.info("6 * 7 = " + multiplyResult);
        
        // Extract the result value
        double actualMultiplyResult;
        if (multiplyResult instanceof Builder) {
            Builder resultBuilder = (Builder) multiplyResult;
            actualMultiplyResult = Double.parseDouble(resultBuilder.get("result").toString());
        } else {
            actualMultiplyResult = Double.parseDouble(multiplyResult.toString());
        }
        
        assertEquals(42.0, actualMultiplyResult, "6 * 7 should equal 42");
        
        // Test division
        Builder divideParams = new Builder();
        divideParams.put("operation", "divide");
        divideParams.put("a", 20);
        divideParams.put("b", 4);
        Object divideResult = client.executeResource("calculator", divideParams);
        LOGGER.info("20 / 4 = " + divideResult);
        
        // Extract the result value
        double actualDivideResult;
        if (divideResult instanceof Builder) {
            Builder resultBuilder = (Builder) divideResult;
            actualDivideResult = Double.parseDouble(resultBuilder.get("result").toString());
        } else {
            actualDivideResult = Double.parseDouble(divideResult.toString());
        }
        
        assertEquals(5.0, actualDivideResult, "20 / 4 should equal 5");
    }
    
    /**
     * Test that the client handles errors correctly.
     */
    @Test
    public void testErrorHandling() {
        // Test division by zero
        Builder divideByZeroParams = new Builder();
        divideByZeroParams.put("operation", "divide");
        divideByZeroParams.put("a", 10);
        divideByZeroParams.put("b", 0);
        
        MCPException exception = assertThrows(MCPException.class, () -> {
            client.executeResource("calculator", divideByZeroParams);
        });
        
        LOGGER.info("Division by zero error: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("Division by zero") || 
                   exception.getMessage().contains("divide by zero"),
                   "Exception should mention division by zero");
        
        // Test invalid operation
        Builder invalidOpParams = new Builder();
        invalidOpParams.put("operation", "invalid");
        invalidOpParams.put("a", 5);
        invalidOpParams.put("b", 3);
        
        exception = assertThrows(MCPException.class, () -> {
            client.executeResource("calculator", invalidOpParams);
        });
        
        LOGGER.info("Invalid operation error: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("Unknown operation") || 
                   exception.getMessage().contains("invalid value") ||
                   exception.getMessage().contains("Parameter validation failed"),
                   "Exception should mention invalid operation");
    }
    
    /**
     * Test that the client can execute the power and modulo operations.
     */
    @Test
    public void testAdvancedOperations() throws MCPException, IOException {
        // Test power operation
        Builder powerParams = new Builder();
        powerParams.put("operation", "power");
        powerParams.put("a", 2);
        powerParams.put("b", 3);
        Object powerResult = client.executeResource("calculator", powerParams);
        LOGGER.info("2^3 = " + powerResult);
        
        // Extract the result value
        double actualPowerResult;
        if (powerResult instanceof Builder) {
            Builder resultBuilder = (Builder) powerResult;
            actualPowerResult = Double.parseDouble(resultBuilder.get("result").toString());
        } else {
            actualPowerResult = Double.parseDouble(powerResult.toString());
        }
        
        assertEquals(8.0, actualPowerResult, "2^3 should equal 8");
        
        // Test modulo operation
        Builder moduloParams = new Builder();
        moduloParams.put("operation", "modulo");
        moduloParams.put("a", 10);
        moduloParams.put("b", 3);
        Object moduloResult = client.executeResource("calculator", moduloParams);
        LOGGER.info("10 % 3 = " + moduloResult);
        
        // Extract the result value
        double actualModuloResult;
        if (moduloResult instanceof Builder) {
            Builder resultBuilder = (Builder) moduloResult;
            actualModuloResult = Double.parseDouble(resultBuilder.get("result").toString());
        } else {
            actualModuloResult = Double.parseDouble(moduloResult.toString());
        }
        
        assertEquals(1.0, actualModuloResult, "10 % 3 should equal 1");
    }
}
