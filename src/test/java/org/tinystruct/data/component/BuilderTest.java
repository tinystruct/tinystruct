package org.tinystruct.data.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;
import org.tinystruct.system.util.TextFileLoader;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}