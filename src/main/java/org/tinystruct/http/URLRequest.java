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

import org.tinystruct.ApplicationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class URLRequest {

    private URL url;
    private Map<String, String> headers;
    private Method method = Method.GET;
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
            String parameters = "";
            URL url;
            if (request.parameters().size() > 0) {
                parameters = this.buildQuery(request.parameters());
                if (request.uri().contains("?"))
                    url = new URL(this.url.toString() + "&" + parameters);
                else
                    url = new URL(this.url.toString() + "?" + parameters);
            } else {
                url = this.url;
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
            request.headers().values().forEach(h -> this.connection.setRequestProperty(h.name(), h.value().toString()));

            this.connection.setRequestMethod(request.method().name());

            if (request.requestBody() != null) {
                this.connection.setDoOutput(true);
            }

            try (OutputStream wr = connection.getOutputStream()) {
                // Send request
                wr.write(request.requestBody().getBytes(StandardCharsets.UTF_8));
                wr.flush();
            } catch (IOException e) {
                throw new ApplicationException(e.toString(), e);
            }
        } catch (IOException e) {
            throw new ApplicationException(e.toString(), e);
        }

        connection.connect();

        try (final InputStream in = connection.getResponseCode() == HttpURLConnection.HTTP_OK ? connection.getInputStream() : connection.getErrorStream(); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[1024];
            int len;
            while ((len = in.read(bytes)) != -1) {
                out.write(bytes, 0, len);
            }
            return callback.process(out);
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        } finally {
            connection.disconnect();
        }
    }

    private String buildQuery(Map<String, Object> parameters) {
        Set<Map.Entry<String, Object>> keySet = parameters.entrySet();
        Iterator<Map.Entry<String, Object>> iterator = keySet.iterator();
        StringBuilder buffer = new StringBuilder();
        Map.Entry<String, Object> entry;
        boolean first = true;
        while (iterator.hasNext()) {
            entry = iterator.next();
            if (first) {
                first = false;
            } else
                buffer.append("&");

            buffer.append(entry.getKey()).append("=").append(entry.getValue());
        }

        return buffer.toString();
    }

}
