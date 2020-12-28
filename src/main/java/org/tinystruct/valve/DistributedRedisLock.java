package org.tinystruct.valve;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import org.tinystruct.ApplicationException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.channels.FileChannel.open;

public class DistributedRedisLock implements Lock {
    private static final Logger logger = Logger.getLogger(DistributedRedisLock.class.getName());
    private final RedisURI uri;
    private final StatefulRedisConnection<String, String> redisClient;
    private String id;
    private String value;
    private static String lockScript;
    private static String unlockScript;

    static {
        ByteBuffer buff;
        try (FileChannel channel = open(
                Paths.get(DistributedRedisLock.class.getClassLoader().getResource("lock.lua").toURI()
                ))) {
            buff = ByteBuffer.allocate(800);
            channel.read(buff);
            lockScript = new String(buff.array(), StandardCharsets.UTF_8).trim();
        } catch (IOException | URISyntaxException e) {
            logger.log(Level.SEVERE, "Load lock.lua error.", e);
        }

        try (FileChannel channel = open(Paths.get(DistributedRedisLock.class.getClassLoader().getResource("unlock.lua").toURI()))) {
            buff = ByteBuffer.allocate(1024);
            channel.read(buff);
            unlockScript = new String(buff.array(), StandardCharsets.UTF_8).trim();
        } catch (IOException | URISyntaxException e) {
            logger.log(Level.SEVERE, "Load unlock.lua error.", e);
        }
    }

    public DistributedRedisLock() {
        this.uri = RedisURI.Builder.redis("localhost", 6379)
//                .withPassword("password")
                .withDatabase(1)
                .withTimeout(Duration.ofSeconds(60))
                .build();
        this.redisClient = RedisClient.create(this.uri).connect();
        this.id = UUID.randomUUID().toString();
        this.value = "1";
    }

    public DistributedRedisLock(String id) {
        this.uri = RedisURI.Builder.redis("localhost", 6379)
//                .withPassword("password")
                .withDatabase(1)
                .withTimeout(Duration.ofSeconds(60))
                .build();
        this.redisClient = RedisClient.create(this.uri).connect();
        this.id = id;
        this.value = "1";
    }

    @Override
    public void lock() throws ApplicationException {
        while ((Long) (this.redisClient.sync().eval(lockScript, ScriptOutputType.INTEGER, new String[]{this.id, this.value}, "1000")) == 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {

            }
        }
    }

    @Override
    public boolean tryLock() throws ApplicationException {
        return this.redisClient.sync().eval(lockScript, ScriptOutputType.INTEGER, new String[]{this.id, this.value}, "1000");
    }

    @Override
    public boolean tryLock(long l, TimeUnit timeUnit) throws ApplicationException {
        return this.redisClient.sync().eval(lockScript, ScriptOutputType.INTEGER, new String[]{this.id, this.value}, timeUnit.toString());
    }

    @Override
    public void unlock() throws ApplicationException {
        this.redisClient.sync().eval(unlockScript, ScriptOutputType.INTEGER, this.id, this.value);
    }

    @Override
    public String id() {
        return this.id;
    }

    private volatile static int tickets = 100;

    static class ticket implements Runnable {
        Lock lock = Watcher.getInstance().acquire(true);

        //        Lock lock = new DistributedRedisLock("ticket");
        @Override
        public void run() {
            while (tickets > 0) {
                try {
                    if (lock != null) {
                        logger.info("Lock Id:" + lock.id());
                        lock.lock();
                    }
                    if (tickets > 0) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        logger.info(Thread.currentThread().getName() + " is selling #" + (tickets--));
                    }
                } catch (ApplicationException e) {
                    logger.log(Level.SEVERE, "Execute error.", e);
                } finally {
                    try {
                        if (lock != null) {
                            lock.unlock();
                        }
                    } catch (ApplicationException e) {
                        logger.log(Level.SEVERE, "Unlock error.", e);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 20; i++) {
            new Thread(new ticket(), "Window #" + i).start();
        }
    }
}
