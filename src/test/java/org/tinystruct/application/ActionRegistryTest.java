package org.tinystruct.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.AbstractApplication;

import static org.junit.jupiter.api.Assertions.*;

public class ActionRegistryTest {

    private ActionRegistry registry;
    private TestApp app;

    @BeforeEach
    public void setUp() {
        registry = ActionRegistry.getInstance();
        app = new TestApp();
        // Register actions for testing
        registry.set(app, "api/children/{id}/subjects", "getSubjects");
        registry.set(app, "api/users", "lookupUsers"); // Changed name to avoid conflicts if any
        registry.set(app, "say", "say");
    }

    @Test
    public void testPlaceholderAction() {
        Action action = registry.getAction("api/children/123/subjects");
        assertNotNull(action, "Action for api/children/123/subjects should not be null");
        assertEquals("getSubjects", action.getMethod());
    }

    @Test
    public void testLegacyActionMatching0() {
        // This should still work as before (appending parameter)
        Action action = registry.getAction("say/Praise the Lord!");
        assertNotNull(action, "Action for `say/Praise the Lord!` should not be null");
        assertEquals("say", action.getMethod());
    }

    @Test
    public void testLegacyActionMatching() {
        // This should still work as before (appending parameter)
        Action action = registry.getAction("api/users/456");
        assertNotNull(action, "Action for api/users/456 should not be null");
        assertEquals("lookupUsers", action.getMethod());
    }

    @Test
    public void testPlaceholderWithMultipleParameters() {
        registry.set(app, "api/{category}/{id}/details", "getDetails");
        Action action = registry.getAction("api/books/789/details");
        assertNotNull(action);
        assertEquals("getDetails", action.getMethod());
    }

    @Test
    public void testManualRegexPath() {
        // Original behavior allowed users to put regex directly in the path
        registry.set(app, "api/v\\d+/users", "lookupUsers");
        Action action = registry.getAction("api/v1/users/123");
        assertNotNull(action, "Manual regex api/v\\d+/users should still work");
        assertEquals("lookupUsers", action.getMethod());
    }

    public static class TestApp extends AbstractApplication {
        @Override
        public void init() {
        }

        @org.tinystruct.system.annotation.Action(value = "api/children/{id}/subjects", arguments = {
                @org.tinystruct.system.annotation.Argument(key = "id", description = "id")})
        public String getSubjects(int id) {
            return "subjects for " + id;
        }

        @org.tinystruct.system.annotation.Action(value = "api/users", arguments = {
                @org.tinystruct.system.annotation.Argument(key = "id", description = "id")})
        public String lookupUsers(int id) {
            return "user " + id;
        }

        @org.tinystruct.system.annotation.Action(value = "api/{category}/{id}/details", arguments = {
                @org.tinystruct.system.annotation.Argument(key = "category", description = "category"),
                @org.tinystruct.system.annotation.Argument(key = "id", description = "id")
        })
        public String getDetails(String category, int id) {
            return category + ":" + id;
        }

        @org.tinystruct.system.annotation.Action("say")
        public void say(String name) {
            System.out.println("Hello, " + name);
        }

        @Override
        public String version() {
            return "1.0";
        }
    }
}
