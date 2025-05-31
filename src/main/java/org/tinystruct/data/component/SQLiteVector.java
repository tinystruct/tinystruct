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
import java.util.*;
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
    private static final int NUM_HASH_TABLES = 4;  // Number of hash tables for LSH
    private static final int NUM_PROJECTIONS = 8;  // Number of random projections per hash table
    private final double[][] randomProjections;    // Random projection vectors for LSH

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
                    "entity_type TEXT NOT NULL," +
                    "hash_values TEXT NOT NULL)";  // Store hash values as comma-separated string
            dbOperator.execute(createTableSQL);

            // Initialize random projections for LSH
            this.randomProjections = new double[NUM_HASH_TABLES][NUM_PROJECTIONS];
            Random random = new Random(42);  // Fixed seed for reproducibility
            for (int i = 0; i < NUM_HASH_TABLES; i++) {
                for (int j = 0; j < NUM_PROJECTIONS; j++) {
                    randomProjections[i][j] = random.nextGaussian();
                }
            }
        } catch (ApplicationException e) {
            LOGGER.log(Level.SEVERE, "Error initializing SQLite database connection", e);
            throw new ApplicationRuntimeException("Failed to initialize SQLite database connection", e);
        }
    }

    /**
     * Computes LSH hash values for a vector.
     *
     * @param vector The vector to hash
     * @return Array of hash values, one per hash table
     */
    private int[] computeHashValues(double[] vector) {
        int[] hashValues = new int[NUM_HASH_TABLES];
        for (int i = 0; i < NUM_HASH_TABLES; i++) {
            double projection = 0;
            for (int j = 0; j < NUM_PROJECTIONS; j++) {
                projection += vector[j] * randomProjections[i][j];
            }
            hashValues[i] = projection > 0 ? 1 : 0;
        }
        return hashValues;
    }

    /**
     * Converts hash values to a string representation for storage.
     */
    private String hashValuesToString(int[] hashValues) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hashValues.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(hashValues[i]);
        }
        return sb.toString();
    }

    /**
     * Converts string representation back to hash values.
     */
    private int[] stringToHashValues(String hashString) {
        String[] parts = hashString.split(",");
        int[] hashValues = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            hashValues[i] = Integer.parseInt(parts[i]);
        }
        return hashValues;
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
        // Compute hash values for the vector
        int[] hashValues = computeHashValues(vector);
        String hashString = hashValuesToString(hashValues);

        // SQL query to insert a new vector into the database
        String sql = "INSERT INTO vectors (vector, label, entity_id, entity_type, hash_values) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = dbOperator.preparedStatement(sql, new Object[]{
                toByteArray(vector), label, entityId, entityType, hashString})) {
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
        // Compute hash values for the query vector
        int[] queryHashValues = computeHashValues(queryVector);
        String queryHashString = hashValuesToString(queryHashValues);

        // Find candidate vectors using LSH
        String sql = "SELECT * FROM vectors WHERE hash_values = ?";
        List<SearchResult> results = new ArrayList<>();
        Set<Integer> processedIds = new HashSet<>();  // Track processed vectors to avoid duplicates

        try (PreparedStatement statement = dbOperator.preparedStatement(sql, new Object[]{queryHashString});
             ResultSet resultSet = dbOperator.executeQuery(statement)) {
            
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                if (processedIds.contains(id)) continue;
                processedIds.add(id);

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

        // If we don't have enough results, fall back to exact search
        if (results.size() < topK) {
            String fallbackSql = "SELECT * FROM vectors WHERE id NOT IN (" + 
                String.join(",", processedIds.stream().map(String::valueOf).toArray(String[]::new)) + ")";
            try (ResultSet resultSet = dbOperator.query(fallbackSql)) {
                while (resultSet.next() && results.size() < topK) {
                    byte[] vectorBytes = resultSet.getBytes("vector");
                    double[] vector = toDoubleArray(vectorBytes);
                    String label = resultSet.getString("label");
                    int entityId = resultSet.getInt("entity_id");
                    String entityType = resultSet.getString("entity_type");
                    double similarity = cosineSimilarity(queryVector, vector);
                    results.add(new SearchResult(vector, label, entityId, entityType, similarity));
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error in fallback search", e);
            }
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

