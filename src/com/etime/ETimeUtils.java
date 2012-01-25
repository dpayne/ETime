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
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

/**
 * User: dpayne2
 * Date: 1/5/12
 * Time: 3:16 PM
 */
class ETimeUtils {
    private static final String TAG = "ETimeUtils-4321";

    private static final String DIV_CLOSE = "</div>";
    private static final String TABLE_ROW_TAG = "<tr class";
    private static final String PUNCH_TAG = "Punch\">";
    private static final String PUNCH_IN = "InPunch";
    private static final String PUNCH_END = "<div class=\"\" title=\"\">";
    private static final String TOTAL_STR = "Total:";
    private static final String HTML_SPACE = "&nbsp;";
    private static final String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final String TABLE_ROW_END_TAG = "</tr>";

    protected static String getHtmlPage(DefaultHttpClient client, String url) {
        HttpResponse response;
        HttpGet httpGet;
        BufferedReader in = null;
        String page = null;

        try {
            httpGet = new HttpGet(url);

            response = client.execute(httpGet);

            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuilder sb = new StringBuilder("");
            String line;
            String NL = System.getProperty("line.separator");

            while ((line = in.readLine()) != null) {
                sb.append(line).append(NL);
            }

            in.close();
            page = sb.toString();

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

        return page;
    }

    protected static WebView setupWebView(WebView webview, WebViewClient webViewClient, WebChromeClient webChromeClient) {

        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webview.getSettings().setAllowFileAccess(true);
        webview.getSettings().setBuiltInZoomControls(true);
        webview.getSettings().setSupportZoom(true);
        webview.canGoBack();

        webview.setWebViewClient(webViewClient);
        webview.setWebChromeClient(webChromeClient);

        return webview;
    }


    protected static double getTotalsHrs(String page) {
        double total = 0;

        try {
            int startOfTotal = page.indexOf(TOTAL_STR);

            int endOfTotal = page.indexOf(DIV_CLOSE, startOfTotal);
            String withHtmlSpaces = page.substring(startOfTotal + TOTAL_STR.length(), endOfTotal);
            String withoutHtmlSpaces = withHtmlSpaces.replaceAll(HTML_SPACE, "");

            total = Double.parseDouble(withoutHtmlSpaces);
        } catch (Exception e) {
            Log.w(TAG, e.toString());
        }

        return total;
    }

    private static int findNextTodaysRow(String page, int startIndex) {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String dayOfWeek = daysOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1];

        String date;
        if (day < 10) {
            date = ">" + dayOfWeek + " " + Integer.toString(month) + "/0" + Integer.toString(day);
        } else {
            date = ">" + dayOfWeek + " " + Integer.toString(month) + "/" + Integer.toString(day);
        }

        int indexOfDate = page.indexOf(date, startIndex);

        if (indexOfDate < 0) {
            return -1;
        }

        return page.lastIndexOf(TABLE_ROW_TAG, indexOfDate);
    }

    protected static List<Punch> getTodaysPunches(String page) {

        int indexOfCurRow;
        int indexOfEndOfCurRow;
        String curRow;
        int index = 0;
        List<Punch> punchesList = new LinkedList<Punch>();
        try {
            do {

                indexOfCurRow = findNextTodaysRow(page, index);
                if (indexOfCurRow < 0)
                    break;

                indexOfEndOfCurRow = page.indexOf(TABLE_ROW_END_TAG, indexOfCurRow);
                if (indexOfEndOfCurRow < 0)
                    break;

                curRow = page.substring(indexOfCurRow, indexOfEndOfCurRow);
                punchesList.addAll(getPunchesFromRow(curRow));

                index = indexOfEndOfCurRow + TABLE_ROW_END_TAG.length();

            } while (indexOfCurRow >= 0);

        } catch (Exception e) {
            Log.w(TAG, e);
        }

        return punchesList;

    }

    private static List<Punch> getPunchesFromRow(String row) {
        List<Punch> punchesList = new LinkedList<Punch>();

        Punch punch;
        int index = 0;
        do {
            row = row.substring(index);
            punch = new Punch();
            index = getNextPunch(row, index, punch);
            if (index >= 0) {
                punchesList.add(punch);
            }
        } while (index > 0);
        return punchesList;
    }

