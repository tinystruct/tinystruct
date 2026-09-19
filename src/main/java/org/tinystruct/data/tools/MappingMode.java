/*******************************************************************************
 * Copyright  (c) 2013, 2026 James M. ZHOU
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
package org.tinystruct.data.tools;

/**
 * How a {@link Generator} describes the table mapping of the POJO it generates.
 */
public enum MappingMode {

    /** Write a {@code .map.xml} resource next to the class. This is the default. */
    XML,

    /** Annotate the class and its fields with {@code @Table}, {@code @Id} and {@code @Column}; no XML is written. */
    ANNOTATION;

    /**
     * Parses a user-supplied value such as {@code xml} or {@code annotation}.
     *
     * @param value the value; {@code null} or blank selects {@link #XML}
     * @return the matching mode
     * @throws IllegalArgumentException if the value is not recognised
     */
    public static MappingMode parse(String value) {
        if (value == null || value.isBlank()) {
            return XML;
        }

        switch (value.trim().toLowerCase()) {
            case "xml":
                return XML;
            case "annotation":
            case "annotations":
                return ANNOTATION;
            default:
                throw new IllegalArgumentException("Unknown mapping mode '" + value + "', expected 'xml' or 'annotation'");
        }
    }
}
