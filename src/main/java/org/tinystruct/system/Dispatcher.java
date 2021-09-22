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

import org.tinystruct.*;
import org.tinystruct.application.Context;
import org.tinystruct.transfer.http.ReadableByteChannelWrapper;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Dispatcher extends AbstractApplication {
    private static final Logger logger = Logger.getLogger(Dispatcher.class.getName());

    private final String help = "Usage:\tdispatcher [--attributes] [actions[/args...]...]\n"
            + "\twhere attributes include any custom attributes you defined in context \n"
            + "\tor keypair parameters are going to be passed by context,\n " + "\tsuch as: \n"
            + "\t--http.proxyHost=127.0.0.1 or --http.proxyPort=3128 or --param=value\n\n" + "\tSystem actions are:\n"
            + "\t\tinstall\t\tInstall specific app\n" + "\t\tupdate\t\tUpdate framework to the latest version\n"
            + "\t\tdownload\tA download org.tinystruct.data.tools\n\t\t\t\t--url URL\n"
            + "\t\tset\t\tSet system property\n"
            + "\t\texec\t\tTo execute native command(s)\n\t\t\t\t--shell-command Commands\n\n"
            + "\tSystem attributes are:\n" + "\t\t--import-applications\tImport apps class your expected\n"
            + "\t\t--settings\t\tPrint all attributes set\n" + "\t\t--logo\t\tPrint the logo of the framework\n"
            + "\t\t--help\t\tPrint help\n";

    /**
     * Main functionality.
     *
     * @param args arguments
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            ApplicationManager.install(new Dispatcher());

            Settings config = new Settings("/application.properties");
            if (config.get("system.directory") == null) {
                config.set("system.directory", System.getProperty("user.dir"));
            }

            StringBuilder default_import_applications = new StringBuilder(config.get("default.import.applications"));
            String[] __arg;
            for (String arg : args) {
                if (arg.startsWith("--") && arg.indexOf('=') >= 3) {
                    __arg = arg.split("=");

                    if ("--import-applications".equalsIgnoreCase(__arg[0])) {
                        if (default_import_applications.length() >= 1)
                            default_import_applications.append(";").append(__arg[1]);
                        else
                            default_import_applications.append(__arg[1]);

                        break;
                    }
                }
            }

            config.set("default.import.applications", default_import_applications.toString());

            try {
                ApplicationManager.init(config);
            } catch (ApplicationException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }

            Context context = new ApplicationContext();
            Set<String> propertyNames = config.propertyNames();
            Iterator<String> iterator = propertyNames.iterator();
            String keyName;
            while (iterator.hasNext()) {
                keyName = iterator.next();
                if (!keyName.startsWith("["))
                    context.setAttribute(keyName, config.get(keyName));
            }

            String attributeName;
            int endIndex;
            for (String arg : args) {
                try {
                    if (arg.startsWith("--") && arg.indexOf('=') >= 3) {
                        endIndex = arg.indexOf('=');
                        if (endIndex != -1) {
                            attributeName = arg.substring(0, endIndex);
                            context.setAttribute(attributeName, arg.substring(endIndex + 1));
                            if ("--shell-command".equalsIgnoreCase(attributeName)) {
                                ApplicationManager.call("exec", context);
                            } else {
                                System.out.println(ApplicationManager.call("set/" + attributeName, context));
                            }
                        } else {
                            System.out.println("Context attribute value should not be empty.");
                            break;
                        }
                    }
                } catch (ApplicationException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            }

            for (String arg : args) {
                try {
                    if (!arg.startsWith("--") || arg.indexOf('=') == -1) {
                        Object o = ApplicationManager.call(arg, context);
                        if (o != null) {
                            System.out.println(o);
                        }
                    }
                } catch (ApplicationException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        } else {
            System.out.println(new Dispatcher().help());
        }
    }

    public void install() {
        if (this.context.getParameterValues("app") == null)
            throw new ApplicationRuntimeException("The appName could not be found in the context.");
        System.out.println("Installing...");
        String appName = this.context.getParameterValues("app").get(0);
        Application app = null;
        try {
            app = (Application) Class.forName(appName).newInstance();
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (InstantiationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        if (null != ApplicationManager.getConfiguration()) {
            assert app != null;
            app.setConfiguration(ApplicationManager.getConfiguration());
        }

        assert app != null;
        ApplicationManager.install(app);

        System.out.println("Completed!");
    }

    public String update() {
        System.out.println("Updating...");
        try {
            this.download(new URL("https://repo1.maven.org/maven2/org/tinystruct/tinystruct/" + version()
                    + "/tinystruct-" + version() + "-jar-with-dependencies.jar"), ".");
        } catch (ApplicationException e) {
            return e.toString();
        } catch (MalformedURLException e) {
            return e.toString();
        }
        return "\r\nCompleted!";
    }

    public void download(URL uri, String destination) throws ApplicationException {
        ReadableByteChannel rbc;
        try {
            rbc = new ReadableByteChannelWrapper(uri);
        } catch (Exception e) {
            throw new ApplicationException("Could not be downloaded:" + e.getMessage(), e.getCause());
        }

        if (destination.trim().length() <= 1) {
            destination = uri.toString().replaceAll("[http://|https://|/]", "-");
        }

        String path = new File("").getAbsolutePath() + File.separatorChar + destination;
        Path dest = Paths.get(path);
        try {
            Files.createDirectories(dest.getParent());
            if (!Files.exists(dest))
                Files.createFile(dest);
        } catch (IOException e) {
            if (rbc != null) {
                try {
                    rbc.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            throw new ApplicationException(e.getMessage(), e.getCause());
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        } finally {
            if (fos != null)
                try {
                    fos.close();
                } catch (IOException e) {
                    throw new ApplicationException(e.getMessage(), e.getCause());
                }
            if (rbc != null) {
                try {
                    rbc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void download() throws ApplicationException {
        if (this.context.getAttribute("--url") != null) {
            URL uri;
            try {
                uri = new URL(this.context.getAttribute("--url").toString());
                this.download(uri, uri.getFile());
            } catch (MalformedURLException e) {
                throw new ApplicationException(e.getMessage(), e.getCause());
            }
        }
    }

    public void exec() throws ApplicationException {
        if (this.context.getAttribute("--shell-command") == null) {
            throw new ApplicationException("Invalid shell command");
        }

        String cmd = this.context.getAttribute("--shell-command").toString();
        try {
            String line;
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                System.out.println(line);
            }
            input.close();

            BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = error.readLine()) != null) {
                System.out.println(line);
            }
            error.close();
            p.destroy();
        } catch (Exception err) {
            throw new ApplicationException(err.getMessage(), err);
        }
    }

    public void init() {
        this.setAction("install", "install");
        this.setAction("update", "update");
        this.setAction("download", "download");
        this.setAction("set", "setProperty");
        this.setAction("exec", "exec");
        this.setAction("--settings", "settings");
        this.setAction("--logo", "logo");
        this.setAction("--version", "logo");
        this.setAction("--help", "help");
        this.setAction("say", "say");

        this.setTemplateRequired(false);
    }

    public String say(String words) {
        return words;
    }

    public String setProperty(String propertyName) {
        if (this.context.getAttribute(propertyName) == null)
            throw new ApplicationRuntimeException("The key " + propertyName + " could not be found in the context.");
        String property = this.context.getAttribute(propertyName).toString();
        propertyName = propertyName.substring(2);
        System.setProperty(propertyName, property);
        return propertyName + "=" + System.getProperty(propertyName);
    }

    public void settings() {
        String[] names = this.context.getAttributeNames();
        Arrays.sort(names);
        for (String name : names) {
            logger.info(name + "=" + this.context.getAttribute(name));
        }
    }

    public String logo() {
        return "\n" + "  _/  '         _ _/  _     _ _/   \n" + "  /  /  /) (/ _)  /  /  (/ (  /  "
                + this.color(this.version(), FORE_COLOR.green) + "  \n" + "           /                       \n";
    }

    public String color(Object s, int color) {
        return "\u001b[" + color + "m" + s + "\u001b[0m";
    }

    public String help() {
        return help;
    }

    public String version() {
        return ApplicationManager.VERSION;
    }

    private static class FORE_COLOR {
        public static final int black = 30;
        public static final int red = 31;
        public static final int green = 32;
        public static final int yellow = 33;
        public static final int blue = 34;
        public static final int magenta = 35;
        public static final int cyan = 36;
        public static final int white = 37;
    }

    private static class BACKGROUND_COLOR {
        public static final int black = 40;
        public static final int red = 41;
        public static final int green = 42;
        public static final int yellow = 43;
        public static final int blue = 44;
        public static final int magenta = 45;
        public static final int cyan = 46;
        public static final int white = 47;
    }

}