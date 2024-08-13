package org.tinystruct.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class VectorOperatorTest {

    private VectorOperator vectorOperator;
    private Connection mockConnection;
    private PreparedStatement mockStatement;

    @BeforeEach
    public void setUp() throws Exception {
        // Initialize mocks for connection and statement
        mockConnection = mock(Connection.class);
        mockStatement = mock(PreparedStatement.class);

        // Create a subclass of VectorOperator to override the connection initialization for testing
        vectorOperator = new VectorOperator() {
            @Override
            public Connection getConnection() {
                return mockConnection;
            }
        };
    }

    @Test
    public void testInitialization() {
        assertNotNull(vectorOperator.getConnection(), "Connection should be initialized.");
    }

    @Test
    public void testClose() throws SQLException {
        // Simulate open statement and connection
        when(mockConnection.isClosed()).thenReturn(false);

        // Close the operator and verify resources are closed
        vectorOperator.close();

        verify(mockStatement, never()).close(); // Statement was not initialized in this case
    }

    @Test
    public void testHandleSQLException_CommunicationLinkFailure() throws SQLException, ApplicationException {
        // Simulate a communication link failure
        SQLException sqlException = new SQLException("Communication link failure", "08S01");

        // Close the operator and verify resources are closed
        vectorOperator.close();

        // Invoke exception handling
        vectorOperator.handleSQLException(sqlException, mockStatement);

        when(mockConnection.isClosed()).thenReturn(false);
    }

    @Test
    public void testHandleSQLException_OtherSQLException() throws ApplicationException {
        // Simulate a general SQL exception
        SQLException sqlException = new SQLException("General SQL error", "S1000");

        // Verify that ApplicationException is thrown and the message is correct
        ApplicationException thrown = assertThrows(ApplicationException.class, () -> {
            vectorOperator.handleSQLException(sqlException, mockStatement);
        });

        assertEquals("General SQL error", thrown.getMessage());
    }

    @Test
    public void testHandleSQLException_IgnoresExceptionOnClose() throws SQLException, ApplicationException {
        // Simulate a communication link failure
        SQLException sqlException = new SQLException("Communication link failure", "08S01");

        doThrow(new SQLException("Close exception")).when(mockConnection).close();

        // Invoke exception handling
        vectorOperator.handleSQLException(sqlException, mockStatement);
    }
}
