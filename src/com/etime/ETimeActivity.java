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

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.*;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Calendar;
import java.util.List;

public class ETimeActivity extends Activity {
    private AlarmManager am;
    private PendingIntent pendingIntentAutoClockAlarm;

    private String PREFS_USERNAME = "username";
    private String PREFS_PASSWORD = "password";

    private WebView webview;
    private DefaultHttpClient httpClient;
    final Activity activity = this;

    private String loginName = null;
    private String password = null;

    private String TAG = "ETime-4321";
    private String PREF_LUNCH = "lunch";

    private String TIMESTAMP_RECORD_URL;
    private String TIMECARD_URL;

    private String TIMESTAMP_URL;
    private String TIMESTAMP_SUCCESS;
    private String LOGIN_FAILED_URL;

    private long loginTime;

    final private long DEF_TIMEOUT = 900000; // 15 mins in milliseconds

    private ProgressBar progressBar;
    private Button recordTime;

    private List<Punch> punches;
    private double totalHrs;
    private String oldLoginNameBeforePreferencePage;
    private String LOGIN_FAILED_URL_2;

    private Button curStatus;
    private Button textViewTotalHrs;
    private TextView loading;
    private Button timeToClockOut;

    private Punch lastPunch;
    private boolean AUTO_CLOCKOUT;

    private NotificationManager mManager;
    private static final int APP_ID = 1;

    private boolean notCreated = true;
    private boolean autoClockOutIfOkTimeCard = false;
    private boolean oldAutoClockBeforePreferencePage;
    private CookieManager cookieManager;
    private Button totalHrsLoggedToday;
    private String lastNotificationMessage;