    private static int getNextPunch(String page, int startIndex, Punch punch) {
        try {
            int indexOfPunch = page.indexOf(PUNCH_TAG, startIndex);
            int indexOfStartOfDiv = page.indexOf(PUNCH_END, indexOfPunch + PUNCH_TAG.length() + 1);
            int endOfPunch = page.indexOf(DIV_CLOSE, indexOfStartOfDiv);

            if (page.substring(0, indexOfStartOfDiv).contains(PUNCH_IN)) {
                punch.setClockIn(true);
            } else {
                punch.setClockIn(false);
            }

            String strDate = page.substring(indexOfStartOfDiv + PUNCH_END.length(), endOfPunch);
            strDate = strDate.trim();

            int hour = Integer.parseInt(strDate.substring(0, strDate.indexOf(':')));
            boolean am;
            int min;

            if (strDate.contains("P")) {
                am = false;
                min = Integer.parseInt(strDate.substring(strDate.indexOf(':') + 1, strDate.indexOf('P')));
            } else if (strDate.contains("A")) {
                am = true;
                min = Integer.parseInt(strDate.substring(strDate.indexOf(':') + 1, strDate.indexOf('A')));
            } else {
                return -1;
            }

            Calendar calendar = Calendar.getInstance();
            int hour24;
            if (am) {
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

            return endOfPunch + PUNCH_END.length();
        } catch (Exception e) {
            Log.w(TAG, e.toString());
            return -1;
        }
    }

    protected static Punch getLastClockIn(List<Punch> punches) {
        Punch lastClockIn = null;
        for (Punch punch : punches) {
            if (punch.isClockIn()) {
                lastClockIn = punch;
            }
        }
        return lastClockIn;
    }

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
        double hrs = (((runningMilliSecTotal / 1000) / 60) / 60) % 24;
        double mins = (((runningMilliSecTotal / 1000) / 60) % 60) / 100.0;

        return hrs + mins;
    }


    protected static Punch getEightHrPunch(List<Punch> punches) {
        if (punches == null || punches.isEmpty())
            return null;
        Punch lastPunch = punches.get(punches.size() - 1);
        Punch eightHrPunch = new Punch();
        Punch lastClockIn = getLastClockIn(punches);
        Calendar calendar;

        if (lastClockIn == null || (lastPunch != null && !lastPunch.isClockIn())) {
            calendar = Calendar.getInstance();
        } else {
            calendar = lastClockIn.getCalendar();
        }

        eightHrPunch.setClockIn(false);

        double totalHrsLoggedToday = todaysTotalHrsLogged(punches);

        if (totalHrsLoggedToday >= 8.0) {
            eightHrPunch.setCalendar(Calendar.getInstance());
            return eightHrPunch;
        }
        double timeLeft = 8.0 - totalHrsLoggedToday;

        int hrs = (int) timeLeft;
        int mins = (int) ((timeLeft - Math.floor(timeLeft)) * 60);

        int curHr = calendar.get(Calendar.HOUR_OF_DAY);
        int curMin = calendar.get(Calendar.MINUTE);

        hrs += ((mins + curMin) / 60 + curHr) % 24;
        mins = (mins + curMin) % 60;

        calendar.set(Calendar.HOUR_OF_DAY, hrs);
        calendar.set(Calendar.MINUTE, mins);

        eightHrPunch.setCalendar(calendar);

        return eightHrPunch;
    }

    /**
     * Round the list of punches for today. Does some weird rounding rules.
     * Rounds the first time stamp to the nearest 15 mins. Round any
     * pair of clock out then clock in's to the neareset total 15 mins.
     *
     * Example:
     *  Clock out for lunch at 12:04, Clock back in at 12:27. A total
     *  of 23 mins spent on lunch. The lunch time is then rounded to the
     *  nearest 15 mins. In this case 23 is rounded to 30 mins.
     *
     * @param punches List of punches for today
     */
    protected static void roundPunches(List<Punch> punches) {
        Punch lastPunch = null;
        int index = 1;
        for (Punch punch : punches) {
            if (index == 1) {
                punch.setCalendar((RoundingRules.getRoundedPunch(punch)).getCalendar());
            } else {
                if (!punch.isClockIn()) {
                    lastPunch.setCalendar((RoundingRules.getRoundedPunch(lastPunch)).getCalendar());
                    punch.setCalendar((RoundingRules.getRoundedFromLunchTime(lastPunch, punch)).getCalendar());
                }
            }
            lastPunch = punch;
            index++;
        }
    }
}
