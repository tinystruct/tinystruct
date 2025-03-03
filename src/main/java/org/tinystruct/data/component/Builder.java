/*******************************************************************************
 * Copyright  (c) 2025 James M. Zhou
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
import org.tinystruct.system.util.StringUtilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Builder class represents a key-value data structure, similar to a JSON object.
 * It extends HashMap to provide a map-like interface while adding JSON parsing and formatting capabilities.
 * This class implements the Struct interface to provide consistent data structure handling across the application.
 */
public class Builder extends HashMap<String, Object> implements Struct, Serializable {

    private static final long serialVersionUID = 3484789992424316230L;
    private static final Logger logger = Logger.getLogger(Builder.class.getName());

    // JSON parsing constants
    public static final char QUOTE = '"';
    public static final char COMMA = ',';
    public static final char COLON = ':';
    public static final char LEFT_BRACE = '{';
    public static final char RIGHT_BRACE = '}';
    public static final char LEFT_BRACKETS = '[';
    public static final char RIGHT_BRACKETS = ']';
    public static final char ESCAPE_CHAR = '\\';

    // Value type patterns
    public static final Pattern INTEGER = Pattern.compile("^-?\\d+$");
    public static final Pattern DOUBLE = Pattern.compile("^-?\\d+(\\.\\d+)$");
    public static final Pattern BOOLEAN = Pattern.compile("^(true|false)$");
    private int closedPosition = 0;
    private String key = null;
    private Object value = null;

    /**
     * Default constructor for Builder.
     */
    public Builder() {
    }

    /**
     * Constructor that initializes with a string value.
     * @param value The string value to initialize with
     */
    public Builder(String value) {
        this.value = value;
    }

    /**
     * Constructor that initializes with a number value.
     * @param value The number value to initialize with
     */
    public Builder(Number value) {
        this.value = value;
    }

    /**
     * Constructor that initializes with a key-value pair.
     * @param key The key for the value
     * @param value The value to associate with the key
     */
    public Builder(String key, Object value) {
        this.key = key;
        this.value = value;
        this.put(key, value);
    }

    /**
     * Override of HashMap.put to allow method chaining.
     * @param key The key to store the value under
     * @param value The value to store
     * @return This Builder instance for method chaining
     */
    @Override
    public Builder put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    /**
     * Get the current value of the Builder.
     * @return The current value
     */
    public Object getValue() {
        return this.value;
    }

    /**
     * Convert the Builder object to its string representation.
     * The output follows JSON format with proper escaping of special characters.
     *
     * @return String representation of the Builder in JSON format
     */
    @Override
    public String toString() {
        if (this.value != null) {
            String tmp;
            if (this.value instanceof String) {
                tmp = QUOTE + this.value.toString() + QUOTE;
            } else {
                tmp = this.value.toString();
            }

            if (this.key != null) {
                return QUOTE + this.key + QUOTE + ":" + tmp;
            }

            return tmp;
        }

        // Build a string representation of the data
        StringBuilder buffer = new StringBuilder();
        Set<Entry<String, Object>> keys = this.entrySet();
        Object value;
        String key;
        for (Entry<String, Object> entry : keys) {
            value = entry.getValue();
            key = entry.getKey();

            if (value instanceof String || value instanceof StringBuffer || value instanceof StringBuilder)
                buffer.append(QUOTE).append(key).append(QUOTE).append(COLON).append(QUOTE).append(StringUtilities.escape(value.toString())).append(QUOTE);
            else buffer.append(QUOTE).append(key).append(QUOTE).append(COLON).append(value);

            buffer.append(COMMA);
        }

        // Remove trailing comma if there are key-value pairs
        if (buffer.length() > 0) {
            buffer.setLength(buffer.length() - 1);
        }

        return LEFT_BRACE + buffer.toString() + RIGHT_BRACE;
    }

    /**
     * Parse the resource string and populate the Builder object with key-value pairs.
     * The input string should be in JSON format.
     *
     * @param resource JSON string to parse
     * @throws ApplicationException If the data format is invalid or parsing fails
     */
    @Override
    public void parse(String resource) throws ApplicationException {
        // Ensure the input string is a valid JSON format
        resource = resource.trim();
        if (!resource.isEmpty()) {
            if (resource.charAt(0) == QUOTE) {
                this.parseValue(resource);
            }

            if (resource.charAt(0) != LEFT_BRACE && resource.charAt(resource.length() - 1) != RIGHT_BRACE) {
                throw new ApplicationException("Invalid data format!");
            }

            if (resource.charAt(0) == LEFT_BRACE) {
                // Find the closing position of the JSON structure
                this.closedPosition = this.seekPosition(resource);
                // Extract the key-value pairs sequence from the JSON structure
                String values = resource.substring(1, closedPosition - 1);

                // Parse the key-value pairs
                this.parseValue(values);
            }
        }
    }

    /**
     * Get the position of the closing brace in the JSON structure.
     *
     * @return Position of the closing brace.
     */
    public int getClosedPosition() {
        return closedPosition;
    }

    /**
     * Set the position of the closing brace in the JSON structure.
     *
     * @param closedPosition Position of the closing brace.
     */
    public void setClosedPosition(int closedPosition) {
        this.closedPosition = closedPosition;
    }

