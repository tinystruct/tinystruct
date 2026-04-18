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

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;

/**
 * Redis-backed implementation of {@link SessionRepository}.
 * Uses Lettuce as the Redis client.
 *
 * <p>Expects {@code redis.host}, {@code redis.port}, and optionally
 * {@code redis.password} to be set in {@code application.properties}.
 */
public class RedisSessionRepository implements SessionRepository {

    private final RedisCommands<String, String> commands;

    public RedisSessionRepository() {
        Configuration<String> settings = new Settings();
        String host = settings.get("redis.host") != null ? settings.get("redis.host") : "127.0.0.1";
        int port = settings.get("redis.port") != null ? Integer.parseInt(settings.get("redis.port")) : 6379;
        String password = settings.get("redis.password");

        RedisURI.Builder builder = RedisURI.builder()
                .withHost(host)
                .withPort(port);

        if (password != null && !password.isEmpty()) {
            builder.withPassword(password.toCharArray());
        }

        RedisClient client = RedisClient.create(builder.build());
        StatefulRedisConnection<String, String> connection = client.connect();
        this.commands = connection.sync();
    }

    @Override
    public Session findById(String sessionId) {
        String key = "session:" + sessionId;
        if (commands.exists(key) > 0) {
            return new RedisSession(sessionId, commands);
        }
        return null;
    }

    @Override
    public Session create(String sessionId) {
        RedisSession session = new RedisSession(sessionId, commands);
        // We set a marker to ensure the session exists in Redis
        session.setAttribute("_created", String.valueOf(System.currentTimeMillis()));
        return session;
    }

    @Override
    public void delete(String sessionId) {
        commands.del("session:" + sessionId);
    }
}
