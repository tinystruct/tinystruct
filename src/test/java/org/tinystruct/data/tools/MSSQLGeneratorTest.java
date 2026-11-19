package org.tinystruct.data.tools;

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Table;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class MSSQLGeneratorTest {

    @Test
    public void testIOExceptionCauseHandling() throws Exception {
        MSSQLGenerator generator = new MSSQLGenerator() {
            @Override
            public Table find(String SQL) throws ApplicationException {
                return new Table();
            }
        };

        // Create a file at target/blockerFile so that trying to create a directory
        // at target/blockerFile/child will result in an IOException (FileAlreadyExistsException).
        Path blocker = Paths.get("target/blockerFile");
        if (Files.notExists(blocker)) {
            Files.createFile(blocker);
        }
        generator.setPath(blocker + "/child");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> {
            generator.create("TestClass", "TestTable");
        });

        assertNotNull(ex.getCause(), "ApplicationException cause should not be null when IOException occurs");
        assertTrue(ex.getCause() instanceof IOException, "ApplicationException cause should be an instance of IOException");
    }
}

