package org.tinystruct.valve;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A distributed hash map implementation that uses file-based storage and distributed locking
 * for concurrent access control.
 *
 * @param <T> The type of elements in the queues stored in this map
 */
public class DistributedHashMap<T> extends ConcurrentHashMap<String, Queue<T>> {
    private static final long serialVersionUID = 2329878484809829362L;
    private static final Logger logger = Logger.getLogger(DistributedHashMap.class.getName());

    private static final int FIXED_LOCK_DATA_SIZE = 44;
    private static final int HEADER_SIZE = 8; // 4 bytes for entry count, 4 bytes for version
    private static final int VERSION = 1;

    private RandomAccessFile data;
    private final FileChannel channel;
    private final DistributedLock lock;
    private final String dataFilePath;
    private volatile int size;

    public DistributedHashMap() throws IOException {
        this.lock = new DistributedLock();
        this.dataFilePath = "." + this.lock.id() + ".data";
        this.data = new RandomAccessFile(this.dataFilePath, "rw");
        this.channel = this.data.getChannel();
        
        // Initialize or load existing data
        if (this.data.length() == 0) {
            initializeNewFile();
        } else {
            loadExistingData();
        }
    }

    private void initializeNewFile() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        header.putInt(0);  // Initial size
        header.putInt(VERSION);  // Version
        header.flip();
        channel.write(header, 0);
        this.size = 0;
    }

    @SuppressWarnings("unchecked")
    private void loadExistingData() throws IOException {
        try {
            lock.lock();
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
                    Queue<T> value = (Queue<T>) ois.readObject();
                    super.put(key, value);
                } catch (ClassNotFoundException e) {
                    logger.log(Level.SEVERE, "Failed to deserialize value", e);
                }
                
                position += 8 + keyLength + valueLength;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Queue<T> put(String key, Queue<T> value) {
        try {
            lock.tryLock(5, TimeUnit.SECONDS);
            Queue<T> previous = super.put(key, value);
            
            // Write to file
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(value);
                byte[] serializedValue = baos.toByteArray();
                byte[] keyBytes = key.getBytes();
                
                // Calculate position for new entry
                long position = calculatePositionForNewEntry();
                
                // Write entry header (key length + value length)
                ByteBuffer entryHeader = ByteBuffer.allocate(8);
                entryHeader.putInt(keyBytes.length);
                entryHeader.putInt(serializedValue.length);
                entryHeader.flip();
                channel.write(entryHeader, position);
                
                // Write key and value
                channel.write(ByteBuffer.wrap(keyBytes), position + 8);
                channel.write(ByteBuffer.wrap(serializedValue), position + 8 + keyBytes.length);
                
                // Update size in header
                ByteBuffer header = ByteBuffer.allocate(4);
                header.putInt(++size);
                header.flip();
                channel.write(header, 0);
                
                return previous;
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to write to data file", e);
                throw new RuntimeException("Failed to persist data", e);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to acquire lock", e);
            throw new RuntimeException("Failed to acquire lock", e);
        } finally {
            lock.unlock();
        }
    }

    private long calculatePositionForNewEntry() throws IOException {
        // Skip header
        long position = HEADER_SIZE;
        
        // Skip existing entries
        for (int i = 0; i < size; i++) {
            ByteBuffer entryHeader = ByteBuffer.allocate(8);
            channel.read(entryHeader, position);
            entryHeader.flip();
            
            int keyLength = entryHeader.getInt();
            int valueLength = entryHeader.getInt();
            
            position += 8 + keyLength + valueLength;
        }
        
        return position;
    }

    @Override
    public Queue<T> remove(Object key) {
        try {
            lock.tryLock(5, TimeUnit.SECONDS);
            Queue<T> removed = super.remove(key);
            if (removed != null) {
                // Rewrite the entire file without the removed entry
                rewriteFile();
            }
            return removed;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to acquire lock", e);
            throw new RuntimeException("Failed to acquire lock", e);
        } finally {
            lock.unlock();
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
        
        // Replace old file with new file
        File oldFile = new File(dataFilePath);
        if (!tempFile.renameTo(oldFile)) {
            throw new IOException("Failed to replace data file");
        }
        
        // Reopen the file
        this.data = new RandomAccessFile(dataFilePath, "rw");
        size--;
    }

    @Override
    public void clear() {
        try {
            lock.tryLock(5, TimeUnit.SECONDS);
            super.clear();
            // Truncate file and reinitialize
            channel.truncate(0);
            initializeNewFile();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to acquire lock", e);
            throw new RuntimeException("Failed to acquire lock", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (data != null) {
                data.close();
            }
            // Delete the data file
            new File(dataFilePath).delete();
        } finally {
            super.finalize();
        }
    }
}
