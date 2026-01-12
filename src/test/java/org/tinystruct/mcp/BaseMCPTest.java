package org.tinystruct.mcp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.tinystruct.ApplicationContext;
import org.tinystruct.application.Context;
import org.tinystruct.mcp.tools.CalculatorTool;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Dispatcher;
import org.tinystruct.system.HttpServer;
import org.tinystruct.system.Settings;
import org.tinystruct.system.annotation.Action;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Base class for MCP integration tests that require a running MCP server.
 */
public abstract class BaseMCPTest {
    protected static final Logger LOGGER = Logger.getLogger(BaseMCPTest.class.getName());
    protected static final int SERVER_PORT = 8004;
    protected static final String SERVER_URL = "http://localhost:" + SERVER_PORT;
    protected static MCPServerApplication serverApp;
    protected static Thread serverThread;
    protected static String authToken;

    @BeforeAll
    public static void startServer() throws Exception {
        serverThread = new Thread(() -> {
            try {
                Settings settings = new Settings();
                settings.set("default.base_url", "/?q=");
                settings.set("default.language", "en_US");
                settings.set("charset", "utf-8");
                settings.set("server.port", String.valueOf(SERVER_PORT));

                serverApp = new MCPServerApplication();
                ApplicationManager.install(serverApp, settings);
                serverApp.registerTool(new CalculatorTool());

                Context serverContext = new ApplicationContext();
                serverContext.setAttribute("--server-port", String.valueOf(SERVER_PORT));
                ApplicationManager.install(new Dispatcher());
                ApplicationManager.install(new HttpServer());
                ApplicationManager.call("start", serverContext, Action.Mode.CLI);

                // Keep the thread alive as long as the server is running
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                LOGGER.severe("Server thread error: " + e.getMessage());
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for the server to be ready (poll the port)
        boolean started = false;
        for (int i = 0; i < 30; i++) { // wait up to 30 seconds
            try (Socket socket = new Socket("localhost", SERVER_PORT)) {
                started = true;
                break;
            } catch (IOException e) {
                Thread.sleep(1000);
            }
        }
        if (!started) {
            throw new RuntimeException("Server did not start in time");
        }

        authToken = "Bearer " + serverApp.getConfiguration().get(MCPSpecification.Config.AUTH_TOKEN);
        LOGGER.info("MCP Server started on port " + SERVER_PORT);
    }

    @AfterAll
    public static void stopServer() {
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
            LOGGER.info("MCP Server stopped");
        }
    }
}
