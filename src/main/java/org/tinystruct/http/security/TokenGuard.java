/*******************************************************************************
 * Copyright  (c) 2013, 2026 James M. ZHOU
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.tinystruct.http.security;

import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * TokenGuard provides configurable, pre-authentication validation for tokens
 * across the tinystruct framework.
 *
 * <p>It defends against malformed, oversized, or malicious token inputs, such as:
 * <ul>
 *   <li>Oversized tokens (preventing buffer/memory exhaustion and DoS)</li>
 *   <li>Empty or whitespace-only tokens</li>
 *   <li>Illegal control characters, newlines, tabs, and non-printable characters</li>
 *   <li>Syntactically invalid Base64 padding (non-modulo-4 lengths, misplaced '=' characters)</li>
 *   <li>Malformed JWT structures (missing or invalid segments)</li>
 *   <li>Explicitly denied token patterns via configurable blocklist</li>
 * </ul>
 *
 * <p>Validation is performed prior to expensive cryptographic operations or action dispatching,
 * returning distinct statuses to enable proper HTTP responses:
 * <ul>
 *   <li>400 Bad Request for malformed or oversized input</li>
 *   <li>401 Unauthorized for valid syntax failing authentication</li>
 *   <li>403 Forbidden for explicitly blocked security policies</li>
 * </ul>
 */
public class TokenGuard {

    private static final Logger LOGGER = Logger.getLogger(TokenGuard.class.getName());

    public static final String GUARD_ENABLED = "security.token.guard.enabled";
    public static final String MAX_LENGTH = "security.token.guard.max-length";
    public static final String REJECT_MALFORMED = "security.token.guard.reject-malformed";
    public static final String PATTERN_DETECTION = "security.token.guard.pattern-detection";
    public static final String BLOCK_PATTERNS = "security.token.guard.block-patterns";

    public static final int DEFAULT_MAX_LENGTH = 4096;
    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_REJECT_MALFORMED = true;
    public static final boolean DEFAULT_PATTERN_DETECTION = false;

    /** Max character reads allowed per deny-pattern evaluation (bounds catastrophic backtracking). */
    private static final long PATTERN_STEP_BUDGET = 50_000L;

    private static final class PatternBudgetExceededException extends RuntimeException {
        PatternBudgetExceededException() {
            super("pattern evaluation budget exceeded", null, false, false);
        }
    }

    /** CharSequence that aborts matching after a fixed number of reads. */
    private static final class BudgetedCharSequence implements CharSequence {
        private final CharSequence delegate;
        private final long[] remaining;

        BudgetedCharSequence(CharSequence delegate, long budget) {
            this(delegate, new long[]{budget});
        }

        private BudgetedCharSequence(CharSequence delegate, long[] remaining) {
            this.delegate = delegate;
            this.remaining = remaining;
        }

        @Override
        public int length() {
            return delegate.length();
        }

