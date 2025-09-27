package org.tinystruct.data.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

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
        assertNotNull(builders.get(3));

        Builder nestedArray = builders.get(3);
        assertEquals(2, nestedArray.size());
        assertEquals(1, nestedArray.get(0));
        assertEquals(2, nestedArray.get(1));
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

    // ========== Enhanced toString() Tests ==========

    @Test
    void testToStringWithStringEscaping() throws ApplicationException {
        builders.parse("[\"He said \\\"Hello World\\\"\", \"Path: C:\\\\Users\\\\Test\", \"Line1\\nLine2\\rLine3\"]");
        
        String result = builders.toString();
        System.out.println("result = " + result);
        assertTrue(result.contains("\"He said \\\\\\\"Hello World\\\\\\\""));
        assertTrue(result.contains("\"Path: C:\\\\\\\\Users\\\\\\\\Test\""));
        assertTrue(result.contains("\"Line1\\\\nLine2\\\\rLine3\""));
    }

    @Test
    void testToStringWithSpecialCharacters() throws ApplicationException {
        builders.parse("[\"Hello 世界\", \"Special chars: !@#$%^&*()\", \"\"]");
        
        String result = builders.toString();
        // Chinese characters will be converted to Unicode escape sequences
        assertTrue(result.contains("\"Hello \\u4E16\\u754C\""));
        assertTrue(result.contains("\"Special chars: !@#$%^&*()\""));
        assertTrue(result.contains("\"\""));
    }

    @Test
    void testToStringWithNullValues() throws ApplicationException {
        builders.parse("[null, \"test\", null]");
        
        String result = builders.toString();
        assertTrue(result.contains("null"));
        assertTrue(result.contains("\"test\""));
    }

    @Test
    void testToStringWithNestedObjects() throws ApplicationException {
        builders.parse("[{\"key\":\"value\"}, {\"nested\":{\"inner\":\"data\"}}]");
        
        String result = builders.toString();
        assertTrue(result.contains("{\"key\":\"value\"}"));
        assertTrue(result.contains("{\"nested\":{\"inner\":\"data\"}}"));
    }

    @Test
    void testToStringWithNestedArrays() throws ApplicationException {
        builders.parse("[[1,2,3], [\"a\",\"b\",\"c\"]]");
        
        String result = builders.toString();
        assertTrue(result.contains("[1,2,3]"));
        assertTrue(result.contains("[\"a\",\"b\",\"c\"]"));
    }

    @Test
    void testToStringWithMixedTypes() throws ApplicationException {
        builders.parse("[\"string\", 123, true, false, null, {\"key\":\"value\"}, [1,2,3]]");
        
        String result = builders.toString();
        assertTrue(result.contains("\"string\""));
        assertTrue(result.contains("123"));
        assertTrue(result.contains("true"));
        assertTrue(result.contains("false"));
        assertTrue(result.contains("null"));
        assertTrue(result.contains("{\"key\":\"value\"}"));
        assertTrue(result.contains("[1,2,3]"));
    }

    @Test
    void testToStringWithBooleanAndNumbers() throws ApplicationException {
        builders.parse("[true, false, 42, 3.14, -10, 0]");
        
        String result = builders.toString();
        assertTrue(result.contains("true"));
        assertTrue(result.contains("false"));
        assertTrue(result.contains("42"));
        assertTrue(result.contains("3.14"));
        assertTrue(result.contains("-10"));
        assertTrue(result.contains("0"));
    }

    @Test
    void testToStringEmptyArray() {
        assertEquals("[]", builders.toString());
    }

    @Test
    void testToStringSingleElement() throws ApplicationException {
        builders.parse("[\"single\"]");
        assertEquals("[\"single\"]", builders.toString());
    }

    @Test
    void testToStringSingleNumber() throws ApplicationException {
        builders.parse("[42]");
        assertEquals("[42]", builders.toString());
    }

    @Test
    void testToStringSingleBoolean() throws ApplicationException {
        builders.parse("[true]");
        assertEquals("[true]", builders.toString());
    }

    @Test
    void testToStringSingleNull() throws ApplicationException {
        builders.parse("[null]");
        assertEquals("[null]", builders.toString());
    }

    @Test
    void testToStringSingleObject() throws ApplicationException {
        builders.parse("[{\"key\":\"value\"}]");
        assertEquals("[{\"key\":\"value\"}]", builders.toString());
    }

    @Test
    void testToStringSingleArray() throws ApplicationException {
        builders.parse("[[1,2,3]]");
        assertEquals("[[1,2,3]]", builders.toString());
    }

    @Test
    void testToStringWithEmptyStrings() throws ApplicationException {
        builders.parse("[\"\", \"value\", \"\"]");
        
        String result = builders.toString();
        assertTrue(result.contains("\"\""));
        assertTrue(result.contains("\"value\""));
    }

    @Test
    void testToStringWithVeryLongStrings() throws ApplicationException {
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longString.append("This is a very long string with many characters. ");
        }
        
        builders.add(new Builder(longString.toString()));
        
        String result = builders.toString();
        assertTrue(result.startsWith("[\""));
        assertTrue(result.endsWith("\"]"));
        assertTrue(result.contains("This is a very long string"));
    }

    @Test
    void testToStringWithZeroAndNegativeNumbers() throws ApplicationException {
        builders.parse("[0, -42, 0.0, -3.14]");
        
        String result = builders.toString();
        assertTrue(result.contains("0"));
        assertTrue(result.contains("-42"));
        assertTrue(result.contains("0.0"));
        assertTrue(result.contains("-3.14"));
    }

    @Test
    void testToStringThreadSafety() throws Exception {
        int threadCount = 10;
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        Builders sharedBuilders = new Builders();
        sharedBuilders.add(new Builder("shared"));

        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < iterations; j++) {
                    Builders localBuilders = new Builders();
                    localBuilders.add(new Builder("thread-" + threadId));
                    localBuilders.add(new Builder(j));
                    
                    String result = localBuilders.toString();
                    assertTrue(result.startsWith("["));
                    assertTrue(result.endsWith("]"));
                    assertTrue(result.contains("\"thread-" + threadId + "\""));
                    assertTrue(result.contains(String.valueOf(j)));
                    
                    // Test shared builders toString in parallel
                    String sharedResult = sharedBuilders.toString();
                    assertTrue(sharedResult.contains("\"shared\""));
                }
            }, executor);
        }

        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);
        executor.shutdown();
    }

    @Test
    void testToStringWithComplexNestedStructure() throws ApplicationException {
        builders.parse("[{\"name\":\"Test\",\"age\":25,\"hobbies\":[\"reading\",\"gaming\"]}, [1,2,{\"nested\":\"value\"}]]");
        
        String result = builders.toString();
        
        // Verify structure
        assertTrue(result.contains("{\"name\":\"Test\",\"age\":25,\"hobbies\":[\"reading\",\"gaming\"]}") || result.contains("{\"hobbies\":[\"reading\",\"gaming\"],\"name\":\"Test\",\"age\":25}"));
        assertTrue(result.contains("[1,2,{\"nested\":\"value\"}]"));
    }

    @Test
    void testToStringWithLargeArray() throws ApplicationException {
        StringBuilder input = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) input.append(",");
            input.append(i);
        }
        input.append("]");
        
        builders.parse(input.toString());
        
        String result = builders.toString();
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("0"));
        assertTrue(result.contains("99"));
    }

    @Test
    void testToStringWithUnicodeCharacters() throws ApplicationException {
        builders.parse("[\"Hello 世界\", \"مرحبا بالعالم\", \"🌍🌎🌏\"]");
        
        String result = builders.toString();
        // Unicode characters will be converted to escape sequences
        assertTrue(result.contains("\"Hello \\u4E16\\u754C\""));
        assertTrue(result.contains("\"\\u0645\\u0631\\u062D\\u0628\\u0627 \\u0628\\u0627\\u0644\\u0639\\u0627\\u0644\\u0645\""));
        assertTrue(result.contains("\"\\uD83C\\uDF0D\\uD83C\\uDF0E\\uD83C\\uDF0F\""));
    }

    @Test
    void testToStringWithEscapedQuotesInStrings() throws ApplicationException {
        builders.parse("[\"He said \\\"Hello\\\"\", \"She said \\\"Hi\\\"\"]");
        
        String result = builders.toString();
        System.out.println("result = " + result);
        assertTrue(result.contains("\"He said \\\\\\\"Hello\\\\\\\"\""));
        assertTrue(result.contains("\"She said \\\\\\\"Hi\\\\\\\"\""));
    }

    @Test
    void testToStringWithBackslashesInStrings() throws ApplicationException {

        String text= "[\"Path: C:\\\\Users\\\\Test\", \"Regex: \\\\d+\\\\w*\"]";
        builders.parse(text);
        
        String result = builders.toString();
        System.out.println("result = " + result);
        assertTrue(result.contains("\"Path: C:\\\\\\\\Users\\\\\\\\Test\""));
        assertTrue(result.contains("\"Regex: \\\\\\\\d+\\\\\\\\w*\""));
    }

    @Test
    void testToStringWithControlCharacters() throws ApplicationException {
        builders.parse("[\"Line1\\nLine2\\rLine3\", \"Tab\\tSeparated\"]");
        
        String result = builders.toString();
        System.out.println("result = " + result);
        assertTrue(result.contains("\"Line1\\\\nLine2\\\\rLine3\""));
        assertTrue(result.contains("\"Tab\\\\tSeparated\""));
    }

    @Test
    void testToStringConsistency() throws ApplicationException {
        // Test that multiple calls to toString() return the same result
        builders.parse("[\"test\", 123, true]");
        
        String result1 = builders.toString();
        String result2 = builders.toString();
        String result3 = builders.toString();
        
        assertEquals(result1, result2);
        assertEquals(result2, result3);
    }

    @Test
    void testToStringWithAllPrimitiveTypes() throws ApplicationException {
        builders.parse("[true, false, null, 0, 1, -1, 0.0, 1.5, -3.14, \"string\", \"\"]");
        
        String result = builders.toString();
        assertTrue(result.contains("true"));
        assertTrue(result.contains("false"));
        assertTrue(result.contains("null"));
        assertTrue(result.contains("0"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("-1"));
        assertTrue(result.contains("0.0"));
        assertTrue(result.contains("1.5"));
        assertTrue(result.contains("-3.14"));
        assertTrue(result.contains("\"string\""));
        assertTrue(result.contains("\"\""));
    }
}
