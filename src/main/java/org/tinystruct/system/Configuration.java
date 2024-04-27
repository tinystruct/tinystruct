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

package org.tinystruct.system;

import java.io.Serializable;
import java.util.Set;

public interface Configuration<T> extends Serializable {
    void set(String name, T value);

    T get(String name);

    void remove(String name);

    String toString();

    Set<String> propertyNames();

    T getOrDefault(T s, T value);

    void setIfAbsent(T s, T path);
}
