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
import org.junit.jupiter.api.TestInstance;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Context;
import org.tinystruct.mcp.tools.CalculatorTool;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Settings;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit test for the MCPClientApplication.
 * This test demonstrates how to use the MCPClientApplication to interact with an MCP server.
 * 
 * Note: This test is disabled by default because it requires setting up the application manager.
 * It can be enabled for manual testing.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MCPClientApplicationTest {
    private static final Logger LOGGER = Logger.getLogger(MCPClientApplicationTest.class.getName());
    private static final String SERVER_URL = "http://localhost:8004";
    
    /**
     * Set up the test environment before all tests.
     * This includes initializing the application manager, registering the server and client applications,
     * and starting the server.
     */
    @BeforeAll
    public void setUp() throws ApplicationException {
        // Initialize the application manager with default settings
        Settings settings = new Settings();
        settings.set("default.base_url", "/?q=");
        settings.set("default.language", "en_US");
        settings.set("charset", "utf-8");
        settings.set("server.port", "8004");
        
        // Create and register the MCP server application
        MCPServerApplication serverApp = new MCPServerApplication();
        ApplicationManager.install(serverApp, settings);
        
        // Register the calculator tool
        serverApp.registerTool(new CalculatorTool());
        LOGGER.info("Registered calculator tool with MCP server");
        
        // Register the MCP client application
        MCPClientApplication clientApp = new MCPClientApplication();
        ApplicationManager.install(clientApp, settings);
        
        // Start the server
        LOGGER.info("Starting MCP server...");
        Context serverContext = new ApplicationContext();
        String startResult = (String) ApplicationManager.call("mcp-server/start", serverContext);
        LOGGER.info("Server start result: " + startResult);
        
        // Wait for server to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
     * Test the connect action of the MCPClientApplication.
     */
    @Test
    public void testConnect() throws ApplicationException {
        // Create a context with the URL parameter
        Context context = new ApplicationContext();
        context.setAttribute("--url", SERVER_URL);
        
        // Call the connect action
        String result = (String) ApplicationManager.call("mcp/connect", context);
        LOGGER.info("Connect result: " + result);
        
        // Verify the result
        assertTrue(result.contains("Connected to MCP server"), "Should connect to the server");
    }
    
    /**
     * Test the list action of the MCPClientApplication.
     */
    @Test
    public void testList() throws ApplicationException {
        // First connect to the server
        Context connectContext = new ApplicationContext();
        connectContext.setAttribute("--url", SERVER_URL);
        ApplicationManager.call("mcp/connect", connectContext);
        
        // Call the list action
        Context listContext = new ApplicationContext();
        String result = (String) ApplicationManager.call("mcp/list", listContext);
        LOGGER.info("List result: " + result);
        
        // Verify the result
        assertTrue(result.contains("calculator"), "Should list the calculator tool");
    }
    
    /**
     * Test the execute action of the MCPClientApplication.
     */
    @Test
    public void testExecute() throws ApplicationException {
        // First connect to the server
        Context connectContext = new ApplicationContext();
        connectContext.setAttribute("--url", SERVER_URL);
        ApplicationManager.call("mcp/connect", connectContext);
        
        // Call the execute action for addition
        Context executeContext = new ApplicationContext();
        executeContext.setAttribute("--name", "calculator");
        executeContext.setAttribute("--params", "operation:add,a:5,b:3");
        String result = (String) ApplicationManager.call("mcp/execute", executeContext);
        LOGGER.info("Execute result: " + result);
        
        // Verify the result
        assertTrue(result.contains("8.0") || result.contains("8"), "5 + 3 should equal 8");
    }
    
    /**
     * Test the info action of the MCPClientApplication.
     */
    @Test
    public void testInfo() throws ApplicationException {
        // First connect to the server
        Context connectContext = new ApplicationContext();
        connectContext.setAttribute("--url", SERVER_URL);
        ApplicationManager.call("mcp/connect", connectContext);
        
        // Call the info action
        Context infoContext = new ApplicationContext();
        infoContext.setAttribute("--name", "calculator");
        String result = (String) ApplicationManager.call("mcp/info", infoContext);
        LOGGER.info("Info result: " + result);
        
        // Verify the result
        assertTrue(result.contains("calculator"), "Should show info for the calculator tool");
        assertTrue(result.contains("operation"), "Should show the operation parameter");
        assertTrue(result.contains("a"), "Should show the a parameter");
        assertTrue(result.contains("b"), "Should show the b parameter");
    }
    
    /**
     * Test the disconnect action of the MCPClientApplication.
     */
    @Test
    public void testDisconnect() throws ApplicationException {
        // First connect to the server
        Context connectContext = new ApplicationContext();
        connectContext.setAttribute("--url", SERVER_URL);
        ApplicationManager.call("mcp/connect", connectContext);
        
        // Call the disconnect action
        Context disconnectContext = new ApplicationContext();
        String result = (String) ApplicationManager.call("mcp/disconnect", disconnectContext);
        LOGGER.info("Disconnect result: " + result);
        
        // Verify the result
        assertTrue(result.contains("Disconnected") || result.contains("Not connected"), 
                   "Should disconnect from the server or indicate not connected");
    }
    
    /**
     * Test the help action of the MCPClientApplication.
     */
    @Test
    public void testHelp() throws ApplicationException {
        // Call the help action
        Context helpContext = new ApplicationContext();
        helpContext.setAttribute("--help", "true");
        String result = (String) ApplicationManager.call("mcp", helpContext);
        LOGGER.info("Help result: " + result);
        
        // Verify the result
        assertTrue(result.contains("Usage"), "Should show usage information");
        assertTrue(result.contains("mcp/connect"), "Should show connect command");
        assertTrue(result.contains("mcp/list"), "Should show list command");
        assertTrue(result.contains("mcp/execute"), "Should show execute command");
    }
}