        @Override
        public char charAt(int index) {
            if (--remaining[0] < 0) {
                throw new PatternBudgetExceededException();
            }
            return delegate.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new BudgetedCharSequence(delegate.subSequence(start, end), remaining);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    private boolean enabled = DEFAULT_ENABLED;
    private int maxLength = DEFAULT_MAX_LENGTH;
    private boolean rejectMalformed = DEFAULT_REJECT_MALFORMED;
    private boolean patternDetection = DEFAULT_PATTERN_DETECTION;
    private final List<Pattern> blockPatterns = new ArrayList<>();

    public enum TokenType {
        /** Bearer token: JWT if periods are present, otherwise generic Base64/opaque */
        BEARER,
        /** Strict compact JWT (RFC 7515/7519: 3 dot-separated base64url segments without padding) */
        JWT,
        /** Base64 encoded token with strict padding validation */
        BASE64,
        /** General token (length, printable characters, deny patterns) */
        GENERIC
    }

    public enum Status {
        VALID,
        MALFORMED,
        OVERSIZED,
        BLOCKED
    }

    public static class ValidationResult {
        private final Status status;
        private final String message;

        public ValidationResult(Status status, String message) {
            this.status = status;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(Status.VALID, "Token is valid");
        }

        public static ValidationResult malformed(String message) {
            return new ValidationResult(Status.MALFORMED, message);
        }

        public static ValidationResult oversized(String message) {
            return new ValidationResult(Status.OVERSIZED, message);
        }

        public static ValidationResult blocked(String message) {
            return new ValidationResult(Status.BLOCKED, message);
        }

        public boolean isValid() {
            return this.status == Status.VALID;
        }

        public Status getStatus() {
            return this.status;
        }

        public String getMessage() {
            return this.message;
        }

        @Override
        public String toString() {
            return "ValidationResult{status=" + status + ", message='" + message + "'}";
        }
    }

    public TokenGuard() {
        this(new Settings());
    }

    public TokenGuard(Configuration<String> settings) {
        if (settings != null) {
            String enabledVal = settings.get(GUARD_ENABLED);
            if (enabledVal != null && !enabledVal.isEmpty()) {
                this.enabled = Boolean.parseBoolean(enabledVal);
            } else {
                this.enabled = DEFAULT_ENABLED;
            }

            String maxLenStr = settings.get(MAX_LENGTH);
            if (maxLenStr != null && !maxLenStr.isEmpty()) {
                try {
                    this.maxLength = Integer.parseInt(maxLenStr);
                } catch (NumberFormatException e) {
                    this.maxLength = DEFAULT_MAX_LENGTH;
                }
            } else {
                this.maxLength = DEFAULT_MAX_LENGTH;
            }

            String rejectMalformedVal = settings.get(REJECT_MALFORMED);
            if (rejectMalformedVal != null && !rejectMalformedVal.isEmpty()) {
                this.rejectMalformed = Boolean.parseBoolean(rejectMalformedVal);
            } else {
                this.rejectMalformed = DEFAULT_REJECT_MALFORMED;
            }

            String patternDetectionVal = settings.get(PATTERN_DETECTION);
            if (patternDetectionVal != null && !patternDetectionVal.isEmpty()) {
                this.patternDetection = Boolean.parseBoolean(patternDetectionVal);
            } else {
                this.patternDetection = DEFAULT_PATTERN_DETECTION;
            }

            String patterns = settings.get(BLOCK_PATTERNS);
            setBlockPatterns(patterns);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public TokenGuard setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public TokenGuard setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public boolean isRejectMalformed() {
        return rejectMalformed;
    }

    public TokenGuard setRejectMalformed(boolean rejectMalformed) {
        this.rejectMalformed = rejectMalformed;
        return this;
    }

    public boolean isPatternDetection() {
        return patternDetection;
    }

    public TokenGuard setPatternDetection(boolean patternDetection) {
        this.patternDetection = patternDetection;
        return this;
    }

    public List<Pattern> getBlockPatterns() {
        return Collections.unmodifiableList(blockPatterns);
    }

    public TokenGuard setBlockPatterns(String commaSeparatedPatterns) {
        this.blockPatterns.clear();
        if (commaSeparatedPatterns != null && !commaSeparatedPatterns.trim().isEmpty()) {
            String[] patterns = commaSeparatedPatterns.split(",");
            for (String p : patterns) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        this.blockPatterns.add(Pattern.compile(trimmed));
                    } catch (PatternSyntaxException e) {
                        LOGGER.warning("Invalid regex in block pattern: " + trimmed);
                    }
                }
            }
        }
        return this;
    }

    /**
     * Validates a token as a Bearer token.
     *
     * @param token The token string to validate
     * @return ValidationResult indicating valid, malformed, oversized, or blocked
     */
    public ValidationResult validateBearerToken(String token) {
        return validate(token, TokenType.BEARER);
    }

