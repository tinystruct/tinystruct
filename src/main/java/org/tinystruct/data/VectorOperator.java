package org.tinystruct.data;

import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * VectorOperator is a specialized DatabaseOperator for managing vector operations
 * with an SQLite database. It handles connections, statement preparation, and
 * exception management tailored to vector data storage and retrieval.
 */
public class VectorOperator extends DatabaseOperator {
    private static final Logger logger = Logger.getLogger(VectorOperator.class.getName());

    // SQLState code for communication link failure, used for specific exception handling.
    private static final String SQL_STATE_COMMUNICATION_LINK_FAILURE = "08S01";

    /**
     * Constructor for VectorOperator. It initializes the database connection
     * using the provided configuration settings or defaults.
     *
     * @throws ApplicationException If an error occurs while initializing the connection.
     */
    public VectorOperator() throws ApplicationException {
        super(initializeConnection());
    }

    /**
     * Initializes the database connection and returns it.
     *
     * @return Connection object
     * @throws ApplicationException If an error occurs during connection initialization
     */
    private static Connection initializeConnection() throws ApplicationException {
        // Load configuration settings for the vector database
        Configuration<String> context = new Settings();

        String driver = context.get("vector.database.driver").isEmpty() ? "org.sqlite.JDBC" : context.get("vector.database.driver");
        String url = context.get("vector.database.url").isEmpty() ? "jdbc:sqlite:src/main/resources/vector.db" : context.get("vector.database.url");

        try {
            // Load the database driver class
            Class.forName(driver);

            // Establish the database connection
            return DriverManager.getConnection(url);
        } catch (ClassNotFoundException | SQLException e) {
            // Wrap and throw exceptions that occur during initialization
            throw new ApplicationRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Closes the VectorOperator by closing the prepared statement and connection.
     * Proper resource management ensures no resources are left open.
     */
    @Override
    public void close() {
        // Close the result set if open
        closeResultSet();
        try {
            // Close the prepared statement if it exists
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        } catch (SQLException e) {
            // Wrap and throw any SQLExceptions that occur during statement closure
            throw new ApplicationRuntimeException(e.getMessage(), e);
        } finally {
            try {
                // Close the database connection
                if (this.connection != null && !this.connection.isClosed()) {
                    this.connection.close();
                }
            } catch (SQLException ignore) {
                // Silently ignore exceptions during connection closure
            }
        }
    }

    /**
     * Handles SQLExceptions that occur during database operations.
     * Specifically checks for communication link failures and attempts to
     * close the connection before re-throwing the exception.
     *
     * @param e         The SQLException to handle.
     * @param statement The PreparedStatement that caused the exception.
     * @throws ApplicationException If an error occurs while handling the exception.
     */
    @Override
    public void handleSQLException(SQLException e, PreparedStatement statement) throws ApplicationException {
        // Check if the exception is due to a communication link failure
        if (SQL_STATE_COMMUNICATION_LINK_FAILURE.equals(e.getSQLState())) {
            // Attempt to close the result set and connection gracefully
            closeResultSet();
            try {
                if (this.connection != null && !this.connection.isClosed()) {
                    this.connection.close();
                }
            } catch (SQLException ignore) {
                // Silently ignore exceptions during connection closure
            }
        } else {
            // Log the exception details including SQLState and vendor code
            logger.severe("SQLState(" + e.getSQLState() + ") vendor code(" + e.getErrorCode() + "); Query: " + statement + " Message: " + e.getMessage());

            // Re-throw the exception wrapped in an ApplicationException
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    public Connection getConnection() {
        return this.connection;
    }
}
