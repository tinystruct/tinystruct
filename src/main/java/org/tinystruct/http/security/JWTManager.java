package org.tinystruct.http.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.tinystruct.data.component.Builder;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

public class JWTManager {
    // A default secret key for signing JWTs
    private static final SecretKey SECRET_KEY = Jwts.SIG.HS256.key().build();
    private SecretKey base64Key;

    /**
     * Sets the secret key using a base64 encoded string.
     * This method allows for a custom secret key to be used instead of the default one.
     *
     * @param secret The secret key as a plain text string
     */
    public void withSecret(String secret) {
        this.base64Key = Keys.hmacShaKeyFor(Base64.getEncoder().encode(secret.getBytes()));
    }

    /**
     * Builds a JWT with the given subject and claims and returns it as a JWS signed compact String.
     *
     * @param subject The subject (typically the user identifier)
     * @param builder A Builder object containing the claims to be included in the JWT
     * @param validity The validity period of the token in hours
     * @return The generated JWT as a compact JWS signed String
     */
    public String createToken(final String subject, final Builder builder, final long validity) {
        final Instant now = Instant.now();
        final Date expiryDate = Date.from(now.plus(Duration.ofHours(validity)));

        // Initialize the JWT builder with the subject, issued at, and expiration time
        JwtBuilder jwtBuilder = Jwts.builder().subject(subject).expiration(expiryDate).issuedAt(Date.from(now));

        // Add the claims from the builder to the JWT
        builder.forEach(jwtBuilder::claim);

        // Sign the JWT with the provided secret key, or the default key if none provided
        if (this.base64Key != null)
            jwtBuilder.signWith(this.base64Key);
        else
            jwtBuilder.signWith(SECRET_KEY);

        // Build and return the compact JWS string
        return jwtBuilder.compact();
    }

    /**
     * Parses the given JWS signed compact JWT, returning the claims.
     * If this method returns without throwing an exception, the token can be trusted.
     *
     * @param compactToken The JWT to be parsed
     * @return The claims contained in the JWT
     * @throws ExpiredJwtException      If the token is expired
     * @throws UnsupportedJwtException  If the token is unsupported
     * @throws MalformedJwtException    If the token is malformed
     * @throws IllegalArgumentException If the token is invalid
     */
    public Jws<Claims> parseToken(final String compactToken)
            throws ExpiredJwtException,
            UnsupportedJwtException,
            MalformedJwtException,
            IllegalArgumentException {

        // Initialize the JWT parser
        JwtParserBuilder parser = Jwts.parser();

        // Set the signing key for the parser
        if (this.base64Key != null)
            parser.verifyWith(this.base64Key);
        else
            parser.verifyWith(SECRET_KEY);

        // Parse the JWT and return the claims
        return parser.build().parseSignedClaims(compactToken);
    }
}