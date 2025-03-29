package org.tinystruct.mcp;

import org.tinystruct.http.Header;
import org.tinystruct.http.Request;
import java.lang.SecurityException;
import java.util.UUID;

/**
 * Handles authentication and authorization for MCP requests
 */
public class AuthorizationHandler {
    private final String authToken;

    public AuthorizationHandler(String authToken) {
        this.authToken = authToken;
    }

    /**
     * Validates the authorization header from a request
     * @param request HTTP request containing authorization header
     * @return Client ID if authentication successful
     * @throws SecurityException if authentication fails
     */
    public String validateAuthHeader(Request request) throws SecurityException {
        String auth = request.headers().get(Header.AUTHORIZATION).toString();
        if (auth == null || !authenticate(auth)) {
            throw new SecurityException("Invalid authorization");
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Validates a token against the configured auth token
     * @param token Token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean authenticate(String token) {
        return authToken != null && authToken.equals(token);
    }

    /**
     * Generates a new authentication token
     * @return New UUID-based auth token
     */
    public static String generateAuthToken() {
        return UUID.randomUUID().toString();
    }
} 