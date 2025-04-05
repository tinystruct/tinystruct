package org.tinystruct.data.examples;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.DatabaseOperator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;

/**
 * Example class demonstrating how to use database transactions with DatabaseOperator.
 */
public class TransactionExample {

    /**
     * Example of a simple transaction that transfers money between two accounts.
     *
     * @param fromAccountId The account to transfer from
     * @param toAccountId   The account to transfer to
     * @param amount        The amount to transfer
     * @return true if the transfer was successful, false otherwise
     * @throws ApplicationException If an error occurs during the transaction
     */
    public boolean transferMoney(int fromAccountId, int toAccountId, double amount) throws ApplicationException {
        try (DatabaseOperator operator = new DatabaseOperator()) {
            // Begin transaction
            operator.beginTransaction();
            
            try {
                // Check if the source account has enough balance
                PreparedStatement checkBalanceStmt = operator.preparedStatement(
                        "SELECT balance FROM accounts WHERE id = ?",
                        new Object[]{fromAccountId}
                );
                ResultSet rs = operator.executeQuery(checkBalanceStmt);
                
                if (!rs.next() || rs.getDouble("balance") < amount) {
                    // Insufficient funds or account not found
                    operator.rollbackTransaction();
                    return false;
                }
                
                // Withdraw from source account
                PreparedStatement withdrawStmt = operator.preparedStatement(
                        "UPDATE accounts SET balance = balance - ? WHERE id = ?",
                        new Object[]{amount, fromAccountId}
                );
                operator.executeUpdate(withdrawStmt);
                
                // Deposit to destination account
                PreparedStatement depositStmt = operator.preparedStatement(
                        "UPDATE accounts SET balance = balance + ? WHERE id = ?",
                        new Object[]{amount, toAccountId}
                );
                operator.executeUpdate(depositStmt);
                
                // Commit the transaction
                operator.commitTransaction();
                return true;
                
            } catch (Exception e) {
                // Rollback on any error
                operator.rollbackTransaction();
                throw new ApplicationException("Transaction failed: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new ApplicationException("Error in money transfer: " + e.getMessage(), e);
        }
    }

    /**
     * Example of a transaction with savepoints.
     *
     * @param userId The user ID
     * @return true if the operation was successful, false otherwise
     * @throws ApplicationException If an error occurs during the transaction
     */
    public boolean complexOperation(int userId) throws ApplicationException {
        try (DatabaseOperator operator = new DatabaseOperator()) {
            // Begin transaction
            operator.beginTransaction();
            
            try {
                // First operation - update user status
                PreparedStatement updateStatusStmt = operator.preparedStatement(
                        "UPDATE users SET status = 'active' WHERE id = ?",
                        new Object[]{userId}
                );
                int statusUpdated = operator.executeUpdate(updateStatusStmt);
                
                if (statusUpdated == 0) {
                    // User not found
                    operator.rollbackTransaction();
                    return false;
                }
                
                // Create savepoint after status update
                Savepoint afterStatusUpdate = operator.createSavepoint("AFTER_STATUS_UPDATE");
                
                // Second operation - create user preferences
                PreparedStatement createPrefsStmt = operator.preparedStatement(
                        "INSERT INTO user_preferences (user_id, theme, notifications) VALUES (?, 'default', true)",
                        new Object[]{userId}
                );
                
                try {
                    operator.executeUpdate(createPrefsStmt);
                } catch (ApplicationException e) {
                    // If preferences creation fails, rollback to savepoint
                    // This keeps the status update but discards the preferences operation
                    operator.rollbackTransaction(afterStatusUpdate);
                    
                    // Try alternative approach for preferences
                    PreparedStatement updatePrefsStmt = operator.preparedStatement(
                            "UPDATE user_preferences SET theme = 'default', notifications = true WHERE user_id = ?",
                            new Object[]{userId}
                    );
                    operator.executeUpdate(updatePrefsStmt);
                }
                
                // Third operation - log the activity
                PreparedStatement logActivityStmt = operator.preparedStatement(
                        "INSERT INTO activity_log (user_id, action, timestamp) VALUES (?, 'account_setup', NOW())",
                        new Object[]{userId}
                );
                operator.executeUpdate(logActivityStmt);
                
                // Commit the transaction
                operator.commitTransaction();
                return true;
                
            } catch (Exception e) {
                // Rollback on any error
                operator.rollbackTransaction();
                throw new ApplicationException("Complex operation failed: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new ApplicationException("Error in complex operation: " + e.getMessage(), e);
        }
    }
}
