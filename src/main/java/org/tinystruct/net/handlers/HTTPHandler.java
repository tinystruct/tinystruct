package org.tinystruct.net.handlers;

import org.brotli.dec.BrotliInputStream;
import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.Attachments;
import org.tinystruct.net.URLHandler;
import org.tinystruct.net.URLRequest;
import org.tinystruct.net.URLResponse;
import org.tinystruct.transfer.http.upload.ContentDisposition;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import static org.tinystruct.transfer.http.upload.ContentDisposition.LINE;

public class HTTPHandler implements URLHandler {

    private Boolean followRedirects = true;

    @Override
    public URLResponse handleRequest(URLRequest request) throws ApplicationException {
        HttpClient client = buildClient(request);
        try {
            HttpResponse<InputStream> response;
            HttpRequest httpRequest = buildHttpRequest(request);
            response = client.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());
            // Auto-detect SSE by Content-Type
            String contentType = request.getHeaders().get("Content-Type");
            if (contentType != null && contentType.contains("text/event-stream")) {
                return new HTTPResponse(response, System.out::println);
            }

            return new HTTPResponse(response);
        } catch (IOException e) {
            if (e instanceof ConnectException) {
                throw new ApplicationException(e.getClass().getSimpleName() + ":Connection Error", e);
            }
            throw new ApplicationException(e.getClass().getSimpleName() + ":" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApplicationException(e.getClass().getSimpleName() + ":" + e.getMessage(), e);
        }
    }

    @Override
    public URLResponse handleRequest(URLRequest request, Consumer<String> consumer) throws ApplicationException {
        HttpClient client = buildClient(request);
        try {
            HttpResponse<InputStream> response;
            HttpRequest httpRequest = buildHttpRequest(request);
            response = client.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());
            // The caller explicitly provided a consumer, so always use the streaming path.
            // Note: java.net.http.HttpClient transparently handles chunked transfer
            // encoding
            // and does not expose the Transfer-Encoding header in the response.
            return new HTTPResponse(response, consumer);
        } catch (IOException e) {
            throw new ApplicationException(e.getClass().getSimpleName() + ":" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApplicationException(e.getClass().getSimpleName() + ":" + e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<URLResponse> handleRequestAsync(URLRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return handleRequest(request);
            } catch (ApplicationException e) {
                throw new CompletionException(e);
            }
        });
    }

    public void disableFollowRedirects() {
        this.followRedirects = false;
    }

