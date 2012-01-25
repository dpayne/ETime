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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * User: dpayne2
 * Date: 1/13/12
 * Time: 11:03 PM
 */
public class TimeAlarm extends BroadcastReceiver {

	private String loginName;
    private String password;

    @Override
    public void onReceive(Context context, Intent intent) {
    	Log.v("ETime-TimeAlarm", "In TimeAlarm onReceive");
    	
    	loginName = intent.getStringExtra("username");
        password = intent.getStringExtra("password");
        Log.v("ETime-TimeAlarm", "loginName="+loginName);
        
    	Intent alarmIntent = new Intent(context, TimeAlarmService.class);
    	alarmIntent.putExtra(TimeAlarmService.USERNAME, loginName);
    	alarmIntent.putExtra(TimeAlarmService.PASSWORD, password);
    	TimeAlarmService.getLock(context).acquire();
    	context.startService(alarmIntent);    	
        
    }

   
}

