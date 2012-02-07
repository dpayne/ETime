package com.etime;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * User: dpayne2
 * Date: 2/7/12
 * Time: 1:58 PM
 */
public class TimestampAsyncTask extends AsyncTask <String, Integer, Boolean> implements ETimeAsyncTask{

    private ETimeActivity activity;
    private DefaultHttpClient httpClient;
    private ProgressBar progressBar;
    private String TIMESTAMP_RECORD_URL;
    private String LOGIN_FAILED;
    private int TIMESTAMP_URL_PAGE_SIZE;

    @Override
    protected void onPreExecute() {
        Resources res = activity.getResources();
        TIMESTAMP_RECORD_URL = activity.getString(R.string.timestamp_record_url);
        LOGIN_FAILED = activity.getString(R.string.login_failed_str);
        TIMESTAMP_URL_PAGE_SIZE = res.getInteger(R.integer.size_of_timestamp_success_url);
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        String page = ETimeUtils.getHtmlPageWithProgress(httpClient, TIMESTAMP_RECORD_URL, this, 0, 100, TIMESTAMP_URL_PAGE_SIZE);
        if (page == null || page.contains(LOGIN_FAILED)) {
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        Toast.makeText(activity, "Timestamp Successful", Toast.LENGTH_LONG).show();
        activity.hideProgressBar();
        activity.showTitlePageBtns();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        progressBar.setProgress(values[0]);
    }

    public void setActivity(ETimeActivity activity) {
        this.activity = activity;
    }

    public void setHttpClient(DefaultHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public void publishToProgressBar(int progress) {
        publishProgress(progress);
    }
}
