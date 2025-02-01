/*******************************************************************************
 * Copyright  (c) 2013, 2025 James M. ZHOU
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

/**
 * A singleton class for managing a mapping of string keys to Document objects.
 */
public final class MappingManager {
    // The underlying ConcurrentHashMap for storing the mapping
    private final ConcurrentHashMap<String, Document> map;

    // Private constructor to prevent external instantiation
    private MappingManager() {
        this.map = new ConcurrentHashMap<String, Document>();
    }

    /**
     * Returns the singleton instance of MappingManager.
     *
     * @return The singleton instance of MappingManager
     */
    public static MappingManager getInstance() {
        return SingletonHolder.manager;
    }

    /**
     * Sets a key-value pair in the mapping.
     *
     * @param name  The key
     * @param value The Document object to associate with the key
     */
    public void set(String name, Document value) {
        map.putIfAbsent(name, value);
    }

    /**
     * Retrieves the Document object associated with the specified key.
     *
     * @param name The key to retrieve the value for
     * @return The Document object associated with the key, or null if not found
     */
    public Document get(String name) {
        return map.get(name);
    }

    /**
     * Removes the entry with the specified key from the mapping.
     *
     * @param name The key of the entry to remove
     */
    public void remove(String name) {
        map.remove(name);
    }

    /**
     * Returns the number of entries in the mapping.
     *
     * @return The number of entries in the mapping
     */
    public int size() {
        return map.size();
    }

    // Private static inner class for lazy initialization of the singleton instance
    private static final class SingletonHolder {
        static final MappingManager manager = new MappingManager();
    }
}
