package org.tinystruct;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AbstractApplicationLocaleTest {

    private static class DummyApplication extends AbstractApplication {
        @Override
        public void init() {
        }

        @Override
        public String version() {
            return "1.0";
        }
    }

    @Test
    public void testSetLocaleWithoutUnderscore() {
        DummyApplication app = new DummyApplication();
        app.setTemplateRequired(false);
        // Passing a locale string without an underscore (e.g., "en") previously threw ArrayIndexOutOfBoundsException
        app.setLocale("en");
        
        assertNotNull(app.getLocale());
        assertEquals("en", app.getLocale().getLanguage());
    }
}
