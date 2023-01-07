package org.tinystruct.system.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;

class TextFileLoaderTest {
    @Test
    void testConstructor() {
        assertEquals("foo.txt", (new TextFileLoader("foo.txt")).getFilePath());
    }

    @Test
    void testGetContent() throws ApplicationException {
        assertThrows(ApplicationException.class, () -> (new TextFileLoader("foo.txt")).getContent());
        assertThrows(ApplicationException.class, () -> (new TextFileLoader()).getContent());
    }

    @Test
    void testGetContent2() throws UnsupportedEncodingException, ApplicationException {
        // TODO: This test is incomplete.
        //   Reason: No meaningful assertions found.
        //   Diffblue Cover was unable to create an assertion.
        //   Make sure that fields modified by getContent()
        //   have package-private, protected, or public getters.
        //   See https://diff.blue/R004 to resolve this issue.

        TextFileLoader textFileLoader = new TextFileLoader();
        textFileLoader.setInputStream(new ByteArrayInputStream("AAAAAAAAAAAAAAAAAAAAAAAA".getBytes(StandardCharsets.UTF_8)));
        textFileLoader.getContent();
    }

    @Test
    void testGetFilePath() {
        assertEquals("foo.txt", (new TextFileLoader("foo.txt")).getFilePath());
    }

    @Test
    void testGetFilePath2() throws UnsupportedEncodingException {
        TextFileLoader textFileLoader = new TextFileLoader("foo.txt");
        textFileLoader.setInputStream(new ByteArrayInputStream("AAAAAAAA".getBytes(StandardCharsets.UTF_8)));
        assertEquals("foo.txt", textFileLoader.getFilePath());
    }
}

