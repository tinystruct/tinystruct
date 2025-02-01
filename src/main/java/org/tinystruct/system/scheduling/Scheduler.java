/*******************************************************************************
 * Copyright  (c) 2013, 2025 James M. ZHOU
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

/**
 * The Scheduler class provides a way to schedule tasks at specific times or intervals.
 */
public class Scheduler {

    private final static Scheduler INSTANCE = new Scheduler(true);
    private final Timer timer;

    /**
     * Constructs a Scheduler instance.
     * @param isDaemon Whether the Timer associated with this Scheduler is a daemon thread.
     */
    public Scheduler(boolean isDaemon) {
        this.timer = new Timer(isDaemon);
    }

    /**
     * Retrieves the singleton instance of the Scheduler.
     * @return The Scheduler instance.
     */
    public static Scheduler getInstance() {
        return INSTANCE;
    }

    /**
     * Cancels the Timer associated with this Scheduler.
     */
    public void cancel() {
        timer.cancel();
    }

    /**
     * Schedules a task to be executed at specific times based on the provided ScheduleIterator.
     * @param schedulerTask The task to be scheduled.
     * @param iterator The ScheduleIterator defining the times for task execution.
     */
    public void schedule(final SchedulerTask schedulerTask, final ScheduleIterator iterator) {
        synchronized (schedulerTask.lock) {
            final Scheduler scheduler = this;

            Date time = iterator.getTime();
            if (time == null) {
                schedulerTask.cancel();
            } else {
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

    /**
     * Schedules a task to be executed at fixed-rate intervals.
     * @param task The task to be scheduled.
     * @param iterator The ScheduleIterator defining the initial execution time.
     * @param period The interval between successive task executions, in milliseconds.
     */
    public void schedule(final TimerTask task, final ScheduleIterator iterator, long period) {
        this.timer.scheduleAtFixedRate(task, iterator.getTime(), period);
    }

    /**
     * Removes a previously scheduled task from execution.
     * @param task The task to be removed.
     */
    public void remove(final TimerTask task) {
        task.cancel();
    }
}