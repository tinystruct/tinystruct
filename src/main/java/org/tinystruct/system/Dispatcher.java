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

import org.tinystruct.*;
import org.tinystruct.application.Context;
import org.tinystruct.data.DatabaseOperator;
import org.tinystruct.data.repository.Type;
import org.tinystruct.data.tools.*;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;
import org.tinystruct.system.cli.CommandLine;
import org.tinystruct.system.cli.Kernel32;
import org.tinystruct.system.event.UpgradeEvent;
import org.tinystruct.system.util.StringUtilities;
import org.tinystruct.system.util.URLResourceLoader;
import org.tinystruct.transfer.http.ReadableByteChannelWrapper;

import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Action(value = "", description = "A command line tool for tinystruct framework",
        options = {
                @Argument(key = "import", description = "Import application"),
                @Argument(key = "allow-remote-access", description = "Allow to be accessed remotely"),
                @Argument(key = "host", description = "Host name / IP"),
                @Argument(key = "logo", description = "Print logo"),
                @Argument(key = "settings", description = "Print settings"),
                @Argument(key = "version", description = "Print version"),
                @Argument(key = "help", description = "Print help information")
        }, mode = org.tinystruct.application.Action.Mode.CLI)
public class Dispatcher extends AbstractApplication implements RemoteDispatcher {
    public static final String OK = "OK!";
    private static final Logger logger = Logger.getLogger(Dispatcher.class.getName());
    private boolean virtualTerminal;
    private final EventDispatcher eventDispatcher = EventDispatcher.getInstance();

