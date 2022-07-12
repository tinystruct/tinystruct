/*******************************************************************************
 * Copyright  (c) 2013, 2017 James Mover Zhou
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

import java.sql.Types;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.Vector;

public final class FieldType {

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>INTEGER</code>.
     */
    private final static int _INTEGER = Types.INTEGER;
    public final static FieldType INTEGER = new FieldType("INTEGER", _INTEGER, "int");

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>BIGINT</code>.
     */
    private final static int _BIGINT = Types.BIGINT;
    public final static FieldType BIGINT = new FieldType("BIGINT", _BIGINT, "int");

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>BIT</code>.
     */
    private final static int _BIT = Types.BIT;
    public final static FieldType BIT = new FieldType("BIT", _BIT, "boolean");

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>TINYINT</code>.
     */
    private final static int _TINYINT = Types.TINYINT;
    public final static FieldType TINYINT = new FieldType("TINYINT", _TINYINT, "int");
    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>SMALLINT</code>.
     */
    private final static int _SMALLINT = Types.SMALLINT;
    public final static FieldType SMALLINT = new FieldType("SMALLINT", _SMALLINT, "int");

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>FLOAT</code>.
     */
    private final static int _FLOAT = Types.FLOAT;
    public final static FieldType FLOAT = new FieldType("FLOAT", _FLOAT, "float");
    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>REAL</code>.
     */
    private final static int _REAL = Types.REAL;
    public final static FieldType REAL = new FieldType("REAL", _REAL, "real");

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>DOUBLE</code>.
     */
    private final static int _DOUBLE = Types.DOUBLE;
    public final static FieldType DOUBLE = new FieldType("DOUBLE", _DOUBLE, "double");
    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>NUMERIC</code>.
     */
    private final static int _NUMERIC = Types.NUMERIC;
    public final static FieldType NUMERIC = new FieldType("NUMERIC", _NUMERIC, "int");
    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>DECIMAL</code>.
     */
    private final static int _DECIMAL = Types.DECIMAL;
    public final static FieldType DECIMAL = new FieldType("DECIMAL", _DECIMAL, "int");
    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>CHAR</code>.
     */
    private final static int _CHAR = Types.CHAR;
    public final static FieldType CHAR = new FieldType("CHAR", _CHAR, "CHAR");
    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>VARCHAR</code>.
     */
    private final static int _VARCHAR = Types.VARCHAR;
    public final static FieldType VARCHAR = new FieldType("VARCHAR", _VARCHAR, "String");
    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>LONGVARCHAR</code>.
     */
    private final static int _LONGVARCHAR = Types.LONGVARCHAR;
    public final static FieldType LONGVARCHAR = new FieldType("LONGVARCHAR", _LONGVARCHAR, "String");

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>DATE</code>.
     */
    private final static int _DATE = Types.DATE;
    public final static FieldType DATE = new FieldType("DATE", _DATE, "Date");
    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>TIME</code>.
     */
    private final static int _TIME = Types.TIME;
    public final static FieldType TIME = new FieldType("TIME", _TIME, "Time");

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>BINARY</code>.
     */
    private final static int _BINARY = Types.BINARY;
    public final static FieldType BINARY = new FieldType("BINARY", _BINARY, "BINARY");
    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>VARBINARY</code>.
     */
    private final static int _VARBINARY = Types.VARBINARY;
    public final static FieldType VARBINARY = new FieldType("VARBINARY", _VARBINARY, "VARBINARY");
    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>LONGVARBINARY</code>.
     */
    private final static int _LONGVARBINARY = Types.LONGVARBINARY;
    public final static FieldType LONGVARBINARY = new FieldType("LONGVARBINARY", _LONGVARBINARY, "LONGVARBINARY");
    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * <code>NULL</code>.
     */
    private final static int _NULL = Types.NULL;
    public final static FieldType NULL = new FieldType("NULL", _NULL, "NULL");
    /**
     * The constant in the Java programming language that indicates
     * that the SQL type is database-specific and
     * gets mapped to a Java object that can be accessed via
     * the methods <code>getObject</code> and <code>setObject</code>.
     */
    private final static int _OTHER = Types.OTHER;
    public final static FieldType OTHER = new FieldType("OTHER", _OTHER, "OTHER");

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * <code>JAVA_OBJECT</code>.
     *
     * @since 1.2
     */
    private final static int _JAVA_OBJECT = Types.JAVA_OBJECT;
    public final static FieldType JAVA_OBJECT = new FieldType("JAVA_OBJECT", _JAVA_OBJECT, "JAVA_OBJECT");
    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * <code>DISTINCT</code>.
     *
     * @since 1.2
     */
    private final static int _DISTINCT = Types.DISTINCT;
    public final static FieldType DISTINCT = new FieldType("DISTINCT", _DISTINCT, "DISTINCT");
    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * <code>STRUCT</code>.
     *
     * @since 1.2
     */
    private final static int _STRUCT = Types.STRUCT;
    public final static FieldType STRUCT = new FieldType("STRUCT", _STRUCT, "STRUCT");
    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * <code>ARRAY</code>.
     *
     * @since 1.2
     */
    private final static int _ARRAY = Types.ARRAY;
    public final static FieldType ARRAY = new FieldType("ARRAY", _ARRAY, "ARRAY");
    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * <code>BLOB</code>.
     *
     * @since 1.2
     */
    private final static int _BLOB = Types.BLOB;
    public final static FieldType BLOB = new FieldType("BLOB", _BLOB, "BLOB");
    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * <code>CLOB</code>.
     *
     * @since 1.2
     */
    private final static int _CLOB = Types.CLOB;
    public final static FieldType CLOB = new FieldType("CLOB", _CLOB, "CLOB");
    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * <code>REF</code>.
     *
     * @since 1.2
     */
    private final static int _REF = Types.REF;
    public final static FieldType REF = new FieldType("REF", _REF, "REF");
    /**
     * The constant in the Java programming language, somtimes referred to
     * as a type code, that identifies the generic SQL type <code>DATALINK</code>.
     *
     * @since 1.4
     */
    private final static int _DATALINK = Types.DATALINK;
    public final static FieldType DATALINK = new FieldType("DATALINK", _DATALINK, "DATALINK");
    /**
     * The constant in the Java programming language, somtimes referred to
     * as a type code, that identifies the generic SQL type <code>BOOLEAN</code>.
     *
     * @since 1.4
     */
    private final static int _BOOLEAN = Types.BOOLEAN;
    public final static FieldType BOOLEAN = new FieldType("BIT", _BOOLEAN, "boolean");

    public final static FieldType INT = new FieldType("INT", 0, "int");
    public final static FieldType STRING = new FieldType("STRING", 0, "String");
    public final static FieldType LONG = new FieldType("LONG", 0, "long");
    public final static FieldType TEXT = new FieldType("TEXT", 0, "String");
    public final static FieldType LONGTEXT = new FieldType("LONGTEXT", 0, "String");


    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * {@code TIME WITH TIMEZONE}.
     *
     * @since 1.8
     */
    private final static int _DATETIME = Types.TIME_WITH_TIMEZONE;
    public final static FieldType DATETIME = new FieldType("DATETIME", _DATETIME, "LocalDateTime");

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * {@code TIMESTAMP WITH TIMEZONE}.
     *
     * @since 1.8
     */
    private final static int _TIMESTAMP = Types.TIMESTAMP_WITH_TIMEZONE;
    public final static FieldType TIMESTAMP = new FieldType("TIMESTAMP", _TIMESTAMP, "Timestamp");

    public static final long serialVersionUID = -2800196753010521325L;
    private String typeName;
    private int value;
    private static Vector<FieldType> typeList;
    private final String realType;

    private FieldType(String typeName, int value, String realType) {
        this.typeName = typeName;
        this.value = value;
        this.realType = realType;

        if (typeList == null)
            typeList = new Vector<FieldType>();
        typeList.add(this);
    }

    public String getRealType() {
        return this.realType;
    }

    public String getTypeName() {
        return this.typeName;
    }

    public boolean equals(Object o) {
        if (o instanceof FieldType) {
            FieldType t = (FieldType) o;

            t.typeName = this.typeName;
            t.value = this.value;

            return true;
        }
        return false;
    }

    public int hashCode() {
        return super.hashCode();
    }

    public String toString() {
        return this.typeName;
    }

    public static FieldType valueOf(String s) {
        FieldType currentType;
        for (FieldType fieldType : typeList) {
            currentType = fieldType;

            if (currentType.typeName.equalsIgnoreCase(s)) {
                return currentType;
            }
        }
        return FieldType.NULL;
    }

};


