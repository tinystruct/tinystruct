/*******************************************************************************
 * Copyright  (c) 2013, 2026 James M. ZHOU
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

import org.tinystruct.AbstractApplication;
import org.tinystruct.Application;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Build-time generator that computes an auto-incrementing numeric version
 * (Git commit count) for each {@link Application} implementation and writes
 * the mapping to {@value AbstractApplication#APPLICATION_VERSIONS_RESOURCE}.
 * <p>
 * Exposed as the Dispatcher command {@code generate-versions}, so it can run
 * from the Maven build or from any tinystruct project via {@code bin/dispatcher}.
 */
public class ApplicationVersionGenerator extends AbstractApplication {

    private static final Logger LOGGER = Logger.getLogger(ApplicationVersionGenerator.class.getName());

    @Override
    public void init() {
        this.setTemplateRequired(false);
    }

    /**
     * Dispatcher command. Run it with:
     * <pre>
     * bin/dispatcher generate-versions --import org.tinystruct.system.ApplicationVersionGenerator
     * </pre>
     * All options are optional and default to the standard Maven layout under {@code --basedir}.
     * Any failure is logged and skipped so that a build is never broken by this step.
     */
    @Action(value = "generate-versions",
            description = "Generate auto-incrementing application versions from Git history",
            options = {
                    @Argument(key = "basedir", description = "Project base directory where Git runs (default: current directory)"),
                    @Argument(key = "source", description = "Java source root (default: <basedir>/src/main/java)"),
                    @Argument(key = "classes", description = "Compiled classes directory (default: <basedir>/target/classes)"),
                    @Argument(key = "output", description = "Output properties file (default: <classes>/" + AbstractApplication.APPLICATION_VERSIONS_RESOURCE + ")")
            },
            mode = Action.Mode.CLI)
    public void generateVersions() {
        Path baseDir = Paths.get(option("--basedir", "")).toAbsolutePath().normalize();
        Path sourceDir = baseDir.resolve(option("--source", "src/main/java"));
        Path classesDir = baseDir.resolve(option("--classes", "target/classes"));
        Path outputFile = baseDir.resolve(option("--output",
                classesDir.resolve(AbstractApplication.APPLICATION_VERSIONS_RESOURCE).toString()));

        try {
            generate(baseDir, sourceDir, classesDir, outputFile);
        } catch (Exception e) {
            // Dispatcher exits the JVM on uncaught exceptions, which would abort a Maven build
            // running this in-process; log and skip instead.
            LOGGER.log(Level.WARNING, "Application version generation failed; skipping. " + e.getMessage(), e);
        }
    }

    private String option(String key, String defaultValue) {
        Object value = getContext() == null ? null : getContext().getAttribute(key);
        if (value == null || value instanceof Boolean) {
            return defaultValue;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? defaultValue : text;
    }

    /**
     * Generates the application versions properties file from Git history.
     *
     * @param baseDir    Project base directory; Git runs here and paths are resolved relative to it
     * @param sourceDir  Java source root directory (e.g. {@code src/main/java})
     * @param classesDir Compiled classes directory (e.g. {@code target/classes})
     * @param outputFile Output properties file path
     * @throws IOException if writing the output file fails
     */
    public static void generate(Path baseDir, Path sourceDir, Path classesDir, Path outputFile) throws IOException {
        // Always (re)write the output, empty when skipped, so a file from an earlier build is never left stale
        writeProperties(computeVersions(baseDir, sourceDir, classesDir), outputFile);
    }

    private static Map<String, Integer> computeVersions(Path baseDir, Path sourceDir, Path classesDir) throws IOException {
        Path workDir = baseDir.toAbsolutePath().normalize();
        Path absSourceDir = sourceDir.toAbsolutePath().normalize();
        String relativeSource;
        try {
            relativeSource = workDir.relativize(absSourceDir).toString().replace('\\', '/');
        } catch (IllegalArgumentException e) {
            // e.g. different drive letters on Windows
            LOGGER.warning("Source directory " + absSourceDir + " is not under " + workDir
                    + "; skipping application version generation.");
            return new TreeMap<>();
        }
        if (!relativeSource.endsWith("/")) {
            relativeSource = relativeSource + "/";
        }

        if (isShallowRepository(workDir)) {
            LOGGER.warning("Shallow Git clone detected; skipping application version generation. "
                    + "Fetch full history (e.g. fetch-depth: 0) to enable it.");
            return new TreeMap<>();
        }

        Map<String, Integer> commitCounts = collectGitCommitCounts(workDir, relativeSource);
        if (commitCounts.isEmpty()) {
            return commitCounts;
        }
        return filterApplications(commitCounts, classesDir);
    }

    static Map<String, Integer> collectGitCommitCounts(Path workDir, String relativeSourcePrefix) {
        String targetPath = relativeSourcePrefix.endsWith("/")
                ? relativeSourcePrefix.substring(0, relativeSourcePrefix.length() - 1)
                : relativeSourcePrefix;

        // --relative makes output paths relative to workDir instead of the repository root,
        // so the source prefix still matches when the project is a subdirectory of a repo.
        // --name-status with -M reports renames, so history before a rename still counts toward the class.
        ProcessBuilder pb = new ProcessBuilder("git", "log", "--relative", "-M", "--format=", "--name-status", "--", targetPath);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(false);

        try {
            Process process = pb.start();
            Map<String, Integer> counts;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                counts = parseGitLog(reader, relativeSourcePrefix);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.fine("git log exited with code " + exitCode + "; skipping application version generation.");
                return new TreeMap<>();
            }
            return counts;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.fine("Git unavailable (" + e.getMessage() + "); skipping application version generation.");
            return new TreeMap<>();
        }
    }

