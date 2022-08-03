package org.tinystruct.valve;

import org.tinystruct.ApplicationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Create a watcher to monitor the lock.
 *
 * @author James Zhou
 */
public final class Watcher implements Runnable {
    private static final Logger logger = Logger.getLogger(Watcher.class.getName());

    /**
     * Lock file name.
     */
    private final String file = ".lock";

    /**
     * Empty bytes.
     */
    private final static byte[] EMPTY_BYTES = new byte[36];

    /**
     * Lock size.
     */
    private static final int FIXED_LOCK_DATA_SIZE = 44;

    private int[] interspace;
    private int size;

    /**
     * Lock collection.
     */
    private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<String, Lock>(8);
    private volatile boolean started = false;
    private volatile boolean stopped = false;

    /**
     * Lock event listeners.
     */
    private final ConcurrentHashMap<String, EventListener> listeners = new ConcurrentHashMap<String, EventListener>(8);

    public void waitFor(String lockId) throws InterruptedException {
        this.listeners.get(lockId).waitFor();
    }

    public void waitFor(String lockId, long timeout, TimeUnit unit) throws InterruptedException {
        this.listeners.get(lockId).waitFor(timeout, unit);
    }

    /**
     * Single instance holder for {@link Watcher}
     */
    private static final class SingletonHolder {
        static final Watcher manager = new Watcher();
    }

