/*******************************************************************************
 * Copyright  (c) 2013, 2017 James Mover Zhou
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
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Get properties in the configuration file.
 *
 * @author Mover
 */
public class Settings implements Configuration<String> {

    private static final long serialVersionUID = 8348657988449703373L;
    private static final String FILE = "/application.properties";
    private static String fileName;
    private final Properties properties;
    private boolean overwrite = false;

    public Settings() {
        this(FILE);
        this.overwrite = true;
    }

    public Settings(String file) throws ApplicationRuntimeException {
        if (!file.equalsIgnoreCase(FILE)) {
            fileName = file;
        } else {
            fileName = FILE;
        }

        properties = SingletonHolder.INSTANCE.getProperties();
    }

    public Properties getProperties() {
        return properties;
    }

    public String get(String property) {
        String value = properties.getProperty(property);

        if (value != null && value.startsWith("$_")) {
            return System.getenv(value.substring(2).toUpperCase());
        }

        try {
            if (value != null) {
                byte[] bytes = value.getBytes(StandardCharsets.ISO_8859_1);
                return new String(bytes, StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ignored) {

        }

        return "";
    }

    public void set(String key, String value) {
        properties.put(key, value);
    }

    public void remove(String key) {
        properties.remove(key);
    }

    public Set<String> propertyNames() {
        HashSet<String> sets = new HashSet<String>();
        for (Object o : this.getProperties().keySet()) {
            sets.add(o.toString());
        }
        return sets;
    }

    public void update() {
        if (!this.overwrite) return;

        String comments = "Tinystruct Configuration";
        try (OutputStream out = new FileOutputStream(System.getProperty("user.dir")
                + File.separatorChar + fileName)) {
            properties.store(out, comments);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    public String toString() {
        return properties.toString();
    }

    private static final class SingletonHolder {
        public static final SingletonHolder INSTANCE = new SingletonHolder();
        private static final Properties properties = new Properties();

        static {
            try (InputStream in = SingletonHolder.class.getResourceAsStream(fileName)) {
                properties.load(in);
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