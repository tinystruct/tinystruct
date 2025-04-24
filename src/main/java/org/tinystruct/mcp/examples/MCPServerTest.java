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
package org.tinystruct.mcp.examples;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Context;
import org.tinystruct.data.component.Builder;
import org.tinystruct.mcp.MCPClient;
import org.tinystruct.mcp.MCPException;
import org.tinystruct.mcp.MCPServerApplication;
import org.tinystruct.mcp.tools.CalculatorTool;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Settings;
import org.tinystruct.system.annotation.Action;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test application for the MCP server and calculator tool.
 */
public class MCPServerTest extends AbstractApplication {
    private static final Logger LOGGER = Logger.getLogger(MCPServerTest.class.getName());
    private static final String SERVER_URL = "http://localhost:8080";

    @Override
    public void init() {
        // Set template not required for CLI operations
        this.setTemplateRequired(false);
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    /**
     * Runs the MCP server test.
     *
     * @return Test results
     */
    @Action(value = "mcp-server-test/run",
            description = "Run the MCP server test",
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String runTest() {
        StringBuilder sb = new StringBuilder();
        MCPClient client = null;

        try {
            // Get the MCP server application
            MCPServerApplication serverApp = (MCPServerApplication) ApplicationManager.get("org.tinystruct.mcp.MCPServerApplication");

            // Register the calculator tool
            serverApp.registerTool(new CalculatorTool());
            sb.append("Registered calculator tool with MCP server\n\n");

            // Start the server
            sb.append("Starting MCP server...\n");
            Context serverContext = new ApplicationContext();
            String startResult = (String) ApplicationManager.call("mcp-server/start", serverContext);
            sb.append(startResult).append("\n\n");

            // Connect to the server
            sb.append("Connecting to MCP server...\n");
            client = new MCPClient(SERVER_URL, null);
            client.connect();
            sb.append("Connected to MCP server at ").append(SERVER_URL).append("\n\n");

            // List tools
            sb.append("Listing tools...\n");
            String toolsResult = (String) ApplicationManager.call("mcp-server/list-tools", serverContext);
            sb.append(toolsResult).append("\n\n");

            // Test the calculator tool
            sb.append("Testing calculator tool:\n");

            // Test addition
            Builder addParams = new Builder();
            addParams.put("operation", "add");
            addParams.put("a", 5);
            addParams.put("b", 3);
            Object addResult = client.executeResource("calculator", addParams);
            sb.append("5 + 3 = ").append(addResult).append("\n");

            // Test subtraction
            Builder subtractParams = new Builder();
            subtractParams.put("operation", "subtract");
            subtractParams.put("a", 10);
            subtractParams.put("b", 4);
            Object subtractResult = client.executeResource("calculator", subtractParams);
            sb.append("10 - 4 = ").append(subtractResult).append("\n");

            // Test multiplication
            Builder multiplyParams = new Builder();
            multiplyParams.put("operation", "multiply");
            multiplyParams.put("a", 6);
            multiplyParams.put("b", 7);
            Object multiplyResult = client.executeResource("calculator", multiplyParams);
            sb.append("6 * 7 = ").append(multiplyResult).append("\n");

            // Test division
            Builder divideParams = new Builder();
            divideParams.put("operation", "divide");
            divideParams.put("a", 20);
            divideParams.put("b", 4);
            Object divideResult = client.executeResource("calculator", divideParams);
            sb.append("20 / 4 = ").append(divideResult).append("\n\n");

            // Disconnect from the server
            sb.append("Disconnecting from MCP server...\n");
            client.disconnect();
            sb.append("Disconnected from MCP server\n\n");

            // Stop the server
            sb.append("Stopping MCP server...\n");
            String stopResult = (String) ApplicationManager.call("mcp-server/stop", serverContext);
            sb.append(stopResult).append("\n");

            return sb.toString();
        } catch (IOException | ApplicationException e) {
            LOGGER.log(Level.SEVERE, "Error running MCP server test", e);
            return "Error running MCP server test: " + e.getMessage();
        }
    }

    /**
     * Main method to run the test directly.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            // Initialize the application manager with default settings
            Settings settings = new Settings();
            settings.set("default.base_url", "/?q=");
            settings.set("default.language", "en_US");
            settings.set("charset", "utf-8");

            // Register the applications
            ApplicationManager.install(new MCPServerApplication(), settings);
            ApplicationManager.install(new MCPServerTest(), settings);

            // Run the test
            Context context = new ApplicationContext();
            String result = (String) ApplicationManager.call("mcp-server-test/run", context);
            System.out.println(result);

            // Wait for user input before exiting
            System.out.println("Press Enter to exit...");
            new Scanner(System.in).nextLine();

        } catch (ApplicationException e) {
            LOGGER.log(Level.SEVERE, "Error running MCP server test", e);
        }
    }
}
