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
package org.tinystruct.system.scheduling;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Scheduler {

    private final Timer timer;

    public Scheduler(boolean isDaemon) {
        this.timer = new Timer(isDaemon);
    }

    public void cancel() {
        timer.cancel();
    }

    public void schedule(final SchedulerTask schedulerTask, final ScheduleIterator iterator) {

        synchronized (schedulerTask.lock) {
            final Scheduler scheduler = this;

            Date time = iterator.getTime();
            if (time == null) {
                schedulerTask.cancel();
            } else
                timer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        schedulerTask.start();

                        if (schedulerTask.next()) {
                            scheduler.schedule(schedulerTask, iterator.next());
                        }
                    }

                }, time);
        }
    }


}
