package etime;

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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * User: dpayne2
 * Date: 1/7/12
 * Time: 3:26 AM
 */
public class TimeCardActivity extends Activity {
    ProgressBar progressBar;
    private TextView loadingText;
    private String loginName;
    private String password;

    private String TIMECARD_URL;

    private WebView webview;
    
    private static final String TAG = "TimeCardActivity-4321";
    private static String LOGIN_URL;
    private String TIMESTAMP_URL;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.time_card);
        Bundle extras = getIntent().getExtras();

        TIMECARD_URL = getString(R.string.timecard_url);
        LOGIN_URL = getString(R.string.login_url);
        TIMESTAMP_URL = getString(R.string.timestamp_url);

        progressBar = (ProgressBar) findViewById(R.id.pb_timecard);
        loadingText = (TextView) findViewById(R.id.tv_loadTimeCard);

        if(extras !=null)
        {
            loginName = extras.getString("loginName");
            password = extras.getString("password");
        }

        setupWebView();

        webview.loadUrl(LOGIN_URL);
    }

    private WebView setupWebView() {
        
        webview = (WebView) findViewById(R.id.web_timecard);

        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webview.getSettings().setAllowFileAccess(true);
        webview.getSettings().setBuiltInZoomControls(true);
        webview.getSettings().setSupportZoom(true);
        webview.canGoBack();

        webview.setWebViewClient(new MyWebViewClient());
        webview.setWebChromeClient(new MyWebChromeClient());

        return webview;
    }
    
    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
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
            if (url.equals(TIMECARD_URL)) {
                hideProgressBar();
                webview.setVisibility(View.VISIBLE);
            } else if (url.equals(TIMESTAMP_URL)) {
                progressBar.setProgress(0);
                webview.loadUrl(TIMECARD_URL);
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            hideProgressBar();
            Toast.makeText(getApplicationContext(), "Unable to connect to service", Toast.LENGTH_LONG).show();
        }
    }


    private class MyWebChromeClient extends  WebChromeClient{
        public void onProgressChanged(WebView view, int progress) {
            progressBar.setProgress(progress);
        }
    }
}
