# Database Transactions in Tinystruct

This document explains how to use database transactions in the Tinystruct framework.

## Overview

Database transactions allow you to execute a series of operations as a single unit of work. If any operation fails, all operations can be rolled back, ensuring data integrity. The `DatabaseOperator` class in Tinystruct now provides comprehensive transaction support.

## Basic Transaction Usage

```java
try (DatabaseOperator operator = new DatabaseOperator()) {
    // Begin transaction
    operator.beginTransaction();
    
    try {
        // Execute database operations
        PreparedStatement stmt1 = operator.preparedStatement("INSERT INTO users (name) VALUES (?)", new Object[]{"John"});
        operator.executeUpdate(stmt1);
        
        PreparedStatement stmt2 = operator.preparedStatement("UPDATE settings SET value = ? WHERE name = ?", new Object[]{"new_value", "setting_name"});
        operator.executeUpdate(stmt2);
        
        // Commit transaction if all operations succeed
        operator.commitTransaction();
        
    } catch (Exception e) {
        // Rollback transaction if any operation fails
        operator.rollbackTransaction();
        throw e;
    }
}
```

## Transaction Methods

The `DatabaseOperator` class provides the following transaction-related methods:

### `beginTransaction()`

Begins a new transaction by setting the connection's auto-commit mode to false.

```java
Savepoint savepoint = operator.beginTransaction();
```

If a transaction is already in progress, this method creates a savepoint and returns it.

### `commitTransaction()`

Commits the current transaction and sets the connection's auto-commit mode back to true.

```java
operator.commitTransaction();
```

### `rollbackTransaction()`

Rolls back the entire transaction and sets the connection's auto-commit mode back to true.

```java
operator.rollbackTransaction();
```

### `rollbackTransaction(Savepoint savepoint)`

Rolls back the transaction to the specified savepoint. The transaction remains active.

```java
operator.rollbackTransaction(savepoint);
```

### `createSavepoint(String name)`

Creates a named savepoint within the current transaction.

```java
Savepoint savepoint = operator.createSavepoint("AFTER_INSERT");
```

### `releaseSavepoint(Savepoint savepoint)`

Releases a savepoint from the current transaction.

```java
operator.releaseSavepoint(savepoint);
```

### `isInTransaction()`

Checks if a transaction is currently active.

```java
boolean active = operator.isInTransaction();
```

## Savepoints

Savepoints allow you to create points within a transaction that you can roll back to without rolling back the entire transaction.

```java
// Begin transaction
operator.beginTransaction();

// Execute first operation
PreparedStatement stmt1 = operator.preparedStatement("INSERT INTO users (name) VALUES (?)", new Object[]{"John"});
operator.executeUpdate(stmt1);

// Create savepoint after first operation
Savepoint savepoint = operator.createSavepoint("AFTER_INSERT");

try {
    // Execute second operation
    PreparedStatement stmt2 = operator.preparedStatement("UPDATE settings SET value = ? WHERE name = ?", new Object[]{"new_value", "setting_name"});
    operator.executeUpdate(stmt2);
} catch (Exception e) {
    // If second operation fails, roll back to savepoint
    operator.rollbackTransaction(savepoint);
    
    // Try alternative operation
    PreparedStatement altStmt = operator.preparedStatement("INSERT INTO logs (message) VALUES (?)", new Object[]{"Operation failed"});
    operator.executeUpdate(altStmt);
}

// Commit transaction
operator.commitTransaction();
```

## Auto-Rollback on Close

If a `DatabaseOperator` with an active transaction is closed without explicitly committing or rolling back the transaction, the transaction will be automatically rolled back to ensure data integrity.

## Best Practices

1. Always use try-with-resources to ensure proper closure of the `DatabaseOperator`
2. Wrap transaction operations in a try-catch block
3. Always commit or rollback transactions explicitly
4. Use savepoints for complex operations where partial rollbacks might be needed
5. Keep transactions as short as possible to avoid locking resources for extended periods
6. Handle exceptions appropriately, ensuring transactions are rolled back on errors

## Example Use Cases

See the `TransactionExample` class for complete examples of:
- Simple money transfer between accounts
- Complex operations with savepoints and partial rollbacks
