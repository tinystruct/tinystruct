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
 * In-process {@link SessionRepository} backed by {@link SessionManager}'s
 * {@link java.util.concurrent.ConcurrentHashMap} — the original default behaviour.
 *
 * <p>This implementation reuses the existing {@link MemorySession} and
 * {@link SessionManager} expiration scheduler unchanged.
 */
public class MemorySessionRepository implements SessionRepository {

    private final SessionManager manager = SessionManager.getInstance();

    @Override
    public Session findById(String sessionId) {
        return manager.getSession(sessionId);
    }

    @Override
    public Session create(String sessionId) {
        Session session = new MemorySession(sessionId);
        manager.setSession(sessionId, session);
        return session;
    }

    @Override
    public void delete(String sessionId) {
        manager.removeSession(sessionId);
    }
}
