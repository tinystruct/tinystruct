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
package org.tinystruct.handler;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Context;
import org.tinystruct.http.*;
import org.tinystruct.http.servlet.RequestBuilder;
import org.tinystruct.http.servlet.ResponseBuilder;
import org.tinystruct.system.*;
import org.tinystruct.system.util.StringUtilities;

import java.io.*;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.tinystruct.Application.LANGUAGE;
import static org.tinystruct.Application.METHOD;
import static org.tinystruct.http.Constants.*;

/**
 * DefaultHandler is responsible for handling HTTP requests and managing the application's lifecycle.
 */
public class DefaultHandler extends HttpServlet implements Bootstrap, Filter {
    private static final Logger logger = Logger.getLogger(DefaultHandler.class.getName());
    private static final long serialVersionUID = 0;
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

        final Context context = new ApplicationContext();
        Request<HttpServletRequest, ServletInputStream> _request = new RequestBuilder(request, isSSL());
        Response<HttpServletResponse, ServletOutputStream> _response = new ResponseBuilder(response);
        try {
            context.setId(_request.getSession().getId());
            context.setAttribute(HTTP_REQUEST, _request);
            context.setAttribute(HTTP_RESPONSE, _response);
            context.setAttribute(HTTP_SCHEME, request.getScheme());
            context.setAttribute(HTTP_SERVER, request.getServerName());
            context.setAttribute(HTTP_PROTOCOL, getProtocol(request));

            String lang = _request.getParameter("lang"), language = "";
            if (lang != null && !lang.trim().isEmpty()) {
                String name = lang.replace('-', '_');

                if (Language.support(name) && !lang.equalsIgnoreCase(this.settings.get("language"))) {
                    String[] local = name.split("_");
                    context.setAttribute(LANGUAGE, name);
                    language = "lang=" + local[0] + "-" + local[1].toUpperCase() + "&";
                }
            }

            String url_prefix = "/";
            if (this.settings.get("default.url_rewrite") != null && !"enabled".equalsIgnoreCase(this.settings.get("default.url_rewrite"))) {
                url_prefix = "/?" + language + "q=";
            }

            context.setAttribute(HTTP_HOST, getHost(request) + url_prefix);
            context.setAttribute(METHOD, request.getMethod());

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
     * Handles the HTTP request by processing the query.
     *
     * @param query     The query string
     * @param context   The application context
     * @param response The HTTP response object
     * @throws IOException if an I/O error occurs
     */
    private void handleRequest(String query, Context context, Response<HttpServletResponse, ServletOutputStream> response) throws IOException, ApplicationException {
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
     * @param context   The application context
     * @param response The HTTP response object
     * @throws IOException if an I/O error occurs
     */
    private void handleDefaultPage(Context context, Response<HttpServletResponse, ServletOutputStream> response) throws IOException, ApplicationException {
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
    private void handleApplicationException(Request request, Response response, ApplicationException e) throws ApplicationException {
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

    @Override
    public void destroy() {
        this.stop();
    }

    @Override
    public void run() {
        //TODO
    }
}
