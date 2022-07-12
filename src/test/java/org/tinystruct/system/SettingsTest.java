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
        assertEquals("{util=, ApplicationManagerTest.class=}", (new Settings()).toString());
    }

    @Test
    void testConstructor2() {
        Settings actualSettings = new Settings();
        assertFalse(actualSettings.getProperties().isEmpty());
        assertEquals("{util=, ApplicationManagerTest.class=}", actualSettings.toString());
        assertFalse(actualSettings.isEmpty());
    }

    @Test
    void testConstructor3() throws ApplicationRuntimeException {
        Settings actualSettings = new Settings("foo.properties");
        assertEquals(0, actualSettings.getProperties().size());
        assertEquals("{}", actualSettings.toString());
        assertTrue(actualSettings.isEmpty());
    }

    @Test
    void testConstructor4() throws ApplicationRuntimeException {
        Settings actualSettings = new Settings("/application.properties");
        assertEquals(2, actualSettings.getProperties().size());
        assertEquals("{util=, ApplicationManagerTest.class=}", actualSettings.toString());
        assertFalse(actualSettings.isEmpty());
    }

    @Test
    void testConstructor5() throws ApplicationRuntimeException {
        Settings actualSettings = new Settings("File Name");
        assertEquals(0, actualSettings.getProperties().size());
        assertEquals("{}", actualSettings.toString());
        assertTrue(actualSettings.isEmpty());
    }

    @Test
    void testGet() {
        assertEquals("", (new Settings()).get("Property"));
    }

    @Test
    void testSet() {
        // TODO: This test is incomplete.
        //   Reason: No meaningful assertions found.
        //   Diffblue Cover was unable to create an assertion.
        //   Make sure that fields modified by set(String, String)
        //   have package-private, protected, or public getters.
        //   See https://diff.blue/R004 to resolve this issue.

        (new Settings()).set("Key", "42");
    }

    @Test
    void testRemove() {
        // TODO: This test is incomplete.
        //   Reason: No meaningful assertions found.
        //   Diffblue Cover was unable to create an assertion.
        //   Make sure that fields modified by remove(String)
        //   have package-private, protected, or public getters.
        //   See https://diff.blue/R004 to resolve this issue.

        (new Settings()).remove("Key");
    }

    @Test
    void testPropertyNames() {
        Set<String> actualPropertyNamesResult = (new Settings("/application.properties")).propertyNames();
        assertEquals(2, actualPropertyNamesResult.size());
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

        (new Settings()).update();
    }

    @Test
    void testIsEmpty() {
        assertFalse((new Settings()).isEmpty());
    }
}

