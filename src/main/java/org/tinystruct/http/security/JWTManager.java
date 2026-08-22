package org.tinystruct.http.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.tinystruct.data.component.Builder;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;

public class JWTManager {
    // A default secret key for signing JWTs
    private static final SecretKey SECRET_KEY = Jwts.SIG.HS256.key().build();
    private Key signKey;
    private Key verifyKey;
    private long clockSkew = 0;

    /**
     * Sets the secret key using a plain text string.
     * This method allows for a custom secret key to be used instead of the default
     * one.
     *
     * @param secret The secret key as a plain text string
     */
    public void withSecret(String secret) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getEncoder().encode(secret.getBytes()));
        this.signKey = key;
        this.verifyKey = key;
    }

    /**
     * Sets the secret key using a base64 encoded string.
     * This method allows for a custom secret key to be used instead of the default
     * one.
     *
     * @param base64Secret The secret key as a base64 string
     */
    public void withBase64Secret(String base64Secret) {
        SecretKey key = Keys.hmacShaKeyFor(base64Secret.getBytes());
        this.signKey = key;
        this.verifyKey = key;
    }

    /**
     * Sets the private key for signing tokens from a base64 encoded PKCS8 string.
     *
     * @param base64PrivateKey The private key as a base64 string
     * @throws IllegalArgumentException if the key cannot be parsed
     */
    public void withPrivateKey(String base64PrivateKey) {
        try {
            byte[] encoded = Base64.getDecoder().decode(base64PrivateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.signKey = kf.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid private key.", e);
        }
    }

    /**
     * Sets the public key for verifying tokens from a base64 encoded X.509 string.
     *
     * @param base64PublicKey The public key as a base64 string
     * @throws IllegalArgumentException if the key cannot be parsed
     */
    public void withPublicKey(String base64PublicKey) {
        try {
            byte[] encoded = Base64.getDecoder().decode(base64PublicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.verifyKey = kf.generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid public key.", e);
        }
    }

    /**
     * Sets the timezone and calculates the clock skew in seconds.
     * This method allows for a leeway in the token's expiration time to account for
     * clock differences between machines.
     *
     * @param zoneId The timezone ID
     */
    public void withTimezone(ZoneId zoneId) {
        this.clockSkew = Duration.between(
                ZonedDateTime.now(zoneId).toLocalDateTime(),
                ZonedDateTime.now(ZoneId.systemDefault()).toLocalDateTime()).getSeconds();
    }

    /**
     * Sets the timezone and calculates the clock skew in seconds.
     * This method allows for a leeway in the token's expiration time to account for
     * clock differences between machines.
     *
     * @param timezone The timezone string
     */
    public void withTimezone(String timezone) {
        this.withTimezone(ZoneId.of(timezone));
    }

    /**
     * Builds a JWT with the given subject and claims and returns it as a JWS signed
     * compact String.
     *
     * @param subject  The subject (typically the user identifier)
     * @param builder  A Builder object containing the claims to be included in the
     *                 JWT
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

        // Sign the JWT with the provided secret key, or the default key if none
        // provided
        if (this.signKey != null) {
            if (this.signKey instanceof SecretKey) {
                jwtBuilder.signWith(this.signKey);
            } else if (this.signKey instanceof java.security.PrivateKey) {
                jwtBuilder.signWith(this.signKey);
            }
        } else {
            jwtBuilder.signWith(SECRET_KEY);
        }

        // Build and return the compact JWS string
        return jwtBuilder.compact();
    }

    /**
     * Parses the given JWS signed compact JWT, returning the claims.
     * If this method returns without throwing an exception, the token can be
     * trusted.
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
        if (this.verifyKey != null) {
            if (this.verifyKey instanceof SecretKey) {
                parser.verifyWith((SecretKey) this.verifyKey);
            } else if (this.verifyKey instanceof java.security.PublicKey) {
                parser.verifyWith((java.security.PublicKey) this.verifyKey);
            }
        } else {
            parser.verifyWith(SECRET_KEY);
        }

        parser.clockSkewSeconds(this.clockSkew);

        // Parse the JWT and return the claims
        return parser.build().parseSignedClaims(compactToken);
    }
}