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
import org.tinystruct.ApplicationRuntimeException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.logging.Logger;

public class Action {
    public static final int MAX_ARGUMENTS = 10;
    private String path;
    private String name;
    private int id;
    private Application app;
    private String method = null;
    private Method[] functions;
    private Logger logger = Logger.getLogger(Action.class.getName());

    public Action(int id, Application app, String path, String name) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.app = app;
        this.functions = this.getFunctions(this.name);
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

    protected Method[] getFunctions(String name) {
        Class<?> clazz = app.getClass();
        Method[] list = new Method[MAX_ARGUMENTS];
        int n = 0;
        try {
            Method[] methods = clazz.getMethods();
            for (Method value : methods) {
                if (value.getName().equals(name)) {
                    list[n++] = value;
                }
            }
        } catch (SecurityException e) {
            throw new ApplicationRuntimeException("[" + this.name + "]"
                    + e.getMessage(), e);
        }

        return list;
    }

    public Object execute(Object[] args) throws ApplicationException {
        Object attr;
        if (this.app.getContext() != null && this.method != null && (attr = this.app.getContext().getAttribute(Application.METHOD)) != null && !this.method.equalsIgnoreCase(attr.toString())) {
            throw new ApplicationException("The action doesn't allow this method.");
        }

        if (args.length > this.functions.length) {
            throw new ApplicationException(this.app.getClass().toString() + ":" + this.name + ":Illegal Argument.");
        }

        Method method = null;
        Object[] arguments = new Object[0];
        for (Method m : this.functions) {
            if (null != m) {
                try {
                    Class<?>[] types = m.getParameterTypes();
                    if (args.length == types.length) {
                        arguments = getArguments(args, types);
                        method = m;

                        break;
                    }

                } catch (SecurityException e) {
                    throw new ApplicationException("[" + this.name + "]"
                            + e.getMessage(), e);
                } catch (IllegalArgumentException e) {
                    // try to use other method with different parameter types
                }
            }
            else break;
        }

        if (method == null) throw new UnsupportedOperationException("[" + this.name + "]");
        if (method.getReturnType().isAssignableFrom(Void.TYPE)) {
            try {
                method.invoke(app, arguments);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ApplicationRuntimeException("[" + this.name + "]" + method.toGenericString(), e);
            }

            return app.toString();
        }

        try {
            return method.invoke(app, arguments);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ApplicationRuntimeException("[" + this.name + "]" + method.toGenericString(), e);
        }
    }

    private Object[] getArguments(Object[] args, Class<?>[] types) {
        if (args.length > 0 && types.length > 0) {
            Object[] arguments = new Object[types.length];
            for (int n = 0; n < types.length; n++) {
                if (types[n].isAssignableFrom(String.class)) {
                    arguments[n] = args[n];
                }
                if (types[n].isAssignableFrom(Date.class)) {
                    arguments[n] = args[n];
                }
                else if (types[n].isAssignableFrom(Integer.TYPE)) {
                    arguments[n] = Integer.valueOf(String.valueOf(args[n]));
                } else if (types[n].isAssignableFrom(Long.TYPE)) {
                    arguments[n] = Long.valueOf(String.valueOf(args[n]));
                } else if (types[n].isAssignableFrom(Float.TYPE)) {
                    arguments[n] = Float.valueOf(String.valueOf(args[n]));
                } else if (types[n].isAssignableFrom(Double.TYPE)) {
                    arguments[n] = Double.valueOf(String.valueOf(args[n]));
                } else if (types[n].isAssignableFrom(Short.TYPE)) {
                    arguments[n] = Short.valueOf(String.valueOf(args[n]));
                } else if (types[n].isAssignableFrom(Byte.TYPE)) {
                    arguments[n] = Byte.valueOf(String.valueOf(args[n]));
                }
                else {
                    arguments[n] = args[n];
                }
            }
            return arguments;
        }

        return args;
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