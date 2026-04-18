/*******************************************************************************
 * Copyright  (c) 2025 James M. Zhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.tinystruct.http;

import io.lettuce.core.api.sync.RedisCommands;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Redis-backed {@link Session} implementation.
 *
 * <p>Session attributes are stored as a Redis hash at key
 * {@code session:<id>} with a TTL of {@link #TTL_SECONDS}.
 * Every {@link #getAttribute} call slides the expiry forward
 * (i.e. rolling expiration, matching {@link MemorySession} behaviour).
 *
 * <p>All values are persisted as strings. Callers who store non-string
 * objects must be prepared to receive a {@code String} back from
 * {@link #getAttribute}; this is consistent with how browser sessions
 * work in most frameworks.
 */
public class RedisSession implements Session {

    /** Rolling TTL — 30 minutes, matching MemorySession. */
    static final long TTL_SECONDS = 1800L;

    private static final String KEY_PREFIX = "session:";
    private static final Logger logger = Logger.getLogger(RedisSession.class.getName());

    private final String sessionId;
    private final RedisCommands<String, String> commands;
    private final String redisKey;

    RedisSession(String sessionId, RedisCommands<String, String> commands) {
        this.sessionId = sessionId;
        this.commands = commands;
        this.redisKey = KEY_PREFIX + sessionId;
    }

    @Override
    public String getId() {
        return sessionId;
    }

    @Override
    public void setAttribute(String key, Object value) {
        commands.hset(redisKey, key, value == null ? "" : value.toString());
        commands.expire(redisKey, TTL_SECONDS);
    }

    @Override
    public Object getAttribute(String key) {
        // Slide expiry on every access (rolling TTL)
        commands.expire(redisKey, TTL_SECONDS);
        return commands.hget(redisKey, key);
    }

    @Override
    public void removeAttribute(String key) {
        commands.hdel(redisKey, key);
    }

    @Override
    public boolean isExpired() {
        Long ttl = commands.ttl(redisKey);
        // ttl == -2 means the key does not exist; ttl == -1 means no expiry (unexpected)
        return ttl == null || ttl == -2;
    }

    /**
     * Load all attributes from Redis into a local snapshot map.
     * Useful for debugging or serialisation — not used in normal request flow.
     *
     * @return a copy of the hash stored in Redis, or an empty map if the key is gone
     */
    public Map<String, String> snapshot() {
        return commands.hgetall(redisKey);
    }
}
