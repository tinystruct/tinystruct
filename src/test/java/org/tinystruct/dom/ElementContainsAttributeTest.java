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
package org.tinystruct.dom;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementContainsAttributeTest {

    @Test
    void testContainsAttributeUsesPassedList() throws Exception {
        Element element = new Element("root");
        Method method = Element.class.getDeclaredMethod("containsAttribute", List.class, String.class);
        method.setAccessible(true);

        List<Attribute> customAttributes = new ArrayList<>();
        customAttributes.add(new Attribute("customName", "customValue"));

        assertDoesNotThrow(() -> {
            try {
                boolean result = (boolean) method.invoke(element, customAttributes, "customName");
                assertTrue(result);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }
}
