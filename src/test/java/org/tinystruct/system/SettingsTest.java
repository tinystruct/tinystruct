package org.tinystruct.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationRuntimeException;

class SettingsTest {
    @Test
    void testConstructor() {
        Settings settings = new Settings("foo.properties");
        settings.set("util", "");
        settings.set("ApplicationManagerTest.class", "");
        settings.saveProperties();
        assertEquals("{util=, ApplicationManagerTest.class=}", settings.toString());
    }

    @Test
    void testConstructor2() {
        Settings actualSettings = new Settings("foo.properties");
        assertFalse(actualSettings.getProperties().isEmpty());
        assertEquals("{util=, ApplicationManagerTest.class=, Key=42}", actualSettings.toString());
        assertFalse(actualSettings.isEmpty());
    }

    @Test
    void testConstructor4() throws ApplicationRuntimeException {
        Settings actualSettings = new Settings("foo.properties");
        assertEquals(3, actualSettings.getProperties().size());
        assertEquals("{util=, ApplicationManagerTest.class=, Key=42}", actualSettings.toString());
        assertFalse(actualSettings.isEmpty());
    }

    @Test
    void testPropertyNames() {
        Set<String> actualPropertyNamesResult = (new Settings("foo.properties")).propertyNames();
        assertEquals(3, actualPropertyNamesResult.size());
        assertTrue(actualPropertyNamesResult.contains("util"));
        assertTrue(actualPropertyNamesResult.contains("ApplicationManagerTest.class"));
    }

    @Test
    void testUpdate() throws IOException {
        // TODO: This test is incomplete.
        //   Reason: No meaningful assertions found.
        //   Diffblue Cover was unable to create an assertion.
        //   Make sure that fields modified by update()
        //   have package-private, protected, or public getters.
        //   See https://diff.blue/R004 to resolve this issue.

        (new Settings("foo.properties")).saveProperties();
    }

    @Test
    void testIsEmpty() {
        assertFalse((new Settings("foo.properties")).isEmpty());
    }

    @Test
    void testGet() {
        assertEquals("", (new Settings("foo.properties")).get("Property"));
    }

    @Test
    void testSet() {
        // TODO: This test is incomplete.
        //   Reason: No meaningful assertions found.
        //   Diffblue Cover was unable to create an assertion.
        //   Make sure that fields modified by set(String, String)
        //   have package-private, protected, or public getters.
        //   See https://diff.blue/R004 to resolve this issue.

        (new Settings("foo.properties")).set("Key", "42");
    }

    @Test
    void testRemove() {
        // TODO: This test is incomplete.
        //   Reason: No meaningful assertions found.
        //   Diffblue Cover was unable to create an assertion.
        //   Make sure that fields modified by remove(String)
        //   have package-private, protected, or public getters.
        //   See https://diff.blue/R004 to resolve this issue.

        (new Settings("foo.properties")).remove("Key");
    }
}

