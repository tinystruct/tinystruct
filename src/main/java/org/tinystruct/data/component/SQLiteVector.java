package org.tinystruct.data.component;

import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.Data;
import org.tinystruct.data.DatabaseOperator;
import org.tinystruct.data.Vector;
import org.tinystruct.data.VectorOperator;

import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.tinystruct.data.tools.CosineSimilarity.cosineSimilarity;

/**
 * SQLiteVector manages vectors stored in an SQLite database,
 * providing functionality to add, search, and delete vectors
 * with individual database connections.
 */
public class SQLiteVector implements Vector {
    private static final Logger LOGGER = Logger.getLogger(SQLiteVector.class.getName());
    private final DatabaseOperator dbOperator;

    public SQLiteVector() {
        try {
            // Initialize DatabaseOperator with the database
            this.dbOperator = new VectorOperator();

            // Initialize the vectors table if it doesn't exist
            String createTableSQL = "CREATE TABLE IF NOT EXISTS vectors (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "vector BLOB NOT NULL," +
                    "label TEXT NOT NULL," +
                    "entity_id INTEGER NOT NULL," +
                    "entity_type TEXT NOT NULL)";
            dbOperator.execute(createTableSQL);
        } catch (ApplicationException e) {
            LOGGER.log(Level.SEVERE, "Error initializing SQLite database connection", e);
            throw new ApplicationRuntimeException("Failed to initialize SQLite database connection", e);
        }
    }

    /**
     * Adds a vector to the database with the associated label, entity ID, and entity type.
     *
     * @param vector     The vector to add, represented as a double array.
     * @param label      The label associated with the vector.
     * @param entityId   The ID of the entity associated with the vector.
     * @param entityType The type of the entity associated with the vector.
     * @throws ApplicationException If an error occurs while adding the vector to the database.
     */
    @Override
    public void add(double[] vector, String label, int entityId, String entityType) throws ApplicationException {
        // SQL query to insert a new vector into the database
        String sql = "INSERT INTO vectors (vector, label, entity_id, entity_type) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = dbOperator.preparedStatement(sql, new Object[]{
                toByteArray(vector), label, entityId, entityType})) {
            // Execute the SQL statement to add the vector
            dbOperator.execute(statement);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding vector to SQLite", e);
            throw new ApplicationException("Failed to add vector to SQLite", e);
        }
    }

    /**
     * Searches the database for vectors similar to the provided query vector.
     * Returns the top K most similar vectors based on cosine similarity.
     *
     * @param queryVector The vector to search for, represented as a double array.
     * @param topK        The number of top similar vectors to return.
     * @return A list of SearchResult objects containing the top K similar vectors.
     * @throws ApplicationException If an error occurs while searching the database.
     */
    @Override
    public List<SearchResult> search(double[] queryVector, int topK) throws ApplicationException {
        String sql = "SELECT * FROM vectors";
        List<SearchResult> results = new ArrayList<>();

        try (ResultSet resultSet = dbOperator.query(sql)) {
            while (resultSet.next()) {
                byte[] vectorBytes = resultSet.getBytes("vector");
                double[] vector = toDoubleArray(vectorBytes);
                String label = resultSet.getString("label");
                int entityId = resultSet.getInt("entity_id");
                String entityType = resultSet.getString("entity_type");
                double similarity = cosineSimilarity(queryVector, vector);
                results.add(new SearchResult(vector, label, entityId, entityType, similarity));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching vectors in SQLite", e);
            throw new ApplicationException("Failed to search vectors in SQLite", e);
        }

        results.sort(Comparator.comparingDouble(SearchResult::getSimilarity).reversed());
        return results.subList(0, Math.min(topK, results.size()));
    }

    /**
     * Deletes vectors from the database based on the provided list of vector IDs.
     *
     * @param ids A list of vector IDs to delete.
     * @return True if the deletion was successful, false otherwise.
     * @throws ApplicationException If an error occurs while deleting vectors from the database.
     */
    @Override
    public boolean delete(List<String> ids) throws ApplicationException {
        // Check if there are IDs to delete
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        // SQL query to delete vectors by ID
        String sql = "DELETE FROM vectors WHERE id = ?";
        boolean success = true;

        for (String id : ids) {
            try (PreparedStatement statement = dbOperator.preparedStatement(sql, new Object[]{id})) {
                int affectedRows = dbOperator.executeUpdate(statement);
                // If no rows were affected, the deletion was not successful
                if (affectedRows == 0) {
                    success = false;
                    LOGGER.log(Level.WARNING, "No vector found with ID: " + id);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error deleting vector with ID: " + id, e);
                throw new ApplicationException("Failed to delete vector with ID: " + id, e);
            }
        }

        return success;
    }

    @Override
    public List<Data> similarity(String query) {
        return List.of();
    }

    /**
     * Converts a byte array to a double array.
     *
     * @param bytes The byte array to convert.
     * @return The resulting double array.
     */
    private double[] toDoubleArray(byte[] bytes) {
        double[] doubles = new double[bytes.length / 8];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = ByteBuffer.wrap(bytes, i * 8, 8).getDouble();
        }
        return doubles;
    }

    /**
     * Converts a double array to a byte array.
     *
     * @param doubles The double array to convert.
     * @return The resulting byte array.
     */
    private byte[] toByteArray(double[] doubles) {
        ByteBuffer buffer = ByteBuffer.allocate(doubles.length * 8);
        for (double d : doubles) {
            buffer.putDouble(d);
        }
        return buffer.array();
    }
}

