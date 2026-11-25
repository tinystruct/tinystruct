package org.tinystruct.http.security;

import io.jsonwebtoken.io.AbstractDeserializer;
import org.tinystruct.data.component.Builder;

import java.io.Reader;
import java.util.Map;

/**
 * JWTDeserializer provides JJWT JSON deserialization support
 * using the tinystruct {@link Builder}.
 */
public class JWTDeserializer extends AbstractDeserializer<Map<String, ?>> {

    @Override
    protected Map<String, ?> doDeserialize(Reader reader) throws Exception {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[1024];
        int len;
        while ((len = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, len);
        }

        Builder builder = new Builder();
        builder.parse(sb.toString());
        return builder;
    }
}
