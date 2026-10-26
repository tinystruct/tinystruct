package org.tinystruct.system.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tinystruct.system.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class LoggerConfigurerTest {

    @TempDir
    Path dir;

    @AfterEach
    void closeHandlers() {
        LoggerConfigurer.setup(config("logging.enabled", "FALSE"));
    }

    private static Configuration<String> config(String... keyValues) {
        Properties props = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            props.put(keyValues[i], keyValues[i + 1]);
        }
        return new Configuration<String>() {
            public void set(String name, String value) { props.put(name, value); }
            public String get(String name) { return props.getProperty(name); }
            public void remove(String name) { props.remove(name); }
            public Set<String> propertyNames() { return props.stringPropertyNames(); }
            public String getOrDefault(String name, String value) { return props.getProperty(name, value); }
            public void setIfAbsent(String name, String value) { props.putIfAbsent(name, value); }
        };
    }

    private Configuration<String> fileConfig() {
        String pattern = dir.resolve("logs").resolve("test-%g.log").toString();
        return config(
                "logging.enabled", "TRUE",
                "logging.level", "INFO",
                "handlers", "java.util.logging.ConsoleHandler, java.util.logging.FileHandler",
                "java.util.logging.FileHandler.pattern", pattern,
                "java.util.logging.FileHandler.level", "INFO",
                "java.util.logging.FileHandler.formatter", "org.tinystruct.system.logging.LogFormatter",
                "java.util.logging.FileHandler.encoding", "UTF-8");
    }

    private long countHandlers(Class<?> type) {
        long count = 0;
        for (Handler handler : Logger.getLogger("").getHandlers()) {
            if (type.isInstance(handler)) count++;
        }
        return count;
    }

    private void flush() {
        for (Handler handler : Logger.getLogger("").getHandlers()) handler.flush();
    }

    @Test
    void writesToTheConfiguredFileWithoutColorCodes() throws Exception {
        LoggerConfigurer.setup(fileConfig());
        Logger.getLogger("file.test").info("hello-file");
        flush();

        String content = Files.readString(dir.resolve("logs/test-0.log"));
        assertTrue(content.contains("hello-file"));
        assertFalse(content.contains("\u001B["), "the file must not contain ANSI colors");
    }

    @Test
    void consoleHandlerKeepsTheColorFormatter() {
        LoggerConfigurer.setup(fileConfig());
        for (Handler handler : Logger.getLogger("").getHandlers()) {
            if (handler instanceof java.util.logging.ConsoleHandler) {
                java.util.logging.LogRecord record = new java.util.logging.LogRecord(java.util.logging.Level.INFO, "colored");
                assertTrue(handler.getFormatter().format(record).contains("\u001B["));
            }
        }
    }

    @Test
    void repeatedSetupReleasesTheFileLock() throws Exception {
        for (int i = 0; i < 3; i++) {
            LoggerConfigurer.setup(fileConfig());
            Logger.getLogger("file.test").info("run" + i);
        }
        flush();

        assertEquals(1, countHandlers(java.util.logging.FileHandler.class), "old handlers must be removed");
        try (Stream<Path> files = Files.list(dir.resolve("logs"))) {
            assertEquals(1, files.filter(p -> p.toString().endsWith(".log") || p.toString().contains(".log.")).filter(p -> !p.toString().endsWith(".lck")).count(),
                    "no test-0.log.1, test-0.log.2 ... leftovers");
        }
        String content = Files.readString(dir.resolve("logs/test-0.log"));
        assertTrue(content.contains("run0") && content.contains("run1") && content.contains("run2"));
    }

    @Test
    void usesTheConsoleOnlyWhenNoHandlersAreConfigured() {
        LoggerConfigurer.setup(config("logging.enabled", "TRUE"));
        assertEquals(1, countHandlers(java.util.logging.ConsoleHandler.class));
        assertEquals(0, countHandlers(java.util.logging.FileHandler.class));
    }

    @Test
    void unknownFormatterFallsBackToTheBuiltInOne() throws Exception {
        Configuration<String> config = fileConfig();
        config.set("java.util.logging.FileHandler.formatter", "org.apache.juli.DoesNotExist");
        LoggerConfigurer.setup(config);
        Logger.getLogger("file.test").info("fallback");
        flush();
        assertTrue(Files.readString(dir.resolve("logs/test-0.log")).contains("fallback"));
    }
}
