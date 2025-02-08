package org.tinystruct.application;

import org.tinystruct.Application;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.component.Cache;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.system.cli.CommandLine;
import org.tinystruct.system.util.StringUtilities;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * The ActionRegistry class is responsible for managing and mapping actions/methods to their corresponding URL patterns.
 */
public final class ActionRegistry {

    // Map to store pattern groups
    private static final Map<String, List<Action>> patternGroups = new ConcurrentHashMap<>();
    // Map to store URL patterns and their corresponding CommandLine objects
    private static final Map<String, CommandLine> commands = new ConcurrentHashMap<>();
    private final Set<String> paths = new HashSet<>();

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

        paths.add(path);
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

        String group = extractGroupFromPattern(action.getPathRule());
        List<Action> actions = patternGroups.getOrDefault(group, new ArrayList<>());
        actions.add(action);
        patternGroups.put(group, actions);
    }

    /**
     * Retrieve the Action object for a given URL pattern.
     *
     * @param path The URL pattern
     * @return The corresponding Action object or null if not found
     */
    public Action get(final String path) {
        Action bestMatch = null;
        Object[] args = new Object[]{};
        int bestPriority = Integer.MIN_VALUE; // assume lower numbers indicate higher priority
        String group = extractGroupFromPath(path);
        List<Action> actions = patternGroups.getOrDefault(group, new ArrayList<>());
        for (Action action : actions) {
            Matcher matcher = action.getPattern().matcher(path);
            if (matcher.matches()) {
                if (action.getPriority() > bestPriority) {
                    args = new Object[matcher.groupCount()];
                    for (int i = 0; i < matcher.groupCount(); i++) {
                        args[i] = matcher.group(i + 1);
                    }
                    bestMatch = action;
                    bestPriority = action.getPriority();
                }
            }
        }

        return bestMatch != null ? new Action(bestMatch, args) : null;
    }

    /**
     * Remove an Action object for a given URL pattern.
     *
     * @param action The URL pattern
     * @return True if the Action was removed, false otherwise
     */
    public boolean remove(final Action action) {
        String group = extractGroupFromPattern(action.getPathRule());
        List<Action> actions = patternGroups.get(group);
        if (actions != null && !actions.isEmpty())
            return actions.remove(action);
        return false;
    }

    /**
     * Get a collection of all registered Action objects.
     *
     * @return Collection of Action objects
     */
    public Collection<Action> list() {
        return patternGroups.values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * Return all paths has been registered.
     *
     * @return paths Set
     */
    public Collection<String> paths() {
        return this.paths;
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
     * @param mode       The execution mode
     */
    private synchronized void initializePatterns(Application app, String path, String methodName, Action.Mode mode) {
        Class<?> clazz = app.getClass();
        Method[] methods = getMethods(methodName, clazz);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        CommandLine cli = app.getCommandLines().get(path);
        if (cli != null) {
            commands.put(path, cli);
        }
        String group = extractGroupFromPath(path);
        String patternPrefix = "^/?" + path;
        for (Method m : methods) {
            if (null != m) {
                Class<?>[] types = m.getParameterTypes();
                String expression;

                int priority = 0;
                if (types.length > 0) {
                    StringBuilder patterns = new StringBuilder();
                    for (Class<?> type : types) {
                        if (type.isAssignableFrom(Request.class) || type.isAssignableFrom(Response.class)) continue;
                        String[] patternWithPriority = this.getPatternForType(type).split(":");
                        String patternForType = patternWithPriority[0];
                        priority += Integer.parseInt(patternWithPriority[1]);

                        String pattern = "(" + patternForType + ")";
                        if (patterns.length() != 0) {
                            patterns.append("/");
                        }
                        patterns.append(pattern);
                    }

                    if (patterns.length() > 0) {
                        expression = patternPrefix + "/" + patterns + "$";
                    } else {
                        expression = patternPrefix + "$";
                    }
                } else {
                    expression = patternPrefix + "$";
                }

                try {
                    MethodHandle handle = lookup.unreflect(m);
                    List<Action> actions = patternGroups.getOrDefault(group, new ArrayList<>());
                    Action action = mode == Action.Mode.All ? new Action(actions.size(), app, expression, handle, m.getName(), m.getReturnType(), m.getParameterTypes(), priority) : new Action(actions.size(), app, expression, handle, m.getName(), m.getReturnType(), m.getParameterTypes(), priority, mode);
                    actions.add(action);

                    patternGroups.put(group, actions);
                } catch (IllegalAccessException e) {
                    throw new ApplicationRuntimeException(e);
                }
            }
        }
    }

    private String getPatternForType(Class<?> type) {
        if (type.isAssignableFrom(Integer.TYPE) || type.isAssignableFrom(Integer.class)) {
            return "-?\\d+:2";
        } else if (type.isAssignableFrom(Long.TYPE) || type.isAssignableFrom(Long.class)) {
            return "-?\\d+:2";
        } else if (type.isAssignableFrom(Float.TYPE) || type.isAssignableFrom(Float.class)) {
            return "-?\\d+(\\.\\d+)?:3";
        } else if (type.isAssignableFrom(Double.TYPE) || type.isAssignableFrom(Double.class)) {
            return "-?\\d+(\\.\\d+)?:3";
        } else if (type.isAssignableFrom(Short.TYPE) || type.isAssignableFrom(Short.class)) {
            return "-?\\d+:2";
        } else if (type.isAssignableFrom(Byte.TYPE) || type.isAssignableFrom(Byte.class)) {
            return "\\d+:2";
        } else if (type.isAssignableFrom(Boolean.TYPE) || type.isAssignableFrom(Boolean.class)) {
            return "true|false:3";
        } else if (type.isAssignableFrom(Character.TYPE) || type.isAssignableFrom(Character.class)) {
            return ".{1}:1";
        } else {
            return "[^/]+:0";
        }
    }

    /**
     * Extracts the first static segment from a pattern.
     * Example: "^/?download/([^/]+)/([^/]+)$" -> "download"
     */
    private String extractGroupFromPattern(String pattern) {
        // Remove the start and end anchors (^ and $) and optional leading slash
        String cleanedPattern = pattern.replaceAll("^\\^/\\?|\\$$", "");
        // Split into segments
        String[] segments = cleanedPattern.split("/");
        // Return the first segment if it's static (not a regex group)
        if (segments.length > 0 && !segments[0].startsWith("(") && !segments[0].isEmpty()) {
            return segments[0];
        }
        // If no static segment is found, use a default group name
        return "other";
    }

    /**
     * Extracts the first segment from a path.
     * Example: "/download/123/abc" -> "download"
     */
    private String extractGroupFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "other";
        }
        // Remove leading/trailing slashes and split
        String[] parts = path.replaceAll("^/|/$", "").split("/");
        return parts.length > 0 ? parts[0] : "other";
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
