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

import org.tinystruct.Application;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Action;
import org.tinystruct.application.Actions;
import org.tinystruct.application.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public final class ApplicationManager {
    private static final ConcurrentHashMap<String, Application> applications = new ConcurrentHashMap<String, Application>();
    private static final Actions actions = Actions.getInstance();
    private static Configuration<String> settings;
    private static volatile boolean initialized = false;
    public static final String VERSION = "0.1.5";

    private ApplicationManager() {
    }

    public static void init(final Configuration<String> config) throws ApplicationException {
        settings = config;
        init();
    }

    public static void init() throws ApplicationException {
        if (initialized) return;

        synchronized (ApplicationManager.class) {
            if (initialized) return;

            String paths = "bin/dispatcher";
            String origin = paths;
            Path path = Paths.get(paths);
            if (!Files.exists(path)) {
                try {
                    while (!Files.exists(path)) {
                        paths = paths.substring(0, paths.indexOf("/"));
                        path = Paths.get(paths);
                        Files.createDirectories(path);
                    }
                    path = Paths.get(origin);
                    Files.createFile(path);

                    String cmd = "#!/bin/sh\n" +
                            "ROOT=\"`pwd`\"\n" +
                            "VERSION=\"" + VERSION + "\"\n" +
                            "cd \"`dirname \"$0\"`\"\n" +
                            "cd \"../\"\n" +
                            "cd \"$ROOT\"\n" +
                            "java -cp \"$ROOT/lib/*:$ROOT/target/classes:$ROOT/WEB-INF/lib/*:$ROOT/WEB-INF/classes:$HOME/.m2/repository/org/tinystruct/tinystruct/$VERSION/tinystruct-$VERSION-jar-with-dependencies.jar\" org.tinystruct.system.Dispatcher \"$@\"\n";

                    Files.write(path, cmd.getBytes());
                } catch (IOException e) {
                    throw new ApplicationException(e.getMessage(), e);
                }
            }

            settings = settings == null ? new Settings("/application.properties") : settings;

            if (settings.get("default.import.applications").trim().length() > 0) {
                String[] apps = settings.get("default.import.applications").split(";");
                int i = 0;
                while (i < apps.length) {
                    if (apps[i].trim().length() > 0) {
                        try {
                            Application app = (Application) Class.forName(apps[i]).newInstance();
                            app.setConfiguration(settings);
                            ApplicationManager.install(app);
                        } catch (InstantiationException e) {
                            throw new ApplicationException(e.getMessage(), e);
                        } catch (IllegalAccessException e) {
                            throw new ApplicationException(e.getMessage(), e);
                        } catch (ClassNotFoundException e) {
                            throw new ApplicationException(e.getMessage(), e);
                        }
                    }

                    i++;
                }

                initialized = true;
            }


        }
    }

    public static void install(Application app) {
        if (!applications.containsKey(app.getName())) {
            applications.put(app.getName(), app);
        }
    }

    ;

    public static boolean uninstall(Application application) {
        applications.remove(application.getName());

        Actions actions = application.actions();
        Object[] list = actions.list().toArray();

        Action action;
        int i = 0;
        while (i < list.length) {
            action = (Action) list[i++];

            if (action.getApplicationName().equalsIgnoreCase(application.getName())) {
                actions.remove(action.getPath());
            }
        }

        return true;
    }

    ;

    public static Application get(String clsid) {
        return applications.get(clsid);
    }

    public static Collection<Application> list() {
        return applications.values();
    }

    public static Object call(final String path, final Context context) throws ApplicationException {
        if (path == null || path.trim().length() == 0) {
            throw new ApplicationException(
                    "Invalid: empty path");
        }

        String method = null;
        if (context != null && context.getAttribute("METHOD") != null) {
            method = context.getAttribute("METHOD").toString();
        }

        Action action = actions.getAction(path, method);
        if (action == null) {
            int pos = -1;
            String tpath = path;
            while ((pos = tpath.lastIndexOf('/')) != -1) {
                tpath = tpath.substring(0, pos);
                action = actions.getAction(tpath, method);

                if (action != null) {
                    if (context != null) {
                        context.setAttribute("REQUEST_ACTION", path);
                        action.setContext(context);
                    }

                    String arg = path.substring(pos + 1);
                    if (arg.length() > 0) {
                        String[] args = arg.split("/");

                        return action.execute(args);
                    }

                    return action.execute();
                }
            }

            throw new ApplicationException(
                    "Access error [" + path + "]: Application has not been installed, or it has been uninstalled already.");
        }

        if (context != null) {
            context.setAttribute("REQUEST_ACTION", path);
            action.setContext(context);
        }

        return action.execute();
    }

    public static Configuration<String> getConfiguration() {
        return settings;
    }
}