package org.tinystruct.http.security;

import io.jsonwebtoken.io.AbstractSerializer;
import org.tinystruct.data.component.Builder;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * JWTSerializer provides JJWT JSON serialization support
 * using the tinystruct {@link Builder}.
 */
public class JWTSerializer extends AbstractSerializer<Map<String, ?>> {

    @Override
    protected void doSerialize(Map<String, ?> map, OutputStream out) throws Exception {
        Builder builder = new Builder();
        builder.putAll(map);
        out.write(builder.toString().getBytes(StandardCharsets.UTF_8));
    }
}
