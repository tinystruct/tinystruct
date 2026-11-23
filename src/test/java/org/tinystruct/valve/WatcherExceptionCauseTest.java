package org.tinystruct.valve;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tinystruct.ApplicationException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class WatcherExceptionCauseTest {

    @Test
    public void testRegisterPassesIOExceptionAsCause(@TempDir Path tempDir) throws Exception {
        // Create a directory where ".lock" would be, but make it a directory instead of a file
        // so that new RandomAccessFile(".lock", "rw") throws an IOException (e.g. Is a directory).
        Path lockPath = tempDir.resolve(".lock");
        Files.createDirectory(lockPath);

        // We need the relative path ".lock" resolved against the JVM working directory to point to our test directory.
        // Since we cannot change user.dir reliably for relative paths, we can back up any existing ".lock" in the working directory
        // and temporarily move our test directory's ".lock" or use the working directory's ".lock".
        // Wait, the instructions say: "If the defect can only be reached through a fixed path in the working directory (for example a hard-coded ".lock"),
        // you may change that path for the duration of the test, but only like this: if it exists, move it aside with Files.move to a backup name first;
        // create the condition you need; and in a finally block delete what you created and move the original back."

        Path workingLock = Path.of(".lock");
        Path backupLock = Path.of(".lock.bak." + UUID.randomUUID());
        boolean existed = Files.exists(workingLock);
        if (existed) {
            Files.move(workingLock, backupLock);
        }

        // Create a directory at workingLock (".lock") so RandomAccessFile throws an IOException.
        Files.createDirectory(workingLock);

        try {
            Watcher watcher = Watcher.getInstance();
            Lock lock = new DistributedLock(UUID.randomUUID().toString().getBytes());

            // Ensure the lock is not already in the watcher's locks map so it proceeds to try and open the file
            ApplicationException ex = assertThrows(ApplicationException.class, () -> {
                watcher.register(lock);
            });

            // The bug is that new ApplicationException(e.getMessage(), e.getCause()) passes e.getCause() (which is null)
            // instead of e (the IOException). Therefore, ex.getCause() should be an instance of IOException.
            assertNotNull(ex.getCause(), "ApplicationException cause should not be null");
            assertInstanceOf(IOException.class, ex.getCause(), "ApplicationException cause should be the caught IOException");

        } finally {
            // Clean up the created condition and restore original if it existed
            try {
                if (Files.exists(workingLock)) {
                    // Since it's a directory, delete it recursively or simply delete
                    try (var stream = Files.walk(workingLock)) {
                        stream.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException ignored) {
                            }
                        });
                    }
                }
            } finally {
                if (existed && Files.exists(backupLock)) {
                    Files.move(backupLock, workingLock);
                }
            }

            // Also clear the lock from Watcher if it got added or state needs cleaning
        }
    }
}
