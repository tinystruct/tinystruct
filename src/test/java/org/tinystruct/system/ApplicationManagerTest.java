package org.tinystruct.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;

class ApplicationManagerTest {
    @Test
    void testInstall() {
        Dispatcher dispatcher = new Dispatcher();
        ApplicationManager.install(dispatcher);
        assertEquals("org.tinystruct.system.Dispatcher", dispatcher.getName());
        assertTrue(dispatcher.getConfiguration() instanceof Settings);
    }

    @Test
    void testInstall2() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setAction("Action", "Function", "Method");
        ApplicationManager.install(dispatcher);
        assertEquals("org.tinystruct.system.Dispatcher", dispatcher.getName());
        assertTrue(dispatcher.getConfiguration() instanceof Settings);
    }

    @Test
    void testUninstall() {
        assertTrue(ApplicationManager.uninstall(new Dispatcher()));
        assertFalse(ApplicationManager.uninstall(new MQTTServer()));
    }

    @Test
    void testUninstall2() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setAction("Action", "Function", "Method");
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
}

