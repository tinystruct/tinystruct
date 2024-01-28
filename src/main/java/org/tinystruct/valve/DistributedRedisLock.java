package org.tinystruct.valve;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.system.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Distributed lock depends on Redis.
 *
 * @author James Zhou
 */
public class DistributedRedisLock implements Lock {
    private static final Logger logger = Logger.getLogger(DistributedRedisLock.class.getName());
    private static String lockScript;
    private static String unlockScript;

    static {
        ByteBuffer buff;
        try (InputStream stream = Objects.requireNonNull(DistributedRedisLock.class.getResource("/lock.lua")).openStream(); ReadableByteChannel channel = Channels.newChannel(stream)) {
            buff = ByteBuffer.allocate(800);
            channel.read(buff);
            lockScript = new String(buff.array(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Load lock.lua error.", e);
        }

        try (InputStream stream = Objects.requireNonNull(DistributedRedisLock.class.getResource("/unlock.lua")).openStream(); ReadableByteChannel channel = Channels.newChannel(stream)) {
            buff = ByteBuffer.allocate(1024);
            channel.read(buff);
            unlockScript = new String(buff.array(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Load unlock.lua error.", e);
        }
    }

    private final RedisURI uri;
    private final StatefulRedisConnection<String, String> redisClient;
    private String id;
    private final String value;

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

    public DistributedRedisLock(Configuration<String> settings) {
        this.uri = RedisURI.Builder.redis(settings.get("redis.host"), Integer.parseInt(settings.get("redis.port")))
                .withDatabase(1)
                .withTimeout(Duration.ofSeconds(60)).build();

        if (settings.get("redis.password") != null) {
            RedisCredentialsProvider credentialsProvider = new StaticCredentialsProvider("redis", settings.get("redis.password").toCharArray());
            this.uri.setCredentialsProvider(credentialsProvider);
        }

        this.redisClient = RedisClient.create(this.uri).connect();
        this.id = UUID.randomUUID().toString();
        this.value = "1";
    }

    public DistributedRedisLock(Configuration<String> settings, String id) {
        this(settings);
        this.id = id;
    }

    @Override
    public void lock() {
        while ((Long) (this.redisClient.sync().eval(lockScript, ScriptOutputType.INTEGER, new String[]{this.id, this.value}, "1000")) == 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new ApplicationRuntimeException(e.getMessage(), e.getCause());
            }
        }
    }

    @Override
    public boolean tryLock() {
        return this.redisClient.sync().eval(lockScript, ScriptOutputType.INTEGER, new String[]{this.id, this.value}, "1000");
    }

    @Override
    public boolean tryLock(long l, TimeUnit timeUnit) throws ApplicationException {
        return this.redisClient.sync().eval(lockScript, ScriptOutputType.INTEGER, new String[]{this.id, this.value}, timeUnit.toString());
    }

    @Override
    public void unlock() {
        this.redisClient.sync().eval(unlockScript, ScriptOutputType.INTEGER, this.id, this.value);
    }

    @Override
    public String id() {
        return this.id;
    }

}
