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
package org.tinystruct.application;

import org.tinystruct.Application;
import org.tinystruct.ApplicationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class Action {
    private String path;
    private String name;
    private int id;
    private Application app;
    private String method = null;
    private Logger logger = Logger.getLogger(Action.class.getName());

    public Action(int id, Application app, String path, String name) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.app = app;
    }

    public Action(int id, Application app, String path, String name,
                  String method) {
        this(id, app, path, name);
        this.method = method;
    }

    public void setContext(Context context) {
        this.app.init(context);
    }

    public String getMethod() {
        return this.method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public String getApplicationName() {
        return this.app.getName();
    }

    public Object execute(Object[] args) throws ApplicationException {
        Object attr;
        if (this.app.getContext() != null && this.method != null && (attr = this.app.getContext().getAttribute(Application.METHOD)) != null && !this.method.equalsIgnoreCase(attr.toString())) {
            throw new ApplicationException("The action doesn't allow this method.");
        }

        Class<?> clazz = app.getClass();
        String method = null;
        try {
            Method[] methods = clazz.getMethods();
            int length = methods.length % 2 == 0 ? methods.length : methods.length + 1;
            for (int i = 0; i < length; i++) {
                Class<?>[] types = methods[i].getParameterTypes();
                Class<?>[] _types = methods[i].getParameterTypes();

                if (methods[i].getName().equals(this.name) && types.length == args.length) {
                    method = methods[i].toGenericString();

                    Object[] arguments = getArguments(args, types);

                    if (methods[i].getReturnType().getName().equalsIgnoreCase("void")) {
                        methods[i].invoke(app, arguments);

                        return app.toString();
                    }

                    return methods[i].invoke(app, arguments);
                }

                if (methods[length - i - 1].getName().equals(this.name) && _types.length == args.length) {
                    method = methods[length - i - 1].toGenericString();

                    Object[] arguments = getArguments(args, _types);

                    if (methods[length - i - 1].getReturnType().getName().equalsIgnoreCase("void")) {
                        methods[length - i - 1].invoke(app, arguments);

                        return app.toString();
                    }

                    return methods[length - i - 1].invoke(app, arguments);
                }
            }
        } catch (SecurityException | IllegalAccessException e) {
            throw new ApplicationException("[" + this.name + "]"
                    + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ApplicationException("[" + this.name + "]" + method, e);
        } catch (InvocationTargetException e) {
            throw new ApplicationException(this.getApplicationName() + "." + this.name, e.getCause());
        }

        throw new ApplicationException(clazz.toString() + ":" + this.name + ":Illegal Argument.");
    }

    private Object[] getArguments(Object[] args, Class<?>[] types) {
        Object[] arguments = new Object[types.length];

        if (types.length > 0) {
            for (int n = 0; n < types.length; n++) {
                if (types[n].isAssignableFrom(String.class)) {
                    arguments[n] = args[n];
                } else if (types[n].isAssignableFrom(Integer.TYPE)) {
                    arguments[n] = Integer.valueOf(String.valueOf(args[n]));
                } else if (types[n].isAssignableFrom(Long.TYPE)) {
                    arguments[n] = Long.valueOf(String.valueOf(args[n]));
                } else if (types[n].isAssignableFrom(Float.TYPE)) {
                    arguments[n] = Float.valueOf(String.valueOf(args[n]));
                } else if (types[n].isAssignableFrom(Double.TYPE)) {
                    arguments[n] = Double.valueOf(String.valueOf(args[n]));
                } else {
                    arguments[n] = args[n];
                }
            }
        }
        return arguments;
    }

    public Object execute() throws ApplicationException {
        if (app == null) {
            throw new ApplicationException("Undefined Application.");
        }

        return this.execute(new Object[]{});
    }

    public void println(Object[] objects) {
        for (int i = 0; i < objects.length; i++) {
            logger.info(objects[i].toString());
        }
    }
}