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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

public class Settings implements Configuration<String> {
    private static final long serialVersionUID = 8348657988449703373L;
    private static final String DEFAULT_FILE = "application.properties";
    private final Properties properties;
    private final String file;

    public Settings() {
        this(DEFAULT_FILE);
    }

    public Settings(String file) {
        this.file = file;
        this.properties = SingletonHolder.INSTANCE.getProperties(file);
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
        saveProperties();
    }

    @Override
    public void remove(String key) {
        properties.remove(key);
        saveProperties();
    }

    @Override
    public Set<String> propertyNames() {
        return properties.stringPropertyNames();
    }

    @Override
    public String getOrDefault(String key, String value) {
        return this.get(key).equals("") ? value : this.get(key);
    }

    @Override
    public void setIfAbsent(String key, String value) {
        if (!properties.containsKey(key))
            this.set(key, value);
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public String toString() {
        return properties.toString();
    }

    public void saveProperties() {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(Objects.requireNonNull(Settings.class.getClassLoader().getResource(this.file != null ? this.file : DEFAULT_FILE)).toURI().getPath()))) {
            properties.store(out, "Tinystruct Configuration");
        } catch (IOException e) {
            throw new ApplicationRuntimeException("Error saving properties: " + e.getMessage(), e);
        } catch (URISyntaxException e) {
            throw new ApplicationRuntimeException("Error saving properties: " + e.getMessage(), e);
        }
    }

    private static final class SingletonHolder {
        public static final SingletonHolder INSTANCE = new SingletonHolder();
        private final Properties properties = new Properties();

        private Properties getProperties(String fileName) {
            try (InputStream in = Settings.class.getClassLoader().getResourceAsStream(fileName)) {
                if (in != null) {
                    properties.load(in);
                } else {
                    Logger.getLogger(Settings.class.getName()).warning("No settings loaded.");
                }
            } catch (IOException e) {
                throw new ApplicationRuntimeException("Error loading properties: " + e.getMessage(), e);
            }
            return properties;
        }
    }
}
