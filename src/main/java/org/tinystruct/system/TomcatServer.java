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

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.*;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.http.Reforward;
import org.tinystruct.http.*;
import org.tinystruct.http.Session;
import org.tinystruct.http.servlet.RequestBuilder;
import org.tinystruct.http.servlet.ResponseBuilder;
import org.tinystruct.mcp.MCPPushManager;
import org.tinystruct.mcp.MCPSpecification;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;
import org.tinystruct.system.util.StringUtilities;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.tinystruct.http.Constants.*;

public class TomcatServer extends AbstractApplication implements Bootstrap {
    private final Logger logger = Logger.getLogger(TomcatServer.class.getName());
    private boolean started = false;

    public TomcatServer() {
    }

    @Override
    public void init() {
        this.setTemplateRequired(false);
    }

    @Action(value = "start", description = "Start a Tomcat server.", options = {
            @Argument(key = "server-port", description = "Server port"),
            @Argument(key = "http.proxyHost", description = "Proxy host for http"),
            @Argument(key = "http.proxyPort", description = "Proxy port for http"),
            @Argument(key = "https.proxyHost", description = "Proxy host for https"),
            @Argument(key = "https.proxyPort", description = "Proxy port for https")
    }, example = "bin/dispatcher start --import org.tinystruct.system.TomcatServer --server-port 777", mode = org.tinystruct.application.Action.Mode.CLI)
    @Override
    public void start() throws ApplicationException {
        if (started) return;

        String charsetName = null;
        Settings settings = new Settings();
        if (settings.get("default.file.encoding") != null)
            charsetName = settings.get("default.file.encoding");

        if (charsetName != null && !charsetName.trim().isEmpty())
            System.setProperty("file.encoding", charsetName);

        settings.set("language", "zh_CN");
        if (settings.get("system.directory") == null)
            settings.set("system.directory", System.getProperty("user.dir"));

        try {
            // Initialize the application manager with the configuration.
            ApplicationManager.init(settings);
        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        // The port that we should run on can be set into an environment variable
        // Look for that variable and default to 8080 if it isn't there.
        int webPort = 8080;
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
        }

        System.out.println(ApplicationManager.call("--logo", null, org.tinystruct.application.Action.Mode.CLI));

        final long start = System.currentTimeMillis();
        final String webappDirLocation = ".";
        final Tomcat tomcat = new Tomcat();

        tomcat.setPort(webPort);
        tomcat.setAddDefaultWebXmlToWebapp(false);
        tomcat.getConnector();

        Host host = tomcat.getHost();
        host.setConfigClass(DefaultContextConfig.class.getName());
        host.setAutoDeploy(false);
        try {
            LifecycleListener config = new DefaultContextConfig();

            String docBase = new File(webappDirLocation).getAbsolutePath();
            Context ctx = tomcat.addWebapp(host, "", docBase, config);
            initWebappDefaults(ctx);

            logger.info("Configuring app with basedir: " + docBase);

            Class<?> filterClass = DefaultHandler.class;
            FilterDef filterDef = new FilterDef();
            filterDef.setFilterClass(filterClass.getName());
            filterDef.setFilterName(filterClass.getSimpleName());
            ctx.addFilterDef(filterDef);

            FilterMap filterMap = new FilterMap();
            filterMap.setFilterName(filterClass.getSimpleName());
            filterMap.addURLPattern("/");
            ctx.addFilterMap(filterMap);

            tomcat.start();
            this.started = true;
            logger.info("Tomcat server (" + webPort + ") startup in " + (System.currentTimeMillis() - start) + " ms");

            // Open the default browser
            getContext().setAttribute("--url", "http://localhost:" + webPort);
            ApplicationManager.call("open", getContext(), org.tinystruct.application.Action.Mode.CLI);

            tomcat.getServer().await();
        } catch (LifecycleException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }
    }

