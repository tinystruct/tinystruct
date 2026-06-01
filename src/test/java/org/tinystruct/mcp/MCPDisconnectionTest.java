package org.tinystruct.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.http.*;
import org.tinystruct.system.Settings;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MCPDisconnectionTest {
    private MCPServer app;
    private final String authToken = "test-token";
    private final String sessionId = "test-session-123";

    @BeforeEach
    public void setUp() {
        app = new MCPServer();
        Settings settings = new Settings();
        settings.set(MCPSpecification.Config.AUTH_TOKEN, authToken);
        app.setConfiguration(settings);
        app.init();
    }

    @Test
    public void testHandleSseDisconnectSuccess() {
        // Prepare request/response
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        Headers headers = new Headers();
        headers.add(Header.value0f(MCPSpecification.Http.AUTH_HEADER).set(authToken));
        headers.add(Header.value0f(MCPSpecification.Http.SESSION_ID).set(sessionId));
        
        when(request.headers()).thenReturn(headers);
        
        // We need to be in READY state to disconnect
        // Simulating READY state by manually putting it into sessionStates
        app.sessionStates.put(sessionId, MCPSpecification.SessionState.READY);
        app.sessionMap.put(sessionId, System.currentTimeMillis());

        String result = app.handleSseDisconnect(request, response);
        
        assertNotNull(result);
        assertTrue(result.contains("disconnected"), "Expected result to contain 'disconnected', but was: " + result);
        assertEquals(MCPSpecification.SessionState.DISCONNECTED, app.sessionStates.get(sessionId));
        assertFalse(app.sessionMap.containsKey(sessionId));
    }

    @Test
    public void testHandleSseDisconnectNotReady() {
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        Headers headers = new Headers();
        headers.add(Header.value0f(MCPSpecification.Http.AUTH_HEADER).set(authToken));
        headers.add(Header.value0f(MCPSpecification.Http.SESSION_ID).set(sessionId));
        
        when(request.headers()).thenReturn(headers);
        
        // State is DISCONNECTED by default
        String result = app.handleSseDisconnect(request, response);
        
        assertNotNull(result);
        assertTrue(result.contains("Not in ready state"), "Expected result to contain 'Not in ready state', but was: " + result);
        verify(response).setStatus(ResponseStatus.BAD_REQUEST);
    }

    @Test
    public void testHandleSseDisconnectUnauthorized() {
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        Headers headers = new Headers();
        headers.add(Header.value0f(MCPSpecification.Http.AUTH_HEADER).set("wrong-token"));
        
        when(request.headers()).thenReturn(headers);
        
        String result = app.handleSseDisconnect(request, response);
        
        assertNotNull(result);
        assertTrue(result.contains("Unauthorized"), "Expected result to contain 'Unauthorized', but was: " + result);
        verify(response).setStatus(ResponseStatus.UNAUTHORIZED);
    }
}
