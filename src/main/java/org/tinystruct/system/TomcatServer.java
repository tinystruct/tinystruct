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

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;

import javax.servlet.ServletException;
import java.io.File;
import java.util.logging.Logger;

public class TomcatServer extends AbstractApplication implements Bootstrap {
    private Logger logger = Logger.getLogger(TomcatServer.class.getName());

    public void init() {
        this.setAction("--start-server", "start");
    }

    public void start() throws ApplicationException {
        System.out.println("Starting...");
        String webappDirLocation = ".";

        Tomcat tomcat = new Tomcat();
        // The port that we should run on can be set into an environment variable
        // Look for that variable and default to 8080 if it isn't there.
        String webPort = null;
        if (this.context.getAttribute("--server-port") != null) {
            webPort = this.context.getAttribute("--server-port")
                    .toString();
        }
        if (webPort == null || webPort.isEmpty()) {
            webPort = "8080";
        }

        tomcat.setPort(Integer.valueOf(webPort));
        try {
            tomcat.addWebapp("/", new File(webappDirLocation).getAbsolutePath());
            logger.info("Configuring app with basedir: "
                    + new File(webappDirLocation).getAbsolutePath());

            tomcat.start();
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }

    }

    @Override
    public void stop() {
    }

    public String version() {
        return "";
    }
}
