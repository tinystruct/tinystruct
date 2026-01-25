package org.tinystruct.data.repository;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.Repository;
import org.tinystruct.data.component.Field;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;

/**
 * Repository implementation for Redis database.
 */
public class RedisServer implements Repository {

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> commands;

    /**
     * Constructor for creating a Redis instance.
     */
    public RedisServer() {
        // Create a RedisClient
        client = RedisClient.create("redis://localhost");

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
            throw new ApplicationException("Failed to append record to Redis: " + e.getMessage());
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
            // Update record in Redis hash
            // Extract field names and values from the Field object
            for (String fieldName : ready_fields.keySet()) {
                String value = ready_fields.get(fieldName).toString();
                commands.hset(table, fieldName, value);
            }
            return true;
        } catch (Exception e) {
            throw new ApplicationException("Failed to update record in Redis: " + e.getMessage());
        }
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
            commands.hdel(table, Id.toString());
            return true;
        } catch (Exception e) {
            throw new ApplicationException("Failed to delete record from Redis: " + e.getMessage());
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
            // Not supported for Redis
            throw new ApplicationException("Find operation not supported for Redis");
        } catch (Exception e) {
            throw new ApplicationException("Failed to perform find operation on Redis: " + e.getMessage());
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
            // Not supported for Redis
            throw new ApplicationException("FindOne operation not supported for Redis");
        } catch (Exception e) {
            throw new ApplicationException("Failed to perform findOne operation on Redis: " + e.getMessage());
        }
    }
}
