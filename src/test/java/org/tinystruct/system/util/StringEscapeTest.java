package org.tinystruct.system.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.*;

class StringUnescapeTest {

    // -------------------------------------------------------------------------
    // Null / empty / fast-path
    // -------------------------------------------------------------------------

    @Test
    void returnsNullForNull() {
        assertNull(unescape(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void returnsInputWhenNoEscapeCharPresent(String input) {
        // null handled above; empty string should return same object (fast path)
        String result = unescape(input);
        if (input == null) assertNull(result);
        else assertSame(input, result, "fast path should return the same String instance");
    }

    @Test
    void fastPathReturnsSameInstanceWhenNoBackslash() {
        String s = "hello world";
        assertSame(s, unescape(s));
    }

    // -------------------------------------------------------------------------
    // Basic single-character escapes
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "'\\\"', '\"'",
            "'\\\\ ', '\\ '",   // double-backslash -> single backslash (space avoids blank)
            "'\\/', '/'",
            "'\\b', '\b'",
            "'\\f', '\f'",
            "'\\n', '\n'",
            "'\\r', '\r'",
            "'\\t', '\t'",
    })
    void basicEscapes(String input, String expected) {
        assertEquals(expected, unescape(input));
    }

    @Test
    void unicodeLowercase() {
        assertEquals("A", unescape("\\u0041"));
    }

    @Test
    void unicodeUppercaseHexDigits() {
        assertEquals("A", unescape("\\u0041"));
        assertEquals("\u00e9", unescape("\\u00E9")); // é
    }

    @Test
    void unicodeNullCharacter() {
        assertEquals("\u0000", unescape("\\u0000"));
    }

    @Test
    void unicodeLastBmpCodepoint() {
        assertEquals("\uFFFF", unescape("\\uFFFF"));
    }

    @Test
    void unicodeEmbeddedInText() {
        assertEquals("caf\u00e9", unescape("caf\\u00E9"));
    }

    // -------------------------------------------------------------------------
    // Unicode — surrogate pairs (> U+FFFF)
    // -------------------------------------------------------------------------

    @Test
    void surrogatePairGrinningFace() {
        // U+1F600 GRINNING FACE = \uD83D\uDE00
        String expected = "\uD83D\uDE00";
        assertEquals(expected, unescape("\\uD83D\\uDE00"));
        assertEquals(1, expected.codePointCount(0, expected.length()));
    }

    @Test
    void surrogatePairMidString() {
        assertEquals("hi \uD83D\uDE00!", unescape("hi \\uD83D\\uDE00!"));
    }

    @Test
    void highSurrogateWithoutLowSurrogatePreservedAsChar() {
        // \uD83D not followed by a low surrogate — keep the high surrogate char
        String result = unescape("\\uD83D\\u0041"); // high + 'A'
        assertEquals("\uD83DA", result);
    }

    @Test
    void lowSurrogateAlonePreservedAsChar() {
        assertEquals("\uDE00", unescape("\\uDE00"));
    }

    @Test
    void unicodeWithInvalidHexDigitPreservesLiterally() {
        String result = unescape("\\uGHIJ");
        assertEquals("\\uGHIJ", result);
    }

    @Test
    void truncatedUnicodeAtEndOfString() {
        String result = unescape("\\u004");
        assertEquals("\\u004", result);
    }

    @Test
    void unicodeEscapeExactlyAtEndOfString() {
        assertEquals("A", unescape("\\u0041"));
    }

    // -------------------------------------------------------------------------
    // Unrecognised escape sequences
    // -------------------------------------------------------------------------

    @Test
    void unknownEscapePreservedLiterally() {
        assertEquals("\\q", unescape("\\q"));
    }

    @Test
    void trailingBackslashPreservedLiterally() {
        // lone \ at end of string — guard prevents out-of-bounds, appends as-is
        assertEquals("abc\\", unescape("abc\\"));
    }

    // -------------------------------------------------------------------------
    // Compound / real-world inputs
    // -------------------------------------------------------------------------

    @Test
    void multipleEscapesInOnString() {
        assertEquals("line1\nline2\ttabbed", unescape("line1\\nline2\\ttabbed"));
    }

    @Test
    void jsonLikeStringValue() {
        assertEquals("She said \"hello\"", unescape("She said \\\"hello\\\""));
    }

    @Test
    void windowsPathRoundtrip() {
        assertEquals("C:\\Users\\name", unescape("C:\\\\Users\\\\name"));
    }

    @Test
    void mixedUnicodeAndBasicEscapes() {
        assertEquals("caf\u00e9\nnew line", unescape("caf\\u00E9\\nnew line"));
    }

    @Test
    void noEscapesPassesThroughUnchanged() {
        String s = "plain text 123 !@#";
        assertSame(s, unescape(s));
    }

    // -------------------------------------------------------------------------
    // Helper — wire up to your actual class
    // -------------------------------------------------------------------------

    private static String unescape(String s) {
        return StringUtilities.unescape(s); // replace with your actual class name
    }
}
