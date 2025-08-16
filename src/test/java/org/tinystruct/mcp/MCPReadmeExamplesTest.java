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
import org.tinystruct.mcp.tools.CalculatorTool;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Settings;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test for the examples in the README.md file.
 * This test demonstrates the usage examples from the README.md file.
 * 
 * Note: This test is disabled by default because it requires setting up the application manager.
 * It can be enabled for manual testing.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("This test requires setting up the application manager")
public class MCPReadmeExamplesTest {
    private static final Logger LOGGER = Logger.getLogger(MCPReadmeExamplesTest.class.getName());
    private static final String SERVER_URL = "http://localhost:8005";
    
    private MCPServerApplication serverApp;
    
    /**
     * Set up the test environment before all tests.
     * This includes initializing the application manager, registering the server application,
     * and starting the server.
     */
    @BeforeAll
    public void setUp() throws ApplicationException {
        // Initialize the application manager with default settings
        Settings settings = new Settings();
        settings.set("default.base_url", "/?q=");
        settings.set("default.language", "en_US");
        settings.set("charset", "utf-8");
        settings.set("server.port", "8005");
        
        // Create and register the MCP server application
        serverApp = new MCPServerApplication();
        ApplicationManager.install(serverApp, settings);
        
        // Register the calculator tool
        serverApp.registerTool(new CalculatorTool());
        LOGGER.info("Registered calculator tool with MCP server");
        
        // Register the MCP client application
        ApplicationManager.install(new MCPClientApplication(), settings);
        
        // Start the server
        LOGGER.info("Starting MCP server...");
        Context serverContext = new ApplicationContext();
        String startResult = (String) ApplicationManager.call("mcp-server/start", serverContext);
        LOGGER.info("Server start result: " + startResult);
    }
    
    /**
     * Clean up the test environment after all tests.
     * This includes stopping the server.
     */
    @AfterAll
    public void tearDown() throws ApplicationException {
        // Stop the server
        Context serverContext = new ApplicationContext();
        String stopResult = (String) ApplicationManager.call("mcp-server/stop", serverContext);
        LOGGER.info("Server stop result: " + stopResult);
    }
    
    /**
     * Test the command line interface example from the README.md file.
     */
    @Test
    public void testCommandLineInterface() throws ApplicationException {
        // Connect to the server
        Context connectContext = new ApplicationContext();
        connectContext.setAttribute("--url", SERVER_URL);
        String connectResult = (String) ApplicationManager.call("mcp/connect", connectContext);
        LOGGER.info("Connect result: " + connectResult);
        assertTrue(connectResult.contains("Connected"), "Should connect to the server");
        
        // List all available resources
        Context listContext = new ApplicationContext();
        String listResult = (String) ApplicationManager.call("mcp/list", listContext);
        LOGGER.info("List result: " + listResult);
        assertTrue(listResult.contains("calculator"), "Should list the calculator tool");
        
        // Execute a resource
        Context executeContext = new ApplicationContext();
        executeContext.setAttribute("--name", "calculator");
        executeContext.setAttribute("--params", "operation:add,a:1,b:2");
        String executeResult = (String) ApplicationManager.call("mcp/execute", executeContext);
        LOGGER.info("Execute result: " + executeResult);
        assertTrue(executeResult.contains("3.0") || executeResult.contains("3"), "1 + 2 should equal 3");
        
        // Get information about a resource
        Context infoContext = new ApplicationContext();
        infoContext.setAttribute("--name", "calculator");
        String infoResult = (String) ApplicationManager.call("mcp/info", infoContext);
        LOGGER.info("Info result: " + infoResult);
        assertTrue(infoResult.contains("calculator"), "Should show info for the calculator tool");
        
        // Disconnect from the MCP server
        Context disconnectContext = new ApplicationContext();
        String disconnectResult = (String) ApplicationManager.call("mcp/disconnect", disconnectContext);
        LOGGER.info("Disconnect result: " + disconnectResult);
        assertTrue(disconnectResult.contains("Disconnected") || disconnectResult.contains("Not connected"), 
                   "Should disconnect from the server or indicate not connected");
    }
    
    /**
     * Test the programmatic usage example from the README.md file.
     */
    @Test
    public void testProgrammaticUsage() throws ApplicationException {
        // Connect to an MCP server
        Context connectContext = new ApplicationContext();
        connectContext.setAttribute("--url", SERVER_URL);
        ApplicationManager.call("mcp/connect", connectContext);
        
        // List available resources
        String resources = (String) ApplicationManager.call("mcp/list", null);
        LOGGER.info("Resources: " + resources);
        assertTrue(resources.contains("calculator"), "Should list the calculator tool");
        
        // Execute a resource
        Context executeContext = new ApplicationContext();
        executeContext.setAttribute("--name", "calculator");
        executeContext.setAttribute("--params", "operation:add,a:1,b:2");
        String result = (String) ApplicationManager.call("mcp/execute", executeContext);
        LOGGER.info("Result: " + result);
        assertTrue(result.contains("3.0") || result.contains("3"), "1 + 2 should equal 3");
        
        // Disconnect from the MCP server
        ApplicationManager.call("mcp/disconnect", null);
    }
    
    /**
     * Test the direct API usage example from the README.md file.
     */
    @Test
    public void testDirectApiUsage() throws IOException, MCPException {
        // Create and connect the client
        MCPClient client = new MCPClient(SERVER_URL, null);
        client.connect();
        
        try {
            // List all resources
            List<MCPResource> resources = client.listResources();
            assertFalse(resources.isEmpty(), "Resources list should not be empty");
            
            boolean foundCalculator = false;
            for (MCPResource resource : resources) {
                LOGGER.info(resource.getName() + ": " + resource.getDescription());
                if ("calculator".equals(resource.getName())) {
                    foundCalculator = true;
                }
            }
            
            assertTrue(foundCalculator, "Calculator tool should be in the resources list");
            
            // Execute a tool
            Builder parameters = new Builder();
            parameters.put("operation", "add");
            parameters.put("a", 1);
            parameters.put("b", 2);
            Object result = client.executeResource("calculator", parameters);
            LOGGER.info("Result: " + result);
            
            // Extract the result value
            double actualResult;
            if (result instanceof Builder) {
                Builder resultBuilder = (Builder) result;
                actualResult = Double.parseDouble(resultBuilder.get("result").toString());
            } else {
                actualResult = Double.parseDouble(result.toString());
            }
            
            assertEquals(3.0, actualResult, "1 + 2 should equal 3");
            
        } finally {
            // Disconnect
            client.disconnect();
        }
    }
}
