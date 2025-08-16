package org.tinystruct.data.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildersTest {

    private Builders builders;

    @BeforeEach
    void setUp() {
        builders = new Builders();
    }

    @Test
    void testToStringEmpty() {
        assertEquals("[]", builders.toString());
    }

    @Test
    void testToStringWithElements() throws ApplicationException {
        builders.parse("[\"A\", \"B\", \"C\"]");
        assertEquals("[\"A\",\"B\",\"C\"]", builders.toString());
    }

    @Test
    void testParseSimpleArray() throws ApplicationException {
        String input = "[\"A\", \"B\", \"C\"]";
        builders.parse(input);

        assertEquals(3, builders.size());
        assertEquals("A", builders.get(0).getValue());
        assertEquals("B", builders.get(1).getValue());
        assertEquals("C", builders.get(2).getValue());
    }

    @Test
    void testParseArrayWithNumbers() throws ApplicationException {
        String input = "[1, 2, 3]";
        builders.parse(input);

        assertEquals(3, builders.size());
        assertEquals(1, builders.get(0).getValue());
        assertEquals(2, builders.get(1).getValue());
        assertEquals(3, builders.get(2).getValue());
    }

    @Test
    void testParseArrayWithNestedObjects() throws ApplicationException {
        String input = "[{\"key\":\"value\"}, {\"another\":\"object\"}]";
        builders.parse(input);

        assertEquals(2, builders.size());
        assertEquals("value", ((Builder)builders.get(0)).get("key"));
        assertEquals("object", ((Builder)builders.get(1)).get("another"));
    }


    @Test
    void testParseArrayWithMixedValues() throws ApplicationException {
        String input = "[\"string\", 123, {\"key\":\"value\"}, [1, 2]]";
        builders.parse(input);

        assertEquals(4, builders.size());
        assertEquals("string", builders.get(0).getValue());
        assertEquals(123, builders.get(1).getValue());
        assertTrue(builders.get(2).toString().contains("key"));
        assertTrue(builders.get(2).toString().contains("value"));
        assertTrue(builders.get(3) instanceof Builder);

        Builder nestedArray = (Builder) builders.get(3);
        assertEquals(2, nestedArray.size());

        assertEquals(1, nestedArray.get("0"));
        assertEquals(2, nestedArray.get("1"));
    }

    @Test
    void testParseEmptyArray() throws ApplicationException {
        String input = "[]";
        builders.parse(input);

        assertEquals(0, builders.size());
    }

/*    @Test
    void testParseInvalidInputThrowsException() {
        String input = "[\"A\", , \"B\"]"; // Invalid syntax
        assertThrows(ApplicationException.class, () -> builders.parse(input));
    }*/

    @Test
    void testParseArrayWithSpecialCharacters() throws ApplicationException {
        String input = "[\"A, B\", \"C: D\"]";
        builders.parse(input);

        assertEquals(2, builders.size());
        assertEquals("A, B", builders.get(0).getValue());
        assertEquals("C: D", builders.get(1).getValue());
    }

/*    @Test
    void testParseComplexNestedStructure() throws ApplicationException {
        String input = "[{\"key\":\"value\"}, [\"nested\", {\"inner\":\"object\"}]]";
        builders.parse(input);

        assertEquals(2, builders.size());

        // First element: Object
        assertTrue(builders.get(0).toString().contains("key"));
        assertTrue(builders.get(0).toString().contains("value"));

        // Second element: Nested array
        assertTrue(builders.get(1) instanceof Builder);
        Builder nestedArray = (Builder) builders.get(1);
        assertEquals(2, nestedArray.size());
        assertEquals("nested", ((Builder)nestedArray.get(0)).getValue());

        // Second element inside nested array: Object
        Builder innerObject = (Builder) nestedArray.get(1);
        assertTrue(innerObject.toString().contains("inner"));
        assertTrue(innerObject.toString().contains("object"));
    }*/
}
