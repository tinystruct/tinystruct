package org.tinystruct.system;

import org.junit.jupiter.api.*;
import org.tinystruct.ApplicationContext;

import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Only the dedicated static directory may be downloaded, never the project root.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpServerStaticFilesTest {

    private static final int TEST_PORT = 18081;
    private static final String BASE_URL = "http://localhost:" + TEST_PORT;

    private final Path staticDir = Paths.get(System.getProperty("user.dir")).resolve("static").toAbsolutePath();
    private boolean createdStaticDir;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    public void setUp() throws Exception {
        createdStaticDir = !Files.exists(staticDir);
        Files.createDirectories(staticDir.resolve("css"));
        Files.writeString(staticDir.resolve("static-test.txt"), "static-ok");
        Files.writeString(staticDir.resolve("css/static-test.css"), "body{}");
        Files.writeString(staticDir.resolve(".static-secret"), "static-secret");

        Settings settings = new Settings();
        settings.set("default.base_url", "/?q=");
        settings.set("default.language", "en_US");
        settings.set("charset", "utf-8");
        ApplicationManager.install(new StaticTestApp(), settings);
        ApplicationManager.install(new Dispatcher());
        HttpServer server = new HttpServer();
        ApplicationManager.install(server, settings);

        Thread serverThread = new Thread(() -> {
            try {
                ApplicationContext context = new ApplicationContext();
                context.setAttribute("--server-port", String.valueOf(TEST_PORT));
                server.setContext(context);
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        for (int i = 0; i < 30; i++) {
            try (Socket ignored = new Socket("localhost", TEST_PORT)) {
                Thread.sleep(500);
                return;
            } catch (Exception e) {
                Thread.sleep(1000);
            }
        }
        fail("Server failed to start within 30 seconds");
    }

    @AfterAll
    public void tearDown() throws Exception {
        Files.deleteIfExists(staticDir.resolve("static-test.txt"));
        Files.deleteIfExists(staticDir.resolve("css/static-test.css"));
        Files.deleteIfExists(staticDir.resolve(".static-secret"));
        Files.deleteIfExists(staticDir.resolve("css"));
        if (createdStaticDir) {
            try (Stream<Path> walk = Files.walk(staticDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(BASE_URL + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void servesFilesFromTheStaticDirectory() throws Exception {
        HttpResponse<String> response = get("/static-test.txt");
        assertEquals(200, response.statusCode());
        assertEquals("static-ok", response.body());
        assertEquals("body{}", get("/css/static-test.css").body());
    }

    @Test
    void staticDirectoryIsAlsoServedUnderItsOwnPath() throws Exception {
        // lets existing folders (e.g. ./themes) stay in place and keep their URLs
        assertEquals("static-ok", get("/static/static-test.txt").body());
        assertEquals(403, get("/static/../pom.xml").statusCode());
    }

    @Test
    void projectRootFilesCannotBeDownloaded() throws Exception {
        String pom = Files.readString(Paths.get("pom.xml"));
        for (String path : new String[]{"/pom.xml", "/src/main/java/org/tinystruct/system/HttpServer.java"}) {
            HttpResponse<String> response = get(path);
            assertEquals(403, response.statusCode(), path);
            assertNotEquals(pom, response.body(), path);
        }
        assertFalse(get("/.git/config").body().contains("[core]"), "git config must not be downloadable");
    }

    @Test
    void pathTraversalIsRejected() throws Exception {
        String pom = Files.readString(Paths.get("pom.xml"));
        for (String path : new String[]{"/static/../pom.xml", "/%2e%2e/pom.xml", "/css/..%2f..%2fpom.xml"}) {
            HttpResponse<String> response = get(path);
            assertNotEquals(pom, response.body(), path);
        }
        assertEquals(403, get("/static/../pom.xml").statusCode());
    }

    @Test
    void hiddenFilesInTheStaticDirectoryAreNotServed() throws Exception {
        assertNotEquals("static-secret", get("/.static-secret").body());
    }

    @Test
    void windowsAliasesOfStaticFilesAreNotServed() throws Exception {
        assertNotEquals("static-ok", get("/static-test.txt.").body());
        assertNotEquals("static-ok", get("/static-test.txt::$DATA").body());
    }

    @Test
    void dynamicUrlsWithDotsColonsAndSpacesAreNotForbidden() throws Exception {
        assertNotEquals(403, get("/say/Hello.").statusCode());
        assertNotEquals(403, get("/say/time%2010:30").statusCode());
        assertNotEquals(403, get("/.well-known/acme").statusCode());
    }

    @Test
    void directoriesOfTheProjectRootReachTheApplication() throws Exception {
        assertNotEquals(403, get("/src").statusCode());
        assertNotEquals(403, get("/target").statusCode());
    }

    public static class StaticTestApp extends org.tinystruct.AbstractApplication {
        @Override
        public void init() {
            this.setTemplateRequired(false);
        }

        @Override
        public String version() {
            return "test";
        }
    }
}
