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
 */
public class Builder extends HashMap<String, Object> implements Struct, Serializable {

    private static final long serialVersionUID = 3484789992424316230L;

    public static final char QUOTE = '"';
    public static final char COMMA = ',';
    public static final char COLON = ':';
    public static final char LEFT_BRACE = '{';
    public static final char RIGHT_BRACE = '}';
    public static final char LEFT_BRACKETS = '[';
    public static final char RIGHT_BRACKETS = ']';
    public static final char ESCAPE_CHAR = '\\';

    private static final Logger logger = Logger.getLogger(Builder.class.getName());

    private int closedPosition = 0;

    /**
     * Default constructor for Builder.
     */
    public Builder() {
    }

    /**
     * Convert the Builder object to its string representation.
     *
     * @return String representation of the Builder.
     */
    @Override
    public String toString() {
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
            else
                buffer.append(QUOTE).append(key).append(QUOTE).append(COLON).append(value);

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
     *
     * @param resource JSON string to parse.
     * @throws ApplicationException If the data format is invalid.
     */
    @Override
    public void parse(String resource) throws ApplicationException {
        // Ensure the input string is a valid JSON format
        resource = resource.trim();
        if (resource.charAt(0) != LEFT_BRACE && resource.charAt(resource.length()-1) != RIGHT_BRACE) {
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

        if (value.charAt(0) == QUOTE) {
            // Handle key-value pair starting with a quoted key
            int COLON_POSITION = value.indexOf(COLON);
            int start = COLON_POSITION + 1;
            String keyName = value.substring(1, COLON_POSITION - 1);

            String $value = value.substring(start).trim();

            Object keyValue;
            if ($value.charAt(0) == QUOTE) {
                // Extract the value if it is enclosed in quotes
                int $end = this.next($value);
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
                String remainings = builders.parse($value);
                keyValue = builders;
                if (!Objects.equals(remainings, "")) {
                    this.parseValue(remainings);
                }
            } else {
                if ($value.indexOf(COMMA) != -1) {
                    // Extract and parse a single value if there are more values in the sequence
                    String _value = $value.substring(0, $value.indexOf(COMMA));
                    if (_value.length() > 0) {
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

            // Add the key-value pair to the map
            this.put(keyName, keyValue);
        }
    }

    /**
     * Determine the type of the value and parse it accordingly.
     *
     * @param value String representation of the value.
     * @return Parsed value.
     */
    private Object getValue(String value) {
        // Determine the type of the value and parse it accordingly
        Object keyValue;
        if (Pattern.compile("^-?\\d+$").matcher(value).find()) {
            try {
                keyValue = Integer.parseInt(value);
            } catch (Exception e) {
                keyValue = Long.valueOf(value);
            }
        } else if (Pattern.compile("^-?\\d+(\\.\\d+)$").matcher(value).find()) {
            keyValue = Double.parseDouble(value);
        } else if (Pattern.compile("^(true|false)$").matcher(value.toLowerCase(Locale.ROOT)).find()) {
            keyValue = Boolean.parseBoolean(value);
        } else {
            keyValue = value;
        }
        return keyValue;
    }

    /**
     * Find the closing position of the JSON structure.
     *
     * @param value JSON structure string.
     * @return Closing position of the JSON structure.
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
                } else
                    n++;
            } else if (c == RIGHT_BRACE) {
                if (i - 1 >= 0 && chars[i - 1] == ESCAPE_CHAR) {
                } else
                    n--;
            }

            i++;
            if (n == 0)
                position = i;
        }

        return position;
    }

    /**
     * Find the position of the next occurrence of a character in a string.
     *
     * @param value String to search.
     * @return Position of the next occurrence of the character.
     */
    private int next(String value) {
        // Find the position of the next occurrence of a character in a string
        char[] chars = value.toCharArray();
        int i = 0;
        int n = 0;
        int position = chars.length;

        while (i < position) {
            char c = chars[i];
            if (c == Builder.QUOTE) {
                if (i - 1 >= 0 && chars[i - 1] == ESCAPE_CHAR) {
                } else
                    n++;
            }

            i++;
            if (n == 2)
                position = i;
        }

        return position;
    }

    /**
     * Convert the Builder object to a Row object.
     *
     * @return Row object representing the data in the Builder.
     */
    @Override
    public Row toData() {
        // Convert the builder to a Row object
        Row row = new Row();
        Field field = new Field();
        Set<Entry<String, Object>> keySet = this.entrySet();
        String key;
        Object value;
        for (Entry<String, Object> entry : keySet) {
            value = entry.getValue();
            key = entry.getKey();
            FieldInfo info = new FieldInfo();
            info.set("name", key);
            info.set("value", value);
            field.append(key, info);
        }

        row.append(field);
        return row;
    }

    /**
     * Save the string representation of the data to a file.
     *
     * @param file File to save the data.
     * @throws ApplicationException If there is an issue saving the data.
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
     * @return Size of the key set.
     */
    @Override
    public int size() {
        // Get the size of the key set
        return this.keySet().size();
    }
}