    /*
    * todo: auto clock in/out for lunch
    * get rid of webview for ETimeActivity, move clock in to httpClient
    * round lunch time properly, right now it's rounding every timestamp
    */

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (validConfig()) {
            setupTitlePage();
        } else { /* show config page, to set username password */
            Toast.makeText(getApplicationContext(), "Username/Password required", Toast.LENGTH_LONG).show();
            startPreferencesPage();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mManager != null)
            mManager.cancel(APP_ID);
    }

    private void notify(String message) {
        if (message.equalsIgnoreCase(lastNotificationMessage)) {
            return;
        } else {
            lastNotificationMessage = message;
        }

        int icon = R.drawable.icon;
        long when = System.currentTimeMillis();
        Context context = getApplicationContext();
        CharSequence contentTitle = "ETime";
        Intent notificationIntent = new Intent(this, ETimeActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, message, when);
        notification.flags |= Notification.DEFAULT_LIGHTS;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.setLatestEventInfo(context, contentTitle, message, contentIntent);
        mManager.notify("ETime", APP_ID, notification);
    }

    protected void startPreferencesPage() {
        oldLoginNameBeforePreferencePage = loginName;
        oldAutoClockBeforePreferencePage = AUTO_CLOCKOUT;
        startActivity(new Intent(activity, ETimePreferences.class));
    }

    public void setPunches(List<Punch> punches) {
        this.punches = punches;
    }

    public void setTotalHrs(double totalHrs) {
        this.totalHrs = totalHrs;
    }

    public void onPostParsingTimeCard() {
        textViewTotalHrs.setText("Total Hrs this pay period: " + totalHrs);
        totalHrsLoggedToday.setText("Total Hrs Today: " + ETimeUtils.todaysTotalHrsLogged(punches));
        if (punches.size() > 0) {
            ETimeUtils.roundPunches(punches);
            lastPunch = punches.get(punches.size() - 1);
            if (lastPunch != null) {
                StringBuilder sb = new StringBuilder("Clocked ");
                if (lastPunch.isClockIn()) {
                    sb.append("in ");
                } else {
                    sb.append("out ");
                }
                sb.append("at ");
                Calendar lastPunchCalendar = lastPunch.getCalendar();
                sb.append(Integer.toString(getHourFromCalendar(lastPunchCalendar))).append(":");

                int minute = lastPunch.getCalendar().get(Calendar.MINUTE);
                if (minute < 10) {
                    sb.append("0");
                }
                sb.append(Integer.toString(minute));

                if (lastPunchCalendar.get(Calendar.AM_PM) == Calendar.AM) {
                    sb.append(" AM");
                } else {
                    sb.append(" PM");
                }

                curStatus.setText(sb.toString());

                Punch eightHrPunch = ETimeUtils.getEightHrPunch(punches);
                Calendar eightHrPunchCalendar = eightHrPunch.getCalendar();

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Clock out at ");

                StringBuilder clockTimeString = new StringBuilder();

                clockTimeString.append(Integer.toString(getHourFromCalendar(eightHrPunchCalendar))).append(":");

                int min = eightHrPunchCalendar.get(Calendar.MINUTE);
                if (min < 10) {
                    clockTimeString.append("0");
                }
                clockTimeString.append(Integer.toString(min));

                if (eightHrPunchCalendar.get(Calendar.AM_PM) == Calendar.AM) {
                    clockTimeString.append(" AM");
                } else {
                    clockTimeString.append(" PM");
                }

                stringBuilder.append(clockTimeString);
                timeToClockOut.setText(stringBuilder.toString());

                if (AUTO_CLOCKOUT) {
                    if (autoClockOutIfOkTimeCard && lastPunch.isClockIn()) {
                        clockOut();
                        autoClockOutIfOkTimeCard = false;

                        return;
                    }
                    autoClockOutIfOkTimeCard = false;

                    long countDownTime = eightHrPunch.getCalendar().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
                    if (countDownTime > 0 && lastPunch.isClockIn()) {
                        setOneTimeAlarm(eightHrPunch.getCalendar().getTimeInMillis());
                        ETimeActivity.this.notify("Auto clock out at: " + clockTimeString);
                    }
                }
            }
        }
    }

    private int getHourFromCalendar(Calendar calendar) {
        int hour24 = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour24 == 0) //12 AM
        {
            return 12;
        } else if (hour24 > 12) {
            return (hour24 - 12);
        } else {
            return hour24;
        }
    }

    public void onPostLogin() {
        hideProgressBar();

        loginTime = Calendar.getInstance().getTimeInMillis();
        showTitlePageBtns();
        parseTimeCard();
    }

    private class MyWebChromeClient extends WebChromeClient {
        public void onProgressChanged(WebView view, int progress) {
            progressBar.setProgress(progress);
        }
    }

    private void setupGlobals() {
        loginTime = 0;
        httpClient = new DefaultHttpClient();
        lastPunch = null;


        TIMESTAMP_RECORD_URL = getString(R.string.timestamp_record_url);
        TIMECARD_URL = getString(R.string.timecard_url);

        TIMESTAMP_URL = getString(R.string.timestamp_url);
        TIMESTAMP_SUCCESS = getString(R.string.timestamp_success_url);
        LOGIN_FAILED_URL = getString(R.string.login_failed_url);
        LOGIN_FAILED_URL_2 = getString(R.string.login_failed_url_2);

        webview = (WebView) findViewById(R.id.web_engine);
        webview = ETimeUtils.setupWebView(webview, new MyWebViewClient(), new MyWebChromeClient());
        cookieManager = getSyncedCookieManager();

        progressBar = (ProgressBar) findViewById(R.id.pb_progressBar);
        recordTime = (Button) findViewById(R.id.btn_recordTime);
        textViewTotalHrs = (Button) findViewById(R.id.btn_totalHrs);
        totalHrsLoggedToday = (Button) findViewById(R.id.btn_totalHrsToday);
        curStatus = (Button) findViewById(R.id.btn_curStatus);
        loading = (TextView) findViewById(R.id.tv_load);
        timeToClockOut = (Button) findViewById(R.id.btn_timeToClockOut);
    }

    private void setupTitlePage() {
        if (notCreated) {
            setContentView(R.layout.title_page);

            setupGlobals();
            setupButtons();
            notCreated = false;
        }

        login();
    }

    private CookieManager getSyncedCookieManager() {
        CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(webview.getContext());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.removeSessionCookie();
        cookieSyncManager.sync();
        return cookieManager;
    }


    private void login() {
        long curTime = Calendar.getInstance().getTimeInMillis();

        setTitle("ETime - " + loginName);

        if ((curTime - loginTime) > DEF_TIMEOUT || (oldLoginNameBeforePreferencePage != null && !oldLoginNameBeforePreferencePage.equals(loginName))) {
            if (oldLoginNameBeforePreferencePage != null && !oldLoginNameBeforePreferencePage.equals(loginName)) {
                cookieManager.removeSessionCookie();
                cookieManager.removeAllCookie();
            }

            LoginAsyncTask loginAsyncTask = new LoginAsyncTask();
            progressBar.setProgress(0);

            httpClient = new DefaultHttpClient();
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, -1),
                    new UsernamePasswordCredentials(loginName, password));
            loginAsyncTask.setProgressBar(progressBar);
            loginAsyncTask.setActivity(this);
            loginAsyncTask.setCookieManager(cookieManager);
            loginAsyncTask.setHttpClient(httpClient);
            loginAsyncTask.setContext(getApplicationContext());
            loginAsyncTask.execute();
        } else {
            hideProgressBar();
            showTitlePageBtns();
            if ((AUTO_CLOCKOUT != oldAutoClockBeforePreferencePage) || (AUTO_CLOCKOUT && autoClockOutIfOkTimeCard)) {
                parseTimeCard();
            }
        }
        oldAutoClockBeforePreferencePage = AUTO_CLOCKOUT;

    }

    private void setupButtons() {
        recordTime.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clockOut();
            }
        });
    }

    protected void startTimeCardActivity() {
        Intent intent = new Intent(activity, TimeCardActivity.class);
        intent.putExtra("loginName", loginName);
        intent.putExtra("password", password);
        startActivity(intent);
    }

    protected void parseTimeCard() {
        Log.v(TAG, "in timecard");
        TimeCardAsyncTask timeCardAsyncTask = new TimeCardAsyncTask();
        timeCardAsyncTask.setActivity((ETimeActivity) activity);
        timeCardAsyncTask.setHttpClient(httpClient);
        timeCardAsyncTask.setProgressBar(progressBar);
        timeCardAsyncTask.execute();
    }

    protected void hideTitlePageBtns() {
        recordTime.setVisibility(View.GONE);
        curStatus.setVisibility(View.GONE);
        textViewTotalHrs.setVisibility(View.GONE);
        totalHrsLoggedToday.setVisibility(View.GONE);
        timeToClockOut.setVisibility(View.GONE);
    }

    protected void showTitlePageBtns() {
        recordTime.setVisibility(View.VISIBLE);
        curStatus.setVisibility(View.VISIBLE);
        textViewTotalHrs.setVisibility(View.VISIBLE);
        totalHrsLoggedToday.setVisibility(View.VISIBLE);
        timeToClockOut.setVisibility(View.VISIBLE);
    }

    protected void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);
    }

    protected void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        loading.setVisibility(View.VISIBLE);
    }

    private boolean validConfig() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity);
        loginName = pref.getString(PREFS_USERNAME, null);
        password = pref.getString(PREFS_PASSWORD, null);
        AUTO_CLOCKOUT = pref.getBoolean(getString(R.string.autoclock), false);

        if (AUTO_CLOCKOUT != oldAutoClockBeforePreferencePage && !AUTO_CLOCKOUT) {
            if (pendingIntentAutoClockAlarm != null) {
                am.cancel(pendingIntentAutoClockAlarm);
                pendingIntentAutoClockAlarm = null;
            }
            ETimeActivity.this.notify("Auto clock out cancelled!");
        }

        return (loginName != null && !loginName.equals("")) && (password != null && !password.equals(""));
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onReceivedHttpAuthRequest(WebView view,
                                              HttpAuthHandler handler, String host, String realm) {
            handler.proceed(loginName, password);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.v(TAG, url);
            if (url.equals(TIMESTAMP_URL)) {
                hideProgressBar();
                showTitlePageBtns();
            } else if (url.equals(TIMESTAMP_SUCCESS)) {
                hideProgressBar();
                parseTimeCard();
                showTitlePageBtns();
                Toast.makeText(getApplicationContext(), "Time Stamp Successful", Toast.LENGTH_LONG).show();
            } else if (url.equals(TIMECARD_URL)) {
                hideProgressBar();
                webview.setVisibility(View.VISIBLE);
            } else if (url.equals(LOGIN_FAILED_URL) || url.equals(LOGIN_FAILED_URL_2)) {
                hideProgressBar();
                Toast.makeText(getApplicationContext(), "Invalid Username/Password", Toast.LENGTH_LONG).show();
                startPreferencesPage();
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            hideProgressBar();

            Toast.makeText(getApplicationContext(), "Unable to connect to service", Toast.LENGTH_LONG).show();
        }
    }

    private void clockOut() {
        hideTitlePageBtns();
        showProgressBar();
        webview.loadUrl(TIMESTAMP_RECORD_URL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_preferences:
                startPreferencesPage();
                break;
            case R.id.menu_timecard:
                startTimeCardActivity();
                break;
        }
        return true;
    }

    protected void loadUrl(String url) {
        hideTitlePageBtns();
        showProgressBar();
        webview.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed Called");
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity);
        if (pref.getBoolean(getString(R.string.keepInBackground), true)) {
            Intent setIntent = new Intent(Intent.ACTION_MAIN);
            setIntent.addCategory(Intent.CATEGORY_HOME);
            setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(setIntent);
        } else {
            super.onBackPressed();
        }
    }

    public void setOneTimeAlarm(long alarmTime) {
        Log.v(TAG, "in setOneTimeAlarm");
        Intent intent = new Intent(this, TimeAlarm.class);
        intent.putExtra("username", loginName);
        intent.putExtra("password", password);
        pendingIntentAutoClockAlarm = PendingIntent.getBroadcast(this, 0,
                intent, PendingIntent.FLAG_ONE_SHOT);
        am.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntentAutoClockAlarm);
    }

}
