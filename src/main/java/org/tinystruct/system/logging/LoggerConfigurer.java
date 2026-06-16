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

        // 3. Create our custom LogFormatter and ConsoleHandler
        LogFormatter formatter = new LogFormatter();
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);
        consoleHandler.setLevel(Level.ALL); // Let the individual loggers/root logger control level filtering
        rootLogger.addHandler(consoleHandler);

        // 4. Look for individual logger level overrides in the configuration
        for (String propertyName : config.propertyNames()) {
            if (propertyName.endsWith(".level") && !propertyName.equals("logging.level")) {
                String loggerName = propertyName.substring(0, propertyName.length() - ".level".length());
                String specificLevelStr = config.get(propertyName);
                Level specificLevel = parseLevel(specificLevelStr, null);
                if (specificLevel != null) {
                    Logger.getLogger(loggerName).setLevel(specificLevel);
                }
            }
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
