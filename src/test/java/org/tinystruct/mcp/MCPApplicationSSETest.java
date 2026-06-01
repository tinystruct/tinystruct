package org.tinystruct.mcp;

import org.junit.jupiter.api.Test;
import org.tinystruct.data.component.Builder;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.http.Session;
import org.tinystruct.system.Settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MCPApplicationSSETest {

    @Test
    public void testHandleSseConnect() {
        MCPServer app = new MCPServer();
        app.setConfiguration(new Settings());
        app.init();

        Request request = mock(Request.class);
        Response response = mock(Response.class);
        Session session = mock(Session.class);
        
        when(request.getSession()).thenReturn(session);
        when(session.getId()).thenReturn("test-session-id");
        
        Builder result = app.handleSseConnect(request, response);
        assertNotNull(result);
        assertEquals("connect", result.get("type"));
    }
}
