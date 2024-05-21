package org.tinystruct.http.security;

import io.jsonwebtoken.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.data.component.Builder;

import static org.junit.jupiter.api.Assertions.*;

public class JWTManagerTest {

    private static final String SECRET = "mySecretKey123213!@3!2#13@!@#!#!@#!#!@E";
    private static final String SUBJECT = "user123";
    private static final long VALIDITY = 1; // 1 hour validity

    private JWTManager jwtManager;

    @BeforeEach
    public void setUp() {
        jwtManager = new JWTManager();
        jwtManager.withSecret(SECRET);
    }

    @Test
    public void testCreateAndParseToken() {
        // Create a builder with some claims
        Builder builder = new Builder();
        builder.put("role", "admin");

        // Create a JWT
        String token = jwtManager.createToken(SUBJECT, builder, VALIDITY);

        // Parse the JWT
        Jws<Claims> parsedToken = jwtManager.parseToken(token);

        // Verify the subject and claims
        assertEquals(SUBJECT, parsedToken.getPayload().getSubject());
        assertEquals("admin", parsedToken.getPayload().get("role", String.class));
    }

    @Test
    public void testExpiredToken() {
        // Create a builder with some claims
        Builder builder = new Builder();
        builder.put("role", "admin");

        // Create a JWT with 1 second validity
        String token = jwtManager.createToken(SUBJECT, builder, 0);

        // Sleep for 1 second to ensure token expiration
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Parsing an expired token should throw ExpiredJwtException
        assertThrows(ExpiredJwtException.class, () -> jwtManager.parseToken(token));
    }

    @Test
    public void testInvalidToken() {
        // Create an invalid JWT (tampered token)
        String invalidToken = "invalid.token";

        // Parsing an invalid token should throw MalformedJwtException
        assertThrows(MalformedJwtException.class, () -> jwtManager.parseToken(invalidToken));
    }
}

