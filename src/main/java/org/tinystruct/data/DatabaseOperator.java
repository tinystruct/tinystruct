package org.tinystruct.data;

import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseOperator implements Closeable {
    private static final Logger logger = Logger.getLogger(DatabaseOperator.class.getName());
    private static final String SQL_STATE_COMMUNICATION_LINK_FAILURE = "08S01";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private final ConnectionManager manager;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    /**
     * Default constructor to create a DatabaseOperator and obtain a connection from the ConnectionManager.
     *
     * @throws ApplicationException If an error occurs while obtaining a connection.
     */
    public DatabaseOperator() throws ApplicationException {
        manager = ConnectionManager.getInstance();
        connection = manager.getConnection();
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
        if (sql == null) {
            throw new ApplicationException("SQL statement is NULL");
        }

        try {
            preparedStatement = connection.prepareStatement(sql);
            if (parameters.length == 0) return preparedStatement;

            for (int n = 0; n < parameters.length; n++) {
                preparedStatement.setObject(n + 1, parameters[n]);
            }

            return preparedStatement;
        } catch (SQLException ex) {
            throw new ApplicationException(ex.getMessage(), ex);
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
                return statement.executeQuery(); // Return execution result if successful
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
        try {
            int effect = statement.executeUpdate();
            statement.close(); // Close the statement after execution
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
        try {
            boolean execute = statement.execute();
            statement.close(); // Close the statement after execution
            return execute;
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
            int resultSetType = scrollable ? ResultSet.TYPE_SCROLL_INSENSITIVE : ResultSet.TYPE_FORWARD_ONLY;
            int resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;

            return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
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
        return execute(preparedStatement);
    }

    /**
     * Get the current result set.
     *
     * @return The result set.
     */
    public ResultSet getResultSet() {
        return resultSet;
    }

    /**
     * Close the result set if it is not null.
     */
    private void closeResultSet() {
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
        } catch (SQLException e) {
            throw new ApplicationRuntimeException(e.getMessage(), e);
        } finally {
            if (manager != null && connection != null) {
                manager.flush(connection);
            }
        }
    }

    /**
     * Handle SQLException, specifically handling communication link failure.
     *
     * @param e         The SQLException to handle.
     * @param statement
     * @throws ApplicationException If an error occurs while handling the exception.
     */
    private void handleSQLException(SQLException e, PreparedStatement statement) throws ApplicationException {
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
}
