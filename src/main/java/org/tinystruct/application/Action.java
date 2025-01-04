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
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.tinystruct.http.Constants.HTTP_REQUEST;
import static org.tinystruct.http.Constants.HTTP_RESPONSE;

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

    public Action(Action action, Object[] args) {
        this.id = action.getId();
        this.app = action.app;
        this.pathRule = action.getPathRule();
        this.method = action.method;
        this.pattern = action.getPattern();
        this.mode = action.getMode();
        this.args = args;
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
            Application app;
            Context context;
            if ((context = this.app.getContext()) == null || (app = this.app.getInstance(context)) == null) {
                app = this.app;
            }

            Object[] arguments = new Object[0];
            Class<?>[] types = method.getParameterTypes();
            if (types.length > 0) {
                arguments = getArguments(args, types, context);
            }

            if (method.getReturnType().isAssignableFrom(Void.TYPE)) {
                try {
                    method.invoke(app, arguments);

                    // start destroy process.
                    app.destroy();
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
     * Converts an array of arguments to match the specified target types, using a context for special cases.
     *
     * @param args   The input arguments to convert.
     * @param types  The target types for each argument.
     * @param context The context providing additional information for certain types like Request or Response.
     * @return An array of converted arguments matching the target types.
     */
    private Object[] getArguments(Object[] args, Class<?>[] types, Context context) {
        // If no target types are specified, return the input arguments as-is.
        if (types.length == 0) {
            return args;
        }

        // Initialize the resulting arguments array.
        Object[] arguments = new Object[types.length];

        // Iterate over each target type.
        for (int n = 0; n < types.length; n++) {
            Class<?> targetType = types[n];
            Object arg = (args != null && args.length > n) ? args[n] : null;

            // Convert the argument to the required target type, if provided.
            if (arg != null) {
                arguments[n] = convertArgument(arg, targetType);
            }

            // Handle context-specific arguments like Request and Response.
            if (context != null) {
                if (targetType.isAssignableFrom(Request.class) && context.getAttribute(HTTP_REQUEST) != null) {
                    arguments[n] = context.getAttribute(HTTP_REQUEST);
                } else if (targetType.isAssignableFrom(Response.class) && context.getAttribute(HTTP_RESPONSE) != null) {
                    arguments[n] = context.getAttribute(HTTP_RESPONSE);
                }
            }
        }

        return arguments;
    }

    /**
     * Converts a single argument to the specified target type.
     *
     * @param arg        The argument to convert.
     * @param targetType The target type to convert to.
     * @return The converted argument.
     */
    private Object convertArgument(Object arg, Class<?> targetType) {
        if (arg == null) {
            return null; // Return null if the input argument is null.
        }

        String _arg = String.valueOf(arg); // Convert argument to string for parsing.
        try {
            // Handle Date type conversion.
            if (targetType.isAssignableFrom(Date.class)) {
                return parseDate(_arg);
            }
            // Handle primitive and wrapper number types.
            else if (targetType.isPrimitive() || Number.class.isAssignableFrom(targetType)) {
                return parsePrimitive(_arg, targetType);
            }
            // Handle Boolean type conversion.
            else if (targetType.isAssignableFrom(Boolean.TYPE) || targetType.isAssignableFrom(Boolean.class)) {
                return Boolean.valueOf(_arg);
            }
            // Default case: Return the argument as-is.
            else {
                return arg;
            }
        } catch (Exception e) {
            // Wrap and rethrow any conversion errors with additional context.
            throw new ApplicationRuntimeException("Error converting argument: " + _arg, e);
        }
    }

    /**
     * Parses a date string into a Date object, using appropriate date formats.
     *
     * @param dateString The date string to parse.
     * @return The parsed Date object.
     * @throws ParseException If the date string cannot be parsed.
     */
    private Date parseDate(String dateString) throws ParseException {
        String defaultDateFormat = "yyyy-MM-dd HH:mm:ss";
        String extendedDateFormat = "yyyy-MM-dd HH:mm:ss z";

        // Choose the appropriate format based on the input length.
        SimpleDateFormat formatter = dateString.length() < extendedDateFormat.length()
                ? new SimpleDateFormat(defaultDateFormat)
                : new SimpleDateFormat(extendedDateFormat);

        return formatter.parse(dateString);
    }

    /**
     * Parses a string into a primitive or wrapper type object.
     *
     * @param arg  The string representation of the argument.
     * @param targetType The target type to convert to.
     * @return The converted primitive or wrapper type object.
     */
    private Object parsePrimitive(String arg, Class<?> targetType) {
        switch (targetType.getName()) {
            case "int":
            case "java.lang.Integer":
                return Integer.valueOf(arg);
            case "long":
            case "java.lang.Long":
                return Long.valueOf(arg);
            case "float":
            case "java.lang.Float":
                return Float.valueOf(arg);
            case "double":
            case "java.lang.Double":
                return Double.valueOf(arg);
            case "short":
            case "java.lang.Short":
                return Short.valueOf(arg);
            case "byte":
            case "java.lang.Byte":
                return Byte.valueOf(arg);
            default:
                // Throw an error for unsupported primitive types.
                throw new IllegalArgumentException("Unsupported primitive type: " + targetType.getName());
        }
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

