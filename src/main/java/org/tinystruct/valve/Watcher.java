package org.tinystruct.valve;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
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
     * Empty bytes array.
     */
    private final static byte[] EMPTY_BYTES = new byte[36];
    /**
     * Lock size.
     */
    private static final int FIXED_LOCK_DATA_SIZE = 44;
    /**
     * Lock file name.
     */
    private static final String LOCK = ".lock";
    /**
     * Lock event listeners.
     */
    private final ConcurrentHashMap<String, java.util.List<EventListener>> listeners = new ConcurrentHashMap<>(8);
    /**
     * Lock collection.
     */
    private final ConcurrentHashMap<String, Lock> locks;
    private int[] interspace;
    private volatile boolean started = false;
    private volatile boolean stopped = false;

    private Watcher() {
        this.locks = new ConcurrentHashMap<>(16);
        try (RandomAccessFile lockFile = new RandomAccessFile(LOCK, "r")) {
            this.interspace = new int[(int) (lockFile.length() / FIXED_LOCK_DATA_SIZE + 1)];
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
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
                        updateLocksFromFile(lockFile, size);
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
    private void updateLocksFromFile(RandomAccessFile lockFile, int size) throws IOException {
        if (size > 0) {
            lockFile.seek(0);
            String lockId;
            for (int i = 0; i < size; i++) {
                byte[] id = new byte[36];
                boolean condition = lockFile.read(id) != -1 && lockFile.readLong() == 1L;
                if (condition) {
                    lockId = new String(id, StandardCharsets.UTF_8);
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
        synchronized (Watcher.class) {
            String lockId = lock.id();
            if (!locks.containsKey(lockId)) {
                try (RandomAccessFile lockFile = new RandomAccessFile(LOCK, "rw");
                        FileLock fileLock = lockFile.getChannel().lock()) {
                    long length = lockFile.length();
                    byte[] emptyBuffer = new byte[36];

                    int size = (length >= FIXED_LOCK_DATA_SIZE) ? (int) (length / FIXED_LOCK_DATA_SIZE) : 0;
                    this.interspace = new int[size];

                    boolean required = true, registered = false;
                    if (size > 0) {
                        for (int i = 0; i < size; i++) {
                            int position = i * FIXED_LOCK_DATA_SIZE;
                            lockFile.seek(position);
                            if (lockFile.read(emptyBuffer) != -1) {
                                if (Arrays.equals(lockId.getBytes(StandardCharsets.UTF_8), emptyBuffer)) {
                                    if (lockFile.readLong() == 0L) {
                                        lockFile.seek(position + emptyBuffer.length);
                                        lockFile.writeLong(1L);
                                    }
                                    required = false;
                                    registered = true;
                                    break;
                                }
                                if (lockFile.readLong() == 0L) {
                                    this.interspace[i] = 1;
                                    required = false;
                                }
                            }
                        }

                        if (!registered) {
                            for (int i = 0; i < size; i++) {
                                if (this.interspace[i] == 1) {
                                    lockFile.seek((long) i * FIXED_LOCK_DATA_SIZE);
                                    lockFile.writeBytes(lockId);
                                    lockFile.writeLong(1L);
                                    required = false;
                                    break;
                                }
                            }
                        }
                    }

                    if (required) {
                        lockFile.seek((long) size * FIXED_LOCK_DATA_SIZE);
                        lockFile.writeBytes(lockId);
                        lockFile.writeLong(1L);
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
                    long length = lockFile.length();
                    if (length < FIXED_LOCK_DATA_SIZE)
                        return;
                    try (FileLock fileLock = lockFile.getChannel().lock()) {
                        byte[] empty = EMPTY_BYTES;

                        int size = (int) (length / FIXED_LOCK_DATA_SIZE);
                        this.interspace = new int[size];

                        int position;
                        for (int i = 0; i < size; i++) {
                            position = i * FIXED_LOCK_DATA_SIZE;
                            lockFile.seek(position);
                            boolean condition = lockFile.read(empty) != -1 && Arrays.equals(lockId.getBytes(), empty);
                            if (condition) {
                                lockFile.seek(position + EMPTY_BYTES.length); // The pointer should be resumed.
                                lockFile.writeLong(0L);
                                this.locks.remove(lockId);
                                java.util.List<EventListener> list = this.listeners.get(lockId);
                                if (list != null) {
                                    for (EventListener listener : list) {
                                        listener.onDelete(lockId);
                                    }
                                }

                                // If all locks are not available, then remove them all and set the lockFile to
                                // be empty.
                                if (isAllLocksAvailable(lockFile)) {
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

    private boolean isAllLocksAvailable(RandomAccessFile lockFile) throws IOException {
        long length = lockFile.length();
        if (length == 0)
            return true;

        lockFile.seek(0);
        int size = (int) (length / FIXED_LOCK_DATA_SIZE);
        for (int i = 0; i < size; i++) {
            lockFile.skipBytes(36);
            if (lockFile.readLong() == 1L) {
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