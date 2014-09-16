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

package com.hardcopy.retrowatchle.service;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.hardcopy.retrowatchle.R;
import com.hardcopy.retrowatchle.connectivity.BluetoothManager;
import com.hardcopy.retrowatchle.connectivity.ConnectionInfo;
import com.hardcopy.retrowatchle.connectivity.TransactionBuilder;
import com.hardcopy.retrowatchle.connectivity.TransactionReceiver;
import com.hardcopy.retrowatchle.connectivity.TransactionBuilder.Transaction;
import com.hardcopy.retrowatchle.contents.ContentManager;
import com.hardcopy.retrowatchle.contents.IContentManagerListener;
import com.hardcopy.retrowatchle.contents.objects.CPObject;
import com.hardcopy.retrowatchle.contents.objects.ContentObject;
import com.hardcopy.retrowatchle.contents.objects.EmergencyObject;
import com.hardcopy.retrowatchle.contents.objects.FilterObject;
import com.hardcopy.retrowatchle.utils.Constants;
import com.hardcopy.retrowatchle.utils.Logs;
import com.hardcopy.retrowatchle.utils.Settings;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class RetroWatchService extends Service implements IContentManagerListener {

	private static final String TAG = "RetroWatchService";
	
	private static final long SENDING_CONTENTS_INTERVAL = 30*60*1000;
	
	// Context, System
	private Context mContext = null;
	private static Handler mActivityHandler = null;
	private ServiceHandler mServiceHandler = new ServiceHandler();
	private final IBinder mBinder = new RetroWatchServiceBinder();
	
	// Notification broadcast receiver
	private NotificationReceiver mReceiver;
	
	// Bluetooth
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothManager mBtManager = null;
	
	private ConnectionInfo mConnectionInfo = null;
	
	private TransactionBuilder mTransactionBuilder = null;
	private TransactionReceiver mTransactionReceiver = null;
	
	// Contents
	private ContentManager mContentManager = null;
	
	// Auto-refresh timer
	private Timer mRefreshTimer = null;
	private Timer mDeleteTimer = null;
    
	
	/*****************************************************
	 * 
	 *	Overrided methods
	 *
	 ******************************************************/
	@Override
	public void onCreate() {
		Logs.d(TAG, "# Service - onCreate() starts here");
		
		mContext = getApplicationContext();
		initialize();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Logs.d(TAG, "# Service - onStartCommand() starts here");
		
		// If service returns START_STICKY, android restarts service automatically after forced close.
		// At this time, onStartCommand() method in service must handle null intent.
		return Service.START_STICKY;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig){
		// This prevents reload after configuration changes
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Logs.d(TAG, "# Service - onBind()");
		return mBinder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Logs.d(TAG, "# Service - onUnbind()");
		return true;
	}
	
	@Override
	public void onDestroy() {
		Logs.d(TAG, "# Service - onDestroy()");
		finalizeService();
	}
	
	@Override
	public void onLowMemory (){
		Logs.d(TAG, "# Service - onLowMemory()");
		// onDestroy is not always called when applications are finished by Android system.
		finalizeService();
	}
	
	@Override
	public void OnContentCallback(int msgType, int arg0, int arg1, String arg2, String arg3, Object arg4) {
		switch(msgType) {
		case IContentManagerListener.CALLBACK_GMAIL_UPDATED:
			if(mActivityHandler != null)
				mActivityHandler.obtainMessage(Constants.MESSAGE_GMAIL_UPDATED, arg4).sendToTarget();
			if(arg4 != null)
				sendContentsToDevice((ContentObject) arg4);
			break;
		
		case IContentManagerListener.CALLBACK_FEED_UPDATED:
			ArrayList<ContentObject> feedList = mContentManager.refreshFeedList();
			mActivityHandler.obtainMessage(Constants.MESSAGE_FEED_UPDATED, feedList).sendToTarget();
			break;
			
		default:
			break;
		}
	}

	/*****************************************************
	 * 
	 *	Private methods
	 *
	 ******************************************************/
	private void initialize() {
		Logs.d(TAG, "# Service : initialize ---");
		
		// Get content manager instance
		mContentManager = ContentManager.getInstance(mContext, this);
		// Get connection info instance
		mConnectionInfo = ConnectionInfo.getInstance(mContext);
		
		// Set notification broadcast receiver 
		mReceiver = new NotificationReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.NOTIFICATION_LISTENER);
		registerReceiver(mReceiver,filter);
		
		// Set battery broadcast receiver
		IntentFilter iFilter = new IntentFilter();
		iFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(mBatteryInfoReceiver, iFilter);
		
		// Set SMS listener
		IntentFilter smsFilter = new IntentFilter();
		smsFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
		registerReceiver(mSmsListener, smsFilter);
		
		// Set telephony listener
		TelephonyStateListener telephonyListener = new TelephonyStateListener();
		TelephonyManager telephony = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		telephony.listen(telephonyListener, PhoneStateListener.LISTEN_SERVICE_STATE);
		telephony.listen(telephonyListener, PhoneStateListener.LISTEN_CALL_STATE);
		
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			return;
		}
		
		if (!mBluetoothAdapter.isEnabled()) {
			// BT is not on, need to turn on manually.
			// Activity will do this.
		} else {
			if(mBtManager == null) {
				setupBT();
			}
		}
		
		// Start service monitoring
		startServiceMonitoring();
	}
	
	/**
	 * Disabled: Make a notification and register it.
	 */
	private void makeNotification(String title, String text, String ticker) {
		NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification.Builder ncomp = new Notification.Builder(this);
		ncomp.setContentTitle(title);
		if(text != null)
			ncomp.setContentText(text);
		if(ticker != null)
			ncomp.setTicker(ticker);
		ncomp.setSmallIcon(R.drawable.ic_launcher);
		ncomp.setAutoCancel(true);
		nManager.notify((int)System.currentTimeMillis(),ncomp.build());
	}
	
	private void sendTimeToDevice() {
		TransactionBuilder.Transaction transaction = mTransactionBuilder.makeTransaction();
		transaction.begin();
		transaction.setCommand(Transaction.COMMAND_TYPE_SET_TIME);
		transaction.setDate();
		transaction.settingFinished();
		transaction.sendTransaction();
	}
	
	private void resetNormalObjectOfDevice() {
		TransactionBuilder.Transaction transaction = mTransactionBuilder.makeTransaction();
		transaction.begin();
		transaction.setCommand(Transaction.COMMAND_TYPE_RESET_NORMAL_OBJ);
		transaction.settingFinished();
		transaction.sendTransaction();
		
		// Arduino doesn't catch first command's end byte. 
		// But I don't know why.
		// To be sure, send reset transaction again.
		TransactionBuilder.Transaction transaction2 = mTransactionBuilder.makeTransaction();
		transaction2.begin();
		transaction2.setCommand(Transaction.COMMAND_TYPE_RESET_NORMAL_OBJ);
		transaction2.settingFinished();
		transaction2.sendTransaction();
	}
	
	private void resetEmergencyObjectOfDevice() {
		TransactionBuilder.Transaction transaction = mTransactionBuilder.makeTransaction();
		transaction.begin();
		transaction.setCommand(Transaction.COMMAND_TYPE_RESET_EMERGENCY_OBJ);
		transaction.settingFinished();
		transaction.sendTransaction();
		
		// Arduino doesn't catch first command's end byte. 
		// But I don't know why.
		// To be sure, send reset transaction again.
		TransactionBuilder.Transaction transaction2 = mTransactionBuilder.makeTransaction();
		transaction2.begin();
		transaction2.setCommand(Transaction.COMMAND_TYPE_RESET_EMERGENCY_OBJ);
		transaction2.settingFinished();
		transaction2.sendTransaction();
	}
	
	private void sendDataResetSignalToDevice() {
		sendTimeToDevice();
		resetNormalObjectOfDevice();
		resetEmergencyObjectOfDevice();
	}
	
	private void sendContentsToDevice(ArrayList<ContentObject> contents) {
		for(ContentObject obj : contents) {
			if(obj.mIsEnabled)
				sendContentsToDevice(obj);	// Send enabled items only
		}
	}
	
	private boolean sendContentsToDevice(ContentObject obj) {
		if(obj == null || mTransactionBuilder==null || !obj.mIsEnabled
				/*|| obj.mId < 0*/ || obj.mFilteredString == null || obj.mFilteredString.length() < 1)
			return false;
		
		TransactionBuilder.Transaction transaction = mTransactionBuilder.makeTransaction();
		transaction.begin();

		switch(obj.mContentType) {
		case ContentObject.CONTENT_TYPE_NOTIFICATION:
			// Set transaction parameters
			transaction.setCommand(Transaction.COMMAND_TYPE_ADD_NORMAL_OBJ);
			transaction.setIcon(obj.mIconType);
			transaction.setMessage(obj.mId, obj.mFilteredString);
			
			transaction.settingFinished();
			transaction.sendTransaction();
			break;
			
		case ContentObject.CONTENT_TYPE_FEED:
		case ContentObject.CONTENT_TYPE_MESSAGING:
			// Set transaction parameters
			transaction.setCommand(Transaction.COMMAND_TYPE_ADD_NORMAL_OBJ);
			transaction.setIcon(obj.mIconType);
			transaction.setMessage(obj.mId, obj.mFilteredString);
			
			transaction.settingFinished();
			transaction.sendTransaction();
			break;
			
		case ContentObject.CONTENT_TYPE_EMERGENCY:
			// Set transaction parameters
			transaction.setCommand(Transaction.COMMAND_TYPE_ADD_EMERGENCY_OBJ);
			transaction.setIcon(obj.mIconType);
			transaction.setMessage(obj.mId, obj.mFilteredString);
			
			transaction.settingFinished();
			transaction.sendTransaction();
			break;
			
		default:
			return false;
		}
		
		return true;
	}
	
	private void deleteEmergencyOfDevice(int type) {
		if(mTransactionBuilder == null && mBtManager != null && mActivityHandler != null) {
			mTransactionBuilder = new TransactionBuilder(mBtManager, mActivityHandler);
		}
		
		if(mTransactionBuilder != null) {
			TransactionBuilder.Transaction transaction = mTransactionBuilder.makeTransaction();
			transaction.begin();
			transaction.setCommand(TransactionBuilder.Transaction.COMMAND_TYPE_DELETE_EMERGENCY_OBJ);
			transaction.setId(type);		// use type as ID
			
			transaction.settingFinished();
			transaction.sendTransaction();
		}
	}
	
	private void sendClockStyleToDevice(int style) {
		if(mTransactionBuilder == null && mBtManager != null && mActivityHandler != null) {
			mTransactionBuilder = new TransactionBuilder(mBtManager, mActivityHandler);
		}
		
		if(mTransactionBuilder != null) {
			TransactionBuilder.Transaction transaction = mTransactionBuilder.makeTransaction();
			transaction.begin();
			transaction.setCommand(TransactionBuilder.Transaction.COMMAND_TYPE_SET_CLOCK_STYLE);
			transaction.setMessage(style, null);
			
			transaction.settingFinished();
			transaction.sendTransaction();
		}
	}
	
	private void sendIndicatorSettingToDevice(int code) {
		if(mTransactionBuilder == null && mBtManager != null && mActivityHandler != null) {
			mTransactionBuilder = new TransactionBuilder(mBtManager, mActivityHandler);
		}
		
		if(mTransactionBuilder != null) {
			TransactionBuilder.Transaction transaction = mTransactionBuilder.makeTransaction();
			transaction.begin();
			transaction.setCommand(TransactionBuilder.Transaction.COMMAND_TYPE_SHOW_INDICATOR);
			transaction.setMessage(code, null);
			
			transaction.settingFinished();
			transaction.sendTransaction();
		}
	}
	
	
	/*****************************************************
	 * 
	 *	Public methods
	 *
	 ******************************************************/
	public void finalizeService() {
		Logs.d(TAG, "# Service : finalize ---");
		
		mBluetoothAdapter = null;
		// Stop the bluetooth session
		if (mBtManager != null)
			mBtManager.stop();
		mBtManager = null;
		// Unregister broadcast receiver
		if(mReceiver != null)
			unregisterReceiver(mReceiver);
		mReceiver = null;
		if(mBatteryInfoReceiver != null)
			unregisterReceiver(mBatteryInfoReceiver);
		mBatteryInfoReceiver = null;
		
		// Stop the timer
		if(mRefreshTimer != null) {
			mRefreshTimer.cancel();
			mRefreshTimer = null;
		}
		if(mDeleteTimer != null) {
			mDeleteTimer.cancel();
			mDeleteTimer = null;
		}
		
		mContentManager.finalize();
	}
	
	public void setupService(Handler h) {
		mActivityHandler = h;
		
		// Double check BT manager instance
		if(mBtManager == null)
			setupBT();
		
		// Initialize transaction builder & receiver
		if(mTransactionBuilder == null)
			mTransactionBuilder = new TransactionBuilder(mBtManager, mActivityHandler);
		if(mTransactionReceiver == null)
			mTransactionReceiver = new TransactionReceiver(mActivityHandler);
		
		// If ConnectionInfo holds previous connection info,
		// try to connect using it.
		if(mConnectionInfo.getDeviceAddress() != null && mConnectionInfo.getDeviceName() != null) {
			connectDevice(mConnectionInfo.getDeviceAddress());
		} 
		// or wait in listening mode
		else {
			if (mBtManager.getState() == BluetoothManager.STATE_NONE) {
				// Start the bluetooth services
				mBtManager.start();
			}
		}
	}
	
    /**
     * Setup and initialize BT manager
     */
	public void setupBT() {
        Logs.d(TAG, "Service - setupBT()");

        // Initialize the BluetoothManager to perform bluetooth connections
        if(mBtManager == null)
        	mBtManager = new BluetoothManager(this, mServiceHandler);
    }
	
    /**
     * Check bluetooth is enabled or not.
     */
	public boolean isBluetoothEnabled() {
		if(mBluetoothAdapter==null) {
			Logs.e(TAG, "# Service - cannot find bluetooth adapter. Restart app.");
			return false;
		}
		return mBluetoothAdapter.isEnabled();
	}
	
	/**
	 * Get scan mode
	 */
	public int getBluetoothScanMode() {
		int scanMode = -1;
		if(mBluetoothAdapter != null)
			scanMode = mBluetoothAdapter.getScanMode();
		
		return scanMode;
	}

    /**
     * Initiate a connection to a remote device.
     * @param address  Device's MAC address to connect
     */
	public void connectDevice(String address) {
		Logs.d(TAG, "Service - connect to " + address);
		
		// Get the BluetoothDevice object
		if(mBluetoothAdapter != null) {
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			
			if(device != null && mBtManager != null) {
				mBtManager.connect(device);
			}
		}
	}
	
    /**
     * Connect to a remote device.
     * @param device  The BluetoothDevice to connect
     */
	public void connectDevice(BluetoothDevice device) {
		if(device != null && mBtManager != null) {
			mBtManager.connect(device);
		}
	}

	/**
	 * Get connected device name
	 */
	public String getDeviceName() {
		return mConnectionInfo.getDeviceName();
	}
	
	public boolean sendEveryContentsToDevice() {
		ArrayList<ContentObject> contents = mContentManager.getContentObjectList();
		sendContentsToDevice(contents);
		return true;
	}
	
	public void sendClockStyle(int style) {
		sendClockStyleToDevice(style);
	}
	
	public void showIndicator(int code) {
		sendIndicatorSettingToDevice(code);
	}
	
	public ArrayList<ContentObject> refreshContentObjectList() {
		return mContentManager.refreshContentObjectList();
	}

	/**
	 * WARNING: Remove all notifications from indicator.
	 */
	public void sendClearAllNotificationsSignal() {
		// Send command to NotificationListenerService
		Intent i = new Intent(Constants.NOTIFICATION_LISTENER_SERVICE);
		i.putExtra("command","clearall");
		sendBroadcast(i);
		
		// Clear all contents in ContentManager
		mContentManager.clearAllNotifications();
	}
	
	/**
	 * Extract all notifications currently registered.
	 * NotificationReceiverService will send results one by one using broadcast.
	 * And service send each notification to activity handler. 
	 */
	public void sendGetAllNotificationsSignal() {
		// Send command to NotificationService
		Intent i = new Intent(Constants.NOTIFICATION_LISTENER_SERVICE);
		i.putExtra("command","list");
		sendBroadcast(i);
		
		// Clear all contents in ContentManager
		mContentManager.clearAllNotifications();
		
		//----- Result will be delivered on broadcast receiver
	}
	
	public ArrayList<CPObject> getRssAll() {
		return mContentManager.getCPObjectList();
	}
	
	public int addRss(CPObject cpo) {
		if(cpo == null)
			return Constants.RESPONSE_ADD_RSS_FAILED;
		return mContentManager.addCPObject(cpo);
	}
	
	public int editRss(CPObject cpo) {
		if(cpo == null)
			return Constants.RESPONSE_EDIT_RSS_FAILED;
		return mContentManager.updateCPObject(cpo);
	}
	
	public int deleteRss(int rss_id) {
		if(rss_id < 0)
			return Constants.RESPONSE_DELETE_FILTER_FAILED;
		return mContentManager.deleteCPObject(rss_id);
	}
	
	public ArrayList<FilterObject> getFiltersAll() {
		return mContentManager.getFilterObjectList();
	}
	
	public int addFilter(FilterObject filter) {
		if(filter == null)
			return Constants.RESPONSE_ADD_FILTER_FAILED;
		return mContentManager.addFilter(filter);
	}
	
	public int editFilter(FilterObject filter) {
		if(filter == null)
			return Constants.RESPONSE_EDIT_FILTER_FAILED;
		return mContentManager.editFilter(filter);
	}
	
	public int deleteFilter(int filter_id) {
		if(filter_id < 0)
			return Constants.RESPONSE_DELETE_FILTER_FAILED;
		return mContentManager.deleteFilter(filter_id);
	}
	
	public void reserveRemoteUpdate(long delay) {
		try {
			if(mRefreshTimer != null)
				mRefreshTimer.cancel();
		} catch(IllegalStateException e) {
			e.printStackTrace();
		}
		
		mRefreshTimer = new Timer();
		mRefreshTimer.schedule(new RefreshTimerTask(), delay, SENDING_CONTENTS_INTERVAL);
	}
	
	public void setGmailAddress(String gmailAddr) {
		mContentManager.setGmailAddress(gmailAddr);
	}
	
	/**
	 * Start service monitoring. Service monitoring prevents
	 * unintended close of service.
	 */
	public void startServiceMonitoring() {
		if(Settings.getInstance(mContext).getRunInBackground()) {
			ServiceMonitoring.startMonitoring(mContext);
		} else {
			ServiceMonitoring.stopMonitoring(mContext);
		}
	}
	
	
	
	/*****************************************************
	 * 
	 *	Handler, Listener, Timer, Sub classes
	 *
	 ******************************************************/
	public class RetroWatchServiceBinder extends Binder {
		public RetroWatchService getService() {
			return RetroWatchService.this;
		}
	}
	
	class ServiceHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg) {
			
			switch(msg.what) {
			case BluetoothManager.MESSAGE_STATE_CHANGE:
				// Bluetooth state Changed
				Logs.d(TAG, "Service - MESSAGE_STATE_CHANGE: " + msg.arg1);
				
				switch (msg.arg1) {
				case BluetoothManager.STATE_NONE:
					mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_INITIALIZED).sendToTarget();
					if(mRefreshTimer != null) {
						mRefreshTimer.cancel();
						mRefreshTimer = null;
					}
					break;
					
				case BluetoothManager.STATE_LISTEN:
					mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_LISTENING).sendToTarget();
					break;
					
				case BluetoothManager.STATE_CONNECTING:
					mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_CONNECTING).sendToTarget();
					break;
					
				case BluetoothManager.STATE_CONNECTED:
					mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_CONNECTED).sendToTarget();
					
					// Fully update remote device every 1 hour
					reserveRemoteUpdate(5000);
					break;
				}
				break;

			case BluetoothManager.MESSAGE_WRITE:
				Logs.d(TAG, "Service - MESSAGE_WRITE: ");
				break;

			case BluetoothManager.MESSAGE_READ:
				Logs.d(TAG, "Service - MESSAGE_READ: ");
				
				byte[] readBuf = (byte[]) msg.obj;
				// construct commands from the valid bytes in the buffer
				if(mTransactionReceiver != null)
					mTransactionReceiver.setByteArray(readBuf);
				break;
				
			case BluetoothManager.MESSAGE_DEVICE_NAME:
				Logs.d(TAG, "Service - MESSAGE_DEVICE_NAME: ");
				
				// save connected device's name and notify using toast
				String deviceAddress = msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_ADDRESS);
				String deviceName = msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_NAME);
				
				if(deviceName != null && deviceAddress != null) {
					// Remember device's address and name
					mConnectionInfo.setDeviceAddress(deviceAddress);
					mConnectionInfo.setDeviceName(deviceName);
					
					Toast.makeText(getApplicationContext(), 
							"Connected to " + deviceName, Toast.LENGTH_SHORT).show();
				}
				break;
				
			case BluetoothManager.MESSAGE_TOAST:
				Logs.d(TAG, "Service - MESSAGE_TOAST: ");
				
