/*
 * Copyright (C) 2014 The Retro Watch - Open source smart watch project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hardcopy.retrowatch.service;

import com.hardcopy.retrowatch.utils.Constants;
import com.hardcopy.retrowatch.utils.Logs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotificationReceiverService extends NotificationListenerService {

    private static final String TAG = "NotificationReceiverService";
	
    // Notification broadcast intent key
    public static final String NOTIFICATION_KEY_CMD = "notification_command";
    public static final String NOTIFICATION_KEY_ID = "notification_id";
    public static final String NOTIFICATION_KEY_PACKAGE = "notification_package";
    public static final String NOTIFICATION_KEY_TEXT = "notification_text";
    
    // Notification command type
    public static final int NOTIFICATION_CMD_ADD = 1;
    public static final int NOTIFICATION_CMD_REMOVE = 2;
    public static final int NOTIFICATION_CMD_LIST = 3;
	
    private NLServiceReceiver nlservicereciver;
    
    
    @Override
    public void onCreate() {
        super.onCreate();
        nlservicereciver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.NOTIFICATION_LISTENER_SERVICE);
        registerReceiver(nlservicereciver,filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nlservicereciver);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Logs.d(TAG,"**********  onNotificationPosted");
        Logs.d(TAG,"ID :" + sbn.getId() + "t" + sbn.getNotification().tickerText + "t" + sbn.getPackageName());
        
        Intent i = new  Intent(Constants.NOTIFICATION_LISTENER);
        i.putExtra(NOTIFICATION_KEY_CMD, NOTIFICATION_CMD_ADD);
        i.putExtra(NOTIFICATION_KEY_ID, sbn.getId());
        i.putExtra(NOTIFICATION_KEY_PACKAGE, sbn.getPackageName());
        i.putExtra(NOTIFICATION_KEY_TEXT, sbn.getNotification().tickerText);
        sendBroadcast(i);

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Logs.d(TAG,"********** onNOtificationRemoved");
        Logs.d(TAG,"ID :" + sbn.getId() + "t" + sbn.getNotification().tickerText +"t" + sbn.getPackageName());
        
        Intent i = new  Intent(Constants.NOTIFICATION_LISTENER);
        i.putExtra(NOTIFICATION_KEY_CMD, NOTIFICATION_CMD_REMOVE);
        i.putExtra(NOTIFICATION_KEY_ID, sbn.getId());
        i.putExtra(NOTIFICATION_KEY_PACKAGE, sbn.getPackageName());
        i.putExtra(NOTIFICATION_KEY_TEXT, sbn.getNotification().tickerText);
        sendBroadcast(i);
    }

    
    class NLServiceReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("command").equals("clearall")){
            	NotificationReceiverService.this.cancelAllNotifications();
            }
            else if(intent.getStringExtra("command").equals("list")){
                for (StatusBarNotification sbn : NotificationReceiverService.this.getActiveNotifications()) {
                    Intent i2 = new  Intent(Constants.NOTIFICATION_LISTENER);
                    i2.putExtra(NOTIFICATION_KEY_CMD, NOTIFICATION_CMD_LIST);
                    i2.putExtra(NOTIFICATION_KEY_ID, sbn.getId());
                    i2.putExtra(NOTIFICATION_KEY_PACKAGE, sbn.getPackageName());
                    i2.putExtra(NOTIFICATION_KEY_TEXT, sbn.getNotification().tickerText);
                    sendBroadcast(i2);
                }
            }

        }
    }	// End of class NLServiceReceiver
    
}
