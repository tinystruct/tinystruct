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
package org.tinystruct;

import org.tinystruct.application.Context;

import java.util.HashMap;
import java.util.Objects;

/**
 * Implementation of {@link Context} for application.
 *
 * @author James Zhou
 * @since 0.1.0
 */
public final class ApplicationContext implements Context {

    private static final ThreadLocal<HashMap<String, Object>> threadLocal = ThreadLocal.withInitial(HashMap::new);

    public ApplicationContext() {
    }

    @Override
    public String getId() {
        if (null != getAttribute("Id"))
            return Objects.requireNonNull(getAttribute("Id")).toString();

        return "";
    }

    @Override
    public void setId(String Id) {
        this.setAttribute("Id", Id);
    }

    @Override
    public void setAttribute(String name, Object value) {
        threadLocal.get().put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        if (threadLocal.get().containsKey(name)) {
            return threadLocal.get().get(name);
        }

        return null;
    }

    @Override
    public void removeAttribute(String name) {
        threadLocal.get().remove(name);
    }

    @Override
    public String[] getAttributeNames() {
        return threadLocal.get().keySet().toArray(new String[]{});
    }
}