    /**
     * Validates a token against the specified TokenType.
     *
     * @param token The token string to validate
     * @param type  The expected token type
     * @return ValidationResult
     */
    public ValidationResult validate(String token, TokenType type) {
        if (!this.enabled) {
            return ValidationResult.valid();
        }

        if (token == null) {
            return this.rejectMalformed
                    ? ValidationResult.malformed("Token must not be null")
                    : ValidationResult.valid();
        }

        // Bounded length check protects against resource exhaustion and ReDoS
        if (token.length() > this.maxLength) {
            return ValidationResult.oversized("Token exceeds maximum length of " + this.maxLength + " (length: " + token.length() + ")");
        }

        if (token.trim().isEmpty()) {
            return this.rejectMalformed
                    ? ValidationResult.malformed("Token must not be empty or whitespace only")
                    : ValidationResult.valid();
        }

        // Check configurable deny patterns on the length-bounded string
        if (this.patternDetection && !this.blockPatterns.isEmpty()) {
            for (Pattern pattern : this.blockPatterns) {
                try {
                    if (pattern.matcher(new BudgetedCharSequence(token, PATTERN_STEP_BUDGET)).find()) {
                        return ValidationResult.blocked("Token matches blocked pattern policy");
                    }
                } catch (PatternBudgetExceededException e) {
                    // Fail closed: a pattern that backtracks excessively must not become a DoS vector.
                    LOGGER.warning("Block pattern exceeded evaluation budget; token rejected");
                    return ValidationResult.blocked("Token could not be evaluated against blocked pattern policy");
                }
            }
        }

        if (!this.rejectMalformed) {
            return ValidationResult.valid();
        }

        // Check for illegal control characters or non-printable ASCII
        if (containsControlCharacters(token)) {
            return ValidationResult.malformed("Token contains illegal control or whitespace characters");
        }

        // Syntax validation according to TokenType
        switch (type) {
            case JWT:
                return validateJwtStructure(token);

            case BASE64:
                ValidationResult b64CharCheck = validateBase64Characters(token);
                if (!b64CharCheck.isValid()) {
                    return b64CharCheck;
                }
                return validateBase64Padding(token);

            case BEARER:
                // If it contains periods, it is formatted as a JWT
                if (token.indexOf('.') != -1) {
                    return validateJwtStructure(token);
                }
                // Otherwise validate as an opaque / Base64 token (RFC 6750 b64token)
                ValidationResult bearerCharCheck = validateTokenCharacters(token);
                if (!bearerCharCheck.isValid()) {
                    return bearerCharCheck;
                }
                return validateBase64Padding(token);

            case GENERIC:
            default:
                return validateTokenCharacters(token);
        }
    }

