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
import org.tinystruct.system.cli.CommandArgument;
import org.tinystruct.system.cli.CommandLine;
import org.tinystruct.system.cli.CommandOption;
import org.tinystruct.system.util.StringUtilities;
import org.tinystruct.transfer.http.ReadableByteChannelWrapper;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Dispatcher extends AbstractApplication {
    private static final Logger logger = Logger.getLogger(Dispatcher.class.getName());
    public static final String PROGRESS_CONTINUE = "NOT_CONTINUE";

    /**
     * Main functionality.
     *
     * @param args arguments
     */
    public static void main(String[] args) {

        // Process the system.directory.
        Settings config = new Settings();
        if (config.get("system.directory") == null) {
            config.set("system.directory", System.getProperty("user.dir"));
        }

        // Initialize the context.
        Context context = new ApplicationContext();

        // Install Dispatcher.
        ApplicationManager.install(new Dispatcher());

        if (args.length > 0) {
            // Detect the command.
            String command = null;
            if (!args[0].startsWith("--")) {
                command = args[0];
            }

            // Process attributes / options.
            String arg;
            for (int i = 0; i < args.length; i++) {
                arg = args[i];
                if (arg.startsWith("--")) {
                    if ((i + 1) < args.length) {
                        String value = args[++i].trim();
                        if (value.length() > 0 && !value.startsWith("--")) {
                            if (context.getAttribute(arg) != null) {
                                List<String> list;
                                if (context.getAttribute(arg) instanceof List) {
                                    list = (List<String>) context.getAttribute(arg);
                                } else {
                                    list = new ArrayList<String>();
                                    list.add(context.getAttribute(arg).toString());
                                }
                                list.add(value);
                                context.setAttribute(arg, list);
                            } else
                                context.setAttribute(arg, value);
                        }
                        else {
                            i--;
                            context.setAttribute(arg, true);
                        }
                    } else {
                        context.setAttribute(arg, true);
                    }
                } else
                    command = arg;
            }

            // Load the default import.
            StringBuilder defaultImportApplications = new StringBuilder(config.get("default.import.applications"));

            // Load the packages from import attribute.
            if (context.getAttribute("--import") != null) {
                if (context.getAttribute("--import") instanceof List) {
                    List<String> list = (List<String>) context.getAttribute("--import");
                    defaultImportApplications.append(StringUtilities.implode(";", list));
                } else
                    defaultImportApplications.append(context.getAttribute("--import"));
            }

            // Update the imports.
            config.set("default.import.applications", defaultImportApplications.toString());

            try {
                // Initialize the application manager.
                ApplicationManager.init(config);
            } catch (ApplicationException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }

//            System.out.println("config.get(\"default.import.applications\") = " + config.get("default.import.applications"));

            // Load all the configuration to context.
            Set<String> propertyNames = config.propertyNames();
            Iterator<String> iterator = propertyNames.iterator();
            String keyName;
            while (iterator.hasNext()) {
                keyName = iterator.next();
                if (!keyName.startsWith("["))
                    context.setAttribute(keyName, config.get(keyName));
            }

            execute(command, context);
        } else {
            try {
                // Initialize the application manager.
                ApplicationManager.init(config);
            } catch (ApplicationException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }

            System.out.println(new Dispatcher().help());
        }
    }

    private static void execute(String command, Context context) {
        if (context.getAttribute(PROGRESS_CONTINUE) != null && !Boolean.parseBoolean(context.getAttribute(PROGRESS_CONTINUE).toString())) {
            return;
        }

        try {
            // Execute the command with the context.
            if (command != null) {
                Object o = ApplicationManager.call(command, context);
                if (o != null) {
                    System.out.println(o);
                }
            }
            else
                System.out.println(new Dispatcher().help());

        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Install specific app
     */
    public void install() {
        if (this.context.getParameterValues("app") == null)
            throw new ApplicationRuntimeException("The appName could not be found in the context.");
        System.out.println("Installing...");
        String appName = this.context.getParameterValues("app").get(0);
        Application app = null;
        try {
            app = (Application) Class.forName(appName).newInstance();
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
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
            try {
                rbc.close();
            } catch (IOException e1) {
                e1.printStackTrace();
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
            try {
                rbc.close();
            } catch (IOException e) {
                e.printStackTrace();
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
        List<CommandOption> options = new ArrayList<CommandOption>();
        options.add(new CommandOption("app", "", "Packages to be installed"));
        this.commandLines.get("install").setOptions(options).setDescription("Install a package");

        this.setAction("update", "update");
        this.setAction("download", "download");
        List<CommandOption> opts = new ArrayList<>();
        opts.add(new CommandOption("url", "", "URL resource to be downloaded"));
        CommandOption opt = new CommandOption("http.proxyHost", "127.0.0.1", "Proxy host");
        opts.add(opt);
        opt = new CommandOption("http.proxyPort", "3128", "Proxy port");
        opts.add(opt);
        this.commandLines.get("download").setOptions(opts).setDescription("Download a resource from other servers");

        this.setAction("set", "setProperty");
        this.commandLines.get("set").setDescription("Set system property");

        this.setAction("exec", "exec");
        List<CommandOption> execOpts = new ArrayList<>();
        opt = new CommandOption("shell-commands", "", "Commands needs to be executed");
        execOpts.add(opt);
        this.commandLines.get("exec").setOptions(execOpts).setDescription("To execute native command(s)");

        this.setAction("say", "say");
        CommandArgument argument = new CommandArgument("words", "", "What you want to say");
        Set<CommandArgument<String, Object>> arguments = new HashSet<>();
        arguments.add(argument);
        this.commandLines.get("say").setArguments(arguments).setDescription("Output words");

        this.setAction("--settings", "settings");
        this.setAction("--logo", "logo");
        this.setAction("--version", "logo");

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
        this.context.setAttribute(PROGRESS_CONTINUE, true);
        return propertyName + "=" + System.getProperty(propertyName);
    }

    public void settings() {
        String[] names = this.context.getAttributeNames();
        Arrays.sort(names);
        for (String name : names) {
            logger.info(name + "=" + this.context.getAttribute(name));
        }
        this.context.setAttribute(PROGRESS_CONTINUE, true);
    }

    public String logo() {
        this.context.setAttribute(PROGRESS_CONTINUE, false);
        return "\n"
                + "  _/  '         _ _/  _     _ _/   \n"
                + "  /  /  /) (/ _)  /  /  (/ (  /  "
                + this.color(this.version(), FORE_COLOR.green) + "  \n"
                + "           /                       \n";
    }

    public String color(Object s, int color) {
        return "\u001b[" + color + "m" + s + "\u001b[0m";
    }

    public String version() {
        this.context.setAttribute(PROGRESS_CONTINUE, false);
        return ApplicationManager.VERSION;
    }

    @Override
    public String help() {
        StringBuilder builder = new StringBuilder("Usage: bin/dispatcher COMMAND [OPTIONS]\n");

        StringBuilder commands = new StringBuilder("Commands: \n");
        StringBuilder options = new StringBuilder("Options: \n");

        Map<String, CommandLine> commandLines = new HashMap<>();
        Collection<Application> apps = ApplicationManager.list();
        apps.forEach(app -> commandLines.putAll(app.getCommandLines()));

        OptionalInt longSizeCommand = commandLines.keySet().stream().mapToInt(String::length).max();
        int max = longSizeCommand.orElse(0);

        commandLines.forEach((s, commandLine) -> {
            String command = commandLine.getCommand();
            String description = commandLine.getDescription();
            if(command.startsWith("--")) {
                options.append("\t").append(StringUtilities.rightPadding(command, max, ' ')).append("\t").append(description).append("\n");
            }
            else {
                commands.append("\t").append(StringUtilities.rightPadding(command, max, ' ')).append("\t").append(description).append("\n");
            }
        });

        builder.append(commands).append("\n");
        builder.append(options);

        builder.append("\nRun 'bin/dispatcher COMMAND --help' for more information on a command.");
        return builder.toString();
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