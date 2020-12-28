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

import org.tinystruct.system.template.variable.Variable;

import java.util.concurrent.ConcurrentHashMap;

public class Variables {

    private static final class SingletonHolder {
        static final ConcurrentHashMap<String, Variable<?>> variables = new ConcurrentHashMap<String, Variable<?>>();
    }

    ;

    private Variables() {

    }

    public static ConcurrentHashMap<String, Variable<?>> getInstance() {
        return SingletonHolder.variables;
    }
}
