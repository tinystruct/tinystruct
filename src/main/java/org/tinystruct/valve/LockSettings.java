package org.tinystruct.valve;

import org.tinystruct.system.Settings;

import java.util.logging.Logger;

/**
 * Timeouts used by the file-backed locking classes in this package, read from
 * {@code application.properties} (all values are in milliseconds):
 *
 * <ul>
 *   <li>{@value #LOCK_TIMEOUT} - how long a {@link DistributedHashMap} operation waits to
 *       acquire its lock before failing. Default {@value #DEFAULT_LOCK_TIMEOUT_MS}.</li>
 *   <li>{@value #FILE_LOCK_TIMEOUT} - how long {@link Watcher} waits for the whole-file
 *       advisory lock on the {@code .lock} file. Default {@value #DEFAULT_FILE_LOCK_TIMEOUT_MS}.</li>
 * </ul>
 *
 * A missing, non-numeric or non-positive value falls back to the default.
 */
final class LockSettings {
    static final String LOCK_TIMEOUT = "distributed.lock.timeout";
    static final String FILE_LOCK_TIMEOUT = "distributed.lock.file.timeout";
    static final long DEFAULT_LOCK_TIMEOUT_MS = 5000;
    static final long DEFAULT_FILE_LOCK_TIMEOUT_MS = 5000;

    private static final Logger logger = Logger.getLogger(LockSettings.class.getName());

    private LockSettings() {
    }

    static long timeoutMs(String key, long defaultMs) {
        String value = new Settings().get(key);
        if (value.isEmpty()) {
            return defaultMs;
        }
        try {
            long ms = Long.parseLong(value);
            if (ms > 0) {
                return ms;
            }
        } catch (NumberFormatException ignored) {
            // reported below
        }
        logger.warning("Ignoring invalid " + key + "=" + value + " (expected a positive number of milliseconds); using " + defaultMs);
        return defaultMs;
    }
}
