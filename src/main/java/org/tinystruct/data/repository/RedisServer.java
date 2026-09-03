package org.tinystruct.data.repository;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.Repository;
import org.tinystruct.data.component.Field;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;
import org.tinystruct.data.component.FieldInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository implementation for Redis database.
 */
public class RedisServer implements Repository {
    private static final Logger logger = Logger.getLogger(RedisServer.class.getName());

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> commands;

    /**
     * Constructor for creating a Redis instance.
     */
    public RedisServer() {
        Configuration<String> config = new Settings();
        String host = config.get("redis.host") != null && !config.get("redis.host").isEmpty() ? config.get("redis.host") : "localhost";
        String port = config.get("redis.port") != null && !config.get("redis.port").isEmpty() ? config.get("redis.port") : "6379";
        String password = config.get("redis.password") != null && !config.get("redis.password").isEmpty() ? config.get("redis.password") : "";

        String uri = "redis://" + (password.isEmpty() ? "" : ":" + password + "@") + host + ":" + port;
        // Create a RedisClient
        client = RedisClient.create(uri);

        // Connect to Redis
        connection = client.connect();

        // Sync API to perform synchronous commands
        commands = connection.sync();
    }

    /**
     * Get the type of the repository.
     *
     * @return the type of the repository.
     */
    @Override
    public Type getType() {
        return Type.Redis;
    }

    /**
     * Append a new record to the Redis database.
     *
     * @param ready_fields the fields ready for insertion.
     * @param table        the table to append the record to (not applicable for
     *                     Redis).
     * @return true if the operation is successful, false otherwise.
     * @throws ApplicationException if an application-specific error occurs.
     */
    @Override
    public boolean append(Field ready_fields, String table) throws ApplicationException {
        Object id = appendAndGetId(ready_fields, table);
        return id != null;
    }

