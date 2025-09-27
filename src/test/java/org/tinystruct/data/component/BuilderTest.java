package org.tinystruct.data.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;
import org.tinystruct.system.util.TextFileLoader;

import java.io.File;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class BuilderTest {

    @Test
    void parse() {
        String payload = "{\n" +
                "  \"model\": \"text-davinci-003,ok\"," +
                "  \"prompt\": \"\"," +
                "  \"max_tokens\": 2500," +
                "  \"temperature\": 0," +
                "  \"correct\": true," +
                "  \"passed\": FALSE," +
                "  \"object\":[{\"id\":\"0001\"}]" +
                "}";

        Builder builder = new Builder();
        Assertions.assertDoesNotThrow(() -> {
            builder.parse(payload);

            assertEquals("text-davinci-003,ok", builder.get("model"));
            assertEquals("", builder.get("prompt"));
            assertEquals(true, builder.get("correct"));
            assertEquals(false, builder.get("passed"));
            assertEquals(2500, builder.get("max_tokens"));
            assertEquals(0, builder.get("temperature"));
        });
    }

    @Test
    void testBuilderToString() {
        Builder builder = new Builder();
        builder.put("name", "John Doe");
        builder.put("age", 25);
        builder.put("isStudent", true);

        String expected = "{\"name\":\"John Doe\",\"age\":25,\"isStudent\":true}";
        assertEquals(expected, builder.toString());
    }

    @Test
    void testBuilderParse() throws ApplicationException {
        String jsonString = "{\"name\":\"John Doe\",\"age\":25,\"isStudent\":true}";
        Builder builder = new Builder();
        builder.parse(jsonString);

        assertEquals("John Doe", builder.get("name"));
        assertEquals(25, builder.get("age"));
        assertEquals(true, builder.get("isStudent"));
    }

    @Test
    void testBuilderSaveAsFile() throws ApplicationException {
        Builder builder = new Builder();
        builder.put("name", "Jane Doe");
        builder.put("age", 30);
        builder.put("isStudent", false);

        // Specify a file path where you want to save the data
        String filePath = "src/test/testBuilderSaveAsFile.json";
        builder.saveAsFile(new File(filePath));

        // Load the saved file and parse it to check if the data is saved correctly
        Builder loadedBuilder = new Builder();
        loadedBuilder.parse(new TextFileLoader(new File(filePath)).getContent().toString());

        assertEquals("Jane Doe", loadedBuilder.get("name"));
        assertEquals(30, loadedBuilder.get("age"));
        assertEquals(false, loadedBuilder.get("isStudent"));

        // Clean up: Delete the created file
        new File(filePath).delete();
    }

    // ========== Enhanced toString() Tests ==========

    @Test
    void testToStringWithStringEscaping() {
        Builder builder = new Builder();
        builder.put("quotes", "He said \"Hello World\"");
        builder.put("backslashes", "Path: C:\\Users\\Test");
        builder.put("newlines", "Line1\nLine2\rLine3");
        builder.put("tabs", "Column1\tColumn2");

        String result = builder.toString();
        assertTrue(result.contains("\"He said \\\"Hello World\\\"\""));
        assertTrue(result.contains("\"Path: C:\\\\Users\\\\Test\""));
        assertTrue(result.contains("\"Line1\\nLine2\\rLine3\""));
        assertTrue(result.contains("\"Column1\\tColumn2\""));
    }

    @Test
    void testToStringWithSpecialCharacters() {
        Builder builder = new Builder();
        builder.put("unicode", "Hello 世界");
        builder.put("symbols", "Special chars: !@#$%^&*()");
        builder.put("empty", "");

        String result = builder.toString();
        // Chinese characters will be converted to Unicode escape sequences
        assertTrue(result.contains("\"Hello \\u4E16\\u754C\""));
        assertTrue(result.contains("\"Special chars: !@#$%^&*()\""));
        assertTrue(result.contains("\"empty\":\"\""));
    }

    @Test
    void testToStringWithNullValues() {
        Builder builder = new Builder();
        builder.put("nullValue", null);
        builder.put("stringValue", "test");

        String result = builder.toString();
        assertTrue(result.contains("\"nullValue\":null"));
        assertTrue(result.contains("\"stringValue\":\"test\""));
    }

    @Test
    void testToStringWithNestedBuilders() {
        Builder outer = new Builder();
        Builder inner = new Builder();
        inner.put("nestedKey", "nestedValue");
        outer.put("inner", inner);

        String result = outer.toString();
        assertTrue(result.contains("\"inner\":{\"nestedKey\":\"nestedValue\"}"));
    }

    @Test
    void testToStringWithNestedBuildersArray() {
        Builder outer = new Builder();
        Builders innerArray = new Builders();
        innerArray.add(new Builder("item1"));
        innerArray.add(new Builder("item2"));
        outer.put("array", innerArray);

        String result = outer.toString();
        assertTrue(result.contains("\"array\":[\"item1\",\"item2\"]"));
    }

    @Test
    void testToStringWithPrimitiveArrays() {
        Builder builder = new Builder();
        builder.put("intArray", new int[]{1, 2, 3});
        builder.put("stringArray", new String[]{"a", "b", "c"});

        String result = builder.toString();
        assertTrue(result.contains("\"intArray\":[1,2,3]"));
        assertTrue(result.contains("\"stringArray\":[\"a\",\"b\",\"c\"]"));
    }

    @Test
    void testToStringWithBooleanAndNumbers() {
        Builder builder = new Builder();
        builder.put("booleanTrue", true);
        builder.put("booleanFalse", false);
        builder.put("integer", 42);
        builder.put("double", 3.14);
        builder.put("long", 123456789L);

        String result = builder.toString();
        assertTrue(result.contains("\"booleanTrue\":true"));
        assertTrue(result.contains("\"booleanFalse\":false"));
        assertTrue(result.contains("\"integer\":42"));
        assertTrue(result.contains("\"double\":3.14"));
        assertTrue(result.contains("\"long\":123456789"));
    }

    @Test
    void testToStringEmptyBuilder() {
        Builder builder = new Builder();
        assertEquals("{}", builder.toString());
    }

    @Test
    void testToStringSingleValueString() {
        Builder builder = new Builder("test string");
        assertEquals("\"test string\"", builder.toString());
    }

    @Test
    void testToStringSingleValueNumber() {
        Builder builder = new Builder(42);
        assertEquals("42", builder.toString());
    }

    @Test
    void testToStringSingleValueArray() {
        Builder builder = new Builder(new String[]{"a", "b", "c"});
        assertEquals("[\"a\",\"b\",\"c\"]", builder.toString());
    }

    @Test
    void testToStringSingleValueWithEscaping() {
        Builder builder = new Builder("He said \"Hello\"");
        assertEquals("\"He said \\\"Hello\\\"\"", builder.toString());
    }

    @Test
    void testToStringOrderConsistency() {
        Builder builder = new Builder();
        builder.put("z", "last");
        builder.put("a", "first");
        builder.put("m", "middle");

        String result = builder.toString();
        // HashMap doesn't guarantee order, but we test that all keys are present
        assertTrue(result.contains("\"z\":\"last\""));
        assertTrue(result.contains("\"a\":\"first\""));
        assertTrue(result.contains("\"m\":\"middle\""));
    }

    @Test
    void testToStringThreadSafety() throws InterruptedException, ExecutionException, TimeoutException {
        int threadCount = 10;
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        Builder sharedBuilder = new Builder();
        sharedBuilder.put("shared", "value");

        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < iterations; j++) {
                    Builder localBuilder = new Builder();
                    localBuilder.put("thread", threadId);
                    localBuilder.put("iteration", j);
                    
                    String result = localBuilder.toString();
                    assertTrue(result.startsWith("{"));
                    assertTrue(result.endsWith("}"));
                    assertTrue(result.contains("\"thread\":" + threadId));
                    assertTrue(result.contains("\"iteration\":" + j));
                    
                    // Test shared builder toString in parallel
                    String sharedResult = sharedBuilder.toString();
                    assertTrue(sharedResult.contains("\"shared\":\"value\""));
                }
            }, executor);
        }

        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);
        executor.shutdown();
    }

    @Test
    void testToStringWithComplexNestedStructure() {
        Builder root = new Builder();
        
        // Add primitive values
        root.put("name", "Test");
        root.put("age", 25);
        root.put("active", true);
        
        // Add nested object
        Builder address = new Builder();
        address.put("street", "123 Main St");
        address.put("city", "Anytown");
        root.put("address", address);
        
        // Add array of objects
        Builders hobbies = new Builders();
        Builder hobby1 = new Builder();
        hobby1.put("name", "Reading");
        hobby1.put("hours", 5);
        Builder hobby2 = new Builder();
        hobby2.put("name", "Gaming");
        hobby2.put("hours", 10);
        hobbies.add(hobby1);
        hobbies.add(hobby2);
        root.put("hobbies", hobbies);
        
        // Add primitive array
        root.put("scores", new int[]{95, 87, 92});

        String result = root.toString();
        
        // Verify structure
        assertTrue(result.contains("\"name\":\"Test\""));
        assertTrue(result.contains("\"age\":25"));
        assertTrue(result.contains("\"active\":true"));
        assertTrue(result.contains("\"address\":{\"street\":\"123 Main St\",\"city\":\"Anytown\"}")||result.contains("\"address\":{\"city\":\"Anytown\",\"street\":\"123 Main St\"}"));
        assertTrue(result.contains("\"hobbies\":[{\"name\":\"Reading\",\"hours\":5},{\"name\":\"Gaming\",\"hours\":10}]")||result.contains("\"hobbies\":[{\"hours\":5,\"name\":\"Reading\"},{\"hours\":10,\"name\":\"Gaming\"}]"));
        assertTrue(result.contains("\"scores\":[95,87,92]"));
    }

    @Test
    void testToStringWithVeryLongString() {
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longString.append("This is a very long string with many characters. ");
        }
        
        Builder builder = new Builder();
        builder.put("longString", longString.toString());
        
        String result = builder.toString();
        assertTrue(result.startsWith("{\"longString\":\""));
        assertTrue(result.endsWith("\"}"));
        assertTrue(result.contains("This is a very long string"));
    }

    @Test
    void testToStringWithEmptyStringValues() {
        Builder builder = new Builder();
        builder.put("empty", "");
        builder.put("normal", "value");
        builder.put("anotherEmpty", "");

        String result = builder.toString();
        assertTrue(result.contains("\"empty\":\"\""));
        assertTrue(result.contains("\"normal\":\"value\""));
        assertTrue(result.contains("\"anotherEmpty\":\"\""));
    }

    @Test
    void testToStringWithZeroAndNegativeNumbers() {
        Builder builder = new Builder();
        builder.put("zero", 0);
        builder.put("negative", -42);
        builder.put("decimal", 0.0);
        builder.put("negativeDecimal", -3.14);

        String result = builder.toString();
        assertTrue(result.contains("\"zero\":0"));
        assertTrue(result.contains("\"negative\":-42"));
        assertTrue(result.contains("\"decimal\":0.0"));
        assertTrue(result.contains("\"negativeDecimal\":-3.14"));
    }
}