//				Toast.makeText(getApplicationContext(), 
//						msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_TOAST), 
//						Toast.LENGTH_SHORT).show();
				break;
				
			}	// End of switch(msg.what)
			
			super.handleMessage(msg);
		}
	}	// End of class MainHandler

	
	/**
	 * Broadcast receiver class. Receives notification data
	 */
	class NotificationReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			/**
			 * RetroWatch LE doesn't support notification listener service
			 * This feature requires Android v4.3 or over.
			 */
			
			/*
			int cmd = intent.getIntExtra(NotificationReceiverService.NOTIFICATION_KEY_CMD, 0);
			int noti_id = intent.getIntExtra(NotificationReceiverService.NOTIFICATION_KEY_ID, -1);
			String packageName = intent.getStringExtra(NotificationReceiverService.NOTIFICATION_KEY_PACKAGE);
			String textTicker = intent.getStringExtra(NotificationReceiverService.NOTIFICATION_KEY_TEXT);
			
			switch(cmd) {
			case NotificationReceiverService.NOTIFICATION_CMD_LIST:
			case NotificationReceiverService.NOTIFICATION_CMD_ADD:
				if(packageName != null) {
					Logs.d(TAG,"** Service - Add noti="+noti_id+", package="+packageName);
					// Add notification and get converted content type object
					ContentObject obj = mContentManager.addNotification(ContentObject.CONTENT_TYPE_MESSAGING, packageName, textTicker);
						
					if(obj != null) {
						// notify to activity
						mActivityHandler.obtainMessage(Constants.MESSAGE_ADD_NOTIFICATION, noti_id, 0, (Object)obj).sendToTarget();
						// send to device
						sendContentsToDevice(obj);
					}
				}
				break;
				
			case NotificationReceiverService.NOTIFICATION_CMD_REMOVE:
				Logs.d(TAG,"** Service - Delete noti="+noti_id+", package="+packageName);
				
				// notify to Content manager
				mContentManager.deleteNotification(noti_id);
				// notify to activity
				mActivityHandler.obtainMessage(Constants.MESSAGE_DELETE_NOTIFICATION, noti_id, 0).sendToTarget();
				// Disabled: notify to device
				// removeContentsOfDevice(obj);
			
				// Set delete timer.
				// User action [clear noti] could make another delete message.
				// To make simple scenario, wait for a while and synchronize every item with remote.
				if(mDeleteTimer == null) {
					mDeleteTimer = new Timer();
					mDeleteTimer.schedule(new DeleteTimerTask(), 3*1000);
				}
				break;
			}	// End of switch(cmd)
			*/
			
		}	// End of onReceive()
		
	}	// End of class NotificationReceiver
	
	
	public SmsListener mSmsListener = new SmsListener();
	public class SmsListener extends BroadcastReceiver{
		public SmsListener() {
			super();
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
				Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
				SmsMessage[] msgs = null;
				if (bundle != null){
					//---retrieve the SMS message received---
					try{
						Object[] pdus = (Object[]) bundle.get("pdus");
						msgs = new SmsMessage[pdus.length];
						if(msgs != null && msgs.length > 0) {
							ContentObject co = mContentManager.addSMSObject(msgs.length);	// Add content using message count
							if(co != null) {
								mActivityHandler.obtainMessage(Constants.MESSAGE_SMS_RECEIVED, (Object)co).sendToTarget();
								// send to device
								sendContentsToDevice(co);
							}
						}
						
						// Use new message count only
//						for(int i=0; i<msgs.length; i++){
//							msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
//							String msg_from = msgs[i].getOriginatingAddress();
//							String msgBody = msgs[i].getMessageBody();
//						}
					}catch(Exception e){
						Logs.d(TAG, e.getMessage());
					}
				}
			}
			
		}	// End of onReceive()
	}
	
	public class TelephonyStateListener extends PhoneStateListener  {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			Logs.d(TAG, "PhoneStateListener - onCallStateChanged();");
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
			case TelephonyManager.CALL_STATE_RINGING:
				ContentObject co = mContentManager.addCallObject(state, incomingNumber);
				if(mActivityHandler != null)
					mActivityHandler.obtainMessage(Constants.MESSAGE_CALL_STATE_RECEIVED, co).sendToTarget();
				// send to device
				if(co != null)
					sendContentsToDevice(co);
				else
					deleteEmergencyOfDevice(EmergencyObject.EMERGENCY_TYPE_CALL_STATE);
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
			default:
				Logs.d(TAG, "PhoneStateListener - Default state="+state+", Number="+incomingNumber);
				break;
			}
		}
		
		@Override
		public void onServiceStateChanged(ServiceState serviceState) {
			int state = serviceState.getState();
			
			switch (state) {
			case ServiceState.STATE_IN_SERVICE:
			case ServiceState.STATE_OUT_OF_SERVICE:
			case ServiceState.STATE_EMERGENCY_ONLY:
			case ServiceState.STATE_POWER_OFF:
				ContentObject co = mContentManager.addRFStateObject(state);
				if(mActivityHandler != null)
					mActivityHandler.obtainMessage(Constants.MESSAGE_RF_STATE_RECEIVED, co).sendToTarget();
				// send to device
				if(co != null)
					sendContentsToDevice(co);
				else
					deleteEmergencyOfDevice(EmergencyObject.EMERGENCY_TYPE_RF_STATE);
				break;
			}
		}
	}	// End of TelephonyStateListener
	
	private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
				int plugType = intent.getIntExtra("plugged", 0);
				int status = intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
				
				int chargingStatus = EmergencyObject.BATT_STATE_UNKNOWN;
				if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
					if (plugType > 0) {
						chargingStatus = ((plugType == BatteryManager.BATTERY_PLUGGED_AC) 
								? EmergencyObject.BATT_STATE_AC_CHARGING : EmergencyObject.BATT_STATE_USB_CHARGING);
					}
				} else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
					chargingStatus = EmergencyObject.BATT_STATE_DISCHARGING;
				} else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
					chargingStatus = EmergencyObject.BATT_STATE_NOT_CHARGING;
				} else if (status == BatteryManager.BATTERY_STATUS_FULL) {
					chargingStatus = EmergencyObject.BATT_STATE_FULL;
				} else {
					chargingStatus = EmergencyObject.BATT_STATE_UNKNOWN;
				}
				
				int level = intent.getIntExtra("level", 0);
				int scale = intent.getIntExtra("scale", 100);
				
				// WARNING: Battery service makes too many broadcast.
				// Process data only when there's change in battery level or status.
				if(mContentManager.getBatteryLevel() == level
						&& mContentManager.getBatteryChargingState() == status)
					return;
				
				ContentObject co = mContentManager.setBatteryInfo(level * 100 / scale, chargingStatus);
				if(co != null)
					sendContentsToDevice(co);
			}
		}
	};
	
    /**
     * Auto-refresh Timer
     */
	private class RefreshTimerTask extends TimerTask {
		public RefreshTimerTask() {}
		
		@Override
		public void run() {
			mServiceHandler.post(new Runnable() {
				public void run() {
					sendDataResetSignalToDevice();
					sendEveryContentsToDevice();
				}
			});
		}
	}
	
	private class DeleteTimerTask extends TimerTask {
		public DeleteTimerTask() {}
		
		@Override
		public void run() {
			mServiceHandler.post(new Runnable() {
				public void run() {
					sendDataResetSignalToDevice();
					sendEveryContentsToDevice();
					mDeleteTimer = null;
				}
			});
		}
	}
	
}
