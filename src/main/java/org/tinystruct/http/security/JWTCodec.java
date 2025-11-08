package org.tinystruct.http.security;

import io.jsonwebtoken.io.DeserializationException;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.io.SerializationException;
import io.jsonwebtoken.io.Serializer;
import org.tinystruct.data.component.Builder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * JWTCodec provides JJWT JSON serialization/deserialization support
 * using the tinystruct Builder to parse and construct JSON structures.
 */
public class JWTCodec implements Serializer<Map<String, ?>>, Deserializer<Map<String, ?>> {

    /**
     * Deserialize from a byte array into a Map using Builder.
     */
    @Override
    public Map<String, ?> deserialize(byte[] bytes) throws DeserializationException {
        try {
            String json = new String(bytes, StandardCharsets.UTF_8);
            Builder builder = new Builder();
            builder.parse(json); // Parse JSON string into builder structure
            return builder;
        } catch (Exception e) {
            throw new DeserializationException("Failed to deserialize JWT payload using Builder", e);
        }
    }

    /**
     * Deserialize from a Reader into a Map using Builder.
     */
    @Override
    public Map<String, ?> deserialize(Reader reader) throws DeserializationException {
        try {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }

            Builder builder = new Builder();
            builder.parse(sb.toString());
            return builder;
        } catch (IOException e) {
            throw new DeserializationException("Error reading JSON input stream", e);
        } catch (Exception e) {
            throw new DeserializationException("Failed to deserialize JWT payload using Builder", e);
        }
    }

    /**
     * Serialize a Map into a byte array as JSON using Builder.
     */
    @Override
    public byte[] serialize(Map<String, ?> map) throws SerializationException {
        try {
            Builder builder = new Builder();
            builder.putAll(map);
            String json = builder.toString(); // Builder should produce JSON-like string
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize JWT payload using Builder", e);
        }
    }

    /**
     * Serialize a Map to an OutputStream as JSON using Builder.
     */
    @Override
    public void serialize(Map<String, ?> map, OutputStream out) throws SerializationException {
        try {
            Builder builder = new Builder();
            builder.putAll(map);
            String json = builder.toString();
            out.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SerializationException("Error writing JWT payload to output stream", e);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize JWT payload using Builder", e);
        }
    }
}
