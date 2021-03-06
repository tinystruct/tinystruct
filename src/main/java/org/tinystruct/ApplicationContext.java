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
import org.tinystruct.data.FileEntity;
import org.tinystruct.system.template.variable.ObjectVariable;
import org.tinystruct.system.template.variable.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link Context} for application.
 *
 * @author James Zhou
 * @since 0.1.0
 */
public class ApplicationContext implements Context {

    Map<String, Object> attr = new HashMap<String, Object>();
    HashMap<String, List<String>> params = new HashMap<String, List<String>>();
    List<FileEntity> list = new ArrayList<>();

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

    @Override
    public List<String> getParameterValues(String name) {
        return this.params.get(name);
    }

    public void setParameter(String name, List<String> value) {
        this.params.put(name, value);
    }

    @Override
    public String getParameter(String name) {
        if (null != this.params.get(name) && this.params.get(name).size() > 0) {
            return this.params.get(name).get(0);
        }
        return null;
    }

    @Override
    public void resetParameters() {
        this.params = new HashMap<>();
    }

    @Override
    public void setFiles(List<FileEntity> list) {
        this.list = list;
    }

    @Override
    public List<FileEntity> getFiles() {
        return this.list;
    }

    public void removeAttribute(String name) {
        attr.remove(name);
    }

    public String[] getAttributeNames() {
        return attr.keySet().toArray(new String[]{});
    }
}