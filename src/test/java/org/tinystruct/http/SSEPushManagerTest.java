package org.tinystruct.http;

import org.junit.jupiter.api.Test;
import org.tinystruct.data.component.Builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SSEPushManagerTest {

    @Test
    public void testFormatSSEMessageConnect() {
        Builder message = new Builder();
        message.put("type", "connect");
        String formatted = SSEPushManager.formatSSEMessage(message);
        assertEquals("event: connect\ndata: Connected\n\n", formatted);
    }

    @Test
    public void testFormatSSEMessageData() {
        Builder message = new Builder();
        message.put("content", "hello");
        String formatted = SSEPushManager.formatSSEMessage(message);
        assertTrue(formatted.startsWith("data: "));
        assertTrue(formatted.contains("\"content\":\"hello\""));
        assertTrue(formatted.endsWith("\n\n"));
    }
}
