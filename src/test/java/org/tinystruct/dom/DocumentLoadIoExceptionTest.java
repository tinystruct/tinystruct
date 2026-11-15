package org.tinystruct.dom;

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentLoadIoExceptionTest {

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }

    @Test
    public void testLoadStringIoExceptionPreservesCause() throws Exception {
        File tempFile = File.createTempFile("test-doc", ".xml");
        tempFile.deleteOnExit();

        Document doc = new Document() {
            @Override
            public boolean load(java.io.InputStream input) {
                throw DocumentLoadIoExceptionTest.<RuntimeException>sneakyThrow(new IOException("Simulated disk read error"));
            }
        };

        ApplicationException ex = assertThrows(ApplicationException.class, () -> {
            doc.load(tempFile.getAbsolutePath());
        });

        assertNotNull(ex.getCause(), "ApplicationException cause should not be null");
        assertInstanceOf(IOException.class, ex.getCause(), "Cause should be an IOException");
        assertEquals("Simulated disk read error", ex.getCause().getMessage());
    }
}
