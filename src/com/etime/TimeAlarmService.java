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

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Calendar;
import java.util.List;

public class TimeAlarmService extends IntentService {
    static final String NAME = "com.etime.TimeAlarmService";
    private static final String TAG = "TimeAlarmService-4321";

    private static volatile PowerManager.WakeLock lockStatic = null;
    private static volatile Context lockContext = null;

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    private String loginName;
    private String password;
    private String TIMECARD_URL;
    private String LOGIN_URL;
    private String LOGIN_URL_STEP2;
    private String TIMESTAMP_RECORD_URL;
    private String LOGIN_FAILED;
    private List<Punch> punches;
    private DefaultHttpClient httpClient;
    private static final int APP_ID = 1;
    private NotificationManager nm;
    private int runs = 0;

    public TimeAlarmService() {
        super("TimeAlarmService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            nm = (NotificationManager) this
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            boolean retval;

            loginName = intent.getStringExtra(USERNAME);
            password = intent.getStringExtra(PASSWORD);

            LOGIN_URL = lockContext.getString(R.string.login_url);
            LOGIN_URL_STEP2 = lockContext.getString(R.string.login_url2);
            TIMECARD_URL = lockContext.getString(R.string.timecard_url);
            TIMESTAMP_RECORD_URL = lockContext.getString(R.string.timestamp_record_url);
            LOGIN_FAILED = lockContext.getString(R.string.login_failed_str);

            retval = login();

            if (!retval) {
                notifyAutoClockOutFailure();
            }

            parseTimeCard();

            retval = clockOut();

            if (!retval) {
                notifyAutoClockOutFailure();
            } else {
                notifyAutoClockOutSuccess();
            }
        } finally {
            TimeAlarmService.getLock().release();
        }
    }

    /**
     * Send a notification to the user with the message 'notificationString'
     * @param notifcationString    The message to notify the user with.
     */
    private void notification(String notifcationString) {
        CharSequence from = "ETime";
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(), 0);
        Notification notif = new Notification(R.drawable.icon,
                notifcationString, System.currentTimeMillis());

        notif.flags |= Notification.DEFAULT_LIGHTS;
        notif.defaults |= Notification.DEFAULT_VIBRATE;

        notif.setLatestEventInfo(this, from, notifcationString, contentIntent);
        nm.notify("ETime", APP_ID, notif);
    }

    /**
     * Notify user of success.
     */
    private void notifyAutoClockOutSuccess() {
        notification("Auto clock out successful!!!");
    }

    /**
     * Notify user of failure.
     */
    private void notifyAutoClockOutFailure() {
        notification("Auto clock out failed!!!");
    }

    /**
     * Auto clockout.
     * @return return true if autoclock out was successful, false otherwise.
     */
    private boolean clockOut() {

        if (punches == null || punches.isEmpty()) {
            return false;
        }

        Punch lastPunch = punches.get(punches.size() - 1);

        if (lastPunch == null || !lastPunch.isClockIn()) {
            return false;
        }

        Punch eightHrPunch = ETimeUtils.getEightHrPunch(punches);

        if (eightHrPunch == null) {
            return false;
        }

        if (!withinFifteenMinutes(eightHrPunch)) {
            return false;
        }

        ETimeUtils.getHtmlPage(httpClient, TIMESTAMP_RECORD_URL);

        return true;
    }

    /**
     * Returns true if the eight hour clock is within 15 minutes of the current time. Used to check if auto clock out
     * should happen now.
     * @param punch The calculated eight hour punch to be checked.
     * @return true if the current time is within 15 mins, false otherwise.
     */
    private boolean withinFifteenMinutes(Punch punch) {
        int deltaHr = Math.abs(Calendar.getInstance().get(Calendar.HOUR_OF_DAY) - punch.getCalendar().get(Calendar.HOUR_OF_DAY));
        int deltaMin = Math.abs(Calendar.getInstance().get(Calendar.MINUTE) - punch.getCalendar().get(Calendar.MINUTE));

        double delta = ((double) deltaHr) + (((double) deltaMin) / 60.0);
        Log.v(TAG, "Delta is " + deltaHr + ":" + deltaMin + " delta total is " + delta);

        return (delta <= 0.25);
    }

    /**
     * Parse the current users time card and set punches to the total list of punches.
     */
    private void parseTimeCard() {
        String page = ETimeUtils.getHtmlPage(httpClient, TIMECARD_URL);
        punches = ETimeUtils.getTodaysPunches(page);
    }

    /**
     * Login to the ADP site
     * @return  true if login is successful, false otherwise
     */
    private boolean login() {
        httpClient = new DefaultHttpClient();
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, -1),
                new UsernamePasswordCredentials(loginName, password));

        return signon(httpClient);
    }

    /**
     * Sign on to the ADP eTime site. Return true if signon was successful, false otherwise. Sign on is done through
     * hitting a series of ADP pages with basic authentication set. Sets the progress bar on the main page.
     * @param httpClient the http client to be used to sign on.
     * @return  true if signon was successful, false otherwise.
     */
    public boolean signon(DefaultHttpClient httpClient) {
        String page;

        page = ETimeUtils.getHtmlPage(httpClient, LOGIN_URL);
        if (page == null || page.contains(LOGIN_FAILED)) {
            return false;
        }

        page = ETimeUtils.getHtmlPage(httpClient, LOGIN_URL_STEP2);

        return !(page == null || page.contains(LOGIN_FAILED));
    }

    /**
     * Acquires the wake lock to keep the phone awake while the auto clocks out the user.
     * @return the wake lock.
     */
    synchronized static PowerManager.WakeLock getLock() {
        if (lockStatic == null && lockContext != null) {
            PowerManager mgr = (PowerManager) lockContext.getSystemService(Context.POWER_SERVICE);

            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    NAME);
            lockStatic.setReferenceCounted(true);
        }

        return (lockStatic);
    }

    protected synchronized static void setLockContext(Context context) {
        TimeAlarmService.lockContext = context;
    }
}
