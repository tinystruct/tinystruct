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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Action implements org.tinystruct.application.Method<Object> {
    public static final int MAX_ARGUMENTS = 10;
    private static final Logger logger = Logger.getLogger(Action.class.getName());
    private final int id;
    private final Pattern pattern;
    private final Application app;
    private final Method method;
    private String pathRule;
    private Object[] args = new Object[]{};

    public Action(int id, Application app, String pathRule, Method method) {
        this.id = id;
        this.app = app;
        this.pathRule = pathRule;
        this.method = method;
        this.pattern = Pattern.compile(pathRule);
    }

    public int getId() {
        return this.id;
    }

    public String getApplicationName() {
        return this.app.getName();
    }

    public void setContext(Context context) {
        this.app.init(context);
    }

    public String getPathRule() {
        return pathRule;
    }

    public void setPathRule(String path) {
        this.pathRule = path;
    }

    @Override
    public Object execute(Object[] args) throws ApplicationException {
        if (method != null) {
            Object[] arguments = new Object[0];
            Class<?>[] types = method.getParameterTypes();
            if (types.length > 0 && args.length == types.length) {
                arguments = getArguments(args, types);
            }

            if (method.getReturnType().isAssignableFrom(Void.TYPE)) {
                try {
                    method.invoke(app, arguments);
                    if (!app.isTemplateRequired()) return null;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new ApplicationException(method.toGenericString() + ":" + e.getMessage(), e);
                }

                return app.toString();
            }

            try {
                return method.invoke(app, arguments);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ApplicationException(method.toGenericString() + ":" + e.getMessage(), e);
            }
        }

        logger.warning("Unsupported Operation.");
        throw new UnsupportedOperationException();
    }

    public Object execute() throws ApplicationException {
        if (app == null) {
            throw new ApplicationException("Undefined Application.");
        }

        if (app.getContext() != null && app.getContext().getAttribute("--help") != null) {
            return app.help();
        }

        return this.execute(this.args);
    }

    private Object[] getArguments(Object[] args, Class<?>[] types) {
        if (args.length > 0 && types.length > 0) {
            Object[] arguments = new Object[types.length];
            String format = "yyyy-MM-dd HH:mm:ss z";
            for (int n = 0; n < types.length; n++) {
                if (types[n].isAssignableFrom(Date.class)) {
                    String s = String.valueOf(args[n]);
                    try {
                        if (s.length() < format.length()) {
                            arguments[n] = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
                        } else {
                            arguments[n] = new SimpleDateFormat(format).parse(s);
                        }
                    } catch (ParseException e) {
                        throw new ApplicationRuntimeException(e.getMessage(), e);
                    }
                } else if (types[n].isAssignableFrom(Integer.TYPE)) {
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
                } else if (types[n].isAssignableFrom(Boolean.TYPE)) {
                    arguments[n] = Boolean.valueOf(String.valueOf(args[n]));
                } else {
                    arguments[n] = args[n];
                }
            }
            return arguments;
        }

        return args;
    }

    public void setArguments(Object[] args) {
        this.args = args;
    }

    public Pattern getPattern() {
        return pattern;
    }
}