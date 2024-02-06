package org.tinystruct.system;

import com.sun.jna.Platform;
import org.tinystruct.Application;
import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.application.Action;
import org.tinystruct.application.ActionRegistry;
import org.tinystruct.application.Context;
import org.tinystruct.system.cli.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

public final class ApplicationManager {
    public static final String VERSION = "1.1.5";
    private static final ConcurrentHashMap<String, Application> applications = new ConcurrentHashMap<>();
    private static final ActionRegistry ROUTE_REGISTRY_INSTANCE = ActionRegistry.getInstance();
    private static Configuration<String> settings;
    private static volatile boolean initialized = false;

    private ApplicationManager() {
    }

    /**
     * Initialize the ApplicationManager with a configuration.
     *
     * @param config Configuration for the ApplicationManager.
     * @throws ApplicationException If an error occurs during initialization.
     */
    public static void init(final Configuration<String> config) throws ApplicationException {
        settings = config;
        init();
    }

    /**
     * Initialize the ApplicationManager using default configuration.
     *
     * @throws ApplicationException If an error occurs during initialization.
     */
    public static void init() throws ApplicationException {
        if (initialized) return;

        synchronized (ApplicationManager.class) {
            if (initialized) return;

            settings = (settings == null) ? new Settings("/application.properties") : settings;
            // Generate Command Script
            generateDispatcherCommand(VERSION, false);

            if (!settings.get("default.import.applications").trim().isEmpty()) {
                StringTokenizer tokenizer = new StringTokenizer(settings.get("default.import.applications"), ";");
                while (tokenizer.hasMoreTokens()) {
                    String appClassName = tokenizer.nextToken().trim();
                    if (!appClassName.isEmpty()) {
                        try {
                            Application app = (Application) Class.forName(appClassName).getDeclaredConstructor().newInstance();
                            ApplicationManager.install(app);
                        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException |
                                 InvocationTargetException | NoSuchMethodException e) {
                            throw new ApplicationException(e.toString(), e);
                        }
                    }
                }

                initialized = true;
            }
        }
    }

    /**
     * Generate the dispatcher command script.
     *
     * @param version Version of the dispatcher script.
     * @param force   Force generation of the script even if it exists.
     * @throws ApplicationException If an error occurs during script generation.
     */
    public static void generateDispatcherCommand(String version, boolean force) throws ApplicationException {
        String scriptName = Platform.isWindows() ? "dispatcher.cmd" : "dispatcher";
        String userDir = System.getProperty("user.dir");
        if (userDir.endsWith(File.separator + "bin"))
            userDir = userDir.substring(0, userDir.length() - 4);

        String paths = userDir + File.separator + "bin" + File.separator + scriptName;
        String origin = paths;
        Path path = Paths.get(paths);
        if (force || (!Files.exists(Paths.get(scriptName)) && !Files.exists(path))) {
            try {
                while (!Files.exists(path)) {
                    paths = paths.substring(0, paths.indexOf(File.separator));
                    path = Paths.get(paths);
                    Files.createDirectories(path);
                }

                String cmd;
                if (Platform.isWindows()) {
                    cmd = "@echo off\n" +
                            "set \"ROOT=%~dp0..\\\"\n" +
                            "set \"VERSION=" + version + "\"\n" +
                            "set \"classpath=%ROOT%target\\classes;%ROOT%lib\\*;%ROOT%WEB-INF\\lib\\*;%ROOT%WEB-INF\\classes;%classpath%\"\n" +
                            "@java -cp \"%ROOT%target\\classes;%ROOT%lib\\tinystruct-%VERSION%-jar-with-dependencies.jar;%ROOT%lib\\*;%ROOT%WEB-INF\\lib\\*;%ROOT%WEB-INF\\classes;%USERPROFILE%\\.m2\\repository\\org\\tinystruct\\tinystruct\\%VERSION%\\tinystruct-%VERSION%-jar-with-dependencies.jar\" org.tinystruct.system.Dispatcher %*";
                } else {
                    cmd = "#!/usr/bin/env sh\n" +
                            "ROOT=\"$(pwd)\"\n" +
                            "VERSION=\"" + version + "\"\n" +
                            "cd \"$(dirname \"$0\")\" || exit\n" +
                            "cd \"../\"\n" +
                            "cd \"$ROOT\" || exit\n" +
                            "JAVA_OPTS=\"\"\n" +
                            "args=\"\"\n" +
                            "for arg\n" +
                            "do\n" +
                            " str=$(echo \"$arg\" | awk  '{ string=substr($0, 0, 2); print string; }' )\n" +
                            " if [ \"$str\" = \"-D\" -o \"$str\" = \"-X\" ]\n" +
                            " then\n" +
                            "     JAVA_OPTS=$JAVA_OPTS\" \"$arg\n" +
                            " else\n" +
                            "     args=$args\" \"$arg\n" +
                            " fi\n" +
                            "done\n" +
                            "JAR_FILE=\"$ROOT/lib/tinystruct-$VERSION-jar-with-dependencies.jar\"\n" +
                            "if [ ! -f \"$JAR_FILE\" ]; then\n" +
                            " JAR_FILE=\"\"\n" +
                            "else\n" +
                            " JAR_FILE=$JAR_FILE\":\"\n" +
                            "fi\n" +
                            "java \\\n" +
                            "$JAVA_OPTS \\\n" +
                            "-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/ \\\n" +
                            "-cp \"$ROOT/target/classes:$JAR_FILE$ROOT/lib/*:$ROOT/WEB-INF/lib/*:$ROOT/WEB-INF/classes:$HOME/.m2/repository/org/tinystruct/tinystruct/$VERSION/tinystruct-$VERSION-jar-with-dependencies.jar\" org.tinystruct.system.Dispatcher \"$@\"";
                }

                path = Paths.get(origin);
                if (!Files.exists(path))
                    Files.createFile(path);

                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    writer.write(cmd);
                }

            } catch (IOException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
        }
    }

