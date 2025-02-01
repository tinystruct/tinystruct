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

import org.tinystruct.ApplicationException;

import java.io.*;
import java.util.logging.Logger;

/**
 * ClassFileLoader loads class files dynamically from the specified path.
 * It extends ClassLoader to provide custom class loading functionality.
 */
public final class ClassFileLoader extends ClassLoader {

    // Logger for logging messages
    private static final Logger logger = Logger.getLogger(ClassFileLoader.class.getName());

    // Configuration settings for the class loader
    private final Configuration<String> config;

    // Private constructor to prevent direct instantiation
    private ClassFileLoader() {
        // Initialize configuration settings from application.properties
        config = new Settings("application.properties");
    }

    /**
     * Get an instance of ClassFileLoader using the Singleton pattern.
     *
     * @return An instance of ClassFileLoader.
     */
    public static ClassFileLoader getInstance() {
        return SingletonHolder.loader;
    }

    /**
     * Load a class given its fully qualified name.
     *
     * @param name The fully qualified name of the class.
     * @return The loaded class.
     */
    @Override
    public Class<?> findClass(String name) {
        // Get default package and directory from configuration
        String appsPackage = config.get("default.apps.package");
        String appsPackageDir = config.get("default.apps.path") + "/" + appsPackage.replaceAll("\\.", "/");
        StringBuilder path = new StringBuilder();

        // Check if the path starts with '/', if not, use system directory
        if (config.get("default.apps.path").startsWith("/")) {
            path.append(appsPackageDir);
        } else {
            path.append(config.get("system.directory")).append("/").append(appsPackageDir);
        }

        // Append the class name to the path
        path.append("/").append(name);

        // If the name does not end with ".class", append it
        if (!name.endsWith(".class")) {
            path.append(".class");
        } else {
            name = name.replace(".class", "");
        }

        try {
            // Get bytes from the class file
            byte[] data = getBytes(path.toString());
            // Define and return the class
            return defineClass(appsPackage + "." + name, data, 0, data.length);
        } catch (ApplicationException e) {
            logger.severe(e.getMessage());
        }

        return null;
    }

    /**
     * Load a class given its path and simple class name.
     *
     * @param path            The path where the class file is located.
     * @param simpleClassName The simple class name.
     * @return The loaded class.
     */
    @Override
    public Class<?> findClass(String path, String simpleClassName) {
        // Set the default apps path to the provided path
        config.set("default.apps.path", path);
        // Call the findClass method with the simple class name
        return findClass(simpleClassName);
    }

    /**
     * Read bytes from a file.
     *
     * @param filename The name of the file.
     * @return The byte array read from the file.
     * @throws ApplicationException If an error occurs while reading the file.
     */
    private byte[] getBytes(String filename) throws ApplicationException {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename))) {
            // Get the length of the file
            long len = new File(filename).length();
            // Create a byte array to hold the file content
            byte[] raw = new byte[(int) len];
            // Read bytes from the file
            int bytesRead = in.read(raw);

            // Check if all bytes are read
            if (bytesRead != len) {
                throw new ApplicationException("Cannot load the class file bytes completely, " + bytesRead + " != " + len);
            }
            return raw;
        } catch (IOException e) {
            // Throw ApplicationException if an I/O error occurs
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    // SingletonHolder pattern to ensure only one instance is created
    private static class SingletonHolder {
        static final ClassFileLoader loader = new ClassFileLoader();
    }
}