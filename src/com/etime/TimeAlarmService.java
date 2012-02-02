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
import android.webkit.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Calendar;
import java.util.List;

public class TimeAlarmService extends IntentService {
    static final String NAME = "com.etime.TimeAlarmService";
    private static volatile PowerManager.WakeLock lockStatic = null;
    private static volatile Context lockContext = null;

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    private String loginName;
    private String password;
    private static final String TAG = "TimeAlarmService-4321";
    private static final String TIMECARD_URL = "https://eet.adp.com/wfc/applications/mss/esstimecard.do";
    private static final String LOGIN_URL = "https://eet.adp.com/public/etime/html.html";
    private static final String LOGIN_URL_STEP2 = "https://eet.adp.com/wfc/SSOLogon/logonWithUID?IntendedURL=/wfc/applications/suitenav/navigation.do?ESS=true";
    private static final String LOGIN_URL_STEP3 = "https://eet.adp.com/wfc/applications/wtk/html/ess/timestamp.jsp";
    private static final String LOGIN_FAILED = "Logon attempt failed";
    private List<Punch> punches;
    private DefaultHttpClient httpClient;
    private static final String TIMESTAMP_RECORD_URL = "https://eet.adp.com/wfc/applications/wtk/html/ess/timestamp-record.jsp";
    private static final int APP_ID = 1;
    private NotificationManager nm;

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
            getLock().release();
        }
    }

    private void notification(String notifcationString) {
        CharSequence from = "ETime";
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(), 0);
        Notification notif = new Notification(R.drawable.icon,
                notifcationString, System.currentTimeMillis());

        notif.flags |= Notification.DEFAULT_LIGHTS;
        notif.defaults |= Notification.DEFAULT_VIBRATE;
        //notif.flags |= Notification.FLAG_ONGOING_EVENT;

        notif.setLatestEventInfo(this, from, notifcationString, contentIntent);
        nm.notify("ETime", APP_ID, notif);
    }

    private void notifyAutoClockOutSuccess() {
        notification("Auto clock out successful!!!");
    }

    private void notifyAutoClockOutFailure() {
        notification("Auto clock out failed!!!");
    }

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

    private boolean withinFifteenMinutes(Punch punch) {
        int deltaHr = Math.abs(Calendar.getInstance().get(Calendar.HOUR_OF_DAY) - punch.getCalendar().get(Calendar.HOUR_OF_DAY));
        int deltaMin = Math.abs(Calendar.getInstance().get(Calendar.MINUTE) - punch.getCalendar().get(Calendar.MINUTE));

        double delta = ((double) deltaHr) + (((double) deltaMin) / 60.0);
        Log.v(TAG, "Delta is " + deltaHr + ":" + deltaMin + " delta total is " + delta);

        return (delta <= 0.25);
    }


    private boolean parseTimeCard() {
        String page = ETimeUtils.getHtmlPage(httpClient, TIMECARD_URL);
        punches = ETimeUtils.getTodaysPunches(page);
        return false;
    }

    private boolean login() {
        CookieManager cookieManager = getSyncedCookieManager();
        httpClient = new DefaultHttpClient();
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, -1),
                new UsernamePasswordCredentials(loginName, password));

        if (!signon(httpClient))
            return false;

        List<Cookie> cookies = httpClient.getCookieStore().getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String cookieString = cookie.getName() + "="
                        + cookie.getValue() + "; domain=" + cookie.getDomain();
                cookieManager.setCookie(cookie.getDomain(),
                        cookieString);
            }
            CookieSyncManager.getInstance().sync();
        }
        return true;
    }

    private CookieManager getSyncedCookieManager() {
        CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.removeSessionCookie();
        cookieSyncManager.sync();
        return cookieManager;
    }

    public boolean signon(DefaultHttpClient httpClient) {
        String page;

        page = ETimeUtils.getHtmlPage(httpClient, LOGIN_URL);
        if (page == null || page.contains(LOGIN_FAILED)) {
            return false;
        }

        page = ETimeUtils.getHtmlPage(httpClient, LOGIN_URL_STEP2);
        if (page == null || page.contains(LOGIN_FAILED)) {
            return false;
        }

        page = ETimeUtils.getHtmlPage(httpClient, LOGIN_URL_STEP3);
        if (page == null || page.contains(LOGIN_FAILED)) {
            return false;
        }

        return true;
    }

    public static void setLockContext(Context context) {
        lockContext = context;
    }

    synchronized static PowerManager.WakeLock getLock() {
        if (lockStatic == null && lockContext != null) {
            PowerManager mgr = (PowerManager) lockContext.getSystemService(Context.POWER_SERVICE);

            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    NAME);
            lockStatic.setReferenceCounted(true);
        }

        return (lockStatic);
    }
}
