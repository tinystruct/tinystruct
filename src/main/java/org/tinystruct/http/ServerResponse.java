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
    private boolean closed = false;
    private boolean headersSent = false;
    private OutputStream outputStream;

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
        try {
            this.exchange.sendResponseHeaders(this.status.code(), 0);
            this.headersSent = true;
        } catch (IOException e) {
            throw new ApplicationException(e);
        }
        close();
    }

    @Override
    public void writeAndFlush(byte[] bytes) throws ApplicationException {
        try {
            // Ensure headers are sent at least once.
            // If we have a concrete byte array and headers not yet sent, prefer fixed Content-Length.
            if (!headersSent) {
                // If content length is known and this is a one-shot response, let caller use sendHeaders(len).
                // Fallback here: if bytes provided, use exact length; if not, use 0 to enable chunked per JDK HttpServer.
                long len = (bytes != null) ? bytes.length : 0;
                this.exchange.sendResponseHeaders(this.status.code(), len);
                this.headersSent = true;
            }

            if (this.outputStream == null) {
                this.outputStream = this.exchange.getResponseBody();
            }

            if (bytes != null && bytes.length > 0) {
                this.outputStream.write(bytes);
                this.outputStream.flush();
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
        if (closed) return;
        try {
            if (this.outputStream != null) {
                this.outputStream.close();
            }
        } catch (IOException ignore) {
        } finally {
            exchange.close();
        }
        closed = true;
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

    /**
     * Send response headers once with a known content length. Use -1 to keep chunked (0) behavior.
     */
    public void sendHeaders(long contentLength) throws ApplicationException {
        if (headersSent) return;
        long len = contentLength >= 0 ? contentLength : 0;
        try {
            this.exchange.sendResponseHeaders(this.status.code(), len);
            this.headersSent = true;
        } catch (IOException e) {
            throw new ApplicationException(e);
        }
    }

    public boolean isClosed() {
        return closed;
    }
}