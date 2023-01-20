/*******************************************************************************
 * Copyright  (c) 2013, 2023 James Mover Zhou
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

import java.text.SimpleDateFormat;
import java.util.Date;

public class AlarmClock {

    private final Scheduler scheduler = new Scheduler(false);
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy HH:mm:ss.SSS");

    public void start() {
        TimeIterator iterator = new TimeIterator(0, 0, 0);
        iterator.setInterval(7);

        this.scheduler.schedule(new SchedulerTask() {
            private final Object lock = new Object();
            private boolean status = false;

            public void start() {
                synchronized (lock) {
                    System.out.println("\r\nStart: " + dateFormat.format(new Date()));

                    System.out.println("you need spend 20 seconds to finish the task.");

                    this.status = true;
                    System.out.println("End: " + dateFormat.format(new Date()));
                }

                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("Waited 20 seconds!");
                }
            }

            @Override
            public boolean next() {
                synchronized (lock) {
                    return this.status;
                }
            }

            @Override
            public void cancel() {
                scheduler.cancel();
            }

        }, iterator);
    }

    public static void main(String[] args) {
        AlarmClock alarmClock = new AlarmClock();
        alarmClock.start();
    }
}

