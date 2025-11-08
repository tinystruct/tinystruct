package org.tinystruct.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Action;
import org.tinystruct.application.ActionRegistry;
import org.tinystruct.http.Method;

import static org.junit.jupiter.api.Assertions.*;

public class HttpServerHttpModeTest {

    private TestWebApp app;
    private ActionRegistry registry;

    @BeforeEach
    public void setup() throws ApplicationException {
        this.app = new TestWebApp();

        this.registry = ActionRegistry.getInstance();
        // Initialize configuration to trigger annotation processing
        this.app.setConfiguration(new Settings());
        // Ensure annotations are processed
        new AnnotationProcessor(this.app).processActionAnnotations();
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
    public void testHttpGetRouting() throws ApplicationException {
        // Test that GET requests match HTTP_GET mode actions
        Action action = registry.getAction("api/users", Action.Mode.HTTP_GET);
        assertNotNull(action, "GET action should be found");
        assertEquals(Action.Mode.HTTP_GET, action.getMode());
        
        Object result = action.execute();
        assertEquals("GET users", String.valueOf(result));
    }

    @Test
    public void testHttpPostRouting() throws ApplicationException {
        // Test that POST requests match HTTP_POST mode actions
        Action action = registry.getAction("api/users", Action.Mode.HTTP_POST);
        assertNotNull(action, "POST action should be found");
        assertEquals(Action.Mode.HTTP_POST, action.getMode());
        
        Object result = action.execute();
        assertEquals("POST users", String.valueOf(result));
    }

    @Test
    public void testHttpPutRouting() throws ApplicationException {
        // Test that PUT requests match HTTP_PUT mode actions
        // Note: The path pattern "api/users" with a String parameter will match "api/users/123"
        Action action = registry.getAction("api/users/123", Action.Mode.HTTP_PUT);
        assertNotNull(action, "PUT action should be found");
        assertEquals(Action.Mode.HTTP_PUT, action.getMode());
        
        // The action will extract "123" from the path
        Object result = action.execute();
        assertEquals("PUT user 123", String.valueOf(result));
    }

    @Test
    public void testHttpDeleteRouting() throws ApplicationException {
        // Test that DELETE requests match HTTP_DELETE mode actions
        // Note: The path pattern "api/users" with a String parameter will match "api/users/123"
        Action action = registry.getAction("api/users/123", Action.Mode.HTTP_DELETE);
        assertNotNull(action, "DELETE action should be found");
        assertEquals(Action.Mode.HTTP_DELETE, action.getMode());
        
        // The action will extract "123" from the path
        Object result = action.execute();
        assertEquals("DELETE user 123", String.valueOf(result));
    }

    @Test
    public void testHttpMethodMismatch() {
        // Test that wrong HTTP method doesn't match
        Action getAction = registry.getAction("api/users", Action.Mode.HTTP_GET);
        assertNotNull(getAction, "GET action should exist");
        
        // POST mode should not match GET action
        Action postAction = registry.getAction("api/users", Action.Mode.HTTP_POST);
        assertNotNull(postAction, "POST action should exist");
        assertNotEquals(getAction.getMode(), postAction.getMode());
    }

    @Test
    public void testDefaultModeAcceptsAllMethods() throws ApplicationException {
        // Test that DEFAULT mode actions accept any HTTP method
        Action getAction = registry.getAction("api/ping", Action.Mode.HTTP_GET);
        Action postAction = registry.getAction("api/ping", Action.Mode.HTTP_POST);
        Action putAction = registry.getAction("api/ping", Action.Mode.HTTP_PUT);
        
        // All should match the same DEFAULT mode action
        assertNotNull(getAction);
        assertNotNull(postAction);
        assertNotNull(putAction);
        
        // They should all return the same result
        assertEquals("pong", String.valueOf(getAction.execute()));
        assertEquals("pong", String.valueOf(postAction.execute()));
        assertEquals("pong", String.valueOf(putAction.execute()));
    }

    @Test
    public void testApplicationManagerCallWithMode() throws ApplicationException {
        // Test that ApplicationManager.call correctly uses mode
        ApplicationContext context = new ApplicationContext();
        
        // GET request
        Object getResult = ApplicationManager.call("api/users", context, Action.Mode.HTTP_GET);
        assertEquals("GET users", String.valueOf(getResult));
        
        // POST request
        Object postResult = ApplicationManager.call("api/users", context, Action.Mode.HTTP_POST);
        assertEquals("POST users", String.valueOf(postResult));
        
        // PUT request with parameter (path includes the ID)
        Object putResult = ApplicationManager.call("api/users/456", context, Action.Mode.HTTP_PUT);
        assertTrue(String.valueOf(putResult).contains("PUT user"));
    }

    @Test
    public void testHttpMethodExtractionFromRequest() {
        // Test that HTTP method can be extracted from Method enum
        Method getMethod = Method.GET;
        Action.Mode mode = Action.Mode.fromName(getMethod.name());
        assertEquals(Action.Mode.HTTP_GET, mode);
        
        Method postMethod = Method.POST;
        mode = Action.Mode.fromName(postMethod.name());
        assertEquals(Action.Mode.HTTP_POST, mode);
    }

    public class TestWebApp extends AbstractApplication {
        @Override
        public void init() {
            this.setTemplateRequired(false);
        }

        @org.tinystruct.system.annotation.Action(
            value = "api/users",
            description = "Get users",
            mode = Action.Mode.HTTP_GET
        )
        public String getUsers() {
            return "GET users";
        }

        @org.tinystruct.system.annotation.Action(
            value = "api/users",
            description = "Create user",
            mode = Action.Mode.HTTP_POST
        )
        public String createUser() {
            return "POST users";
        }

        @org.tinystruct.system.annotation.Action(
            value = "api/users",
            description = "Update user",
            mode = Action.Mode.HTTP_PUT
        )
        public String updateUser(String id) {
            return "PUT user " + (id != null ? id : "unknown");
        }

        @org.tinystruct.system.annotation.Action(
            value = "api/users",
            description = "Delete user",
            mode = Action.Mode.HTTP_DELETE
        )
        public String deleteUser(String id) {
            return "DELETE user " + (id != null ? id : "unknown");
        }

        @org.tinystruct.system.annotation.Action(
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

