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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        try {
            String boundary = null;
            URL effectiveUrl;

            // Determine if we need to use multipart form-data
            String contentType = request.getHeaders().get("Content-Type");
            if (contentType != null && contentType.equalsIgnoreCase("multipart/form-data")) {
                boundary = UUID.randomUUID().toString();
                effectiveUrl = request.getURL();
            } else {
                // Append query parameters if available
                if (request.getParameters() != null && !request.getParameters().isEmpty()) {
                    String parameters = buildQuery(request.getParameters());
                    String urlStr = request.getURL().toString();
                    effectiveUrl = URI.create(urlStr.contains("?") ? urlStr + "&" + parameters : urlStr + "?" + parameters).toURL();
                } else {
                    effectiveUrl = request.getURL();
                }
            }

            // Proxy: use the one set in the request or autodetect from system properties
            Proxy proxy = request.getProxy();
            if (proxy == null) {
                if (System.getProperty("https.proxyHost") != null && System.getProperty("https.proxyPort") != null) {
                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                            System.getProperty("https.proxyHost"),
                            Integer.parseInt(System.getProperty("https.proxyPort"))
                    ));
                } else if (System.getProperty("http.proxyHost") != null && System.getProperty("http.proxyPort") != null) {
                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                            System.getProperty("http.proxyHost"),
                            Integer.parseInt(System.getProperty("http.proxyPort"))
                    ));
                }
            }

            // Open connection using proxy if available
            HttpURLConnection connection = (proxy != null)
                    ? (HttpURLConnection) effectiveUrl.openConnection(proxy)
                    : (HttpURLConnection) effectiveUrl.openConnection();

            connection.setRequestMethod(request.getMethod());

            // Disable follow-redirects option if user disables it
            if (!this.followRedirects) {
                connection.setInstanceFollowRedirects(false);
            }

            // Set headers
            if (request.getHeaders() != null) {
                for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }

            // Enable output if there's a body or multipart data
            if (request.getBody() != null || boundary != null ||
                    (request.getFormData() != null && request.getFormData().length > 0) ||
                    (request.getAttachments() != null) ||
                    ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType) && request.getParameters() != null && !request.getParameters().isEmpty())) {
                connection.setDoOutput(true);

                if (boundary != null) {
                    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                }

                try (OutputStream writer = connection.getOutputStream()) {
                    // Write multipart parts if needed
                    if (boundary != null) {
                        // Write parameters as multipart parts
                        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
                            final String finalBoundary = boundary;
                            for (Map.Entry<String, Object> entry : request.getParameters().entrySet()) {
                                ContentDisposition cd = new ContentDisposition(
                                        entry.getKey(),
                                        null,
                                        "text/plain",
                                        entry.getValue().toString().getBytes(StandardCharsets.UTF_8)
                                );
                                writer.write(("--" + finalBoundary + LINE).getBytes(StandardCharsets.UTF_8));
                                writer.write(cd.getTransferBytes());
                            }
                            writer.flush();
                        }

                        // Write explicit form-data parts if available
                        if (request.getFormData() != null && request.getFormData().length > 0) {
                            for (ContentDisposition cd : request.getFormData()) {
                                writer.write(("--" + boundary + LINE).getBytes(StandardCharsets.UTF_8));
                                writer.write(cd.getTransferBytes());
                                writer.flush();
                            }
                        } else if (request.getAttachments() != null) {
                            // Write attachments if available
                            Attachments attachments = request.getAttachments();
                            for (var attachment : attachments.list()) {
                                ContentDisposition cd = new ContentDisposition(
                                        attachments.getParameterName(),
                                        attachment.getFilename(),
                                        "binary",
                                        attachment.get()
                                );
                                writer.write(("--" + boundary + LINE).getBytes(StandardCharsets.UTF_8));
                                writer.write(cd.getTransferBytes());
                                writer.flush();
                            }
                        }
                        // Write closing boundary
                        writer.write(("--" + boundary + "--" + LINE).getBytes(StandardCharsets.UTF_8));
                    } else if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
                        // URL-encoded body
                        String formBody = buildQuery(request.getParameters());
                        writer.write(formBody.getBytes(StandardCharsets.UTF_8));
                    } else if (request.getBody() != null) {
                        writer.write(request.getBody().getBytes(StandardCharsets.UTF_8));
                    }
                    writer.flush();
                }
            }

            connection.connect();

            // Auto-detect SSE by Content-Type
            if (contentType != null && contentType.contains("text/event-stream")) {
                return new HTTPResponse(connection, System.out::println);
            }

            // Default response for normal HTTP request
            return new HTTPResponse(connection);
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    public URLResponse handleRequest(URLRequest request, Consumer<String> consumer) throws ApplicationException {
        try {
            String boundary = null;
            URL effectiveUrl;

            // Determine if we need to use multipart form-data
            String contentType = request.getHeaders().get("Content-Type");
            if (contentType != null && contentType.equalsIgnoreCase("multipart/form-data")) {
                boundary = UUID.randomUUID().toString();
                effectiveUrl = request.getURL();
            } else {
                // Append query parameters if available
                if (request.getParameters() != null && !request.getParameters().isEmpty()) {
                    String parameters = buildQuery(request.getParameters());
                    String urlStr = request.getURL().toString();
                    effectiveUrl = URI.create(urlStr.contains("?") ? urlStr + "&" + parameters : urlStr + "?" + parameters).toURL();
                } else {
                    effectiveUrl = request.getURL();
                }
            }

            // Proxy: use the one set in the request or autodetect from system properties
            Proxy proxy = request.getProxy();
            if (proxy == null) {
                if (System.getProperty("https.proxyHost") != null && System.getProperty("https.proxyPort") != null) {
                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                            System.getProperty("https.proxyHost"),
                            Integer.parseInt(System.getProperty("https.proxyPort"))
                    ));
                } else if (System.getProperty("http.proxyHost") != null && System.getProperty("http.proxyPort") != null) {
                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                            System.getProperty("http.proxyHost"),
                            Integer.parseInt(System.getProperty("http.proxyPort"))
                    ));
                }
            }

            // Open connection using proxy if available
            HttpURLConnection connection = (proxy != null)
                    ? (HttpURLConnection) effectiveUrl.openConnection(proxy)
                    : (HttpURLConnection) effectiveUrl.openConnection();

            connection.setRequestMethod(request.getMethod());

            // Disable follow-redirects option if user disables it
            if (!this.followRedirects) {
                connection.setInstanceFollowRedirects(false);
            }

            // Set headers
            if (request.getHeaders() != null) {
                for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }

            // Enable output if there's a body or multipart data
            if (request.getBody() != null || boundary != null ||
                    (request.getFormData() != null && request.getFormData().length > 0) ||
                    (request.getAttachments() != null) ||
                    ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType) && request.getParameters() != null && !request.getParameters().isEmpty())) {

                connection.setDoOutput(true);

                if (boundary != null) {
                    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                }

                try (OutputStream writer = connection.getOutputStream()) {
                    // Write multipart parts if needed
                    if (boundary != null) {
                        // Write parameters as multipart parts
                        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
                            final String finalBoundary = boundary;
                            for (Map.Entry<String, Object> entry : request.getParameters().entrySet()) {
                                ContentDisposition cd = new ContentDisposition(
                                        entry.getKey(),
                                        null,
                                        "text/plain",
                                        entry.getValue().toString().getBytes(StandardCharsets.UTF_8)
                                );
                                writer.write(("--" + finalBoundary + LINE).getBytes(StandardCharsets.UTF_8));
                                writer.write(cd.getTransferBytes());
                            }
                            writer.flush();
                        }

                        // Write explicit form-data parts if available
                        if (request.getFormData() != null && request.getFormData().length > 0) {
                            for (ContentDisposition cd : request.getFormData()) {
                                writer.write(("--" + boundary + LINE).getBytes(StandardCharsets.UTF_8));
                                writer.write(cd.getTransferBytes());
                                writer.flush();
                            }
                        } else if (request.getAttachments() != null) {
                            // Write attachments if available
                            Attachments attachments = request.getAttachments();
                            for (var attachment : attachments.list()) {
                                ContentDisposition cd = new ContentDisposition(
                                        attachments.getParameterName(),
                                        attachment.getFilename(),
                                        "binary",
                                        attachment.get()
                                );
                                writer.write(("--" + boundary + LINE).getBytes(StandardCharsets.UTF_8));
                                writer.write(cd.getTransferBytes());
                                writer.flush();
                            }
                        }
                        // Write closing boundary
                        writer.write(("--" + boundary + "--" + LINE).getBytes(StandardCharsets.UTF_8));
                    } else if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
                        // URL-encoded body
                        String formBody = buildQuery(request.getParameters());
                        writer.write(formBody.getBytes(StandardCharsets.UTF_8));
                    } // Write simple body if provided
                    else if (request.getBody() != null) {
                        writer.write(request.getBody().getBytes(StandardCharsets.UTF_8));
                    }
                    writer.flush();
                }
            }

            connection.connect();

            String transferEncoding = connection.getHeaderField("Transfer-Encoding");
            if ("chunked".equalsIgnoreCase(transferEncoding)) {
                return new HTTPResponse(connection, consumer);
            }

            // Default response for normal HTTP request
            return new HTTPResponse(connection);
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * @param request
     * @return
     */
    @Override
    public CompletableFuture<URLResponse> handleRequestAsync(URLRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return handleRequest(request); // Call the synchronous method inside async execution
            } catch (ApplicationException e) {
                throw new CompletionException(e);
            }
        });
    }

    public void disableFollowRedirects() {
        this.followRedirects = false;
    }

    // Helper: Build query string from parameters.
    private String buildQuery(Map<String, Object> parameters) {
        StringBuilder queryBuilder = new StringBuilder();
        parameters.forEach((key, value) -> {
            if (queryBuilder.length() > 0) {
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

    public HTTPResponse(HttpURLConnection connection) {
        try {
            this.statusCode = connection.getResponseCode();
            this.headers = connection.getHeaderFields();

            // Choose the appropriate input stream based on the status code.
            InputStream in = (statusCode >= HttpURLConnection.HTTP_OK &&
                    statusCode < HttpURLConnection.HTTP_BAD_REQUEST)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            if (in != null) {
                String contentEncoding = connection.getContentEncoding();
                InputStream decodedStream = getDecodedInputStream(contentEncoding, in);
                this.body = new String(decodedStream.readAllBytes(), StandardCharsets.UTF_8);
                decodedStream.close();
            } else {
                this.body = "";
            }
        } catch (IOException e) {
            throw new ApplicationRuntimeException(e);
        }
    }

    /**
     * Constructor for SSE streaming.
     */
    public HTTPResponse(HttpURLConnection connection, Consumer<String> onMessage) throws IOException {
        this.statusCode = connection.getResponseCode();
        this.headers = connection.getHeaderFields();
        this.body = ""; // SSE mode does not return the entire body content.

        handleSSE(connection, onMessage);
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

    private void handleSSE(HttpURLConnection connection, Consumer<String> listener) throws IOException {
        String transferEncoding = connection.getHeaderField("Transfer-Encoding");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // When an empty line is encountered, dispatch the collected event
                listener.accept(line + "\n");
            }
        }

        // Handle the end of the SSE stream (e.g., no more events)
        if (transferEncoding == null || transferEncoding.equalsIgnoreCase("identity")) {
            // Ensure the stream is properly closed after chunking finishes
            // The client should be prepared to handle a properly closed connection.
            // No further action is needed here as chunking indicates the end of transmission.
            connection.disconnect();
        }
    }
}