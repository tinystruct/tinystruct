/*******************************************************************************
 * Copyright  (c) 2013, 2017 James Mover Zhou
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
package org.tinystruct.application;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public final class Mutex {
    private final static AtomicInteger resources = new AtomicInteger(0);

    private Mutex() {
    }

    public static int get() {
        return resources.decrementAndGet();
    }

    public static int put() {
        return resources.addAndGet(1);
    }

    private final Synchronizer synchronizer = new Synchronizer();

    public void lock() {
        synchronizer.acquire(1);
    }

    public void unlock() {
        synchronizer.release(1);
    }

    static class Synchronizer extends AbstractQueuedSynchronizer {
        public Synchronizer() {
            super.setState(1);
        }

        @Override
        protected boolean tryAcquire(int arg) {
            return super.compareAndSetState(1, 0);
        }

        @Override
        protected boolean tryRelease(int arg) {
            super.setState(1);
            return true;
        }
    }

}
