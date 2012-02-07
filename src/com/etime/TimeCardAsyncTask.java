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

import android.os.AsyncTask;
import android.widget.ProgressBar;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.List;

/**
 * User: dpayne2
 * Date: 1/5/12
 * Time: 12:02 PM
 */
public class TimeCardAsyncTask extends AsyncTask<Void, Integer, Boolean> {

    private ProgressBar progressBar;
    private ETimeActivity activity;
    private DefaultHttpClient httpClient;
    private String TIMECARD_URL;

    private static final String TAG = "TimeCard-4321";

    int myProgress;

    private List<Punch> punches;
    private double totalHrs;

    @Override
    protected void onPostExecute(Boolean result) {
        activity.setPunches(punches);
        activity.setTotalHrs(totalHrs);
        activity.onPostParsingTimeCard();
    }

    @Override
    protected void onPreExecute() {
        TIMECARD_URL = activity.getString(R.string.timecard_url);
        myProgress = 0;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        String page = ETimeUtils.getHtmlPage(httpClient, TIMECARD_URL);
        totalHrs = ETimeUtils.getTotalsHrs(page);
        punches = ETimeUtils.getTodaysPunches(page);
        boolean status = !punches.isEmpty();
        myProgress = 100;
        publishProgress(myProgress);
        return status;
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
}