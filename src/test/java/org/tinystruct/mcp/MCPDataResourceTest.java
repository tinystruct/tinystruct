package org.tinystruct.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.data.component.Builder;

import static org.junit.jupiter.api.Assertions.*;

public class MCPDataResourceTest {
    private MCPDataResource localResource;
    private MCPDataResource remoteResource;
    private static final String URI_TEMPLATE = "/data/{id}/info";

    @BeforeEach
    public void setUp() {
        localResource = new MCPDataResource("test-data", "Test data resource", URI_TEMPLATE, null) {
            @Override
            protected boolean supportsLocalExecution() { return true; }
            @Override
            protected Object executeLocally(Builder builder) { return "local-data"; }
        };
        remoteResource = new MCPDataResource("test-data", "Test data resource", URI_TEMPLATE, new MCPClient("http://localhost", null)) {
            @Override
            protected boolean supportsLocalExecution() { return false; }
        };
    }

    @Test
    public void testGetType() {
        assertEquals(MCPResource.ResourceType.DATA, localResource.getType());
    }

    @Test
    public void testGetUriTemplate() {
        assertEquals(URI_TEMPLATE, localResource.getUriTemplate());
    }

    @Test
    public void testExecuteLocal() throws MCPException {
        Builder params = new Builder();
        params.put("id", 123);
        assertEquals("local-data", localResource.execute(params));
    }

    @Test
    public void testExecuteRemoteThrows() {
        Builder params = new Builder();
        params.put("id", 123);
        Exception ex = assertThrows(MCPException.class, () -> remoteResource.execute(params));
        assertTrue(ex.getMessage().contains("Local execution not implemented") || ex.getMessage().contains("Error accessing resource"));
    }

    @Test
    public void testUriTemplateResolution() throws MCPException {
        Builder params = new Builder();
        params.put("id", 42);
        // Use reflection to call private resolveUriTemplate
        try {
            java.lang.reflect.Method m = MCPDataResource.class.getDeclaredMethod("resolveUriTemplate", String.class, java.util.Map.class);
            m.setAccessible(true);
            String resolved = (String) m.invoke(localResource, URI_TEMPLATE, params);
            assertEquals("/data/42/info", resolved);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    public void testConstructorValidation() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> new MCPDataResource("", "desc", URI_TEMPLATE, null));
        assertTrue(ex.getMessage().contains("must not be null or empty"));
    }
} 