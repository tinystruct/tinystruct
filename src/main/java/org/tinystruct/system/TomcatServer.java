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
import org.tinystruct.system.cli.CommandOption;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TomcatServer extends AbstractApplication implements Bootstrap {
    private final Logger logger = Logger.getLogger(TomcatServer.class.getName());
    private boolean started = false;

    public TomcatServer() {
    }

    public void init() {
        this.setAction("start", "start");
        List<CommandOption> options = new ArrayList<CommandOption>();
        options.add(new CommandOption("server-port", "", "Server port"));
        this.commandLines.get("start").setOptions(options).setDescription("Start a Tomcat server");

        this.setAction("error", "exceptionCaught");
        this.setTemplateRequired(false);
    }

    public void start() throws ApplicationException {
        if (started) return;

        System.out.println(ApplicationManager.call("--logo", this.context));

        final long start = System.currentTimeMillis();
        final String webappDirLocation = ".";
        final Tomcat tomcat = new Tomcat();
        // The port that we should run on can be set into an environment variable
        // Look for that variable and default to 8080 if it isn't there.
        int webPort;
        if (this.context.getAttribute("--server-port") != null) {
            webPort = Integer.parseInt(this.context.getAttribute("--server-port").toString());
        } else {
            webPort = 8080;
        }

        tomcat.setPort(webPort);
        tomcat.setAddDefaultWebXmlToWebapp(false);

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
            logger.info("Server startup in " + (System.currentTimeMillis() - start) + " ms");

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

        // Welcome files
        ctx.addWelcomeFile("index.html");
    }

    @Override
    public void stop() {
    }

    public Object exceptionCaught() throws ApplicationException {
        Request request = (Request) this.context.getAttribute("HTTP_REQUEST");
        Response response = (Response) this.context.getAttribute("HTTP_RESPONSE");

        Reforward reforward = new Reforward(request, response);
        this.setVariable("from", reforward.getFromURL());

        Session session = request.getSession();
        if (session.getAttribute("error") != null) {
            ApplicationException exception = (ApplicationException) session.getAttribute("error");

            String message = exception.getRootCause().getMessage();
            if (message != null) this.setVariable("exception.message", message);
            else this.setVariable("exception.message", "Unknown error");

            logger.severe(exception.toString());
            exception.printStackTrace();
            return this.getVariable("exception.message").getValue().toString();
        } else {
            reforward.forward();
        }

        return "This request is forbidden!";
    }

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
                Valve valves[] = null;
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
