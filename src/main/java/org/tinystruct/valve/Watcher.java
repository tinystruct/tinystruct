package org.tinystruct.valve;

import java.io.FileNotFoundException;
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
 *
 * @author James Zhou
 */
public final class Watcher implements Runnable {
    private static final Logger logger = Logger.getLogger(Watcher.class.getName());
    /**
     * Empty bytes.
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
    private final ConcurrentHashMap<String, EventListener> listeners = new ConcurrentHashMap<String, EventListener>(8);
    /**
     * Lock collection.
     */
    private final ConcurrentHashMap<String, Lock> locks;
    private int[] interspace;
    private volatile boolean started = false;
    private volatile boolean stopped = false;

    private Watcher() {
        this.locks = new ConcurrentHashMap<String, Lock>(16);
        try (RandomAccessFile lockFile = new RandomAccessFile(LOCK, "r")) {
            this.interspace = new int[(int) (lockFile.length() / FIXED_LOCK_DATA_SIZE + 1)];
        } catch (FileNotFoundException e) {
            logger.warning(e.getMessage());
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }

    public static Watcher getInstance() {
        return SingletonHolder.manager;
    }

    public void waitFor(String lockId) throws InterruptedException {
        this.listeners.get(lockId).waitFor();
    }

    public void waitFor(String lockId, long timeout, TimeUnit unit) throws InterruptedException {
        this.listeners.get(lockId).waitFor(timeout, unit);
    }

    /**
     * Read the .lock file and transform the information to the hash map in memory.
     */
    @Override
    public void run() {
        this.started = true;

        FileLock fileLock;
        while (!this.stopped) {
            synchronized (Watcher.class) {
                try {
                    Watcher.class.wait();
                } catch (InterruptedException e) {
                    logger.severe(e.getMessage());
                }
                // Synchronize the locks map with Lock file.
                try (RandomAccessFile lockFile = new RandomAccessFile(LOCK, "rwd")) {
                    // If the length of the lockFile is bigger than the default length: 44.
                    // then the size of the locks map would be easy to be calculated.
                    // Lock the file.
                    int size;
                    if (lockFile.length() >= FIXED_LOCK_DATA_SIZE) {
                        size = (int) lockFile.length() / FIXED_LOCK_DATA_SIZE;
                        fileLock = lockFile.getChannel().tryLock();

                        if (fileLock != null) {
                            // If the size more than zero
                            if (size > 0) {
                                // Seek the file from 0 position.
                                lockFile.seek(0);

                                String lockId;
                                EventListener listener;
                                // Assume all the locks are in use.
                                // Read all locks into the map.
                                for (int i = 0; i < size && lockFile.length() > 0; i++) { // Cautious!!!
                                    byte[] id = EMPTY_BYTES;
                                    boolean condition = lockFile.read(id) != -1 && lockFile.readLong() == 1L;
									// Read lock status.
									// If the lock is expired, then it should not be in the locks map.
									// Only read the lock which status is active.
									// Read lock id.
                                    if (condition) {
									    lockId = new String(id, StandardCharsets.UTF_8);
									    // No need to check if the lock exists.
									    if(!this.locks.containsKey(lockId)) {
									        // Add a new lock with id.
									        this.locks.putIfAbsent(lockId, new DistributedLock(id));
									        if ((listener = this.listeners.get(lockId)) != null)
									            listener.onCreate(lockId);
									    }
									}
									// Otherwise, the lock should not be in the locks map.
                                }
                            }

                            fileLock.release();
                        }
                        // Notify the other thread to work on.
                        Watcher.class.notifyAll();
                    } else {
                        lockFile.setLength(0);
                        Watcher.class.notifyAll();
                    }
                } catch (IOException e) {
                    // If there is IO Exception, then the Watcher should stop to synchronize.
                    this.stop();
                    logger.severe(e.getMessage());
                }
            }
        }
    }

    public void addListener(EventListener listener) {
        this.listeners.put(listener.id(), listener);
    }

    private void start() {
        Thread monitor = new Thread(this);
        monitor.setDaemon(true);
        monitor.start();
    }

    public boolean watch(final Lock lock) throws ApplicationException {
        synchronized (Lock.class) {
            // If the Watcher has not been started, then should be started to synchronize the locks.
            if (!this.started) {
                this.start();
            }
        }

        // Check if the lock is in the container.
        return locks.containsKey(lock.id());
    }

    public void register(Lock lock) throws ApplicationException {
        this.register(lock, 0L, TimeUnit.SECONDS);
    }

    public void register(Lock lock, long expiration, TimeUnit tu) throws ApplicationException {
        synchronized (Watcher.class) {
            String lockId = lock.id();
            if (!locks.containsKey(lockId)) {
                try (RandomAccessFile lockFile = new RandomAccessFile(LOCK, "rw")) {

                    FileLock fileLock = lockFile.getChannel().tryLock();

                    long length = lockFile.length();
                    if (null != fileLock) {
                        byte[] empty = EMPTY_BYTES;

                        int size;
                        if (length >= FIXED_LOCK_DATA_SIZE) {
                            size = (int) (length / FIXED_LOCK_DATA_SIZE);
                            this.interspace = new int[size];
                        } else
                            size = 0;
                        // If it's required to occupy for new space.
                        boolean required = true, registered = false;
                        if (size > 0) {
                            int position;
                            // Check if the lock id does exist, if so then start to
                            for (int i = 0; i < size; i++) {
                                position = i * FIXED_LOCK_DATA_SIZE;
                                lockFile.seek(position);
                                if (lockFile.read(empty) != -1) {
                                    if (Arrays.equals(lockId.getBytes(), empty)) {
                                        if (lockFile.readLong() == 0L) {
                                            // If the Lock does exist, then just update the status for the Lock
                                            lockFile.seek(position + EMPTY_BYTES.length);
                                            lockFile.writeLong(1L);
                                        }
                                        // Not been used in logic currently.
                                        this.interspace[i] = 0;
                                        required = false;
                                        registered = true;
                                        // Once get a space, then it's enough to be used for the current Lock.
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
                                        // If the Lock does exist, then just update the status for the Lock
                                        lockFile.seek((long) i * FIXED_LOCK_DATA_SIZE);
                                        lockFile.writeBytes(lockId);
                                        lockFile.writeLong(1L);

                                        this.interspace[i] = 0;
                                        required = false;
                                        // Once get a space, then it's enough to be used for the current Lock.
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
                        if (this.listeners.get(lockId) != null)
                            this.listeners.get(lockId).onCreate(lockId);

                        fileLock.release();

                        // Notify the other thread to work on.
                        Watcher.class.notify();
                    }
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
                    FileLock fileLock = lockFile.getChannel().tryLock();
                    if (null != fileLock) {
                        byte[] empty = EMPTY_BYTES;

                        int size = (int) (length / FIXED_LOCK_DATA_SIZE);
                        this.interspace = new int[size];

                        int availableLockSize = size;
                        int position;
                        for (int i = 0; i < size; i++) {
                            position = i * FIXED_LOCK_DATA_SIZE;
                            lockFile.seek(position);
                            boolean condition = lockFile.read(empty) != -1 && Arrays.equals(lockId.getBytes(), empty);
							if (condition) {
							    lockFile.seek(position + EMPTY_BYTES.length); // The pointer should be resumed.
							    lockFile.writeLong(0L);
							    this.locks.remove(lockId);
							    if (this.listeners.get(lockId) != null)
							        this.listeners.get(lockId).onDelete(lockId);

							    // If all locks are not available, then remove them all and set the lockFile to be empty.
							    if (--availableLockSize == 0) {
							        if (!this.locks.isEmpty())
							            this.locks.clear();

							        lockFile.setLength(0);
							    }

							    break;
							}
                        }

                        fileLock.release();
                        // Notify the other thread to work on.
                        Watcher.class.notify();
                    } else {
                        this.unregister(lock);
                    }
                } catch (IOException e) {
                    throw new ApplicationException(e.getMessage(), e.getCause());
                }
            }
        }
    }

    public Lock acquire() {
        synchronized (Watcher.class) {
            Lock lock;
            if (null != this.locks && this.locks.size() > 0 && null != (lock = this.locks.values().toArray(new Lock[]{})[0])) {
                return lock;
            }

            return new DistributedLock();
        }
    }

    public void stop() {
        this.stopped = this.locks.size() == 0;
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
     * Single instance holder for {@link Watcher}
     */
    private static final class SingletonHolder {
        static final Watcher manager = new Watcher();
    }

    /**
     * EventListener implementation for Lock.
     *
     * @author James Zhou
     */
    static class LockEventListener implements EventListener {
        private static final Logger logger = Logger.getLogger(LockEventListener.class.getName());
        private final Lock lock;
        private CountDownLatch latch;

        LockEventListener(Lock lock) {
            this.lock = lock;
        }

        @Override
        public void onCreate(String lockId) {
            if (lockId.equalsIgnoreCase(lock.id())) {
                latch = new CountDownLatch(1);
                logger.log(Level.INFO, "Created " + lockId);
            }
        }

        @Override
        public void onUpdate() {

        }

        @Override
        public void onDelete(String lockId) {
            if (lockId.equalsIgnoreCase(lock.id())) {
                logger.log(Level.INFO,"Deleted " + lockId);
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