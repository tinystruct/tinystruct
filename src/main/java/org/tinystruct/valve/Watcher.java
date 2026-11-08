package org.tinystruct.valve;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tinystruct.ApplicationException;

/**
 * Create a watcher to monitor the lock.
 * <p>
 * The Watcher class is responsible for monitoring and managing distributed
 * locks.
 * It reads and synchronizes locks with a lock file in the file system.
 * Lock events trigger corresponding listeners.
 * This class implements the Singleton pattern to ensure a single instance.
 *
 * @author James Zhou
 */
public final class Watcher implements Runnable {
    private static final Logger logger = Logger.getLogger(Watcher.class.getName());

    /**
     * Lock size.
     */
    private static final int FIXED_LOCK_DATA_SIZE = 44;
    /**
     * Lock file name.
     */
    private static final String LOCK = ".lock";
    /**
     * Upper bound on how long {@link #register} / {@link #unregister} will poll for the
     * whole-file advisory lock on {@link #LOCK} before giving up. Without this, a lock file
     * held by another process (including a leaked/zombie JVM from a previous run) would hang
     * every {@link DistributedLock} operation in this JVM forever, regardless of whatever
     * timeout the caller passed to {@link DistributedLock#tryLock(long, TimeUnit)} - the file
     * lock acquisition sat behind {@code synchronized (Watcher.class)} using the blocking,
     * no-timeout {@link FileChannel#lock()}.
     * <p>
     * Configurable through {@code distributed.lock.file.timeout} (milliseconds, default 5000);
     * see {@link LockSettings}.
     */
    private final long fileLockTimeoutMs = LockSettings.timeoutMs(LockSettings.FILE_LOCK_TIMEOUT,
            LockSettings.DEFAULT_FILE_LOCK_TIMEOUT_MS);
    /**
     * Size of the lock id stored at the start of each slot of the lock file.
     */
    private static final int ID_SIZE = 36;
    /**
     * Delay between successive {@link FileChannel#tryLock()} polling attempts.
     */
    private static final long FILE_LOCK_POLL_INTERVAL_MS = 20;
    /**
     * Lock event listeners.
     */
    private final ConcurrentHashMap<String, java.util.List<EventListener>> listeners = new ConcurrentHashMap<>(8);
    /**
     * Lock collection.
     */
    private final ConcurrentHashMap<String, Lock> locks;
    private volatile boolean started = false;
    private volatile boolean stopped = false;

    private Watcher() {
        this.locks = new ConcurrentHashMap<>(16);
    }

    /**
     * Get the instance of the Watcher using the Singleton pattern.
     *
     * @return The singleton instance of the Watcher.
     */
    public static Watcher getInstance() {
        return SingletonHolder.manager;
    }

    /**
     * Wait for the specified lock ID.
     *
     * @param lockId The ID of the lock to wait for.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public void waitFor(String lockId) throws InterruptedException {
        java.util.List<EventListener> list = this.listeners.get(lockId);
        if (list != null) {
            for (EventListener listener : list) {
                listener.waitFor();
            }
        }
    }

    /**
     * Wait for the specified lock ID with a timeout.
     *
     * @param lockId  The ID of the lock to wait for.
     * @param timeout The maximum time to wait.
     * @param unit    The time unit of the timeout argument.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public boolean waitFor(String lockId, long timeout, TimeUnit unit) throws InterruptedException {
        java.util.List<EventListener> list = this.listeners.get(lockId);
        if (list != null) {
            for (EventListener listener : list) {
                if (!listener.waitFor(timeout, unit))
                    return false;
            }
        }
        return true;
    }

    /**
     * Read the .lock file and transform the information to the hash map in memory.
     */
    @Override
    public void run() {
        this.started = true;

        while (!this.stopped) {
            synchronized (Watcher.class) {
                try {
                    Watcher.class.wait();
                } catch (InterruptedException e) {
                    logger.severe(e.getMessage());
                }
                // Synchronize the locks map with Lock file.
                synchronizeLocks();
            }
        }
    }

