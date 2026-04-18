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

/**
 * Strategy interface for session storage backends.
 *
 * <p>Implementations are selected at startup via the
 * {@code default.session.repository} property in {@code application.properties}:
 * <pre>
 *   # in-process (default when property is absent)
 *   default.session.repository=org.tinystruct.http.MemorySessionRepository
 *
 *   # Redis-backed (requires redis.host / redis.port / redis.password configuration)
 *   default.session.repository=org.tinystruct.http.RedisSessionRepository
 * </pre>
 */
public interface SessionRepository {

    /**
     * Load an existing session by ID, or return {@code null} if it does not exist.
     *
     * @param sessionId the session identifier
     * @return the session, or {@code null}
     */
    Session findById(String sessionId);

    /**
     * Create and persist a new session with the given ID.
     *
     * @param sessionId the session identifier
     * @return the newly created session
     */
    Session create(String sessionId);

    /**
     * Remove a session by ID.
     *
     * @param sessionId the session identifier
     */
    void delete(String sessionId);
}
