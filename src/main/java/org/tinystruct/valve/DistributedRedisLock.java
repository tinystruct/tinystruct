package org.tinystruct.valve;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import org.tinystruct.ApplicationException;
import org.tinystruct.system.Settings;

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

/**
 * Distributed lock depends on Redis.
 *
 * @author James Zhou
 */
public class DistributedRedisLock implements Lock {
    private static final Logger logger = Logger.getLogger(DistributedRedisLock.class.getName());
    private final RedisURI uri;
    private final StatefulRedisConnection<String, String> redisClient;
    private String id;
    private String value;
    private static String lockScript;
    private static String unlockScript;

    public DistributedRedisLock() {
        this(UUID.randomUUID().toString());
    }

    public DistributedRedisLock(String id) {
        this.uri = RedisURI.Builder.redis("127.0.0.1", 6379)
                .withDatabase(0)
                .withTimeout(Duration.ofSeconds(60))
                .build();
        this.redisClient = RedisClient.create(this.uri).connect();
        this.id = id;
        this.value = "1";
    }

    public DistributedRedisLock(Settings settings) {
        this.uri = RedisURI.Builder.redis(settings.get("redis.host"), Integer.parseInt(settings.get("redis.port")))
                .withDatabase(1)
                .withTimeout(Duration.ofSeconds(60)).build();

        if (settings.get("redis.password")!=null)
                this.uri.setPassword(settings.get("redis.password"));

        this.redisClient = RedisClient.create(this.uri).connect();
        this.id = UUID.randomUUID().toString();
        this.value = "1";
    }

    public DistributedRedisLock(Settings settings, String id) {
        this(settings);
        this.id = id;
    }


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

}
