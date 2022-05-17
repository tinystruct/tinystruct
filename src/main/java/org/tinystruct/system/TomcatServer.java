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

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.handler.DefaultHandler;
import org.tinystruct.handler.Reforward;
import org.tinystruct.system.cli.CommandOption;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TomcatServer extends AbstractApplication implements Bootstrap {
    private Logger logger = Logger.getLogger(TomcatServer.class.getName());

    public TomcatServer(){

    }

    public void init() {
        this.setAction("start", "start");
        List<CommandOption> options = new ArrayList<CommandOption>();
        options.add(new CommandOption("server-port", "", "Server port"));
        this.commandLines.get("start").setOptions(options).setDescription("Start a Tomcat server");

        this.setAction("error", "exceptionCaught");
    }

    public void start() throws ApplicationException {
        System.out.println(ApplicationManager.call("--logo", this.context));

        long start = System.currentTimeMillis();
        String webappDirLocation = ".";
        Tomcat tomcat = new Tomcat();
        // The port that we should run on can be set into an environment variable
        // Look for that variable and default to 8080 if it isn't there.
        int webPort;
        if (this.context.getAttribute("--server-port") != null) {
            webPort = Integer.parseInt(this.context.getAttribute("--server-port").toString());
        }
        else
            webPort = 8080;

        tomcat.setPort(webPort);
        try {
            Context ctx = tomcat.addWebapp("/", new File(webappDirLocation).getAbsolutePath());
            logger.info("Configuring app with basedir: "
                    + new File(webappDirLocation).getAbsolutePath());

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
            logger.info("Server started in "+(System.currentTimeMillis() - start)/1000 + " seconds");
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }

    }

    @Override
    public void stop() {
    }

    public Object exceptionCaught()
            throws ApplicationException {
        HttpServletRequest request = (HttpServletRequest) this.context.getAttribute("HTTP_REQUEST");
        HttpServletResponse response = (HttpServletResponse) this.context.getAttribute("HTTP_RESPONSE");

        Reforward reforward = new Reforward(request, response);
        this.setVariable("from", reforward.getFromURL());

        HttpSession session = request.getSession();
        if (session.getAttribute("error") != null) {
            ApplicationException exception = (ApplicationException) session.getAttribute("error");

            String message = exception.getRootCause().getMessage();
            if (message != null)
                this.setVariable("exception.message", message);
            else
                this.setVariable("exception.message", "Unknown error");

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
}
