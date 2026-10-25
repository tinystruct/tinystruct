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

import org.tinystruct.system.Configuration;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.*;

public final class LoggerConfigurer {

    private LoggerConfigurer() {}

    /**
     * Set up and configure the java.util.logging framework programmatically
     * using the properties specified in the Configuration.
     *
     * @param config The application configuration containing logging settings.
     */
    public static void setup(Configuration<String> config) {
        if (config == null) {
            return;
        }

        // 1. Check if logging is enabled
        String enabledStr = config.get("logging.enabled");
        boolean enabled = enabledStr == null || !enabledStr.trim().equalsIgnoreCase("FALSE");

        // Get the root logger
        Logger rootLogger = Logger.getLogger("");

        // Clean up any existing handlers (default JUL handlers)
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            rootLogger.removeHandler(handler);
            // Release the FileHandler lock, otherwise the next setup() falls back to "<name>.1", ".2"...
            handler.close();
        }

        if (!enabled) {
            // Disable all logging by setting level to OFF and returning
            rootLogger.setLevel(Level.OFF);
            return;
        }

        // 2. Parse and set overall log level
        String levelStr = config.get("logging.level");
        Level level = parseLevel(levelStr, Level.INFO);
        rootLogger.setLevel(level);

        // 3. Create the handlers listed in "handlers" (default: ConsoleHandler)
        String handlersStr = config.get("handlers");
        if (handlersStr == null || handlersStr.trim().isEmpty()) {
            handlersStr = ConsoleHandler.class.getName();
        }
        Set<String> handlerNames = new LinkedHashSet<>();
        for (String name : handlersStr.split("[,\\s]+")) {
            if (!name.isEmpty()) {
                handlerNames.add(name);
            }
        }
        for (String name : handlerNames) {
            Handler handler = null;
            try {
                handler = createHandler(name, config);
                handler.setLevel(parseLevel(config.get(name + ".level"), Level.ALL));
                handler.setFormatter(createFormatter(config.get(name + ".formatter"), handler instanceof ConsoleHandler));
                String encoding = config.get(name + ".encoding");
                if (encoding != null && !encoding.trim().isEmpty()) {
                    handler.setEncoding(encoding.trim());
                }
                rootLogger.addHandler(handler);
            } catch (Exception | LinkageError e) {
                if (handler != null) {
                    // Release any file/lock acquired by a partially configured handler.
                    handler.close();
                }
                System.err.println("Unable to create log handler " + name + ": " + e);
            }
        }

        // 4. Look for individual logger level overrides in the configuration
        for (String propertyName : config.propertyNames()) {
            if (propertyName.endsWith(".level") && !propertyName.equals("logging.level")) {
                String loggerName = propertyName.substring(0, propertyName.length() - ".level".length());
                if (isHandlerProperty(loggerName)) {
                    // "<handler class>.level" configures the handler, not a logger.
                    continue;
                }
                String specificLevelStr = config.get(propertyName);
                Level specificLevel = parseLevel(specificLevelStr, null);
                if (specificLevel != null) {
                    Logger.getLogger(loggerName).setLevel(specificLevel);
                }
            }
        }
    }

    private static Handler createHandler(String name, Configuration<String> config) throws Exception {
        if (name.equals(ConsoleHandler.class.getName())) {
            return new ConsoleHandler();
        }
        if (name.equals(FileHandler.class.getName())) {
            String pattern = config.get(name + ".pattern");
            if (pattern == null || pattern.trim().isEmpty()) {
                pattern = "%h/java%u.log";
            }
            pattern = pattern.trim();
            java.io.File parent = new java.io.File(pattern).getAbsoluteFile().getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            int limit = parseInt(config.get(name + ".limit"), 0);
            int count = parseInt(config.get(name + ".count"), 1);
            boolean append = !"false".equalsIgnoreCase(config.get(name + ".append"));
            return new FileHandler(pattern, limit, count, append);
        }
        return (Handler) Class.forName(name).getDeclaredConstructor().newInstance();
    }

    /**
     * Whether the given name refers to a {@link Handler} implementation, in which case
     * "&lt;name&gt;.level" is a handler setting rather than a logger level override.
     */
    private static boolean isHandlerProperty(String name) {
        try {
            return Handler.class.isAssignableFrom(Class.forName(name, false, LoggerConfigurer.class.getClassLoader()));
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static Formatter createFormatter(String className, boolean useColor) {
        // ANSI colors only make sense on the console, never in a log file.
        if (className == null || className.trim().isEmpty() || className.trim().equals(LogFormatter.class.getName())) {
            return new LogFormatter(useColor);
        }
        try {
            return (Formatter) Class.forName(className.trim()).getDeclaredConstructor().newInstance();
        } catch (Exception | LinkageError e) {
            // Unavailable on the classpath; fall back to the built-in formatter.
            System.err.println("Unable to create log formatter " + className.trim() + ", using "
                    + LogFormatter.class.getName() + ": " + e);
        }
        return new LogFormatter(useColor);
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return value == null ? defaultValue : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Map common logging level string patterns to JUL Level instances.
     */
    private static Level parseLevel(String levelStr, Level defaultLevel) {
        if (levelStr == null || levelStr.trim().isEmpty()) {
            return defaultLevel;
        }
        levelStr = levelStr.trim().toUpperCase();
        switch (levelStr) {
            case "OFF":
                return Level.OFF;
            case "SEVERE":
            case "ERROR":
                return Level.SEVERE;
            case "WARNING":
            case "WARN":
                return Level.WARNING;
            case "INFO":
                return Level.INFO;
            case "CONFIG":
                return Level.CONFIG;
            case "FINE":
            case "DEBUG":
                return Level.FINE;
            case "FINER":
                return Level.FINER;
            case "FINEST":
            case "TRACE":
                return Level.FINEST;
            case "ALL":
                return Level.ALL;
            default:
                try {
                    return Level.parse(levelStr);
                } catch (IllegalArgumentException e) {
                    return defaultLevel;
                }
        }
    }
}
