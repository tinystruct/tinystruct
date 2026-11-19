package org.tinystruct.mcp;

import org.junit.jupiter.api.Test;
import org.tinystruct.http.Header;
import org.tinystruct.http.Headers;
import org.tinystruct.http.Request;
import org.tinystruct.http.security.TokenGuard;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

public class AuthorizationHandlerTokenGuardTest {

    @SuppressWarnings("unchecked")
    private Request<?, ?> requestWith(String authorization) {
        Headers headers = new Headers();
        headers.add(Header.AUTHORIZATION.set(authorization));
        return (Request<?, ?>) Proxy.newProxyInstance(
                Request.class.getClassLoader(),
                new Class<?>[]{Request.class},
                (proxy, method, args) -> {
                    if ("headers".equals(method.getName())) {
                        return headers;
                    }
                    return null;
                }
        );
    }

    @Test
    public void bareTokenStillAccepted() {
        AuthorizationHandler handler = new AuthorizationHandler("secret-token-123", new TokenGuard());
        assertNotNull(handler.validateAuthHeader(requestWith("secret-token-123")));
    }

    @Test
    public void bearerPrefixedHeaderIsNotRejectedByGuardForContainingSpace() {
        // Header "Bearer x" != configured "x" so authentication fails, but it must fail as an
        // authentication error, not because the guard treated the scheme's space as illegal.
        AuthorizationHandler handler = new AuthorizationHandler("secret-token-123", new TokenGuard());
        SecurityException ex = assertThrows(SecurityException.class,
                () -> handler.validateAuthHeader(requestWith("Bearer secret-token-123")));
        assertEquals("Invalid authorization", ex.getMessage());
    }

    @Test
    public void oversizedAndMalformedCredentialsRejectedByGuard() {
        AuthorizationHandler handler = new AuthorizationHandler("secret-token-123", new TokenGuard().setMaxLength(32));
        SecurityException oversized = assertThrows(SecurityException.class,
                () -> handler.validateAuthHeader(requestWith("A".repeat(100))));
        assertTrue(oversized.getMessage().contains("exceeds maximum length"));

        SecurityException padded = assertThrows(SecurityException.class,
                () -> handler.validateAuthHeader(requestWith("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")));
        assertTrue(padded.getMessage().contains("Invalid authorization"));
    }
}
