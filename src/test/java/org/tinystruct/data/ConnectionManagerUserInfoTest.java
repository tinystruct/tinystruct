package org.tinystruct.data;

import org.junit.jupiter.api.Test;
import org.tinystruct.system.Settings;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ConnectionManagerUserInfoTest {

    @Test
    public void testLoadDatabaseConfigWithUsernameOnlyUserInfo() throws Exception {
        ConnectionManager manager = ConnectionManager.getInstance();

        Field driverNameField = ConnectionManager.class.getDeclaredField("driverName");
        driverNameField.setAccessible(true);
        String originalDriver = (String) driverNameField.get(manager);

        Method loadConfigMethod = ConnectionManager.class.getDeclaredMethod("loadDatabaseConfig", org.tinystruct.system.Configuration.class);
        loadConfigMethod.setAccessible(true);

        try {
            driverNameField.set(manager, "org.postgresql.Driver");

            Settings customSettings = new Settings() {
                @Override
                public String get(String key) {
                    if ("database.url".equals(key)) {
                        return "postgresql://user@localhost:5432/testdb";
                    }
                    return "";
                }
            };

            assertDoesNotThrow(() -> {
                try {
                    loadConfigMethod.invoke(manager, customSettings);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                    throw new RuntimeException(cause);
                }
            });
        } finally {
            driverNameField.set(manager, originalDriver);
            loadConfigMethod.invoke(manager, new Settings());
        }
    }
}
