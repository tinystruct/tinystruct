/*******************************************************************************
 * Copyright (c) 2013, 2023 James Mover Zhou
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
import org.tinystruct.system.template.variable.Variable;

import java.util.Map;

/**
 * Template interface for defining methods to parse and manipulate templates.
 * Implement this interface to create custom template implementations.
 *
 * @author James M. ZHOU
 */
public interface Template {
    /**
     * Get the name of the template.
     *
     * @return The name of the template.
     */
    String getName();

    /**
     * Set a variable in the template.
     *
     * @param variable The variable to set.
     */
    void setVariable(Variable<?> variable);

    /**
     * Get a variable from the template by its name.
     *
     * @param name The name of the variable.
     * @return The variable, or null if not found.
     */
    Variable<?> getVariable(String name);

    /**
     * Get all variables present in the template.
     *
     * @return A map containing all variables in the template.
     */
    Map<String, Variable<?>> getVariables();

    /**
     * Parse the template and return the result as a string.
     *
     * @return The parsed template as a string.
     * @throws ApplicationException If an error occurs during template parsing.
     */
    String parse() throws ApplicationException;
}
