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
package org.tinystruct.system.util;

import org.tinystruct.ApplicationException;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;

import java.io.File;

public class ClassInfo {
    private Object object;
    private Configuration<String> properties;
    private String default_application_path;

    public ClassInfo() throws ApplicationException {
        this.object = this;
        this.properties = new Settings("/application.properties");

        this.default_application_path = this.properties.get("system.directory");
        this.default_application_path = this.default_application_path.trim().length() > 0
                ? new Local(this.default_application_path).getPath()
                : new Local(getApplicationPath(this.getClass())).getPath();
    }

    public ClassInfo(Object object) throws ApplicationException {
        this.object = object;
        this.properties = new Settings("/application.properties");

        this.default_application_path = this.properties.get("system.directory");
        this.default_application_path = this.default_application_path.trim().length() > 0
                ? new Local(this.default_application_path).getPath()
                : new Local(getApplicationPath(this.getClass())).getPath();
    }

    public String getClassName() {
        return this.object.getClass().getName();
    }

    public String getSimpleName() {
        String className = this.getClassName();
        return className.substring(className.lastIndexOf('.') + 1);
    }

    public String getClassRootPath() {
        return new Local(this.default_application_path + "WEB-INF" + File.separatorChar + "classes").getPath();
    }

    public String getClassPath() {
        String className = this.getClassName().replace('.', File.separatorChar);
        className = className.substring(0, className.lastIndexOf(File.separatorChar) + 1);
        return className;
    }

    public final static String getApplicationPath(Class<?> _class) {
        return _class.getProtectionDomain().getCodeSource().getLocation().getPath();
    }

}