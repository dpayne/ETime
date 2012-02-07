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

    /**
     * Round a round a long of a punch in milliseconds to since epoch the the nearest 15 mins.
     * @param punchTime
     * @return
     */
    public static long getRoundedTime(long punchTime) {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(punchTime);
        int minutes = (int) (Math.round(time.get(Calendar.MINUTE) / 15.0) * 15);

        time.set(Calendar.MINUTE, minutes);

        return time.getTimeInMillis();
    }
}