    private void initWebappDefaults(Context ctx) {
        // Default servlet
        Wrapper servlet = Tomcat.addServlet(
                ctx, "default", "org.apache.catalina.servlets.DefaultServlet");
        servlet.setLoadOnStartup(1);
        servlet.setOverridable(true);

        // Servlet mappings
        ctx.addServletMappingDecoded("/", "default");

        // Sessions
        ctx.setSessionTimeout(30);

        // MIME type mappings
        Tomcat.addDefaultMimeTypeMappings(ctx);
    }

    @Override
    public void stop() {
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
        return "";
    }

    static class DefaultContextConfig extends ContextConfig {
        private static final Log log = LogFactory.getLog(ContextConfig.class);

        @Override
        protected synchronized void configureStart() {
            // Called from StandardContext.start()

            if (log.isDebugEnabled()) {
                log.debug(sm.getString("contextConfig.start"));
            }

            if (log.isDebugEnabled()) {
                log.debug(sm.getString("contextConfig.xmlSettings", context.getName(), Boolean.valueOf(context.getXmlValidation()), Boolean.valueOf(context.getXmlNamespaceAware())));
            }

            if (!skipWebXmlFileScan()) webConfig();

            if (!context.getIgnoreAnnotations()) {
                applicationAnnotationsConfig();
            }

            if (ok) {
                validateSecurityRoles();
            }

            // Configure an authenticator if we need one
            if (ok) {
                authenticatorConfig();
            }

            // Dump the contents of this pipeline if requested
            if (log.isDebugEnabled()) {
                log.debug("Pipeline Configuration:");
                Pipeline pipeline = context.getPipeline();
                Valve[] valves = null;
                if (pipeline != null) {
                    valves = pipeline.getValves();
                }
                if (valves != null) {
                    for (Valve valve : valves) {
                        log.debug("  " + valve.getClass().getName());
                    }
                }
                log.debug("======================");
            }

            // Make our application available if no problems were encountered
            if (ok) {
                context.setConfigured(true);
            } else {
                log.error(sm.getString("contextConfig.unavailable"));
                context.setConfigured(false);
            }
        }

        public boolean skipWebXmlFileScan() {
            return true;
        }
    }

    /**
     * DefaultHandler is responsible for handling HTTP requests and managing the application's lifecycle.
     */
    private static class DefaultHandler extends HttpServlet implements Bootstrap, Filter {
        private static final Logger logger = Logger.getLogger(DefaultHandler.class.getName());
        private static final long serialVersionUID = 0;
        private static final String DATE_FORMAT_PATTERN = "yyyy-M-d h:m:s";
        private static final SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        private String charsetName;
        private Configuration<String> settings;
        private String path;

        @Override
        public void init(ServletConfig config) {
            this.path = config.getServletContext().getRealPath("");
            try {
                this.start();
            } catch (ApplicationException e) {
                logger.severe(e.getMessage());
            }

            logger.info("Initialized servlet config and starting...");
        }

        @Override
        public void init(FilterConfig config) throws ServletException {
            this.path = config.getServletContext().getRealPath("");
            try {
                this.start();
            } catch (ApplicationException e) {
                logger.severe(e.getMessage());
            }

            logger.info("Initialized filter config and starting...");
        }

        @Override
        public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
            request.setCharacterEncoding(charsetName);
            response.setContentType("text/html;charset=" + charsetName);
            response.setCharacterEncoding(charsetName);
            response.setHeader("Pragma", "No-cache");
            response.setHeader("Cache-Control", "No-cache");
            response.setDateHeader("Expires", 0);

