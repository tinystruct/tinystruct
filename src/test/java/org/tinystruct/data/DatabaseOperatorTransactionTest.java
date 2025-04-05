package org.tinystruct.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseOperatorTransactionTest {

    private Connection mockConnection;
    private DatabaseOperator operator;
    private Savepoint mockSavepoint;

    @BeforeEach
    void setUp() throws SQLException, ApplicationException {
        // Create mock objects
        mockConnection = mock(Connection.class);
        mockSavepoint = mock(Savepoint.class);

        // Set up mock behavior
        when(mockConnection.setSavepoint(anyString())).thenReturn(mockSavepoint);
        when(mockSavepoint.getSavepointName()).thenReturn("TEST_SAVEPOINT");

        // Create DatabaseOperator with mock connection
        operator = new DatabaseOperator(mockConnection);
    }

    @AfterEach
    void tearDown() {
        operator.close();
    }

    @Test
    void testBeginTransaction() throws ApplicationException, SQLException {
        // Begin transaction
        Savepoint result = operator.beginTransaction();

        // Verify connection.setAutoCommit(false) was called
        verify(mockConnection).setAutoCommit(false);

        // Verify transaction state
        assertTrue(operator.isInTransaction());
        assertNull(result); // First transaction returns null
    }

    @Test
    void testBeginNestedTransaction() throws ApplicationException, SQLException {
        // Begin first transaction
        operator.beginTransaction();

        // Begin nested transaction (should create savepoint)
        Savepoint result = operator.beginTransaction();

        // Verify savepoint was created
        assertNotNull(result);
        assertEquals(mockSavepoint, result);
        verify(mockConnection).setSavepoint(anyString());
    }

    @Test
    void testCommitTransaction() throws ApplicationException, SQLException {
        // Begin transaction
        operator.beginTransaction();

        // Commit transaction
        operator.commitTransaction();

        // Verify connection.commit() and setAutoCommit(true) were called
        verify(mockConnection).commit();
        verify(mockConnection).setAutoCommit(true);

        // Verify transaction state
        assertFalse(operator.isInTransaction());
    }

    @Test
    void testRollbackTransaction() throws ApplicationException, SQLException {
        // Begin transaction
        operator.beginTransaction();

        // Rollback transaction
        operator.rollbackTransaction();

        // Verify connection.rollback() and setAutoCommit(true) were called
        verify(mockConnection).rollback();
        verify(mockConnection).setAutoCommit(true);

        // Verify transaction state
        assertFalse(operator.isInTransaction());
    }

    @Test
    void testRollbackToSavepoint() throws ApplicationException, SQLException {
        // Begin transaction
        operator.beginTransaction();

        // Create savepoint
        Savepoint savepoint = operator.createSavepoint("TEST_POINT");

        // Rollback to savepoint
        operator.rollbackTransaction(savepoint);

        // Verify connection.rollback(savepoint) was called
        verify(mockConnection).rollback(savepoint);

        // Verify transaction is still active
        assertTrue(operator.isInTransaction());
    }

    @Test
    void testCreateAndReleaseSavepoint() throws ApplicationException, SQLException {
        // Begin transaction
        operator.beginTransaction();

        // Create savepoint
        Savepoint savepoint = operator.createSavepoint("TEST_POINT");
        assertNotNull(savepoint);

        // Release savepoint
        operator.releaseSavepoint(savepoint);

        // Verify connection.releaseSavepoint() was called
        verify(mockConnection).releaseSavepoint(savepoint);
    }

    @Test
    void testCreateSavepointWithoutTransaction() {
        // Try to create savepoint without active transaction
        assertThrows(ApplicationException.class, () -> operator.createSavepoint("TEST_POINT"));
    }

    @Test
    void testReleaseSavepointWithoutTransaction() {
        // Try to release savepoint without active transaction
        assertThrows(ApplicationException.class, () -> operator.releaseSavepoint(mockSavepoint));
    }

    @Test
    void testCloseWithActiveTransaction() throws SQLException, ApplicationException {
        // Begin transaction
        operator.beginTransaction();

        // Close operator
        operator.close();

        // Verify rollback was called
        verify(mockConnection).rollback();
        verify(mockConnection).setAutoCommit(true);
    }

    @Test
    void testTransactionExample() throws ApplicationException, SQLException {
        // Mock PreparedStatement and ResultSet
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        // Set up mock behavior
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);

        try {
            // Begin transaction
            operator.beginTransaction();

            // Execute first update
            PreparedStatement stmt1 = operator.preparedStatement("INSERT INTO users (name) VALUES (?)", new Object[]{"John"});
            operator.executeUpdate(stmt1);

            // Create savepoint
            Savepoint savepoint = operator.createSavepoint("AFTER_INSERT");

            // Execute second update
            PreparedStatement stmt2 = operator.preparedStatement("UPDATE users SET email = ? WHERE name = ?", new Object[]{"john@example.com", "John"});
            operator.executeUpdate(stmt2);

            // Simulate error condition and rollback to savepoint
            if (true) { // Simulating condition
                operator.rollbackTransaction(savepoint);

                // Verify rollback to savepoint was called
                verify(mockConnection).rollback(savepoint);
            }

            // Commit transaction
            operator.commitTransaction();

            // Verify commit was called
            verify(mockConnection).commit();
        } catch (ApplicationException e) {
            // Rollback on error
            operator.rollbackTransaction();
            throw e;
        }
    }
}
