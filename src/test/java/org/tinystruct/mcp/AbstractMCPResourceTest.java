package org.tinystruct.mcp;

import org.junit.jupiter.api.Test;
import org.tinystruct.data.component.Builder;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractMCPResourceTest {
    static class DummyResource extends AbstractMCPResource {
        DummyResource(String name, String description) { super(name, description, null); }
        @Override
        public MCPResource.ResourceType getType() { return MCPResource.ResourceType.DATA; }
        @Override
        public Object execute(org.tinystruct.data.component.Builder builder) { return null; }
    }

    @Test
    public void testConstructorValidation() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> new DummyResource("", "desc"));
        assertTrue(ex.getMessage().contains("must not be null or empty"));
    }

    @Test
    public void testGetNameAndDescription() {
        DummyResource res = new DummyResource("foo", "bar");
        assertEquals("foo", res.getName());
        assertEquals("bar", res.getDescription());
    }

    @Test
    public void testValidateParametersDefault() {
        DummyResource res = new DummyResource("foo", "bar");
        assertDoesNotThrow(() -> res.validateParameters(new Builder()));
    }

    @Test
    public void testSupportsLocalExecutionDefault() {
        DummyResource res = new DummyResource("foo", "bar");
        assertFalse(res.supportsLocalExecution());
    }

    @Test
    public void testExecuteLocallyDefaultThrows() {
        DummyResource res = new DummyResource("foo", "bar");
        Exception ex = assertThrows(MCPException.class, () -> res.executeLocally(new Builder()));
        assertTrue(ex.getMessage().contains("Local execution not supported"));
    }
} 