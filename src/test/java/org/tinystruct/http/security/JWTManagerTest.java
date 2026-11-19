package org.tinystruct.http.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.data.component.Builder;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

public class JWTManagerTest {

    private static final String SECRET = "mySecretKey123213!@3!2#13@!@#!#!@#!#!@E";
    private static final String SUBJECT = "user123";
    private static final long VALIDITY = 1; // 1 hour validity

    private JWTManager jwtManager = new JWTManager();

    @BeforeEach
    public void setUp() {
        jwtManager = new JWTManager();
        jwtManager.withSecret(SECRET);
    }

    @Test
    public void testCreateAndParseToken() {
        assertNotNull(jwtManager); // Ensure jwtManager is not null
        jwtManager.withSecret(SECRET);
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
        assertNotNull(jwtManager); // Ensure jwtManager is not null
        jwtManager.withSecret(SECRET);

        // Create a builder with some claims
        Builder builder = new Builder();
        builder.put("role", "admin");

        // Create a JWT with -1 hour validity (already expired)
        String token = jwtManager.createToken(SUBJECT, builder, -1);

        // Parsing an expired token should throw ExpiredJwtException
        assertThrows(ExpiredJwtException.class, () -> jwtManager.parseToken(token));
    }

    @Test
    public void testTokenWithClockSkew() {
        assertNotNull(jwtManager); // Ensure jwtManager is not null
        jwtManager.withSecret(SECRET);

        // Create a builder with some claims
        Builder builder = new Builder();
        builder.put("role", "admin");

        // Create a JWT with 0 validity (expires immediately)
        String token = jwtManager.createToken(SUBJECT, builder, 0);

        // Sleep for 1 second to ensure token expiration
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Set timezone to UTC to ensure there's some clock skew if the system is not in
        // UTC
        jwtManager.withTimezone(ZoneId.of("UTC"));

        // Parsing the token should NOT throw ExpiredJwtException because of clock skew
        assertDoesNotThrow(() -> jwtManager.parseToken(token));

        Jws<Claims> parsedToken = jwtManager.parseToken(token);
        assertEquals(SUBJECT, parsedToken.getPayload().getSubject());
    }

    @Test
    public void testInvalidToken() {
        assertNotNull(jwtManager); // Ensure jwtManager is not null
        jwtManager.withSecret(SECRET);

        // Create an invalid JWT (tampered token)
        String invalidToken = "invalid.token";

        // Parsing an invalid token should throw MalformedJwtException
        assertThrows(MalformedJwtException.class, () -> jwtManager.parseToken(invalidToken));
    }

    @Test
    public void testExact64AWithEqualsRejectedBeforeParsing() {
        String payload = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
        MalformedJwtException ex = assertThrows(MalformedJwtException.class, () -> jwtManager.parseToken(payload));
        assertTrue(ex.getMessage().contains("Token validation failed"),
                "Exception should indicate TokenGuard validation failure: " + ex.getMessage());
    }

    @Test
    public void testExact64AWithoutEqualsRejectedAsMalformedJwt() {
        String payload = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        MalformedJwtException ex = assertThrows(MalformedJwtException.class, () -> jwtManager.parseToken(payload));
        assertTrue(ex.getMessage().contains("Token validation failed"),
                "Exception should indicate TokenGuard validation failure: " + ex.getMessage());
    }

    @Test
    public void testEmptyAndBlankTokensThrowMalformedJwtException() {
        assertThrows(MalformedJwtException.class, () -> jwtManager.parseToken(""));
        assertThrows(MalformedJwtException.class, () -> jwtManager.parseToken("   "));
        assertThrows(MalformedJwtException.class, () -> jwtManager.parseToken(null));
    }

    @Test
    public void testOversizedTokenThrowsMalformedJwtException() {
        String oversized = "A".repeat(5000);
        MalformedJwtException ex = assertThrows(MalformedJwtException.class, () -> jwtManager.parseToken(oversized));
        assertTrue(ex.getMessage().contains("OVERSIZED"),
                "Exception should indicate OVERSIZED token: " + ex.getMessage());
    }

    @Test
    public void testBlockedPatternThrowsBlockedTokenException() {
        TokenGuard guard = new TokenGuard();
        guard.setPatternDetection(true);
        guard.setBlockPatterns("^DENIED-.*,.*MALICIOUS.*");
        jwtManager.withTokenGuard(guard);

        JWTManager.BlockedTokenException ex = assertThrows(JWTManager.BlockedTokenException.class,
                () -> jwtManager.parseToken("DENIED-token-xyz"));
        assertTrue(ex.getMessage().contains("Token blocked by policy"));
        // Must be catchable as JwtException so callers map it to an auth failure, not a 500
        assertInstanceOf(io.jsonwebtoken.JwtException.class, ex);
    }

    @Test
    public void testTokenGuardDisabledAllowsBypass() {
        jwtManager.getTokenGuard().setEnabled(false);

        // When guard is disabled, JJWT's parser receives it directly and throws JJWT's MalformedJwtException
        MalformedJwtException ex = assertThrows(MalformedJwtException.class,
                () -> jwtManager.parseToken("invalid.token"));
        assertFalse(ex.getMessage().contains("Token validation failed"),
                "Guard message should not appear when guard is disabled");
    }
}
