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

import org.apache.catalina.*;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.handler.DefaultHandler;
import org.tinystruct.handler.Reforward;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.http.Session;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.tinystruct.http.Constants.HTTP_REQUEST;
import static org.tinystruct.http.Constants.HTTP_RESPONSE;

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
}
