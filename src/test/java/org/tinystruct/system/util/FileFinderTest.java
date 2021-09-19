package org.tinystruct.system.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class FileFinderTest {
    @Test
    public void TestProcess() throws IOException {
        Files.walkFileTree(Paths.get("."), new FileFinder("*.java") {
            @Override
            public void process(Path file) {
                System.out.println("file = " + file.toAbsolutePath());
            }
        });
    }
}
