package org.tinystruct.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DispatcherAgentFilesTest {
    private static final String SKILL = "tinystruct-patterns";
    private static final String SYSTEM_PROPERTY = "tinystruct.agent.files";

    @TempDir
    Path root;

    private Dispatcher dispatcher;
    private Settings settings;
    private String savedProperty;

    @BeforeEach
    void setUp() {
        savedProperty = System.clearProperty(SYSTEM_PROPERTY);
        settings = new Settings(root.resolve("test.properties").toString());
        settings.remove("default.generate.agent_files");
        dispatcher = new Dispatcher();
        dispatcher.setConfiguration(settings);
    }

    @AfterEach
    void tearDown() {
        settings.remove("default.generate.agent_files");
        if (savedProperty == null) System.clearProperty(SYSTEM_PROPERTY);
        else System.setProperty(SYSTEM_PROPERTY, savedProperty);
    }

    @Test
    void generatesInstructionsAndBothSkillCopies() throws IOException {
        List<Path> created = dispatcher.generateAgentFiles(root, false);

        assertEquals(List.of(
                Path.of("AGENTS.md"),
                Path.of("CLAUDE.md"),
                Path.of(".agents", "skills", SKILL),
                Path.of(".claude", "skills", SKILL)), created);

        String agents = Files.readString(root.resolve("AGENTS.md"));
        assertTrue(agents.contains("(version " + ApplicationManager.VERSION + ")"));
        assertFalse(agents.contains("{%VERSION%}"));
        assertEquals("@AGENTS.md\n", Files.readString(root.resolve("CLAUDE.md")).replace("\r\n", "\n"));

        byte[] bundled = resource("META-INF/tinystruct/agents/skills/" + SKILL + "/SKILL.md");
        for (String location : new String[]{".agents", ".claude"}) {
            Path skillDir = root.resolve(location).resolve("skills").resolve(SKILL);
            assertArrayEquals(bundled, Files.readAllBytes(skillDir.resolve("SKILL.md")));
            assertTrue(Files.isRegularFile(skillDir.resolve("references/routing.md")));
        }
    }

    @Test
    void keepsExistingFilesWithoutForce() throws IOException {
        Files.writeString(root.resolve("AGENTS.md"), "custom");
        Path skillDir = Files.createDirectories(root.resolve(".agents/skills").resolve(SKILL));
        Files.writeString(skillDir.resolve("SKILL.md"), "custom skill");

        List<Path> created = dispatcher.generateAgentFiles(root, false);

        assertEquals("custom", Files.readString(root.resolve("AGENTS.md")));
        assertEquals("custom skill", Files.readString(skillDir.resolve("SKILL.md")));
        // An existing skill is left alone as a unit: no references are added to it.
        assertFalse(Files.exists(skillDir.resolve("references")));
        assertEquals(List.of(Path.of("CLAUDE.md"), Path.of(".claude", "skills", SKILL)), created);

        assertTrue(dispatcher.generateAgentFiles(root, false).isEmpty());
    }

    @Test
    void forceOverwritesExistingFiles() throws IOException {
        Files.writeString(root.resolve("AGENTS.md"), "custom");
        Path skillDir = Files.createDirectories(root.resolve(".claude/skills").resolve(SKILL));
        Files.writeString(skillDir.resolve("SKILL.md"), "custom skill");

        List<Path> created = dispatcher.generateAgentFiles(root, true);

        assertEquals(4, created.size());
        assertTrue(Files.readString(root.resolve("AGENTS.md")).startsWith("# AGENTS.md"));
        assertNotEquals("custom skill", Files.readString(skillDir.resolve("SKILL.md")));
    }

    @Test
    void automaticRunGeneratesInBuildProject() throws IOException {
        Files.writeString(root.resolve("pom.xml"), "<project/>");

        assertEquals(4, dispatcher.generateAgentFilesIfMissing(root).size());
        assertTrue(Files.exists(root.resolve("AGENTS.md")));
        assertTrue(dispatcher.generateAgentFilesIfMissing(root).isEmpty());
    }

    @Test
    void automaticRunAcceptsGradleBuild() throws IOException {
        Files.writeString(root.resolve("build.gradle.kts"), "");

        assertFalse(dispatcher.generateAgentFilesIfMissing(root).isEmpty());
    }

    @Test
    void automaticRunSkipsDirectoryWithoutBuildFile() {
        assertTrue(dispatcher.generateAgentFilesIfMissing(root).isEmpty());
        assertFalse(Files.exists(root.resolve("AGENTS.md")));
    }

    @Test
    void automaticRunHonoursSetting() throws IOException {
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        settings.set("default.generate.agent_files", "false");

        assertTrue(dispatcher.generateAgentFilesIfMissing(root).isEmpty());
        assertFalse(Files.exists(root.resolve("AGENTS.md")));
    }

    @Test
    void automaticRunHonoursSystemProperty() throws IOException {
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        System.setProperty(SYSTEM_PROPERTY, "false");

        assertTrue(dispatcher.generateAgentFilesIfMissing(root).isEmpty());
        assertFalse(Files.exists(root.resolve("AGENTS.md")));
    }

    @Test
    void automaticRunSkipsFrameworkSourceTree() throws IOException {
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        Path templates = Files.createDirectories(root.resolve("src/main/resources/META-INF/tinystruct/agents"));
        Files.writeString(templates.resolve("AGENTS.md"), "# template");

        assertTrue(dispatcher.generateAgentFilesIfMissing(root).isEmpty());
        assertFalse(Files.exists(root.resolve("AGENTS.md")));
    }

    private static byte[] resource(String name) throws IOException {
        try (InputStream in = DispatcherAgentFilesTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in, "Missing bundled resource " + name);
            return in.readAllBytes();
        }
    }
}
