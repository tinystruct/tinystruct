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
import org.tinystruct.system.util.StringUtilities;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Actions {

    private static final Map<String, List<Action>> map = new ConcurrentHashMap<String, List<Action>>();

    private static final class SingletonHolder {
        static final Actions actions = new Actions();
    }

    ;

    private Actions() {
    }

    public static Actions getInstance() {
        return SingletonHolder.actions;
    }

    public boolean containsAction(String function, List<Action> list) {
        Action action = null;
        Iterator<Action> iterator = list.iterator();
        while (iterator.hasNext()) {
            action = iterator.next();
            if (action.getName().equalsIgnoreCase(function)) {
                return true;
            }
        }

        return false;
    }

    public void set(final Application app, final String action, final String function) {
        if (action == null)
            return;
        List<Action> list;
        if (map.containsKey(action)) {
            list = map.get(action);
        } else {
            list = new ArrayList<Action>();
        }

        if (list.size() == 0 || !this.containsAction(function, list))
            list.add(new Action(map.size(), app, action, function));

        map.put(action, list);
    }

    public void set(final Application app, final String action, final String function, final String method) {
        List<Action> list;
        if (map.containsKey(action)) {
            list = map.get(action);
        } else {
            list = new ArrayList<Action>();
        }

        if (list.size() == 0 || !this.containsAction(function, list))
            list.add(new Action(map.size(), app, action, function, method));

        map.put(action, list);
    }

    public List<Action> get(final String path) {
        if (map.containsKey(path)) {
            return map.get(path);
        }

        return Collections.emptyList();
    }

    public Action get(final String path, final String method) {
        if (map.containsKey(path)) {
            List<Action> actions = map.get(path);

            if (actions.size() > 0) {
                Action action;
                Iterator<Action> iterator = actions.iterator();
                while (iterator.hasNext()) {
                    action = iterator.next();
                    if (action != null && action.getMethod() != null && action.getMethod().equalsIgnoreCase(method)) {
                        return action;
                    }
                }

                return actions.get(0);
            }
        }

        return null;
    }

    public void remove(final String path) {
        map.remove(path);
    }

    public Collection<Action> list() {
        Collection<List<Action>> values = map.values();
        Iterator<List<Action>> iterator = values.iterator();
        ArrayList<Action> collections = new ArrayList<Action>();
        while (iterator.hasNext()) {
            List<Action> list = iterator.next();
            Iterator<Action> it = list.iterator();
            while (it.hasNext()) {
                collections.add(it.next());
            }
        }
        return collections;
    }

    public Collection<String> paths() {
        return map.keySet();
    }

    public Action getAction(String path) {
        List<Action> list;
        if (((list = this.get(path)) != null || (list = this.get(new StringUtilities(path).rtrim('/'))) != null)
                && list.size() > 0) {
            return list.get(0);
        }

        return null;
    }

    public Action getAction(String path, String method) {
        if (method == null) {
            return this.getAction(path);
        } else {
            Action action = this.get(path, method);
            if (action == null)
                return this.get(new StringUtilities(path).rtrim('/'), method);
            return action;
        }
    }

}
