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
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Optimized Builder class represents a key-value data structure, similar to a JSON object.
 * This version includes significant performance improvements over the original implementation.
 */
public class Builder extends HashMap<String, Object> implements Struct, Serializable {

    private static final long serialVersionUID = 3484789992424316230L;
    private static final Logger logger = Logger.getLogger(Builder.class.getName());

    // Thread-local StringBuilder for better performance in multi-threaded environments
    private static final ThreadLocal<StringBuilder> STRING_BUILDER_CACHE =
            ThreadLocal.withInitial(() -> new StringBuilder(256));

    private int closedPosition = 0;
    private Object value = null;

    /**
     * Default constructor with optimized initial capacity
     */
    public Builder() {
    }

    /**
     * Constructor that initializes with a string value.
     *
     * @param value The string value to initialize with
     */
    public Builder(String value) {
        this.value = value;
    }

    /**
     * Constructor that initializes with a number value.
     *
     * @param value The number value to initialize with
     */
    public Builder(Number value) {
        this.value = value;
    }

    /**
     * Constructor that initializes with an array value.
     *
     * @param value The array value to initialize with
     */
    public Builder(Object[] value) {
        this.value = value;
    }

    /**
     * Override of HashMap.put to allow method chaining.
     *
     * @param key   The key to store the value under
     * @param value The value to store
     * @return This Builder instance for method chaining
     */
    @Override
    public Builder put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    /**
     * Check if the Builder represents a single value (not a map).
     *
     * @return true if the Builder represents a single value, false if it's a map
     */
    public boolean isSingleValue() {
        return this.isEmpty() && this.value != null;
    }

    /**
     * Get the current value of the Builder.
     * This method is only available when the Builder represents a single value (not a map).
     *
     * @return The current value
     * @throws IllegalStateException if the Builder is being used as a map (has key-value pairs)
     */
    public Object getValue() {
        if (!this.isEmpty()) {
            throw new IllegalStateException("getValue() is only available when Builder represents a single value, not a map");
        }
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
        // Handle single value case
        if (this.value != null && this.isEmpty()) {
            return formatSingleValue(this.value);
        }

        // Use thread-local StringBuilder for better performance
        StringBuilder buffer = STRING_BUILDER_CACHE.get();
        buffer.setLength(0); // Reset the buffer

        buffer.append(LEFT_BRACE);

        Set<Entry<String, Object>> entries = this.entrySet();
        boolean first = true;

        for (Entry<String, Object> entry : entries) {
            if (!first) {
                buffer.append(COMMA);
            }
            first = false;

            appendKeyValue(buffer, entry.getKey(), entry.getValue());
        }

        buffer.append(RIGHT_BRACE);
        return buffer.toString();
    }

    /**
     * Optimized single value formatting
     */
    private String formatSingleValue(Object val) {
        if (val instanceof String) {
            return QUOTE + val.toString() + QUOTE;
        } else if (val != null && val.getClass().isArray()) {
            return formatArray(val);
        } else {
            return val != null ? val.toString() : NULL_STRING;
        }
    }

