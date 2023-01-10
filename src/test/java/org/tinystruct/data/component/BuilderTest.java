package org.tinystruct.data.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BuilderTest {

    @Test
    void parse() {
        String payload = "{\n" +
                "  \"model\": \"text-davinci-003\"," +
                "  \"prompt\": \"\"," +
                "  \"max_tokens\": 2500," +
                "  \"temperature\": 0," +
                "  \"correct\": true," +
                "  \"passed\": FALSE," +
                "}";

        Builder builder = new Builder();
        Assertions.assertDoesNotThrow(() -> {
            builder.parse(payload);

            assertEquals(true, builder.get("correct"));
            assertEquals(false, builder.get("passed"));
            assertEquals(2500, builder.get("max_tokens"));
            assertEquals(0, builder.get("temperature"));
        });
    }
}