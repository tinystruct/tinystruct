package org.tinystruct.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.tinystruct.ApplicationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.mockito.Mockito.*;

class DatabaseOperatorTest {

    @Mock
    Connection connection;
    @Mock
    PreparedStatement preparedStatement;
    @Mock
    ResultSet resultSet;
    @Mock
    ConnectionManager connectionManager;
    @InjectMocks
    DatabaseOperator databaseOperator;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(connectionManager.getConnection()).thenReturn(connection);
        // You might need to adjust this depending on how you initialize your DatabaseOperator
        databaseOperator = new DatabaseOperator(connection);
    }

    @Test
    void testSetCatalog() throws SQLException, ApplicationException {
        doNothing().when(connection).setCatalog(anyString());

        databaseOperator.setCatalog("database");

        verify(connection).setCatalog("database");
    }

    @Test
    void testPreparedStatement() throws SQLException, ApplicationException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        PreparedStatement result = databaseOperator.preparedStatement("SELECT * FROM list where id=?", new Object[]{"1"});

        verify(preparedStatement).setObject(1, "1");
        verify(preparedStatement, times(1)).setObject(1, "1");
        Assertions.assertNotNull(result);
    }

    @Test
    void testExecuteQuery() throws SQLException, ApplicationException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        ResultSet result = databaseOperator.executeQuery(preparedStatement);

        verify(preparedStatement).executeQuery();
        verify(resultSet, never()).close();
        Assertions.assertNotNull(result);
    }

    @Test
    void testExecuteUpdate() throws SQLException, ApplicationException {
        when(preparedStatement.executeUpdate()).thenReturn(1);

        int result = databaseOperator.executeUpdate(preparedStatement);

        verify(preparedStatement).close();
        Assertions.assertEquals(1, result);
    }

    @Test
    void testExecute() throws SQLException, ApplicationException {
        when(preparedStatement.execute()).thenReturn(true);

        boolean result = databaseOperator.execute(preparedStatement);

        verify(preparedStatement).close();
        Assertions.assertTrue(result);
    }

    @Test
    void testCreatePreparedStatement() throws SQLException, ApplicationException {
        when(connection.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(preparedStatement);

        PreparedStatement result = databaseOperator.createPreparedStatement("sql", true);

        Assertions.assertNotNull(result);
    }

    @Test
    void testQuery() throws SQLException, ApplicationException {
        when(connection.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        ResultSet result = databaseOperator.query("SELECT 1");

        verify(preparedStatement).executeQuery();
        verify(resultSet, never()).close();
        Assertions.assertNotNull(result);
    }

    @Test
    void testUpdate() throws SQLException, ApplicationException {
        when(connection.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        int result = databaseOperator.update("UPDATE list SET name='OK'");

        verify(preparedStatement).close();
        Assertions.assertEquals(1, result);
    }

    @Test
    void testExecuteWithSQL() throws SQLException, ApplicationException {
        when(connection.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);

        boolean result = databaseOperator.execute("UPDATE list SET name='OK'");

        verify(preparedStatement).close();
        Assertions.assertTrue(result);
    }

    @Test
    void testClose() throws SQLException {
        doNothing().when(preparedStatement).close();
        doNothing().when(resultSet).close();

        databaseOperator.close();

        verify(resultSet).close();
        verify(preparedStatement).close();
    }
}
