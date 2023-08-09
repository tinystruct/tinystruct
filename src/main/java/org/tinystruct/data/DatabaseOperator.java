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
    private final ConnectionManager manager;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private int effect = 0;

    public DatabaseOperator() throws ApplicationException {
        manager = ConnectionManager.getInstance();
        connection = manager.getConnection();
    }

    public DatabaseOperator(String database) throws ApplicationException {
        this();
        if (connection != null) {
            setCatalog(database);
        }
    }

    public DatabaseOperator(Connection connection) {
        manager = null;
        this.connection = connection;
    }

    public void setCatalog(String database) throws ApplicationException {
        try {
            connection.setCatalog(database);
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    public PreparedStatement preparedStatement(String sql, Object[] parameters)
            throws ApplicationException {
        if (sql == null) {
            throw new ApplicationException("SQL statement is NULL");
        }

        try {
            preparedStatement = connection.prepareStatement(sql);

            for (int n = 0; n < parameters.length; n++) {
                preparedStatement.setObject(n + 1, parameters[n]);
            }

            return preparedStatement;
        } catch (SQLException ex) {
            throw new ApplicationException(ex.getMessage(), ex);
        }
    }

    public ResultSet executeQuery(PreparedStatement statement) throws ApplicationException {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
        }

        try {
            resultSet = statement.executeQuery();
            statement.close();
            logger.log(Level.INFO, statement.toString());
        } catch (SQLException e) {
            handleSQLException(e);
        }

        return resultSet;
    }

    public int executeUpdate(PreparedStatement statement) throws ApplicationException {
        try {
            effect = statement.executeUpdate();
            statement.close();
            logger.log(Level.INFO, statement.toString());
            return effect;
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    public boolean execute(PreparedStatement statement) throws ApplicationException {
        try {
            boolean execute = statement.execute();
            statement.close();
            return execute;
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

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

    public ResultSet query(String sql) throws ApplicationException {
        preparedStatement = createPreparedStatement(sql, false);
        return executeQuery(preparedStatement);
    }

    public int update(String sql) throws ApplicationException {
        preparedStatement = createPreparedStatement(sql, false);
        return executeUpdate(preparedStatement);
    }

    public boolean execute(String sql) throws ApplicationException {
        preparedStatement = createPreparedStatement(sql, false);
        return execute(preparedStatement);
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    @Override
    public void close() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }

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

    private void handleSQLException(SQLException e) throws ApplicationException {
        if (e.getSQLState().equals(SQL_STATE_COMMUNICATION_LINK_FAILURE)) {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (SQLException ex) {
                logger.severe("ResultSet Close Error: " + ex.getMessage());
            }
            if (manager != null) {
                manager.clear();
            }
            PreparedStatement ps = createPreparedStatement(preparedStatement.toString(), false); // Re-prepare the statement
            executeQuery(ps);
        } else {
            logger.severe("SQLState(" + e.getSQLState() + ") vendor code(" + e.getErrorCode() + ")");
            throw new ApplicationException(e.getMessage(), e);
        }
    }
}