    /**
     * Builds an HttpClient with proxy and redirect settings derived from the
     * request.
     */
    private HttpClient buildClient(URLRequest request) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30));

        // Follow redirects
        builder.followRedirects(this.followRedirects
                ? HttpClient.Redirect.NORMAL
                : HttpClient.Redirect.NEVER);

        // Proxy: use the one set in the request or autodetect from system properties
        Proxy proxy = request.getProxy();
        if (proxy == null) {
            if (System.getProperty("https.proxyHost") != null && System.getProperty("https.proxyPort") != null) {
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                        System.getProperty("https.proxyHost"),
                        Integer.parseInt(System.getProperty("https.proxyPort"))));
            } else if (System.getProperty("http.proxyHost") != null && System.getProperty("http.proxyPort") != null) {
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                        System.getProperty("http.proxyHost"),
                        Integer.parseInt(System.getProperty("http.proxyPort"))));
            }
        }

        if (proxy != null) {
            InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
            builder.proxy(ProxySelector.of(proxyAddress));
        }

        return builder.build();
    }

    /**
     * Builds an HttpRequest from the URLRequest, handling query parameters,
     * multipart/form-data, URL-encoded forms, and plain body content.
     */
    private HttpRequest buildHttpRequest(URLRequest request) throws IOException {
        String boundary = null;
        URI effectiveUri;

        // Determine if we need to use multipart form-data
        String contentType = request.getHeaders().get("Content-Type");
        if (contentType != null && contentType.equalsIgnoreCase("multipart/form-data")) {
            boundary = UUID.randomUUID().toString();
            effectiveUri = URI.create(request.getURL().toString());
        } else {
            // Append query parameters if available
            if (request.getParameters() != null && !request.getParameters().isEmpty()) {
                String parameters = buildQuery(request.getParameters());
                String urlStr = request.getURL().toString();
                effectiveUri = URI.create(urlStr.contains("?") ? urlStr + "&" + parameters : urlStr + "?" + parameters);
            } else {
                effectiveUri = URI.create(request.getURL().toString());
            }
        }

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(effectiveUri);

        // Set headers
        if (request.getHeaders() != null) {
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                String key = header.getKey();
                // Skip restricted headers that HttpClient manages internally
                if ("Content-Length".equalsIgnoreCase(key) || "Host".equalsIgnoreCase(key)) {
                    continue;
                }
                reqBuilder.header(key, header.getValue());
            }
        }

        // Build the request body
        HttpRequest.BodyPublisher bodyPublisher = buildBodyPublisher(request, boundary, contentType);

        if (boundary != null) {
            // Override Content-Type with the boundary-included version
            reqBuilder.header("Content-Type", "multipart/form-data; boundary=" + boundary);
        }

        // Set the method with the body publisher — HttpClient natively supports PATCH
        reqBuilder.method(request.getMethod(), bodyPublisher);

        return reqBuilder.build();
    }

    /**
     * Constructs the appropriate BodyPublisher based on the request content.
     */
    private HttpRequest.BodyPublisher buildBodyPublisher(URLRequest request, String boundary, String contentType)
            throws IOException {
        boolean hasBody = request.getBody() != null;
        boolean hasMultipart = boundary != null;
        boolean hasFormData = request.getFormData() != null && request.getFormData().length > 0;
        boolean hasAttachments = request.getAttachments() != null;
        boolean hasUrlEncodedForm = "application/x-www-form-urlencoded".equalsIgnoreCase(contentType)
                && request.getParameters() != null && !request.getParameters().isEmpty();

        if (!hasBody && !hasMultipart && !hasFormData && !hasAttachments && !hasUrlEncodedForm) {
            return HttpRequest.BodyPublishers.noBody();
        }

        if (hasMultipart) {
            return buildMultipartBody(request, boundary);
        } else if (hasUrlEncodedForm) {
            String formBody = buildQuery(request.getParameters());
            return HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8);
        } else if (hasBody) {
            return HttpRequest.BodyPublishers.ofString(request.getBody(), StandardCharsets.UTF_8);
        }

        return HttpRequest.BodyPublishers.noBody();
    }

    /**
     * Builds a multipart/form-data body publisher.
     */
    private HttpRequest.BodyPublisher buildMultipartBody(URLRequest request, String boundary) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Write parameters as multipart parts
        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            for (Map.Entry<String, Object> entry : request.getParameters().entrySet()) {
                ContentDisposition cd = new ContentDisposition(
                        entry.getKey(),
                        null,
                        "text/plain",
                        entry.getValue().toString().getBytes(StandardCharsets.UTF_8));
                baos.write(("--" + boundary + LINE).getBytes(StandardCharsets.UTF_8));
                baos.write(cd.getTransferBytes());
            }
        }

        // Write explicit form-data parts if available
        if (request.getFormData() != null && request.getFormData().length > 0) {
            for (ContentDisposition cd : request.getFormData()) {
                baos.write(("--" + boundary + LINE).getBytes(StandardCharsets.UTF_8));
                baos.write(cd.getTransferBytes());
            }
        } else if (request.getAttachments() != null) {
            // Write attachments if available
            Attachments attachments = request.getAttachments();
            for (var attachment : attachments.list()) {
                ContentDisposition cd = new ContentDisposition(
                        attachments.getParameterName(),
                        attachment.getFilename(),
                        "binary",
                        attachment.get());
                baos.write(("--" + boundary + LINE).getBytes(StandardCharsets.UTF_8));
                baos.write(cd.getTransferBytes());
            }
        }

        // Write closing boundary
        baos.write(("--" + boundary + "--" + LINE).getBytes(StandardCharsets.UTF_8));

        return HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray());
    }

    // Helper: Build query string from parameters.
    private String buildQuery(Map<String, Object> parameters) {
        StringBuilder queryBuilder = new StringBuilder();
        parameters.forEach((key, value) -> {
            if (!queryBuilder.isEmpty()) {
                queryBuilder.append("&");
            }
            queryBuilder.append(key)
                    .append("=")
                    .append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
        });
        return queryBuilder.toString();
    }

}

class HTTPResponse implements URLResponse {
    private final int statusCode;
    private final String body;
    private final Map<String, List<String>> headers;

    /**
     * Constructor for a standard HTTP response.
     */
    public HTTPResponse(HttpResponse<InputStream> response) {
        this.statusCode = response.statusCode();
        this.headers = response.headers().map();

        try (InputStream in = response.body()) {
            if (in != null && in.available() > 0) {
                String contentEncoding = response.headers()
                        .firstValue("Content-Encoding").orElse(null);
                InputStream decodedStream = getDecodedInputStream(contentEncoding, in);
                this.body = new String(decodedStream.readAllBytes(), StandardCharsets.UTF_8);
                decodedStream.close();
            } else {
                this.body = null;
            }
        } catch (IOException e) {
            throw new ApplicationRuntimeException(e);
        }
    }

    /**
     * Constructor for SSE / chunked streaming.
     */
    public HTTPResponse(HttpResponse<InputStream> response, Consumer<String> onMessage) {
        this.statusCode = response.statusCode();
        this.headers = response.headers().map();
        this.body = null; // SSE mode does not return the entire body content.

        handleSSE(response, onMessage);
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Returns a decoded InputStream if the content encoding is supported.
     */
    private static InputStream getDecodedInputStream(String contentEncoding, InputStream in) throws IOException {
        if (contentEncoding != null) {
            switch (contentEncoding.toLowerCase()) {
                case "gzip":
                    return new GZIPInputStream(in);
                case "deflate":
                    return new DeflaterInputStream(in);
                case "br":
                    return new BrotliInputStream(in);
                default:
                    break;
            }
        }
        return in;
    }

    private void handleSSE(HttpResponse<InputStream> response, Consumer<String> listener) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                listener.accept(line + "\n");
            }
        } catch (IOException e) {
            throw new ApplicationRuntimeException(e);
        }
    }
}