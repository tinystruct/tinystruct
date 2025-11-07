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
package org.tinystruct.system;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Context;
import org.tinystruct.http.Reforward;
import org.tinystruct.http.*;
import org.tinystruct.mcp.MCPPushManager;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;
import org.tinystruct.system.util.StringUtilities;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.tinystruct.http.Constants.HTTP_REQUEST;
import static org.tinystruct.http.Constants.HTTP_RESPONSE;
import static org.tinystruct.http.Constants.HTTP_HOST;

public class HttpServer extends AbstractApplication implements Bootstrap {
    private final Logger logger = Logger.getLogger(HttpServer.class.getName());
    private com.sun.net.httpserver.HttpServer server;
    private boolean started = false;
    private Settings settings;
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;

    public HttpServer() {
    }

    @Override
    public void init() {
        this.setTemplateRequired(false);
    }

    @Action(value = "start", description = "Start an HTTP server.", options = {
            @Argument(key = "server-port", description = "Server port"),
            @Argument(key = "http.proxyHost", description = "Proxy host for http"),
            @Argument(key = "http.proxyPort", description = "Proxy port for http"),
            @Argument(key = "https.proxyHost", description = "Proxy host for https"),
            @Argument(key = "https.proxyPort", description = "Proxy port for https"),
            @Argument(key = "server-threads", description = "Number of server threads (default: 0 - uses system default)")
    }, example = "bin/dispatcher start --import org.tinystruct.system.HttpServer --server-port 8080", mode = org.tinystruct.application.Action.Mode.CLI)
    @Override
    public void start() throws ApplicationException {
        if (started) return;

        String charsetName = null;
        this.settings = new Settings();
        if (this.settings.get("default.file.encoding") != null)
            charsetName = this.settings.get("default.file.encoding");

        if (charsetName != null && !charsetName.trim().isEmpty())
            System.setProperty("file.encoding", charsetName);

        this.settings.set("language", "zh_CN");
        if (this.settings.get("system.directory") == null)
            this.settings.set("system.directory", System.getProperty("user.dir"));

        try {
            // Initialize the application manager with the configuration.
            ApplicationManager.init(this.settings);
        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        // The port that we should run on can be set into an environment variable
        // Look for that variable and default to 8080 if it isn't there.
        int webPort = 8080;
        int serverThreads = 0; // 0 means system default

        if (getContext() != null) {
            if (getContext().getAttribute("--http.proxyHost") != null && getContext().getAttribute("--http.proxyPort") != null) {
                System.setProperty("http.proxyHost", getContext().getAttribute("--http.proxyHost").toString());
                System.setProperty("http.proxyPort", getContext().getAttribute("--http.proxyPort").toString());
            }

            if (getContext().getAttribute("--https.proxyHost") != null && getContext().getAttribute("--https.proxyPort") != null) {
                System.setProperty("https.proxyHost", getContext().getAttribute("--https.proxyHost").toString());
                System.setProperty("https.proxyPort", getContext().getAttribute("--https.proxyPort").toString());
            }

            if (getContext().getAttribute("--server-port") != null) {
                webPort = Integer.parseInt(getContext().getAttribute("--server-port").toString());
            }

            if (getContext().getAttribute("--server-threads") != null) {
                serverThreads = Integer.parseInt(getContext().getAttribute("--server-threads").toString());
            }
        }

        System.out.println(ApplicationManager.call("--logo", null, org.tinystruct.application.Action.Mode.CLI));

        final long start = System.currentTimeMillis();

        try {
            // Create HTTP server
            server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(webPort), 0);

            // Set thread pool
            if (serverThreads > 0) {
                server.setExecutor(Executors.newFixedThreadPool(serverThreads));
            } else {
                server.setExecutor(Executors.newCachedThreadPool()); // Use default executor
            }

            // Create context and set handler
            server.createContext("/", new DefaultHttpHandler(this.settings));

            // Configure context attributes similar to Tomcat setup
            initServerDefaults();

            // Start the server
            server.start();
            this.started = true;

            logger.info("HTTP server (" + webPort + ") startup in " + (System.currentTimeMillis() - start) + " ms");

            // Open the default browser
            getContext().setAttribute("--url", "http://localhost:" + webPort);
            ApplicationManager.call("open", getContext(), org.tinystruct.application.Action.Mode.CLI);

            // Keep the server running
            logger.info("Server is running. Press Ctrl+C to stop.");

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down HTTP server...");
                stop();
            }));

            // Keep the main thread alive
            while (started) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

        } catch (IOException e) {
            throw new ApplicationException("Failed to start HTTP server: " + e.getMessage(), e);
        }
    }

    private void initServerDefaults() {
        // Initialize default server settings
        logger.info("Initializing HTTP server defaults");

        // Set system properties for server configuration
        System.setProperty("sun.net.httpserver.nodelay", "true");

        // Additional server configuration can be added here
    }

    @Override
    public void stop() {
        if (server != null && started) {
            logger.info("Stopping HTTP server...");
            server.stop(2); // Wait up to 2 seconds for existing connections to finish
            started = false;
            logger.info("HTTP server stopped");
        }
    }

    @Action(value = "error", description = "Error page")
    public Object exceptionCaught() throws ApplicationException {
        Request<?, ?> request = (Request<?, ?>) getContext().getAttribute(HTTP_REQUEST);
        Response<?, ?> response = (Response<?, ?>) getContext().getAttribute(HTTP_RESPONSE);

        Reforward reforward = new Reforward(request, response);
        this.setVariable("from", reforward.getFromURL());

        Session session = request.getSession();
        if (session.getAttribute("error") != null) {
            ApplicationException exception = (ApplicationException) session.getAttribute("error");

            String message = exception.getRootCause().getMessage();
            this.setVariable("exception.message", Objects.requireNonNullElse(message, "Unknown error"));

            StackTraceElement[] stackTrace = exception.getStackTrace();
            StringBuilder builder = new StringBuilder();
            builder.append(exception).append("\n");
            for (StackTraceElement stackTraceElement : stackTrace) {
                builder.append(stackTraceElement.toString()).append("\n");
            }
            logger.severe(builder.toString());

            return this.getVariable("exception.message").getValue().toString();
        } else {
            reforward.forward();
        }

        return "This request is forbidden!";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    /**
     * HTTP handler that integrates with TinyStruct framework
     */
    private class DefaultHttpHandler implements HttpHandler {

        private final Settings settings;

        private DefaultHttpHandler(Settings settings) {
            this.settings = settings;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Serve static files first (mirror Netty's HttpStaticFileHandler precedence)
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    if (tryServeStatic(exchange)) {
                        return;
                    }
                }

                // Create TinyStruct Request and Response wrappers
                ServerRequest request = new ServerRequest(exchange);
                ServerResponse response = new ServerResponse(exchange);

                // Set up context
                ApplicationContext context = new ApplicationContext();
                // Set up session ID first
                context.setId(request.getSession().getId());
                context.setAttribute(HTTP_REQUEST, request);
                context.setAttribute(HTTP_RESPONSE, response);

                // Process SSE first to ensure correct headers and long-lived connection
                if (isSSE(exchange)) {
                    handleSSE(request, response, context);
                    return;
                }

                // Process the request using TinyStruct's DefaultHandler logic
                processRequest(request, response, context);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error processing request", e);
                // Try to send error only if headers haven't been committed yet
                try {
                    sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
                } catch (Exception ignored) {
                    // If we can't send an error (headers/body already sent), just log.
                }
            }
        }

        private boolean isSSE(HttpExchange exchange) {
            String accept = exchange.getRequestHeaders().getFirst("Accept");
            return accept != null && accept.contains("text/event-stream");
        }

        private SSEPushManager getAppropriatePushManager(boolean isMCP) {
            return isMCP ? MCPPushManager.getInstance() : SSEPushManager.getInstance();
        }

        private void handleSSE(ServerRequest request, ServerResponse response, Context context) throws IOException, ApplicationException {
            // Set SSE headers
            response.addHeader(Header.CONTENT_TYPE.name(), "text/event-stream");
            response.addHeader(Header.CACHE_CONTROL.name(), "no-cache");
            response.addHeader(Header.CONNECTION.name(), "keep-alive");
            response.addHeader(Header.TRANSFER_ENCODING.name(), "chunked");
            response.addHeader("X-Accel-Buffering", "no");

            String query = request.getParameter("q");
            boolean isMCP = false;
            if (query != null) {
                query = StringUtilities.htmlSpecialChars(query);
                if (query.equals(org.tinystruct.mcp.MCPSpecification.Endpoints.SSE)) {
                    isMCP = true;
                }

                Object call = ApplicationManager.call(query, context);
                String sessionId = context.getId();
                SSEPushManager pushManager = getAppropriatePushManager(isMCP);
                response.setStatus(ResponseStatus.OK);
                // Ensure chunked streaming for SSE before any write
                response.sendHeaders(-1);
                SSEClient client = pushManager.register(sessionId, response);

                if (call instanceof org.tinystruct.data.component.Builder) {
                    pushManager.push(sessionId, (org.tinystruct.data.component.Builder) call);
                } else if (call instanceof String) {
                    org.tinystruct.data.component.Builder builder = new org.tinystruct.data.component.Builder();
                    builder.parse((String) call);
                    pushManager.push(sessionId, builder);
                }

                if (client != null) {
                    try {
                        while (client.isActive()) {
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new ApplicationException("Stream interrupted: " + e.getMessage(), e);
                    } catch (Exception e) {
                        throw new ApplicationException("Error in stream: " + e.getMessage(), e);
                    } finally {
                        client.close();
                        pushManager.remove(sessionId);
                    }
                }
            }
        }

        private boolean tryServeStatic(HttpExchange exchange) {
            try {
                String uri = exchange.getRequestURI().toString();
                String path = sanitizeUri(uri);
                if (path == null) return false;

                String filepath = path;
                int q = path.indexOf("?");
                if (q >= 0) filepath = path.substring(0, q);

                java.io.File file = new java.io.File(filepath);
                if (!file.exists() || file.isHidden()) {
                    if (filepath.endsWith("/favicon.ico")) {
                        try (InputStream stream = Objects.requireNonNull(getClass().getResource("/favicon.ico")).openStream()) {
                            byte[] bytes = stream.readAllBytes();
                            exchange.getResponseHeaders().set("Content-Type", "image/x-icon");
                            exchange.sendResponseHeaders(200, bytes.length);
                            try (OutputStream os = exchange.getResponseBody()) {
                                os.write(bytes);
                            }
                            return true;
                        } catch (Exception ignore) {
                            // fall-through to dynamic handling
                            return false;
                        }
                    }
                    return false;
                }

                if (!file.isFile()) return false;

                // If-Modified-Since support
                String ifModifiedSince = exchange.getRequestHeaders().getFirst("If-Modified-Since");
                if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
                    java.text.SimpleDateFormat df = new java.text.SimpleDateFormat(HTTP_DATE_FORMAT, java.util.Locale.US);
                    df.setTimeZone(java.util.TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));
                    java.util.Date ims = df.parse(ifModifiedSince);
                    long imsSeconds = ims.getTime() / 1000;
                    long fileSeconds = file.lastModified() / 1000;
                    if (imsSeconds == fileSeconds) {
                        // 304 Not Modified
                        setDateHeader(exchange);
                        exchange.sendResponseHeaders(304, -1);
                        return true;
                    }
                }

                // Content-Type
                String contentType = null;
                try {
                    jakarta.activation.MimetypesFileTypeMap mimeTypesMap = new jakarta.activation.MimetypesFileTypeMap(HttpServer.class.getResourceAsStream("/META-INF/mime.types"));
                    contentType = mimeTypesMap.getContentType(file);
                } catch (Exception ignore) {
                }
                if (contentType == null || contentType.equalsIgnoreCase("application/octet-stream")) {
                    try {
                        contentType = java.nio.file.Files.probeContentType(java.nio.file.Path.of(file.getName()));
                    } catch (IOException ignore) {
                    }
                }
                if (contentType == null) contentType = "application/octet-stream";

                // Cache headers
                setDateAndCacheHeaders(exchange, file);
                exchange.getResponseHeaders().set("Content-Type", contentType);

                long length = file.length();
                exchange.sendResponseHeaders(200, length);
                try (InputStream in = new java.io.FileInputStream(file); OutputStream os = exchange.getResponseBody()) {
                    in.transferTo(os);
                }
                return true;
            } catch (Exception e) {
                logger.log(Level.FINE, "Static serve miss: " + e.getMessage(), e);
                return false;
            }
        }

        private String sanitizeUri(String uri) throws java.io.UnsupportedEncodingException {
            String decoded = java.net.URLDecoder.decode(uri, java.nio.charset.StandardCharsets.UTF_8);
            if (decoded.isEmpty() || decoded.charAt(0) != '/') return null;
            if (decoded.length() > 255) throw new IllegalArgumentException("Input too long");
            decoded = decoded.replace('/', java.io.File.separatorChar);
            decoded = decoded.replace("..", "");
            if (decoded.contains(java.io.File.separator + '.') || decoded.contains('.' + java.io.File.separator) || decoded.charAt(0) == '.' || decoded.charAt(decoded.length() - 1) == '.')
                return null;
            return System.getProperty("user.dir") + java.io.File.separator + decoded;
        }

        private void setDateHeader(HttpExchange exchange) {
            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat(HTTP_DATE_FORMAT, java.util.Locale.US);
            df.setTimeZone(java.util.TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));
            java.util.Calendar time = new java.util.GregorianCalendar();
            exchange.getResponseHeaders().set("Date", df.format(time.getTime()));
        }

        private void setDateAndCacheHeaders(HttpExchange exchange, java.io.File file) {
            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat(HTTP_DATE_FORMAT, java.util.Locale.US);
            df.setTimeZone(java.util.TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));
            java.util.Calendar time = new java.util.GregorianCalendar();
            exchange.getResponseHeaders().set("Date", df.format(time.getTime()));
            time.add(java.util.Calendar.SECOND, HTTP_CACHE_SECONDS);
            exchange.getResponseHeaders().set("Expires", df.format(time.getTime()));
            exchange.getResponseHeaders().set("Cache-Control", "private, max-age=" + HTTP_CACHE_SECONDS);
            exchange.getResponseHeaders().set("Last-Modified", df.format(new java.util.Date(file.lastModified())));
        }

        private void processRequest(ServerRequest request, ServerResponse response, Context context) throws IOException, ApplicationException {
            try {
                // Handle command line parameters first (like HttpRequestHandler)
                String[] parameterNames = request.parameterNames();
                for (String parameter : parameterNames) {
                    if (parameter.startsWith("--")) {
                        context.setAttribute(parameter, request.getParameter(parameter));
                    }
                }

                // Handle language parameter
                String lang = request.getParameter("lang");
                if (lang != null && !lang.trim().isEmpty()) {
                    String name = lang.replace('-', '_');
                    if (Language.support(name) && !lang.equalsIgnoreCase(this.settings.get("language"))) {
                        context.setAttribute(LANGUAGE, name);
                    }
                }

                // Set up URL prefix logic (like HttpRequestHandler)
                String url_prefix = "/";
                if (this.settings.get("default.url_rewrite") != null && !"enabled".equalsIgnoreCase(this.settings.get("default.url_rewrite"))) {
                    url_prefix = "/?q=";
                }

                // Handle hostname configuration (like HttpRequestHandler)
                String host = request.headers().get(Header.HOST).toString();
                String hostName;
                if ((hostName = this.settings.get("default.hostname")) != null) {
                    if (hostName.length() <= 3) {
                        hostName = host;
                    }
                } else {
                    hostName = host;
                }

                // Set up protocol (like HttpRequestHandler)
                String http_protocol = "http://";
                if (request.isSecure()) {
                    http_protocol = "https://";
                }

                // Set up context attributes (like HttpRequestHandler)
                context.setAttribute(HTTP_HOST, http_protocol + hostName + url_prefix);
                context.setAttribute(HTTP_REQUEST, request);
                context.setAttribute(HTTP_RESPONSE, response);

                // Ensure session cookie (JSESSIONID) is set like other servers
                boolean sessionCookieExists = false;
                for (Cookie cookie : request.cookies()) {
                    if (cookie.name().equalsIgnoreCase(Constants.JSESSIONID)) {
                        sessionCookieExists = true;
                        break;
                    }
                }
                if (!sessionCookieExists) {
                    Cookie cookie = new CookieImpl(Constants.JSESSIONID);
                    if (host.contains(":"))
                        cookie.setDomain(host.substring(0, host.indexOf(":")));
                    cookie.setValue(context.getId());
                    cookie.setHttpOnly(true);
                    cookie.setPath("/");
                    cookie.setMaxAge(-1);
                    response.addHeader(Header.SET_COOKIE.name(), cookie);
                }

                // Handle query using request.query() method (like HttpRequestHandler)
                String query = request.getParameter("q");
                if (query != null && query.length() > 1) {
                    Method method = request.method();
                    org.tinystruct.application.Action.Mode mode = org.tinystruct.application.Action.Mode.fromName(method.name());
                    handleRequest(query, context, response, mode);
                } else {
                    handleDefaultPage(context, response);
                }
            } catch (ApplicationException e) {
                logger.log(Level.SEVERE, "Error in request processing", e);
                response.setContentType("text/plain; charset=UTF-8");
                int status = e.getStatus();
                response.setStatus(org.tinystruct.http.ResponseStatus.valueOf(status));
                response.writeAndFlush("500 - Internal Server Error".getBytes("UTF-8"));
                response.close();
            }
        }

        /**
         * Handles the HTTP request by processing the query.
         *
         * @param query    The query string
         * @param context  The application context
         * @param response The HTTP response object
         * @throws IOException if an I/O error occurs
         */
        private void handleRequest(String query, Context context, ServerResponse response, org.tinystruct.application.Action.Mode mode) throws IOException, ApplicationException {
            // Handle request
            query = StringUtilities.htmlSpecialChars(query);
            Object message = ApplicationManager.call(query, context, mode);
            byte[] bytes;
            if (message != null) {
                if (message instanceof byte[]) {
                    bytes = (byte[]) message;
                } else {
                    response.setContentType("text/html; charset=UTF-8");
                    bytes = String.valueOf(message).getBytes("UTF-8");
                }
            } else {
                response.setContentType("text/html; charset=UTF-8");
                bytes = "No response retrieved!".getBytes("UTF-8");
            }

            response.setStatus(ResponseStatus.OK);
            response.writeAndFlush(bytes);
            response.close();
        }

        /**
         * Handles the default page request.
         *
         * @param context  The application context
         * @param response The HTTP response object
         * @throws IOException if an I/O error occurs
         */
        private void handleDefaultPage(Context context, ServerResponse response) throws ApplicationException {
            response.setContentType("text/html; charset=UTF-8");
            Object result = ApplicationManager.call(settings.getOrDefault("default.home.page", "say/Praise the Lord."), context, org.tinystruct.application.Action.Mode.HTTP_GET);
            if (!response.isClosed()) {
                try {
                    byte[] bytes = String.valueOf(result).getBytes("UTF-8");
                    response.setStatus(ResponseStatus.OK);
                    response.writeAndFlush(bytes);
                } catch (UnsupportedEncodingException e) {
                    throw new ApplicationException(e);
                } finally {
                    response.close();
                }
            }
        }

        private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) {
            try {
                byte[] responseBytes = message.getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(statusCode, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                    os.flush();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Client disconnected while sending error response", e);
            } finally {
                try {
                    exchange.close();
                } catch (Exception e) {
                    logger.log(Level.FINE, "Error while closing exchange", e);
                }
            }
        }

    }
}