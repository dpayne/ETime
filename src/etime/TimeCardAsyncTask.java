package etime;

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
    private String TIMECARD_URL = "https://eet.adp.com/wfc/applications/mss/esstimecard.do";
    private boolean status = false;

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
        myProgress = 0;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        String page =  ETimeUtils.getHtmlPage(httpClient, TIMECARD_URL);
        totalHrs = ETimeUtils.getTotalsHrs(page);
        punches = ETimeUtils.getTodaysPunches(page);
        status = !punches.isEmpty();
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