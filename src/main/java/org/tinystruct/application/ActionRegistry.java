package org.tinystruct.application;

import org.tinystruct.Application;
import org.tinystruct.data.component.Cache;
import org.tinystruct.system.cli.CommandLine;
import org.tinystruct.system.util.StringUtilities;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

/**
 * The ActionRegistry class is responsible for managing and mapping actions/methods to their corresponding URL patterns.
 */
public final class ActionRegistry {

    // Map to store URL patterns and their corresponding Action objects
    private static final Map<String, Action> map = new ConcurrentHashMap<>();
    // Map to store URL patterns and their corresponding CommandLine objects
    private static final Map<String, CommandLine> commands = new ConcurrentHashMap<>();

    // Private constructor to enforce singleton pattern
    private ActionRegistry() {
    }

    /**
     * Get an instance of the ActionRegistry (Singleton pattern).
     *
     * @return ActionRegistry instance
     */
    public static ActionRegistry getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Register a method with a specific URL pattern.
     *
     * @param app        The Application instance
     * @param path       The URL pattern
     * @param methodName The method name
     */
    public void set(final Application app, final String path, final String methodName) {
        this.set(app, path, methodName, Action.Mode.All);
    }

    /**
     * Register a method with a specific URL pattern and method type (GET, POST, etc.).
     *
     * @param app        The Application instance
     * @param path       The URL pattern
     * @param methodName The method name
     * @param mode       The mode name
     */
    public void set(final Application app, final String path, final String methodName, final Action.Mode mode) {
        if (path == null || methodName == null) {
            throw new IllegalArgumentException("Path or methodName cannot be null.");
        }

        initializePatterns(app, path, methodName, mode);
    }

    /**
     * Register a method with a specific URL pattern and method type (GET, POST, etc.).
     *
     * @param app        The Application instance
     * @param path       The URL pattern
     * @param methodName The method name
     * @param method     The HTTP method type
     */
    public void set(final Application app, final String path, final String methodName, final String method) {
        this.set(app, path, methodName);
    }

    /**
     * Register a new Action object.
     *
     * @param action The Action object to register
     */
    public void set(final Action action) {
        if (action == null || action.getPathRule() == null) {
            throw new IllegalArgumentException("Action or pathRule cannot be null.");
        }

        map.put(action.getPathRule(), action);
    }

    /**
     * Retrieve the Action object for a given URL pattern.
     *
     * @param path The URL pattern
     * @return The corresponding Action object or null if not found
     */
    public Action get(final String path) {
        for (Action action : map.values()) {
            Matcher matcher = action.getPattern().matcher(path);
            if (matcher.find()) {
                Object[] args = new Object[matcher.groupCount()];
                for (int i = 0; i < matcher.groupCount(); i++) {
                    args[i] = matcher.group(i + 1);
                }

                Action clone = new Action(action);
                clone.setArguments(args);
                return clone;
            }
        }
        return null;
    }

    /**
     * Remove an Action object for a given URL pattern.
     *
     * @param path The URL pattern
     * @return True if the Action was removed, false otherwise
     */
    public boolean remove(final String path) {
        return map.remove(path) != null;
    }

    /**
     * Get a collection of all registered Action objects.
     *
     * @return Collection of Action objects
     */
    public Collection<Action> list() {
        return Collections.unmodifiableCollection(map.values());
    }

    /**
     * Get the Action object for a given URL pattern (without trailing slash).
     *
     * @param path The URL pattern
     * @return The corresponding Action object or null if not found
     */
    public Action getAction(String path) {
        Action action = this.get(path);
        if (action == null) {
            action = this.get(new StringUtilities(path).removeTrailingSlash());
        }
        return action;
    }

    /**
     * Get the CommandLine object for a given URL pattern.
     *
     * @param path The URL pattern
     * @return The corresponding CommandLine object or null if not found
     */
    public CommandLine getCommand(String path) {
        return commands.get(path);
    }

    /**
     * Get the Action object for a given URL pattern and HTTP method type.
     *
     * @param path   The URL pattern
     * @param method The HTTP method type
     * @return The corresponding Action object or null if not found
     */
    public Action getAction(String path, String method) {
        return this.getAction(path);
    }

    /**
     * Initialize URL patterns based on the registered methods in the Application class.
     *
     * @param app        The Application instance
     * @param path       The URL pattern
     * @param methodName The method name
     * @param mode
     */
    private synchronized void initializePatterns(Application app, String path, String methodName, Action.Mode mode) {
        Class<?> clazz = app.getClass();
        Method[] methods = getMethods(methodName, clazz);

        CommandLine cli = app.getCommandLines().get(path);
        if (cli != null) {
            commands.put(path, cli);
        }
        String patternPrefix = "/?" + path;
        for (Method m : methods) {
            if (null != m) {
                Class<?>[] types = m.getParameterTypes();
                String expression;
                if (types.length > 0) {
                    StringBuilder patterns = new StringBuilder();

                    for (Class<?> type : types) {
                        String pattern = "(" + this.getPatternForType(type) + ")";

                        if (patterns.length() != 0) {
                            patterns.append("/");
                        }
                        patterns.append(pattern);
                    }

                    expression = patternPrefix + "/" + patterns + "$";
                } else {
                    expression = patternPrefix + "$";
                }

                Action action = mode == Action.Mode.All ? new Action(map.size(), app, expression, m) : new Action(map.size(), app, expression, m, mode);
                map.put(expression, action);
            }
        }
    }

    private String getPatternForType(Class<?> type) {
        if (type.isAssignableFrom(Integer.TYPE) || type.isAssignableFrom(Integer.class)) {
            return "-?\\d+";
        } else if (type.isAssignableFrom(Long.TYPE) || type.isAssignableFrom(Long.class)) {
            return "-?\\d+";
        } else if (type.isAssignableFrom(Float.TYPE) || type.isAssignableFrom(Float.class)) {
            return "-?\\d+(\\.\\d+)?";
        } else if (type.isAssignableFrom(Double.TYPE) || type.isAssignableFrom(Double.class)) {
            return "-?\\d+(\\.\\d+)?";
        } else if (type.isAssignableFrom(Short.TYPE) || type.isAssignableFrom(Short.class)) {
            return "-?\\d+";
        } else if (type.isAssignableFrom(Byte.TYPE) || type.isAssignableFrom(Byte.class)) {
            return "\\d+";
        } else if (type.isAssignableFrom(Boolean.TYPE) || type.isAssignableFrom(Boolean.class)) {
            return "true|false";
        } else if (type.isAssignableFrom(Character.TYPE) || type.isAssignableFrom(Character.class)) {
            return ".{1}";
        } else {
            return ".*";
        }
    }

    /**
     * Get methods and make it cacheable.
     *
     * @param methodName The method name
     * @param clazz      The class
     * @return Array of methods
     */
    private Method[] getMethods(String methodName, Class<?> clazz) {
        Cache instance = Cache.getInstance();

        String key = clazz.getName() + ":" + methodName;
        Method[] methods;
        if ((methods = (Method[]) instance.get(key)) == null) {
            methods = Arrays.stream(clazz.getMethods())
                    .filter(m -> m.getName().equals(methodName))
                    .toArray(Method[]::new);
            instance.set(key, methods);
        }
        return methods;
    }

    // Inner class to hold the singleton instance of ActionRegistry
    private static class SingletonHolder {
        private static final ActionRegistry INSTANCE = new ActionRegistry();
    }
}
