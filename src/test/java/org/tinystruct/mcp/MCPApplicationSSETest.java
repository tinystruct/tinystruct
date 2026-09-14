package org.tinystruct.mcp;

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.http.Session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MCPApplicationSSETest {

    @Test
    public void testHandleSseConnect() throws ApplicationException {
        MCPServer app = new MCPServer();
        // Deliberately not calling setConfiguration()/ApplicationManager.init()/install() here:
        // handleSseConnect() only needs getContext() to be non-null, which setContext() alone
        // provides. setConfiguration() has two process-wide side effects that would leak into
        // every other test sharing this JVM if used for a throwaway instance like this one:
        // (1) AbstractApplication.setConfiguration() registers this instance's @Action-annotated
        // methods into the static ActionRegistry singleton, keyed by path - and ActionRegistry
        // never replaces an existing equal-priority registration for a path, so whichever
        // MCPServer instance calls setConfiguration() first "wins" that route for the rest of the
        // JVM's life, silently stealing real requests away from e.g. BaseMCPTest's fully-configured
        // server; (2) MCPApplication.init() (which setConfiguration() also triggers) builds this
        // instance's own AuthorizationHandler from whatever auth token happens to be in the shared
        // config at that moment, which would then be the one enforced for that hijacked route.
        app.setContext(new ApplicationContext());

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
