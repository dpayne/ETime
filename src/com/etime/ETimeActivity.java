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
    final Activity activity = this;
    private String TAG = "ETime-4321";

    /* used to handle the auto clock out alarm */
    private AlarmManager am;
    private PendingIntent pendingIntentAutoClockAlarm;

    private String PREFS_USERNAME = "username";
    private String PREFS_PASSWORD = "password";

    private String TIMESTAMP_RECORD_URL;
    private String TIMESTAMP_SUCCESS;
    private String LOGIN_FAILED_URL;
    private String LOGIN_FAILED_URL_2;

    private WebView webview;
    private DefaultHttpClient httpClient;

    private String loginName = null;
    private String password = null;

    private long loginTime; //Used to determine if loginTime is expired

    private static final long DEF_TIMEOUT = 900000; // 15 mins in milliseconds

    private ProgressBar progressBar;
    private ProgressBar progressBar2;
    private Button recordTime;

    private List<Punch> punches;  // list of punches in/out for today
    private double totalHrs;      // total hrs logged today, not counting time since last punch in
    private String oldLoginNameBeforePreferencePage;

    private Button curStatus;
    private Button textViewTotalHrs;
    private Button totalHrsLoggedToday;
    private Button timeToClockOut;

    private TextView loading;

    private Punch lastPunch;
    private boolean AUTO_CLOCKOUT;

    private NotificationManager mManager;
    private static final int APP_ID = 1;

    private boolean notCreated = true;    // onResume not run yet
    private boolean autoClockOutIfOkTimeCard = false;
    private boolean oldAutoClockBeforePreferencePage;
    private CookieManager cookieManager;
    private String lastNotificationMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Called when the activity is resumed from sleep. Called during onCreate.
     * Checks if the preferences are valid (password and username are
     * non-empty). If empty, preferences activty is brought up, other wise
     * title page is setup.
     *
     * {@inheritDoc}
     */
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

    /**
     * Notify the user with the message "message". Notification is set to
     * on-going, on-going is needed to tell android not to kill the app.
     * The phone with vibrate, and light up on notification. If the message
     * is the exact same message as the last message notified then the
     * notification is not set again.
     * @param message  Message to notify user with
     */
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
        //notification.defaults |= Notification.DEFAULT_VIBRATE;
        //notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.setLatestEventInfo(context, contentTitle, message, contentIntent);
        mManager.notify("ETime", APP_ID, notification);
    }

    /**
     * Start the preferences page
     */
    protected void startPreferencesPage() {
        oldLoginNameBeforePreferencePage = loginName;
        oldAutoClockBeforePreferencePage = AUTO_CLOCKOUT;
        startActivity(new Intent(activity, ETimePreferences.class));
    }

    /**
     * Set punches
     * @param punches List of punches of all of today's punch in/out's
     */
    public void setPunches(List<Punch> punches) {
        this.punches = punches;
    }

    /**
     * Set total hrs logged today, does not include time since last clock in
     * @param totalHrs total hrs logged today, does not include time since last clock in
     */
    public void setTotalHrs(double totalHrs) {
        this.totalHrs = totalHrs;
    }

    /**
     * Called when TimeCardAsyncTask is done processing. Does lot of stuff.
     */
    public void onPostParsingTimeCard() {

        textViewTotalHrs.setText("Total Hrs this pay period: " + totalHrs);
        totalHrsLoggedToday.setText("Total Hrs Today: " + ETimeUtils.todaysTotalHrsLogged(punches));

        if (punches.size() > 0) {
            lastPunch = punches.get(punches.size() - 1);
            if (lastPunch != null) {
                updateCurStatusBtn();
                Punch eightHrPunch = ETimeUtils.getEightHrPunch(punches);
                Calendar eightHrPunchCalendar = eightHrPunch.getCalendar();

                updateTimeToClockOut(eightHrPunchCalendar);

                setAutoClockOut(eightHrPunch);
            }
        }
    }

    /**
     * Update the curStatus button with most recent data from the users
     * time card.
     */
    private void updateCurStatusBtn() {
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
    }

    /**
     * set the alarm so auto clock occurs at the time specified
     * by eightHrPunch.
     * @param eightHrPunch Is an identical Punch instance to
     *                     what the punch would be if the user
     *                     clocked out at exactly 8 hrs.
     */
    private void setAutoClockOut(Punch eightHrPunch) {
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
                ETimeActivity.this.notify("Auto clock out at: " + punchToTimeString(eightHrPunch));
            }
        }
    }

    /**
     * Converts a punch to a human readable time format
     *          "12:00 AM"
     * @param punch Punch to convert to readable string
     * @return A string conversion of a Punch in a readable format
     */
    public String punchToTimeString(Punch punch) {
        Calendar calendar = punch.getCalendar();
        int hour = getHourFromCalendar(calendar);
        int minute =  calendar.get(Calendar.MINUTE);
        String minStr = (minute < 10) ? "0" + minute : ""+minute;

        return " " + hour
                + ":" + minStr + " "
                + ((calendar.get(Calendar.AM_PM) == Calendar.AM) ? "AM" : "PM");
    }

    /**
     * Update the button timeToClockOut
     * @param eightHrPunchCalendar    A calendar object specifying the time where
     *                                the user has been punched in for exactly 8 hrs
     *                                today.
     */
    private void updateTimeToClockOut(Calendar eightHrPunchCalendar) {
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
    }

    /**
     * Get the hour of day in am/pm from a Calendar time
     * @param calendar Calendar with the hour you want to convert
     * @return The hour in am/pm format from HOUR_OF_DAY in Calendar which
     * ranges from 0-23
     */
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

    /**
     * Run after LoginAsyncTask finishes. Record the time this function
     * is run for loginTime.
     */
    public void onPostLogin() {
        hideProgressBar();

        loginTime = Calendar.getInstance().getTimeInMillis();
        showTitlePageBtns();
        parseTimeCard();
    }

    /**
     * Used to set the progressBar in the UI.
     */
    private class MyWebChromeClient extends WebChromeClient {
        public void onProgressChanged(WebView view, int progress) {
            progressBar.setProgress(progress);
        }
    }

    /**
     * setup global variables, only called on the first call to onResume
     */
    private void setupGlobals() {
        loginTime = 0;
        httpClient = new DefaultHttpClient();
        lastPunch = null;

        TIMESTAMP_RECORD_URL = getString(R.string.timestamp_record_url);
        TIMESTAMP_SUCCESS = getString(R.string.timestamp_success_url);
        LOGIN_FAILED_URL = getString(R.string.login_failed_url);
        LOGIN_FAILED_URL_2 = getString(R.string.login_failed_url_2);

        webview = (WebView) findViewById(R.id.web_engine);
        webview = ETimeUtils.setupWebView(webview, new MyWebViewClient(), new MyWebChromeClient());
        cookieManager = getSyncedCookieManager();

        progressBar = (ProgressBar) findViewById(R.id.pb_progressBar);
        progressBar2 = (ProgressBar) findViewById(R.id.progressBar2);
        recordTime = (Button) findViewById(R.id.btn_recordTime);
        textViewTotalHrs = (Button) findViewById(R.id.btn_totalHrs);
        totalHrsLoggedToday = (Button) findViewById(R.id.btn_totalHrsToday);
        curStatus = (Button) findViewById(R.id.btn_curStatus);
        loading = (TextView) findViewById(R.id.tv_load);
        timeToClockOut = (Button) findViewById(R.id.btn_timeToClockOut);
    }

    /**
     * setup the ui title page. On the first time run, setup global variables
     * and buttons.
     */
    private void setupTitlePage() {
        if (notCreated) {
            setContentView(R.layout.title_page);

            setupGlobals();
            setupButtons();
            notCreated = false;
        }

        login();
    }

    /**
     * Return a sync cookie manager with webview, clear previous sessions
     * and cookies in webview.
     * @return A sync cookie manager with webivew.
     */
    private CookieManager getSyncedCookieManager() {
        CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(webview.getContext());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.removeSessionCookie();
        cookieSyncManager.sync();
        return cookieManager;
    }

    /**
     * login - sets the title of the app to "ETime - username". Hides the progress bar and
     * shows the title page including the text and buttons. Logs in into the main adp site
     * and saves the relevant cookies in httpclient.
     *
     * If login has happened in the last 15 mins, don't re-login.
     */
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

    /**
     * setup the buttons on the title page (e.g. setup the record timestamp
     * button to clockOut() when pressed.
     */
    private void setupButtons() {
        recordTime.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clockOut();
            }
        });
    }

    /**
     * start the time card activity
     */
    protected void startTimeCardActivity() {
        Intent intent = new Intent(activity, TimeCardActivity.class);
        intent.putExtra("loginName", loginName);
        intent.putExtra("password", password);
        startActivity(intent);
    }

    /**
     * parse the timecard of the user. Starts a TimeCardAsyncTask
     */
    protected void parseTimeCard() {
        TimeCardAsyncTask timeCardAsyncTask = new TimeCardAsyncTask();
        timeCardAsyncTask.setActivity((ETimeActivity) activity);
        timeCardAsyncTask.setHttpClient(httpClient);
        timeCardAsyncTask.setProgressBar(progressBar);
        timeCardAsyncTask.execute();
    }

    /**
     * Hide title page buttons and text.
     */
    protected void hideTitlePageBtns() {
        recordTime.setVisibility(View.GONE);
        curStatus.setVisibility(View.GONE);
        textViewTotalHrs.setVisibility(View.GONE);
        totalHrsLoggedToday.setVisibility(View.GONE);
        timeToClockOut.setVisibility(View.GONE);
    }

    /**
     * Show title page buttons and text.
     */
    protected void showTitlePageBtns() {
        recordTime.setVisibility(View.VISIBLE);
        curStatus.setVisibility(View.VISIBLE);
        textViewTotalHrs.setVisibility(View.VISIBLE);
        totalHrsLoggedToday.setVisibility(View.VISIBLE);
        timeToClockOut.setVisibility(View.VISIBLE);
    }

    /**
     * Hide progress bar and text.
     */
    protected void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);
        progressBar2.setVisibility(View.GONE);
    }

    /**
     * Show progress bar and text.
     */
    protected void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        loading.setVisibility(View.VISIBLE);
    }

    /**
     * Return if the username and password is non-empty.
     * This method also has the side effect of settting the loginName,
     * password, and autoclock.
     * @return return true if loginName an password are non-empty, false otherwise.
     */
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

    /**
     * Internal class to create a custom webview. Used to override authentication,
     * set callback when a page loads, and
     */
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
            if (url.equals(TIMESTAMP_SUCCESS)) {
                hideProgressBar();
                parseTimeCard();
                showTitlePageBtns();
                Toast.makeText(getApplicationContext(), "Time Stamp Successful", Toast.LENGTH_LONG).show();
            } else if (url.equals(LOGIN_FAILED_URL) || url.equals(LOGIN_FAILED_URL_2)) {
                hideProgressBar();
                Toast.makeText(getApplicationContext(), "Invalid Username/Password", Toast.LENGTH_LONG).show();
                startPreferencesPage();
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            hideProgressBar();
            showTitlePageBtns();
            Toast.makeText(getApplicationContext(), "Unable to connect to service", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Load the timestamp page in webview
     */
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
            case R.id.menu_refresh:
                loginTime = 0;
                progressBar2.setVisibility(View.VISIBLE);
                onResume();                
                break;
        }
        return true;
    }

    /**
     * Load a given url in webview
     * @param url the url page to be loaded
     */
    protected void loadUrl(String url) {
        hideTitlePageBtns();
        showProgressBar();
        webview.loadUrl(url);
    }

    /**
     * Callback for pressing the back button. Used to prevent the app
     * from destroying it self if the back button is pressed to go to
     * previous app/homescreen.
     */
    @Override
    public void onBackPressed() {
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

    /**
     * Set the alarm that will perform autoclock out at the specified time.
     * @param alarmTime time in milliseconds since epoch when the alarm
     *                  should run.
     */
    public void setOneTimeAlarm(long alarmTime) {
        Intent intent = new Intent(this, TimeAlarm.class);
        intent.putExtra("username", loginName);
        intent.putExtra("password", password);
        pendingIntentAutoClockAlarm = PendingIntent.getBroadcast(this, 0,
                intent, PendingIntent.FLAG_ONE_SHOT);
        am.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntentAutoClockAlarm);
    }

}
