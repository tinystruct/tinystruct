package org.tinystruct.http.client;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.Attachment;
import org.tinystruct.data.Attachments;
import org.tinystruct.http.*;
import org.tinystruct.transfer.http.upload.ContentDisposition;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequestBuilder {
    private final Map<String, Object> parameters = new HashMap<>();
    private Attachments attachments;

    private Version version;
    private Headers headers;
    private Method method = Method.GET;
    private String uri;
    private String requestBody;
    private ContentDisposition[] formData;

    /**
     * Returns the protocol version of this {@link Protocol}
     *
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
     *
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

    /**
     * It's more efficient to use this method instead of {@link #attach(String, File[])}
     * to forward the content to the destination.
     *
     * @param formData form data
     * @return this builder
     */
    public HttpRequestBuilder setFormData(ContentDisposition[] formData) {
        this.formData = formData;
        return this;
    }

    public ContentDisposition[] getFormData() {
        return this.formData;
    }

    public HttpRequestBuilder attach(String parameter, File[] files) throws ApplicationException {
        List<Attachment> list = new ArrayList<Attachment>();
        for (File file : files) {
            Attachment attachment = new Attachment();
            attachment.setFilename(file.getName());
            try {
                attachment.setContent(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
            } catch (IOException e) {
                throw new ApplicationException("Could not read file: " + file.getAbsolutePath());
            }

            list.add(attachment);
        }
        this.attachments = new Attachments(parameter, list);
        return this;
    }

    public Attachments getAttachments() {
        return this.attachments;
    }

    public String requestBody() {
        return this.requestBody;
    }

    public HttpRequestBuilder setRequestBody(String requestBody) {
        this.requestBody = requestBody;
        return this;
    }
}
