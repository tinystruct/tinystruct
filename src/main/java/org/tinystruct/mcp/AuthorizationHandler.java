package org.tinystruct.mcp;

import org.tinystruct.http.Header;
import org.tinystruct.http.Request;
import org.tinystruct.http.security.TokenGuard;

import java.util.UUID;

/**
 * Handles authentication and authorization for MCP requests
 */
public class AuthorizationHandler {
    private final String authToken;
    private final TokenGuard tokenGuard;

    public AuthorizationHandler(String authToken) {
        this(authToken, new TokenGuard());
    }

    public AuthorizationHandler(String authToken, TokenGuard tokenGuard) {
        this.authToken = authToken;
        this.tokenGuard = tokenGuard != null ? tokenGuard : new TokenGuard();
    }

    /**
     * Validates the authorization header from a request
     *
     * @param request HTTP request containing authorization header
     * @return Client ID if authentication successful
     * @throws SecurityException if authentication fails
     */
    public String validateAuthHeader(Request request) throws SecurityException {
        if (this.authToken != null && !this.authToken.isEmpty()) {
            // Check if the request has an Authorization header
            if (!request.headers().contains(Header.AUTHORIZATION)) {
                throw new SecurityException("Authorization header is missing");
            }
            String auth = request.headers().get(Header.AUTHORIZATION).toString();
            if (this.tokenGuard != null) {
                // Validate only the credential part: tolerate an optional "Bearer " scheme prefix,
                // since the raw header legitimately contains a space.
                String credential = auth;
                if (credential != null && credential.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    credential = credential.substring(7).trim();
                }
                TokenGuard.ValidationResult result = this.tokenGuard.validate(credential, TokenGuard.TokenType.GENERIC);
                if (!result.isValid()) {
                    throw new SecurityException("Invalid authorization: " + result.getMessage());
                }
            }
            if (auth == null || !authenticate(auth)) {
                throw new SecurityException("Invalid authorization");
            }
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Validates a token against the configured auth token
     *
     * @param token Token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean authenticate(String token) {
        return authToken.equals(token);
    }

    /**
     * Generates a new authentication token
     *
     * @return New UUID-based auth token
     */
    public static String generateAuthToken() {
        return UUID.randomUUID().toString();
    }
}