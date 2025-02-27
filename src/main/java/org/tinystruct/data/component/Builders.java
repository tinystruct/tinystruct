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

import static org.tinystruct.data.component.Builder.*;

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
            buffer.append(COMMA);
        }

        // Remove trailing comma if there are Builder objects in the list
        if (buffer.length() > 0) {
            buffer.setLength(buffer.length() - 1);
        }

        return LEFT_BRACKETS + buffer.toString() + RIGHT_BRACKETS;
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
        if (value.isEmpty()) {
            return "";
        }

        if (value.charAt(0) == LEFT_BRACKETS) {
            int end = findClosingBracket(value);
            if (end == -1) {
                throw new ApplicationException("Invalid array format: missing closing bracket");
            }
            if (end > 1) { // If array is not empty
                parseArrayContent(value.substring(1, end));
            }
            return value.substring(Math.min(end + 1, value.length())).trim();
        }

        // Handle single elements
        if (value.charAt(0) == LEFT_BRACE) {
            Builder builder = new Builder();
            builder.parse(value);
            this.add(builder);
            int p = builder.getClosedPosition();
            if (p < value.length() && value.charAt(p) == COMMA) {
                return parse(value.substring(p + 1).trim());
            }
        }

        return value;
    }

    private void parseArrayContent(String content) throws ApplicationException {
        content = content.trim();
        if (content.isEmpty()) {
            return;
        }

        StringBuilder element = new StringBuilder();
        int depth = 0;
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == QUOTE && (i == 0 || content.charAt(i - 1) != ESCAPE_CHAR)) {
                inQuotes = !inQuotes;
                element.append(c);
            } else if (!inQuotes) {
                if (c == LEFT_BRACE || c == LEFT_BRACKETS) {
                    depth++;
                    element.append(c);
                } else if (c == RIGHT_BRACE || c == RIGHT_BRACKETS) {
                    depth--;
                    element.append(c);
                } else if (c == COMMA && depth == 0) {
                    addElement(element.toString().trim());
                    element.setLength(0);
                } else {
                    element.append(c);
                }
            } else {
                element.append(c);
            }
        }

        if (element.length() > 0) {
            addElement(element.toString().trim());
        }
    }

    private void addElement(String element) throws ApplicationException {
        if (element.isEmpty()) {
            return;
        }

        if (element.charAt(0) == QUOTE) {
            // String value
            String value = element.substring(1, element.length() - 1);
            this.add(new Builder(value));
        } else if (element.charAt(0) == LEFT_BRACE) {
            // Object value
            Builder builder = new Builder();
            builder.parse(element);
            this.add(builder);
        } else if (element.charAt(0) == LEFT_BRACKETS) {
            // Array value
            Builder arrayBuilder = new Builder();
            Builders nestedBuilders = new Builders();
            nestedBuilders.parse(element);
            for (Builder b : nestedBuilders) {
                arrayBuilder.put(String.valueOf(arrayBuilder.size()), b.getValue());
            }
            this.add(arrayBuilder);
        } else {
            // All other values (numbers, booleans, null) are stored as strings
            if (isNumber(element)) {
                this.add(new Builder(parseNumber(element)));
            } else {
                this.add(new Builder(element));
            }
        }
    }

    private int findClosingBracket(String value) {
        int depth = 0;
        boolean inQuotes = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c == QUOTE && (i == 0 || value.charAt(i - 1) != ESCAPE_CHAR)) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == LEFT_BRACKETS) {
                    depth++;
                } else if (c == RIGHT_BRACKETS) {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Check if the string represents a valid number.
     *
     * @param value The string to check.
     * @return True if the string is a valid number, false otherwise.
     */
    private boolean isNumber(String value) {
        try {
            Double.parseDouble(value);  // Try parsing the value as a number (integer or float)
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Parse a number from a string.
     *
     * @param value The string value to parse.
     * @return The parsed number.
     */
    private Number parseNumber(String value) {
        if (value.contains(".")) {
            return Double.parseDouble(value);  // Parse as double if it contains a decimal point
        } else {
            return Integer.parseInt(value);  // Parse as integer otherwise
        }
    }
}

