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
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

public class FieldInfo extends ConcurrentHashMap<String, Object> {
    private final static long serialVersionUID = 1;
    private String name;
    private String column;
    private boolean autoIncrement;
    private int length;

    public FieldInfo() {
        super(8);

        this.autoIncrement = false;
        this.length = 0;
    }

    public FieldInfo(String name) {
        this();
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        if (this.containsKey("name"))
            this.name = String.valueOf(this.get("name"));
        return this.name;
    }

    public String getColumnName() {
        if (this.containsKey("column"))
            this.column = String.valueOf(this.get("column"));
        return this.column;
    }

    public int getLength() {
        if (this.containsKey("length"))
            this.length = Integer.parseInt(String.valueOf(this.get("length")));
        return this.length;
    }

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

    public void append(String property, Object value) {
        this.set(property, value);
    }

    public void set(String property, Object value) {
        if (property != null && value != null)
            this.put(property, value);
    }

    public Object value() {
        if (!this.containsKey("value"))
            return null;
        return this.get("value");
    }

    public String stringValue() {
        Object value = this.value();

        if (value == null)
            return null;

        return value.toString();
    }

    public double doubleValue() {
        Object value = this.value();

        if (value == null)
            return 0;

        return Double.parseDouble(value.toString());
    }

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

    public boolean booleanValue() {
        Object value = this.value();
        if (value == null)
            return false;
        return Boolean.parseBoolean(value.toString());
    }

    public byte[] byteArrayValue() {
        Object value = this.value();
        if (value == null)
            return null;
        return (byte[]) value;
    }

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

    public FieldType getType() {
        return FieldType.valueOf(this.get("type").toString());
    }

    public Timestamp timestampValue() {

        Object value = this.value();
        if (value == null)
            return Timestamp.valueOf("1982-03-20");
        return Timestamp.valueOf(value.toString());
    }

    public Date dateValue() {
        Object value = this.value();
        if (value instanceof Date)
            return (Date) value;

        return new Date();
    }

    public LocalDateTime localDateTimeValue() {
        Object value = this.value();
        if (value instanceof LocalDateTime)
            return (LocalDateTime) value;

        return LocalDateTime.now();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
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

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
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
