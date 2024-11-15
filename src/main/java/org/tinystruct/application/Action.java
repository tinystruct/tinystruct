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

/**
 * Action represents a method mapped to a specific URL pattern in an application.
 */
public class Action implements org.tinystruct.application.Method<Object> {
    public static final int MAX_ARGUMENTS = 10;
    private static final Logger logger = Logger.getLogger(Action.class.getName());
    private final int id;
    private final Pattern pattern;
    private final Application app;
    private final Method method;
    private Mode mode;
    private String pathRule;
    private Object[] args = new Object[]{};

    /**
     * Constructor for Action.
     *
     * @param id       The unique identifier for the action.
     * @param app      The associated Application instance.
     * @param pathRule The URL pattern associated with the action.
     * @param method   The method to be executed.
     */
    public Action(int id, Application app, String pathRule, Method method) {
        this.id = id;
        this.app = app;
        this.pathRule = pathRule;
        this.method = method;
        this.pattern = Pattern.compile(pathRule);
        this.mode = Mode.All;
    }

    public Action(Action action) {
        this.id = action.getId();
        this.app = action.app;
        this.pathRule = action.getPathRule();
        this.method = action.method;
        this.pattern = action.getPattern();
        this.mode = action.getMode();
        this.args = new Object[]{};
    }

    /**
     * Constructor for Action.
     *
     * @param id       The unique identifier for the action.
     * @param app      The associated Application instance.
     * @param pathRule The URL pattern associated with the action.
     * @param method   The method to be executed.
     * @param mode     The method only be executable with the specified mode.
     */
    public Action(int id, Application app, String pathRule, Method method, Mode mode) {
        this(id, app, pathRule, method);
        this.mode = mode;
    }

    /**
     * Get the unique identifier for the action.
     *
     * @return The action's ID.
     */
    public int getId() {
        return this.id;
    }

    /**
     * Get the name of the associated application.
     *
     * @return The application name.
     */
    public String getApplicationName() {
        return this.app.getName();
    }

    /**
     * Set the context for the action.
     *
     * @param context The context to set.
     */
    public void setContext(Context context) {
        this.app.init(context);
    }

    /**
     * Get the URL pattern associated with the action.
     *
     * @return The URL pattern.
     */
    public String getPathRule() {
        return pathRule;
    }

    /**
     * Set the URL pattern associated with the action.
     *
     * @param path The URL pattern to set.
     */
    public void setPathRule(String path) {
        this.pathRule = path;
    }

    /**
     * Execute the action with the provided arguments.
     *
     * @param args The arguments to use during execution.
     * @return The result of the execution.
     * @throws ApplicationException If there is an error during execution.
     */
    @Override
    public Object execute(Object[] args) throws ApplicationException {
        if (method != null) {
            Object[] arguments = new Object[0];
            Class<?>[] types = method.getParameterTypes();
            if (types.length > 0 && args.length == types.length) {
                arguments = getArguments(args, types);
            }

            Application app;
            Context context;
            if ((context = this.app.getContext()) != null && (app = this.app.getInstance(context.getId())) != null) {
            } else {
                app = this.app;
            }

            if (method.getReturnType().isAssignableFrom(Void.TYPE)) {
                try {
                    method.invoke(app, arguments);
                    if (!app.isTemplateRequired()) return null;
                } catch (IllegalAccessException e) {
                    throw new ApplicationException(method + ":" + e.getMessage(), e.getCause());
                } catch (InvocationTargetException e) {
                    throw new ApplicationException(method + ":" + e.getTargetException().getMessage(), e.getCause());
                }

                return app.toString();
            }

            try {
                return method.invoke(app, arguments);
            } catch (IllegalAccessException e) {
                throw new ApplicationException(method + ":" + e.getMessage(), e.getCause());
            } catch (InvocationTargetException e) {
                throw new ApplicationException(method + ":" + e.getTargetException().getMessage(), e.getCause());
            }
        }

        logger.warning("Unsupported Operation.");
        throw new UnsupportedOperationException();
    }

    /**
     * Execute the action without providing specific arguments.
     *
     * @return The result of the execution.
     * @throws ApplicationException If there is an error during execution.
     */
    public Object execute() throws ApplicationException {
        if (app == null) {
            throw new ApplicationException("Undefined Application.");
        }

        if (app.getContext() != null && app.getContext().getAttribute("--help") != null) {
            return app.help();
        }

        return this.execute(this.args);
    }

    /**
     * Get the arguments for the method based on the provided arguments and parameter types.
     *
     * @param args  The provided arguments.
     * @param types The parameter types of the method.
     * @return The formatted arguments.
     */
    private Object[] getArguments(Object[] args, Class<?>[] types) {
        if (args.length > 0 && types.length > 0) {
            Object[] arguments = new Object[types.length];
            String format = "yyyy-MM-dd HH:mm:ss z";
            for (int n = 0; n < types.length; n++) {
                if (!types[n].isAssignableFrom(Object.class) && types[n].isAssignableFrom(Date.class)) {
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

    /**
     * Set the arguments for the action.
     *
     * @param args The arguments to set.
     */
    public void setArguments(final Object[] args) {
        this.args = args.clone();
    }

    /**
     * Get the pattern associated with the action.
     *
     * @return The pattern.
     */
    public Pattern getPattern() {
        return pattern;
    }

    public Mode getMode() {
        return mode;
    }

    public enum Mode {
        CLI,
        Web,
        All
    }
}

