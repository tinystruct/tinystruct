/*******************************************************************************
 * Copyright  (c) 2013, 2023 James Mover Zhou
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
package org.tinystruct.system;

import org.tinystruct.ApplicationRuntimeException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Settings implements Configuration<String> {
    private static final long serialVersionUID = 8348657988449703373L;
    private static final String DEFAULT_FILE = "/application.properties";
    private static String fileName;
    private final Properties properties;
    private boolean overwrite = false;

    public Settings() {
        this(DEFAULT_FILE);
        this.overwrite = true;
    }

    public Settings(String file) {
        fileName = file.equalsIgnoreCase(DEFAULT_FILE) ? DEFAULT_FILE : file;
        properties = SingletonHolder.INSTANCE.getProperties();
    }

    public Properties getProperties() {
        return properties;
    }

    @Override
    public String get(String property) {
        String value = properties.getProperty(property);

        if (value != null && value.startsWith("$_")) {
            String envVariableName = value.substring(2).toUpperCase();
            return System.getenv(envVariableName) != null ? System.getenv(envVariableName) : "";
        }

        try {
            if (value != null) {
                byte[] bytes = value.getBytes(StandardCharsets.ISO_8859_1);
                return new String(bytes, StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ignored) {
            // Ignored intentionally
        }

        return "";
    }

    @Override
    public void set(String key, String value) {
        properties.put(key, value);
    }

    @Override
    public void remove(String key) {
        properties.remove(key);
    }

    @Override
    public Set<String> propertyNames() {
        return properties.stringPropertyNames();
    }

    public void update() {
        if (!overwrite) {
            return;
        }

        String comments = "Tinystruct Configuration";
        String filePath = System.getProperty("user.dir") + File.separator + fileName;
        try (OutputStream out = new FileOutputStream(filePath)) {
            properties.store(out, comments);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public String toString() {
        return properties.toString();
    }

    private static final class SingletonHolder {
        public static final SingletonHolder INSTANCE = new SingletonHolder();
        private static final Properties properties = new Properties();
        private static final LogManager logManager = LogManager.getLogManager();
        private static final Logger logger = Logger.getLogger(Settings.class.getName());

        static {
            try (InputStream in = SingletonHolder.class.getResourceAsStream(fileName)) {
                if (null != in) {
                    properties.load(in);
                    String loggingOverride = properties.getProperty("logging.override");
                    if (loggingOverride != null && loggingOverride.equalsIgnoreCase("true")) {
                        logManager.readConfiguration(in);
                    }
                } else {
                    logger.warning("No settings loaded.");
                }
            } catch (IOException e) {
                throw new ApplicationRuntimeException(e.getMessage(), e);
            }
        }

        private SingletonHolder() {
        }

        public Properties getProperties() {
            return properties;
        }
    }
}
