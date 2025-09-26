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

import java.util.ArrayList;

import static org.tinystruct.data.component.Builder.*;

/**
 * Optimized Builders class represents a collection of Builder objects with improved performance.
 */
public class Builders extends ArrayList<Builder> implements Struct {

    private static final long serialVersionUID = -6787714840442861559L;

    /**
     * Constructor with optimized initial capacity
     */
    public Builders() {
    }

    /**
     * Optimized toString method using thread-local StringBuilder
     */
    @Override
    public String toString() {
        if (this.isEmpty()) {
            return "[]";
        }

        StringBuilder buffer = new StringBuilder(256);
        buffer.append(LEFT_BRACKETS);

        boolean first = true;
        for (Builder builder : this) {
            if (!first) {
                buffer.append(COMMA);
            }
            first = false;
            buffer.append(builder.toString());
        }

        buffer.append(RIGHT_BRACKETS);
        return buffer.toString();
    }

    /**
     * Optimized parse method with single-pass parsing and reduced method calls
     */
    public void parse(String value) throws ApplicationException {
        if (value == null || value.isEmpty() || (value = value.trim()).isEmpty()) {
            throw new ApplicationException("Invalid array format: missing closing bracket");
        }

        if (value.charAt(0) == LEFT_BRACKETS) {
            int end = findMatchingBracket(value, 0);
            if (end == -1) {
                throw new ApplicationException("Invalid array format: missing closing bracket");
            }

            if (end > 1) { // Array has content
                parseArrayContent(value, 1, end);
            }
        }

        // Handle single elements that are objects
        if (value.charAt(0) == LEFT_BRACE) {
            Builder builder = new Builder();
            builder.parse(value);
            this.add(builder);
            int p = builder.getClosedPosition();

            if (p < value.length() && value.charAt(p) == COMMA) {
                parse(value.substring(p + 1));
            }
        }
    }

    @Override
    public Row toData() {
        throw new UnsupportedOperationException("It's not been implemented.");
    }

    /**
     * Optimized array content parsing with single-pass tokenization
     */
    private void parseArrayContent(String content, int start, int end) throws ApplicationException {
        if (start >= end) {
            return;
        }

        int pos = start;

        while (pos < end) {
            // Skip whitespace
            while (pos < end && Character.isWhitespace(content.charAt(pos))) {
                pos++;
            }

            if (pos >= end) {
                break;
            }

            ElementParseResult result = parseElementAtPosition(content, pos, end);
            if (result.element != null) {
                this.add(result.element);
            }

            pos = result.nextPosition;

            // Skip comma and whitespace
            while (pos < end && (Character.isWhitespace(content.charAt(pos)) || content.charAt(pos) == COMMA)) {
                pos++;
            }
        }
    }

    /**
     * Parse a single element at the given position
     */
    private ElementParseResult parseElementAtPosition(String content, int pos, int end) throws ApplicationException {
        char firstChar = content.charAt(pos);

        if (firstChar == QUOTE) {
            // String element
            int stringEnd = findStringEnd(content, pos + 1);
            if (stringEnd == -1) {
                throw new ApplicationException("Unterminated string in array");
            }

            String value = content.substring(pos + 1, stringEnd);
            return new ElementParseResult(new Builder(value), stringEnd + 1);
        } else if (firstChar == LEFT_BRACE) {
            // Object element
            int objEnd = findMatchingBrace(content, pos);
            if (objEnd == -1) {
                throw new ApplicationException("Unmatched opening brace in array");
            }

            Builder builder = new Builder();
            builder.parse(content.substring(pos, objEnd + 1));
            return new ElementParseResult(builder, objEnd + 1);
        } else if (firstChar == LEFT_BRACKETS) {
            // Nested array element
            int arrayEnd = findMatchingBracket(content, pos);
            if (arrayEnd == -1) {
                throw new ApplicationException("Unmatched opening bracket in array");
            }

            Builder arrayBuilder = new Builder();
            Builders nestedBuilders = new Builders();
            nestedBuilders.parseArrayContent(content, pos + 1, arrayEnd);

            // Convert nested array to Builder with indexed keys
            for (int i = 0; i < nestedBuilders.size(); i++) {
                Builder nested = nestedBuilders.get(i);
                arrayBuilder.put(String.valueOf(i), nested.isSingleValue() ? nested.getValue() : nested);
            }

            return new ElementParseResult(arrayBuilder, arrayEnd + 1);
        } else {
            // Primitive element (number, boolean, null)
            int elementEnd = findElementEnd(content, pos, end);
            String elementStr = content.substring(pos, elementEnd).trim();

            Object parsedValue = parsePrimitiveValue(elementStr);
            if (parsedValue instanceof Number)
                return new ElementParseResult(new Builder((Number) parsedValue), elementEnd);
            return new ElementParseResult(new Builder(elementStr), elementEnd);
        }
    }

    /**
     * Helper class for element parsing results
     */
    private static class ElementParseResult {
        final Builder element;
        final int nextPosition;

        ElementParseResult(Builder element, int nextPosition) {
            this.element = element;
            this.nextPosition = nextPosition;
        }
    }

    /**
     * Find the end of a string value
     */
    private int findStringEnd(String content, int start) {
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == QUOTE && (i == 0 || content.charAt(i - 1) != ESCAPE_CHAR)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the end of a primitive element
     */
    private int findElementEnd(String content, int start, int maxEnd) {
        for (int i = start; i < maxEnd; i++) {
            char c = content.charAt(i);
            if (c == COMMA || c == RIGHT_BRACKETS) {
                return i;
            }
        }
        return maxEnd;
    }

    /**
     * Check if all elements in this Builders collection are single values
     */
    public boolean allSingleValues() {
        for (Builder builder : this) {
            if (!builder.isSingleValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get all values as an Object array (only works if all elements are single values)
     */
    public Object[] getValuesArray() {
        if (!allSingleValues()) {
            throw new IllegalStateException("Cannot get values array when not all elements are single values");
        }

        Object[] values = new Object[this.size()];
        for (int i = 0; i < this.size(); i++) {
            values[i] = this.get(i).getValue();
        }
        return values;
    }

    /**
     * Clear the collection and reset capacity for reuse
     */
    public void reset() {
        this.clear();
        // Don't shrink the underlying array to avoid repeated allocations
    }

}