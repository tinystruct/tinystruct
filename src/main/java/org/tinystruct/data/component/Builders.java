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
        if (!value.isEmpty()) {
            if (value.charAt(0) == LEFT_BRACE) {
                // Parse entity and add it to the list
                logger.log(Level.FINE, "Parsing entity: {}", value);
                Builder builder = new Builder();
                builder.parse(value);
                this.add(builder);

                int p = builder.getClosedPosition();
                // Check if there are more entities in the string
                if (p < value.length() && value.charAt(p) == COMMA) {
                    value = value.substring(p + 1);
                    return this.parse(value);
                }
            }

            if (value.charAt(0) == LEFT_BRACKETS) {
                // Parse array and add its entities to the list
                logger.log(Level.FINE, "Parsing array: []", value);
                int end = this.seekPosition(value);
                this.parseArray(value.substring(1, end - 1));

                // Check if there are more entities in the string
                int len = value.length();
                if (end < len - 1) {
                    return this.parse(value.substring(end + 1));
                }
            }

            if (value.charAt(0) == QUOTE) {
                // Return the string if it starts with a quote
                return value;
            }
        }

        // Return an empty string if no valid parsing is performed
        return "";
    }

    /**
     * Parse the array elements in the array-like format (e.g., ["A", "B", "C"]).
     *
     * @param value The portion of the input string containing array elements.
     * @throws ApplicationException If there is an issue parsing the array data.
     */
    private void parseArray(String value) throws ApplicationException {
        value = value.trim();
        StringBuilder currentElement = new StringBuilder();
        boolean insideQuotes = false;
        int nestedLevel = 0;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c == QUOTE) {
                insideQuotes = !insideQuotes; // Toggle the quote state
                currentElement.append(c);
            } else if (c == LEFT_BRACE || c == LEFT_BRACKETS) {
                nestedLevel++;
                currentElement.append(c);
            } else if (c == RIGHT_BRACE || c == RIGHT_BRACKETS) {
                nestedLevel--;
                currentElement.append(c);
            } else if (c == COMMA && !insideQuotes && nestedLevel == 0) {
                // Split the element when outside quotes and no nested structure
                processElement(currentElement.toString().trim());
                currentElement.setLength(0); // Reset the current element
            } else {
                currentElement.append(c);
            }
        }

        // Process the last element in the array
        if (currentElement.length() > 0) {
            processElement(currentElement.toString().trim());
        }
    }

    /**
     * Process a single array element.
     *
     * @param element The array element as a string.
     * @throws ApplicationException If parsing fails.
     */
    private void processElement(String element) throws ApplicationException {
        element = element.trim();
        logger.fine("Processing element: " + element);
        if (element.startsWith("{")) {
            // Handle nested object
            Builder builder = new Builder();
            builder.parse(element);
            this.add(builder);

            int p = builder.getClosedPosition();
            // Check if there are more entities in the string
            if (p < element.length() && element.charAt(p) == COMMA) {
                element = element.substring(p + 1).trim();  // Skip comma and move to next entity
                this.processElement(element);  // Recursively parse the next part of the string
            }
        } else if (element.startsWith("[")) {
            // Handle nested array
            Builders nestedBuilders = new Builders();
            nestedBuilders.parse(element);
        } else if (element.startsWith("\"") && element.endsWith("\"")) {
            // Handle quoted string
            this.add(new Builder(element.substring(1, element.length() - 1)));
        } else if (isNumber(element)) {
            // Handle number
            this.add(new Builder(parseNumber(element)));
        } else {
            // Handle unknown type (pass through recursive parsing if needed)
            this.parse(element);
        }
    }

    /**
     * Parse a single value (string or number).
     *
     * @param value The string value to parse.
     * @return The parsed value (String or Number).
     */
    private Object parseValue(String value) {
        value = value.trim();
        if (value.charAt(0) == QUOTE) {
            // Remove quotes from strings
            return value.substring(1, value.length() - 1);
        } else if (isNumber(value)) {
            // Parse numbers
            return parseNumber(value);
        } else {
            // Return the raw value if unrecognized
            return value;
        }
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

            if (c == LEFT_BRACKETS) {
                n++;
            } else if (c == RIGHT_BRACKETS) {
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

