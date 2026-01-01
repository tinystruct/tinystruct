package org.tinystruct.application;

import org.tinystruct.Application;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.component.Cache;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.system.annotation.Action.Mode;
import org.tinystruct.system.cli.CommandLine;
import org.tinystruct.system.util.StringUtilities;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * The ActionRegistry class is responsible for managing and mapping
 * actions/methods to their corresponding URL patterns.
 */
public final class ActionRegistry {

    // Map to store pattern groups
    private static final Map<String, List<Action>> patternGroups = new ConcurrentHashMap<>();
    // Map to store URL patterns and their corresponding CommandLine objects per Mode
    private static final Map<String, Map<Mode, CommandLine>> commands = new ConcurrentHashMap<>();
    // Map of type patterns for URL parameter matching
    private static final Map<Class<?>, PatternPriority> TYPE_PATTERNS = new HashMap<>();
    private final Set<String> paths = ConcurrentHashMap.newKeySet();

    static {
        // Initialize the type pattern mappings
        TYPE_PATTERNS.put(Integer.TYPE, new PatternPriority("-?\\d+", 2));
        TYPE_PATTERNS.put(Integer.class, new PatternPriority("-?\\d+", 2));
        TYPE_PATTERNS.put(Long.TYPE, new PatternPriority("-?\\d+", 2));
        TYPE_PATTERNS.put(Long.class, new PatternPriority("-?\\d+", 2));
        TYPE_PATTERNS.put(Float.TYPE, new PatternPriority("-?\\d+(\\.\\d+)?", 3));
        TYPE_PATTERNS.put(Float.class, new PatternPriority("-?\\d+(\\.\\d+)?", 3));
        TYPE_PATTERNS.put(Double.TYPE, new PatternPriority("-?\\d+(\\.\\d+)?", 3));
        TYPE_PATTERNS.put(Double.class, new PatternPriority("-?\\d+(\\.\\d+)?", 3));
        TYPE_PATTERNS.put(Short.TYPE, new PatternPriority("-?\\d+", 2));
        TYPE_PATTERNS.put(Short.class, new PatternPriority("-?\\d+", 2));
        TYPE_PATTERNS.put(Byte.TYPE, new PatternPriority("\\d+", 2));
        TYPE_PATTERNS.put(Byte.class, new PatternPriority("\\d+", 2));
        TYPE_PATTERNS.put(Boolean.TYPE, new PatternPriority("true|false", 3));
        TYPE_PATTERNS.put(Boolean.class, new PatternPriority("true|false", 3));
        TYPE_PATTERNS.put(Character.TYPE, new PatternPriority(".{1}", 1));
        TYPE_PATTERNS.put(Character.class, new PatternPriority(".{1}", 1));
        TYPE_PATTERNS.put(String.class, new PatternPriority("[^/]+", 1));
    }

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
        this.set(app, path, methodName, Mode.DEFAULT);
    }

    /**
     * Register a method with a specific URL pattern.
     *
     * @param app    The Application instance
     * @param path   The URL pattern
     * @param method The method
     */
    public void set(final Application app, final String path, final Method method) {
        this.set(app, path, method, Mode.DEFAULT);
    }

    /**
     * Register a method with a specific URL pattern and method name.
     *
     * @param app        The Application instance
     * @param path       The URL pattern
     * @param methodName The method name
     * @param mode       The mode name
     */
    public void set(final Application app, final String path, final String methodName, final Mode mode) {
        validateParameters(path, methodName);
        paths.add(path);

        Class<?> clazz = app.getClass();
        storeCommandLine(app, path);

        Method[] methods = getMethods(methodName, clazz);
        for (Method method : methods) {
            if (method != null) {
                initializePattern(app, path, method, mode);
            }
        }
    }

    /**
     * Register a method with a specific URL pattern and method name.
     *
     * @param app    The Application instance
     * @param path   The URL pattern
     * @param method The method name
     * @param mode   The mode name
     */
    public void set(final Application app, final String path, final Method method, final Mode mode) {
        validateParameters(path, method);
        paths.add(path);
        storeCommandLine(app, path);
        initializePattern(app, path, method, mode);
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
     * @param mode The mode of action
     * @return The corresponding Action object or null if not found
     */
    public Action get(final String path, Mode mode) {
        Action bestMatch = null;
        Object[] args = new Object[]{};
        int bestPriority = Integer.MIN_VALUE; // assume lower numbers indicate higher priority
        String group = extractGroupFromPath(path);
        List<Action> actions = patternGroups.getOrDefault(group, new ArrayList<>());
        for (Action action : actions) {
            Matcher matcher = action.getPattern().matcher(path);
            if (matcher.matches()) {
                args = new Object[matcher.groupCount()];
                for (int i = 0; i < matcher.groupCount(); i++) {
                    args[i] = matcher.group(i + 1);
                }

                if (mode != null && mode == action.getMode()) {
                    bestMatch = action;
                    break;
                }

                if (action.getPriority() > bestPriority) {
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
        return Collections.unmodifiableSet(this.paths);
    }

    /**
     * Validate the specific path.
     *
     * @param path The path
     * @return String
     */
    public boolean validate(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        // Normalize the path by removing leading and trailing slashes
        // This reuse the normalization pattern found in extractGroupFromPath
        String normalizedPath = path.replaceAll("^/|/$", "");

        // Check if the path is explicitly registered or matches a dynamic action rule
        if (paths.contains(normalizedPath) || this.getAction(normalizedPath) != null) {
            return true;
        }

        return false;
    }

    /**
     * Get the CommandLine object for a given URL pattern.
     *
     * @param path The URL pattern
     * @return The corresponding CommandLine object or null if not found
     */
    public CommandLine getCommand(String path) {
        return getCommand(path, Mode.DEFAULT);
    }

    /**
     * Get the CommandLine object for a given URL pattern and Mode.
     * Falls back to DEFAULT mode if the requested mode is not available.
     *
     * @param path The URL pattern
     * @param mode The action mode
     * @return The corresponding CommandLine object or null if not found
     */
    public CommandLine getCommand(String path, Mode mode) {
        Map<Mode, CommandLine> map = commands.get(path);
        if (map == null || map.isEmpty()) return null;
        // Try exact mode
        if (mode != null && map.containsKey(mode)) return map.get(mode);
        // Fallback to DEFAULT
        if (map.containsKey(Mode.DEFAULT)) return map.get(Mode.DEFAULT);
        // Otherwise return any available command line (first entry)
        return map.values().stream().findFirst().orElse(null);
    }

    /**
     * Get the Action object for a given URL pattern and HTTP method type.
     *
     * @param path The URL pattern
     * @return The corresponding Action object or null if not found
     */
    public Action getAction(String path) {
        return this.getAction(path, Mode.DEFAULT);
    }

    /**
     * Get the Action object for a given URL pattern and HTTP method type.
     *
     * @param path The URL pattern
     * @param mode The mode of action
     * @return The corresponding Action object or null if not found
     */
    public Action getAction(String path, Mode mode) {
        Action action = this.get(path, mode);
        if (action == null) {
            action = this.get(new StringUtilities(path).removeTrailingSlash(), mode);
        }

        return action;
    }

    /**
     * Validate required parameters are not null
     *
     * @param path         The URL pattern
     * @param methodObject The method object or name
     */
    private void validateParameters(String path, Object methodObject) {
        if (path == null || methodObject == null) {
            throw new IllegalArgumentException("Path or method cannot be null.");
        }
    }

    /**
     * Store CommandLine object if available
     *
     * @param app  The application instance
     * @param path The URL pattern
     */
    private void storeCommandLine(Application app, String path) {
        // Support nested map format: Map<String, Map<Mode, CommandLine>>
        for (Map.Entry<String, Map<Mode, CommandLine>> entry : app.getCommandLines().entrySet()) {
            String key = entry.getKey();
            Map<Mode, CommandLine> modeMap = entry.getValue();

            if (key.equals(path)) {
                // Put all available modes for this command name
                for (Map.Entry<Mode, CommandLine> me : modeMap.entrySet()) {
                    Mode m = me.getKey() == null ? Mode.DEFAULT : me.getKey();
                    commands.computeIfAbsent(path, k -> new ConcurrentHashMap<>()).put(m, me.getValue());
                }
            }
        }
    }

    /**
     * Initialize URL patterns based on the registered methods in the Application class.
     *
     * @param app    The Application instance
     * @param path   The URL pattern
     * @param method The method
     * @param mode   The execution mode
     */
    private synchronized void initializePattern(Application app, String path, Method method, Mode mode) {
        Class<?> clazz = app.getClass();
        String group = extractGroupFromPath(path);

        if (method != null) {
            Class<?>[] types = method.getParameterTypes();
            PatternBuilder patternBuilder = buildPattern(path, types);

            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandle handle = lookup.findVirtual(clazz, method.getName(),
                        MethodType.methodType(method.getReturnType(), types));

                List<Action> actions = patternGroups.getOrDefault(group, new ArrayList<>());
                Action action = createAction(actions.size(), app, patternBuilder.getExpression(),
                        handle, method.getName(), method.getReturnType(),
                        method.getParameterTypes(), patternBuilder.getPriority(), mode);

                actions.add(action);
                patternGroups.put(group, actions);
            } catch (IllegalAccessException e) {
                throw new ApplicationRuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new ApplicationRuntimeException("Method not found", e);
            }
        }
    }

    /**
     * Create an Action object based on provided parameters
     */
    private Action createAction(int id, Application app, String expression, MethodHandle handle, String methodName,
            Class<?> returnType, Class<?>[] parameterTypes, int priority, Mode mode) {
        if (mode == Mode.DEFAULT) {
            return new Action(id, app, expression, handle, methodName, returnType, parameterTypes, priority);
        } else {
            return new Action(id, app, expression, handle, methodName, returnType, parameterTypes, priority, mode);
        }
    }

    /**
     * Build a pattern and calculate priority for method parameters
     */
    private PatternBuilder buildPattern(String path, Class<?>[] types) {
        String patternPrefix = "^/?" + path;
        StringBuilder patterns = new StringBuilder();
        int priority = 0;

        if (types.length > 0) {
            for (Class<?> type : types) {
                if (Request.class.isAssignableFrom(type) || Response.class.isAssignableFrom(type))
                    continue;

                PatternPriority patternPriority = getPatternForType(type);
                priority += patternPriority.getPriority();

                String pattern = "(" + patternPriority.getPattern() + ")";
                if (patterns.length() != 0) {
                    patterns.append("/");
                }
                patterns.append(pattern);
            }

            String expression;
            if (patterns.length() > 0) {
                expression = patternPrefix + "/" + patterns + "$";
            } else {
                expression = patternPrefix + "$";
            }

            return new PatternBuilder(expression, priority);
        } else {
            return new PatternBuilder(patternPrefix + "$", 0);
        }
    }

    /**
     * Helper class to store pattern and priority together
     */
    private static class PatternPriority {
        private final String pattern;
        private final int priority;

        public PatternPriority(String pattern, int priority) {
            this.pattern = pattern;
            this.priority = priority;
        }

        public String getPattern() {
            return pattern;
        }

        public int getPriority() {
            return priority;
        }
    }

    /**
     * Helper class to build and store pattern expressions
     */
    private static class PatternBuilder {
        private final String expression;
        private final int priority;

        public PatternBuilder(String expression, int priority) {
            this.expression = expression;
            this.priority = priority;
        }

        public String getExpression() {
            return expression;
        }

        public int getPriority() {
            return priority;
        }
    }

    /**
     * Gets pattern and priority for a specific parameter type
     */
    private PatternPriority getPatternForType(Class<?> type) {
        return TYPE_PATTERNS.getOrDefault(type, new PatternPriority("[^/]+", 0));
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
