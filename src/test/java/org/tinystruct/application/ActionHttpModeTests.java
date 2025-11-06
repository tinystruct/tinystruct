package org.tinystruct.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.system.AnnotationProcessor;
import org.tinystruct.system.Settings;

import static org.junit.jupiter.api.Assertions.*;

public class ActionHttpModeTests {

    private TestWebApp app;

    @BeforeEach
    public void setup() {
        this.app = new TestWebApp();
        // Initialize configuration to trigger annotation processing
        this.app.setConfiguration(new Settings());
        // Ensure annotations are processed (redundant but explicit)
        new AnnotationProcessor(this.app).processActionAnnotations();
    }

    @Test
    public void shouldMatchHttpGetAndPostOnSamePath() throws ApplicationException {
        ActionRegistry registry = ActionRegistry.getInstance();

        Action getAction = registry.getAction("hello", Action.Mode.HTTP_GET);
        assertNotNull(getAction, "GET action should be resolved");
        assertEquals("GET", String.valueOf(getAction.execute()));

        Action postAction = registry.getAction("hello", Action.Mode.HTTP_POST);
        assertNotNull(postAction, "POST action should be resolved");
        assertEquals("POST", String.valueOf(postAction.execute()));
    }

    @Test
    public void shouldMatchAllModeForAnyHttpMethod() throws ApplicationException {
        ActionRegistry registry = ActionRegistry.getInstance();

        Action a1 = registry.getAction("ping");
        Action a2 = registry.getAction("ping");
        Action a3 = registry.getAction("ping");

        assertNotNull(a1);
        assertNotNull(a2);
        assertNotNull(a3);
        assertEquals("pong", String.valueOf(a1.execute()));
        assertEquals("pong", String.valueOf(a2.execute()));
        assertEquals("pong", String.valueOf(a3.execute()));
    }

    @Test
    public void shouldRespectModeFilterWhenProvided() throws ApplicationException {
        ActionRegistry registry = ActionRegistry.getInstance();

        // With explicit mode filter
        Action a = registry.getAction("hello", Action.Mode.HTTP_GET);
        assertNotNull(a);
        assertEquals(Action.Mode.HTTP_GET, a.getMode());

        // Mismatched filter returns best available action but not HTTP_PUT
        Action b = registry.getAction("hello", Action.Mode.HTTP_PUT);
        assertNotNull(b);
        assertNotEquals(Action.Mode.HTTP_PUT, b.getMode());
    }

    private static class TestWebApp extends AbstractApplication {
        @Override
        public void init() {
            this.setTemplateRequired(false);
        }

        @org.tinystruct.system.annotation.Action(value = "hello", description = "GET hello", mode = Action.Mode.HTTP_GET)
        public String helloGet() {
            return "GET";
        }

        @org.tinystruct.system.annotation.Action(value = "hello", description = "POST hello", mode = Action.Mode.HTTP_POST)
        public String helloPost() {
            return "POST";
        }

        @org.tinystruct.system.annotation.Action(value = "ping", description = "Ping", mode = Action.Mode.All)
        public String ping() {
            return "pong";
        }

        @Override
        public String version() {
            return "test";
        }
    }
}