    /**
     * Synchronize locks by reading the lock file and updating the locks map
     * accordingly.
     */
    private void synchronizeLocks() {
        try (RandomAccessFile lockFile = new RandomAccessFile(LOCK, "rwd")) {
            int size;
            if (lockFile.length() >= FIXED_LOCK_DATA_SIZE) {
                size = (int) lockFile.length() / FIXED_LOCK_DATA_SIZE;
                try (FileLock fileLock = lockFile.getChannel().tryLock()) {
                    if (fileLock != null) {
                        updateLocksFromFile(lockFile.getChannel());
                    }
                }
            } else if (lockFile.length() > 0) {
                lockFile.setLength(0);
            }

            // Notify the other thread to work on.
            synchronized (Watcher.class) {
                Watcher.class.notifyAll();
            }
        } catch (IOException e) {
            this.stop();
            logger.severe(e.getMessage());
        }
    }

    /**
     * Update locks map based on the content of the lock file.
     */
    private void updateLocksFromFile(FileChannel channel) throws IOException {
        ByteBuffer slots = readSlots(channel);
        int size = slots.capacity() / FIXED_LOCK_DATA_SIZE;
        for (int i = 0; i < size; i++) {
            if (flag(slots, i) == 1L) {
                byte[] id = new byte[ID_SIZE];
                slots.get(i * FIXED_LOCK_DATA_SIZE, id);
                String lockId = new String(id, StandardCharsets.UTF_8);
                if (!this.locks.containsKey(lockId)) {
                    this.locks.putIfAbsent(lockId, new DistributedLock(id));
                    java.util.List<EventListener> list = this.listeners.get(lockId);
                    if (list != null) {
                        for (EventListener listener : list) {
                            listener.onCreate(lockId);
                        }
                    }
                }
            }
        }
    }

    /*
     * Lock file layout: a sequence of FIXED_LOCK_DATA_SIZE-byte slots, each a 36-byte lock id
     * followed by an 8-byte big-endian flag (1 = held, 0 = free). Everything below reads the
     * whole file with one read and scans it in memory, and writes a slot with one write.
     * RandomAccessFile.read(byte[]) is fine but readLong() is eight single-byte reads, and the
     * old code did that per slot, per scan - on a file kept long by stale entries that cost
     * hundreds of syscalls per register/unregister/monitor pass, all while holding the
     * Watcher.class monitor.
     */

    /**
     * Reads every complete slot of the lock file with a single read.
     */
    private static ByteBuffer readSlots(FileChannel channel) throws IOException {
        int size = (int) (channel.size() / FIXED_LOCK_DATA_SIZE);
        ByteBuffer slots = ByteBuffer.allocate(size * FIXED_LOCK_DATA_SIZE);
        long position = 0;
        while (slots.hasRemaining()) {
            int read = channel.read(slots, position);
            if (read < 0) {
                break; // file shrank underneath us; the unread tail stays zeroed, i.e. free
            }
            position += read;
        }
        return ByteBuffer.wrap(slots.array());
    }

    private static long flag(ByteBuffer slots, int slot) {
        return slots.getLong(slot * FIXED_LOCK_DATA_SIZE + ID_SIZE);
    }

    private static boolean idMatches(ByteBuffer slots, int slot, byte[] id) {
        int from = slot * FIXED_LOCK_DATA_SIZE;
        return Arrays.equals(slots.array(), from, from + ID_SIZE, id, 0, ID_SIZE);
    }

    /**
     * The on-disk form of a lock id: exactly {@link #ID_SIZE} bytes, zero padded if shorter.
     */
    private static byte[] idBytes(String lockId) {
        byte[] id = new byte[ID_SIZE];
        byte[] source = lockId.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(source, 0, id, 0, Math.min(source.length, ID_SIZE));
        return id;
    }

