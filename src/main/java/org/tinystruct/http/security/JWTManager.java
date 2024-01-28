package org.tinystruct.http.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.crypto.MacProvider;
import io.jsonwebtoken.security.Keys;
import org.tinystruct.data.component.Builder;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

public class JWTManager {

    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;
    private static final SecretKey SECRET_KEY = MacProvider.generateKey(SIGNATURE_ALGORITHM);
    private byte[] base64Key;

    public JWTManager withSecret(String secret) {
        this.base64Key = Base64.getEncoder().encode(secret.getBytes());
        return this;
    }

    /**
     * Builds a JWT with the given subject and claims and returns it as a JWS signed compact String.
     *
     * @param subject subject
     * @param builder Claims
     * @return token
     */
    public String createToken(final String subject, final Builder builder, final long validity) {
        final Instant now = Instant.now();
        final Date expiryDate = Date.from(now.plus(Duration.ofHours(validity)));

        JwtBuilder jwts = Jwts.builder()
                .setSubject(subject)
                .setExpiration(expiryDate)
                .setIssuedAt(Date.from(now));

        builder.forEach(jwts::claim);

        if (this.base64Key != null)
            jwts.signWith(Keys.hmacShaKeyFor(base64Key), SIGNATURE_ALGORITHM);
        else
            jwts.signWith(SECRET_KEY, SIGNATURE_ALGORITHM);

        return jwts.compact();
    }

    /**
     * Parses the given JWS signed compact JWT, returning the claims.
     * If this method returns without throwing an exception, the token can be trusted.
     *
     * @param compactToken token
     * @return claims
     * @throws ExpiredJwtException      expired exception
     * @throws UnsupportedJwtException  unsupported exception
     * @throws MalformedJwtException    malformed exception
     * @throws IllegalArgumentException illegal arguments exception
     */
    public Jws<Claims> parseToken(final String compactToken)
            throws ExpiredJwtException,
            UnsupportedJwtException,
            MalformedJwtException,
            IllegalArgumentException {

        JwtParserBuilder parser = Jwts.parserBuilder();
        if (this.base64Key != null)
            parser.setSigningKey(this.base64Key);
        else
            parser.setSigningKey(SECRET_KEY);

        return parser.build().parseClaimsJws(compactToken);
    }
}