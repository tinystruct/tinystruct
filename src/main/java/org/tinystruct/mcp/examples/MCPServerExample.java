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

import org.tinystruct.ApplicationException;
import org.tinystruct.mcp.MCPApplication;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Settings;

import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example application that runs an MCP server.
 */
public class MCPServerExample {
    private static final Logger LOGGER = Logger.getLogger(MCPServerExample.class.getName());
    
    public static void main(String[] args) {
        // Initialize the application manager with default settings
        Settings settings = new Settings();
        settings.set("default.base_url", "/?q=");

        settings.set("default.language", "en_US");
        settings.set("charset", "utf-8");

        // Create and register the MCP application
        MCPApplication mcpApp = new MCPApplication();
        ApplicationManager.install(mcpApp, settings);

        System.out.println("MCP server started");
        System.out.println("You can now use the MCP client to connect to the server");
        System.out.println("Press Enter to stop the server...");

        // Wait for user input before exiting
        new Scanner(System.in).nextLine();

        System.out.println("MCP server stopped");

    }
}
