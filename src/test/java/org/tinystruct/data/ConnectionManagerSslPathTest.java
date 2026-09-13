/*******************************************************************************
 * Copyright  (c) 2023, 2026 James M. Zhou
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
package org.tinystruct.data;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ConnectionManager#resolveClasspathFileParams(String)} — the rewriting of
 * certificate/key parameters in a JDBC URL query string to absolute filesystem paths.
 */
class ConnectionManagerSslPathTest {

    private static final String RESOURCE = "certs/tinystruct-test-root.pem";

    private static String expectedAbsolutePath() throws Exception {
        return Paths.get(
                        Thread.currentThread().getContextClassLoader().getResource(RESOURCE).toURI())
                .toAbsolutePath().normalize().toString().replace('\\', '/');
    }

    @Test
    void nullAndEmptyAndNoParamsAreUntouched() {
        assertNull(ConnectionManager.resolveClasspathFileParams(null));
        assertEquals("", ConnectionManager.resolveClasspathFileParams(""));
        assertEquals("sslmode=verify-full",
                ConnectionManager.resolveClasspathFileParams("sslmode=verify-full"));
    }

    @Test
    void nonFileParamsAreNotRewritten() {
        String query = "sslmode=verify-full&user=postgres&ApplicationName=tinystruct";
        assertEquals(query, ConnectionManager.resolveClasspathFileParams(query));
    }

    @Test
    void classpathResourceIsResolvedToAbsoluteFile() throws Exception {
        String result = ConnectionManager.resolveClasspathFileParams(
                "sslmode=verify-full&sslrootcert=" + RESOURCE);

        assertEquals("sslmode=verify-full&sslrootcert=" + expectedAbsolutePath(), result);

        String resolved = result.substring(result.indexOf("sslrootcert=") + "sslrootcert=".length());
        Path path = Paths.get(resolved);
        assertTrue(path.isAbsolute(), "sslrootcert should be rewritten to an absolute path");
        assertTrue(Files.isReadable(path), "rewritten sslrootcert should point at a readable file");
    }

    @Test
    void leadingDotSlashResourceIsResolved() throws Exception {
        String result = ConnectionManager.resolveClasspathFileParams("sslrootcert=./" + RESOURCE);
        assertEquals("sslrootcert=" + expectedAbsolutePath(), result);
    }

    @Test
    void parameterKeyMatchIsCaseInsensitiveAndKeyCaseIsPreserved() throws Exception {
        String result = ConnectionManager.resolveClasspathFileParams("SSLRootCert=" + RESOURCE);
        assertEquals("SSLRootCert=" + expectedAbsolutePath(), result);
    }

    @Test
    void surroundingParametersArePreserved() throws Exception {
        String result = ConnectionManager.resolveClasspathFileParams(
                "a=1&sslrootcert=" + RESOURCE + "&z=9");
        assertEquals("a=1&sslrootcert=" + expectedAbsolutePath() + "&z=9", result);
    }

    @Test
    void unknownResourceIsLeftUnchanged() {
        String query = "sslmode=verify-full&sslrootcert=certs/does-not-exist.pem";
        assertEquals(query, ConnectionManager.resolveClasspathFileParams(query));
    }

    @Test
    void alreadyAbsoluteExistingPathIsStableAcrossCalls() throws Exception {
        String once = ConnectionManager.resolveClasspathFileParams("sslrootcert=" + RESOURCE);
        String twice = ConnectionManager.resolveClasspathFileParams(once);
        assertEquals(once, twice);
    }

    @Test
    void sslKeyIsNotResolvedFromClasspath() {
        // sslkey contains private key material and must NOT be extracted to a shared temp
        // directory. Only sslrootcert and sslcert (public certificates) are resolved.
        String query = "sslkey=" + RESOURCE;
        assertEquals(query, ConnectionManager.resolveClasspathFileParams(query));
    }
}