    /**
     * Optimized array formatting
     */
    private String formatArray(Object array) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(LEFT_BRACKETS);

        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                buffer.append(COMMA);
            }

            Object element = Array.get(array, i);
            if (element instanceof String) {
                buffer.append(QUOTE)
                        .append(escapeString(element.toString()))
                        .append(QUOTE);
            } else {
                buffer.append(element != null ? element.toString() : NULL_STRING);
            }
        }

        buffer.append(RIGHT_BRACKETS);
        return buffer.toString();
    }

    /**
     * Optimized key-value pair appending
     */
    private void appendKeyValue(StringBuilder buffer, String key, Object value) {
        buffer.append(QUOTE).append(key).append(QUOTE).append(COLON);

        if (value == null) {
            buffer.append(NULL_STRING);
        } else if (value.getClass().isArray()) {
            buffer.append(formatArray(value));
        } else if (value instanceof Boolean || value instanceof Number ||
                value instanceof Builder || value instanceof Builders) {
            buffer.append(value);
        } else {
            buffer.append(QUOTE)
                    .append(StringUtilities.escape(value.toString()))
                    .append(QUOTE);
        }
    }

    /**
     * Optimized string escaping
     */
    private String escapeString(String str) {
        if (str.indexOf(QUOTE) == -1) {
            return str; // No quotes to escape
        }
        return str.replace("\"", "\\\"");
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
        if (resource == null || resource.isEmpty()) {
            return;
        }

        resource = resource.trim();
        if (resource.isEmpty()) {
            return;
        }

        if (resource.charAt(0) == QUOTE) {
            this.parsePrimitiveValue(resource);
            return;
        }

        if (resource.charAt(0) != LEFT_BRACE || resource.charAt(resource.length() - 1) != RIGHT_BRACE) {
            throw new ApplicationException("Invalid data format:" + resource);
        }

        // Find the closing position of the JSON structure
        this.closedPosition = this.seekPosition(resource);

        if (closedPosition > 2) { // Must have content between braces
            String values = resource.substring(1, closedPosition - 1);
            this.parseKeyValuePairs(values);
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
     * Optimized key-value pairs parsing with reduced method calls
     */
    private void parseKeyValuePairs(String content) throws ApplicationException {
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        int pos = 0;
        int length = content.length();

        while (pos < length) {
            // Skip whitespace
            while (pos < length && Character.isWhitespace(content.charAt(pos))) {
                pos++;
            }

            if (pos >= length) break;

            if (content.charAt(pos) != QUOTE) {
                throw new ApplicationException("Expected quote at position " + pos);
            }

            // Parse key
            pos++; // Skip opening quote
            int keyStart = pos;
            while (pos < length && content.charAt(pos) != QUOTE) {
                if (content.charAt(pos) == ESCAPE_CHAR) {
                    pos++; // Skip escaped character
                }
                pos++;
            }

            if (pos >= length) {
                throw new ApplicationException("Unterminated key");
            }

            String key = content.substring(keyStart, pos);
            pos++; // Skip closing quote

            // Skip whitespace and find colon
            while (pos < length && Character.isWhitespace(content.charAt(pos))) {
                pos++;
            }

            if (pos >= length || content.charAt(pos) != COLON) {
                throw new ApplicationException("Expected colon after key");
            }
            pos++; // Skip colon

            // Parse value
            ValueParseResult result = parseValueAtPosition(content, pos);
            this.put(key, result.value);
            pos = result.nextPosition;

            // Skip comma if present
            while (pos < length && Character.isWhitespace(content.charAt(pos))) {
                pos++;
            }

            if (pos < length && content.charAt(pos) == COMMA) {
                pos++;
            }
        }
    }

    /**
     * Optimized value parsing with position tracking
     */
    private ValueParseResult parseValueAtPosition(String content, int startPos) throws ApplicationException {
        int pos = startPos;
        int length = content.length();

        // Skip whitespace
        while (pos < length && Character.isWhitespace(content.charAt(pos))) {
            pos++;
        }

        if (pos >= length) {
            return new ValueParseResult(null, pos);
        }

        char firstChar = content.charAt(pos);

        if (firstChar == QUOTE) {
            // String value
            pos++; // Skip opening quote
            int valueStart = pos;
            while (pos < length && content.charAt(pos) != QUOTE) {
                if (content.charAt(pos) == ESCAPE_CHAR) {
                    pos++; // Skip escaped character
                }
                pos++;
            }

            if (pos >= length) {
                throw new ApplicationException("Unterminated string value");
            }

            String value = content.substring(valueStart, pos);
            pos++; // Skip closing quote

            return new ValueParseResult(value, pos);

        } else if (firstChar == LEFT_BRACE) {
            // Nested object
            int objEnd = findMatchingBrace(content, pos);
            if (objEnd == -1) {
                throw new ApplicationException("Unmatched opening brace");
            }

            Builder nested = new Builder();
            nested.parse(content.substring(pos, objEnd + 1));

            return new ValueParseResult(nested, objEnd + 1);

        } else if (firstChar == LEFT_BRACKETS) {
            // Array
            int arrayEnd = findMatchingBracket(content, pos);
            if (arrayEnd == -1) {
                throw new ApplicationException("Unmatched opening bracket");
            }

            Builders builders = new Builders();
            builders.parse(content.substring(pos, arrayEnd + 1));

            return new ValueParseResult(builders, arrayEnd + 1);
        } else {
            // Primitive value (number, boolean, null)
            int valueEnd = findValueEnd(content, pos);
            String valueStr = content.substring(pos, valueEnd).trim();
            Object value = parsePrimitiveValue(valueStr);

            return new ValueParseResult(value, valueEnd);
        }
    }

    /**
     * Helper class for value parsing results
     */
    private static class ValueParseResult {
        final Object value;
        final int nextPosition;

        ValueParseResult(Object value, int nextPosition) {
            this.value = value;
            this.nextPosition = nextPosition;
        }
    }

    /**
     * Find the end of a primitive value
     */
    private int findValueEnd(String content, int start) {
        int pos = start;
        int length = content.length();

        while (pos < length) {
            char c = content.charAt(pos);
            if (c == COMMA || c == RIGHT_BRACE || c == RIGHT_BRACKETS) {
                break;
            }
            pos++;
        }

        return pos;
    }

    /**
     * Optimized brace matching
     */
    static int findMatchingBrace(String content, int start) {
        int depth = 0;
        boolean inQuotes = false;

        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == QUOTE && (i == 0 || content.charAt(i - 1) != ESCAPE_CHAR)) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == LEFT_BRACE) {
                    depth++;
                } else if (c == RIGHT_BRACE) {
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
     * Optimized bracket matching
     */
    static int findMatchingBracket(String content, int start) {
        int depth = 0;
        boolean inQuotes = false;

        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == QUOTE && (i == 0 || content.charAt(i - 1) != ESCAPE_CHAR)) {
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
     * Optimized primitive value parsing with reduced regex usage
     */
    static Object parsePrimitiveValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return value; // Preserve original if only whitespace
        }

        // Fast path for common cases
        if (NULL_STRING.equals(trimmed)) {
            return null;
        }

        if (TRUE_STRING.equalsIgnoreCase(trimmed)) {
            return Boolean.TRUE;
        }

        if (FALSE_STRING.equalsIgnoreCase(trimmed)) {
            return Boolean.FALSE;
        }

        // Try to parse as number without regex first
        if (isNumericFast(trimmed)) {
            return parseNumericValue(trimmed);
        }

        return value; // Return original to preserve whitespace
    }

    /**
     * Fast numeric check without regex
     */
    static boolean isNumericFast(String str) {
        if (str.isEmpty()) return false;

        int start = 0;
        if (str.charAt(0) == '-') {
            if (str.length() == 1) return false;
            start = 1;
        }

        boolean hasDecimal = false;
        boolean hasExponent = false;

        for (int i = start; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isDigit(c)) {
                continue;
            } else if (c == '.' && !hasDecimal && !hasExponent) {
                hasDecimal = true;
            } else if ((c == 'e' || c == 'E') && !hasExponent && i > start) {
                hasExponent = true;
                if (i + 1 < str.length() && (str.charAt(i + 1) == '+' || str.charAt(i + 1) == '-')) {
                    i++; // Skip sign after exponent
                }
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * Optimized numeric value parsing
     */
    static Number parseNumericValue(String value) {
        try {
            if (value.contains(".") || value.contains("e") || value.contains("E")) {
                return Double.parseDouble(value);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                logger.warning("Failed to parse numeric value: " + value);
                throw new IllegalArgumentException("Invalid number value: " + value, ex);
            }
        }
    }

    /**
     * Optimized position seeking with reduced character array allocation
     */
    private int seekPosition(String value) {
        int depth = 0;
        boolean inQuotes = false;
        int length = value.length();

        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);

            if (c == QUOTE && (i == 0 || value.charAt(i - 1) != ESCAPE_CHAR)) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == LEFT_BRACE) {
                    depth++;
                } else if (c == RIGHT_BRACE) {
                    depth--;
                    if (depth == 0) {
                        return i + 1;
                    }
                }
            }
        }

        return length;
    }

    @Override
    public Row toData() {
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

    public void saveAsFile(File file) throws ApplicationException {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.write(this.toString());
        } catch (FileNotFoundException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    @Override
    public int size() {
        return this.keySet().size();
    }
}