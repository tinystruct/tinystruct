package org.tinystruct.transfer;

import org.tinystruct.ApplicationException;
import org.tinystruct.valve.DistributedLock;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DistributedHashMap<T> extends ConcurrentHashMap<String, Queue<T>> {

    private static final long serialVersionUID = 2329878484809829362L;

    private static final int FIXED_LOCK_DATA_SIZE = 44;
    private RandomAccessFile data;
    private int size;

    private DistributedLock lock;
    private String hash;

    public DistributedHashMap() {
        try {
            this.lock = new DistributedLock();
            this.hash = "." + this.lock.id();
            this.data = new RandomAccessFile(this.hash, "rw");
            this.size = (int) (this.data.length() / FIXED_LOCK_DATA_SIZE);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public Queue<T> get(Object key) {
        try {
            Queue<T> queue = new ArrayDeque<T>();
            FileLock fileLock;
            this.data = new RandomAccessFile("." + key, "rw");
            if (this.data.length() >= FIXED_LOCK_DATA_SIZE) {
                this.size = (int) this.data.length() / FIXED_LOCK_DATA_SIZE;
                fileLock = data.getChannel().tryLock(0L, Long.MAX_VALUE, false);

                if (fileLock != null) {
                    if (this.size > 0) {
                        data.seek(0);

                    }
                }
            } else {
                this.size = 0;
                if (queue.size() > 0) {

                }
            }


        } catch (Exception e) {

        }
        return super.get(key);
    }

    @Override
    public Queue<T> put(String key, Queue<T> value) {
        FileLock fileLock = null;
        try {
            this.lock.lock();

            this.data = new RandomAccessFile(this.hash, "rw");
            if (!this.contains(key)) {
                fileLock = data.getChannel().tryLock();

                long length = data.length();
                if (null != fileLock) {
                    if (length >= FIXED_LOCK_DATA_SIZE) {
                        this.size = (int) (length / FIXED_LOCK_DATA_SIZE);
                    } else
                        this.size = 0;

                    if (this.size > 0) {
                        data.seek(this.size * FIXED_LOCK_DATA_SIZE);
                        data.writeBytes(key);
                        data.writeLong(1L);
                    }

                    value = super.put(key, value);
                }
            }
            this.lock.unlock();
        } catch (ApplicationException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (fileLock != null) {
                    fileLock.release();
                    data.close();
                    data.getChannel().close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return value;

    }

    @Override
    public Queue<T> remove(Object key) {
        try {
            this.lock.lock();
            Paths.get(this.hash).toFile().delete();
            super.remove(key);
            this.lock.unlock();
        } catch (ApplicationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Set<Entry<String, Queue<T>>> entrySet() {
        // TODO Auto-generated method stub
        return super.entrySet();
    }

}
