/*******************************************************************************
 * Copyright  (c) 2013, 2026 James M. ZHOU
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.tinystruct.system.logging;

import org.junit.jupiter.api.Test;
import org.tinystruct.system.Configuration;

import java.time.Instant;
import java.util.Properties;
import java.util.Set;
import java.util.logging.*;

import static org.junit.jupiter.api.Assertions.*;

class LoggerTest {

    @Test
    void testLogFormatterFormat() {
        LogFormatter formatter = new LogFormatter(false); // disable colors for exact assertion
        LogRecord record = new LogRecord(Level.INFO, "Test log message");
        record.setSourceClassName("org.tinystruct.system.logging.LoggerTest");
        record.setSourceMethodName("testLogFormatterFormat");
        record.setInstant(Instant.ofEpochMilli(System.currentTimeMillis()));

        String formatted = formatter.format(record);
        assertNotNull(formatted);
        assertTrue(formatted.contains(Level.INFO.getLocalizedName()), "Formatted message should contain log level");
        assertTrue(formatted.contains("Test log message"), "Formatted message should contain the log message");
        assertTrue(formatted.contains("LoggerTest.testLogFormatterFormat"), "Formatted message should contain class and method source");
    }

    @Test
    void testLogFormatterWithThrowable() {
        LogFormatter formatter = new LogFormatter(false);
        LogRecord record = new LogRecord(Level.SEVERE, "Exception occurred");
        Exception ex = new RuntimeException("Test runtime exception");
        record.setThrown(ex);

        String formatted = formatter.format(record);
        assertNotNull(formatted);
        assertTrue(formatted.contains("RuntimeException"), "Formatted message should contain exception class");
        assertTrue(formatted.contains("Test runtime exception"), "Formatted message should contain exception message");
    }

    @Test
    void testLoggerConfigurerDisable() {
        // Mock a Configuration where logging is disabled
        Configuration<String> mockConfig = new Configuration<String>() {
            @Override
            public void set(String name, String value) {}

            @Override
            public String get(String name) {
                if ("logging.enabled".equals(name)) {
                    return "FALSE";
                }
                return "";
            }

            @Override
            public void remove(String name) {}

            @Override
            public Set<String> propertyNames() {
                return Set.of("logging.enabled");
            }

            @Override
            public String getOrDefault(String s, String value) {
                return get(s).isEmpty() ? value : get(s);
            }

            @Override
            public void setIfAbsent(String s, String path) {}
        };

        LoggerConfigurer.setup(mockConfig);
        Logger rootLogger = Logger.getLogger("");
        assertEquals(Level.OFF, rootLogger.getLevel(), "Root logger should be set to OFF when logging is disabled");
    }

    @Test
    void testLoggerConfigurerEnableAndLevels() {
        // Mock a Configuration where logging is enabled with specific overrides
        Configuration<String> mockConfig = new Configuration<String>() {
            private final Properties props = new Properties();
            {
                props.put("logging.enabled", "TRUE");
                props.put("logging.level", "DEBUG");
                props.put("org.tinystruct.test.level", "WARNING");
            }

            @Override
            public void set(String name, String value) {
                props.put(name, value);
            }

            @Override
            public String get(String name) {
                return props.getProperty(name, "");
            }

            @Override
            public void remove(String name) {
                props.remove(name);
            }

            @Override
            public Set<String> propertyNames() {
                return props.stringPropertyNames();
            }

            @Override
            public String getOrDefault(String s, String value) {
                return get(s).isEmpty() ? value : get(s);
            }

            @Override
            public void setIfAbsent(String s, String path) {
                if (!props.containsKey(s)) {
                    props.put(s, path);
                }
            }
        };

        LoggerConfigurer.setup(mockConfig);

        Logger rootLogger = Logger.getLogger("");
        assertEquals(Level.FINE, rootLogger.getLevel(), "Root logger should be set to FINE (DEBUG)");

        // Assert that custom LogFormatter was attached
        Handler[] handlers = rootLogger.getHandlers();
        assertTrue(handlers.length > 0, "Root logger should have handlers configured");
        assertInstanceOf(ConsoleHandler.class, handlers[0], "Handler should be a ConsoleHandler");
        assertInstanceOf(LogFormatter.class, handlers[0].getFormatter(), "Handler's formatter should be LogFormatter");

        // Specific package level override
        Logger specificLogger = Logger.getLogger("org.tinystruct.test");
        assertEquals(Level.WARNING, specificLogger.getLevel(), "Specific logger should have WARNING level applied");
    }
}