    /**
     * Append a new record to the Redis database and return the generated ID.
     * <p>
     * If a field's "generate" property is set to true, its value will be used as
     * the returned ID.
     * </p>
     *
     * @param ready_fields the fields ready for insertion.
     * @param table        the table to append the record to (not applicable for
     *                     Redis).
     * @return the generated ID if the operation is successful, null otherwise.
     * @throws ApplicationException if an application-specific error occurs.
     */
    @Override
    public Object appendAndGetId(Field ready_fields, String table) throws ApplicationException {
        try {
            // Generate a unique ID for the record
            String id = String.valueOf(commands.incr(table + ":id"));

            // Add the ID to the fields
            ready_fields.get("Id").set("value", id);

            // Create a key for the record
            String recordKey = table + ":" + id;

            // Append record to Redis hash
            // Extract field names and values from the Field object
            for (String fieldName : ready_fields.keySet()) {
                String value = ready_fields.get(fieldName).toString();
                commands.hset(recordKey, fieldName, value);
            }

            // Add the record ID to the set of all records for this table
            commands.sadd(table + ":all", id);

            return id;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to append record to Redis: {0}", e.getMessage());
            throw new ApplicationException("Failed to append record to Redis: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing record in the Redis database.
     *
     * @param ready_fields the fields ready for update.
     * @param table        the table to update the record in (not applicable for
     *                     Redis).
     * @return true if the operation is successful, false otherwise.
     * @throws ApplicationException if an application-specific error occurs.
     */
    @Override
    public boolean update(Field ready_fields, String table) throws ApplicationException {
        try {
            String id = ready_fields.get("Id").value().toString();
            String recordKey = table + ":" + id;
            // Update record in Redis hash
            // Extract field names and values from the Field object
            for (String fieldName : ready_fields.keySet()) {
                String value = ready_fields.get(fieldName).toString();
                commands.hset(recordKey, fieldName, value);
            }
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update record in Redis: {0}", e.getMessage());
            throw new ApplicationException("Failed to update record in Redis: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Field ready_fields, String table, Set<String> onlyFields) throws ApplicationException {
        if (onlyFields == null || onlyFields.isEmpty()) {
            return this.update(ready_fields, table);
        }

        Field filteredFields = new Field();
        for (String fieldName : ready_fields.keySet()) {
            FieldInfo fieldInfo = ready_fields.get(fieldName);
            if ("Id".equalsIgnoreCase(fieldInfo.getName()) || onlyFields.contains(fieldInfo.getName()) || onlyFields.contains(fieldInfo.getColumnName())) {
                filteredFields.append(fieldName, fieldInfo);
            }
        }

        return this.update(filteredFields, table);
    }

    /**
     * Delete a record from the Redis database.
     *
     * @param Id    the identifier of the record to delete.
     * @param table the table to delete the record from (not applicable for Redis).
     * @return true if the operation is successful, false otherwise.
     * @throws ApplicationException if an application-specific error occurs.
     */
    @Override
    public boolean delete(Object Id, String table) throws ApplicationException {
        try {
            // Delete record from Redis hash
            String recordKey = table + ":" + Id.toString();
            commands.del(recordKey);
            commands.srem(table + ":all", Id.toString());
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to delete record from Redis: {0}", e.getMessage());
            throw new ApplicationException("Failed to delete record from Redis: " + e.getMessage(), e);
        }
    }

    /**
     * Find records in the Redis database based on the given SQL query and
     * parameters.
     *
     * @param SQL        the SQL query (not applicable for Redis).
     * @param parameters the parameters for the query (not applicable for Redis).
     * @return a Table containing the query result.
     * @throws ApplicationException if an application-specific error occurs.
     */
    @Override
    public Table find(String SQL, Object[] parameters) throws ApplicationException {
        try {
            String table = SQL; // Simplified: expect SQL to be table name or table name in a query
            if (SQL.toLowerCase().contains("from ")) {
                table = SQL.substring(SQL.toLowerCase().indexOf("from ") + 5).trim().split(" ")[0];
            }
            
            Table resultTable = new Table();
            Set<String> ids = commands.smembers(table + ":all");
            for (String id : ids) {
                String recordKey = table + ":" + id;
                Map<String, String> map = commands.hgetall(recordKey);
                if (!map.isEmpty()) {
                    Row row = new Row();
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        FieldInfo fieldInfo = new FieldInfo();
                        fieldInfo.append("name", entry.getKey());
                        fieldInfo.append("value", entry.getValue());
                        Field field = new Field();
                        field.append(fieldInfo.getName(), fieldInfo);
                        row.append(field);
                    }
                    resultTable.append(row);
                }
            }
            return resultTable;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to perform find operation on Redis: {0}", e.getMessage());
            throw new ApplicationException("Failed to perform find operation on Redis: " + e.getMessage(), e);
        }
    }

    /**
     * Find a single record in the Redis database based on the given SQL query and
     * parameters.
     *
     * @param SQL        the SQL query (not applicable for Redis).
     * @param parameters the parameters for the query (not applicable for Redis).
     * @return a Row containing the query result.
     * @throws ApplicationException if an application-specific error occurs.
     */
    @Override
    public Row findOne(String SQL, Object[] parameters) throws ApplicationException {
        try {
            String table = SQL;
            if (SQL.toLowerCase().contains("from ")) {
                table = SQL.substring(SQL.toLowerCase().indexOf("from ") + 5).trim().split(" ")[0];
            }
            
            String id;
            if (parameters != null && parameters.length > 0) {
                id = parameters[0].toString();
            } else {
                // Try to extract ID from SQL if possible (very basic)
                if (SQL.contains("id=")) {
                    id = SQL.substring(SQL.indexOf("id=") + 3).trim().split(" ")[0].replace("'", "");
                } else {
                    throw new ApplicationException("ID not provided for findOne operation");
                }
            }

            String recordKey = table + ":" + id;
            Map<String, String> map = commands.hgetall(recordKey);
            if (map.isEmpty()) return null;

            Row row = new Row();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                FieldInfo fieldInfo = new FieldInfo();
                fieldInfo.append("name", entry.getKey());
                fieldInfo.append("value", entry.getValue());
                Field field = new Field();
                field.append(fieldInfo.getName(), fieldInfo);
                row.append(field);
            }
            return row;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to perform findOne operation on Redis: {0}", e.getMessage());
            throw new ApplicationException("Failed to perform findOne operation on Redis: " + e.getMessage(), e);
        }
    }
}
