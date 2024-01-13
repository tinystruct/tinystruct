/*******************************************************************************
 * Copyright  (c) 2023 James Mover Zhou
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

import org.tinystruct.ApplicationException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builders class represents a collection of Builder objects, providing methods
 * for parsing and managing a list of structured data.
 */
public class Builders extends ArrayList<Builder> implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(Builders.class.getName());

    /**
     * Default constructor for Builders.
     */
    public Builders() {
    }

    /**
     * Convert the Builders object to its string representation.
     *
     * @return String representation of the Builders.
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        // Build a string representation of the list of Builder objects
        for (Object o : this) {
            buffer.append(o);
            buffer.append(',');
        }

        // Remove trailing comma if there are Builder objects in the list
        if (buffer.length() > 0) {
            buffer.setLength(buffer.length() - 1);
        }

        return "[" + buffer + "]";
    }

    /**
     * Parse the input string and populate the Builders object with Builder objects.
     *
     * @param value JSON string to parse.
     * @return Remaining string after parsing.
     * @throws ApplicationException If there is an issue parsing the data.
     */
    public String parse(String value) throws ApplicationException {
        value = value.trim();

        if (value.indexOf("{") == 0) {
            // Parse entity and add it to the list
            logger.log(Level.FINE, "Parsing entity: {}", value);
            Builder builder = new Builder();
            builder.parse(value);
            this.add(builder);

            int p = builder.getClosedPosition();
            // Check if there are more entities in the string
            if (p < value.length() && value.charAt(p) == ',') {
                value = value.substring(p + 1);
                return this.parse(value);
            }
        }

        if (value.indexOf('[') == 0) {
            // Parse array and add its entities to the list
            logger.log(Level.FINE, "Parsing array: {}", value);
            int end = this.seekPosition(value);
            this.parse(value.substring(1, end - 1));

            // Check if there are more entities in the string
            int len = value.length();
            if (end < len - 1) {
                return this.parse(value.substring(end + 1));
            }
        }

        if (value.indexOf('"') == 0) {
            // Return the string if it starts with a quote
            return value;
        }

        // Return an empty string if no valid parsing is performed
        return "";
    }

    /**
     * Find the position of the closing bracket in the JSON array.
     *
     * @param value JSON array string.
     * @return Closing position of the JSON array.
     */
    private int seekPosition(String value) {
        char[] chars = value.toCharArray();
        int i = 0;
        int n = 0;
        int position = chars.length;

        while (i < position) {
            char c = chars[i++];

            if (c == '[') {
                n++;
            } else if (c == ']') {
                n--;
            }

            // If n becomes 0, it means the closing bracket is found
            if (n == 0) {
                position = i;
            }
        }

        return position;
    }
}

