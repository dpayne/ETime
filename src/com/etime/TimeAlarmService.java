package com.etime;

import java.util.Calendar;
import java.util.List;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class TimeAlarmService extends IntentService{

	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
    private WebView webView;
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
    
	public TimeAlarmService() {
		super("AlarmIntentService");
		// TODO Auto-generated constructor stub
	}
	
	 @Override
    protected void onHandleIntent(Intent intent) {
		 Log.v("ETime-AlarmIntentService", "In AlarmIntentService onHandleIntent");
		 try{
		 	boolean retval;
	        webView = new WebView(getApplicationContext());
	        webView = ETimeUtils.setupWebView(webView, new MyWebViewClient(), new MyWebChromeClient());

	        loginName = intent.getStringExtra(USERNAME);
	        password = intent.getStringExtra(PASSWORD);
	        
	        Log.v("ETime-AlarmIntentService", "loginName="+loginName);
	        
	        retval = login();

	        if (!retval) {
	            notifyAutoClockOutFailure(this.getApplicationContext());
	        }

	        parseTimeCard();

	        retval = clockOut();


	        if (!retval) {
	            notifyAutoClockOutFailure(this.getApplicationContext());
	        } else {
	            notifyAutoClockOutSuccess(this.getApplicationContext());
	        }
		 }finally{
			 getLock(this.getApplicationContext()).release();
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
	        notif.setLatestEventInfo(context, from, notifcationString, contentIntent);
	        nm.notify("ETime", 1, notif);
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
	        int deltaHr = Math.abs(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)-punch.getCalendar().get(Calendar.HOUR_OF_DAY));
	        int deltaMin = Math.abs(Calendar.getInstance().get(Calendar.MINUTE)-punch.getCalendar().get(Calendar.MINUTE));

	        double delta = ((double) deltaHr) + (((double) deltaMin)/60.0);
	        Log.v(TAG, "Delta is " + deltaHr + ":" + deltaMin+ " delta total is " + delta);

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
	        CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(webView.getContext());
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
	    }

	    private class MyWebChromeClient extends WebChromeClient {
	    }
	    
	    static final String NAME="com.etime.AlarmIntentService";
	    private static volatile PowerManager.WakeLock lockStatic=null;
	    synchronized static PowerManager.WakeLock getLock(Context context) {
	        if (lockStatic==null) {
	          PowerManager mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
	          
	          lockStatic=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
	                                     NAME);
	          lockStatic.setReferenceCounted(true);
	        }
	        
	        return(lockStatic);
	      }
}