    /**
     * Parse the key-value pairs from the input string and populate the Builder object.
     *
     * @param value Key-value pairs string.
     * @throws ApplicationException If there is an issue parsing the data.
     */
    private void parseValue(String value) throws ApplicationException {
        // Trim the input value
        value = value.trim();

        if (!value.isEmpty() && value.charAt(0) == QUOTE) {
            // Handle key-value pair starting with a quoted key
            int COLON_POSITION = value.indexOf(COLON);

            if (COLON_POSITION != -1) {
                int start = COLON_POSITION + 1;

                String keyName = value.substring(1, COLON_POSITION - 1);
                int QUOTE_POSITION = keyName.lastIndexOf(QUOTE);
                if (QUOTE_POSITION != -1) {
                    keyName = keyName.substring(0, QUOTE_POSITION);
                }

                String $value = value.substring(start).trim();
                Object keyValue = null;
                if (!$value.isEmpty()) {
                    if ($value.charAt(0) == QUOTE) {
                        // Extract the value if it is enclosed in quotes
                        int $end = this.next($value, Builder.QUOTE);
                        keyValue = $value.substring(1, $end - 1).trim();

                        if ($end + 1 < $value.length()) {
                            $value = $value.substring($end + 1); // COMMA length: 1
                            this.parseValue($value);
                        }
                    } else if ($value.charAt(0) == LEFT_BRACE) {
                        // Handle nested JSON structure
                        int closedPosition = this.seekPosition($value);
                        String _$value = $value.substring(0, closedPosition);
                        Builder builder = new Builder();
                        builder.parse(_$value);
                        keyValue = builder;
                        if (closedPosition < $value.length()) {
                            _$value = $value.substring(closedPosition + 1); // COMMA length: 1
                            this.parseValue(_$value);
                        }
                    } else if ($value.charAt(0) == LEFT_BRACKETS) {
                        // Handle array
                        Builders builders = new Builders();
                        String remaining = builders.parse($value).trim();
                        keyValue = builders;
                        if(!remaining.isEmpty() && remaining.charAt(0) == COMMA)
                            remaining = remaining.substring(1); // COMMA length: 1
                        this.parseValue(remaining);
                    } else {
                        if ($value.indexOf(COMMA) != -1) {
                            // Extract and parse a single value if there are more values in the sequence
                            String _value = $value.substring(0, $value.indexOf(COMMA));
                            if (!_value.isEmpty()) {
                                keyValue = getValue(_value);
                            } else {
                                keyValue = _value;
                            }

                            $value = $value.substring($value.indexOf(COMMA) + 1);
                            this.parseValue($value);
                        } else {
                            // Parse the last value in the sequence
                            keyValue = getValue($value);
                        }
                    }
                }

                // Add the key-value pair to the map
                this.put(keyName, keyValue);
            }
        }
    }

    /**
     * Determine the type of the value and parse it accordingly.
     * Supports parsing of integers, doubles, booleans, and strings.
     *
     * @param value String representation of the value
     * @return Parsed value of appropriate type
     */
    private Object getValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim();
        if (INTEGER.matcher(value).matches()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException ex) {
                    logger.warning("Failed to parse integer value: " + value);
                }
            }
        }
        
        if (DOUBLE.matcher(value).matches()) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                logger.warning("Failed to parse double value: " + value);
            }
        }
        
        if (BOOLEAN.matcher(value.toLowerCase(Locale.ROOT)).matches()) {
            return Boolean.parseBoolean(value);
        }
        
        if ("null".equalsIgnoreCase(value)) {
            return null;
        }
        
        return value;
    }

    /**
     * Find the closing position of the JSON structure.
     * Handles nested structures and escaped characters.
     *
     * @param value JSON structure string
     * @return Closing position of the JSON structure
     */
    private int seekPosition(String value) {
        // Find the closing position of the JSON structure
        char[] chars = value.toCharArray();
        int i = 0;
        int n = 0;
        int position = chars.length;

        while (i < position) {
            char c = chars[i];
            if (c == LEFT_BRACE) {
                if (i - 1 >= 0 && chars[i - 1] == ESCAPE_CHAR) {
                } else n++;
            } else if (c == RIGHT_BRACE) {
                if (i - 1 >= 0 && chars[i - 1] == ESCAPE_CHAR) {
                } else n--;
            }

            i++;
            if (n == 0) position = i;
        }

        return position;
    }

    /**
     * Find the position of the next occurrence of a character in a string.
     * Handles escaped characters and nested structures.
     *
     * @param value String to search
     * @param character The character to look for
     * @return Position of the next occurrence of the character
     */
    private int next(String value, char character) {
        // Find the position of the next occurrence of a character in a string
        char[] chars = value.toCharArray();
        int i = 0;
        int n = 0;
        int position = chars.length;

        while (i < position) {
            char c = chars[i];
            if (c == character) {
                if (i - 1 >= 0 && chars[i - 1] == ESCAPE_CHAR) {
                } else n++;
            }

            i++;
            if (n == 2) position = i;
        }

        return position;
    }

    /**
     * Convert the Builder object to a Row object.
     * This method is used to transform the JSON-like structure into a database row format.
     *
     * @return Row object representing the data in the Builder
     */
    @Override
    public Row toData() {
        // Convert the builder to a Row object
        Row row = new Row();
        Field field = new Field();
        
        for (Entry<String, Object> entry : this.entrySet()) {
            FieldInfo info = new FieldInfo();
            info.set("name", entry.getKey());
            info.set("value", entry.getValue());
            field.append(entry.getKey(), info);
        }

        row.append(field);
        return row;
    }

    /**
     * Save the string representation of the data to a file.
     * The data is saved in JSON format.
     *
     * @param file File to save the data
     * @throws ApplicationException If there is an issue saving the data
     */
    public void saveAsFile(File file) throws ApplicationException {
        // Save the string representation of the data to a file
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.write(this.toString());
        } catch (FileNotFoundException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Get the size of the key set.
     *
     * @return Size of the key set
     */
    @Override
    public int size() {
        // Get the size of the key set
        return this.keySet().size();
    }
}
