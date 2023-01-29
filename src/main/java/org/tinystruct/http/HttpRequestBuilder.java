package org.tinystruct.http;

import org.tinystruct.ApplicationException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestBuilder {
    private final Map<String, Object> parameters = new HashMap<>();
    private Version version;
    private Headers headers;
    private Method method = Method.GET;
    private String uri;
    private String requestBody;

    /**
     * Returns the protocol version of this {@link Protocol}
     * @return the protocol version
     */
    public Version version() {
        return this.version;
    }

    /**
     * Set the protocol version of this {@link Protocol}
     *
     * @param version version of this protocol
     * @return this builder
     */
    public HttpRequestBuilder setVersion(Version version) {
        this.version = version;
        return this;
    }

    /**
     * Returns the headers of this message.
     * @return the headers of this message
     */
    public Headers headers() {
        return this.headers;
    }

    public HttpRequestBuilder setHeaders(Headers headers) {
        this.headers = headers;
        return this;
    }

    /**
     * Returns the {@link Method} of this {@link Request}.
     *
     * @return The {@link Method} of this {@link Request}
     */
    public Method method() {
        return this.method;
    }

    /**
     * Set the {@link Method} of this {@link Request}.
     *
     * @param method method of this request
     * @return this builder
     */
    public HttpRequestBuilder setMethod(Method method) {
        this.method = method;
        return this;
    }

    /**
     * Returns the request URI (or alternatively, path)
     *
     * @return The URI to be requested
     */
    public String uri() {
        return this.uri;
    }

    /**
     * Set the request URI (or alternatively, path)
     *
     * @param uri URI
     * @return this builder
     */
    public HttpRequestBuilder setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public Map<String, Object> parameters() {
        return this.parameters;
    }

    public HttpRequestBuilder setParameter(String name, Object value) {
        this.parameters.put(name, value);
        return this;
    }

    public HttpRequestBuilder attach(File[] files) throws ApplicationException {
        return this;
    }

    public String requestBody() {
        return this.requestBody;
    }

    public HttpRequestBuilder setRequestBody(String requestBody) {
        this.requestBody = requestBody;
        return this;
    }
}
