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
package org.tinystruct.application;

import org.tinystruct.Application;
import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
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
    private final String method;
    private final MethodHandle methodHandle;
    private final Class<?>[] parameterTypes;
    private final Class<?> returnType;
    private final int priority;
    private Mode mode;
    private String pathRule;
    private Object[] args = new Object[]{};

    /**
     * Constructs a new Action instance with the specified configuration parameters.
     *
     * <p>This constructor creates an Action that is fully defined by its unique identifier, associated
     * Application instance, URL pattern (expressed as a regular expression), method handle, method name,
     * return type, parameter types, and a priority level. The priority is used to determine the selection
     * order when multiple actions match a given URL.</p>
     *
     * @param id             A unique identifier for the action.
     * @param app            The Application instance associated with this action.
     * @param pathRule       The URL pattern (as a regular expression) that the action responds to.
     * @param methodHandle   A MethodHandle providing reflective access to the underlying method.
     * @param method         The name of the method to be executed when this action is invoked.
     * @param returnType     The return type of the underlying method.
     * @param parameterTypes An array of the parameter types expected by the method (excluding any Request/Response parameters).
     * @param priority       The priority of the action, determining its selection order when multiple actions match.
     */
    public Action(int id, Application app, String pathRule, MethodHandle methodHandle, String method, Class<?> returnType, Class<?>[] parameterTypes, int priority) {
        this.app = app;
        this.id = id;
        this.returnType = returnType;
        this.method = method;
        this.methodHandle = methodHandle;
        this.parameterTypes = parameterTypes;
        this.pathRule = pathRule;
        this.pattern = Pattern.compile(pathRule);
        this.priority = priority;
        this.mode = Mode.All;
    }

    /**
     * Creates a new Action instance based on an existing Action, applying the resolved runtime arguments.
     *
     * <p>This copy constructor is typically used during request processing. It takes an existing Action
     * (which contains all the static configuration such as the URL pattern, method handle, etc.) and combines
     * it with the actual arguments (e.g., values extracted from the URL) to produce an instance that is ready
     * for invocation.</p>
     *
     * @param action The existing Action instance to copy.
     * @param args   An array of arguments extracted from the URL or request to be passed to the method.
     */
    public Action(Action action, Object[] args) {
        this.app = action.app;
        this.args = args;
        this.id = action.getId();
        this.returnType = action.getReturnType();
        this.method = action.getMethod();
        this.methodHandle = action.getMethodHandle();
        this.mode = action.getMode();
        this.parameterTypes = action.getParameterTypes();
        this.pathRule = action.getPathRule();
        this.pattern = action.getPattern();
        this.priority = action.getPriority();
    }

    /**
     * Constructs a new Action instance with the specified configuration parameters, including a custom mode.
     *
     * <p>This constructor extends the basic Action configuration by allowing a specific execution {@code mode} to be set.
     * It leverages the primary constructor to initialize all common properties, and then overrides the default mode
     * with the provided value.</p>
     *
     * @param id             A unique identifier for the action.
     * @param app            The Application instance associated with this action.
     * @param pathRule       The URL pattern (expressed as a regular expression) that this action will respond to.
     * @param methodHandle   A MethodHandle that provides reflective access to the underlying method.
     * @param method         The name of the method to be executed when this action is invoked.
     * @param returnType     The return type of the underlying method.
     * @param parameterTypes An array representing the types of parameters expected by the method (excluding any Request/Response parameters).
     * @param priority       The priority of the action, used to determine the selection order when multiple actions match a given URL.
     * @param mode           The execution mode for the action, allowing for custom handling (e.g., distinguishing between GET, POST, etc.).
     */
    public Action(int id, Application app, String pathRule, MethodHandle methodHandle, String method, Class<?> returnType, Class<?>[] parameterTypes, int priority, Mode mode) {
        this(id, app, pathRule, methodHandle, method, returnType, parameterTypes, priority);
        this.mode = mode;
    }

    /**
     * return the priority of the action.
     *
     * @return priority
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * Return the MethodHandle that provides reflective access to the underlying method.
     * @return methodHandle
     */
    public MethodHandle getMethodHandle() {
        return this.methodHandle;
    }

    /**
     * Return the name of the method to be executed when this action is invoked.
     * @return method
     */
    public String getMethod() {
        return this.method;
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
        if (methodHandle != null) {
            Application app;
            Context context;
            if ((context = this.app.getContext()) == null || (app = this.app.getInstance(context)) == null) {
                app = this.app;
            }

            Object[] arguments;
            Class<?>[] types = this.getParameterTypes();
            if (types.length > 0) {
                arguments = getArguments(app, args, types, context);
            } else {
                arguments = new Object[]{app};
            }

            try {
                // Dynamically invoke the method with resolved arguments.
                Object result = methodHandle.invokeWithArguments(arguments);

                // Handle void return type specifically.
                if (this.getReturnType().isAssignableFrom(Void.TYPE)) {
                    app.destroy(); // Trigger destruction logic if applicable.
                    if (!app.isTemplateRequired()) return null;
                    return app.toString();
                }

                return result;
            } catch (InvocationTargetException e) {
                // Handle target-specific exceptions.
                throw new ApplicationException(method + ": " + e.getTargetException().getMessage(), e.getTargetException());
            } catch (Throwable e) {
                // General error handling.
                throw new ApplicationException(method + ": " + e.getMessage(), e);
            }
        }

        logger.warning("Unsupported Operation.");
        throw new UnsupportedOperationException();
    }

    private Class<?> getReturnType() {
        return this.returnType;
    }

    private Class<?>[] getParameterTypes() {
        return this.parameterTypes;
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
     * @param args    The input arguments to convert.
     * @param types   The target types for each argument.
     * @param context The context providing additional information for certain types like Request or Response.
     * @return An array of converted arguments matching the target types.
     */
    private Object[] getArguments(Application instance, Object[] args, Class<?>[] types, Context context) {
        // Initialize the resulting arguments array start with the instance.
        Object[] arguments = new Object[types.length + 1];
        arguments[0] = instance;

        // If no target types are specified, return the arguments as-is, which only includes the instance.
        if (types.length == 0) return arguments;

        // Iterate over each target type.
        for (int n = 0; n < types.length; n++) {
            Class<?> targetType = types[n];
            Object arg = (args != null && args.length > n) ? args[n] : null;

            // Convert the argument to the required target type, if provided.
            if (arg != null) {
                arguments[n + 1] = convertArgument(arg, targetType);
            }

            // Handle context-specific arguments like Request and Response.
            if (context != null) {
                if (targetType.isAssignableFrom(Request.class) && context.getAttribute(HTTP_REQUEST) != null) {
                    arguments[n + 1] = context.getAttribute(HTTP_REQUEST);
                } else if (targetType.isAssignableFrom(Response.class) && context.getAttribute(HTTP_RESPONSE) != null) {
                    arguments[n + 1] = context.getAttribute(HTTP_RESPONSE);
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
            } else if (targetType.isEnum()) {
                return Enum.valueOf((Class<Enum>) targetType, _arg);
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
     * @param arg        The string representation of the argument.
     * @param targetType The target type to convert to.
     * @return The converted primitive or wrapper type object.
     */
    private Object parsePrimitive(String arg, Class<?> targetType) {
        // Switch block for both null handling and type conversion
        switch (targetType.getName()) {
            case "int":
            case "java.lang.Integer":
                return (arg == null) ? 0 : Integer.parseInt(arg);
            case "long":
            case "java.lang.Long":
                return (arg == null) ? 0L : Long.parseLong(arg);
            case "float":
            case "java.lang.Float":
                return (arg == null) ? 0.0f : Float.parseFloat(arg);
            case "double":
            case "java.lang.Double":
                return (arg == null) ? 0.0d : Double.parseDouble(arg);
            case "boolean":
            case "java.lang.Boolean":
                return Boolean.parseBoolean(arg);
            case "char":
            case "java.lang.Character":
                return (arg == null || arg.isEmpty()) ? '\u0000' : arg.charAt(0);
            case "short":
            case "java.lang.Short":
                return (arg == null) ? (short) 0 : Short.parseShort(arg);
            case "byte":
            case "java.lang.Byte":
                return (arg == null) ? (byte) 0 : Byte.parseByte(arg);
            default:
                throw new IllegalArgumentException("Unsupported type: " + targetType.getName());
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

