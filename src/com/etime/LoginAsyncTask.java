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
import android.os.AsyncTask;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.List;

/**
 * User: dpayne2
 * Date: 1/5/12
 * Time: 12:02 PM
 */
public class LoginAsyncTask extends AsyncTask<String, Integer, Boolean> {

    private ProgressBar progressBar;
    private ETimeActivity activity;
    private DefaultHttpClient httpClient;
    private CookieManager cookieManager;
    private Context context;
    private String LOGIN_URL;
    private String LOGIN_URL_STEP2;
    private String LOGIN_URL_STEP3;
    private String LOGIN_FAILED;

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
        LOGIN_URL = activity.getString(R.string.login_url);
        LOGIN_URL_STEP2 = activity.getString(R.string.login_url2);
        LOGIN_URL_STEP3 = activity.getString(R.string.login_url3);
        LOGIN_FAILED = activity.getString(R.string.login_failed_str);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        boolean status;

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

    /**
     * Sign on to the ADP eTime site. Return true if signon was successful, false otherwise. Sign on is done through
     * hitting a series of ADP pages with basic authentication set. Sets the progress bar on the main page.
     * @return  true if signon was successful, false otherwise.
     */
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
