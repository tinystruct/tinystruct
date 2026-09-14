package org.tinystruct.mcp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.tinystruct.ApplicationContext;
import org.tinystruct.application.Context;
import org.tinystruct.data.component.Builder;
import org.tinystruct.http.security.JWTManager;
import org.tinystruct.mcp.tools.CalculatorTool;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Dispatcher;
import org.tinystruct.system.HttpServer;
import org.tinystruct.system.Settings;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Base class for MCP integration tests that require a running MCP server.
 */
public abstract class BaseMCPTest {
    protected static final Logger LOGGER = Logger.getLogger(BaseMCPTest.class.getName());
    protected static final int SERVER_PORT = 8004;
    protected static final String SERVER_URL = "http://localhost:" + SERVER_PORT;
    protected static MCPServer serverApp;
    protected static Thread serverThread;
    protected static String authToken;
    private static HttpServer httpServer;

    @BeforeAll
    public static void startServer() throws Exception {
        // Signals that serverApp has been fully configured on the background thread, so the
        // main thread never observes a half-initialized instance. A bare socket-connect check
        // is not sufficient: it establishes no happens-before relationship with the writes
        // (setConfiguration/init/registerTool) made on the server thread, so without this latch
        // the main thread could see serverApp non-null but its configuration still null.
        CountDownLatch appReady = new CountDownLatch(1);

        serverThread = new Thread(() -> {
            try {
                Settings settings = new Settings();
                settings.set("default.base_url", "/?q=");
                settings.set("default.language", "en_US");
                settings.set("charset", "utf-8");
                settings.set("server.port", String.valueOf(SERVER_PORT));
                settings.set("jwt.secret", "pt2b7R8aNLwl1imeNU2xjh8z3BZQx8gD6JC7iTaMJD0=");
                settings.set("jwt.timezone", "GMT+8");
                JWTManager jwtManager = new JWTManager();
                jwtManager.withBase64Secret(settings.get("jwt.secret"));
                String token = jwtManager.createToken("test", new Builder(), 1800);
                settings.set(MCPSpecification.Config.AUTH_TOKEN, token);

                ApplicationManager.init(settings);
                serverApp = new MCPServer();
                serverApp.setConfiguration(settings);
                serverApp.init();

                ApplicationManager.install(serverApp);
                serverApp.registerTool(new CalculatorTool());
                appReady.countDown();

                Context serverContext = new ApplicationContext();
                serverContext.setAttribute("--server-port", String.valueOf(SERVER_PORT));
                ApplicationManager.install(new Dispatcher());
                httpServer = new HttpServer();
                ApplicationManager.install(httpServer);
                // Calling start() directly (rather than routing through
                // ApplicationManager.call("start", ...)) is deliberate: ActionRegistry never
                // replaces an existing route registration for an equal-priority path, so the
                // first HttpServer instance in the whole JVM to register "start" keeps that
                // binding forever. Going through dispatch here would make this instance that
                // permanent owner, so any later test's own HttpServer (e.g.
                // HttpServerHttpModeTest) would have its "start" calls silently routed back to
                // this one instead - which would just no-op on its already-started guard.
                httpServer.setContext(serverContext);
                httpServer.start();

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

        if (!appReady.await(30, TimeUnit.SECONDS)) {
            throw new RuntimeException("Server application did not initialize in time");
        }

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

        authToken = serverApp.getConfiguration().get(MCPSpecification.Config.AUTH_TOKEN);
        LOGGER.info("MCP Server started on port " + SERVER_PORT);
    }

    @AfterAll
    public static void stopServer() {
        // Actually stop the underlying HTTP server (not just interrupt the thread that started
        // it): ActionRegistry never replaces an existing route registration, so this same
        // HttpServer instance's "start" action stays permanently bound in the registry for the
        // rest of the JVM's life. HttpServer.start() itself no-ops on an instance whose `started`
        // flag is still true (its reentrancy guard), so unless stop() resets that flag here, any
        // later test in this JVM whose own ApplicationManager.call("start", ...) happens to
        // resolve to this same instance would silently fail to bind its own port.
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
            LOGGER.info("MCP Server stopped");
        }
    }
}
