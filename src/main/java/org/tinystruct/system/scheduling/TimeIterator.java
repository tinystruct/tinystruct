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

import java.util.Calendar;
import java.util.Date;

/**
 * A DailyIterator class returns a sequence of dates on subsequent days
 * representing the same time each day.
 */
public class TimeIterator implements ScheduleIterator {

    private final Calendar calendar = Calendar.getInstance();
    private int seconds;

    public TimeIterator(int hour, int minute, int second) {
        this(hour, minute, second, new Date());
    }

    public TimeIterator(int hour, int minute, int second, Date date) {
        this.calendar.setTime(date);

        this.calendar.set(Calendar.HOUR_OF_DAY, hour);
        this.calendar.set(Calendar.MINUTE, minute);
        this.calendar.set(Calendar.SECOND, second);
        this.calendar.set(Calendar.MILLISECOND, 0);
    }

    public ScheduleIterator next() {
        if (this.seconds == 86400) {
            this.calendar.add(Calendar.DATE, 1);
        } else {
            this.calendar.setTime(new Date());
            this.calendar.add(Calendar.SECOND, this.seconds);
        }

        return this;
    }

    public void setInterval(int seconds) {
        this.seconds = seconds;
    }

    public Date getTime() {
        return this.calendar.getTime();
    }

}