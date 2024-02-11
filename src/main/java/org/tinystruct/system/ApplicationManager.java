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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

public final class ApplicationManager {
    public static final String VERSION = "1.1.6";
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
                            "\n" +
                            "@REM Check if JAVA_HOME is set and valid\n" +
                            "if \"%JAVA_HOME%\" == \"\" (\n" +
                            "    echo Error: JAVA_HOME not found in your environment. >&2\n" +
                            "    echo Please set the JAVA_HOME variable in your environment to match the location of your Java installation. >&2\n" +
                            "    exit /B 1\n" +
                            ")\n" +
                            "\n" +
                            "if not exist \"%JAVA_HOME%\\bin\\java.exe\" (\n" +
                            "    echo Error: JAVA_HOME is set to an invalid directory. >&2\n" +
                            "    echo JAVA_HOME = \"%JAVA_HOME%\" >&2\n" +
                            "    echo Please set the JAVA_HOME variable in your environment to match the location of your Java installation. >&2\n" +
                            "    exit /B 1\n" +
                            ")\n" +
                            "\n" +
                            "set \"JAVA_CMD=%JAVA_HOME%\\bin\\java.exe\"\n" +
                            "\n" +
                            "@REM Consolidate classpath entries, initialize ROOT and VERSION\n" +
                            "set \"ROOT=%~dp0..\\\"\n" +
                            "set \"VERSION=" + version + "\"\n" +
                            "set \"classpath=%ROOT%target\\classes;%ROOT%lib\\tinystruct-%VERSION%-jar-with-dependencies.jar;%ROOT%lib\\*;%ROOT%WEB-INF\\lib\\*;%ROOT%WEB-INF\\classes;%USERPROFILE%\\.m2\\repository\\org\\tinystruct\\tinystruct\\%VERSION%\\tinystruct-%VERSION%-jar-with-dependencies.jar\"\n" +
                            "\n" +
                            "@REM Run Java application\n" +
                            "%JAVA_CMD% -cp \"%classpath%\" org.tinystruct.system.Dispatcher %*";
                } else {
                    cmd = "#!/usr/bin/env sh\n" +
                            "ROOT=\"$(pwd)\"\n" +
                            "VERSION=\"" + version + "\"\n" +
                            "cd \"$(dirname \"$0\")\" || exit\n" +
                            "cd \"../\"\n" +
                            "# Navigate to the root directory\n" +
                            "cd \"$ROOT\" || exit\n" +
                            "\n" +
                            "# Java options initialization\n" +
                            "JAVA_OPTS=\"\"\n" +
                            "\n" +
                            "# Arguments initialization\n" +
                            "args=\"\"\n" +
                            "\n" +
                            "# Loop through each argument\n" +
                            "for arg; do\n" +
                            "    # Extract the first two characters of the argument\n" +
                            "    str=${arg:0:2}\n" +
                            "\n" +
                            "    # Check if it starts with '-D' or '-X'\n" +
                            "    if [ \"$str\" = \"-D\" ] || [ \"$str\" = \"-X\" ]; then\n" +
                            "        JAVA_OPTS=\"$JAVA_OPTS $arg\"\n" +
                            "    else\n" +
                            "        args=\"$args $arg\"\n" +
                            "    fi\n" +
                            "done\n" +
                            "\n" +
                            "# Check if the JAR file exists\n" +
                            "JAR_FILE=\"$ROOT/lib/tinystruct-$VERSION-jar-with-dependencies.jar\"\n" +
                            "[ -f \"$JAR_FILE\" ] && JAR_FILE=\"$JAR_FILE:\"\n" +
                            "\n" +
                            "# Check if additional JAR files exist and add them to the classpath\n" +
                            "# shellcheck disable=SC2043\n" +
                            "for jar_file in \"$ROOT\"/lib/tinystruct-\"$VERSION\".jar; do\n" +
                            "    [ -f \"$jar_file\" ] && JAR_FILE=\"$JAR_FILE$jar_file:\"\n" +
                            "done\n" +
                            "\n" +
                            "# OS specific support.  $var _must_ be set to either true or false.\n" +
                            "cygwin=false;\n" +
                            "darwin=false;\n" +
                            "mingw=false\n" +
                            "case \"`uname`\" in\n" +
                            "  CYGWIN*) cygwin=true ;;\n" +
                            "  MINGW*) mingw=true;;\n" +
                            "  Darwin*) darwin=true\n" +
                            "    # Use /usr/libexec/java_home if available, otherwise fall back to /Library/Java/Home\n" +
                            "    # See https://developer.apple.com/library/mac/qa/qa1170/_index.html\n" +
                            "    if [ -z \"$JAVA_HOME\" ]; then\n" +
                            "      if [ -x \"/usr/libexec/java_home\" ]; then\n" +
                            "        export JAVA_HOME=\"`/usr/libexec/java_home`\"\n" +
                            "      else\n" +
                            "        export JAVA_HOME=\"/Library/Java/Home\"\n" +
                            "      fi\n" +
                            "    fi\n" +
                            "    ;;\n" +
                            "esac\n" +
                            "\n" +
                            "if [ -z \"$JAVA_HOME\" ] ; then\n" +
                            "  if [ -r /etc/gentoo-release ] ; then\n" +
                            "    JAVA_HOME=`java-config --jre-home`\n" +
                            "  fi\n" +
                            "fi\n" +
                            "\n" +
                            "if [ -z \"$M2_HOME\" ] ; then\n" +
                            "  ## resolve links - $0 may be a link to maven's home\n" +
                            "  PRG=\"$0\"\n" +
                            "\n" +
                            "  # need this for relative symlinks\n" +
                            "  while [ -h \"$PRG\" ] ; do\n" +
                            "    ls=`ls -ld \"$PRG\"`\n" +
                            "    link=`expr \"$ls\" : '.*-> \\(.*\\)$'`\n" +
                            "    if expr \"$link\" : '/.*' > /dev/null; then\n" +
                            "      PRG=\"$link\"\n" +
                            "    else\n" +
                            "      PRG=\"`dirname \"$PRG\"`/$link\"\n" +
                            "    fi\n" +
                            "  done\n" +
                            "\n" +
                            "  saveddir=`pwd`\n" +
                            "\n" +
                            "  M2_HOME=`dirname \"$PRG\"`/..\n" +
                            "\n" +
                            "  # make it fully qualified\n" +
                            "  M2_HOME=`cd \"$M2_HOME\" && pwd`\n" +
                            "\n" +
                            "  cd \"$saveddir\"\n" +
                            "  # echo Using m2 at $M2_HOME\n" +
                            "fi\n" +
                            "\n" +
                            "# For Cygwin, ensure paths are in UNIX format before anything is touched\n" +
                            "if $cygwin ; then\n" +
                            "  [ -n \"$M2_HOME\" ] &&\n" +
                            "    M2_HOME=`cygpath --unix \"$M2_HOME\"`\n" +
                            "  [ -n \"$JAVA_HOME\" ] &&\n" +
                            "    JAVA_HOME=`cygpath --unix \"$JAVA_HOME\"`\n" +
                            "  [ -n \"$CLASSPATH\" ] &&\n" +
                            "    CLASSPATH=`cygpath --path --unix \"$CLASSPATH\"`\n" +
                            "fi\n" +
                            "\n" +
                            "# For Migwn, ensure paths are in UNIX format before anything is touched\n" +
                            "if $mingw ; then\n" +
                            "  [ -n \"$M2_HOME\" ] &&\n" +
                            "    M2_HOME=\"`(cd \"$M2_HOME\"; pwd)`\"\n" +
                            "  [ -n \"$JAVA_HOME\" ] &&\n" +
                            "    JAVA_HOME=\"`(cd \"$JAVA_HOME\"; pwd)`\"\n" +
                            "  # TODO classpath?\n" +
                            "fi\n" +
                            "\n" +
                            "if [ -z \"$JAVA_HOME\" ]; then\n" +
                            "  javaExecutable=\"`which javac`\"\n" +
                            "  if [ -n \"$javaExecutable\" ] && ! [ \"`expr \\\"$javaExecutable\\\" : '\\([^ ]*\\)'`\" = \"no\" ]; then\n" +
                            "    # readlink(1) is not available as standard on Solaris 10.\n" +
                            "    readLink=`which readlink`\n" +
                            "    if [ ! `expr \"$readLink\" : '\\([^ ]*\\)'` = \"no\" ]; then\n" +
                            "      if $darwin ; then\n" +
                            "        javaHome=\"`dirname \\\"$javaExecutable\\\"`\"\n" +
                            "        javaExecutable=\"`cd \\\"$javaHome\\\" && pwd -P`/javac\"\n" +
                            "      else\n" +
                            "        javaExecutable=\"`readlink -f \\\"$javaExecutable\\\"`\"\n" +
                            "      fi\n" +
                            "      javaHome=\"`dirname \\\"$javaExecutable\\\"`\"\n" +
                            "      javaHome=`expr \"$javaHome\" : '\\(.*\\)/bin'`\n" +
                            "      JAVA_HOME=\"$javaHome\"\n" +
                            "      export JAVA_HOME\n" +
                            "    fi\n" +
                            "  fi\n" +
                            "fi\n" +
                            "\n" +
                            "if [ -z \"$JAVACMD\" ] ; then\n" +
                            "  if [ -n \"$JAVA_HOME\"  ] ; then\n" +
                            "    if [ -x \"$JAVA_HOME/jre/sh/java\" ] ; then\n" +
                            "      # IBM's JDK on AIX uses strange locations for the executables\n" +
                            "      JAVACMD=\"$JAVA_HOME/jre/sh/java\"\n" +
                            "    else\n" +
                            "      JAVACMD=\"$JAVA_HOME/bin/java\"\n" +
                            "    fi\n" +
                            "  else\n" +
                            "    JAVACMD=\"`which java`\"\n" +
                            "  fi\n" +
                            "fi\n" +
                            "\n" +
                            "if [ ! -x \"$JAVACMD\" ] ; then\n" +
                            "  echo \"Error: JAVA_HOME is not defined correctly.\" >&2\n" +
                            "  echo \"  We cannot execute $JAVACMD\" >&2\n" +
                            "  exit 1\n" +
                            "fi\n" +
                            "\n" +
                            "if [ -z \"$JAVA_HOME\" ] ; then\n" +
                            "  echo \"Warning: JAVA_HOME environment variable is not set.\"\n" +
                            "fi\n" +
                            "\n" +
                            "# For Cygwin, switch paths to Windows format before running java\n" +
                            "if $cygwin; then\n" +
                            "  [ -n \"$M2_HOME\" ] &&\n" +
                            "    M2_HOME=`cygpath --path --windows \"$M2_HOME\"`\n" +
                            "  [ -n \"$JAVA_HOME\" ] &&\n" +
                            "    JAVA_HOME=`cygpath --path --windows \"$JAVA_HOME\"`\n" +
                            "  [ -n \"$CLASSPATH\" ] &&\n" +
                            "    CLASSPATH=`cygpath --path --windows \"$CLASSPATH\"`\n" +
                            "  [ -n \"$MAVEN_PROJECTBASEDIR\" ] &&\n" +
                            "    MAVEN_PROJECTBASEDIR=`cygpath --path --windows \"$MAVEN_PROJECTBASEDIR\"`\n" +
                            "fi\n" +
                            "\n" +
                            "# Check if M2_HOME is not set or is equal to the current project path\n" +
                            "if [ -z \"$M2_HOME\" ] || [ \"$M2_HOME\" = \"$(pwd)\" ]; then\n" +
                            "    # Set M2_HOME to the .m2 folder under the user's home directory\n" +
                            "    M2_HOME=\"$HOME/.m2\"\n" +
                            "fi\n" +
                            "\n" +
                            "# Add all JAR files under the lib folder to the classpath\n" +
                            "for jar_file in \"$ROOT\"/lib/*.jar; do\n" +
                            "    CLASSPATH=\"$CLASSPATH:$jar_file\"\n" +
                            "done\n" +
                            "\n" +
                            "# Java execution\n" +
                            "$JAVACMD \\\n" +
                            "$JAVA_OPTS \\\n" +
                            "-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/ \\\n" +
                            "-cp \"$ROOT/target/classes:$JAR_FILE$CLASSPATH:$ROOT/WEB-INF/lib/*:$ROOT/WEB-INF/classes:$M2_HOME/repository/org/tinystruct/tinystruct/$VERSION/tinystruct-$VERSION-jar-with-dependencies.jar\" org.tinystruct.system.Dispatcher \"$@\"";
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
     * @param app    Application to be installed.
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
