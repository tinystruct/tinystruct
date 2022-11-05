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

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.system.template.variable.DataType;
import org.tinystruct.system.template.variable.StringVariable;
import org.tinystruct.system.template.variable.Variable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Variables {
    private static final ThreadLocal<Map<String, Variable<?>>> threadLocal = ThreadLocal.withInitial(HashMap::new);

    public Variables() {
    }

    public static ConcurrentHashMap<String, Variable<?>> getInstance() {
        return SingletonHolder.variables;
    }

    public void setVariable(Variable<?> variable, boolean force) {
        saveVariable(variable, force, false);
    }

    public void setSharedVariable(Variable<?> variable, boolean force) {
        saveVariable(variable, force, true);
    }

    private void saveVariable(Variable<?> variable, boolean force, boolean shared) {
        String variableName = "{%" + variable.getName() + "%}";
        Map<String, Variable<?>> map;
        if (!shared) {
            map = this.getVariables();
        }
        else {
            map = getInstance();
        }

        if (force || !map.containsKey(variableName)) {
            if (variable.getType() == DataType.OBJECT) {
                Builder builder = new Builder();
                try {
                    builder.parse(variable.getValue().toString());
                    Set<Map.Entry<String, Object>> elements = builder.entrySet();
                    Iterator<Map.Entry<String, Object>> list = elements.iterator();
                    Map.Entry<String, Object> entry;
                    while (list.hasNext()) {
                        entry = list.next();
                        map.put("{%" + variable.getName() + "."
                                + entry.getKey() + "%}", new StringVariable("{%"
                                + variable.getName() + "." + entry.getKey() + "%}",
                                entry.getValue().toString()));
                    }
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }
            }
            map.put(variableName, variable);
        }
    }

    public Variable<?> getVariable(String variable) {
        return getVariables().get("{%" + variable + "%}");
    }

    public Map<String, Variable<?>> getVariables() {
        return threadLocal.get();
    }

    private static final class SingletonHolder {
        static final ConcurrentHashMap<String, Variable<?>> variables = new ConcurrentHashMap<String, Variable<?>>(16);
    }
}