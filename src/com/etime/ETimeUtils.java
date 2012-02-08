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

import android.util.Log;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: dpayne2
 * Date: 1/5/12
 * Time: 3:16 PM
 */
class ETimeUtils {
    private static final String TAG = "ETimeUtils-4321";
    private static final String TOTAL_STR = "Total:&nbsp;";
    private static final String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    /**
     * Return a string of the raw html web page at 'url'.
     *
     * @param client The httpclient to be used to get the webpage
     * @param url    The url the webpage is associated with.
     * @return A String of the raw html web page
     */
    protected static String getHtmlPage(DefaultHttpClient client, String url) {
        return getHtmlPageWithProgress(client, url, null, 0, 0, 0);
    }

    protected static String getHtmlPageWithProgress(DefaultHttpClient client, String url, ETimeAsyncTask asyncTask,
                                                    int startProgress, int maxProgress, int estimatedPageSize) {
        HttpResponse response;
        HttpGet httpGet;
        BufferedReader in = null;
        String page = null;
        int runningSize = 0;
        int progress = startProgress;
        int tempProgress;
        String redirect = null;

        try {
            httpGet = new HttpGet(url);

            response = client.execute(httpGet);

            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuilder sb = new StringBuilder("");
            String line;
            String NL = System.getProperty("line.separator");

            Header [] headers = response.getAllHeaders();
            for (Header header : headers) {
                Log.v(TAG, "Header  " + header.getName() + ":" + header.getValue());
                if (header.getName().equals("Content-Length")) {
                    try {
                        estimatedPageSize = Integer.parseInt(header.getValue());
                    } catch (Exception e) {
                        Log.w(TAG, e.toString());
                    }
                } else if (header.getName().equals("Location")) {
                    redirect = header.getValue();
                }
                if (asyncTask != null) {
                    progress += 5/(maxProgress-startProgress);
                    asyncTask.publishToProgressBar(progress);
                }
            }
            while ((line = in.readLine()) != null) {
                if (asyncTask != null) {
                    runningSize += line.length();
                    tempProgress = startProgress + (int) (((double) runningSize / ((double) estimatedPageSize)) * (maxProgress - startProgress));
                    progress = (progress >= tempProgress ? progress : tempProgress);
                    if (progress > maxProgress) { //happens when estimatedPageSize <= runningSize
                        progress = maxProgress;
                    }

                    asyncTask.publishToProgressBar(progress);
                }
                sb.append(line).append(NL);
            }
            page = sb.toString();
            Log.v(TAG, "Page size for " + url + "  is: " + page.length());

        } catch (Exception e) {
            Log.v(TAG, e.toString());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (redirect != null) {
            return redirect;
        }

        return page;
    }

    /**
     * Get the total hrs logged this pay period.
     *
     * @param page the raw html of the user's timecard page
     * @return A double representing the total hours logged this pay period by the user.
     */
    protected static double getTotalsHrs(String page) {
        double total = 0;

        Pattern pattern = Pattern.compile("(?i)(<div.*?>)(" + TOTAL_STR + ")(.*?)(</div>)");
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            String totalStr = matcher.group(3);
            if (!totalStr.isEmpty()) {
                total = Double.parseDouble(totalStr);
            }
        }

        return total;
    }

    /**
     * Return a List of Punches for the current day. The list is empty if there are no punches for today.
     *
     * @param page the raw html of the user's timecard page
     * @return A list of Punches for the current day.
     */
    protected static List<Punch> getTodaysPunches(String page) {
        String curRow;
        String date;
        List<Punch> punchesList = new LinkedList<Punch>();

        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String dayOfWeek = daysOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1];

        if (day < 10) {
            date = dayOfWeek + " " + Integer.toString(month) + "/0" + Integer.toString(day);
        } else {
            date = dayOfWeek + " " + Integer.toString(month) + "/" + Integer.toString(day);
        }

        Pattern todaysRowsPattern = Pattern.compile("(?i)(>" + date + ")(.*?)(</tr>)", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher todaysRowsMatcher = todaysRowsPattern.matcher(page);
        while (todaysRowsMatcher.find()) {
            curRow = todaysRowsMatcher.group(2);
            addPunchesFromRowToList(curRow, punchesList);
        }

        return punchesList;
    }

    /**
     * Adds all Punches in a given string to a list of punches.
     *
     * @param row     The string to be searched for punches.
     * @param punches A list of punches to be added to.
     */
    private static void addPunchesFromRowToList(String row, List<Punch> punches) {
        //Format to be matched is
        //  <td title="" class="InPunch"><div class="" title=""> 2:45PM </div></td>
        Pattern punchPattern = Pattern.compile("(?i)((InPunch|OutPunch)\">)(.*?)(>\\s*)(.*?)(\\s*</div>)",
                Pattern.MULTILINE | Pattern.DOTALL);
        Matcher punchMatcher = punchPattern.matcher(row);
        Punch punch;

        while (punchMatcher.find()) {
            String punchStr = punchMatcher.group(5);

            if (!punchStr.equals("&nbsp;")) {
                punch = getPunchFromString(punchStr);//parse a Punch from a string (e.g. "12:00PM")
                if (punchMatcher.group(2).equals("InPunch")) {
                    punch.setClockIn(true);
                } else {
                    punch.setClockIn(false);
                }
                punches.add(punch);
            }
        }
    }

