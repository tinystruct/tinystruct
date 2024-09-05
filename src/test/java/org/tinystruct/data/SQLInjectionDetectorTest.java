package org.tinystruct.data;

import org.junit.jupiter.api.Test;
import org.tinystruct.data.tools.SQLInjectionDetector;
import org.tinystruct.data.tools.SQLInjectionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SQLInjectionDetectorTest {

    @Test
    public void testUnsafeSQLWithInjectionPatterns() {
        String unsafeSql1 = "SELECT * FROM users WHERE username = 'admin' OR '1'='1'";
        String unsafeSql2 = "SELECT * FROM users WHERE username = 'admin' -- comment";
        String unsafeSql3 = "SELECT email FROM users WHERE username = 'admin'; DROP TABLE users;";

        // Test for exception throwing
        assertThrows(SQLInjectionException.class, () -> SQLInjectionDetector.checkForUnsafeSQL(unsafeSql1));
        assertThrows(SQLInjectionException.class, () -> SQLInjectionDetector.checkForUnsafeSQL(unsafeSql2));
        assertThrows(SQLInjectionException.class, () -> SQLInjectionDetector.checkForUnsafeSQL(unsafeSql3));
    }

    @Test
    public void testSafeSQL() {
        String safeSql1 = "SELECT email FROM users WHERE username = ?";
        String safeSql2 = "INSERT INTO users (username, email) VALUES (?, ?)";

        // No exception should be thrown for safe SQL
        assertDoesNotThrow(() -> SQLInjectionDetector.checkForUnsafeSQL(safeSql1));
        assertDoesNotThrow(() -> SQLInjectionDetector.checkForUnsafeSQL(safeSql2));
    }
}