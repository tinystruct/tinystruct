/*******************************************************************************
 * Copyright  (c) 2013, 2023 James Mover Zhou
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
package org.tinystruct.data;

import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;

import java.io.Closeable;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseOperator implements Closeable {
    private static final Logger logger = Logger.getLogger(DatabaseOperator.class.getName());
    private static final String SQL_STATE_COMMUNICATION_LINK_FAILURE = "08S01";

    private Connection connection;
    private Statement statement;
    private PreparedStatement preparedstatement;
    private ResultSet resultSet;
    private int effect = 0;
    private final ConnectionManager manager;
    private String preparedSQL;
    private Object[] parameters;

    public DatabaseOperator() throws ApplicationException {
        this.manager = ConnectionManager.getInstance();
        this.connection = this.manager.getConnection();
    }

    public DatabaseOperator(String database) throws ApplicationException {
        this();
        if (this.connection != null)
            this.setCatalog(database);
    }

    public DatabaseOperator(Connection connection) {
        this.manager = null;
        this.connection = connection;
    }

    public void setCatalog(String Database) throws ApplicationException {
        try {
            this.connection.setCatalog(Database);
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    public PreparedStatement preparedStatement(String SQL, Object[] parameters)
            throws ApplicationException {
        if (SQL == null)
            throw new ApplicationException("preparedSQL is NULL");

        this.preparedSQL = SQL;
        this.parameters = parameters;
        try {
            this.preparedstatement = this.connection.prepareStatement(this.preparedSQL);

            int n = 0;
            while (n < this.parameters.length) {
                try {
                    this.preparedstatement.setObject(n + 1, parameters[n]);
                } catch (SQLException e) {
                    throw new ApplicationException(e.getMessage(), e);
                }

                n++;
            }

            return this.preparedstatement;
        } catch (SQLException ex) {
            throw new ApplicationException(ex.getMessage(), ex);
        }
    }

    public void query() throws ApplicationException {
        try {
            if (this.resultSet != null)
                this.resultSet.close();
            this.resultSet = this.preparedstatement.executeQuery();
            logger.log(Level.INFO, this.preparedSQL);
        } catch (SQLException e) {
            if (e.getSQLState().equals(SQL_STATE_COMMUNICATION_LINK_FAILURE)) {
                try {
                    if (this.resultSet != null)
                        this.resultSet.close();
                } catch (SQLException ex) {
                    logger.severe("ResultSet Close Error:"
                            + ex.getMessage());
                }
                if (this.manager != null) this.manager.clear();
                this.preparedStatement(this.preparedSQL, this.parameters);
                this.query();
            } else {
                logger.severe("SQLState(" + e.getSQLState()
                        + ") vendor code(" + e.getErrorCode() + ")");
                throw new ApplicationException(e.getMessage(), e);
            }

        }
    }

    public int update() throws ApplicationException {
        try {
            this.effect = this.preparedstatement.executeUpdate();
            logger.log(Level.INFO, this.preparedSQL);
            return this.effect;
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    public boolean execute() throws ApplicationException {
        try {
            return this.preparedstatement.execute();
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    public void createStatement(boolean scrollable) throws ApplicationException {
        if (this.connection == null)
            this.connection = this.manager.getConnection();

        try {
            if (!scrollable)
                this.statement = this.connection.createStatement();
            else
                this.statement = this.connection.createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    public void query(String SQL) throws ApplicationException {
        try {
            if (this.resultSet != null)
                this.resultSet.close();
            this.resultSet = this.statement.executeQuery(SQL);

            logger.log(Level.INFO, SQL);
        } catch (SQLException e) {
            if (e.getSQLState().equals(SQL_STATE_COMMUNICATION_LINK_FAILURE)) {
                try {
                    if (this.resultSet != null)
                        this.resultSet.close();
                } catch (SQLException e1) {
                    logger.severe("ResultSet Close Error:"
                            + e1.getMessage());
                }
                if (this.manager != null) this.manager.clear();
                this.createStatement(false);
                this.query(SQL);
            } else {
                logger.severe("SQLState(" + e.getSQLState()
                        + ") vendor code(" + e.getErrorCode() + ")");
                throw new ApplicationException(e.getMessage(), e);
            }
        }
    }

    public int update(String SQL) throws ApplicationException {
        try {
            this.effect = this.statement.executeUpdate(SQL);
            logger.log(Level.INFO, SQL);
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        return this.effect;
    }

    public ResultSet getResultSet() {
        return this.resultSet;
    }

    public boolean execute(String SQL) throws ApplicationException {
        try {
            boolean succeed = this.statement.execute(SQL);
            logger.log(Level.INFO, SQL);
            return succeed;
        } catch (SQLException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    @Override
	public void close() {
        try {
            if (this.resultSet != null) {
                this.resultSet.close();
                this.resultSet = null;
            }

            if (this.statement != null) {
                this.statement.close();
                this.statement = null;
            }

            if (this.preparedstatement != null) {
                this.preparedstatement.close();
                this.preparedstatement = null;
            }
        } catch (SQLException e) {
            throw new ApplicationRuntimeException(e.getMessage(), e);
        } finally {
            if (this.manager != null && this.connection != null) {
                this.manager.flush(this.connection);
            }
        }
    }

}