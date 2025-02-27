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
package org.tinystruct.data.component;

import org.tinystruct.ApplicationException;

/**
 * Struct interface defines the contract for structured data parsing and manipulation.
 * This interface is used for JSON-like data structures that can be parsed from strings
 * and converted to different representations.
 */
public interface Struct {
    /**
     * Defines the tokens used in parsing structured data.
     * These tokens represent the basic elements found in JSON-like structures.
     */
    enum TOKEN {
        /** Represents no token or initial state */
        NONE(""),
        /** Opening curly brace '{' */
        CURLY_OPEN("{"),
        /** Closing curly brace '}' */
        CURLY_CLOSE("}"),
        /** Opening square bracket '[' */
        SQUARED_OPEN("["),
        /** Closing square bracket ']' */
        SQUARED_CLOSE("]"),
        /** Colon separator ':' */
        COLON(":"),
        /** Comma separator ',' */
        COMMA(","),
        /** String value */
        STRING("\""),
        /** Numeric value */
        NUMBER("0-9"),
        /** Boolean true value */
        TRUE("true"),
        /** Boolean false value */
        FALSE("false"),
        /** Null value */
        NULL("null");

        private final String symbol;

        TOKEN(String symbol) {
            this.symbol = symbol;
        }

        /**
         * Gets the string representation of this token.
         *
         * @return The symbol associated with this token
         */
        public String getSymbol() {
            return symbol;
        }
    }

    /**
     * Parses a JSON-formatted string into the implementing structure.
     * 
     * @param json The JSON string to parse
     * @throws ApplicationException If the JSON string is malformed or parsing fails
     */
    void parse(String json) throws ApplicationException;

    /**
     * Converts the structure to a Row data format.
     * This allows for transformation into a different data representation.
     * 
     * @return A Row object representing the data structure
     */
    Row toData();

    /**
     * Converts the structure to its string representation.
     * This is typically a JSON-formatted string.
     * 
     * @return A string representation of the structure
     */
    String toString();
}