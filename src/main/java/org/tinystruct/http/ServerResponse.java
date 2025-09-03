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
import org.tinystruct.ApplicationException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Tinystruct Response adapter for JDK HttpServer (no servlet APIs).
 */
public class ServerResponse implements Response<HttpExchange, HttpExchange> {
    private final HttpExchange exchange;
    private final Headers headers = new Headers();
    private ResponseStatus status = ResponseStatus.OK;
    private Version version = Version.HTTP1_1;
    private boolean headersSent = false;

    public ServerResponse(HttpExchange exchange) {
        this.exchange = exchange;
    }

    public void setContentType(String contentType) {
        addHeader(Header.CONTENT_TYPE.name(), contentType);
    }

    @Override
    public void addHeader(String header, Object value) {
        if (value instanceof Integer) {
            exchange.getResponseHeaders().add(header, String.valueOf(value));
        } else {
            exchange.getResponseHeaders().add(header, String.valueOf(value));
        }
        headers.add(new Header(header).set(value));
    }

    @Override
    public void sendRedirect(String url) throws ApplicationException {
        addHeader(Header.LOCATION.name(), url);
        setStatus(ResponseStatus.TEMPORARY_REDIRECT);
        writeAndFlush(new byte[0]);
    }

    @Override
    public void writeAndFlush(byte[] bytes) throws ApplicationException {
        try {
            if (!headersSent) {
                // Use chunked transfer by specifying 0 length
                exchange.sendResponseHeaders(status.code(), 0);
                headersSent = true;
            }
            try (OutputStream os = exchange.getResponseBody()) {
                if (bytes != null && bytes.length > 0) {
                    os.write(bytes);
                    os.flush();
                }
            }
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    @Override
    public HttpExchange get() {
        return exchange;
    }

    @Override
    public void close() throws ApplicationException {
        exchange.close();
    }

    @Override
    public ResponseStatus status() {
        return status;
    }

    @Override
    public Response<HttpExchange, HttpExchange> setStatus(ResponseStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public Headers headers() {
        return headers;
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public void setVersion(Version version) {
        this.version = version;
    }
}