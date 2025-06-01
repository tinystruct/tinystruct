package org.tinystruct.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationRuntimeException;

class SettingsTest {
    @Test
    void testConstructor() throws Exception {
        Path tempFile = Files.createTempFile("foo.properties", ".properties");
        Settings settings = new Settings(tempFile.toString());
        settings.set("util", "");
        settings.set("ApplicationManagerTest.class", "");
        settings.saveProperties();
        Properties props = settings.getProperties();
        assertEquals("", props.getProperty("util"));
        assertEquals("", props.getProperty("ApplicationManagerTest.class"));
        Files.deleteIfExists(tempFile);
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