            final org.tinystruct.application.Context context = new ApplicationContext();
            Request<HttpServletRequest, ServletInputStream> _request = new RequestBuilder(request, isSSL());
            Response<HttpServletResponse, ServletOutputStream> _response = new ResponseBuilder(response);
            try {
                context.setId(_request.getSession().getId());
                context.setAttribute(HTTP_REQUEST, _request);
                context.setAttribute(HTTP_RESPONSE, _response);
                context.setAttribute(HTTP_SCHEME, request.getScheme());
                context.setAttribute(HTTP_SERVER, request.getServerName());
                context.setAttribute(HTTP_PROTOCOL, getProtocol(request));

                String lang = _request.getParameter("lang");
                if (lang != null && !lang.trim().isEmpty()) {
                    String name = lang.replace('-', '_');

                    if (Language.support(name) && !lang.equalsIgnoreCase(this.settings.get("language"))) {
                        context.setAttribute(LANGUAGE, name);
                    }
                }

                String url_prefix = "/";
                if (this.settings.get("default.url_rewrite") != null && !"enabled".equalsIgnoreCase(this.settings.get("default.url_rewrite"))) {
                    url_prefix = "/?q=";
                }

                context.setAttribute(HTTP_HOST, getHost(request) + url_prefix);
                context.setAttribute(METHOD, request.getMethod());

                String[] parameterNames = _request.parameterNames();
                for (String parameter : parameterNames) {
                    if (parameter.startsWith("--")) {
                        context.setAttribute(parameter, request.getParameter(parameter));
                    }
                }

                if (isSSE(request)) {
                    handleSSE(context, _request, _response);
                    return;
                }

                String query = _request.getParameter("q");
                if (query != null) {
                    handleRequest(query, context, _response);
                } else {
                    handleDefaultPage(context, _response);
                }
            } catch (ApplicationException e) {
                try {
                    handleApplicationException(_request, _response, e);
                } catch (ApplicationException ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }

        /**
         * Helper to select the appropriate push manager based on isMCP flag.
         */
        private SSEPushManager getAppropriatePushManager(boolean isMCP) {
            return isMCP ? MCPPushManager.getInstance() : SSEPushManager.getInstance();
        }

        private void handleSSE(org.tinystruct.application.Context context, Request<HttpServletRequest, ServletInputStream> request,
                               Response<HttpServletResponse, ServletOutputStream> response) throws IOException {
            response.addHeader(Header.CONTENT_TYPE.name(), "text/event-stream");
            response.addHeader(Header.CACHE_CONTROL.name(), "no-cache");
            response.addHeader(Header.CONNECTION.name(), "keep-alive");
            response.addHeader("X-Accel-Buffering", "no");

            try {
                String query = request.getParameter("q");
                boolean isMCP = false;
                if (query != null) {
                    query = StringUtilities.htmlSpecialChars(query);
                    if (query.equals(MCPSpecification.Endpoints.SSE)) {
                        isMCP = true;
                    }

                    Object call = ApplicationManager.call(query, context);
                    String sessionId = context.getId();
                    SSEPushManager pushManager = getAppropriatePushManager(isMCP);
                    SSEClient client = pushManager.register(sessionId, response);

                    if(call instanceof Builder) {
                        pushManager.push(sessionId, (Builder) call);
                    }
                    else if(call instanceof String) {
                        Builder builder = new Builder();
                        builder.parse((String)call);
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
            } catch (ApplicationException e) {
                throw new RuntimeException(e);
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
        private void handleRequest(String query, org.tinystruct.application.Context context, Response<HttpServletResponse, ServletOutputStream> response) throws IOException, ApplicationException {
            // Handle request
            query = StringUtilities.htmlSpecialChars(query);
            Object message = ApplicationManager.call(query, context);
            if (message != null) {
                if (message instanceof byte[]) {
                    byte[] bytes = (byte[]) message;
                    response.addHeader(Header.CONTENT_LENGTH.name(), String.valueOf(bytes.length));
                    response.get().write(bytes);
                } else {
                    try (BufferedWriter bufferedWriter = getWriter(response.get())) {
                        bufferedWriter.write(String.valueOf(message));
                    }
                }
            } else {
                try (BufferedWriter bufferedWriter = getWriter(response.get())) {
                    bufferedWriter.write("No response retrieved!");
                }
            }
        }

        /**
         * Handles the default page request.
         *
         * @param context  The application context
         * @param response The HTTP response object
         * @throws IOException if an I/O error occurs
         */
        private void handleDefaultPage(org.tinystruct.application.Context context, Response<HttpServletResponse, ServletOutputStream> response) throws IOException, ApplicationException {
            try (BufferedWriter bufferedWriter = getWriter(response.get())) {
                bufferedWriter.write(String.valueOf(ApplicationManager.call(settings.getOrDefault("default.home.page", "say/Praise the Lord."), context)));
            }
        }

        /**
         * Handles application exceptions.
         *
         * @param request  The HTTP servlet request
         * @param response The HTTP servlet response
         * @param e        The application exception
         */
        private void handleApplicationException(Request<HttpServletRequest, ServletInputStream> request, Response<HttpServletResponse, ServletOutputStream> response, ApplicationException e) throws ApplicationException {
            response.setStatus(ResponseStatus.valueOf(e.getStatus()));
            Session session = request.getSession();
            session.setAttribute("error", e);
            if (!Boolean.parseBoolean(settings.get("default.error.process"))) {
                String defaultErrorPage = settings.get("default.error.page");
                Reforward forward = new Reforward(request, response);
                forward.setDefault(defaultErrorPage.trim().isEmpty() ? "/?q=error" : "/?q=" + defaultErrorPage);
                forward.forward();
            }
        }

        @Override
        public void start() throws ApplicationException {
            settings = new Settings();
            charsetName = settings.getOrDefault("default.file.encoding", Charset.defaultCharset().name());
            settings.set("language", "zh_CN");
            settings.setIfAbsent("system.directory", path);
        }

        private boolean isSSL() {
            String sslEnabled = settings.get("ssl.enabled");
            return Boolean.parseBoolean(sslEnabled);
        }

        private String getProtocol(HttpServletRequest request) {
            return isSSL() ? "https://" : "http://";
        }

        private String getHost(HttpServletRequest request) {
            int serverPort = request.getServerPort();
            String defaultHostName = request.getServerName();
            String protocol = getProtocol(request);

            if (serverPort == 80) {
                return protocol + defaultHostName;
            } else {
                return protocol + defaultHostName + ":" + serverPort;
            }
        }

        /**
         * Use BufferedWriter for top efficiency.
         *
         * @param out OutputStream
         * @return BufferedWriter
         */
        private BufferedWriter getWriter(OutputStream out) {
            return new BufferedWriter(new OutputStreamWriter(out, Charset.forName(charsetName)));
        }

        @Override
        public void stop() {
            System.out.println("Stopping...");
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            final HttpServletRequest req = (HttpServletRequest) request;
            final HttpServletResponse resp = (HttpServletResponse) response;
            final long now = System.currentTimeMillis();
            resp.addHeader("Cache-Control", "public, max-age=86400, must-revalidate");
            resp.setDateHeader("Expires", now + 86400000L);

            // Process the static files in the project
            String uri = req.getRequestURI().replaceAll("^/+", "");
            if (uri.length() > 1) {
                if (uri.indexOf('/') != -1)
                    uri = uri.substring(0, uri.indexOf("/"));

                uri = uri.replace("%2e", ".");
                uri = uri.replace("%2f", "/");
                uri = uri.replace("%5c", "/");

                File resource = new File(uri);
                if (resource.exists()) {
                    chain.doFilter(request, response);
                } else {
                    this.service(req, resp);
                }
            } else
                this.service(req, resp);
        }

        private boolean isSSE(HttpServletRequest request) {
            String acceptHeader = request.getHeader("Accept");
            return acceptHeader != null && acceptHeader.contains("text/event-stream");
        }

        @Override
        public void destroy() {
            this.stop();
        }

        @Override
        public void run() {
            //TODO
        }
    }
}
