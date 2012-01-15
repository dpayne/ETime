package etime;

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