    /**
     * Checks if a string contains ASCII control characters (&lt; 0x20 or 0x7F) or non-ASCII characters (&gt; 0x7E).
     */
    public boolean containsControlCharacters(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20 || c == 0x7F || c > 0x7E) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates that token contains only valid printable characters allowed in RFC 6750 b64token.
     */
    private ValidationResult validateTokenCharacters(String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!isValidTokenChar(c)) {
                return ValidationResult.malformed("Token contains illegal character: '" + c + "' at position " + i);
            }
        }
        return ValidationResult.valid();
    }

    private static boolean isValidTokenChar(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9') ||
                c == '-' || c == '.' || c == '_' || c == '~' || c == '+' || c == '/' || c == '=';
    }

    /**
     * Validates that token contains only standard Base64 characters [A-Za-z0-9+/=].
     */
    private ValidationResult validateBase64Characters(String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!((c >= 'a' && c <= 'z') ||
                  (c >= 'A' && c <= 'Z') ||
                  (c >= '0' && c <= '9') ||
                  c == '+' || c == '/' || c == '=')) {
                return ValidationResult.malformed("Token contains invalid Base64 character: '" + c + "' at position " + i);
            }
        }
        return ValidationResult.valid();
    }

    /**
     * Validates Base64 padding semantics:
     * 1. '=' padding characters may only appear at the end.
     * 2. There can be at most two '=' characters.
     * 3. When '=' padding is present, total length must be a multiple of 4.
     */
    public ValidationResult validateBase64Padding(String token) {
        if (token == null || token.isEmpty()) {
            return ValidationResult.valid();
        }

        int firstEquals = token.indexOf('=');
        if (firstEquals == -1) {
            // No '=' present; valid for unpadded Base64/Base64URL.
            return ValidationResult.valid();
        }

        // Any character after the first '=' MUST also be '='
        for (int i = firstEquals; i < token.length(); i++) {
            if (token.charAt(i) != '=') {
                return ValidationResult.malformed("Invalid Base64 padding: '=' character found within data payload");
            }
        }

        // Count of '=' characters must be at most 2
        int equalsCount = token.length() - firstEquals;
        if (equalsCount > 2) {
            return ValidationResult.malformed("Invalid Base64 padding: more than two '=' padding characters found");
        }

        // When '=' padding is present, total length must be a multiple of 4
        if (token.length() % 4 != 0) {
            return ValidationResult.malformed("Invalid Base64 padding: padded token length must be a multiple of 4 (length: " + token.length() + ")");
        }

        return ValidationResult.valid();
    }

    /**
     * Validates compact signed JWT syntax (RFC 7515/7519):
     * Exactly 2 period separators dividing the string into 3 non-empty unpadded Base64URL parts.
     */
    public ValidationResult validateJwtStructure(String token) {
        if (token == null || token.isEmpty()) {
            return ValidationResult.malformed("JWT string must not be empty");
        }

        int firstDot = token.indexOf('.');
        if (firstDot == -1) {
            return ValidationResult.malformed("Malformed JWT format: missing period separator");
        }
        int secondDot = token.indexOf('.', firstDot + 1);
        if (secondDot == -1) {
            return ValidationResult.malformed("Malformed JWT format: expected 2 period separators, found 1");
        }
        if (token.indexOf('.', secondDot + 1) != -1) {
            return ValidationResult.malformed("Malformed JWT format: expected 2 period separators, found more than 2");
        }

        String header = token.substring(0, firstDot);
        String payload = token.substring(firstDot + 1, secondDot);
        String signature = token.substring(secondDot + 1);

        if (header.isEmpty() || payload.isEmpty() || signature.isEmpty()) {
            return ValidationResult.malformed("Malformed JWT format: segments must not be empty");
        }

        if (!isValidBase64UrlSegment(header)) {
            return ValidationResult.malformed("Malformed JWT format: header contains invalid unpadded Base64URL characters");
        }
        if (!isValidBase64UrlSegment(payload)) {
            return ValidationResult.malformed("Malformed JWT format: payload contains invalid unpadded Base64URL characters");
        }
        if (!isValidBase64UrlSegment(signature)) {
            return ValidationResult.malformed("Malformed JWT format: signature contains invalid unpadded Base64URL characters");
        }

        return ValidationResult.valid();
    }

    private static boolean isValidBase64UrlSegment(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= 'a' && c <= 'z') ||
                  (c >= 'A' && c <= 'Z') ||
                  (c >= '0' && c <= '9') ||
                  c == '-' || c == '_')) {
                return false;
            }
        }
        return true;
    }

    /**
     * Masks a token to prevent sensitive data leakage in logs.
     *
     * @param token The token string
     * @return Masked string suitable for logging
     */
    public static String mask(String token) {
        if (token == null) {
            return "[null]";
        }
        int len = token.length();
        if (len == 0) {
            return "[empty]";
        }
        if (len <= 8) {
            return "[masked, len=" + len + "]";
        }
        return token.substring(0, 3) + "..." + token.substring(len - 3) + " [len=" + len + "]";
    }
}
