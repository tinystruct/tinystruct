package org.tinystruct.valve;

import org.tinystruct.ApplicationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Watcher implements Runnable {
    private final String file = ".lock";
    private final static byte[] EMPTY_BYTES = new byte[36];
    private static final int FIXED_LOCK_DATA_SIZE = 44;

    private RandomAccessFile lockFile;
    private int[] interspace;
    private int size;
    private volatile ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<String, Lock>();
    private volatile boolean started = false;
    private volatile boolean stopped = false;
    private EventListener listener;

    private static final class SingletonHolder {
        static final Watcher manager = new Watcher();
    }

    public ConcurrentHashMap<String, Lock> getLocks() {
        return this.locks;
    }

    /**
     * Read the .lock file and transform the information to the hash map in memory.
     */
    @Override
    public void run() {
        this.started = true;
        while (!this.stopped) {
            FileLock fileLock = null;
            synchronized (Watcher.class) {
                try {
                    this.lockFile = new RandomAccessFile(file, "rw");
                    if (this.lockFile.length() >= FIXED_LOCK_DATA_SIZE) {
                        this.size = (int) this.lockFile.length() / FIXED_LOCK_DATA_SIZE;
                        fileLock = lockFile.getChannel().tryLock(0L, Long.MAX_VALUE, false);
                    } else {
                        this.size = 0;
                        if (this.locks.size() > 0) {
                            Enumeration<Lock> el = this.locks.elements();
                            while (el.hasMoreElements()) {
                                // If the size of the locks is not zero, then all locks should be notified to be
                                // released.
                                if (this.listener != null)
                                    this.listener.onDelete(el.nextElement().id());
                            }

                            this.locks.clear();
                        }
                        Watcher.class.notifyAll();
                    }

                    if (fileLock != null) {
                        if (this.size > 0) {
                            lockFile.seek(0);

                            String lockId;
                            int n = this.size;
                            for (int i = 0; i < this.size && lockFile.length() > 0; i++) { // Cautious!!!
                                byte[] id = EMPTY_BYTES;
                                lockFile.read(id);
                                lockId = new String(id);

                                // If the lock is expired, then it should not be in the locks map
                                if (lockFile.readLong() == 0L) {
//									this.interspace[i] = 1;
                                    n--;
                                    if (this.locks.containsKey(lockId)) {
                                        this.locks.remove(lockId);

                                        if (this.listener != null)
                                            this.listener.onDelete(lockId);
                                    }
                                }
                                // Otherwise, the lock should be in the locks map
                                else {
                                    if (!this.locks.containsKey(lockId)) {
                                        this.locks.put(lockId, new DistributedLock(id));
                                        if (this.listener != null)
                                            this.listener.onCreate(lockId);
                                    }
                                }


                                Watcher.class.notifyAll();
                            }

                            if (n == 0) {
                                this.locks.clear();
                                lockFile.setLength(0);
                                fileLock.release();
                                Watcher.class.notifyAll();
                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (fileLock != null) {
                            fileLock.release();
                            lockFile.close();
                            lockFile.getChannel().close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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

    public static Watcher getInstance() {
        return SingletonHolder.manager;
    }

    private Watcher() {
        try {
            this.lockFile = new RandomAccessFile(file, "rw");
            this.size = (int) (this.lockFile.length() / FIXED_LOCK_DATA_SIZE);
            this.interspace = new int[this.size + 1];
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setListener(EventListener listener) {
        this.listener = listener;
    }

    protected void start(boolean daemon) throws ApplicationException {
        Thread s = new Thread(this);
        s.setDaemon(daemon);
        s.start();
    }

    public boolean watch(Lock lock) throws ApplicationException {
        if (!this.started) {
            try {
                this.start(true);
            } catch (ApplicationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        synchronized (Watcher.class) {
            try {
                Watcher.class.wait();
                // Refresh the locks container
                if (locks.contains(lock))
                    return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return true;
            }

            return false;
        }
    }

    public void register(Lock lock) throws ApplicationException {
        this.register(lock, 0L, TimeUnit.SECONDS);
    }

    public void register(Lock lock, long expiration, TimeUnit tu) throws ApplicationException {
        synchronized (Watcher.class) {
            // TODO ...
            if (!locks.contains(lock)) {
                FileLock fileLock = null;
                try {
                    this.lockFile = new RandomAccessFile(file, "rw");

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

                        boolean append = true, registerred = false;
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

                                    this.interspace[i] = 0; // Not been used in logic currently.
                                    append = false;
                                    registerred = true;
                                    // Once get a space, then it's enough to be used for the current Lock.
                                    break;
                                }

                                if (lockFile.readLong() == 0L) {
                                    this.interspace[i] = 1;
                                    append = false;
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
                                        append = false;
                                        // Once get a space, then it's enough to be used for the current Lock.
                                        break;
                                    }
                                }
                            }
                        }

                        if (append) {
                            lockFile.seek(this.size * FIXED_LOCK_DATA_SIZE);
                            lockFile.writeBytes(id);
                            lockFile.writeLong(1L);
                        }

                        this.locks.put(id, lock);
                        this.listener.onCreate(id);

                        Watcher.class.notifyAll();
                    }
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    throw new ApplicationException(e.getMessage(), e.getCause());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    throw new ApplicationException(e.getMessage(), e.getCause());
                } finally {
                    try {
                        if (fileLock != null) {
                            fileLock.release();
                            lockFile.close();
                            lockFile.getChannel().close();
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        throw new ApplicationException(e.getMessage(), e.getCause());
                    }
                }
            }
        }
    }

    public void unregister(Lock lock) throws ApplicationException {
        synchronized (Watcher.class) {
            FileLock fileLock = null;
            try {
                lockFile = new RandomAccessFile(file, "rw");
                long length = lockFile.length();
                if (length < FIXED_LOCK_DATA_SIZE)
                    return;
                fileLock = lockFile.getChannel().tryLock();
                if (null != fileLock) {
                    byte[] empty = EMPTY_BYTES;

                    if (length >= FIXED_LOCK_DATA_SIZE) {
                        this.size = (int) (length / FIXED_LOCK_DATA_SIZE);
                        this.interspace = new int[this.size];
                    } else
                        this.size = 0;

                    int position = 0;
                    for (int i = 0; i < this.size; i++) {
                        position = i * FIXED_LOCK_DATA_SIZE;
                        lockFile.seek(position);
                        lockFile.read(empty);

                        if (Arrays.equals(lock.id().getBytes(), empty)) {
                            lockFile.seek(position + EMPTY_BYTES.length); // The pointer should be resumed.
                            lockFile.writeLong(0L);

                            this.locks.remove(lock.id());

                            if (this.listener != null)
                                this.listener.onDelete(lock.id());
                            break;
                        }
                    }

                    Watcher.class.notifyAll();
                } else {
                    Watcher.class.notifyAll();
                    this.unregister(lock);
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                throw new ApplicationException(e.getMessage(), e.getCause());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                throw new ApplicationException(e.getMessage(), e.getCause());
            } finally {
                try {
                    if (fileLock != null) {
                        fileLock.release();
                        lockFile.close();
                        lockFile.getChannel().close();
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    throw new ApplicationException(e.getMessage(), e.getCause());
                }
            }
        }
    }

    public Lock acquire() {
        return acquire(false);
    }

    public Lock acquire(boolean autocreate) {
        if (!this.started) {
            try {
                this.start(true);
            } catch (ApplicationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        synchronized (Watcher.class) {
            try {
                if (!this.started)
                    Watcher.class.wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Lock lock;
            if (null != this.locks && this.locks.size() > 0 && null != (lock = this.locks.elements().nextElement())) {
                return lock;
            }

            return autocreate ? new DistributedLock() : null;
        }
    }

    public static void main(String[] args) throws IOException {

        final Watcher watcher = Watcher.getInstance();
        watcher.setListener(new EventListener() {
            @Override
            public void onUpdate() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onDelete(String lockId) {
                // TODO Auto-generated method stub
                System.out.println(String.format("Deleted %s", lockId));
            }

            @Override
            public void onCreate(String lockId) {
                // TODO Auto-generated method stub
                System.out.println(String.format("Created %s", lockId));
            }
        });

        // Write data
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    // TODO Auto-generated method stub
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    for (int i = 0; i < 3; i++) {
                        try {
                            watcher.register(new DistributedLock((i + "00000000000000000000000000000000000").getBytes()));
                        } catch (ApplicationException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();

        try {
            watcher.start(false);
        } catch (ApplicationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void stop() {
        // TODO Auto-generated method stub
        this.stopped = this.locks.size() == 0;
    }

    public interface EventListener {
        /**
         * To be triggered when a lock created.
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
    }
}

