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

import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Context;
import org.tinystruct.http.Header;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.http.ResponseStatus;
import org.tinystruct.http.servlet.RequestBuilder;
import org.tinystruct.http.servlet.ResponseBuilder;
import org.tinystruct.system.*;
import org.tinystruct.system.util.StringUtilities;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import static org.tinystruct.Application.LANGUAGE;
import static org.tinystruct.Application.METHOD;
import static org.tinystruct.http.Constants.*;

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

        System.out.println("Initialize servlet config and starting...");
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        this.path = config.getServletContext().getRealPath("");

        try {
            this.start();
        } catch (ApplicationException e) {
            logger.severe(e.getMessage());
        }

        logger.info("Initialize filter config and starting...");
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding(this.charsetName);

        response.setContentType("text/html;charset=" + this.charsetName);
        response.setCharacterEncoding(this.charsetName);
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "No-cache");
        response.setDateHeader("Expires", 0);

        final Context context = new ApplicationContext();
        Request _request = new RequestBuilder(request);
        Response<ServletOutputStream> _response = new ResponseBuilder(response);

        context.setId(_request.getSession().getId());
        context.setAttribute(HTTP_REQUEST, _request);
        context.setAttribute(HTTP_RESPONSE, _response);
        context.setAttribute(HTTP_SCHEME, request.getScheme());
        context.setAttribute(HTTP_SERVER, request.getServerName());

        String lang = _request.getParameter("lang"), language = "";
        if (lang != null && lang.trim().length() > 0) {
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

        String hostName;
        if ((hostName = this.settings.get("default.hostname")) != null) {
            if (hostName.length() <= 3) {
                hostName = request.getServerName();
            }
        } else {
            hostName = request.getServerName();
        }

        String ssl_enabled, http_protocol = "http://";
        boolean ssl;
        if ((ssl_enabled = this.settings.get("ssl.enabled")) != null) {
            ssl = Boolean.parseBoolean(ssl_enabled);

            if (ssl) http_protocol = "https://";
        }

        context.setAttribute(HTTP_PROTOCOL, http_protocol);

        if (request.getServerPort() == 80)
            context.setAttribute(HTTP_HOST, http_protocol + hostName + url_prefix);
        else
            context.setAttribute(HTTP_HOST, http_protocol + hostName + ":" + request.getServerPort() + url_prefix);
        context.setAttribute(METHOD, request.getMethod());

        String query = _request.getParameter("q");
        try {
            if (query != null) {
                if (query.indexOf('?') != -1)
                    query = query.substring(0, query.indexOf('?'));
                query = StringUtilities.htmlSpecialChars(query);
                if (_request.getParameter("output") != null && "function".equalsIgnoreCase(_request.getParameter("output"))) {
                    ApplicationManager.call(query, context);
                } else {
                    Object message;
                    if ((message = ApplicationManager.call(query, context)) != null)
                        if (message instanceof byte[]) {
                            byte[] bytes = (byte[]) message;
                            _response.addHeader(Header.CONTENT_LENGTH.name(), bytes.length);
                            _response.get().write(bytes);
                            _response.get().close();
                        } else {
                            BufferedWriter bufferedWriter = getWriter(_response.get());
                            bufferedWriter.write(String.valueOf(message));
                            bufferedWriter.close();
                        }
                    else {
                        BufferedWriter bufferedWriter = getWriter(_response.get());
                        bufferedWriter.write("No response retrieved!");
                        bufferedWriter.close();
                    }
                }
            } else {
                BufferedWriter bufferedWriter = getWriter(_response.get());
                bufferedWriter.write(String.valueOf(ApplicationManager.call(this.settings.get("default.home.page"), context)));
                bufferedWriter.close();
            }
        } catch (ApplicationException e) {
            _response.setStatus(ResponseStatus.valueOf(e.getStatus()));
            try {
                HttpSession session = request.getSession();
                session.setAttribute("error", e);

                if (!Boolean.parseBoolean(this.settings.get("default.error.process"))) {
                    String defaultErrorPage = this.settings.get("default.error.page");

                    Reforward forward = new Reforward(_request, _response);
                    forward.setDefault(defaultErrorPage.trim().isEmpty() ? "/?q=error" : "/?q=" + defaultErrorPage);
                    forward.forward();
                }
            } catch (ApplicationException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void start() throws ApplicationException {
        this.settings = new Settings();
        if (this.settings.get("default.file.encoding") != null)
            this.charsetName = this.settings.get("default.file.encoding");

        if (this.charsetName == null || this.charsetName.trim().isEmpty())
            this.charsetName = System.getProperty("file.encoding");
        else
            System.setProperty("file.encoding", charsetName);

        this.settings.set("language", "zh_CN");
        if (this.settings.get("system.directory") == null)
            this.settings.set("system.directory", this.path);
    }

    /**
     * Use BufferedWriter for top efficiency.
     *
     * @param out OutputStream
     * @return BufferedWriter
     */
    private BufferedWriter getWriter(OutputStream out) {
        return new BufferedWriter(new OutputStreamWriter(out, Charset.defaultCharset()));
    }

    @Override
    public void stop() {
        System.out.println("Stopping...");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
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