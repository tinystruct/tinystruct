package org.tinystruct.http;

/*******************************************************************************
 * Copyright  (c) 2013, 2023 James Mover Zhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

import org.brotli.dec.BrotliInputStream;
import org.tinystruct.ApplicationException;
import org.tinystruct.transfer.http.upload.ContentDisposition;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import static org.tinystruct.transfer.http.upload.ContentDisposition.LINE;

public class URLRequest {

    private URL url;
    private Proxy proxy = null;
    private HttpURLConnection connection;

    public URLRequest(URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

    public URLRequest setUrl(URL url) {
        this.url = url;
        return this;
    }

    public void proxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public byte[] send(HttpRequestBuilder request) throws ApplicationException, URISyntaxException {
        try {
            return send(request, new Callback<ByteArrayOutputStream>() {
                @Override
                public byte[] process(ByteArrayOutputStream out) throws ApplicationException {
                    return out.toByteArray();
                }
            });
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }
    }

    public byte[] send(HttpRequestBuilder request, Callback<ByteArrayOutputStream> callback)
            throws ApplicationException, IOException {
        try {
            String boundary = null;
            URL url;
            if (request.headers() != null && request.headers().get(Header.CONTENT_TYPE) != null && request.headers().get(Header.CONTENT_TYPE).toString().equalsIgnoreCase("multipart/form-data")) {
                boundary = String.valueOf(UUID.randomUUID());
                url = this.url;
            } else {
                if (request.parameters().size() > 0) {
                    String parameters = this.buildQuery(request.parameters());
                    if (this.url.toString().contains("?"))
                        url = new URL(this.url + "&" + parameters);
                    else
                        url = new URL(this.url + "?" + parameters);
                } else {
                    url = this.url;
                }
            }

            // Autodetect proxy for http connection
            if (this.proxy == null) {
                if (System.getProperty("https.proxyHost") != null && System.getProperty("https.proxyPort") != null) {
                    this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(System.getProperty("https.proxyHost"), Integer.parseInt(System.getProperty("https.proxyPort"))));
                } else if (System.getProperty("http.proxyHost") != null && System.getProperty("http.proxyPort") != null) {
                    this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(System.getProperty("http.proxyHost"), Integer.parseInt(System.getProperty("http.proxyPort"))));
                }
            }

            // Check proxy first
            if (this.proxy != null) {
                this.connection = (HttpURLConnection) url.openConnection(this.proxy);
            } else {
                this.connection = (HttpURLConnection) url.openConnection();
            }

            // Set headers
            if (request.headers() != null) {
                request.headers().values().forEach(h -> this.connection.setRequestProperty(h.name(), h.value().toString()));
            }

            this.connection.setRequestMethod(request.method().name());

            if (request.requestBody() != null || boundary != null) {
                this.connection.setDoOutput(true);

                if (boundary != null) {
                    this.connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                }

                try (OutputStream writer = connection.getOutputStream()) {
                    if (boundary != null) {
                        if (request.parameters().size() > 0) {
                            String finalBoundary = boundary;
                            request.parameters().forEach((name, value) -> {
                                ContentDisposition contentDisposition = new ContentDisposition(name, null, "text/plain", value.toString().getBytes(StandardCharsets.UTF_8));
                                try {
                                    writer.write(("--" + finalBoundary + LINE).getBytes(StandardCharsets.UTF_8));
                                    writer.write(contentDisposition.getTransferBytes());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            writer.flush();
                        }

                        if (request.getFormData() != null && request.getFormData().length > 0) {
                            ContentDisposition[] formData = request.getFormData();
                            for (ContentDisposition formDatum : formData) {
                                writer.write(("--" + boundary + LINE).getBytes(StandardCharsets.UTF_8));
                                writer.write(formDatum.getTransferBytes());
                                writer.flush();
                            }
                        } else if (request.getAttachments() != null) {
                            HttpRequestBuilder.Attachments attachments = request.getAttachments();
                            String fileBoundary = boundary;
                            attachments.list().forEach(attachment -> {
                                try {
                                    ContentDisposition contentDisposition = new ContentDisposition(attachments.getParameterName(), attachment.getFilename(), "binary", attachment.get());
                                    writer.write(("--" + fileBoundary + LINE).getBytes(StandardCharsets.UTF_8));
                                    writer.write(contentDisposition.getTransferBytes());
                                    writer.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        }

                        writer.write(("--" + boundary + "--" + LINE).getBytes(StandardCharsets.UTF_8));
                    }

                    // Send request
                    if (request.requestBody() != null) {
                        writer.write(request.requestBody().getBytes(StandardCharsets.UTF_8));
                    }
                    writer.flush();
                } catch (IOException e) {
                    throw new ApplicationException(e.toString(), e);
                }
            }
            connection.connect();
        } catch (IOException e) {
            throw new ApplicationException(e.toString(), e);
        }

        try (final InputStream in = connection.getResponseCode() == HttpURLConnection.HTTP_OK ? connection.getInputStream() : connection.getErrorStream();
             final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String contentEncoding = connection.getContentEncoding();
            byte[] buffer = new byte[1024];
            int bytesRead;
            InputStream decodedStream = null;
            if (contentEncoding != null) {
                switch (contentEncoding) {
                    case "gzip":
                        decodedStream = new GZIPInputStream(in);
                        break;
                    case "deflate":
                        decodedStream = new DeflaterInputStream(in);
                        break;
                    case "br":
                        decodedStream = new BrotliInputStream(in);
                        break;
                    default:
                        break;
                }
            }

            InputStream inputToRead = (decodedStream != null) ? decodedStream : in;
            while ((bytesRead = inputToRead.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            return callback.process(out);
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        } finally {
            connection.disconnect();
        }
    }

    public byte[] send0(HttpRequestBuilder request) throws ApplicationException {
        return send0(request, new Callback<HttpResponse<byte[]>>() {
            @Override
            public byte[] process(HttpResponse<byte[]> data) throws ApplicationException {
                return data.body();
            }
        });
    }

    public byte[] send0(HttpRequestBuilder request, Callback<HttpResponse<byte[]>> callback) throws ApplicationException {

        HttpRequest.Builder builder = HttpRequest.newBuilder();
        try {
            builder.uri(this.url.toURI());
        } catch (URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }

        // Set headers
        if (request.headers() != null) {
            request.headers().values().forEach(h -> builder.setHeader(h.name(), h.value().toString()));
        }

        if (request.requestBody() != null) {
            builder.method(request.method().name(), HttpRequest.BodyPublishers.ofByteArray(request.requestBody().getBytes(StandardCharsets.UTF_8)));
        }

        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
        if (request.version() != null) {
            switch (request.version()) {
                case HTTP2_0:
                    httpClientBuilder.version(HttpClient.Version.HTTP_2);
                    break;
                case HTTP1_1:
                case HTTP1_0:
                    httpClientBuilder.version(HttpClient.Version.HTTP_1_1);
                    break;
                default:
                    break;
            }
        }

        if (this.proxy != null) {
            httpClientBuilder.proxy(new ProxySelector());
        }

        try {
            HttpResponse<byte[]> response = httpClientBuilder.build().send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return callback.process(response);
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        } catch (InterruptedException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }
    }

    private String buildQuery(Map<String, Object> parameters) {
        StringBuilder queryBuilder = new StringBuilder();
        parameters.forEach((key, value) -> {
            if (queryBuilder.length() > 0) {
                queryBuilder.append("&");
            }
            queryBuilder.append(key).append("=").append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
        });
        return queryBuilder.toString();
    }

    class ProxySelector extends java.net.ProxySelector {

        /**
         * Selects all the applicable proxies based on the protocol to
         * access the resource with and a destination address to access
         * the resource at.
         * The format of the URI is defined as follow:
         * <UL>
         * <LI>http URI for http connections</LI>
         * <LI>https URI for https connections
         * <LI>{@code socket://host:port}<br>
         * for tcp client sockets connections</LI>
         * </UL>
         *
         * @param uri The URI that a connection is required to
         * @return a List of Proxies. Each element in the
         * the List is of type
         * {@link Proxy Proxy};
         * when no proxy is available, the list will
         * contain one element of type
         * {@link Proxy Proxy}
         * that represents a direct connection.
         * @throws IllegalArgumentException if the argument is null
         */
        @Override
        public List<Proxy> select(URI uri) {
            return List.of(proxy);
        }

        /**
         * Called to indicate that a connection could not be established
         * to a proxy/socks server. An implementation of this method can
         * temporarily remove the proxies or reorder the sequence of
         * proxies returned by {@link #select(URI)}, using the address
         * and the IOException caught when trying to connect.
         *
         * @param uri The URI that the proxy at sa failed to serve.
         * @param sa  The socket address of the proxy/SOCKS server
         * @param ioe The I/O exception thrown when the connect failed.
         * @throws IllegalArgumentException if either argument is null
         */
        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        }
    }

}

