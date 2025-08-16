package org.tinystruct.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MCPPushManagerTest {
    @Test
    public void testSingletonInstance() {
        MCPPushManager instance1 = MCPPushManager.getInstance();
        MCPPushManager instance2 = MCPPushManager.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    public void testInheritance() {
        assertTrue(MCPPushManager.getInstance() instanceof org.tinystruct.http.SSEPushManager);
    }
} 