    private static void writeSlot(FileChannel channel, int slot, byte[] id, long flag) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(FIXED_LOCK_DATA_SIZE);
        buffer.put(id).putLong(flag).flip();
        writeFully(channel, buffer, (long) slot * FIXED_LOCK_DATA_SIZE);
    }

    private static void writeFlag(FileChannel channel, int slot, long flag) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(flag).flip();
        writeFully(channel, buffer, (long) slot * FIXED_LOCK_DATA_SIZE + ID_SIZE);
    }

    private static void writeFully(FileChannel channel, ByteBuffer buffer, long position) throws IOException {
        while (buffer.hasRemaining()) {
            position += channel.write(buffer, position);
        }
    }

    public void addListener(EventListener listener) {
        this.listeners.computeIfAbsent(listener.id(), k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(listener);
    }

    private void start() {
        Thread monitor = new Thread(this);
        monitor.setDaemon(true);
        monitor.start();
    }

    public boolean watch(final Lock lock) throws ApplicationException {
        synchronized (Watcher.class) {
            if (!this.started || this.stopped) {
                this.stopped = false;
                this.start();
            }
            return locks.containsKey(lock.id());
        }
    }

    public void register(Lock lock) throws ApplicationException {
        this.register(lock, 0L, TimeUnit.SECONDS);
    }

    public void register(Lock lock, long expiration, TimeUnit tu) throws ApplicationException {
        long timeoutMs = expiration > 0 ? tu.toMillis(expiration) : fileLockTimeoutMs;
        synchronized (Watcher.class) {
            String lockId = lock.id();
            if (!locks.containsKey(lockId)) {
                try (RandomAccessFile lockFile = new RandomAccessFile(LOCK, "rw");
                        FileLock fileLock = acquireFileLock(lockFile.getChannel(), timeoutMs)) {
                    FileChannel channel = lockFile.getChannel();
                    ByteBuffer slots = readSlots(channel);
                    int size = slots.capacity() / FIXED_LOCK_DATA_SIZE;
                    byte[] id = idBytes(lockId);

                    // Claim the slot already holding this id; otherwise the first free slot;
                    // otherwise append a new one.
                    int target = size;
                    boolean matched = false;
                    int firstFree = -1;
                    for (int i = 0; i < size; i++) {
                        if (idMatches(slots, i, id)) {
                            target = i;
                            matched = true;
                            break;
                        }
                        if (firstFree < 0 && flag(slots, i) == 0L) {
                            firstFree = i;
                        }
                    }

                    if (matched) {
                        if (flag(slots, target) == 0L) {
                            writeFlag(channel, target, 1L);
                        }
                    } else {
                        writeSlot(channel, firstFree >= 0 ? firstFree : size, id, 1L);
                    }

                    this.locks.putIfAbsent(lockId, lock);
                    java.util.List<EventListener> list = this.listeners.get(lockId);
                    if (list != null) {
                        for (EventListener listener : list) {
                            listener.onCreate(lockId);
                        }
                    }

                    Watcher.class.notifyAll();
                } catch (IOException e) {
                    throw new ApplicationException(e.getMessage(), e.getCause());
                }
            }
        }
    }

    public void unregister(Lock lock) throws ApplicationException {
        synchronized (Watcher.class) {
            String lockId = lock.id();
            if (locks.containsKey(lockId)) {
                try (RandomAccessFile lockFile = new RandomAccessFile(LOCK, "rw")) {
                    FileChannel channel = lockFile.getChannel();
                    if (channel.size() < FIXED_LOCK_DATA_SIZE)
                        return;
                    try (FileLock fileLock = acquireFileLock(channel, fileLockTimeoutMs)) {
                        ByteBuffer slots = readSlots(channel);
                        int size = slots.capacity() / FIXED_LOCK_DATA_SIZE;
                        byte[] id = idBytes(lockId);

                        for (int i = 0; i < size; i++) {
                            if (idMatches(slots, i, id)) {
                                writeFlag(channel, i, 0L);
                                slots.putLong(i * FIXED_LOCK_DATA_SIZE + ID_SIZE, 0L);
                                this.locks.remove(lockId);
                                java.util.List<EventListener> list = this.listeners.get(lockId);
                                if (list != null) {
                                    for (EventListener listener : list) {
                                        listener.onDelete(lockId);
                                    }
                                }

                                // If all locks are not available, then remove them all and set the lockFile to
                                // be empty.
                                if (isAllLocksAvailable(slots)) {
                                    if (!this.locks.isEmpty())
                                        this.locks.clear();

                                    lockFile.setLength(0);
                                }

                                break;
                            }
                        }
                        Watcher.class.notifyAll();
                    }
                } catch (IOException e) {
                    throw new ApplicationException(e.getMessage(), e.getCause());
                }
            }
        }
    }

    /**
     * Polls for the whole-file advisory lock on {@code channel} instead of blocking on it
     * indefinitely via {@link FileChannel#lock()}. A lock held by another process (or a
     * leaked/zombie process) must not be able to hang every {@link DistributedLock}
     * operation in this JVM forever.
     *
     * @throws ApplicationException if the lock cannot be acquired within {@code timeoutMs},
     *                               if interrupted while waiting, or on I/O failure.
     */
    private static FileLock acquireFileLock(FileChannel channel, long timeoutMs) throws ApplicationException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (true) {
            FileLock fileLock;
            try {
                fileLock = channel.tryLock();
            } catch (OverlappingFileLockException e) {
                // Held by another thread in this same JVM (e.g. the monitor thread); retry
                // exactly as if another process held it.
                fileLock = null;
            } catch (IOException e) {
                throw new ApplicationException(e.getMessage(), e);
            }

            if (fileLock != null) {
                return fileLock;
            }

            if (System.nanoTime() >= deadline) {
                throw new ApplicationException("Timed out after " + timeoutMs
                        + "ms waiting to acquire the lock file (" + LOCK + "); it may be held by another process.");
            }

            try {
                Thread.sleep(FILE_LOCK_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ApplicationException("Interrupted while waiting to acquire the lock file (" + LOCK + ")", e);
            }
        }
    }

    private boolean isAllLocksAvailable(ByteBuffer slots) {
        int size = slots.capacity() / FIXED_LOCK_DATA_SIZE;
        for (int i = 0; i < size; i++) {
            if (flag(slots, i) == 1L) {
                return false;
            }
        }
        return true;
    }

    public Lock acquire() {
        synchronized (Watcher.class) {
            Lock lock;
            if (null != this.locks && !this.locks.isEmpty()
                    && null != (lock = this.locks.values().toArray(new Lock[] {})[0])) {
                return lock;
            }

            return new DistributedLock();
        }
    }

    public void stop() {
        this.stopped = this.locks.isEmpty();
    }

    public interface EventListener {
        /**
         * To be triggered when a lock created.
         *
         * @param lockId lock id
         */
        void onCreate(String lockId);

        /**
         * To be triggered when a lock updated.
         */
        void onUpdate();

        /**
         * To be triggered when a lock deleted.
         *
         * @param lockId lock id
         */
        void onDelete(String lockId);

        /**
         * Listener Id.
         *
         * @return unique identifier
         */
        String id();

        void waitFor() throws InterruptedException;

        boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException;
    }

    /**
     * Holder for the single instance of Watcher.
     */
    private static final class SingletonHolder {
        static final Watcher manager = new Watcher();
    }

    /**
     * EventListener implementation for Lock.
     */
    static class LockEventListener implements EventListener {
        private static final Logger logger = Logger.getLogger(LockEventListener.class.getName());
        private final Lock lock;
        private volatile CountDownLatch latch = new CountDownLatch(1);

        LockEventListener(Lock lock) {
            this.lock = lock;
        }

        @Override
        public void onCreate(String lockId) {
            if (lockId.equalsIgnoreCase(lock.id())) {
                latch = new CountDownLatch(1);
                logger.log(Level.FINE, "Created " + lockId);
            }
        }

        @Override
        public void onUpdate() {

        }

        @Override
        public void onDelete(String lockId) {
            if (lockId.equalsIgnoreCase(lock.id())) {
                logger.log(Level.FINE, "Deleted " + lockId);
                latch.countDown();
            }
        }

        @Override
        public String id() {
            return lock.id();
        }

        @Override
        public void waitFor() throws InterruptedException {
            latch.await();
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }
}