package org.tinystruct.http.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DeserializationException;
import io.jsonwebtoken.io.SerializationException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JWTCodecTest {

    private final JWTSerializer serializer = new JWTSerializer();
    private final JWTDeserializer deserializer = new JWTDeserializer();

    @Test
    void testSerializeAndDeserializeMap() throws SerializationException, DeserializationException {
        Map<String, Object> original = new HashMap<>();
        original.put("user", "catcat");
        original.put("role", "admin");
        original.put("count", 123);

        // Serialize the map to bytes
        byte[] jsonBytes = serializer.serialize(original);
        assertNotNull(jsonBytes, "Serialized bytes should not be null");

        // Deserialize back to map
        Map<String, ?> deserialized = deserializer.deserialize(jsonBytes);
        assertNotNull(deserialized, "Deserialized map should not be null");

        // Verify values match
        assertEquals(original.get("user"), deserialized.get("user"));
        assertEquals(original.get("role"), deserialized.get("role"));
        assertEquals(original.get("count"), deserialized.get("count"));
    }

    @Test
    void testSerializeToOutputStreamAndDeserializeFromReader() throws Exception {
        Map<String, Object> original = new HashMap<>();
        original.put("framework", "tinystruct");
        original.put("version", "1.7.8");

        // Serialize to OutputStream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(original, out);
        byte[] bytes = out.toByteArray();
        assertTrue(bytes.length > 0, "Output stream should contain data");

        // Deserialize using Reader
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        Map<String, ?> result = deserializer.deserialize(new InputStreamReader(in, StandardCharsets.UTF_8));
        assertEquals(original, result, "Deserialized map should match original");
    }

    @Test
    void testJWTIntegrationWithCodec() {
        SecretKey key = Keys.hmacShaKeyFor("MySecretKey123456789012345678901234567890".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> claims = Map.of("user", "catcat", "role", "admin");

        // Encode JWT manually using serializer
        String token = Jwts.builder().json(serializer)
                .signWith(key)
                .claims(claims)
                .compact();
        assertNotNull(token, "JWT token should not be null");

        // Decode JWT using deserializer
        Map<String, Object> decodedClaims = Jwts.parser().json(deserializer)
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(claims, decodedClaims, "Decoded claims should match original claims");
    }
}
