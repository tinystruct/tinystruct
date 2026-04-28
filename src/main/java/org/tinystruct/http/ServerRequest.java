/*******************************************************************************
 * Copyright  (c) 2013, 2025 James M. ZHOU
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
package org.tinystruct.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsExchange;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.Attachment;
import org.tinystruct.data.FileEntity;
import org.tinystruct.transfer.http.upload.ContentDisposition;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.tinystruct.http.Constants.JSESSIONID;

/**
 * Tinystruct Request adapter for JDK HttpServer (no servlet APIs).
 */
public class ServerRequest implements Request<HttpExchange, InputStream> {
    private final HttpExchange exchange;
    private final Headers headers = new Headers();
    private final Map<String, List<String>> params = new HashMap<>();
    private Cookie[] cookies = new Cookie[0];
    private String uri;
    private Method method;
    private Version version = Version.HTTP1_1;
    private String body;
    private String sessionId;
    private List<FileEntity> attachments;

    public ServerRequest(HttpExchange exchange) throws IOException {
        this.exchange = exchange;
        this.uri = exchange.getRequestURI().toString();

        // Headers
        exchange.getRequestHeaders().forEach((k, v) -> headers.add(Header.value0f(k).set(String.join(",", v))));

        // Cookies
        List<String> cookieHeaders = exchange.getRequestHeaders().get(Header.COOKIE.name());
        if (cookieHeaders != null && !cookieHeaders.isEmpty()) {
            List<Cookie> cookieList = new ArrayList<>();
            for (String header : cookieHeaders) {
                String[] parts = header.split(";\\s*");
                for (String part : parts) {
                    int idx = part.indexOf('=');
                    String name, value;
                    if (idx > 0) {
                        name = part.substring(0, idx).trim();
                        value = part.substring(idx + 1).trim();
                    } else {
                        name = part.trim();
                        value = "";
                    }

                    if (!name.isEmpty()) {
                        CookieImpl cookie = new CookieImpl(name);
                        cookie.setValue(value);
                        if (JSESSIONID.equalsIgnoreCase(name)) {
                            sessionId = value;
                        }
                        cookieList.add(cookie);
                    }
                }
            }
            cookies = cookieList.toArray(new Cookie[0]);
        }

        // Params from query
        URI u = exchange.getRequestURI();
        if (u.getRawQuery() != null) parseQueryString(u.getRawQuery());

        // Params from x-www-form-urlencoded body
        String contentType = exchange.getRequestHeaders().getFirst(Header.CONTENT_TYPE.name());
        if (contentType != null && contentType.toLowerCase().startsWith(Header.StandardValue.APPLICATION_X_WWW_FORM_URLENCODED.toString())) {
            parseQueryString(this.body());
        }
    }

    private void parseQueryString(String query) {
        if (query == null || query.isEmpty()) return;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            try {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = kv.length == 2 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public List<FileEntity> getAttachments() throws ApplicationException {
        if (attachments != null) return attachments;

        String contentType = exchange.getRequestHeaders().getFirst(Header.CONTENT_TYPE.name());
        if (contentType != null && contentType.toLowerCase().startsWith(Header.StandardValue.MULTIPART_FORM_DATA.toString())) {
            attachments = new ArrayList<>();
            MultipartData multipartData = new MultipartData(this);
            ContentDisposition part;
            while ((part = multipartData.getNextPart()) != null) {
                if (part.getFileName() != null) {
                    Attachment attachment = new Attachment();
                    attachment.setFilename(part.getFileName());
                    attachment.setContentType(part.getContentType());
                    attachment.setContent(part.getData());
                    attachments.add(attachment);
                } else {
                    params.computeIfAbsent(part.getName(), k -> new ArrayList<>()).add(new String(part.getData(), StandardCharsets.UTF_8));
                }
            }
        }
        return attachments;
    }

    @Override
    public Session getSession(String id, boolean generate) {
        SessionManager manager = SessionManager.getInstance();
        return manager.getSession(id, generate);
    }

    @Override
    public Session getSession() {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        }
        return getSession(sessionId, true);
    }

    @Override
    public String getParameter(String name) {
        List<String> list = params.get(name);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    @Override
    public Cookie[] cookies() {
        return cookies;
    }

    @Override
    public String query() {
        String q = exchange.getRequestURI().getRawQuery();
        return q == null ? "" : q;
    }

    @Override
    public String body() {
        if (body == null) {
            boolean hasBody = false;
            String contentLength = exchange.getRequestHeaders().getFirst(Header.CONTENT_LENGTH.name());
            if (contentLength != null && Long.parseLong(contentLength) > 0) {
                hasBody = true;
            } else if (exchange.getRequestHeaders().containsKey(Header.TRANSFER_ENCODING.name())) {
                hasBody = true;
            }

            if (hasBody) {
                try (InputStream is = exchange.getRequestBody()) {
                    byte[] bytes = is.readAllBytes();
                    body = new String(bytes, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    body = "";
                }
            }
        }
        return body;
    }

    @Override
    public boolean isSecure() {
        return exchange instanceof HttpsExchange;
    }

    @Override
    public InputStream stream() {
        return exchange.getRequestBody();
    }

    @Override
    public String[] parameterNames() {
        return params.keySet().toArray(new String[0]);
    }

    @Override
    public Headers headers() {
        return headers;
    }

    @Override
    public Method method() {
        if (method == null) method = Method.valueOf(exchange.getRequestMethod());
        return method;
    }

    @Override
    public Request<HttpExchange, InputStream> setMethod(Method method) {
        this.method = method;
        return this;
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public Request<HttpExchange, InputStream> setUri(String uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public void setVersion(Version version) {
        this.version = version;
    }

    // Convenience
    public String getPath() {
        return exchange.getRequestURI().getPath();
    }

    public HttpExchange getHttpExchange() {
        return exchange;
    }

    public InetSocketAddress getRemoteAddress() {
        return exchange.getRemoteAddress();
    }
}