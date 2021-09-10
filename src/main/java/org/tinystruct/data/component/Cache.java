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
package org.tinystruct.data.component;

import org.tinystruct.data.Cacheable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Cache implements Cacheable {
    private final Map<String, Object> map;

    private static final class SingletonHolder {
        static final Cache cache = new Cache();
    }

    private Cache() {
        this.map = new ConcurrentHashMap<String, Object>();
    }

    public static Cache getInstance() {
        return SingletonHolder.cache;
    }

    public Object get(String key) {
        return map.get(key);
    }

    public void set(String key, Object value) {
        if (key != null && value != null)
            map.put(key, value);
    }

    public void remove(String key) {
        map.remove(key);
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public Collection<Object> values() {
        return map.values();
    }

}
