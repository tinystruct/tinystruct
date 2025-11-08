package org.tinystruct.system;

import org.junit.jupiter.api.Test;
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
        assertTrue(ApplicationManager.uninstall(new Dispatcher()));
    }

    @Test
    void testUninstall2() {
        Dispatcher dispatcher = new Dispatcher();
        assertTrue(ApplicationManager.uninstall(dispatcher));
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
        int numCalls = 1_000_000;
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
}

