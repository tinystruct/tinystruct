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
package org.tinystruct.data;

import org.tinystruct.dom.Document;

import java.util.concurrent.ConcurrentHashMap;

public class MappingManager {
    private final ConcurrentHashMap<String, Document> map;

    private static final class SingletonHolder {
        static final MappingManager manager = new MappingManager();
    }

    public static MappingManager getInstance() {
        return SingletonHolder.manager;
    }

    private MappingManager() {
        this.map = new ConcurrentHashMap<String, Document>();
    }

    public void set(String name, Document value) {
        map.put(name, value);
    }

    public Document get(String name) {
        return map.get(name);
    }

    public void remove(String name) {
        map.remove(name);
    }

    public int size() {
        return map.size();
    }

}
