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
package org.tinystruct.data.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a data class to a database table, as an alternative to a {@code .map.xml} file.
 * Mapped columns are the fields annotated with {@link Column}; the identifier, which
 * lives in the inherited {@code AbstractData} state, is described by {@link #id()}.
 * <p>
 * The annotation is read once per class and cached, so it adds no cost to creating
 * instances. When a class carries {@code @Table} it takes precedence over any
 * {@code .map.xml} file of the same name.
 * </p>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {

    /** The table name. */
    String name();

    /** The schema the table belongs to, or an empty string for none. */
    String schema() default "";

    /** The identifier column. Leave the default (empty column) for a table without one. */
    Id id() default @Id;
}
