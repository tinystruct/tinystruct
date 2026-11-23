/*******************************************************************************
 * Copyright  (c) 2013, 2026 James M. ZHOU
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a directory of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.tinystruct.data.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tinystruct.ApplicationException;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SQLiteGeneratorExceptionTest {

    @Test
    public void testCreatePreservesIOExceptionCause(@TempDir Path tempDir) {
        SQLiteGenerator generator = new SQLiteGenerator();
        // Point path to a file instead of a directory so Files.createDirectories(parent)
        // throws an IOException (Not a directory).
        Path blockingFile = tempDir.resolve("blocker");
        try {
            java.nio.file.Files.createFile(blockingFile);
        } catch (IOException e) {
            fail("Setup failed: could not create blocking file", e);
        }

        generator.setPath(blockingFile.resolve("nested").toString() + "/");
        generator.setPackageName("org.tinystruct.test.generated");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> {
            generator.create("DummyClass", "dummy_table");
        });

        assertNotNull(ex.getCause(), "ApplicationException cause should not be null");
        assertInstanceOf(IOException.class, ex.getCause(), "ApplicationException cause should be an IOException");
    }
}