    static boolean isShallowRepository(Path workDir) {
        ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--is-shallow-repository");
        pb.directory(workDir.toFile());
        try {
            Process process = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.readLine();
            }
            return process.waitFor() == 0 && "true".equals(output == null ? null : output.trim());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false; // Git unavailable; collectGitCommitCounts will handle it
        }
    }

    /**
     * Parses {@code git log --format= --name-status} output (newest commit first) into a map of
     * fully qualified class names to their commit counts. Plain {@code --name-only} lines are
     * also accepted.
     * <p>
     * A rename line ({@code R<score>\told\tnew}) credits all older commits on the old path
     * to the class's current name, so renaming a class does not reset its version.
     *
     * @param reader       reader over git log output
     * @param sourcePrefix normalized forward-slash source root prefix (e.g. {@code src/main/java/})
     * @return sorted map of class name to commit count
     * @throws IOException if reading fails
     */
    public static Map<String, Integer> parseGitLog(BufferedReader reader, String sourcePrefix) throws IOException {
        String normalizedPrefix = sourcePrefix.replace('\\', '/');
        if (!normalizedPrefix.isEmpty() && !normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix + "/";
        }

        Map<String, Integer> counts = new TreeMap<>();
        Map<String, String> renamedTo = new HashMap<>(); // old path -> current path
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("\t");
            String path;
            if (parts.length == 1) {
                path = resolve(renamedTo, parts[0].trim());
            } else if (parts[0].startsWith("R") && parts.length >= 3) {
                path = resolve(renamedTo, parts[2].trim());
                renamedTo.put(parts[1].trim(), path);
            } else {
                path = resolve(renamedTo, parts[parts.length - 1].trim());
            }

            if (!path.endsWith(".java") || !path.startsWith(normalizedPrefix)) {
                continue;
            }
            String relativeClassPath = path.substring(normalizedPrefix.length(), path.length() - ".java".length());
            counts.merge(relativeClassPath.replace('/', '.'), 1, Integer::sum);
        }
        return counts;
    }

    private static String resolve(Map<String, String> renamedTo, String path) {
        return renamedTo.getOrDefault(path, path);
    }

    private static Map<String, Integer> filterApplications(Map<String, Integer> commitCounts, Path classesDir) throws IOException {
        Map<String, Integer> appVersions = new TreeMap<>();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) {
            parent = ApplicationVersionGenerator.class.getClassLoader();
        }

        URL classesUrl = classesDir.toAbsolutePath().normalize().toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classesUrl}, parent)) {
            for (Map.Entry<String, Integer> entry : commitCounts.entrySet()) {
                String className = entry.getKey();
                Path classFile = classesDir.resolve(className.replace('.', '/') + ".class");
                if (!Files.isRegularFile(classFile)) {
                    continue;
                }
                try {
                    Class<?> cls = Class.forName(className, false, loader);
                    if (usesGeneratedVersion(cls)) {
                        appVersions.put(className, entry.getValue());
                    }
                } catch (Throwable ignored) {
                    // Skip classes whose optional runtime dependencies are absent
                }
            }
        }
        return appVersions;
    }

    /**
     * True for concrete applications whose {@code version()} is the inherited
     * {@link AbstractApplication#version()}. Abstract classes are never instantiated, and
     * classes that override {@code version()} never read the generated value.
     */
    public static boolean usesGeneratedVersion(Class<?> cls) throws NoSuchMethodException {
        if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers())
                || !Application.class.isAssignableFrom(cls)) {
            return false;
        }
        // Compare by name: cls may come from a different loader than this class
        return AbstractApplication.class.getName()
                .equals(cls.getMethod("version").getDeclaringClass().getName());
    }

    static void writeProperties(Map<String, Integer> versions, Path outputFile) throws IOException {
        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write("# Auto-generated application versions (Git commit count per Application)");
            writer.newLine();
            for (Map.Entry<String, Integer> entry : versions.entrySet()) {
                writer.write(entry.getKey());
                writer.write('=');
                writer.write(String.valueOf(entry.getValue()));
                writer.newLine();
            }
        }
    }
}
