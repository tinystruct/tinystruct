package org.tinystruct.http.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.system.Settings;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class TokenGuardTest {

    private TokenGuard tokenGuard;

    // The exact 64 'A' characters followed by '=' (65 chars total)
    private static final String PAYLOAD_64A_EQUALS =
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    // The same 64 'A' characters without '=' (64 chars total)
    private static final String PAYLOAD_64A_NO_EQUALS =
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    @BeforeEach
    public void setUp() {
        tokenGuard = new TokenGuard();
    }

    @Test
    public void testExact64APayloadWithEquals() {
        // Test 1: The exact 64-character 'A' payload followed by '='.
        // Mathematical / syntactic analysis:
        // Length is 65. 65 % 4 = 1.
        // In Base64, a padded string length must be a multiple of 4 (len % 4 == 0).
        // 64 'A's decode to 48 bytes (16 complete quanta of 4 chars).
        // A trailing 17th quantum with only '=' has no data characters, which is invalid Base64.
        TokenGuard.ValidationResult base64Result = tokenGuard.validate(PAYLOAD_64A_EQUALS, TokenGuard.TokenType.BASE64);
        assertFalse(base64Result.isValid(), "65-character payload ending with '=' must be rejected");
        assertEquals(TokenGuard.Status.MALFORMED, base64Result.getStatus());
        assertTrue(base64Result.getMessage().contains("multiple of 4"),
                "Error message should explain invalid padded length: " + base64Result.getMessage());

        // As a Bearer token (which in tinystruct checks JWT or opaque Base64):
        TokenGuard.ValidationResult bearerResult = tokenGuard.validateBearerToken(PAYLOAD_64A_EQUALS);
        assertFalse(bearerResult.isValid(), "Bearer token with invalid Base64 padding must be rejected");
        assertEquals(TokenGuard.Status.MALFORMED, bearerResult.getStatus());

        // As a JWT:
        TokenGuard.ValidationResult jwtResult = tokenGuard.validate(PAYLOAD_64A_EQUALS, TokenGuard.TokenType.JWT);
        assertFalse(jwtResult.isValid(), "Payload lacking periods must be rejected as malformed JWT");
        assertEquals(TokenGuard.Status.MALFORMED, jwtResult.getStatus());
    }

    @Test
    public void testExact64APayloadWithoutEquals() {
        // Test 2: The same payload without '='.
        // Length is 64. 64 % 4 = 0. Contains only valid Base64 characters 'A'.
        // In Base64: valid 48-byte null sequence.
        TokenGuard.ValidationResult base64Result = tokenGuard.validate(PAYLOAD_64A_NO_EQUALS, TokenGuard.TokenType.BASE64);
        assertTrue(base64Result.isValid(), "64 'A' characters is valid Base64 syntax");
        assertEquals(TokenGuard.Status.VALID, base64Result.getStatus());

        TokenGuard.ValidationResult genericResult = tokenGuard.validate(PAYLOAD_64A_NO_EQUALS, TokenGuard.TokenType.GENERIC);
        assertTrue(genericResult.isValid(), "64 'A' characters is valid generic token syntax");

        // However, as a JWT (RFC 7515/7519), it lacks period separators:
        TokenGuard.ValidationResult jwtResult = tokenGuard.validate(PAYLOAD_64A_NO_EQUALS, TokenGuard.TokenType.JWT);
        assertFalse(jwtResult.isValid(), "JWT requires 3 dot-separated parts");
        assertEquals(TokenGuard.Status.MALFORMED, jwtResult.getStatus());
    }

    @Test
    public void testValidBase64WithAndWithoutPadding() {
        // Test 3: Valid Base64 strings with and without padding
        // Valid unpadded Base64 (length 16, 16 % 4 == 0)
        String unpadded = "MDEyMzQ1Njc4OWFiY2RlZg"; // 22 chars unpadded Base64URL
        assertTrue(tokenGuard.validate(unpadded, TokenGuard.TokenType.GENERIC).isValid());

        // Valid 1 '=' padding (length 8, 8 % 4 == 0, 1 padding char)
        String onePadding = "dGVzdDE="; // "test1" in Base64
        assertEquals(8, onePadding.length());
        assertTrue(tokenGuard.validate(onePadding, TokenGuard.TokenType.BASE64).isValid());

        // Valid 2 '==' padding (length 8, 8 % 4 == 0, 2 padding chars)
        String twoPadding = "dGVzdA=="; // "test" in Base64
        assertEquals(8, twoPadding.length());
        assertTrue(tokenGuard.validate(twoPadding, TokenGuard.TokenType.BASE64).isValid());
    }

    @Test
    public void testEmptyAndBlankTokens() {
        // Test 4: Empty tokens
        TokenGuard.ValidationResult nullRes = tokenGuard.validate(null, TokenGuard.TokenType.BEARER);
        assertFalse(nullRes.isValid());
        assertEquals(TokenGuard.Status.MALFORMED, nullRes.getStatus());

        TokenGuard.ValidationResult emptyRes = tokenGuard.validate("", TokenGuard.TokenType.BEARER);
        assertFalse(emptyRes.isValid());
        assertEquals(TokenGuard.Status.MALFORMED, emptyRes.getStatus());

        TokenGuard.ValidationResult spaceRes = tokenGuard.validate("    ", TokenGuard.TokenType.BEARER);
        assertFalse(spaceRes.isValid());
        assertEquals(TokenGuard.Status.MALFORMED, spaceRes.getStatus());
    }

    @Test
    public void testOversizedTokens() {
        // Test 5: Oversized tokens
        tokenGuard.setMaxLength(100);

        String okToken = "A".repeat(100);
        assertTrue(tokenGuard.validate(okToken, TokenGuard.TokenType.GENERIC).isValid());

        String oversizedToken = "A".repeat(101);
        TokenGuard.ValidationResult result = tokenGuard.validate(oversizedToken, TokenGuard.TokenType.GENERIC);
        assertFalse(result.isValid());
        assertEquals(TokenGuard.Status.OVERSIZED, result.getStatus());
        assertTrue(result.getMessage().contains("exceeds maximum length"));
    }

    @Test
    public void testInvalidCharacters() {
        // Test 6: Invalid characters (control characters, newlines, tabs, injection)
        String withNewline = "BearerToken\nWithNewline";
        TokenGuard.ValidationResult r1 = tokenGuard.validate(withNewline, TokenGuard.TokenType.BEARER);
        assertFalse(r1.isValid());
        assertEquals(TokenGuard.Status.MALFORMED, r1.getStatus());

        String withNullByte = "Token\0WithNull";
        TokenGuard.ValidationResult r2 = tokenGuard.validate(withNullByte, TokenGuard.TokenType.BEARER);
        assertFalse(r2.isValid());
        assertEquals(TokenGuard.Status.MALFORMED, r2.getStatus());

        String withSpace = "Token with space";
        TokenGuard.ValidationResult r3 = tokenGuard.validate(withSpace, TokenGuard.TokenType.BEARER);
        assertFalse(r3.isValid());
        assertEquals(TokenGuard.Status.MALFORMED, r3.getStatus());

        String withHtml = "Token<script>";
        TokenGuard.ValidationResult r4 = tokenGuard.validate(withHtml, TokenGuard.TokenType.BEARER);
        assertFalse(r4.isValid());
        assertEquals(TokenGuard.Status.MALFORMED, r4.getStatus());
    }

    @Test
    public void testMalformedEncoding() {
        // Test 7: Malformed encoding
        // '=' in the middle of data
        String equalsInMiddle = "YWJj=def";
        TokenGuard.ValidationResult r1 = tokenGuard.validateBase64Padding(equalsInMiddle);
        assertFalse(r1.isValid());
        assertEquals(TokenGuard.Status.MALFORMED, r1.getStatus());
        assertTrue(r1.getMessage().contains("within data payload"));

        // 3 '=' characters
        String threeEquals = "YWJj===";
        TokenGuard.ValidationResult r2 = tokenGuard.validateBase64Padding(threeEquals);
        assertFalse(r2.isValid());
        assertEquals(TokenGuard.Status.MALFORMED, r2.getStatus());
        assertTrue(r2.getMessage().contains("more than two"));

        // Non-modulo-4 padded length (length 7 ending in '=')
        String nonMod4 = "dGVzdA=";
        TokenGuard.ValidationResult r3 = tokenGuard.validateBase64Padding(nonMod4);
        assertFalse(r3.isValid());
        assertEquals(TokenGuard.Status.MALFORMED, r3.getStatus());
        assertTrue(r3.getMessage().contains("multiple of 4"));

        // Malformed JWT: only 1 dot
        TokenGuard.ValidationResult r4 = tokenGuard.validateJwtStructure("header.payload");
        assertFalse(r4.isValid());
        assertEquals(TokenGuard.Status.MALFORMED, r4.getStatus());

        // Malformed JWT: empty segment
        TokenGuard.ValidationResult r5 = tokenGuard.validateJwtStructure("header..signature");
        assertFalse(r5.isValid());
        assertEquals(TokenGuard.Status.MALFORMED, r5.getStatus());

        // Malformed JWT: contains '=' padding in segment
        TokenGuard.ValidationResult r6 = tokenGuard.validateJwtStructure("header=.payload.signature");
        assertFalse(r6.isValid());
        assertEquals(TokenGuard.Status.MALFORMED, r6.getStatus());
    }

    @Test
    public void testLegitimateTokensContainingEquals() {
        // Test 8: Legitimate tokens containing '=' must NOT be blocked
        String legitimate1 = "c29tZSBkYXRhIQ=="; // 16 chars, valid base64
        TokenGuard.ValidationResult r1 = tokenGuard.validate(legitimate1, TokenGuard.TokenType.BASE64);
        assertTrue(r1.isValid(), "Legitimate Base64 token with '==' must be accepted");

        String legitimate2 = "c29tZSBkYXRhIQ4="; // 16 chars, valid base64
        TokenGuard.ValidationResult r2 = tokenGuard.validate(legitimate2, TokenGuard.TokenType.BASE64);
        assertTrue(r2.isValid(), "Legitimate Base64 token with '=' must be accepted");
    }

    @Test
    public void testProtectionEnabledAndDisabled() {
        // Test 9: Protection enabled and disabled
        tokenGuard.setEnabled(false);

        // When disabled, malformed / oversized payloads pass through guard
        TokenGuard.ValidationResult r1 = tokenGuard.validate(PAYLOAD_64A_EQUALS, TokenGuard.TokenType.BEARER);
        assertTrue(r1.isValid(), "Disabled guard should not reject tokens");

        TokenGuard.ValidationResult r2 = tokenGuard.validate("", TokenGuard.TokenType.BEARER);
        assertTrue(r2.isValid());

        // Re-enable
        tokenGuard.setEnabled(true);
        TokenGuard.ValidationResult r3 = tokenGuard.validate(PAYLOAD_64A_EQUALS, TokenGuard.TokenType.BEARER);
        assertFalse(r3.isValid(), "Re-enabled guard should reject malformed payload");

        // Reject malformed disabled but enabled=true
        tokenGuard.setRejectMalformed(false);
        TokenGuard.ValidationResult r4 = tokenGuard.validate(PAYLOAD_64A_EQUALS, TokenGuard.TokenType.BEARER);
        assertTrue(r4.isValid(), "When rejectMalformed is false, malformed syntax passes guard");
    }

    @Test
    public void testConfiguredDenyPatterns() {
        // Test 10: Configured deny patterns
        tokenGuard.setPatternDetection(true);
        tokenGuard.setBlockPatterns("^blocked-.*,.*MALICIOUS.*");

        // Blocked pattern
        TokenGuard.ValidationResult r1 = tokenGuard.validate("blocked-token-12345", TokenGuard.TokenType.GENERIC);
        assertFalse(r1.isValid());
        assertEquals(TokenGuard.Status.BLOCKED, r1.getStatus());

        // Another blocked pattern
        TokenGuard.ValidationResult r2 = tokenGuard.validate("some-MALICIOUS-content", TokenGuard.TokenType.GENERIC);
        assertFalse(r2.isValid());
        assertEquals(TokenGuard.Status.BLOCKED, r2.getStatus());

        // Normal token
        TokenGuard.ValidationResult r3 = tokenGuard.validate("normal-token-12345", TokenGuard.TokenType.GENERIC);
        assertTrue(r3.isValid());

        // Disable pattern detection: blocked-token passes pattern check
        tokenGuard.setPatternDetection(false);
        TokenGuard.ValidationResult r4 = tokenGuard.validate("blocked-token-12345", TokenGuard.TokenType.GENERIC);
        assertTrue(r4.isValid(), "Pattern check should be skipped when patternDetection is false");
    }

    @Test
    public void testTokenMasking() {
        // Sensitive data protection in logs
        assertEquals("[null]", TokenGuard.mask(null));
        assertEquals("[empty]", TokenGuard.mask(""));
        assertEquals("[masked, len=6]", TokenGuard.mask("secret"));

        String masked = TokenGuard.mask("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        assertFalse(masked.contains("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="));
        assertTrue(masked.startsWith("AAA...AA="));
        assertTrue(masked.contains("len=65"));
    }

    @Test
    public void testRejectedTokenPreventsActionExecution() {
        // Test 12: Confirmation that rejected requests never execute the corresponding action
        java.util.concurrent.atomic.AtomicBoolean actionExecuted = new java.util.concurrent.atomic.AtomicBoolean(false);
        Runnable action = () -> actionExecuted.set(true);

        String malformedToken = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
        TokenGuard.ValidationResult result = tokenGuard.validateBearerToken(malformedToken);

        if (result.isValid()) {
            action.run();
        }

        assertFalse(actionExecuted.get(), "Action must never execute when TokenGuard rejects input");
    }

    @Test
    public void testCatastrophicDenyPatternFailsClosedQuickly() {
        // Evil backtracking pattern; the evaluation budget must cut it off and fail closed.
        tokenGuard.setPatternDetection(true);
        tokenGuard.setBlockPatterns("a*a*a*a*a*b");
        String evil = "a".repeat(25);

        long start = System.nanoTime();
        TokenGuard.ValidationResult result = tokenGuard.validate(evil, TokenGuard.TokenType.GENERIC);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        assertEquals(TokenGuard.Status.BLOCKED, result.getStatus(), "Budget exhaustion must fail closed");
        assertTrue(elapsedMs < 1_000, "Evaluation must be bounded, took " + elapsedMs + " ms");
    }

    @Test
    public void testPerformanceUnusuallyLongInputs() {
        // Test 13: Performance behavior for unusually long inputs
        String longInput = "A".repeat(100_000);
        long start = System.nanoTime();
        TokenGuard.ValidationResult result = tokenGuard.validate(longInput, TokenGuard.TokenType.GENERIC);
        long elapsedNanos = System.nanoTime() - start;

        assertFalse(result.isValid());
        assertEquals(TokenGuard.Status.OVERSIZED, result.getStatus());
        // Verify bounded execution (should take << 5ms, typically < 0.1ms)
        assertTrue(elapsedNanos < 50_000_000L, "Oversized validation should reject in negligible time");
    }
}
