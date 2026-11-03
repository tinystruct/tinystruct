package org.tinystruct.data.component;

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JSON round-trip fidelity of {@link Builder} and {@link Builders}:
 * <ul>
 *     <li>object keys must be escaped on output, since the parser unescapes them;</li>
 *     <li>literals in arrays ({@code true}, {@code false}, {@code null}) must keep their
 *     type, and strings that merely look like literals must stay quoted.</li>
 * </ul>
 */
class BuilderRoundTripTest {

    // ---------------------------------------------------------------------
    // Object keys
    // ---------------------------------------------------------------------

    @Test
    void keyWithQuoteIsEscapedInOutput() {
        Builder builder = new Builder().put("say \"hi\"", "v");

        assertEquals("{\"say \\\"hi\\\"\":\"v\"}", builder.toString());
    }

    @Test
    void keyWithBackslashIsEscapedInOutput() {
        Builder builder = new Builder().put("C:\\dir", "v");

        assertEquals("{\"C:\\\\dir\":\"v\"}", builder.toString());
    }

    @Test
    void keyWithControlCharactersIsEscapedInOutput() {
        Builder builder = new Builder().put("a\nb\tc", "v");

        assertEquals("{\"a\\nb\\tc\":\"v\"}", builder.toString());
    }

    @Test
    void keysWithSpecialCharactersSurviveRoundTrip() throws ApplicationException {
        String[] keys = {
                "say \"hi\"",
                "C:\\dir",
                "C:\\dir\\",
                "a\nb\tc",
                "quote\" and backslash\\",
        };

        for (String key : keys) {
            Builder original = new Builder().put(key, "v");

            Builder parsed = new Builder();
            parsed.parse(original.toString());

            assertEquals(1, parsed.size(), "Unexpected key count for key: " + key);
            assertTrue(parsed.containsKey(key), "Key lost in round trip: " + key);
            assertEquals("v", parsed.get(key));
        }
    }

    @Test
    void keyWithSpecialCharactersInNestedObjectSurvivesRoundTrip() throws ApplicationException {
        Builder inner = new Builder().put("say \"hi\"", 1);
        Builder original = new Builder().put("outer", inner);

        Builder parsed = new Builder();
        parsed.parse(original.toString());

        Builder parsedInner = (Builder) parsed.get("outer");
        assertTrue(parsedInner.containsKey("say \"hi\""));
        assertEquals(1, parsedInner.get("say \"hi\""));
    }

    // ---------------------------------------------------------------------
    // Literals in arrays
    // ---------------------------------------------------------------------

    @Test
    void booleanLiteralsInArrayParseAsBooleans() throws ApplicationException {
        Builders builders = new Builders();
        builders.parse("[true,false]");

        assertEquals(2, builders.size());
        assertEquals(Boolean.TRUE, builders.get(0).getValue());
        assertEquals(Boolean.FALSE, builders.get(1).getValue());
    }

    @Test
    void nullLiteralInArrayParsesAsNull() throws ApplicationException {
        Builders builders = new Builders();
        builders.parse("[null]");

        assertEquals(1, builders.size());
        assertNull(builders.get(0).getValue());
    }

    @Test
    void stringsThatLookLikeLiteralsKeepTheirQuotesInArray() throws ApplicationException {
        String json = "[\"true\",\"false\",\"null\",\"123\",\"1.5\"]";

        Builders builders = new Builders();
        builders.parse(json);

        assertEquals(json, builders.toString());
    }

    @Test
    void mixedLiteralsAndLookalikeStringsRoundTripUnchanged() throws ApplicationException {
        String json = "[true,false,null,1,\"true\",\"1\"]";

        Builders builders = new Builders();
        builders.parse(json);

        assertEquals(json, builders.toString());
    }

    @Test
    void stringsThatLookLikeLiteralsKeepTheirQuotesInObject() throws ApplicationException {
        // Control case: object values do not have this problem.
        String json = "{\"a\":\"true\",\"b\":\"123\",\"c\":\"null\"}";

        Builder builder = new Builder();
        builder.parse(json);

        assertEquals("true", builder.get("a"));
        assertEquals("123", builder.get("b"));
        assertEquals("null", builder.get("c"));

        Builder reparsed = new Builder();
        reparsed.parse(builder.toString());
        assertEquals("true", reparsed.get("a"));
        assertEquals("123", reparsed.get("b"));
        assertEquals("null", reparsed.get("c"));
    }
}
