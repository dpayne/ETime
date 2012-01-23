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
 *
 *  Much of the code in RoundingRules class was originally written by David Keifer.
 *  Code is used with permission from D. Keifer
 */

import java.util.Calendar;

public class RoundingRules {

    public static Punch getRoundedPunch(Punch punch)
    {
        Calendar time = punch.getCalendar();
        int minutes = (int) (Math.round(time.get(Calendar.MINUTE)/15.0)*15);

        time.set(Calendar.MINUTE, minutes);
        punch.setCalendar(time);

        return punch;
    }

    public static Punch getRoundedFromLunchTime(Punch lunchStart, Punch lunchEnd) {
        Punch roundedLunchStart = getRoundedPunch(lunchStart);

        // The minute value after it was rounded to ADP recorded time.
        int roundedLunchMinutes = roundedLunchStart.getCalendar().get(Calendar.MINUTE);
        // The minute value before rounding
        int startMinutes = lunchStart.getCalendar().get(Calendar.MINUTE);

        // Rounded - Actual = Offset minutes
        int offset = roundedLunchMinutes - startMinutes; //(15m - 10m = 5m to add to clocked in time);

        // Get 
        int endMinutes = lunchEnd.getCalendar().get(Calendar.MINUTE);
        int endHours = lunchEnd.getCalendar().get(Calendar.HOUR);

        int finalMinutes = endMinutes + offset;

        if(finalMinutes >= 60)
        {
            finalMinutes -= 60;
            endHours += 1;
        }

        lunchEnd.getCalendar().set(Calendar.HOUR, endHours);
        lunchEnd.getCalendar().set(Calendar.MINUTE, finalMinutes);

        return lunchEnd;
    }
}
