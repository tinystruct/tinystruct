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

import java.util.TimerTask;

public class SchedulerTimerTask extends TimerTask {
    private SchedulerTask schedulerTask;
    private ScheduleIterator iterator;
    private Scheduler scheduler;

    public SchedulerTimerTask(Scheduler scheduler,
                              SchedulerTask schedulerTask, ScheduleIterator iterator) {
        this.scheduler = scheduler;
        this.schedulerTask = schedulerTask;
        this.iterator = iterator;
    }

    public void run() {
        this.schedulerTask.start();

        if (this.schedulerTask.next())
            this.scheduler.schedule(this.schedulerTask, this.iterator);
    }
}
