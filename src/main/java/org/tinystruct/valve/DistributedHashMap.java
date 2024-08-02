package org.tinystruct.valve;

import org.tinystruct.system.Dispatcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DistributedHashMap implementation. TO BE COMPLETE.
 *
 * @param <T> Data type
 */
public class DistributedHashMap<T> extends ConcurrentHashMap<String, Queue<T>> {

    private static final long serialVersionUID = 2329878484809829362L;
    private static final Logger logger = Logger.getLogger(DistributedHashMap.class.getName());

    private static final int FIXED_LOCK_DATA_SIZE = 44;
    private RandomAccessFile data;
    private int size;

    private DistributedLock lock;
    private String hash;

    public DistributedHashMap() throws IOException {
        try {
            this.lock = new DistributedLock();
            this.hash = "." + this.lock.id();
            this.data = new RandomAccessFile(this.hash, "rw");
            this.size = (int) (this.data.length() / FIXED_LOCK_DATA_SIZE);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }


}
