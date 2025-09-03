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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.tinystruct.http.Constants.JSESSIONID;

/**
 * Tinystruct Request adapter for JDK HttpServer (no servlet APIs).
 */
public class ServerRequest implements Request<HttpExchange, Object> {
    private final HttpExchange exchange;
    private final Headers headers = new Headers();
    private final Map<String, List<String>> params = new HashMap<>();
    private Cookie[] cookies = new Cookie[0];
    private String uri;
    private Method method;
    private Version version = Version.HTTP1_1;
    private String body;
    private String sessionId;

    public ServerRequest(HttpExchange exchange) throws IOException {
        this.exchange = exchange;
        this.uri = exchange.getRequestURI().toString();

        // Headers
        exchange.getRequestHeaders().forEach((k, v) -> headers.add(Header.value0f(k).set(String.join(",", v))));

        // Cookies
        List<String> cookieHeaders = exchange.getRequestHeaders().get("Cookie");
        if (cookieHeaders != null && !cookieHeaders.isEmpty()) {
            List<Cookie> cookieList = new ArrayList<>();
            for (String header : cookieHeaders) {
                String[] parts = header.split(";\\s*");
                for (String part : parts) {
                    int idx = part.indexOf('=');
                    if (idx > 0) {
                        String name = part.substring(0, idx).trim();
                        String value = part.substring(idx + 1).trim();
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

        // Body (only read small bodies; for large, user should stream())
        try (InputStream is = exchange.getRequestBody()) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            body = sb.toString();
        }

        // Params from query and x-www-form-urlencoded body
        URI u = exchange.getRequestURI();
        if (u.getRawQuery() != null) parseQueryString(u.getRawQuery());
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType != null && contentType.toLowerCase().contains("application/x-www-form-urlencoded") && body != null) {
            parseQueryString(body);
        }
    }

    private void parseQueryString(String query) {
        if (query == null || query.isEmpty()) return;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            try {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name());
                String value = kv.length == 2 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name()) : "";
                params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public List<org.tinystruct.data.FileEntity> getAttachments() {
        return null;
    }

    @Override
    public Session getSession(String id, boolean generate) {
        SessionManager manager = SessionManager.getInstance();
        if (manager.getSession(id) == null && generate) {
            manager.setSession(id, new MemorySession(id));
        }
        return manager.getSession(id);
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
        String q = exchange.getRequestURI().getQuery();
        return q == null ? "" : q;
    }

    @Override
    public String body() {
        return body;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public Object stream() {
        return null;
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
    public Request<HttpExchange, Object> setMethod(Method method) {
        this.method = method;
        return this;
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public Request<HttpExchange, Object> setUri(String uri) {
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
}