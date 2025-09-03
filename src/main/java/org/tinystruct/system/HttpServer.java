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

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Context;
import org.tinystruct.http.Reforward;
import org.tinystruct.http.*;
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
import static org.tinystruct.Application.LANGUAGE;
import static org.tinystruct.Application.METHOD;

public class HttpServer extends AbstractApplication implements Bootstrap {
    private final Logger logger = Logger.getLogger(HttpServer.class.getName());
    private com.sun.net.httpserver.HttpServer server;
    private boolean started = false;
    private Settings settings;

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
                server.setExecutor(null); // Use default executor
            }

            // Create context and set handler
            HttpContext context = server.createContext("/", new DefaultHttpHandler(getContext(), this.settings));

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
        Request request = (Request) getContext().getAttribute(HTTP_REQUEST);
        Response response = (Response) getContext().getAttribute(HTTP_RESPONSE);

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

        private final Context context;
        private final Settings settings;

        private DefaultHttpHandler(Context context, Settings settings) {
            this.context = context;
            this.settings = settings;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Create TinyStruct Request and Response wrappers
                ServerRequest request = new ServerRequest(exchange);
                ServerResponse response = new ServerResponse(exchange);

                // Set up context
                this.context.setAttribute(HTTP_REQUEST, request);
                this.context.setAttribute(HTTP_RESPONSE, response);

                // Process the request using TinyStruct's DefaultHandler logic
                processRequest(request, response);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error processing request", e);
                sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            } finally {
                exchange.close();
            }
        }

        private void processRequest(ServerRequest request, ServerResponse response) throws IOException, ApplicationException {
            try {
                // Set up session ID first
                this.context.setId(request.getSession().getId());

                // Handle command line parameters first (like HttpRequestHandler)
                String[] parameterNames = request.parameterNames();
                for (String parameter : parameterNames) {
                    if (parameter.startsWith("--")) {
                        this.context.setAttribute(parameter, request.getParameter(parameter));
                    }
                }

                // Handle language parameter
                String lang = request.getParameter("lang");
                if (lang != null && !lang.trim().isEmpty()) {
                    String name = lang.replace('-', '_');
                    if (Language.support(name) && !lang.equalsIgnoreCase(this.settings.get("language"))) {
                        this.context.setAttribute(LANGUAGE, name);
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
                this.context.setAttribute(HTTP_HOST, http_protocol + hostName + url_prefix);
                this.context.setAttribute(METHOD, request.method().name());
                this.context.setAttribute(HTTP_REQUEST, request);
                this.context.setAttribute(HTTP_RESPONSE, response);

                // Handle query using request.query() method (like HttpRequestHandler)
                String query = request.query();
                if (query != null && query.length() > 1) {
                    handleRequest(query, this.context, response);
                } else {
                    handleDefaultPage(this.context, response);
                }

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in request processing", e);
                response.setContentType("text/plain; charset=UTF-8");
                response.setStatus(org.tinystruct.http.ResponseStatus.INTERNAL_SERVER_ERROR);
                response.writeAndFlush("500 - Internal Server Error".getBytes("UTF-8"));
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
        private void handleRequest(String query, Context context, ServerResponse response) throws IOException, ApplicationException {
            // Handle request
            query = StringUtilities.htmlSpecialChars(query);
            Object message = ApplicationManager.call(query, context);
            if (message != null) {
                if (message instanceof byte[]) {
                    byte[] bytes = (byte[]) message;
                    response.addHeader("Content-Length", String.valueOf(bytes.length));
                    response.writeAndFlush(bytes);
                } else {
                    response.setContentType("text/html; charset=UTF-8");
                    response.writeAndFlush(String.valueOf(message).getBytes("UTF-8"));
                }
            } else {
                response.setContentType("text/plain; charset=UTF-8");
                response.writeAndFlush("No response retrieved!".getBytes("UTF-8"));
            }
        }

        /**
         * Handles the default page request.
         *
         * @param context  The application context
         * @param response The HTTP response object
         * @throws IOException if an I/O error occurs
         */
        private void handleDefaultPage(Context context, ServerResponse response) throws IOException, ApplicationException {
            response.setContentType("text/html; charset=UTF-8");
            Object result = ApplicationManager.call(settings.getOrDefault("default.home.page", "say/Praise the Lord."), context);
            response.writeAndFlush(String.valueOf(result).getBytes("UTF-8"));
        }

        private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) {
            try {
                byte[] responseBytes = message.getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(statusCode, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to send error response", e);
            }
        }


    }
}