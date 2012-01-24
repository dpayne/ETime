package com.etime;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Calendar;
import java.util.List;

/**
 * User: dpayne2
 * Date: 1/23/12
 * Time: 4:02 PM
 */
public class ETimeAlarmService extends WakefulIntentService {
    private static final int APP_ID = 1;

    private String loginName;
    private String password;
    private static final String TAG = "Alarm-4321";
    private static final String TIMECARD_URL = "https://eet.adp.com/wfc/applications/mss/esstimecard.do";
    private static final String LOGIN_URL = "https://eet.adp.com/public/etime/html.html";
    private static final String LOGIN_URL_STEP2 = "https://eet.adp.com/wfc/SSOLogon/logonWithUID?IntendedURL=/wfc/applications/suitenav/navigation.do?ESS=true";
    private static final String LOGIN_URL_STEP3 = "https://eet.adp.com/wfc/applications/wtk/html/ess/timestamp.jsp";
    private static final String LOGIN_FAILED = "Logon attempt failed";
    private List<Punch> punches;
    private DefaultHttpClient httpClient;
    private static final String TIMESTAMP_RECORD_URL = "https://eet.adp.com/wfc/applications/wtk/html/ess/timestamp-record.jsp";


    public ETimeAlarmService() {
        super("ETimeAlarmService");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        boolean retval;

        loginName = intent.getStringExtra("username");
        password = intent.getStringExtra("password");

        retval = login();

        if (!retval) {
            notifyAutoClockOutFailure(this);
        }

        parseTimeCard();

        retval = clockOut();


        if (!retval) {
            notifyAutoClockOutFailure(this);
        } else {
            notifyAutoClockOutSuccess(this);
        }
    }

    private void notification(Context context, String notifcationString) {
        NotificationManager nm;
        nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        CharSequence from = "ETime";
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(), 0);
        Notification notif = new Notification(R.drawable.icon,
                notifcationString, System.currentTimeMillis());

        notif.flags |= Notification.DEFAULT_LIGHTS;
        notif.flags |= Notification.DEFAULT_VIBRATE;

        notif.setLatestEventInfo(context, from, notifcationString, contentIntent);
        nm.notify("ETime", APP_ID, notif);
    }

    private void notifyAutoClockOutSuccess(Context context) {
        notification(context, "Auto clock out successful!!!");
    }

    private void notifyAutoClockOutFailure(Context context) {
        notification(context, "Auto clock out failed!!!");
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

}
