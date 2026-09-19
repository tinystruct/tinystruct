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
package org.tinystruct.data.tools;

import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.DatabaseOperator;
import org.tinystruct.data.component.FieldInfo;
import org.tinystruct.data.component.FieldType;
import org.tinystruct.data.repository.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Creates the table of a mapped class when it does not exist yet, from the column
 * descriptors of its mapping (annotations or XML).
 * <p>
 * The existence guard is part of the statement itself ({@code CREATE TABLE IF NOT
 * EXISTS}, or an {@code OBJECT_ID} check on SQL Server), so creation is a single
 * atomic round trip that is safe when several application instances start together.
 * Types are the portable names of {@link FieldType}; each is rendered with the type
 * the target database actually has. Only a plain length is available in the mapping,
 * so a {@code DECIMAL} column is created without precision or scale.
 * </p>
 */
public final class TableCreator {
    private static final int DEFAULT_LENGTH = 255;
    private static final Pattern COLUMN_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*");
    // Table names arrive already quoted for the dialect and may carry a schema.
    private static final Pattern TABLE_NAME = Pattern.compile("[A-Za-z0-9_$.`\"\\[\\]]+");

    private TableCreator() {
    }

    /**
     * Creates the table if it does not exist, using a connection from the pool.
     *
     * @param dialect the database type
     * @param table   the table name, already quoted for the dialect
     * @param id      the identifier column, or {@code null} for a table without one
     * @param columns the remaining columns
     * @throws ApplicationException if the statement cannot be executed
     */
    public static void create(Type dialect, String table, FieldInfo id, List<FieldInfo> columns)
            throws ApplicationException {
        String sql = createStatement(dialect, table, id, columns);

        try (DatabaseOperator operator = new DatabaseOperator()) {
            // The statement is built from validated identifiers and fixed type names,
            // and the injection check rejects every DDL statement.
            operator.disableSafeCheck();
            operator.execute(sql);
        }
    }

    /**
     * Builds the statement that creates the table only if it does not exist.
     *
     * @throws ApplicationRuntimeException if the dialect has no tables, a name is not a
     *                                     plain identifier, or a column type cannot be
     *                                     mapped
     */
    public static String createStatement(Type dialect, String table, FieldInfo id, List<FieldInfo> columns) {
        if (dialect == Type.Redis) {
            throw new ApplicationRuntimeException("Tables cannot be created for " + dialect);
        }
        if (table == null || !TABLE_NAME.matcher(table).matches()) {
            throw new ApplicationRuntimeException("Invalid table name: " + table);
        }

        List<String> definitions = new ArrayList<>();
        if (id != null) {
            definitions.add(idDefinition(dialect, id));
        }
        for (FieldInfo column : columns) {
            definitions.add(quote(dialect, column.getColumnName()) + " " + sqlType(dialect, column));
        }
        if (definitions.isEmpty()) {
            throw new ApplicationRuntimeException("No columns are mapped for table " + table);
        }

        String body = table + " (" + String.join(", ", definitions) + ")";
        return dialect == Type.SQLServer
                ? "IF OBJECT_ID(N'" + table + "', N'U') IS NULL CREATE TABLE " + body
                : "CREATE TABLE IF NOT EXISTS " + body;
    }

    private static String idDefinition(Type dialect, FieldInfo id) {
        String name = quote(dialect, id.getColumnName());
        String type = sqlType(dialect, id);

        if (!id.isAutoIncrement()) {
            return name + " " + type + " NOT NULL PRIMARY KEY";
        }

        switch (dialect) {
            case SQLite:
                // AUTOINCREMENT is only valid on an INTEGER PRIMARY KEY.
                return name + " INTEGER PRIMARY KEY AUTOINCREMENT";
            case MySQL:
                return name + " " + type + " NOT NULL AUTO_INCREMENT PRIMARY KEY";
            case SQLServer:
                return name + " " + type + " IDENTITY(1,1) PRIMARY KEY";
            default: // PostgreSQL, H2
                return name + " " + type + " GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY";
        }
    }

    /**
     * Quotes a column the way the repositories address it: MySQL and SQL Server quote
     * their identifiers, the others fold unquoted names to their own case.
     */
    private static String quote(Type dialect, String name) {
        if (name == null || !COLUMN_NAME.matcher(name).matches()) {
            throw new ApplicationRuntimeException("Invalid column name: " + name);
        }

        switch (dialect) {
            case MySQL:
                return '`' + name + '`';
            case SQLServer:
                return '[' + name + ']';
            default:
                return name;
        }
    }

    /**
     * Renders the column type for the dialect.
     */
    private static String sqlType(Type dialect, FieldInfo column) {
        FieldType type = column.getType();
        String name = type == null ? "" : type.getTypeName();
        int length = column.getLength() > 0 ? column.getLength() : DEFAULT_LENGTH;

        switch (name) {
            case "INT":
            case "INTEGER":
                return "INTEGER";
            case "BIGINT":
            case "LONG":
                return "BIGINT";
            case "SMALLINT":
                return "SMALLINT";
            case "TINYINT":
                return dialect == Type.PostgreSQL ? "SMALLINT" : "TINYINT";
            case "BIT":
            case "BOOLEAN":
                return dialect == Type.SQLServer ? "BIT" : "BOOLEAN";
            case "FLOAT":
                return "FLOAT";
            case "REAL":
                return "REAL";
            case "DOUBLE":
                return dialect == Type.SQLServer ? "FLOAT"
                        : dialect == Type.PostgreSQL ? "DOUBLE PRECISION" : "DOUBLE";
            case "NUMERIC":
            case "DECIMAL":
                return "DECIMAL";
            case "STRING":
            case "VARCHAR":
            case "CHARACTER VARYING":
            case "LONGVARCHAR":
            case "ENUM":
            case "SET":
                return "VARCHAR(" + length + ")";
            case "CHAR":
                return "CHAR(" + (column.getLength() > 0 ? column.getLength() : 1) + ")";
            case "TEXT":
            case "LONGTEXT":
            case "CLOB":
                return dialect == Type.SQLServer ? "VARCHAR(MAX)"
                        : dialect == Type.MySQL && !"TEXT".equals(name) ? "LONGTEXT" : "TEXT";
            case "UUID":
                return dialect == Type.PostgreSQL || dialect == Type.H2 ? "UUID" : "CHAR(36)";
            case "JSON":
            case "JSONB":
                return dialect == Type.MySQL ? "JSON"
                        : dialect == Type.PostgreSQL ? name
                        : dialect == Type.SQLServer ? "VARCHAR(MAX)" : "TEXT";
            case "DATE":
                return "DATE";
            case "TIME":
                return "TIME";
            case "DATETIME":
            case "SMALLDATETIME":
            case "TIMESTAMP":
                return dialect == Type.MySQL ? "DATETIME" : dialect == Type.SQLServer ? "DATETIME2" : "TIMESTAMP";
            case "BINARY":
            case "VARBINARY":
            case "LONGVARBINARY":
            case "BLOB":
                return dialect == Type.PostgreSQL ? "BYTEA" : dialect == Type.SQLServer ? "VARBINARY(MAX)" : "BLOB";
            default:
                throw new ApplicationRuntimeException("Cannot create column '" + column.getColumnName()
                        + "': unsupported type '" + name + "'");
        }
    }
}
