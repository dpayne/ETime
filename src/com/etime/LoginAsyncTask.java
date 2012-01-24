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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Calendar;
import java.util.List;

/**
 * User: dpayne2
 * Date: 1/5/12
 * Time: 12:02 PM
 */
public class LoginAsyncTask extends AsyncTask<String, Integer, Boolean> {

    private String PREFS_USERNAME = "username";
    private String PREF_LOGINTIME = "loginTime";

    private ProgressBar progressBar;
    private ETimeActivity activity;
    private DefaultHttpClient httpClient;
    private CookieManager cookieManager;
    private Context context = null;
    private String LOGIN_URL = "https://eet.adp.com/public/etime/html.html";
    private String LOGIN_URL_STEP2 = "https://eet.adp.com/wfc/SSOLogon/logonWithUID?IntendedURL=/wfc/applications/suitenav/navigation.do?ESS=true";
    private String LOGIN_URL_STEP3 = "https://eet.adp.com/wfc/applications/wtk/html/ess/timestamp.jsp";
    private static final String LOGIN_FAILED = "Logon attempt failed";

    private boolean status = false;
    private String onPostLoadUrl = null;

    private static final String TAG = "Login-4321";

    int myProgress;

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            activity.onPostLogin();
            if (onPostLoadUrl != null) {
                activity.loadUrl(onPostLoadUrl);
            }
        } else {


            Toast.makeText(context, "Bad Username/Password", Toast.LENGTH_LONG).show();

            activity.startPreferencesPage();
        }
    }

    @Override
    protected void onPreExecute() {
        myProgress = 0;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        if (params.length > 0)
            onPostLoadUrl = params[0];
        if (!signon()) {
            myProgress = 100;
            publishProgress(myProgress);
            status = false;
        } else {
            status = true;
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
        }
        myProgress = 100;
        publishProgress(myProgress);
        return status;
    }

    public boolean signon() {
        String page;
        myProgress = 0;

        page = ETimeUtils.getHtmlPage(httpClient, LOGIN_URL);
        if (page == null || page.contains(LOGIN_FAILED)) {
            return false;
        }
        myProgress += 32;
        publishProgress(myProgress);

        page = ETimeUtils.getHtmlPage(httpClient, LOGIN_URL_STEP2);
        if (page == null || page.contains(LOGIN_FAILED)) {
            return false;
        }
        myProgress += 32;
        publishProgress(myProgress);

        page = ETimeUtils.getHtmlPage(httpClient, LOGIN_URL_STEP3);
        if (page == null || page.contains(LOGIN_FAILED)) {
            return false;
        }
        myProgress += 32;
        publishProgress(myProgress);

        long curTime = Calendar.getInstance().getTimeInMillis();
        SharedPreferences pref = activity.getSharedPreferences(PREFS_USERNAME, Context.MODE_PRIVATE);
        pref.edit().putLong(PREF_LOGINTIME, curTime).commit();

        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        progressBar.setProgress(values[0]);
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public void setActivity(ETimeActivity activity) {
        this.activity = activity;
    }

    public void setCookieManager(CookieManager cookieManager) {
        this.cookieManager = cookieManager;
    }

    public void setHttpClient(DefaultHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