    /**
     * Parse a Punch from a given string. The format is assumed to be "12:00AM". All other calendar fields are set
     * to the current days value. The day, month, year, timezone and other misc fields are set to the current days value.
     *
     * @param punchStr The String to be parsed for the punch
     * @return the parsed Punch
     */
    private static Punch getPunchFromString(String punchStr) {
        Punch punch = new Punch();
        Calendar calendar;
        Pattern punchPattern = Pattern.compile("(\\d+):(\\d+)(A|P)M");//Format is "12:00PM"
        Matcher punchMatcher = punchPattern.matcher(punchStr);

        if (!punchMatcher.find()) {
            return null;
        }

        int hour = Integer.parseInt(punchMatcher.group(1));
        int min = Integer.parseInt(punchMatcher.group(2));

        calendar = Calendar.getInstance();
        int hour24;
        if (punchMatcher.group(3).equals("A")) {
            calendar.set(Calendar.AM_PM, Calendar.AM);
            calendar.set(Calendar.HOUR, hour);
            if (hour == 12) {
                hour24 = 0;
            } else {
                hour24 = hour;
            }
        } else {
            calendar.set(Calendar.AM_PM, Calendar.PM);
            calendar.set(Calendar.HOUR, hour);

            if (hour != 12) {
                hour24 = hour + 12;
            } else {
                hour24 = hour;
            }
        }

        calendar.set(Calendar.HOUR_OF_DAY, hour24);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, 0);
        punch.setCalendar(calendar);
        return punch;
    }

    /**
     * Get the total hours logged today from a list pf punches. This method does not account for lunch rounding.
     *
     * @param punches A list of punches from a given user for today.
     * @return A double of the total hours logged today.
     */
    protected static double todaysTotalHrsLogged(List<Punch> punches) {
        long runningMilliSecTotal = 0;
        Punch curInPunch;
        Punch curOutPunch;
        Iterator<Punch> iterPunches = punches.iterator();

        while (true) {
            //get next clock in
            if (!iterPunches.hasNext())
                break;

            curInPunch = iterPunches.next();

            //get next clock out
            if (!iterPunches.hasNext()) {
                curOutPunch = new Punch();
                curOutPunch.setCalendar(Calendar.getInstance());
                curOutPunch.setClockIn(false);
                break;
            } else {
                curOutPunch = iterPunches.next();
            }

            runningMilliSecTotal += curOutPunch.getCalendar().getTimeInMillis() - curInPunch.getCalendar().getTimeInMillis();
        }

        int hrs = (int) (((runningMilliSecTotal / 1000) / 60) / 60) % 24;
        int minutes = (int) (Math.round((((runningMilliSecTotal / 1000) / 60) % 60) / 15.0) * 15);
        double mins = (minutes) / 60.0;

        return ((double) hrs) + mins;
    }

    /**
     * Get the calculated eight hour punch. The eight hour punch is identical to the punch that is need for the user to
     * log exactly 8 hours for today.
     *
     * @param punches A list of punches logged today by a given user.
     * @return The calculated eight hour punch.
     */
    protected static Punch getEightHrPunch(List<Punch> punches) {
        if (punches == null || punches.isEmpty())
            return null;

        Punch eightHrPunch = new Punch();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(clockOutAt(punches));
        eightHrPunch.setCalendar(calendar);

        return eightHrPunch;
    }

    /**
     * return a long of the milliseconds since epoch the user should clock out at to log exactly 8 hours.
     *
     * @param punches A list of punches logged today by a given user.
     * @return A long of the milliseconds since epoch the user should clock out at to log exactly 8 hours.
     */
    protected static long clockOutAt(List<Punch> punches) {
        Iterator<Punch> iterPunches = punches.iterator();
        long clockOutTime = 0;
        int index = 1;
        while (iterPunches.hasNext()) {
            long punch = iterPunches.next().getCalendar().getTimeInMillis();
            if (index == 1)
                punch = RoundingRules.getRoundedTime(punch);
            else if (index == 2 || index % 2 == 0)
                punch *= -1;

            clockOutTime += punch;
            index++;
        }
        clockOutTime += 8 * 1000 * 60 * 60;
        clockOutTime = RoundingRules.getRoundedTime(clockOutTime);

        return clockOutTime;
    }
}
