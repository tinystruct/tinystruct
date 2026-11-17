package org.tinystruct;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.system.Settings;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractApplicationTest {

    private ClassLoader originalClassLoader;

    public static class DummyApp extends AbstractApplication {
        public DummyApp() {
            super();
            setTemplateName("Dummy");
            setConfiguration(new Settings());
        }

        @Override
        public void init() {
        }

        @Override
        public String version() {
            return "1.0";
        }
    }

    @BeforeEach
    public void setUp() {
        originalClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @AfterEach
    public void tearDown() {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
    }

    @Test
    public void testToStringWithoutTemplateRequired() {
        DummyApp app = new DummyApp();
        app.setTemplateRequired(false);
        String str = app.toString();
        assertTrue(str.startsWith(app.getName() + "@"));
    }

    @Test
    public void testToStringWithOutput() {
        DummyApp app = new DummyApp();
        app.setTemplateRequired(true);
        app.setOutput("Hello {%name%}");
        app.setVariable("name", "World");

        assertEquals("Hello World", app.toString());
    }

    @Test
    public void testToStringMissingTemplateThrowsExceptionWithForwardSlash() {
        DummyApp app = new DummyApp();
        app.setTemplateRequired(true);

        ApplicationRuntimeException ex = assertThrows(ApplicationRuntimeException.class, app::toString);
        assertTrue(ex.getMessage().contains("themes/Dummy.view"), "Error message should use forward slash '/' for template path: " + ex.getMessage());
        assertFalse(ex.getMessage().contains("themes\\"), "Error message should not use backslash: " + ex.getMessage());
    }

    @Test
    public void testToStringLoadsTemplateFromContextClassLoader() {
        DummyApp app = new DummyApp();
        app.setTemplateRequired(true);
        app.setVariable("greeting", "Hello");

        Map<String, String> resources = new HashMap<>();
        resources.put("themes/Dummy.view", "<!DOCTYPE html><html><body>{%greeting%}</body></html>");

        ClassLoader mockLoader = new ClassLoader(originalClassLoader) {
            @Override
            public InputStream getResourceAsStream(String name) {
                if (resources.containsKey(name)) {
                    return new ByteArrayInputStream(resources.get(name).getBytes(StandardCharsets.UTF_8));
                }
                return super.getResourceAsStream(name);
            }
        };

        Thread.currentThread().setContextClassLoader(mockLoader);

        String result = app.toString();
        assertNotNull(result);
        assertTrue(result.contains("Hello"));
    }

    @Test
    public void testToStringLoadsChineseLocalizedTemplate() {
        DummyApp app = new DummyApp();
        app.setTemplateRequired(true);
        app.setLocale("zh_CN");

        Map<String, String> resources = new HashMap<>();
        resources.put("themes/Dummy_zh_CN.view", "<!DOCTYPE html><html><body>Chinese View</body></html>");
        resources.put("themes/Dummy.view", "<!DOCTYPE html><html><body>Default View</body></html>");

        ClassLoader mockLoader = new ClassLoader(originalClassLoader) {
            @Override
            public InputStream getResourceAsStream(String name) {
                if (resources.containsKey(name)) {
                    return new ByteArrayInputStream(resources.get(name).getBytes(StandardCharsets.UTF_8));
                }
                return super.getResourceAsStream(name);
            }
        };

        Thread.currentThread().setContextClassLoader(mockLoader);

        String result = app.toString();
        assertNotNull(result);
        assertTrue(result.contains("Chinese View"), "Should load themes/Dummy_zh_CN.view when locale is zh_CN");
    }

    @Test
    public void testToStringFallsBackToDefaultTemplateWhenLocalizedNotFound() {
        DummyApp app = new DummyApp();
        app.setTemplateRequired(true);
        app.setLocale("fr_FR");

        Map<String, String> resources = new HashMap<>();
        resources.put("themes/Dummy.view", "<!DOCTYPE html><html><body>Default View</body></html>");

        ClassLoader mockLoader = new ClassLoader(originalClassLoader) {
            @Override
            public InputStream getResourceAsStream(String name) {
                if (resources.containsKey(name)) {
                    return new ByteArrayInputStream(resources.get(name).getBytes(StandardCharsets.UTF_8));
                }
                return super.getResourceAsStream(name);
            }
        };

        Thread.currentThread().setContextClassLoader(mockLoader);

        String result = app.toString();
        assertNotNull(result);
        assertTrue(result.contains("Default View"), "Should fall back to themes/Dummy.view when localized template is missing");
    }
}
