package org.tinystruct.system;

import org.junit.jupiter.api.*;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationContext;
import org.tinystruct.net.URLRequest;
import org.tinystruct.net.URLResponse;
import org.tinystruct.net.handlers.HTTPHandler;
import org.tinystruct.system.annotation.Action;

import java.net.Socket;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpServerHttpModeTest {

    private static final int TEST_PORT = 18080;
    private static final String BASE_URL = "http://localhost:" + TEST_PORT;
    private HttpServer httpServer;
    private Thread serverThread;
    private TestWebApp app;

    @BeforeAll
    public void setUp() throws Exception {
        // Initialize settings
        Settings settings = new Settings();
        settings.set("default.base_url", "/?q=");
        settings.set("default.language", "en_US");
        settings.set("charset", "utf-8");

        // Create and install test app
        this.app = new TestWebApp();
        ApplicationManager.install(this.app, settings);

        // Install required applications
        ApplicationManager.install(new Dispatcher());
        this.httpServer = new HttpServer();
        ApplicationManager.install(this.httpServer, settings);

        // Start server in a separate thread
        serverThread = new Thread(() -> {
            try {
                ApplicationContext context = new ApplicationContext();
                context.setAttribute("--server-port", String.valueOf(TEST_PORT));
                ApplicationManager.call("start", context, Action.Mode.CLI);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for server to be ready
        boolean started = false;
        for (int i = 0; i < 30; i++) {
            try (Socket socket = new Socket("localhost", TEST_PORT)) {
                started = true;
                break;
            } catch (Exception e) {
                Thread.sleep(1000);
            }
        }
        if (!started) {
            throw new RuntimeException("Server failed to start within 30 seconds");
        }
        
        // Give server a moment to fully initialize
        Thread.sleep(500);
    }

    @AfterAll
    public void tearDown() {
        if (httpServer != null) {
            httpServer.stop();
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }
    }

    @Test
    public void testActionModeFromName() {
        // Test HTTP method name to Mode mapping
        assertEquals(Action.Mode.HTTP_GET, Action.Mode.fromName("GET"));
        assertEquals(Action.Mode.HTTP_POST, Action.Mode.fromName("POST"));
        assertEquals(Action.Mode.HTTP_PUT, Action.Mode.fromName("PUT"));
        assertEquals(Action.Mode.HTTP_DELETE, Action.Mode.fromName("DELETE"));
        assertEquals(Action.Mode.HTTP_PATCH, Action.Mode.fromName("PATCH"));
        assertEquals(Action.Mode.HTTP_HEAD, Action.Mode.fromName("HEAD"));
        assertEquals(Action.Mode.HTTP_OPTIONS, Action.Mode.fromName("OPTIONS"));
        
        // Test case insensitivity
        assertEquals(Action.Mode.HTTP_GET, Action.Mode.fromName("get"));
        assertEquals(Action.Mode.HTTP_POST, Action.Mode.fromName("post"));
        
        // Test null and unknown values return DEFAULT
        assertEquals(Action.Mode.DEFAULT, Action.Mode.fromName(null));
        assertEquals(Action.Mode.DEFAULT, Action.Mode.fromName("UNKNOWN"));
    }

    @Test
    public void testHttpGetRequest() throws Exception {
        // Make actual HTTP GET request
        String response = sendHttpRequest("GET", BASE_URL + "/?q=api/users", null);
        assertTrue(response.contains("GET users"), "GET request should return 'GET users'");
    }

    @Test
    public void testHttpPostRequest() throws Exception {
        // Make actual HTTP POST request
        String response = sendHttpRequest("POST", BASE_URL + "/?q=api/users", null);
        assertTrue(response.contains("POST users"), "POST request should return 'POST users'");
    }

    @Test
    public void testHttpPutRequest() throws Exception {
        // Make actual HTTP PUT request
        String response = sendHttpRequest("PUT", BASE_URL + "/?q=api/users/123", null);
        assertTrue(response.contains("PUT user"), "PUT request should return 'PUT user'");
        assertTrue(response.contains("123"), "PUT request should include the ID parameter");
    }

    @Test
    public void testHttpDeleteRequest() throws Exception {
        // Make actual HTTP DELETE request
        String response = sendHttpRequest("DELETE", BASE_URL + "/?q=api/users/456", null);
        assertTrue(response.contains("DELETE user"), "DELETE request should return 'DELETE user'");
        assertTrue(response.contains("456"), "DELETE request should include the ID parameter");
    }

    @Test
    public void testDefaultModeAcceptsAllMethods() throws Exception {
        // Test that DEFAULT mode actions accept any HTTP method
        String getResponse = sendHttpRequest("GET", BASE_URL + "/?q=api/ping", null);
        assertTrue(getResponse.contains("pong"), "GET request to ping should return 'pong'");

        String postResponse = sendHttpRequest("POST", BASE_URL + "/?q=api/ping", null);
        assertTrue(postResponse.contains("pong"), "POST request to ping should return 'pong'");

        String putResponse = sendHttpRequest("PUT", BASE_URL + "/?q=api/ping", null);
        assertTrue(putResponse.contains("pong"), "PUT request to ping should return 'pong'");
    }

    @Test
    public void testHttpMethodMismatch() throws Exception {
        // Try to access GET endpoint with POST method - should fail or return error
        // Note: This depends on how the server handles method mismatches
        String response = sendHttpRequest("POST", BASE_URL + "/?q=api/users", null);
        // POST to api/users should work (there's a POST handler), but let's verify it's not the GET handler
        assertTrue(response.contains("POST users"), "POST request should match POST handler, not GET");
    }

    /**
     * Helper method to send HTTP requests
     */
    private String sendHttpRequest(String method, String urlString, String body) throws Exception {
        URLRequest request = new URLRequest(URI.create(urlString).toURL());
        request.setMethod(method.toUpperCase());

        if (body != null && (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT"))) {
            request.setHeader("Content-Type", "application/x-www-form-urlencoded");
            request.setBody(body);
        }

        HTTPHandler handler = new HTTPHandler();
        URLResponse response = handler.handleRequest(request);

        int statusCode = response.getStatusCode();
        String responseText = response.getBody();

        if (statusCode >= 200 && statusCode < 300) {
            return responseText;
        } else {
            // return error response text for non-2xx responses
            return responseText;
        }
    }

    @Test
    public void testHttpMethodExtractionFromRequest() {
        // Test that HTTP method can be extracted from Method enum
        org.tinystruct.http.Method getMethod = org.tinystruct.http.Method.GET;
        Action.Mode mode = Action.Mode.fromName(getMethod.name());
        assertEquals(Action.Mode.HTTP_GET, mode);
        
        org.tinystruct.http.Method postMethod = org.tinystruct.http.Method.POST;
        mode = Action.Mode.fromName(postMethod.name());
        assertEquals(Action.Mode.HTTP_POST, mode);
    }

    public class TestWebApp extends AbstractApplication {
        @Override
        public void init() {
            this.setTemplateRequired(false);
        }

        @Action(
            value = "api/users",
            description = "Get users",
            mode = Action.Mode.HTTP_GET
        )
        public String getUsers() {
            return "GET users";
        }

        @Action(
            value = "api/users",
            description = "Create user",
            mode = Action.Mode.HTTP_POST
        )
        public String createUser() {
            return "POST users";
        }

        @Action(
            value = "api/users",
            description = "Update user",
            mode = Action.Mode.HTTP_PUT
        )
        public String updateUser(String id) {
            return "PUT user " + (id != null ? id : "unknown");
        }

        @Action(
            value = "api/users",
            description = "Delete user",
            mode = Action.Mode.HTTP_DELETE
        )
        public String deleteUser(String id) {
            return "DELETE user " + (id != null ? id : "unknown");
        }

        @Action(
            value = "api/ping",
            description = "Ping endpoint",
            mode = Action.Mode.DEFAULT
        )
        public String ping() {
            return "pong";
        }

        @Override
        public String version() {
            return "test";
        }
    }
}

