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

import org.tinystruct.ApplicationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

public class ClassFileLoader extends ClassLoader {

    private static final Logger logger = Logger.getLogger("ClassFileLoader.class");

    private static final class SingletonHolder {
        static final ClassFileLoader loader = new ClassFileLoader();
    }

    ;

    private ClassFileLoader() {
    }

    public static ClassFileLoader getInstance() {
        return SingletonHolder.loader;
    }

    @Override
    public Class<?> findClass(String name) {
        Configuration<String> config = new Settings("/application.properties");
        String apps_package = config.get("default.apps.package");
        String apps_package_dir = config.get("default.apps.path") + "/" + apps_package.replaceAll("\\.", "/");
        StringBuilder path = new StringBuilder();
        if (config.get("default.apps.path").indexOf('/') == 0) {
            path.append(apps_package_dir);
        } else {
            path.append(config.get("system.directory")).append("/").append(apps_package_dir);
        }

        path.append("/").append(name);

        if (!name.endsWith(".class")) {
            path.append(".class");
        } else {
            name = name.replaceAll(".class", "");
        }

        try {
            byte[] data = getBytes(path.toString());
            return defineClass((new StringBuilder()).append(apps_package).append(".").append(name).toString(), data, 0,
                    data.length);
        } catch (ApplicationException e) {
            logger.severe(e.getMessage());
        }

        return null;
    }

    public Class<?> findClass(String path, String simpleClassName) {
        Configuration<String> config = new Settings("/application.properties");
        config.set("default.apps.path", path);
        return findClass(simpleClassName);
    }

    private byte[] getBytes(String filename) throws ApplicationException {
        File file = new File(filename);
        long len = file.length();
        byte raw[] = new byte[(int) len];
        try (FileInputStream in = new FileInputStream(file)) {
            int r = in.read(raw);

            if (r != len) {
                throw new ApplicationException("Cannot load the class file bytes completely, " + r + " != " + len);
            }
        } catch (FileNotFoundException e) {
            throw new ApplicationException(e.getMessage(), e);
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }

        return raw;
    }

}