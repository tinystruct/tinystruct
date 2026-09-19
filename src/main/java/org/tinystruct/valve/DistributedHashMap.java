package org.tinystruct.valve;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A distributed hash map implementation that uses file-based storage and distributed locking
 * for concurrent access control.
 *
 * @param <T> The type of elements in the queues stored in this map
 */
public class DistributedHashMap<T> extends ConcurrentHashMap<String, Queue<T>> implements AutoCloseable {
    private static final long serialVersionUID = 2329878484809829362L;
    private static final Logger logger = Logger.getLogger(DistributedHashMap.class.getName());

    private static final int HEADER_SIZE = 8; // 4 bytes for entry count, 4 bytes for version
    private static final int VERSION = 1;

    /**
     * Allow-list of classes that may be reconstituted from the on-disk data
     * file via {@link ObjectInputStream}. This closes off native Java
     * deserialization gadget-chain attacks (CWE-502): without a filter,
     * {@code readObject()} will instantiate *any* class named in the byte
     * stream, which is the same class of vulnerability that made libraries
     * like unsafe autoType feature dangerous. Only the queue
     * implementations this class is documented to store are permitted; any
     * other class encountered during deserialization causes the stream to
     * be rejected before an instance is ever constructed.
     */
    private static final ObjectInputFilter ALLOWED_CLASSES_FILTER = ObjectInputFilter.Config.createFilter(
            String.join(";",
                    ArrayDeque.class.getName(),
                    LinkedList.class.getName(),
                    PriorityQueue.class.getName(),
                    ConcurrentLinkedQueue.class.getName(),
                    LinkedBlockingQueue.class.getName(),
                    LinkedBlockingDeque.class.getName(),
                    Queue.class.getName(),
                    // Common boxed element types and arrays thereof.
                    "java.lang.*",
                    "java.math.*",
                    "!*" // reject everything else
            )
    );

    /**
     * File backing all no-argument-constructed instances in this working directory. Using a
     * fixed name (rather than a per-instance random {@link DistributedLock} id, as before) is
     * what makes this map "distributed": separate {@code DistributedHashMap} instances -
     * including ones created later, in the same or a different process - agree on where the
     * shared state lives instead of each silently getting its own private, empty file.
     */
    private static final String DEFAULT_DATA_FILE = ".distributed-hash-map.data";

    private RandomAccessFile data;
    private FileChannel channel;
    private final DistributedLock lock;
    private final String dataFilePath;
    private volatile int size;
    // Cheap (metadata-only) way to detect that another instance has written to the shared file
    // since we last synced, without paying for a full read+deserialize on every lookup.
    private volatile long lastSyncedFileLength;

    public DistributedHashMap() throws IOException {
        this(DEFAULT_DATA_FILE);
    }

    /**
     * Creates (or attaches to) a distributed map backed by the given file name. Instances
     * constructed with the same name - in this process or another - share the same data.
     *
     * @param dataFilePath path of the backing data file
     */
    public DistributedHashMap(String dataFilePath) throws IOException {
        this.lock = new DistributedLock();
        this.dataFilePath = dataFilePath;
        this.data = new RandomAccessFile(this.dataFilePath, "rw");
        this.channel = this.data.getChannel();

        // Initialize or load existing data
        if (this.data.length() == 0) {
            initializeNewFile();
        } else {
            try {
                lock.lock();
                reloadFromDisk();
            } finally {
                lock.unlock();
            }
        }
    }

