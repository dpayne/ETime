package com.etime;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.commonsware.cwac.wakeful.WakefulIntentService;

/**
 * User: dpayne2
 * Date: 1/23/12
 * Time: 3:58 PM
 */
public class ETimeAlarmListener implements WakefulIntentService.AlarmListener {
    private String loginName;
    private String password;
    private long time;
    private Intent intent;
    private boolean intentSet = false;

    public void scheduleAlarms(AlarmManager alarmManager, PendingIntent pendingIntent, Context context) {
        if (!intentSet) {
            intent = new Intent(context, ETimeAlarmService.class);
            intent.putExtra("username", loginName);
            intent.putExtra("password", password);
        } else {
            intentSet = true;
        }

        PendingIntent pendingIntentAutoClockAlarm = PendingIntent.getService(context, 0,
                intent, PendingIntent.FLAG_ONE_SHOT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntentAutoClockAlarm);
    }

    public void sendWakefulWork(Context context) {
        if (!intentSet) {
            intent = new Intent(context, ETimeAlarmService.class);
            intent.putExtra("username", loginName);
            intent.putExtra("password", password);
        } else {
            intentSet = true;
        }
        WakefulIntentService.sendWakefulWork(context, intent);
    }

    public long getMaxAge() {
        return Long.MAX_VALUE;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
