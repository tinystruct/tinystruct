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
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.component.Cache;
import org.tinystruct.system.cli.CommandLine;
import org.tinystruct.system.util.StringUtilities;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import static org.tinystruct.application.Action.MAX_ARGUMENTS;

/**
 * ActionRegistry is responsible for managing and mapping actions/methods to their corresponding URL patterns.
 */
public final class ActionRegistry {

    // Map to store URL patterns and their corresponding Action objects
    private static final Map<String, Action> map = new ConcurrentHashMap<String, Action>(16);
    // Map to store URL patterns and their corresponding CommandLine objects
    private static final Map<String, CommandLine> commands = new ConcurrentHashMap<String, CommandLine>(16);

    // Private constructor to enforce singleton pattern
    private ActionRegistry() {
    }

    /**
     * Get an instance of the ActionRegistry (Singleton pattern).
     *
     * @return ActionRegistry instance
     */
    public static ActionRegistry getInstance() {
        return SingletonHolder.ROUTE_REGISTRY;
    }

    /**
     * Register a method with a specific URL pattern.
     *
     * @param app        The Application instance
     * @param path       The URL pattern
     * @param methodName The method name
     */
    public void set(final Application app, final String path, final String methodName) {
        if (path == null)
            return;

        this.initializePatterns(app, path, methodName);
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
        if (action == null)
            return;

        map.put(action.getPathRule(), action);
    }

    /**
     * Retrieve the Action object for a given URL pattern.
     *
     * @param path The URL pattern
     * @return The corresponding Action object or null if not found
     */
    public Action get(final String path) {
        Set<Map.Entry<String, Action>> set = map.entrySet();
        for (Map.Entry<String, Action> n : set) {
            Action action = n.getValue();
            Matcher matcher = action.getPattern().matcher(path);
            if (matcher.find()) {
                Object[] args = new Object[matcher.groupCount()];
                for (int i = 0; i < matcher.groupCount(); i++) {
                    args[i] = matcher.group(i + 1);
                }
                action.setArguments(args);
                return action;
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
        return null != map.remove(path);
    }

    /**
     * Get a collection of all registered Action objects.
     *
     * @return Collection of Action objects
     */
    public Collection<Action> list() {
        return map.values();
    }

    /**
     * Get the Action object for a given URL pattern (without trailing slash).
     *
     * @param path The URL pattern
     * @return The corresponding Action object or null if not found
     */
    public Action getAction(String path) {
        Action action;
        if (((action = this.get(path)) != null || (action = this.get(new StringUtilities(path).removeTrailingSlash())) != null)) {
            return action;
        }

        return null;
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
     */
    private synchronized void initializePatterns(Application app, String path, String methodName) {
        Class<?> clazz = app.getClass();
        Method[] functions = getMethods(methodName, clazz);

        CommandLine cli = app.getCommandLines().get(path);
        if (cli != null)
            commands.put(path, cli);
        String patternPrefix = "/?" + path;
        for (Method m : functions) {
            if (null != m) {
                Class<?>[] types = m.getParameterTypes();
//                Parameter[] params = m.getParameters();
                String expression;
                if (types.length > 0) {
                    StringBuilder patterns = new StringBuilder();

                    for (int i = 0; i < types.length; i++) {
//                        if (cli != null)
//                            cli.addArgument(new CommandArgument<>(params[i].getName(), null, ""));
                        String pattern = "(";
                        if (types[i].isAssignableFrom(Integer.TYPE)) {
                            pattern += "-?\\d+";
                        } else if (types[i].isAssignableFrom(Long.TYPE)) {
                            pattern += "-?\\d+";
                        } else if (types[i].isAssignableFrom(Float.TYPE)) {
                            pattern += "-?\\d+(\\.\\d+)";
                        } else if (types[i].isAssignableFrom(Double.TYPE)) {
                            pattern += "-?\\d+(\\.\\d+)";
                        } else if (types[i].isAssignableFrom(Short.TYPE)) {
                            pattern += "-?\\d+";
                        } else if (types[i].isAssignableFrom(Byte.TYPE)) {
                            pattern += "\\d+";
                        } else if (types[i].isAssignableFrom(Boolean.TYPE)) {
                            pattern += "true|false";
                        } else {
                            pattern += ".*";
                        }
                        pattern += ")";

                        if (patterns.length() != 0) {
                            patterns.append("/");
                        }
                        patterns.append(pattern);
                    }

                    expression = patternPrefix + "/" + patterns + "$";
                } else {
                    expression = patternPrefix + "$";
                }
                map.put(expression, new Action(map.size(), app, expression, m));
            }
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

        Method[] functions;
        String key = clazz.getName() + ":" + methodName;
        if ((functions = (Method[]) instance.get(key)) != null) {
            return functions;
        } else {
            functions = new Method[MAX_ARGUMENTS];
            int n = 0;
            try {
                Method[] methods = clazz.getMethods();
                for (Method value : methods) {
                    if (value.getName().equals(methodName)) {
                        functions[n++] = value;
                    }
                }

                functions = Arrays.copyOf(functions, n);
                instance.set(key, functions);
            } catch (SecurityException e) {
                throw new ApplicationRuntimeException("[" + methodName + "]" + e.getMessage(), e);
            }
        }

        return functions;
    }

    // Inner class to hold the singleton instance of ActionRegistry
    private static final class SingletonHolder {
        static final ActionRegistry ROUTE_REGISTRY = new ActionRegistry();
    }
}
