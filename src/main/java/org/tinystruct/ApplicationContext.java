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
package org.tinystruct;

import org.tinystruct.application.Context;
import org.tinystruct.system.template.variable.ObjectVariable;
import org.tinystruct.system.template.variable.Variable;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link Context} for application.
 *
 * @author James Zhou
 * @since 0.1.0
 */
public final class ApplicationContext implements Context {

    private final Map<String, Object> attr = new HashMap<String, Object>();
    private String id = "";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAttribute(String name, Object value) {
        Variable<Object> variable = new ObjectVariable(name, value);
        this.attr.put(name, variable);
    }

    public Object getAttribute(String name) {
        if (attr.containsKey(name) && attr.get(name) instanceof Variable<?>) {
            Variable<?> var = (Variable<?>) (attr.get(name));
            if (var.getName().equals(name)) return var.getValue();
        }

        return null;
    }

    public void removeAttribute(String name) {
        attr.remove(name);
    }

    public String[] getAttributeNames() {
        return attr.keySet().toArray(new String[]{});
    }
}