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
import android.content.res.Resources;
import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * User: dpayne2
 * Date: 1/5/12
 * Time: 12:02 PM
 */
public class LoginAsyncTask extends AsyncTask<String, Integer, Boolean> implements ETimeAsyncTask {

    private ProgressBar progressBar;
    private ETimeActivity activity;
    private DefaultHttpClient httpClient;
    private Context context;
    private String LOGIN_URL;
    private String LOGIN_URL_STEP2;
    private String LOGIN_FAILED;

    private static final String TAG = "Login-4321";

    int myProgress;
    private int LOGIN_URL_PAGE_SIZE;
    private int LOGIN_URL2_PAGE_SIZE;

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            activity.onPostLogin();
        } else {
            Toast.makeText(context, "Bad Username/Password", Toast.LENGTH_LONG).show();
            activity.startPreferencesPage();
            activity.setLoginTime(0);
        }
    }

    @Override
    protected void onPreExecute() {
        Resources res = activity.getResources();
        myProgress = 0;
        LOGIN_URL = activity.getString(R.string.login_url);
        LOGIN_URL_STEP2 = activity.getString(R.string.login_url2);
        LOGIN_FAILED = activity.getString(R.string.login_failed_str);
        LOGIN_URL_PAGE_SIZE = res.getInteger(R.integer.size_of_login_url);
        LOGIN_URL2_PAGE_SIZE = res.getInteger(R.integer.size_of_login_url2_noredirect);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        boolean status;

        if (!signon()) {
            myProgress = 100;
            publishProgress(myProgress);
            status = false;
        } else {
            status = true;
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

        publishProgress(10);
        page = ETimeUtils.getHtmlPageWithProgress(httpClient, LOGIN_URL, this, 10, 30, LOGIN_URL_PAGE_SIZE);
        if (page == null || page.contains(LOGIN_FAILED)) {
            return false;
        }

        page = ETimeUtils.getHtmlPageWithProgress(httpClient, LOGIN_URL_STEP2, this, 30, 50, LOGIN_URL2_PAGE_SIZE);
        if (page == null || page.contains(LOGIN_FAILED)) {
            return false;
        }

        page = ETimeUtils.getHtmlPageWithProgress(httpClient, page, this, 50, 80, LOGIN_URL_PAGE_SIZE);
        if (page == null || page.contains(LOGIN_FAILED)) {
            return false;
        }

        page = ETimeUtils.getHtmlPageWithProgress(httpClient, page, this, 80, 100, LOGIN_URL_PAGE_SIZE);
        if (page == null || page.contains(LOGIN_FAILED)) {
            return false;
        }

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

    public void setHttpClient(DefaultHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void publishToProgressBar(int progress) {
        publishProgress(progress);
    }
}
