package org.tinystruct.system;

import org.junit.jupiter.api.Test;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.system.annotation.Action;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationManagerTest {
    @Test
    void testInstall() {
        Dispatcher dispatcher = new Dispatcher();
        ApplicationManager.install(dispatcher, new Settings());
        assertEquals("org.tinystruct.system.Dispatcher", dispatcher.getName());
        assertTrue(dispatcher.getConfiguration() instanceof Settings);
    }

    @Test
    void testInstall2() {
        Dispatcher dispatcher = new Dispatcher();
        ApplicationManager.install(dispatcher, new Settings());
        assertEquals("org.tinystruct.system.Dispatcher", dispatcher.getName());
        assertTrue(dispatcher.getConfiguration() instanceof Settings);
    }

    @Test
    void testUninstall() {
        Dispatcher dispatcher = new Dispatcher();
        ApplicationManager.install(dispatcher, new Settings());
        assertTrue(ApplicationManager.uninstall(dispatcher));
    }

    @Test
    void testUninstall2() {
        Dispatcher dispatcher = new Dispatcher();
        ApplicationManager.install(dispatcher, new Settings());
        assertTrue(ApplicationManager.uninstall(dispatcher));
    }

    @Test
    void testUninstallRemovesAllApplicationActions() throws ApplicationException {
        MultiActionSampleApp app = new MultiActionSampleApp();
        ApplicationManager.install(app, new Settings());

        assertEquals("one", ApplicationManager.call("sample/one", null));
        assertEquals("two", ApplicationManager.call("sample/two", null));

        assertTrue(ApplicationManager.uninstall(app));
        assertNull(ApplicationManager.get(app.getName()));

        assertThrows(ApplicationException.class, () -> ApplicationManager.call("sample/one", null));
        assertThrows(ApplicationException.class, () -> ApplicationManager.call("sample/two", null));
        assertFalse(ApplicationManager.uninstall(app), "Second uninstall should return false when all actions were removed");
    }

    public static class MultiActionSampleApp extends AbstractApplication {
        @Override
        public void init() {
            this.setTemplateRequired(false);
        }

        @Override
        public String version() {
            return "1.0";
        }

        @Action("sample/one")
        public String first() {
            return "one";
        }

        @Action("sample/two")
        public String second() {
            return "two";
        }
    }

    @Test
    void testGet() {
        assertNull(ApplicationManager.get("Clsid"));
    }

    @Test
    void testList() {
        // TODO: This test is incomplete.
        //   Reason: Missing observers.
        //   Diffblue Cover was unable to create an assertion.
        //   Add getters for the following fields or make them package-private:
        //     CollectionView.map

        ApplicationManager.list();
    }

    @Test
    void testCall() throws ApplicationException {
        assertThrows(ApplicationException.class, () -> ApplicationManager.call("Path", new ApplicationContext()));
        assertThrows(ApplicationException.class, () -> ApplicationManager.call("foo", new ApplicationContext()));
        assertThrows(ApplicationException.class, () -> ApplicationManager.call(null, new ApplicationContext()));
        assertThrows(ApplicationException.class,
                () -> ApplicationManager.call("/application.properties", new ApplicationContext()));
        assertThrows(ApplicationException.class, () -> ApplicationManager.call("", new ApplicationContext()));
        assertThrows(ApplicationException.class, () -> ApplicationManager.call("Path", null));
    }

    @Test
    public void testCallMethodPerformance() throws ApplicationException {
        // Number of times to call the method
        int numCalls = 1_0;
        Dispatcher dispatcher = new Dispatcher();
        ApplicationManager.install(dispatcher, new Settings());

        // Start the stopwatch to measure time taken
        Instant start = Instant.now();

        // Perform 1,000,000 calls
        for (int i = 0; i < numCalls; i++) {
            ApplicationManager.call("say/Praise the Lord!", null, Action.Mode.CLI);
        }

        // End the stopwatch
        Instant end = Instant.now();

        // Calculate the duration in milliseconds
        long durationMillis = Duration.between(start, end).toMillis();

        // Print the duration for reference
        System.out.println("Time taken for " + numCalls + " calls: " + durationMillis + " ms");

        // Ensure that the time taken is within acceptable limits (e.g., 1 second per 1000 calls)
        // Here we assert that the test completes within the set time limit
        assertTrue(durationMillis < 60_000, "Performance test failed: took too long.");
    }

    @Test
    void testSetActionSupportsDynamicPathRegistrationAndIsNotDeprecated() throws NoSuchMethodException, ApplicationException {
        assertFalse(org.tinystruct.Application.class
                        .getMethod("setAction", String.class, String.class)
                        .isAnnotationPresent(Deprecated.class),
                "Application.setAction(String, String) supports dynamic path registration and must not be deprecated");
        assertFalse(org.tinystruct.AbstractApplication.class
                        .getMethod("setAction", String.class, String.class)
                        .isAnnotationPresent(Deprecated.class),
                "AbstractApplication.setAction(String, String) supports dynamic path registration and must not be deprecated");

        MultiActionSampleApp dynamicApp = new MultiActionSampleApp();
        ApplicationManager.install(dynamicApp);
        dynamicApp.setAction("test/dynamic/route", "first");

        assertEquals("one", ApplicationManager.call("test/dynamic/route", null));
        ApplicationManager.uninstall(dynamicApp);
    }
}
