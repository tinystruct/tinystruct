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

import org.tinystruct.data.component.Builder;
import org.tinystruct.mcp.MCPClient;
import org.tinystruct.mcp.MCPException;
import org.tinystruct.mcp.MCPResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example application that connects to an MCP server and executes a calculator tool.
 */
public class MCPClientExample {
    private static final Logger LOGGER = Logger.getLogger(MCPClientExample.class.getName());
    private static final String SERVER_URL = "http://localhost:8080";
    
    public static void main(String[] args) {
        MCPClient client = null;
        
        try {
            // Connect to the server
            System.out.println("Connecting to MCP server at " + SERVER_URL + "...");
            client = new MCPClient(SERVER_URL, null);
            client.connect();
            System.out.println("Connected to MCP server successfully");
            
            // List available resources
            System.out.println("\nListing available resources:");
            List<MCPResource> resources = client.listResources();
            for (MCPResource resource : resources) {
                System.out.println("- " + resource.getName() + " (" + resource.getType() + "): " + resource.getDescription());
            }
            
            // Test the calculator tool
            System.out.println("\nTesting calculator tool:");
            
            // Test addition
            Builder addParams = new Builder();
            addParams.put("operation", "add");
            addParams.put("a", 5);
            addParams.put("b", 3);
            Object addResult = client.executeResource("calculator", addParams);
            System.out.println("5 + 3 = " + addResult);
            
            // Test subtraction
            Builder subtractParams = new Builder();
            subtractParams.put("operation", "subtract");
            subtractParams.put("a", 10);
            subtractParams.put("b", 4);
            Object subtractResult = client.executeResource("calculator", subtractParams);
            System.out.println("10 - 4 = " + subtractResult);
            
            // Test multiplication
            Builder multiplyParams = new Builder();
            multiplyParams.put("operation", "multiply");
            multiplyParams.put("a", 6);
            multiplyParams.put("b", 7);
            Object multiplyResult = client.executeResource("calculator", multiplyParams);
            System.out.println("6 * 7 = " + multiplyResult);
            
            // Test division
            Builder divideParams = new Builder();
            divideParams.put("operation", "divide");
            divideParams.put("a", 20);
            divideParams.put("b", 4);
            Object divideResult = client.executeResource("calculator", divideParams);
            System.out.println("20 / 4 = " + divideResult);
            
        } catch (IOException | MCPException e) {
            LOGGER.log(Level.SEVERE, "Error connecting to MCP server", e);
            System.err.println("Error: " + e.getMessage());
        } finally {
            // Disconnect from the server
            if (client != null) {
                try {
                    System.out.println("\nDisconnecting from MCP server...");
                    client.disconnect();
                    System.out.println("Disconnected from MCP server");
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error disconnecting from MCP server", e);
                }
            }
        }
    }
}
