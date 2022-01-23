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
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.system.util.StringUtilities;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import static org.tinystruct.application.Action.MAX_ARGUMENTS;

public class Actions {

    private static final Map<String, Action> map = new ConcurrentHashMap<String, Action>();

    private static final class SingletonHolder {
        static final Actions actions = new Actions();
    }

    private Actions() {
    }

    public static Actions getInstance() {
        return SingletonHolder.actions;
    }

    public void set(final Application app, final String path, final String methodName) {
        if (path == null)
            return;

        this.initializePatterns(app, path, methodName);
    }

    public void set(final Application app, final String path, final String methodName, final String method) {
        this.set(app, path, methodName);
    }

    /**
     * New method registration.
     *
     * @param action method mapped
     */
    public void set(final Action action) {
        if (action == null)
            return;

        map.put(action.getPathRule(), action);
    }

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

    public boolean remove(final String path) {
        return null != map.remove(path);
    }

    public Collection<Action> list() {
        return map.values();
    }

    public Action getAction(String path) {
        Action action;
        if (((action = this.get(path)) != null || (action = this.get(new StringUtilities(path).rtrim('/'))) != null)) {
            return action;
        }

        return null;
    }

    public Action getAction(String path, String method) {
        return this.getAction(path);
    }

    private void initializePatterns(Application app, String path, String methodName) {
        Class<?> clazz = app.getClass();
        Method[] functions = new Method[MAX_ARGUMENTS];
        int n = 0;
        try {
            Method[] methods = clazz.getMethods();
            for (Method value : methods) {
                if (value.getName().equals(methodName)) {
                    functions[n++] = value;
                }
            }
        } catch (SecurityException e) {
            throw new ApplicationRuntimeException("[" + methodName + "]"
                    + e.getMessage(), e);
        }

        String patternPrefix = "/?" + path;
        for (int j = 0; j < n; j++) {
            Method m = functions[j];
            if (null != m) {
                Class<?>[] types = m.getParameterTypes();
                String expression;
                if (types.length > 0) {
                    StringBuilder patterns = new StringBuilder();

                    for (int i = types.length - 1; i >= 0; i--) {
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


}