    /**
     * Main functionality.
     *
     * @param args arguments
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws RemoteException {
        // Process the system.directory.
        Settings config = new Settings();
        if (config.get("system.directory") == null || "".equals(config.get("system.directory"))) {
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
            List<String> commands = new ArrayList<>();
            int start = 0;
            if (!args[0].startsWith("--") && !args[0].startsWith("-")) {
                commands.add(args[0]);
                start = 1;
            }

            // Process attributes / options.
            String arg;
            for (int i = start; i < args.length; i++) {
                arg = args[i];
                if (arg.startsWith("--")) {
                    if ((i + 1) < args.length) {
                        String value = args[++i].trim();
                        if (!value.isEmpty() && !value.startsWith("--")) {
                            if (context.getAttribute(arg) != null) {
                                List<String> list;
                                Object attribute = context.getAttribute(arg);
                                if (attribute instanceof List) {
                                    list = (List<String>) attribute;
                                } else {
                                    list = new ArrayList<>();
                                    list.add(Objects.requireNonNull(context.getAttribute(arg)).toString());
                                }
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
                    commands.add(arg);
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
            } else {
                dispatcher.install(config, null);
            }

            if (remote) {
                if (!commands.isEmpty()) {
                    for (String command : commands) {
                        System.out.print(remoteDispatcher.execute(command, context));
                    }
                } else {
                    System.out.print(remoteDispatcher.execute(null, context));
                }
                System.exit(0);
            }

            if (!commands.isEmpty()) {
                for (String command : commands) {
                    if (!disableHelper || command != null) {
                        // Execute a local method.
                        dispatcher.execute(command, context);
                    }
                }
            } else {
                if (!disableHelper) {
                    // Execute a local method.
                    dispatcher.execute(null, context);
                }
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
                Object o = ApplicationManager.call(command, context, org.tinystruct.application.Action.Mode.CLI);
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
                    output = ApplicationManager.call("--settings", context, org.tinystruct.application.Action.Mode.CLI);
                } else {
                    output = dispatcher.help();
                }
                System.out.println(output);
            }
        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            logger.info("* SOLUTION: if it's caused by a ClassNotFoundException, ensuring that all dependencies are properly downloaded and available in the classpath is crucial. The command uses Maven Wrapper (mvnw) to copy all dependencies to a specified directory (lib) will be helpful to resolve the issue: \n" +
                    ".\\mvnw dependency:copy-dependencies -DoutputDirectory=lib\n");
        }

        return output;
    }

    /**
     * Install a package.
     */
    @Action(value = "install", description = "Install a package", options = {
            @Argument(key = "app", description = "Packages to be installed")
    }, mode = org.tinystruct.application.Action.Mode.CLI)
    public void install() {
        String appName;
        if ((appName = (String) getContext().getAttribute("--app")) == null)
            throw new ApplicationRuntimeException("The app could not be found in the context.");

        System.out.println("Installing...");

        try {
            this.install(getConfiguration(), List.of(appName));
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        System.out.println("Completed installation for " + appName + "!");
    }

    @Override
    public void install(Configuration<String> config, List<String> list) throws RemoteException {
        if (list != null && !list.isEmpty()) {
            // Initialize the application manager with the configuration.
            list.forEach(appName -> {
                try {
                    Application app = (Application) Class.forName(appName).getDeclaredConstructor().newInstance();
                    ApplicationManager.install(app, config);
                } catch (InstantiationException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                } catch (InvocationTargetException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                } catch (NoSuchMethodException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                } catch (ClassNotFoundException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    logger.info("* SOLUTION: if it's caused by a ClassNotFoundException, ensuring that all dependencies are properly downloaded and available in the classpath is crucial. The command uses Maven Wrapper (mvnw) to copy all dependencies to a specified directory (lib) will be helpful to resolve the issue: \n" +
                            ".\\mvnw dependency:copy-dependencies -DoutputDirectory=lib\n");
                }
            });
        }
    }

    /**
     * Update for latest version.
     */
    @Action(value = "update", options = {@Argument(key = "force", description = "Force to update the framework"),
    }, description = "Update for latest version", mode = org.tinystruct.application.Action.Mode.CLI)
    public String update() {
        System.out.print("Checking...");
        System.out.println("\rThe current project is based on tinystruct-" + this.color(ApplicationManager.VERSION, FORE_COLOR.green));
        try {
            URLResourceLoader loader = new URLResourceLoader(URI.create("https://repo1.maven.org/maven2/org/tinystruct/tinystruct/maven-metadata.xml").toURL());
            StringBuilder content = loader.getContent();
            String latestVersion = content.substring(content.indexOf("<latest>") + 8, content.indexOf("</latest>"));

            if (getContext().getAttribute("--force") == null && latestVersion.equalsIgnoreCase(ApplicationManager.VERSION))
                return "\r" + this.color("You are already using the latest available Dispatcher version " + ApplicationManager.VERSION + ".", FORE_COLOR.green);

            eventDispatcher.dispatch(new UpgradeEvent(latestVersion));

            ApplicationManager.generateDispatcherCommand(latestVersion, true);
        } catch (ApplicationException | MalformedURLException e) {
            return e.getMessage();
        }
        return "\rCompleted! \nRun 'bin/dispatcher --help' for more information.";
    }

    /**
     * Download a resource from other servers.
     */
    @Action(value = "download", description = "Download a resource from other servers", options = {
            @Argument(key = "url", description = "URL resource to be downloaded"),
            @Argument(key = "http.proxyHost", description = "Proxy host for http"),
            @Argument(key = "http.proxyPort", description = "Proxy port for http"),
            @Argument(key = "https.proxyHost", description = "Proxy host for https"),
            @Argument(key = "https.proxyPort", description = "Proxy port for https")
    }, mode = org.tinystruct.application.Action.Mode.CLI)
    public void download() throws ApplicationException {
        if (getContext().getAttribute("--url") != null) {
            URL uri;
            try {
                uri = URI.create(getContext().getAttribute("--url").toString()).toURL();
                this.download(uri, uri.getFile());
            } catch (MalformedURLException e) {
                throw new ApplicationException(e.getMessage(), e.getCause());
            }
        }
    }

    public void download(URL uri, String destination) throws ApplicationException {
        try (ReadableByteChannel rbc = new ReadableByteChannelWrapper(uri)) {
            if (destination.trim().length() <= 1) {
                destination = uri.toString().replaceAll("http://|https://|/", "+");
            }

            // Remove insecure string in the destination
            destination = destination.replaceAll("\\.\\.", "");

            // Replace the path with default file separator
            destination = destination.replaceAll("/", "\\" + File.separator);

            // Remove the suffix after '?' if contains '?'
            if (destination.contains("?")) {
                destination = destination.substring(0, destination.indexOf("?"));
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

    /**
     * Execute native command(s).
     */
    @Action(value = "exec", description = "To execute native command(s)", options = {
            @Argument(key = "shell-command", description = "Commands needs to be executed"),
            @Argument(key = "output", description = "Specify a boolean value to determine output of the command")
    }, mode = org.tinystruct.application.Action.Mode.CLI)
    public void exec() throws ApplicationException {
        if (getContext().getAttribute("--shell-command") == null) {
            throw new ApplicationException("Invalid shell command");
        }

        boolean output = true;
        if (getContext().getAttribute("--output") != null) {
            output = Boolean.parseBoolean(getContext().getAttribute("--output").toString());
        }

        String cmd = getContext().getAttribute("--shell-command").toString();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            // Use Platform utility class to determine the shell and command
            if (Platform.isWindows()) {
                processBuilder.command("cmd", "/c", cmd);
            } else {
                processBuilder.command("sh", "-c", cmd);
            }

            // Redirect error stream to standard output
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            if (output) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new ApplicationException("Command execution failed with exit code: " + exitCode);
            }

        } catch (IOException e) {
            throw new ApplicationException("Failed to execute command: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApplicationException("Command execution was interrupted", e);
        }
    }

    /**
     * Executes the given SQL statement, which may be an INSERT, UPDATE, DELETE, or DDL statement.
     */
    @Action(value = "sql-execute", description = "Executes the given SQL statement, which may be an INSERT, UPDATE, DELETE, or DDL statement", options = {
            @Argument(key = "sql", description = "an SQL Data Manipulation Language (DML) statement, such as INSERT, UPDATE or DELETE; or an SQL statement that returns nothing, such as a DDL statement."),
            @Argument(key = "sql-file", description = "path to a SQL script file to execute")
    }, mode = org.tinystruct.application.Action.Mode.CLI)
    public void executeUpdate() throws ApplicationException {
        String sql = null;
        String filePath = null;

        if (getContext().getAttribute("--sql") != null) {
            sql = getContext().getAttribute("--sql").toString();
        } else if (getContext().getAttribute("--sql-file") != null) {
            filePath = getContext().getAttribute("--sql-file").toString();
        } else {
            throw new ApplicationException("Either --sql or --sql-file parameter must be provided.");
        }

        try (DatabaseOperator operator = new DatabaseOperator()) {
            operator.disableSafeCheck();

            if (sql != null) {
                if (operator.update(sql) > 0) {
                    System.out.println("Done!");
                }
            } else if (filePath != null) {
                File sqlFile = new File(filePath);
                if (!sqlFile.exists()) {
                    throw new ApplicationException("SQL script file not found: " + filePath);
                }

                try (BufferedReader reader = new BufferedReader(new FileReader(sqlFile))) {
                    StringBuilder script = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Skip comments and empty lines
                        if (line.trim().startsWith("--") || line.trim().startsWith("//") || line.trim().isEmpty()) {
                            continue;
                        }
                        script.append(line).append("\n");
                    }

                    // Split the script into individual statements
                    String[] statements = script.toString().split(";");
                    for (String statement : statements) {
                        statement = statement.trim();
                        if (!statement.isEmpty()) {
                            if (operator.update(statement) > 0) {
                                System.out.println("Executed: " + statement);
                            }
                        }
                    }
                    System.out.println("Script execution completed!");
                } catch (IOException e) {
                    throw new ApplicationException("Error reading SQL script file: " + e.getMessage());
                }
            }
        } catch (ApplicationException e) {
            System.err.println(e.getCause().getMessage());
        }
    }

    /**
     * Executes the given SQL statement, which returns a single ResultSet object.
     */
    @Action(value = "sql-query", description = "Executes the given SQL statement, which returns a single ResultSet object", options = {
            @Argument(key = "sql", description = "an SQL statement to be sent to the database, typically a static SQL SELECT statement")
    }, mode = org.tinystruct.application.Action.Mode.CLI)
    public void executeQuery() throws ApplicationException {
        if (getContext().getAttribute("--sql") == null) {
            throw new ApplicationException("Invalid SQL Statement.");
        }

        String sql = getContext().getAttribute("--sql").toString();
        try (DatabaseOperator operator = new DatabaseOperator()) {
            operator.disableSafeCheck();

            ResultSet set = operator.query(sql);
            int columnCount = set.getMetaData().getColumnCount();
            String[] columns = new String[columnCount];
            int[] maxItems = new int[columnCount];

            if (columnCount > 0) {
                for (int i = 0; i < columnCount; i++) {
                    columns[i] = set.getMetaData().getColumnName(i + 1);
                    maxItems[i] = columns[i].length();
                }
            }

            List<String[]> list = new ArrayList<>();
            list.add(columns);
            while (set.next()) {
                String[] data = new String[columnCount];
                for (int i = 0; i < columns.length; i++) {
                    Object field = set.getObject(i + 1);
                    String fieldValue = (field == null ? "" : field.toString());
                    data[i] = fieldValue;
                    if (fieldValue.length() > maxItems[i]) maxItems[i] = fieldValue.length();
                }
                list.add(data);
            }

            list.forEach(item -> {
                for (int i = 0; i < columns.length; i++) {
                    System.out.print("|" + StringUtilities.rightPadding(item[i], maxItems[i], ' '));
                }
                System.out.println("|");
            });
        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void init() {
        this.setTemplateRequired(false);

        eventDispatcher.registerHandler(UpgradeEvent.class, evt -> {
            System.out.print("\rGot a new version " + evt.getLatestVersion() + "...");
            System.out.print("\rDownloading...");
            try {
                this.download(URI.create("https://repo1.maven.org/maven2/org/tinystruct/tinystruct/" + evt.getLatestVersion()
                        + "/tinystruct-" + evt.getLatestVersion() + "-jar-with-dependencies.jar").toURL(), "lib/tinystruct-" + evt.getLatestVersion() + "-jar-with-dependencies.jar");

                System.out.println("\nDownloaded (" + this.color(evt.getLatestVersion(), FORE_COLOR.green) + ").");
                System.out.print("\rUpdating..");
                boolean git = new File(".git").exists();
                if (git) {
                    getContext().setAttribute("--shell-command", "git pull");
                    this.exec();
                    System.out.println("Reminder: DO NOT forget to compile it.");
                } else {
                    boolean pom = new File("pom.xml").exists();
                    boolean maven = Platform.isWindows() ? new File("mvnw.cmd").exists() : new File("mvnw").exists();
                    if (pom && maven) {
                        getContext().setAttribute("--shell-command", "./mvnw versions:use-dep-version -Dincludes=org.tinystruct:tinystruct -DdepVersion=" + evt.getLatestVersion() + " -DforceVersion=true");
                        getContext().setAttribute("--output", false);
                        this.exec();

                        // Auto compile the update project
                        getContext().setAttribute("--shell-command", "./mvnw compile");
                        getContext().setAttribute("--output", true);
                        this.exec();
                    }
                }
            } catch (ApplicationException | MalformedURLException e) {
                throw new ApplicationRuntimeException(e.getMessage(), e);
            }
        });
    }

    private void bind(Dispatcher dispatcher) {
        try {
            String name = "Dispatcher";
            RemoteDispatcher stub =
                    (RemoteDispatcher) UnicastRemoteObject.exportObject(dispatcher, 0);
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.bind(name, stub);
            logger.warning("It will be allowed to send commands to the machine with --host option.");
        } catch (Exception e) {
            System.err.println(e.getCause().getMessage());
        }
    }

    /**
     * Output words.
     */
    @Action(value = "say", description = "Output words", arguments = {
            @Argument(key = "words", description = "What you want to say")
    })
    public String say(String words) {
        return words;
    }

    /**
     * Set system property.
     */
    @Action(value = "set", description = "Set system property", mode = org.tinystruct.application.Action.Mode.CLI)
    public String setProperty(String propertyName) {
        if (getContext().getAttribute(propertyName) == null)
            throw new ApplicationRuntimeException("The key " + propertyName + " could not be found in the context.");
        String property = getContext().getAttribute(propertyName).toString();
        propertyName = propertyName.substring(2);
        System.setProperty(propertyName, property);
        return propertyName + ":" + System.getProperty(propertyName);
    }

    /**
     * Print settings.
     */
    @Action(value = "--settings", description = "Print settings", mode = org.tinystruct.application.Action.Mode.CLI)
    public StringBuilder settings() {
        String[] names = getConfiguration().propertyNames().toArray(new String[0]);
        Arrays.sort(names);
        StringBuilder settings = new StringBuilder();
        for (String name : names) {
            settings.append(name).append(":").append(getConfiguration().get(name)).append("\n");
        }
        return settings;
    }

    /**
     * Start a default browser to open the specific URL.
     */
    @Action(value = "open", description = "Start a default browser to open the specific URL", options = {
            @Argument(key = "url", description = "URL resource to be downloaded")
    }, mode = org.tinystruct.application.Action.Mode.CLI)
    public void open() throws ApplicationException {
        if (getContext().getAttribute("--url") == null) {
            throw new ApplicationException("Invalid URL.");
        }

        final String url = getContext().getAttribute("--url").toString();
        new Thread(new Runnable() {
            /**
             * When an object implementing interface <code>Runnable</code> is used
             * to create a thread, starting the thread causes the object's
             * <code>run</code> method to be called in that separately executing
             * thread.
             * <p>
             * The general contract of the method <code>run</code> is that it may
             * take any action whatsoever.
             *
             * @see Thread#run()
             */
            @Override
            public void run() {
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    try {
                        desktop.browse(new URI(url));
                    } catch (IOException | URISyntaxException e) {
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        }).start();
    }

    /**
     * POJO object generator with improved error handling, validation, and customization.
     */
    @Action(value = "generate", description = "POJO object generator",
            options = {
                @Argument(key = "tables", description = "Table names to generate POJO code (semicolon-separated)"),
                @Argument(key = "path", description = "Base path to place the Java code files"),
                @Argument(key = "imports", description = "Packages to be imported (semicolon-separated)"),
                @Argument(key = "transaction", description = "Enable transaction support (true/false)"),
                @Argument(key = "verbose", description = "Enable verbose output (true/false)")
            },
            mode = org.tinystruct.application.Action.Mode.CLI)
    public void generate() throws ApplicationException {
        // Step 1: Determine if we're in interactive or command-line mode
        boolean isInteractive = !hasCommandLineOptions();
        String tableNames = "";
        String basePath = "";
        String imports = "";
        boolean useTransaction = false;
        boolean verbose = false;

        // Initialize scanner for interactive mode
        Scanner scanner = null;
        if (isInteractive) {
            scanner = new Scanner(System.in);
            System.out.println("POJO Generator - Interactive Mode");
            System.out.println("=====================================");
            System.out.println("Follow the steps below to generate code for your project. Press CTRL+C to exit at any time.\n");
        } else {
            // Get values from command line options
            Object tablesObj = getContext().getAttribute("--tables");
            Object pathObj = getContext().getAttribute("--path");
            Object importsObj = getContext().getAttribute("--imports");
            Object transactionObj = getContext().getAttribute("--transaction");
            Object verboseObj = getContext().getAttribute("--verbose");

            tableNames = tablesObj != null ? tablesObj.toString() : "";
            basePath = pathObj != null ? pathObj.toString() : "";
            imports = importsObj != null ? importsObj.toString() : "";
            useTransaction = transactionObj != null && Boolean.parseBoolean(transactionObj.toString());
            verbose = verboseObj != null && Boolean.parseBoolean(verboseObj.toString());
        }

        try {
            // Step 2: Get table names (interactive or command line)
            if (isInteractive) {
                System.out.print("Please provide table name(s) to generate POJO code and use the delimiter `;` for multiple items: ");
                tableNames = scanner.nextLine();
                while (tableNames.isBlank()) {
                    System.out.print("Table names cannot be empty. Please provide table name(s): ");
                    tableNames = scanner.nextLine();
                }
            } else if (tableNames == null || tableNames.isBlank()) {
                throw new ApplicationException("Table names must be specified with --tables option");
            }

            // Step 3: Validate table names for security
            validateTableNames(tableNames);

            // Step 4: Get base path (interactive or default)
            if (isInteractive) {
                System.out.print("Please specify the base path to place the Java code files [src/main/java/custom/objects]: ");
                basePath = scanner.nextLine();
            }

            // Default base path if not provided
            String defaultBasePath = getConfiguration().get("generator.default.path");
            if (defaultBasePath == null || defaultBasePath.isBlank()) {
                defaultBasePath = "src/main/java/custom/objects";
            }
            basePath = basePath.isBlank() ? defaultBasePath : basePath;

            // Normalize path for cross-platform compatibility
            basePath = normalizePath(basePath);

            // Step 5: Get imports (interactive or default)
            if (isInteractive) {
                System.out.print("Please specify the packages to be imported in code and use delimiter `;` for multiple items [java.time.LocalDateTime]: ");
                imports = scanner.nextLine();
            }

            // Default imports if not provided
            String defaultImports = getConfiguration().get("generator.default.imports");
            if (defaultImports == null || defaultImports.isBlank()) {
                defaultImports = "java.time.LocalDateTime";
            }
            imports = imports.isBlank() ? defaultImports : imports;

            // Close scanner if in interactive mode
            if (isInteractive && scanner != null) {
                scanner.close();
                scanner = null;
            }

            // Step 6: Validate database connection before proceeding
            validateDatabaseConnection();

            // Step 7: Get database driver and determine generator type
            String driver = getConfiguration().get("driver");
            if (driver == null || driver.trim().isEmpty()) {
                throw new ApplicationException("Database Connection Driver has not been set in application.properties!");
            }

            // Select appropriate generator based on database type
            Generator generator = createGeneratorForDriver(driver);
            if (generator == null) {
                throw new ApplicationException("Unsupported database type for driver: " + driver);
            }

            // Step 8: Configure generator
            generator.setPath(basePath);

            // Convert path to package name more robustly
            String packageName = convertPathToPackage(basePath);
            generator.setPackageName(packageName);

            // Set imports
            generator.importPackages(imports);

            // Step 9: Split table names and process each one
            String[] tableList = tableNames.split(";");
            int totalTables = tableList.length;
            int processedTables = 0;

            // Step 10: Begin transaction if requested
            DatabaseOperator operator = null;
            if (useTransaction) {
                operator = new DatabaseOperator();
                operator.beginTransaction();
                if (verbose) {
                    System.out.println("Transaction started");
                }
            }

            try {
                // Step 11: Process each table
                for (String tableName : tableList) {
                    // Convert table name to camel case for class name
                    String className = StringUtilities.convertToCamelCase(tableName);

                    if (verbose) {
                        System.out.printf("Generating code for table '%s' as class '%s'...%n", tableName, className);
                    } else {
                        // Show progress
                        processedTables++;
                        System.out.printf("Generating: [%d/%d] %s -> %s", processedTables, totalTables, tableName, className);
                        System.out.print("\r");
                    }

                    // Create the class - use camelCase className but original tableName
                    generator.create(className, tableName);

                    if (verbose) {
                        System.out.printf("File(s) for %s have been generated successfully.%n", className);
                    }
                }

                // Step 12: Commit transaction if used
                if (useTransaction && operator != null) {
                    operator.commitTransaction();
                    if (verbose) {
                        System.out.println("Transaction committed");
                    }
                }

                // Final success message
                System.out.println("\nCode generation completed successfully!");
                System.out.printf("Generated %d class(es) in %s%n", totalTables, basePath);

            } catch (Exception e) {
                // Rollback transaction on error if used
                if (useTransaction && operator != null) {
                    operator.rollbackTransaction();
                    if (verbose) {
                        System.out.println("Transaction rolled back due to error");
                    }
                }
                throw e;
            } finally {
                // Close database operator if used
                if (operator != null) {
                    operator.close();
                }
            }

        } catch (ApplicationException e) {
            System.err.println("\nError: " + e.getMessage());
            logger.log(Level.SEVERE, "Code generation failed", e);
            throw e;
        } catch (Exception e) {
            System.err.println("\nUnexpected error: " + e.getMessage());
            logger.log(Level.SEVERE, "Unexpected error during code generation", e);
            throw new ApplicationException("Code generation failed: " + e.getMessage(), e);
        } finally {
            // Ensure scanner is closed in case of early exit
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    /**
     * Checks if command line options are provided for the generate command.
     *
     * @return true if any command line options are provided, false otherwise
     */
    private boolean hasCommandLineOptions() {
        return getContext().getAttribute("--tables") != null ||
               getContext().getAttribute("--path") != null ||
               getContext().getAttribute("--imports") != null ||
               getContext().getAttribute("--transaction") != null ||
               getContext().getAttribute("--verbose") != null;
    }

    /**
     * Validates table names for security concerns.
     *
     * @param tableNames The table names to validate
     * @throws ApplicationException if validation fails
     */
    private void validateTableNames(String tableNames) throws ApplicationException {
        if (tableNames == null || tableNames.isBlank()) {
            throw new ApplicationException("Table names cannot be empty");
        }

        // Check for SQL injection patterns
        String[] tables = tableNames.split(";");
        for (String table : tables) {
            if (table.contains("'") || table.contains("\"") ||
                table.contains("--") || table.contains(";") ||
                table.contains("/*") || table.contains("*/") ||
                table.contains("=") || table.contains("(") ||
                table.contains(")")) {
                throw new ApplicationException("Invalid table name: " + table + ". Table names should not contain special characters.");
            }
        }
    }

    /**
     * Normalizes a file path for cross-platform compatibility.
     *
     * @param path The path to normalize
     * @return The normalized path
     */
    private String normalizePath(String path) {
        // Replace backslashes with forward slashes
        path = path.replace("\\", "/");

        // Remove trailing slash if present
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    /**
     * Validates that the database connection is available.
     *
     * @throws ApplicationException if database connection fails
     */
    private void validateDatabaseConnection() throws ApplicationException {
        try (DatabaseOperator operator = new DatabaseOperator()) {
            // Try a simple query to validate connection
            operator.query("SELECT 1");
        } catch (Exception e) {
            throw new ApplicationException("Database connection failed. Please check your database settings: " + e.getMessage(), e);
        }
    }

    /**
     * Creates the appropriate generator based on the database driver.
     *
     * @param driver The database driver string
     * @return The appropriate Generator implementation
     */
    private Generator createGeneratorForDriver(String driver) {
        // Check for known database types
        if (driver.toLowerCase().contains("mysql")) {
            return new MySQLGenerator();
        } else if (driver.toLowerCase().contains("sqlserver") || driver.toLowerCase().contains("mssql")) {
            return new MSSQLGenerator();
        } else if (driver.toLowerCase().contains("sqlite")) {
            return new SQLiteGenerator();
        } else if (driver.toLowerCase().contains("h2")) {
            return new H2Generator();
        }

        // Default to MySQL if unknown
        return new MySQLGenerator();
    }

    /**
     * Converts a file path to a Java package name.
     *
     * @param path The file path
     * @return The corresponding package name
     */
    private String convertPathToPackage(String path) {
        // Handle both Unix and Windows paths
        String normalizedPath = path.replace("\\", "/");

        // Extract package from path
        String packageName = normalizedPath;

        // Remove src/main/java prefix if present
        if (packageName.contains("src/main/java/")) {
            packageName = packageName.substring(packageName.indexOf("src/main/java/") + "src/main/java/".length());
        }

        // Convert slashes to dots
        packageName = packageName.replace("/", ".");

        // Remove leading or trailing dots
        packageName = packageName.replaceAll("^\\.+|\\.+$", "");

        return packageName;
    }



    @Action(value = "maven-wrapper", description = "Extract Maven Wrapper", arguments = {
            @Argument(key = "--jar-file-path", description = "The jar file path which included maven-wrapper.zip"),
            @Argument(key = "--destination-dir", description = "The destination dir ")
    }, mode = org.tinystruct.application.Action.Mode.CLI)
    public void extractMavenWrapperFromJar() throws IOException, ApplicationException {
        String jarFilePath;
        String destinationDir;
        if (getContext().getAttribute("--jar-file-path") != null) {
            jarFilePath = getContext().getAttribute("--jar-file-path").toString();
        } else {
            throw new ApplicationException("Missing --jar-file-path. It should point to a jar file of tinystruct framework.");
        }

        if (getContext().getAttribute("--destination-dir") != null) {
            destinationDir = getContext().getAttribute("--destination-dir").toString();
        } else {
            destinationDir = getConfiguration().getOrDefault("system.directory", ".");
        }

        try (JarFile jarFile = new JarFile(jarFilePath)) {
            JarEntry entry = jarFile.getJarEntry("maven-wrapper.zip");  // The path in the JAR

            if (entry != null) {
                File destFile = new File(destinationDir, "maven-wrapper.zip");

                try (InputStream inputStream = jarFile.getInputStream(entry);
                     FileOutputStream outputStream = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, length);
                    }
                    System.out.println("Maven wrapper ZIP extracted successfully.");
                }
            }
        }
    }

    /**
     * Print logo.
     */
    @Action(value = "--logo", description = "Print logo", mode = org.tinystruct.application.Action.Mode.CLI)
    public String logo() {
        return "\n"
                + "  _/  '         _ _/  _     _ _/   \n"
                + "  /  /  /) (/ _)  /  /  (/ (  /  "
                + this.color(ApplicationManager.VERSION, FORE_COLOR.green) + "  \n"
                + "           /                       \n";
    }

    public String color(Object s, int color) {
        if (!virtualTerminal && Platform.isWindows() && Platform.isJnaAvailable()) {
            Kernel32.INSTANCE.SetConsoleMode(Kernel32.INSTANCE.GetStdHandle(-11), Kernel32.ENABLE_VIRTUAL_TERMINAL_PROCESSING);
            virtualTerminal = true;
        }

        return "\u001b[" + color + "m" + s + "\u001b[0m";
    }

    /**
     * Print version.
     */
    @Action(value = "--version", description = "Print version", mode = org.tinystruct.application.Action.Mode.CLI)
    @Override
    public String version() {
        return String.format("Dispatcher (cli) (built on %sinystruct-%s) %nCopyright (c) 2013-%s James M. ZHOU", this.color("t", FORE_COLOR.blue), ApplicationManager.VERSION, LocalDate.now().getYear());
    }

    @Action(value = "--help", description = "Print help information", mode = org.tinystruct.application.Action.Mode.CLI)
    @Override
    public String help() {
        StringBuilder builder = new StringBuilder("Usage: bin" + File.separator + "dispatcher COMMAND [OPTIONS]\n");

        StringBuilder commands = new StringBuilder("Commands: \n");
        StringBuilder options = new StringBuilder("Options: \n");
        StringBuilder examples = new StringBuilder("Example(s): \n");
        int length = examples.length();
        int optionsLength = options.length();
        Map<String, CommandLine> commandLines = new HashMap<>();
        Collection<Application> apps = ApplicationManager.list();
        apps.forEach(app -> commandLines.putAll(app.getCommandLines()));

        OptionalInt longSizeCommand = commandLines.keySet().stream().mapToInt(String::length).max();
        int max = longSizeCommand.orElse(0);

        Stream<CommandLine> sorted = this.commandLines.values().stream().sorted();
        sorted.forEach(commandLine -> {
            String command = commandLine.getCommand();
            String description = commandLine.getDescription();
            String example = commandLine.getExample();
            if (command.startsWith("--")) {
//                    options.append("\t").append(StringUtilities.rightPadding(command, max, ' ')).append("\t").append(description).append("\n");
            } else if (command.isEmpty()) {
                builder.append(description).append("\n");
                commandLine.getOptions().forEach(option -> {
                    options.append("\t").append(StringUtilities.rightPadding(option.getKey(), max, ' ')).append("\t").append(option.getDescription()).append("\n");
                });
            } else {
                commands.append("\t").append(StringUtilities.rightPadding(command, max, ' ')).append("\t").append(description).append("\n");
            }

            if (example != null && !example.isEmpty()) {
                examples.append(example).append("\n");
            }
        });

        builder.append(commands).append("\n");
        if (optionsLength < options.length())
            builder.append(options);

        if (length < examples.length())
            builder.append(examples);

        builder.append("\nRun 'bin").append(File.separator).append("dispatcher COMMAND --help' for more information on a command.");
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