    /**
     * Install an application.
     *
     * @param app Application to be installed.
     */
    public static void install(Application app) {
        if (settings == null)
            throw new ApplicationRuntimeException("Application configuration has not been initialized or specified.");
        if (!applications.containsKey(app.getName())) {
            app.setConfiguration(settings);
            applications.putIfAbsent(app.getName(), app);
        }
    }

    /**
     * Install an application with a specific configuration.
     *
     * @param app   Application to be installed.
     * @param config Configuration for the installed application.
     */
    public static void install(Application app, Configuration<String> config) {
        settings = config;
        install(app);
    }

    /**
     * Uninstall an application.
     *
     * @param application Application to be uninstalled.
     * @return True if the application was uninstalled successfully, false otherwise.
     */
    public static boolean uninstall(Application application) {
        applications.remove(application.getName());

        ActionRegistry actionRegistry = ROUTE_REGISTRY_INSTANCE;
        Object[] list = actionRegistry.list().toArray();

        Action action;
        for (Object item : list) {
            action = (Action) item;

            if (action.getApplicationName().equalsIgnoreCase(application.getName())) {
                return actionRegistry.remove(action.getPathRule());
            }
        }

        return false;
    }

    /**
     * Get an application by its class ID.
     *
     * @param clsid Class ID of the application.
     * @return The application with the specified class ID.
     */
    public static Application get(String clsid) {
        return applications.get(clsid);
    }

    /**
     * Get a collection of all installed applications.
     *
     * @return Collection of installed applications.
     */
    public static Collection<Application> list() {
        return applications.values();
    }

    /**
     * Call an action with a specific context.
     *
     * @param path    Path of the action.
     * @param context Context for the action.
     * @return The result of the action execution.
     * @throws ApplicationException If an error occurs during action execution.
     */
    public static Object call(final String path, final Context context) throws ApplicationException {
        if (path == null || path.trim().isEmpty()) {
            throw new ApplicationException(
                    "Invalid: empty path", 400);
        }

        String method = (context != null && context.getAttribute("METHOD") != null) ? context.getAttribute("METHOD").toString() : null;

        if (context != null && context.getAttribute("--help") != null) {
            CommandLine command;
            if ((command = ROUTE_REGISTRY_INSTANCE.getCommand(path)) != null)
                return command;
        }

        Action action = ROUTE_REGISTRY_INSTANCE.getAction(path, method);
        if (action == null) {
            throw new ApplicationException(
                    "Access error [" + path + "]: Application has not been installed, or it has been uninstalled already.", 404);
        }

        if (context != null) {
            context.setAttribute("REQUEST_PATH", path);
            action.setContext(context);
        }

        return action.execute();
    }

    /**
     * Get the configuration of the ApplicationManager.
     *
     * @return Configuration of the ApplicationManager.
     */
    public static Configuration<String> getConfiguration() {
        return settings;
    }
}
