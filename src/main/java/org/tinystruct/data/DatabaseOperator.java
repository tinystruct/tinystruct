package org.tinystruct.data;

import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.tools.SQLInjectionDetector;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseOperator implements Closeable {
    private static final Logger logger = Logger.getLogger(DatabaseOperator.class.getName());
    private static final String SQL_STATE_COMMUNICATION_LINK_FAILURE = "08S01";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private final ConnectionManager manager;
    private boolean injectionCheckEnabled;
    private boolean inTransaction;
    private Savepoint currentSavepoint;
    Connection connection;
    PreparedStatement preparedStatement;
    private ResultSet resultSet;

    /**
     * Default constructor to create a DatabaseOperator and obtain a connection from the ConnectionManager.
     *
     * @throws ApplicationException If an error occurs while obtaining a connection.
     */
    public DatabaseOperator() throws ApplicationException {
        manager = ConnectionManager.getInstance();
        connection = manager.getConnection();
        injectionCheckEnabled = true;
        inTransaction = false;
        currentSavepoint = null;
    }

    /**
     * Constructor to create a DatabaseOperator with a specific database and obtain a connection from the ConnectionManager.
     *
     * @param database The name of the database.
     * @throws ApplicationException If an error occurs while obtaining a connection or setting the database.
     */
    public DatabaseOperator(String database) throws ApplicationException {
        this();
        if (connection != null) {
            setCatalog(database);
        }
    }

    /**
     * Constructor to create a DatabaseOperator with a provided connection.
     *
     * @param connection The connection to use.
     */
    public DatabaseOperator(Connection connection) {
        manager = null;
        this.connection = connection;
        inTransaction = false;
        currentSavepoint = null;
    }

    /**
     * Set the current active database.
     *
     * @param database The name of the database.
     * @throws ApplicationException If an error occurs while setting the database.
     */
    public void setCatalog(String database) throws ApplicationException {
        try {
            connection.setCatalog(database);
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Prepare a PreparedStatement with the given SQL and parameters.
     *
     * @param sql        The SQL query.
     * @param parameters An array of parameters.
     * @return The prepared statement.
     * @throws ApplicationException If an error occurs while preparing the statement.
     */
    public PreparedStatement preparedStatement(String sql, Object[] parameters) throws ApplicationException {
        if (sql == null || sql.trim().isEmpty()) {
            throw new ApplicationException("SQL statement is NULL or empty");
        }

        if (parameters == null) {
            throw new ApplicationException("Parameters array is NULL");
        }

        try {
            if (this.injectionCheckEnabled)
                SQLInjectionDetector.checkForUnsafeSQL(sql);

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            // Setting parameters if provided
            for (int n = 0; n < parameters.length; n++) {
                if (parameters[n] == null) {
                    preparedStatement.setNull(n + 1, java.sql.Types.NULL);
                } else {
                    preparedStatement.setObject(n + 1, parameters[n]);
                }
            }

            return preparedStatement;
        } catch (SQLException ex) {
            throw new ApplicationException("Error preparing SQL statement: " + ex.getMessage(), ex);
        }
    }

    /**
     * Execute a query and return the result set.
     *
     * @param statement The prepared statement to execute.
     * @return The result set.
     * @throws ApplicationException If an error occurs while executing the query.
     */
    public ResultSet executeQuery(PreparedStatement statement) throws ApplicationException {
        closeResultSet(); // Close previous result set if exists

        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            try {
                logger.log(Level.INFO, statement.toString());
                return resultSet = statement.executeQuery(); // Return execution result if successful
            } catch (SQLException e) {
                handleSQLException(e, statement);
            }

            // Retry delay before next attempt
            if (retry < MAX_RETRIES - 1) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new ApplicationException("Max retries exceeded for execute");
    }

    /**
     * Execute an update query and return the number of affected rows.
     *
     * @param statement The prepared statement to execute.
     * @return The number of affected rows.
     * @throws ApplicationException If an error occurs while executing the update.
     */
    public int executeUpdate(PreparedStatement statement) throws ApplicationException {
        try (statement) {  // Try-with-resources ensures statement is closed
            int effect = statement.executeUpdate();
            logger.log(Level.INFO, statement.toString());
            return effect;
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Execute a query without returning a result set.
     *
     * @param statement The prepared statement to execute.
     * @return True if the first result is a ResultSet object, false if it is an update count or there are no results.
     * @throws ApplicationException If an error occurs while executing the query.
     */
    public boolean execute(PreparedStatement statement) throws ApplicationException {
        try (statement) {
            return statement.execute();
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Create a PreparedStatement with the given SQL and scrollable option.
     *
     * @param sql        The SQL query.
     * @param scrollable True if the result set should be scrollable, false otherwise.
     * @return The prepared statement.
     * @throws ApplicationException If an error occurs while preparing the statement.
     */
    public PreparedStatement createPreparedStatement(String sql, boolean scrollable) throws ApplicationException {
        if (connection == null) {
            connection = manager.getConnection();
        }

        try {
            if (this.injectionCheckEnabled)
                SQLInjectionDetector.checkForUnsafeSQL(sql);

            int resultSetType = scrollable ? ResultSet.TYPE_SCROLL_INSENSITIVE : ResultSet.TYPE_FORWARD_ONLY;
            int resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;

            return connection.prepareStatement(sql,
                    resultSetType,
                    resultSetConcurrency);
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Execute a query and return the result set.
     *
     * @param sql The SQL query.
     * @return The result set.
     * @throws ApplicationException If an error occurs while executing the query.
     */
    public ResultSet query(String sql) throws ApplicationException {
        preparedStatement = createPreparedStatement(sql, false);
        return executeQuery(preparedStatement);
    }

    /**
     * Execute an update query and return the number of affected rows.
     *
     * @param sql The SQL query.
     * @return The number of affected rows.
     * @throws ApplicationException If an error occurs while executing the update.
     */
    public int update(String sql) throws ApplicationException {
        preparedStatement = createPreparedStatement(sql, false);
        return executeUpdate(preparedStatement);
    }

    /**
     * Execute a query without returning a result set.
     *
     * @param sql The SQL query.
     * @return True if the first result is a ResultSet object, false if it is an update count or there are no results.
     * @throws ApplicationException If an error occurs while executing the query.
     */
    public boolean execute(String sql) throws ApplicationException {
        preparedStatement = createPreparedStatement(sql, false);
        if (preparedStatement != null)
            return execute(preparedStatement);

        throw new ApplicationException("Prepared statement is null.");
    }

    /**
     * Get the current result set.
     *
     * @return The result set.
     */
    @Deprecated
    public ResultSet getResultSet() {
        return resultSet;
    }

    /**
     * Close the result set if it is not null.
     */
    void closeResultSet() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            logger.severe("ResultSet Close Error: " + e.getMessage());
        }
    }

    /**
     * Close the DatabaseOperator by closing the result set and prepared statement.
     */
    @Override
    public void close() {
        closeResultSet();
        try {
            if (preparedStatement != null) {
                preparedStatement.close();
            }

            // If there's an active transaction, roll it back before closing
            if (inTransaction && connection != null && !connection.isClosed()) {
                try {
                    connection.rollback();
                    connection.setAutoCommit(true);
                    logger.warning("Uncommitted transaction rolled back during close");
                } catch (SQLException ex) {
                    logger.severe("Error rolling back transaction during close: " + ex.getMessage());
                }
                inTransaction = false;
                currentSavepoint = null;
            }
        } catch (SQLException e) {
            throw new ApplicationRuntimeException(e.getMessage(), e);
        } finally {
            if (manager != null && connection != null) {
                manager.flush(connection);
            } else if (connection != null) {
                try {
                    connection.close();  // Close the connection if not managed by a ConnectionManager
                } catch (SQLException e) {
                    logger.warning("Error closing Connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handle SQLException, specifically handling communication link failure.
     *
     * @param e         The SQLException to handle.
     * @param statement The statement that using in the current operation.
     * @throws ApplicationException If an error occurs while handling the exception.
     */
    void handleSQLException(SQLException e, PreparedStatement statement) throws ApplicationException {
        if (e.getSQLState().equals(SQL_STATE_COMMUNICATION_LINK_FAILURE)) {
            closeResultSet(); // Close the current result set
            if (manager != null) {
                manager.clear();
            }
        } else {
            logger.severe("SQLState(" + e.getSQLState() + ") vendor code(" + e.getErrorCode() + "); Query:" + statement.toString() + " Message:" + e.getMessage());
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Disable safe check. e.g. CLI Mode
     */
    public void disableSafeCheck() {
        this.injectionCheckEnabled = false;
    }

    /**
     * Begin a database transaction. Sets the connection's auto-commit mode to false.
     * If a transaction is already in progress, this method will create a savepoint.
     *
     * @return A Savepoint object if a transaction was already in progress, null otherwise
     * @throws ApplicationException If an error occurs while beginning the transaction
     */
    public Savepoint beginTransaction() throws ApplicationException {
        try {
            if (connection == null) {
                connection = manager.getConnection();
            }

            if (inTransaction) {
                // If already in a transaction, create a savepoint
                currentSavepoint = connection.setSavepoint("SP_" + System.currentTimeMillis());
                logger.info("Created savepoint: " + currentSavepoint.getSavepointName());
                return currentSavepoint;
            } else {
                // Start a new transaction
                connection.setAutoCommit(false);
                inTransaction = true;
                logger.info("Transaction started");
                return null;
            }
        } catch (SQLException e) {
            throw new ApplicationException("Error beginning transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Commit the current transaction. Sets the connection's auto-commit mode back to true.
     *
     * @throws ApplicationException If an error occurs while committing the transaction
     */
    public void commitTransaction() throws ApplicationException {
        if (!inTransaction) {
            logger.warning("No active transaction to commit");
            return;
        }

        try {
            connection.commit();
            connection.setAutoCommit(true);
            inTransaction = false;
            currentSavepoint = null;
            logger.info("Transaction committed");
        } catch (SQLException e) {
            throw new ApplicationException("Error committing transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Rollback the current transaction. If a savepoint is specified, rolls back to that savepoint.
     * Otherwise, rolls back the entire transaction and sets auto-commit mode back to true.
     *
     * @param savepoint The savepoint to roll back to, or null to roll back the entire transaction
     * @throws ApplicationException If an error occurs while rolling back the transaction
     */
    public void rollbackTransaction(Savepoint savepoint) throws ApplicationException {
        if (!inTransaction) {
            logger.warning("No active transaction to rollback");
            return;
        }

        try {
            if (savepoint != null) {
                // Rollback to the specified savepoint
                connection.rollback(savepoint);
                logger.info("Transaction rolled back to savepoint: " + savepoint.getSavepointName());
            } else {
                // Rollback the entire transaction
                connection.rollback();
                connection.setAutoCommit(true);
                inTransaction = false;
                currentSavepoint = null;
                logger.info("Transaction rolled back completely");
            }
        } catch (SQLException e) {
            throw new ApplicationException("Error rolling back transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Rollback the entire current transaction.
     *
     * @throws ApplicationException If an error occurs while rolling back the transaction
     */
    public void rollbackTransaction() throws ApplicationException {
        rollbackTransaction(null);
    }

    /**
     * Check if a transaction is currently active.
     *
     * @return true if a transaction is active, false otherwise
     */
    public boolean isInTransaction() {
        return inTransaction;
    }

    /**
     * Get the current savepoint if one exists.
     *
     * @return The current savepoint, or null if none exists
     */
    public Savepoint getCurrentSavepoint() {
        return currentSavepoint;
    }

    /**
     * Create a new savepoint in the current transaction.
     *
     * @param name The name of the savepoint
     * @return The created savepoint
     * @throws ApplicationException If an error occurs while creating the savepoint or if no transaction is active
     */
    public Savepoint createSavepoint(String name) throws ApplicationException {
        if (!inTransaction) {
            throw new ApplicationException("Cannot create savepoint: No active transaction");
        }

        try {
            Savepoint savepoint = connection.setSavepoint(name);
            logger.info("Created savepoint: " + name);
            return savepoint;
        } catch (SQLException e) {
            throw new ApplicationException("Error creating savepoint: " + e.getMessage(), e);
        }
    }

    /**
     * Release a savepoint from the current transaction.
     *
     * @param savepoint The savepoint to release
     * @throws ApplicationException If an error occurs while releasing the savepoint
     */
    public void releaseSavepoint(Savepoint savepoint) throws ApplicationException {
        if (!inTransaction) {
            throw new ApplicationException("Cannot release savepoint: No active transaction");
        }

        try {
            connection.releaseSavepoint(savepoint);
            if (currentSavepoint != null && currentSavepoint.equals(savepoint)) {
                currentSavepoint = null;
            }
            logger.info("Released savepoint");
        } catch (SQLException e) {
            throw new ApplicationException("Error releasing savepoint: " + e.getMessage(), e);
        }
    }
}
