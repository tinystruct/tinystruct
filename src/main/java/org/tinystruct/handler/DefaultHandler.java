/*******************************************************************************
 * Copyright  (c) 2013, 2017 James Mover Zhou
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
import org.tinystruct.system.*;
import org.tinystruct.system.util.StringUtilities;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;

public class DefaultHandler extends HttpServlet implements Bootstrap, Filter {
    private static final long serialVersionUID = 0;
    public static final String HTTP_REQUEST = "HTTP_REQUEST";
    public static final String HTTP_RESPONSE = "HTTP_RESPONSE";
    public static final String HTTP_SCHEME = "HTTP_SCHEME";
    public static final String HTTP_SERVER = "HTTP_SERVER";
    public static final String HTTP_HOST = "HTTP_HOST";
    public static final String METHOD = "METHOD";
    private String charsetName;
    private Configuration<String> settings;
    private boolean ignore;
    private String path;

    @Override
    public void init(ServletConfig config) {
        this.path = config.getServletContext().getRealPath("");
        try {
            this.start();
        } catch (ApplicationException e) {
            e.printStackTrace();
        }

        System.out.println("Initialize servlet config and starting...");
    }

    public void init(FilterConfig config) throws ServletException {
        this.path = config.getServletContext().getRealPath("");
        String value = config.getInitParameter("ignore");

        if ("true".equalsIgnoreCase(value)) {
            this.ignore = true;
        } else
            this.ignore = "yes".equalsIgnoreCase(value);

        try {
            this.start();
        } catch (ApplicationException e) {
            e.printStackTrace();
        }

        System.out.println("Initialize filter config and starting...");
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(this.charsetName);

        response.setContentType("text/html;charset=" + this.charsetName);
        response.setCharacterEncoding(this.charsetName);
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "No-cache");
        response.setDateHeader("Expires", 0);

        final Context context = new ApplicationContext();
        context.setAttribute(HTTP_REQUEST, request);
        context.setAttribute(HTTP_RESPONSE, response);
        context.setAttribute(HTTP_SCHEME, request.getScheme());
        context.setAttribute(HTTP_SERVER, request.getServerName());

        String lang = request.getParameter("lang"), language = "";
        if (lang != null && lang.trim().length() > 0) {
            String name = lang.replace('-', '_');

            if (Language.support(name) && !lang.equalsIgnoreCase(this.settings.get("language"))) {
                String[] local = name.split("_");
                context.setAttribute("language", name);
                language = "lang=" + local[0] + "-" + local[1].toUpperCase() + "&";
            }
        } else
            context.removeAttribute("language");

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
        boolean ssl = false;
        if ((ssl_enabled = this.settings.get("ssl.enabled")) != null) {
            ssl = Boolean.parseBoolean(ssl_enabled);

            if (ssl) http_protocol = "https://";
        }

        if (request.getServerPort() == 80)
            context.setAttribute(HTTP_HOST, http_protocol + hostName + url_prefix);
        else
            context.setAttribute(HTTP_HOST, http_protocol + hostName + ":" + request.getServerPort() + url_prefix);
        context.setAttribute(METHOD, request.getMethod());

        String query = request.getParameter("q");
        try {
            if (query != null) {
//                query = new String(query.getBytes("ISO8859-1"), this.charsetName);
                if (query.indexOf('?') != -1)
                    query = query.substring(0, query.indexOf('?'));
                query = StringUtilities.htmlSpecialChars(query);
                if (request.getParameter("output") != null && "function".equalsIgnoreCase(request.getParameter("output"))) {
                    ApplicationManager.call(query, context);
                } else {
                    Object message;
                    if ((message = ApplicationManager.call(query, context)) != null)
                        response.getWriter().println(message);
                    else
                        response.getWriter().println("No response retrieved!");
                    response.getWriter().close();
                }
            } else {
                response.getWriter().println(ApplicationManager.call(this.settings.get("default.home.page"), context));
                response.getWriter().close();
            }
        } catch (ApplicationException e) {
            response.setHeader("Status", e.getStatus());
            try {
                HttpSession session = request.getSession();
                session.setAttribute("error", e);

                if (!Boolean.valueOf(this.settings.get("default.error.process"))) {
                    String defaultErrorPage = this.settings.get("default.error.page");

                    Reforward forward = new Reforward(request, response);
                    forward.setDefault(defaultErrorPage.trim().length() == 0 ? "/?q=error" : "/?q=" + defaultErrorPage);
                    forward.forward();
                }
            } catch (ApplicationException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws ApplicationException {
        this.settings = new Settings("/application.properties");
        if (this.settings.get("default.file.encoding") != null)
            this.charsetName = (this.settings.get("default.file.encoding"));

        if (this.charsetName == null || this.charsetName.trim().length() == 0)
            this.charsetName = System.getProperty("file.encoding");
        else
            System.setProperty("file.encoding", charsetName);

        this.settings.set("language", "zh_CN");
        if (this.settings.get("system.directory") == null)
            this.settings.set("system.directory", this.path);

        ApplicationManager.init(this.settings);
    }

    public void stop() {
        System.out.println("Stopping...");
    }

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse resp = (HttpServletResponse) response;
        final long now = System.currentTimeMillis();
        resp.addHeader("Cache-Control", "public, max-age=86400, must-revalidate");
        resp.setDateHeader("Expires", now + 86400000L);

        String uri = req.getRequestURI().replaceAll("^/+", "");
        if (uri.length() > 1) {
            System.out.println("Request URI:" + uri);

            if (uri.indexOf('/') != -1)
                uri = uri.substring(0, uri.indexOf("/"));

            File resource = new File(uri);
            if (resource.exists()) {
                System.out.println("Resource exists:" + uri);

                chain.doFilter(request, response);
            } else {
                System.out.println("Resource exists:" + uri);
                this.service(req, resp);
            }
        } else
            this.service(req, resp);
    }

    public void destroy() {
        this.stop();
    }

    @Override
    public void run() {
        //TODO
    }
}