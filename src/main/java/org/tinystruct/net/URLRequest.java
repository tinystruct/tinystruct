package org.tinystruct.net;

import org.tinystruct.data.Attachments;
import org.tinystruct.transfer.http.upload.ContentDisposition;

import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class URLRequest {
    private URL url;
    private String method = "GET"; // default
    private Map<String, String> headers = new HashMap<>();
    private Map<String, Object> parameters = new HashMap<>(); // Query/form parameters
    private String body; // for simple request body
    private ContentDisposition[] formData; // for multipart form-data parts
    private Attachments attachments; // for file attachments
    private Proxy proxy; // optional proxy

    public URLRequest(URL url) {
        this.url = url;
    }

    // URL getter/setter
    public URL getUrl() {
        return url;
    }

    public URLRequest setUrl(URL url) {
        this.url = url;
        return this;
    }

    // Method getter/setter
    public String getMethod() {
        return method;
    }

    public URLRequest setMethod(String method) {
        this.method = method;
        return this;
    }

    // Headers
    public Map<String, String> getHeaders() {
        return headers;
    }

    public URLRequest setHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    // Parameters
    public Map<String, Object> getParameters() {
        return parameters;
    }

    public URLRequest setParameter(String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }

    // Body
    public String getBody() {
        return body;
    }

    public URLRequest setBody(String body) {
        this.body = body;
        return this;
    }

    // Form Data
    public ContentDisposition[] getFormData() {
        return formData;
    }

    public URLRequest setFormData(ContentDisposition[] formData) {
        this.formData = formData;
        return this;
    }

    // Attachments
    public Attachments getAttachments() {
        return attachments;
    }

    public URLRequest setAttachments(Attachments attachments) {
        this.attachments = attachments;
        return this;
    }

    // Proxy
    public Proxy getProxy() {
        return proxy;
    }

    public URLRequest setProxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }
}
