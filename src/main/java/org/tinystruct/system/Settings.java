/*******************************************************************************
 * Copyright  (c) 2013, 2025 James M. ZHOU
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
        if (value == null) return "";
        value = value.trim();
        // Handle environment variables if needed
        if (value.startsWith("$_")) {
            String envName = value.substring(2).toUpperCase();
            return System.getenv(envName) != null ? System.getenv(envName) : "";
        }

        return value.trim();
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
        return this.get(key).isEmpty() ? value : this.get(key);
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
        URL resource = Settings.class.getClassLoader().getResource(this.file != null ? this.file : DEFAULT_FILE);
        if (null != resource) {
            URI uri;
            try {
                uri = resource.toURI();
            } catch (URISyntaxException e) {
                throw new ApplicationRuntimeException(e.getMessage());
            }

            String path = uri.getPath();
            if (path != null) {
                try (FileOutputStream outputStream = new FileOutputStream(path); OutputStream out = new BufferedOutputStream(outputStream)) {
                    properties.store(out, "#tinystruct configuration");
                } catch (FileNotFoundException e) {
                    throw new ApplicationRuntimeException("File not found:" + e.getMessage(), e);
                } catch (IOException e) {
                    throw new ApplicationRuntimeException("Error saving properties: " + e.getMessage(), e);
                }
            }
        }
    }

    private static final class SingletonHolder {
        public static final SingletonHolder INSTANCE = new SingletonHolder();
        private final Properties properties = new Properties();

        private Properties getProperties(String fileName) {
            try (InputStream in = Settings.class.getClassLoader().getResourceAsStream(fileName)) {
                if (in != null) {
                    try (InputStreamReader reader = new InputStreamReader(
                            in, StandardCharsets.UTF_8)) {
                        properties.load(reader);
                    }
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
