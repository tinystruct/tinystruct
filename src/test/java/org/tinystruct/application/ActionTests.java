package org.tinystruct.application;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinystruct.AbstractApplication;
import org.tinystruct.Application;
import org.tinystruct.ApplicationException;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Settings;

import java.util.Collection;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ActionTests {
    private static final Logger log = LoggerFactory.getLogger(ActionTests.class);

    @AfterAll
    static void done() {
        log.info("Complete all test methods.");
    }

    @BeforeEach
    public void setUp() {
        ApplicationManager.install(new testApp(), new Settings());
    }

    @Test
    public void testAction() throws ApplicationException {
        Collection<Application> list = ApplicationManager.list();
        list.forEach(a -> {
            log.info(a.getName());
        });

        log.info("ApplicationManager.call(\"hi/James\", null) = {}", ApplicationManager.call("hi/James", null));

        assertEquals(String.valueOf(ApplicationManager.call("hi", null)), "Hi.");
        assertEquals(String.valueOf(ApplicationManager.call("hi/10", null)), "hi, 10");
        assertEquals(String.valueOf(ApplicationManager.call("hi/James", null)), "Hi, James");
        assertEquals(String.valueOf(ApplicationManager.call("set/2022-12-13 00:00:00", null)), "Tue Dec 13 00:00:00 CST 2022");
    }

    //    @AfterEach
    void tearDown() {
        log.info("@AfterEach - executed after each test method.");
    }

    @Test
    void shouldThrowException() {
        Throwable exception = assertThrows(UnsupportedOperationException.class, () -> {
            throw new UnsupportedOperationException("Not supported");
        });
        assertEquals(exception.getMessage(), "Not supported");
    }

    @Test
    void assertThrowsException() {
        String str = null;
        assertThrows(IllegalArgumentException.class, () -> {
            Integer.valueOf(str);
        });
    }

    private static class testApp extends AbstractApplication {
        @Override
        public void init() {
            this.setAction("hi", "hi");
            this.setAction("set", "set");
            this.setTemplateRequired(false);
        }

        public String hi() {
            return "Hi.";
        }

        public String hi(int a) {
            return "hi, " + a;
        }

        public String hi(String name) {
            return "Hi, " + name;
        }

        public Date set(Date date) {
            return date;
        }

        @Override
        public String version() {
            return null;
        }
    }

}
