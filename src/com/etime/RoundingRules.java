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

public class RoundingRules {

    public static Punch getRoundedPunch(Punch punch)
    {
        Calendar time = punch.getCalendar();
        int minutes = (int) (Math.round(time.get(Calendar.MINUTE)/15.0)*15);

        time.set(Calendar.MINUTE, minutes);
        punch.setCalendar(time);

        return punch;
    }

    public static Calendar getRoundedFromLunchTime(Calendar lunchStart, Calendar lunchEnd) {
//        Calendar roundedLunchStart = getRoundedPunch(lunchStart);
//
//        // The minute value after it was rounded to ADP recorded time.
//        int roundedLunchMinutes = roundedLunchStart.get(Calendar.MINUTE);
//        // The minute value before rounding
//        int startMinutes = lunchStart.get(Calendar.MINUTE);
//
//        // Rounded - Actual = Offset minutes
//        int offset = roundedLunchMinutes - startMinutes; //(15m - 10m = 5m to add to clocked in time);
//
//        // Get 
//        int endMinutes = lunchEnd.get(Calendar.MINUTE);
//        int endHours = lunchEnd.get(Calendar.HOUR);
//
//        int finalMinutes = endMinutes + offset;
//
//        if(finalMinutes >= 60)
//        {
//            finalMinutes -= 60;
//            endHours += 1;
//        }
//
//        lunchEnd.set(Calendar.HOUR, endHours);
//        lunchEnd.set(Calendar.MINUTE, finalMinutes);
//
//        return lunchEnd;
        return null;
    }
}
