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

import com.sun.jna.Platform;
import org.tinystruct.*;
import org.tinystruct.application.Context;
import org.tinystruct.data.DatabaseOperator;
import org.tinystruct.system.cli.CommandArgument;
import org.tinystruct.system.cli.CommandLine;
import org.tinystruct.system.cli.CommandOption;
import org.tinystruct.system.cli.Kernel32;
import org.tinystruct.system.util.StringUtilities;
import org.tinystruct.system.util.URLResourceLoader;
import org.tinystruct.transfer.http.ReadableByteChannelWrapper;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Dispatcher extends AbstractApplication implements RemoteDispatcher {
    private static final Logger logger = Logger.getLogger(Dispatcher.class.getName());
    public static final String OK = "OK!";
    private boolean virtualTerminal;

    /**
     * Main functionality.
     *
     * @param args arguments
     */
    public static void main(String[] args) throws RemoteException {
        // Process the system.directory.
        Settings config = new Settings();
        if (config.get("system.directory") == null || config.get("system.directory").equals("")) {
            config.set("system.directory", System.getProperty("user.dir"));
        }

        // Initialize the context.
        Context context = new ApplicationContext();

        // Initialize the dispatcher.
        Dispatcher dispatcher = new Dispatcher();

        // Install Dispatcher.
        ApplicationManager.install(dispatcher, config);

        if (args.length > 0) {
            // Detect the command.
            String command = null;
            int start = 0;
            if (!args[0].startsWith("--") && !args[0].startsWith("-")) {
                command = args[0];
                start = 1;
            }

            // Process attributes / options.
            String arg;
            for (int i = start; i < args.length; i++) {
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
                                    list = new ArrayList<>();
                                    list.add(Objects.requireNonNull(context.getAttribute(arg)).toString());
                                }
                                assert list != null;
                                list.add(value);
                                context.setAttribute(arg, list);
                            } else
                                context.setAttribute(arg, value);
                        } else {
                            i--;
                            context.setAttribute(arg, true);
                        }
                    } else {
                        context.setAttribute(arg, true);
                    }
                } else if (arg.startsWith("-D")) {
                    String[] args0 = arg.substring(2).split("=");
                    System.setProperty(args0[0], args0[1]);
                } else
                    command = arg;
            }

            boolean remote = false;
            RemoteDispatcher remoteDispatcher = null;
            if (context.getAttribute("--host") != null) {
                Registry registry;
                try {
                    registry = LocateRegistry.getRegistry(Objects.requireNonNull(context.getAttribute("--host")).toString());
                    remoteDispatcher = (RemoteDispatcher) registry.lookup("Dispatcher");
                    if (remoteDispatcher != null) {
                        remote = true;
                    }
                } catch (RemoteException e) {
                    System.err.println(e.getCause().getMessage());
                    System.exit(0);
                } catch (NotBoundException e) {
                    System.err.println(e.getCause().getMessage());
                    System.exit(0);
                }
            }

            boolean disableHelper = false;
            if (context.getAttribute("--allow-remote-access") != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        dispatcher.bind(dispatcher);
                    }
                }).start();

                disableHelper = true;
            }

            // Load the packages from import attribute.
            if (context.getAttribute("--import") != null && !Boolean.parseBoolean(Objects.requireNonNull(context.getAttribute("--import")).toString())) {
                List<String> list;
                if (context.getAttribute("--import") instanceof List) {
                    list = (List<String>) context.getAttribute("--import");
                } else {
                    list = List.of(Objects.requireNonNull(context.getAttribute("--import")).toString());
                }

                if (remote) {
                    remoteDispatcher.install(config, list);
                } else
                    dispatcher.install(config, list);
            }

            if (remote) {
                System.out.print(remoteDispatcher.execute(command, context));
                System.exit(0);
            }

            if (!disableHelper || command != null) {
                // Execute a local method.
                dispatcher.execute(command, context);
            }
        } else {
            System.out.println(dispatcher.help());
        }
    }

    @Override
    public Object execute(String command, Context context) throws RemoteException {
        Object output = OK;
        try {
            // Execute the command with the context.
            if (command != null) {
                Object o = ApplicationManager.call(command, context);
                if (o != null) {
                    System.out.println(o);
                    return o;
                }
            } else {
                Dispatcher dispatcher = (Dispatcher) ApplicationManager.get(Dispatcher.class.getName());

                if (context.getAttribute("--version") != null) {
                    output = dispatcher.version();
                } else if (context.getAttribute("--logo") != null) {
                    output = dispatcher.logo();
                } else if (context.getAttribute("--settings") != null) {
                    output = ApplicationManager.call("--settings", context);
                } else {
                    output = dispatcher.help();
                }
                System.out.println(output);
            }

        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return output;
    }

    @Override
    public void install(Configuration<String> config, List<String> list) throws RemoteException {
        // Load the default import.
        // Merge the packages from list.
        // Update the imports.
        String defaults;
        if (!(defaults = config.get("default.import.applications")).equals(""))
            defaults += ";";

        config.set("default.import.applications", defaults + String.join(";", list));

        try {
            // Initialize the application manager with the configuration.
            ApplicationManager.init(config);
        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Install specific app
     */
    public void install() {
        String appName;
        if ((appName = (String) this.context.getAttribute("--app")) == null)
            throw new ApplicationRuntimeException("The app could not be found in the context.");

        System.out.println("Installing...");

        try {
            this.install(getConfiguration(), List.of(appName));
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        System.out.println("Completed installation for " + appName + "!");
    }

    public String update() {
        System.out.print("Checking...");
        try {
            URLResourceLoader loader = new URLResourceLoader(new URL("https://repo1.maven.org/maven2/org/tinystruct/tinystruct/maven-metadata.xml"));
            StringBuilder content = loader.getContent();
            String latestVersion = content.substring(content.indexOf("<latest>") + 8, content.indexOf("</latest>"));
            if (latestVersion.equalsIgnoreCase(ApplicationManager.VERSION))
                return "\r" + this.color("You are already using the latest available Dispatcher version " + ApplicationManager.VERSION + ".", FORE_COLOR.green);
            System.out.print("\rGot a new version " + latestVersion + "...");
            System.out.print("\rDownloading...");
            this.download(new URL("https://repo1.maven.org/maven2/org/tinystruct/tinystruct/" + latestVersion
                    + "/tinystruct-" + latestVersion + "-jar-with-dependencies.jar"), "lib/tinystruct-" + latestVersion + "-jar-with-dependencies.jar");
            System.out.println("\nDownloaded (" + this.color(latestVersion, FORE_COLOR.green) + ").");
            System.out.print("\rUpdating..");
            ApplicationManager.generateDispatcherCommand(latestVersion, true);
        } catch (ApplicationException | MalformedURLException e) {
            return e.getMessage();
        }
        return "\rCompleted! \nRun 'bin/dispatcher --help' for more information.";
    }

    public void download(URL uri, String destination) throws ApplicationException {
        try (ReadableByteChannel rbc = new ReadableByteChannelWrapper(uri)) {
            if (destination.trim().length() <= 1) {
                destination = uri.toString().replaceAll("http://|https://|/", "+");
            }

            String path = new File("").getAbsolutePath() + File.separatorChar + destination;
            Path dest = Paths.get(path);
            try {
                Path parent = dest.getParent();
                if (parent != null)
                    Files.createDirectories(parent);
                if (!Files.exists(dest))
                    Files.createFile(dest);
            } catch (IOException e) {
                throw new ApplicationException(e.getMessage(), e.getCause());
            }

            try (FileOutputStream fos = new FileOutputStream(path)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (IOException e) {
                throw new ApplicationException(e.getMessage(), e.getCause());
            }
        } catch (Exception e) {
            throw new ApplicationException("Could not be downloaded:" + e.getMessage(), e.getCause());
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
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()));
            while ((line = input.readLine()) != null) {
                System.out.println(line);
            }
            input.close();

            BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream(), Charset.defaultCharset()));
            while ((line = error.readLine()) != null) {
                System.out.println(line);
            }
            error.close();
            p.destroy();
        } catch (Exception err) {
            throw new ApplicationException(err.getMessage(), err);
        }
    }

    public void executeQuery() throws ApplicationException {
        if (this.context.getAttribute("--query") == null) {
            throw new ApplicationException("Invalid Query.");
        }

        String query = this.context.getAttribute("--query").toString();
        try (DatabaseOperator operator = new DatabaseOperator()) {
            operator.createStatement(false);
            operator.execute(query);
        } catch (ApplicationException e) {
            System.err.println(e.getCause().getMessage());
        }
    }

    public void init() {
        this.setAction("install", "install");
        List<CommandOption> options = new ArrayList<CommandOption>();
        options.add(new CommandOption("app", "", "Packages to be installed"));
        this.commandLines.get("install").setOptions(options).setDescription("Install a package");

        this.setAction("update", "update");
        this.commandLines.get("update").setDescription("Update for latest version");

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
        opt = new CommandOption("shell-command", "", "Commands needs to be executed");
        execOpts.add(opt);
        this.commandLines.get("exec").setOptions(execOpts).setDescription("To execute native command(s)");

        this.setAction("say", "say");
        CommandArgument<String, Object> argument = new CommandArgument<>("words", "", "What you want to say");
        Set<CommandArgument<String, Object>> arguments = new HashSet<>();
        arguments.add(argument);
        this.commandLines.get("say").setArguments(arguments).setDescription("Output words");

        this.setAction("--import", "");
        this.commandLines.get("--import").setDescription("Import application");

        this.setAction("--settings", "settings");
        this.commandLines.get("--settings").setDescription("Print settings");

        this.setAction("--logo", "logo");
        this.commandLines.get("--logo").setDescription("Print logo");

        this.setAction("--allow-remote-access", "");
        this.commandLines.get("--allow-remote-access").setDescription("Allow to be accessed remotely");

        this.setAction("--host", "");
        this.commandLines.get("--host").setDescription("Host name / IP");

        this.setAction("--version", "version");
        this.commandLines.get("--version").setDescription("Print version");

        this.setAction("sql-query", "executeQuery");
        List<CommandOption> sqlOpts = new ArrayList<>();
        opt = new CommandOption("query", "", "Query needs to be executed");
        sqlOpts.add(opt);
        this.commandLines.get("sql-query").setOptions(sqlOpts).setDescription("SQL query needs to be executed.");

        this.setTemplateRequired(false);
    }

    private void bind(Dispatcher dispatcher) {
        try {
            String name = "Dispatcher";
            RemoteDispatcher stub =
                    (RemoteDispatcher) UnicastRemoteObject.exportObject(dispatcher, 0);
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.bind(name, stub);
            logger.info("You will be allowed to send your command to the machine with --host option.");
        } catch (Exception e) {
            System.err.println(e.getCause().getMessage());
        }
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
        return propertyName + ":" + System.getProperty(propertyName);
    }

    public StringBuilder settings() {
        String[] names = this.config.propertyNames().toArray(new String[0]);
        Arrays.sort(names);
        StringBuilder settings = new StringBuilder();
        for (String name : names) {
            settings.append(name).append(":").append(this.config.get(name)).append("\n");
        }
        return settings;
    }

    public String logo() {
        return "\n"
                + "  _/  '         _ _/  _     _ _/   \n"
                + "  /  /  /) (/ _)  /  /  (/ (  /  "
                + this.color(ApplicationManager.VERSION, FORE_COLOR.green) + "  \n"
                + "           /                       \n";
    }

    public String color(Object s, int color) {
        if (!virtualTerminal && Platform.isWindows()) {
            Kernel32.INSTANCE.SetConsoleMode(Kernel32.INSTANCE.GetStdHandle(-11), Kernel32.ENABLE_VIRTUAL_TERMINAL_PROCESSING);
            virtualTerminal = true;
        }

        return "\u001b[" + color + "m" + s + "\u001b[0m";
    }

    public String version() {
        return String.format("Dispatcher (cli) (built on %sinystruct-%s) %nCopyright (c) 2013-%s James M. ZHOU", this.color("t", FORE_COLOR.blue), ApplicationManager.VERSION, LocalDate.now().getYear());
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

        Stream<CommandLine> sorted = commandLines.values().stream().sorted();
        sorted.forEach(commandLine -> {
            String command = commandLine.getCommand();
            String description = commandLine.getDescription();
            if (command.startsWith("--")) {
                options.append("\t").append(StringUtilities.rightPadding(command, max, ' ')).append("\t").append(description).append("\n");
            } else {
                commands.append("\t").append(StringUtilities.rightPadding(command, max, ' ')).append("\t").append(description).append("\n");
            }
        });

        builder.append(commands).append("\n");
        builder.append(options);

        builder.append("\nRun 'bin/dispatcher COMMAND --help' for more information on a command.");
        return builder.toString();
    }

    static class FORE_COLOR {
        public static final int black = 30;
        public static final int red = 31;
        public static final int green = 32;
        public static final int yellow = 33;
        public static final int blue = 34;
        public static final int magenta = 35;
        public static final int cyan = 36;
        public static final int white = 37;
    }

    static class BACKGROUND_COLOR {
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