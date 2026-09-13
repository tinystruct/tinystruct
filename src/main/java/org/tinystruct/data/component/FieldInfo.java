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

import java.sql.Blob;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * Describes a single mapped column (name, value, type, ...) for one row/field.
 * A new instance is allocated per column per row, so it uses a plain {@link HashMap}
 * rather than a concurrent map: these objects are never shared across threads, and the
 * concurrency support of {@code ConcurrentHashMap} is pure overhead on this hot path.
 * {@link #keys()} is kept for source compatibility with existing {@code Enumeration}-based
 * callers.
 *
 * @author James Zhou
 */
public class FieldInfo extends HashMap<String, Object> {
    private final static long serialVersionUID = 1;
    private String name;
    private String column;
    private boolean autoIncrement;
    private int length;

    /**
     * Constructs an empty {@code FieldInfo} instance with an initial capacity of 8.
     * Pre-sizing minimizes rehashing overhead for typical column metadata attributes.
     */
    public FieldInfo() {
        super(8);

        this.autoIncrement = false;
        this.length = 0;
    }

    /**
     * Returns an {@link Enumeration} of the property names (keys) in this map.
     * <p>
     * Maintained for backward compatibility with callers that rely on {@link Enumeration}-based
     * iteration.
     * </p>
     *
     * @return an {@link Enumeration} of the property keys contained in this map
     */
    public Enumeration<String> keys() {
        return Collections.enumeration(this.keySet());
    }

    /**
     * Constructs a {@code FieldInfo} instance with the specified field name.
     *
     * @param name the field name
     */
    public FieldInfo(String name) {
        this();
        this.name = name;
    }

    /**
     * Sets the field name.
     *
     * @param name the field name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the field name. If the field name is not yet cached on this instance,
     * it will be read from the stored {@code "name"} property in the map.
     *
     * @return the field name
     */
    public String getName() {
        if (this.containsKey("name"))
            this.name = String.valueOf(this.get("name"));
        return this.name;
    }

    /**
     * Returns the database column name. If the column name is not yet cached on this instance,
     * it will be read from the stored {@code "column"} property in the map.
     *
     * @return the database column name
     */
    public String getColumnName() {
        if (this.containsKey("column"))
            this.column = String.valueOf(this.get("column"));
        return this.column;
    }

    /**
     * Returns the column length or display size. If not yet cached, it is parsed
     * from the stored {@code "length"} property.
     *
     * @return the column length, or 0 if not specified
     */
    public int getLength() {
        if (this.containsKey("length"))
            this.length = Integer.parseInt(String.valueOf(this.get("length")));
        return this.length;
    }

    /**
     * Returns whether this column is configured as auto-increment.
     * Reads and parses the {@code "increment"} property if present.
     *
     * @return {@code true} if the column is auto-incrementing; {@code false} otherwise
     */
    public boolean autoIncrement() {
        if (this.containsKey("increment")) {
            String value = String.valueOf(this.get("increment"));

            if ("1".equals(value))
                this.autoIncrement = true;
            else
                this.autoIncrement = Boolean.parseBoolean(value);
        }
        return this.autoIncrement;
    }

    /**
     * Appends a property and its value to this field info.
     * Delegated to {@link #set(String, Object)}.
     *
     * @param property the property name
     * @param value    the property value
     */
    public void append(String property, Object value) {
        this.set(property, value);
    }

    /**
     * Sets a property name and its value in this map if neither is {@code null}.
     *
     * @param property the property name
     * @param value    the property value
     */
    public void set(String property, Object value) {
        if (property != null && value != null)
            this.put(property, value);
    }

    /**
     * Returns the raw value associated with the {@code "value"} property.
     *
     * @return the column value object, or {@code null} if no value is present
     */
    public Object value() {
        if (!this.containsKey("value"))
            return null;
        return this.get("value");
    }

    /**
     * Returns the string representation of the column value.
     *
     * @return the string value, or {@code null} if the value is {@code null}
     */
    public String stringValue() {
        Object value = this.value();

        if (value == null)
            return null;

        return value.toString();
    }

    /**
     * Returns the column value converted to a {@code double}.
     *
     * @return the double value, or {@code 0} if the value is {@code null}
     */
    public double doubleValue() {
        Object value = this.value();

        if (value == null)
            return 0;

        return Double.parseDouble(value.toString());
    }

    /**
     * Returns the column value converted to an {@code int}.
     * Truncates decimal portions if present in string format.
     *
     * @return the integer value, or {@code -1} if the value is {@code null}
     */
    public int intValue() {
        Object value = this.value();

        if (value == null)
            return -1;

        String svalue = value.toString();
        if (svalue.lastIndexOf('.') != -1) {
            svalue = svalue.substring(0, svalue.indexOf('.'));
        }

        return Integer.parseInt(svalue);
    }

    /**
     * Returns the column value converted to a {@code long}.
     * Truncates decimal portions if present in string format.
     *
     * @return the long value, or {@code -1} if the value is {@code null}
     */
    public long longValue() {
        Object value = this.value();

        if (value == null)
            return -1;

        String svalue = value.toString();
        if (svalue.lastIndexOf('.') != -1) {
            svalue = svalue.substring(0, svalue.indexOf('.'));
        }

        return Long.parseLong(svalue);
    }

    /**
     * Returns the column value converted to a {@code float}.
     *
     * @return the float value, or {@code 0.0f} if the value is {@code null}
     */
    public float floatValue() {
        Object value = this.value();

        if (value == null)
            return 0.0f;

        return Float.parseFloat(value.toString());
    }

    /**
     * Returns the column value converted to a {@code boolean}.
     * Interprets {@code "1"} as {@code true}, {@code "0"} as {@code false},
     * and otherwise falls back to {@link Boolean#parseBoolean(String)}.
     *
     * @return the boolean value, or {@code false} if the value is {@code null}
     */
    public boolean booleanValue() {
        Object value = this.value();
        if (value == null)
            return false;

        String booleanValue = value.toString();
        if ("1".equals(booleanValue))
            return true;
        else if ("0".equals(booleanValue))
            return false;
        else
            return Boolean.parseBoolean(booleanValue);
    }

    /**
     * Returns the column value cast to a byte array.
     *
     * @return the byte array value, or {@code null} if the value is {@code null}
     */
    public byte[] byteArrayValue() {
        Object value = this.value();
        if (value == null)
            return null;
        return (byte[]) value;
    }

    /**
     * Determines the {@link FieldType} corresponding to the runtime type of the given object.
     *
     * @param object the object to inspect
     * @return the resolved {@link FieldType}, defaulting to {@link FieldType#STRING}
     */
    public FieldType typeOf(Object object) {
        if (object instanceof Integer) {
            return FieldType.INTEGER;
        } else if (object instanceof String) {
            return FieldType.STRING;
        } else if (object instanceof Double) {
            return FieldType.DOUBLE;
        } else if (object instanceof Float) {
            return FieldType.FLOAT;
        } else if (object instanceof Long) {
            return FieldType.LONG;
        } else if (object instanceof Boolean) {
            return FieldType.BOOLEAN;
        } else if (object instanceof Date) {
            return FieldType.DATE;
        } else if (object instanceof LocalDateTime) {
            return FieldType.DATETIME;
        } else if (object instanceof Blob) {
            return FieldType.BLOB;
        }

        return FieldType.STRING;
    }

    /**
     * Returns a JSON-formatted string representation of the field info properties,
     * with special characters properly escaped.
     *
     * @return a JSON fragment of key-value pairs representing this field info
     */
    @Override
    public String toString() {
        StringBuilder to = new StringBuilder();
        String key, value;

        for (Enumeration<String> f = this.keys(); f.hasMoreElements(); ) {
            key = f.nextElement();

            value = this.get(key).toString().replaceAll("\"", "\\\\\"");
            value = value.replaceAll("'", "\\\\'");
            value = value.replaceAll("\\[", "\\\\[");
            value = value.replaceAll("\\]", "\\\\]");
            value = value.replaceAll("\\{", "\\\\{");
            value = value.replaceAll("\\}", "\\\\}");

            if (to.length() == 0) {
                to.append(" \"").append(key).append("\":\"").append(value).append("\"");
            } else {
                to.append(", \"").append(key).append("\":\"").append(value).append("\"");
            }
        }

        return to.toString();
    }

    /**
     * Returns the {@link FieldType} parsed from the stored {@code "type"} property.
     *
     * @return the {@link FieldType} of this field
     */
    public FieldType getType() {
        return FieldType.valueOf(this.get("type").toString());
    }

    /**
     * Returns the column value converted to a {@link Timestamp}.
     * Defaults to {@code Timestamp.valueOf("1982-03-20")} if the value is {@code null}.
     *
     * @return the {@link Timestamp} representation
     */
    public Timestamp timestampValue() {

        Object value = this.value();
        if (value == null)
            return Timestamp.valueOf("1982-03-20");
        return Timestamp.valueOf(value.toString());
    }

    /**
     * Returns the column value as a {@link Date}.
     * Defaults to a new {@link Date} instance if the value is not an instance of {@link Date}.
     *
     * @return the {@link Date} representation
     */
    public Date dateValue() {
        Object value = this.value();
        if (value instanceof Date)
            return (Date) value;

        return new Date();
    }

    /**
     * Returns the column value converted to a {@link LocalDateTime}.
     * Supports {@link LocalDateTime}, {@link java.sql.Timestamp}, or parsing ISO/space-delimited
     * date-time strings. Defaults to {@link LocalDateTime#now()} on failure or {@code null}.
     *
     * @return the {@link LocalDateTime} representation
     */
    public LocalDateTime localDateTimeValue() {
        Object value = this.value();
        if (value instanceof LocalDateTime)
            return (LocalDateTime) value;

        if (value instanceof java.sql.Timestamp)
            return ((java.sql.Timestamp) value).toLocalDateTime();

        if (value != null && !value.toString().isEmpty()) {
            try {
                return LocalDateTime.parse(value.toString().replace(' ', 'T'));
            } catch (Exception e) {
                // Ignore parsing errors and return now()
            }
        }

        return LocalDateTime.now();
    }

    /**
     * Computes the hash code for this {@code FieldInfo} based on its map entries,
     * name, column name, length, and autoIncrement flag.
     *
     * @return the computed hash code
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (autoIncrement ? 1231 : 1237);
        result = prime * result + ((column == null) ? 0 : column.hashCode());
        result = prime * result + length;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /**
     * Compares this {@code FieldInfo} with another object for equality.
     *
     * @param obj the reference object to compare with
     * @return {@code true} if this object is equal to the obj argument; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof FieldInfo))
            return false;
        FieldInfo other = (FieldInfo) obj;
        if (autoIncrement != other.autoIncrement)
            return false;
        if (column == null) {
            if (other.column != null)
                return false;
        } else if (!column.equals(other.column))
            return false;
        if (length != other.length)
            return false;
        if (name == null) {
            return other.name == null;
        } else return name.equals(other.name);
    }

}