    private void initializeNewFile() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        header.putInt(0);  // Initial size
        header.putInt(VERSION);  // Version
        header.flip();
        channel.write(header, 0);
        this.size = 0;
        this.lastSyncedFileLength = channel.size();
    }

    /**
     * Discards the in-memory contents and repopulates them from the current contents of the
     * backing file. Must be called while holding {@link #lock}.
     */
    @SuppressWarnings("unchecked")
    private void reloadFromDisk() throws IOException {
        super.clear();

        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        channel.read(header, 0);
        header.flip();

        this.size = header.getInt();
        int version = header.getInt();

        if (version != VERSION) {
            throw new IOException("Incompatible data file version");
        }

        // Read all entries
        long position = HEADER_SIZE;
        for (int i = 0; i < size; i++) {
            ByteBuffer entryHeader = ByteBuffer.allocate(8); // key length + value length
            channel.read(entryHeader, position);
            entryHeader.flip();

            int keyLength = entryHeader.getInt();
            int valueLength = entryHeader.getInt();

            ByteBuffer keyBuffer = ByteBuffer.allocate(keyLength);
            channel.read(keyBuffer, position + 8);
            String key = new String(keyBuffer.array());

            ByteBuffer valueBuffer = ByteBuffer.allocate(valueLength);
            channel.read(valueBuffer, position + 8 + keyLength);

            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(valueBuffer.array()))) {
                // Reject any class not on the allow-list before it can be
                // instantiated, preventing gadget-chain deserialization attacks
                // against this locally-stored data file.
                ois.setObjectInputFilter(ALLOWED_CLASSES_FILTER);
                Queue<T> value = (Queue<T>) ois.readObject();
                super.put(key, value);
            } catch (ClassNotFoundException e) {
                logger.log(Level.SEVERE, "Failed to deserialize value", e);
            } catch (InvalidClassException e) {
                logger.log(Level.SEVERE, "Rejected disallowed class while deserializing value from " + dataFilePath, e);
            }

            position += 8 + keyLength + valueLength;
        }

        this.lastSyncedFileLength = channel.size();
    }

    @Override
    public Queue<T> put(String key, Queue<T> value) {
        if (key == null || value == null) {
            // Preserve ConcurrentHashMap's contract (throws NullPointerException) instead of
            // masking it as a lock-acquisition failure.
            return super.put(key, value);
        }

        boolean locked = acquireLock();
        try {
            // Pick up entries written by other instances before appending ours, so the
            // header's entry count and the append position stay correct.
            reloadFromDisk();

            Queue<T> previous = super.put(key, value);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(value);
            }
            byte[] serializedValue = baos.toByteArray();
            byte[] keyBytes = key.getBytes();

            // Append at the real end of the file rather than a position derived from a
            // possibly-stale in-memory entry count.
            long position = channel.size();

            ByteBuffer entryHeader = ByteBuffer.allocate(8);
            entryHeader.putInt(keyBytes.length);
            entryHeader.putInt(serializedValue.length);
            entryHeader.flip();
            channel.write(entryHeader, position);

            channel.write(ByteBuffer.wrap(keyBytes), position + 8);
            channel.write(ByteBuffer.wrap(serializedValue), position + 8 + keyBytes.length);

            ByteBuffer header = ByteBuffer.allocate(4);
            header.putInt(++size);
            header.flip();
            channel.write(header, 0);
            this.lastSyncedFileLength = channel.size();

            return previous;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write to data file", e);
            throw new RuntimeException("Failed to persist data", e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    @Override
    public Queue<T> get(Object key) {
        Queue<T> value = super.get(key);
        if (value != null) {
            return value;
        }

        // Local miss: check cheaply (file-size only) whether another instance has written
        // data we don't know about yet before paying for a full reload.
        try {
            if (channel.size() == lastSyncedFileLength) {
                return null;
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to inspect data file size", e);
            return null;
        }

        boolean locked = acquireLock();
        try {
            reloadFromDisk();
            return super.get(key);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to reload distributed map from disk", e);
            return null;
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    @Override
    public Queue<T> remove(Object key) {
        boolean locked = acquireLock();
        try {
            reloadFromDisk();
            Queue<T> removed = super.remove(key);
            if (removed != null) {
                // Rewrite the entire file without the removed entry
                rewriteFile();
            }
            return removed;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to update data file", e);
            throw new RuntimeException("Failed to update data file", e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    /**
     * Acquires {@link #lock}, translating both a timeout and a failed attempt into the same
     * unchecked exception so callers can treat "didn't get the lock" uniformly - the return
     * value must still be checked before {@code finally}-block unlocking, since unconditionally
     * unlocking a lock this thread never acquired throws {@link IllegalMonitorStateException}.
     */
    private boolean acquireLock() {
        try {
            if (!lock.tryLock(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to acquire lock");
            }
            return true;
        } catch (org.tinystruct.ApplicationException e) {
            throw new RuntimeException("Failed to acquire lock", e);
        }
    }

    private void rewriteFile() throws IOException {
        // Create a temporary file
        File tempFile = new File(dataFilePath + ".tmp");
        try (RandomAccessFile tempRaf = new RandomAccessFile(tempFile, "rw");
             FileChannel tempChannel = tempRaf.getChannel()) {
            
            // Write header
            ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
            header.putInt(size - 1);
            header.putInt(VERSION);
            header.flip();
            tempChannel.write(header, 0);
            
            // Write all remaining entries
            long position = HEADER_SIZE;
            for (Map.Entry<String, Queue<T>> entry : this.entrySet()) {
                byte[] keyBytes = entry.getKey().getBytes();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(entry.getValue());
                byte[] valueBytes = baos.toByteArray();
                
                ByteBuffer entryHeader = ByteBuffer.allocate(8);
                entryHeader.putInt(keyBytes.length);
                entryHeader.putInt(valueBytes.length);
                entryHeader.flip();
                tempChannel.write(entryHeader, position);
                
                tempChannel.write(ByteBuffer.wrap(keyBytes), position + 8);
                tempChannel.write(ByteBuffer.wrap(valueBytes), position + 8 + keyBytes.length);
                
                position += 8 + keyBytes.length + valueBytes.length;
            }
        }
        
        // Close current file
        this.data.close();

        // Replace old file with new file. File.renameTo() is unreliable on Windows when the
        // destination was only just closed - antivirus/indexer scans can hold a transient
        // handle on it, making the move fail spuriously rather than overwrite - so use the NIO
        // move (which surfaces a real error instead of a bare boolean) and retry briefly.
        File oldFile = new File(dataFilePath);
        IOException moveFailure = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                java.nio.file.Files.move(tempFile.toPath(), oldFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                moveFailure = null;
                break;
            } catch (IOException e) {
                moveFailure = e;
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Reopen unconditionally - even on failure - so this instance is never left with a
        // permanently-closed channel: on failure oldFile is untouched, so re-attaching to it
        // just restores the pre-rewrite state instead of bricking every future operation.
        this.data = new RandomAccessFile(dataFilePath, "rw");
        this.channel = this.data.getChannel();

        if (moveFailure != null) {
            tempFile.delete();
            throw new IOException("Failed to replace data file", moveFailure);
        }

        size--;
        this.lastSyncedFileLength = channel.size();
    }

    @Override
    public void clear() {
        boolean locked = acquireLock();
        try {
            super.clear();
            // Truncate file and reinitialize
            channel.truncate(0);
            initializeNewFile();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to reset data file", e);
            throw new RuntimeException("Failed to reset data file", e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    @Override
    public void close() throws Exception {
        try {
            if (data != null) {
                data.close();
            }
            // Delete the data file
            new File(dataFilePath).delete();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during cleanup", e);
            throw e;
        }
    }
}