    public static Watcher getInstance() {
        return SingletonHolder.manager;
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
                // Synchronize the locks map with Lock file.
                try (RandomAccessFile lockFile = new RandomAccessFile(file, "rw")) {
                    // If the length of the lockFile is bigger than the default length: 44.
                    // then the size of the locks map would be easy to be calculated.
                    // Lock the file.
                    if (lockFile.length() >= FIXED_LOCK_DATA_SIZE) {
                        this.size = (int) lockFile.length() / FIXED_LOCK_DATA_SIZE;
                        fileLock = lockFile.getChannel().tryLock(0L, Long.MAX_VALUE, false);

                        if (fileLock != null) {
                            // If the size more than zero
                            if (this.size > 0) {
                                // Seek the file from 0 position.
                                lockFile.seek(0);

                                String lockId;
                                EventListener listener;
                                // Assume all of the locks are in use.
                                int availableLockSize = this.size;
                                // Read all locks into the map.
                                for (int i = 0; i < this.size && lockFile.length() > 0; i++) { // Cautious!!!
                                    byte[] id = EMPTY_BYTES;
                                    // Read lock id.
                                    lockFile.read(id);
                                    lockId = new String(id);
                                    listener = this.listeners.get(lockId);
                                    // Read lock status.
                                    // If the lock is expired, then it should not be in the locks map
                                    if (lockFile.readLong() == 0L) {
                                        if (this.locks.containsKey(lockId)) {
                                            this.locks.remove(lockId);
                                            if (listener != null)
                                                listener.onDelete(lockId);
                                        }
                                        // If all locks are not available, then remove them all and set the lockFile to be empty.
                                        if (--availableLockSize == 0) {
                                            if (!this.locks.isEmpty())
                                                this.locks.clear();

                                            lockFile.setLength(0);
                                        }
                                    }
                                    // Otherwise, the lock should be in the locks map
                                    else {
                                        // Check if the lock exists.
                                        if (!this.locks.containsKey(lockId)) {
                                            // Add a new lock with id.
                                            this.locks.put(lockId, new DistributedLock(id));
                                            if (listener != null)
                                                listener.onCreate(lockId);
                                        }
                                    }
                                }
                            }

                            fileLock.release();
                            // Notify the other thread to work on.
                            Watcher.class.notifyAll();
                        }
                    } else {
                        this.size = 0;
                        lockFile.setLength(0);
                        Watcher.class.notifyAll();
                    }
                } catch (IOException e) {
                    // If there is IO Exception, then the Watcher should stop to synchronize.
                    this.stop();
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private Watcher() {
        try (RandomAccessFile lockFile = new RandomAccessFile(file, "r")) {
            this.size = (int) (lockFile.length() / FIXED_LOCK_DATA_SIZE);
            this.interspace = new int[this.size + 1];
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addListener(EventListener listener) {
        this.listeners.put(listener.id(), listener);
    }

    protected void start(boolean daemon) {
        Thread s = new Thread(this);
        s.setDaemon(daemon);
        s.start();
    }

    public boolean watch(Lock lock) throws ApplicationException {

        synchronized (Watcher.class) {
            // If the Watcher has not been started, then should be started to synchronize the locks.
            if (!this.started) {
                this.start(true);
            }

            try {
                Watcher.class.wait();
                // Check if the lock is in the container.
                return locks.contains(lock);
            } catch (InterruptedException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
        }
    }

    public void register(Lock lock) throws ApplicationException {
        this.register(lock, 0L, TimeUnit.SECONDS);
    }

    public void register(Lock lock, long expiration, TimeUnit tu) throws ApplicationException {
        synchronized (Watcher.class) {
            if (!locks.contains(lock)) {
                FileLock fileLock;
                try (RandomAccessFile lockFile = new RandomAccessFile(file, "rw")) {
                    String id = lock.id();
                    fileLock = lockFile.getChannel().tryLock();

                    long length = lockFile.length();
                    if (null != fileLock) {
                        byte[] empty = EMPTY_BYTES;

                        if (length >= FIXED_LOCK_DATA_SIZE) {
                            this.size = (int) (length / FIXED_LOCK_DATA_SIZE);
                            this.interspace = new int[this.size];
                        } else
                            this.size = 0;
                        // If it's required to occupy for new space.
                        boolean required = true, registerred = false;
                        if (this.size > 0) {
                            int position = 0;
                            // Check if the lock id does exist, if so then start to
                            for (int i = 0; i < this.size; i++) {
                                position = i * FIXED_LOCK_DATA_SIZE;
                                lockFile.seek(position);
                                lockFile.read(empty);
                                if (Arrays.equals(lock.id().getBytes(), empty)) {
                                    if (lockFile.readLong() == 0L) {
                                        // If the Lock does exist, then just update the status for the Lock
                                        lockFile.seek(position + EMPTY_BYTES.length);
                                        lockFile.writeLong(1L);
                                    }
                                    // Not been used in logic currently.
                                    this.interspace[i] = 0;
                                    required = false;
                                    registerred = true;
                                    // Once get a space, then it's enough to be used for the current Lock.
                                    break;
                                }

                                if (lockFile.readLong() == 0L) {
                                    this.interspace[i] = 1;
                                    required = false;
                                }
                            }

                            if (!registerred) {
                                for (int i = 0; i < this.size; i++) {
                                    if (this.interspace[i] == 1) {
                                        // If the Lock does exist, then just update the status for the Lock
                                        lockFile.seek(i * FIXED_LOCK_DATA_SIZE);
                                        lockFile.writeBytes(id);
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
                            lockFile.seek(this.size * FIXED_LOCK_DATA_SIZE);
                            lockFile.writeBytes(id);
                            lockFile.writeLong(1L);
                        }

                        this.locks.put(id, lock);
                        if (this.listeners.get(id) != null)
                            this.listeners.get(id).onCreate(id);

                        fileLock.release();

                        // Notify the other thread to work on.
                        Watcher.class.notifyAll();
                    }
                } catch (FileNotFoundException e) {
                    throw new ApplicationException(e.getMessage(), e.getCause());
                } catch (IOException e) {
                    throw new ApplicationException(e.getMessage(), e.getCause());
                }

            }
        }
    }

    public void unregister(Lock lock) throws ApplicationException {
        synchronized (Watcher.class) {
            try (RandomAccessFile lockFile = new RandomAccessFile(file, "rw")) {
                long length = lockFile.length();
                if (length < FIXED_LOCK_DATA_SIZE)
                    return;
                FileLock fileLock = lockFile.getChannel().tryLock();
                if (null != fileLock) {
                    byte[] empty = EMPTY_BYTES;

                    this.size = (int) (length / FIXED_LOCK_DATA_SIZE);
                    this.interspace = new int[this.size];

                    int position = 0;
                    String lockId = lock.id();
                    for (int i = 0; i < this.size; i++) {
                        position = i * FIXED_LOCK_DATA_SIZE;
                        lockFile.seek(position);
                        lockFile.read(empty);

                        if (Arrays.equals(lockId.getBytes(), empty)) {
                            lockFile.seek(position + EMPTY_BYTES.length); // The pointer should be resumed.
                            lockFile.writeLong(0L);
                            if (this.locks.containsKey(lockId)) {
                                this.locks.remove(lockId);

                                if (this.listeners.get(lockId) != null)
                                    this.listeners.get(lockId).onDelete(lockId);
                            }
                            break;
                        }
                    }

                    fileLock.release();
                    // Notify the other thread to work on.
                    Watcher.class.notifyAll();
                } else {
                    this.unregister(lock);
                }
            } catch (FileNotFoundException e) {
                throw new ApplicationException(e.getMessage(), e.getCause());
            } catch (IOException e) {
                throw new ApplicationException(e.getMessage(), e.getCause());
            }
        }
    }

    public Lock acquire() {
        synchronized (Watcher.class) {
            Lock lock;
            if (null != this.locks && this.locks.size() > 0 && null != (lock = this.locks.elements().nextElement())) {
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
}