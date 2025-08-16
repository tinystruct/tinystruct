package org.tinystruct.data.component;

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Builder and Builders classes with CharSequenceView improvements.
 */
public class BuilderCharSequenceViewTest {

    @Test
    public void testBuilderWithCharSequenceView() throws ApplicationException {
        // Test simple JSON object parsing
        String json = "{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}";
        Builder builder = new Builder();
        builder.parse(json);
        
        assertEquals("John", builder.get("name"));
        assertEquals(30, builder.get("age"));
        assertEquals("New York", builder.get("city"));
    }

    @Test
    public void testBuilderWithNestedObjects() throws ApplicationException {
        // Test nested JSON object parsing
        String json = "{\"person\":{\"name\":\"Jane\",\"age\":25},\"active\":true}";
        Builder builder = new Builder();
        builder.parse(json);

        assertInstanceOf(Builder.class, builder.get("person"));
        Builder person = (Builder) builder.get("person");
        assertEquals("Jane", person.get("name"));
        assertEquals(25, person.get("age"));
        assertEquals(true, builder.get("active"));
    }

    @Test
    public void testBuildersWithCharSequenceView() throws ApplicationException {
        // Test array parsing
        String json = "[\"apple\",\"banana\",\"cherry\"]";
        Builders builders = new Builders();
        builders.parse(json);
        
        assertEquals(3, builders.size());
        assertEquals("apple", builders.get(0).getValue());
        assertEquals("banana", builders.get(1).getValue());
        assertEquals("cherry", builders.get(2).getValue());
    }

    @Test
    public void testBuildersWithMixedTypes() throws ApplicationException {
        // Test array with mixed types
        String json = "[\"string\",123,true,{\"key\":\"value\",\"key1\":\"value1\"}]";
        Builders builders = new Builders();
        builders.parse(json);
        
        assertEquals(4, builders.size());
        assertEquals("string", builders.get(0).getValue());
        assertEquals(123, builders.get(1).getValue());
        assertEquals("true", builders.get(2).getValue()); // Boolean values are stored as strings in Builders
        
        // Debug: print the actual type and value
        Object thirdValue = builders.get(3);
        System.out.println("Third value type: " + (thirdValue != null ? thirdValue.getClass().getName() : "null"));
        System.out.println("Third value: " + thirdValue);
        assertInstanceOf(Builder.class, thirdValue);
    }

    @Test
    public void testBuilderWithWhitespacePreservation() throws ApplicationException {
        // Test that whitespace is preserved in string values
        String json = "{\"message\":\"Hello\\nWorld\",\"description\":\"  Preserved  \"}";
        Builder builder = new Builder();
        builder.parse(json);
        
        assertEquals("Hello\\nWorld", builder.get("message")); // Escaped newline should be preserved as literal
        assertEquals("  Preserved  ", builder.get("description"));
    }

    @Test
    public void testBuildersWithNestedArrays() throws ApplicationException {
        // Test nested arrays
        String json = "[[1,2,3],[4,5,6]]";
        Builders builders = new Builders();
        builders.parse(json);
        
        assertEquals(2, builders.size());
        
        // Debug: print the actual types
        Object firstValue = builders.get(0);
        Object secondValue = builders.get(1);
        System.out.println("First value type: " + (firstValue != null ? firstValue.getClass().getName() : "null"));
        System.out.println("First value: " + firstValue);
        System.out.println("Second value type: " + (secondValue != null ? secondValue.getClass().getName() : "null"));
        System.out.println("Second value: " + secondValue);
        
        // Nested arrays are stored as Builder objects with numeric keys
        assertTrue(firstValue instanceof Builder);
        assertTrue(secondValue instanceof Builder);
    }

    @Test
    public void testBuilderToString() throws ApplicationException {
        // Test that toString produces valid JSON
        Builder builder = new Builder();
        builder.put("name", "Test");
        builder.put("number", 42);
        builder.put("boolean", true);
        
        String result = builder.toString();
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        assertTrue(result.contains("\"name\":\"Test\""));
        assertTrue(result.contains("\"number\":42"));
        assertTrue(result.contains("\"boolean\":true"));
    }

    @Test
    public void testBuilderWithArrayValue() throws ApplicationException {
        // Test that Builder can handle array values
        String[] array = {"apple", "banana", "cherry"};
        Builder builder = new Builder(array);
        
        String result = builder.toString();
        assertEquals("[\"apple\",\"banana\",\"cherry\"]", result);
        
        // Test with mixed array
        Object[] mixedArray = {"string", 123, true};
        Builder mixedBuilder = new Builder(mixedArray);
        
        String mixedResult = mixedBuilder.toString();
        assertEquals("[\"string\",123,true]", mixedResult);
    }

    @Test
    public void testBuilderSingleValueMode() throws ApplicationException {
        // Test single value mode
        Builder singleValueBuilder = new Builder("test");
        assertTrue(singleValueBuilder.isSingleValue());
        assertEquals("test", singleValueBuilder.getValue());
        
        // Test map mode
        Builder mapBuilder = new Builder();
        mapBuilder.put("key", "value");
        assertFalse(mapBuilder.isSingleValue());
        
        // Test that getValue() throws exception in map mode
        assertThrows(IllegalStateException.class, () -> {
            mapBuilder.getValue();
        });
        
        // Test empty builder
        Builder emptyBuilder = new Builder();
        assertFalse(emptyBuilder.isSingleValue());
        assertNull(emptyBuilder.getValue());
    }

    @Test
    public void testBuildersToString() throws ApplicationException {
        // Test that Builders toString produces valid JSON array
        Builders builders = new Builders();
        builders.add(new Builder("item1"));
        builders.add(new Builder("item2"));
        
        String result = builders.toString();
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("\"item1\""));
        assertTrue(result.contains("\"item2\""));
    }

    @Test
    public void testInvalidJsonHandling() {
        // Test that invalid JSON throws appropriate exceptions
        String invalidJson = "invalid";
        Builder builder = new Builder();
        
        assertThrows(ApplicationException.class, () -> {
            builder.parse(invalidJson);
        });
    }

    @Test
    public void testEmptyJsonHandling() throws ApplicationException {
        // Test empty JSON objects and arrays
        Builder emptyBuilder = new Builder();
        emptyBuilder.parse("{}");
        assertEquals(0, emptyBuilder.size());
        
        Builders emptyBuilders = new Builders();
        emptyBuilders.parse("[]");
        assertEquals(0, emptyBuilders.size());
    }
}
