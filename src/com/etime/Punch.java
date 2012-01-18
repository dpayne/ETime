package com.etime;

/*
 *  This file is part of ETime.
 *
 *  ETime is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ETime is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with ETime.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Calendar;

/**
 * User: dpayne2
 * Date: 1/6/12
 * Time: 2:00 PM
 */
class Punch {
    private Calendar calendar;
    private boolean isClockIn = false; // true if this punch is a clock in

    public Calendar getCalendar() {
        return calendar;
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
    }

    public boolean isClockIn() {
        return isClockIn;
    }

    public void setClockIn(boolean clockIn) {
        isClockIn = clockIn;
    }

    public String toString() {
        return "Clock " + (isClockIn ? "In" : "Out") + " " + calendar.get(Calendar.HOUR_OF_DAY)
                + ":" + calendar.get(Calendar.MINUTE) + " "
                + ((calendar.get(Calendar.AM_PM) == Calendar.AM) ? "AM" : "PM");
    }
}
