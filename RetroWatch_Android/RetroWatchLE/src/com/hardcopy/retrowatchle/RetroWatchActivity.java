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

package com.hardcopy.retrowatchle;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.hardcopy.retrowatchle.contents.objects.CPObject;
import com.hardcopy.retrowatchle.contents.objects.ContentObject;
import com.hardcopy.retrowatchle.contents.objects.FilterObject;
import com.hardcopy.retrowatchle.service.RetroWatchService;
import com.hardcopy.retrowatchle.utils.Constants;
import com.hardcopy.retrowatchle.utils.Logs;
import com.hardcopy.retrowatchle.utils.RecycleUtils;
import com.hardcopy.retrowatchle.utils.Utils;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class RetroWatchActivity extends FragmentActivity implements ActionBar.TabListener, IFragmentListener {

    // Debugging
    private static final String TAG = "RetroWatchActivity";
    
    private static final long REMOTE_REFRESH_DELAY = 3*1000;
	
	// Context, System
	private Context mContext;
	private RetroWatchService mService;
	private Utils mUtils;
	private ActivityHandler mActivityHandler;
	
	// Global
	private boolean mStopService = false;
	
	// UI stuff
	private FragmentManager mFragmentManager;
	private RetroWatchFragmentAdapter mSectionsPagerAdapter;
	private ViewPager mViewPager;
	
	private ImageView mImageBT = null;
	private TextView mTextStatus = null;

	// Refresh timer
	private Timer mRefreshTimer = null;
	
	
	/*****************************************************
	 * 
	 *	 Overrided methods
	 *
	 ******************************************************/
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//----- System, Context
		mContext = this;//.getApplicationContext();
		mActivityHandler = new ActivityHandler();
		
		setContentView(R.layout.activity_retro_watch);

		// Load static utilities
		mUtils = new Utils(mContext);
		
		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the primary sections of the app.
		mFragmentManager = getSupportFragmentManager();
		mSectionsPagerAdapter = new RetroWatchFragmentAdapter(mFragmentManager, mContext, this);

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by the adapter.
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
		
		// Setup views
		mImageBT = (ImageView) findViewById(R.id.status_title);
		mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_invisible));
		mTextStatus = (TextView) findViewById(R.id.status_text);
		mTextStatus.setText(getResources().getString(R.string.bt_state_init));
		
		// Do data initialization after service started and binded
		doStartService();
	}

	@Override
	public synchronized void onStart() {
		super.onStart();
		
		if(mRefreshTimer != null) {
			mRefreshTimer.cancel();
		}
		mRefreshTimer = new Timer();
		mRefreshTimer.schedule(new RefreshTimerTask(), REMOTE_REFRESH_DELAY);
	}
	
	@Override
	public synchronized void onPause() {
		super.onPause();
	}
	
	@Override
	public void onStop() {
		// Stop the timer
		if(mRefreshTimer != null) {
			mRefreshTimer.cancel();
			mRefreshTimer = null;
		}
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		finalizeActivity();
	}
	
	@Override
	public void onLowMemory (){
		super.onLowMemory();
		// onDestroy is not always called when applications are finished by Android system.
		finalizeActivity();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.retro_watch, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_scan:
			// Launch the DeviceListActivity to see devices and do scan
			doScan();
			return true;
		/**
		 * RetroWatch LE doesn't support notification listener service
		 * 
		case R.id.action_noti_settings:
			// Launch notification settings screen
			setNotificationAccess();
			return true;
		*/
		case R.id.action_refresh:
			// Refresh every contents
			refreshContentObjects();
			return true;
		case R.id.action_send_all:
			// Send all available contents to watch
			mService.reserveRemoteUpdate(100);
			return true;
		/* Disabled:
		case R.id.action_discoverable:
			// Disabled: Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		case R.id.action_settings:
			// Disabled: 
			break;
		*/
		}
		return false;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();		// TODO: Disable this line to run below code
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig){
		// This prevents reload after configuration changes
		super.onConfigurationChanged(newConfig);
	}
	
	
	/**
	 * Implements TabListener
	 */
	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}
	
	@Override
	public void OnFragmentCallback(int msgType, int arg0, int arg1, String arg2, String arg3, Object arg4) {
		switch(msgType) {
		case IFragmentListener.CALLBACK_REQUEST_FILTERS:
			getFiltersAll();
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_ADD_FILTER:
			int id = Constants.RESPONSE_ADD_FILTER_FAILED;
			FilterObject filterObj = null;
			if(mService != null && arg4 != null) {
				filterObj = (FilterObject) arg4;
				id = mService.addFilter(filterObj);
			} else {
				break;
			}
			
			if(id > Constants.RESPONSE_ADD_FILTER_FAILED) {
				FiltersFragment frg = (FiltersFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_FILTERS);
				frg.addFilter(filterObj);
				
				ArrayList<ContentObject> contents = mService.refreshContentObjectList();
				MessageListFragment frg2 = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				frg2.deleteMessageAll();
				frg2.addMessageAll(contents);
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_EDIT_FILTER:
			int id2 = Constants.RESPONSE_EDIT_FILTER_FAILED;
			FilterObject filterObject = null;
			if(mService != null && arg4 != null) {
				filterObject = (FilterObject) arg4;
				id2 = mService.editFilter(filterObject);
			} else {
				break;
			}
			
			if(id2 > Constants.RESPONSE_EDIT_FILTER_FAILED) {
				FiltersFragment frg = (FiltersFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_FILTERS);
				frg.editFilter(filterObject);
				
				ArrayList<ContentObject> contents = mService.refreshContentObjectList();
				MessageListFragment frg2 = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				frg2.deleteMessageAll();
				frg2.addMessageAll(contents);
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_DELETE_FILTER:
			if(mService != null && arg4 != null) {
				FilterObject filter = (FilterObject) arg4;
				if(mService.deleteFilter(filter.mId) > Constants.RESPONSE_DELETE_FILTER_FAILED) {
					FiltersFragment frg = (FiltersFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_FILTERS);
					frg.deleteFilter(filter.mId);
					
					ArrayList<ContentObject> contents = mService.refreshContentObjectList();
					MessageListFragment frg2 = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
					frg2.deleteMessageAll();
					frg2.addMessageAll(contents);
				}
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_RSS:
			getRssAll();
			break;
		case IFragmentListener.CALLBACK_REQUEST_ADD_RSS:
			int id5 = Constants.RESPONSE_ADD_FILTER_FAILED;
			CPObject cpObj = null;
			if(mService != null && arg4 != null) {
				cpObj = (CPObject) arg4;
				id5 = mService.addRss(cpObj);
				if(id5 > -1) {
					RssFragment frg = (RssFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_RSS);
					frg.addRss(cpObj);
				}
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_EDIT_RSS:
			int id6 = Constants.RESPONSE_EDIT_FILTER_FAILED;
			CPObject cpObj2 = null;
			if(mService != null && arg4 != null) {
				cpObj2 = (CPObject) arg4;
				id6 = mService.editRss(cpObj2);
				if(id6 > -1) {
					RssFragment frg = (RssFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_RSS);
					frg.editRss(cpObj2);
				}
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_DELETE_RSS:
			int id7 = Constants.RESPONSE_EDIT_FILTER_FAILED;
			CPObject cpObj3 = null;
			if(mService != null && arg4 != null) {
				cpObj3 = (CPObject) arg4;
				id7 = mService.deleteRss(cpObj3.mId);
				if(id7 > -1) {
					RssFragment frg = (RssFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_RSS);
					frg.deleteRss(cpObj3.mId);
					
					ArrayList<ContentObject> contents = mService.refreshContentObjectList();
					MessageListFragment frg2 = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
					frg2.deleteMessageAll();
					frg2.addMessageAll(contents);
				}
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_CLOCK_STYLE:
			int clockStyle = arg0;
			if(mService != null) {
				mService.sendClockStyle(clockStyle);
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_SHOW_INDICATOR:
			int indicator = arg0;
			if(mService != null) {
				mService.showIndicator(indicator);
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_SET_EMAIL_ADDRESS:
			if(mService != null) {
				mService.setGmailAddress(arg2);
			}
			break;
			
		case IFragmentListener.CALLBACK_REQUEST_RUN_IN_BACKGROUND:
			if(mService != null) {
				mService.startServiceMonitoring();
			}
			break;
			
		default:
			break;
		}
	}
	

	
	/*****************************************************
	 * 
	 *	Private classes
	 *
	 ******************************************************/
	
	/**
	 * Service connection
	 */
	private ServiceConnection mServiceConn = new ServiceConnection() {
		
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Logs.d(TAG, "Activity - Service connected");
			
			mService = ((RetroWatchService.RetroWatchServiceBinder) binder).getService();
			
			// Activity couldn't work with mService until connections are made
			// So initialize parameters and settings here, not while running onCreate()
			initialize();
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};
	
	private void doStartService() {
		Logs.d(TAG, "# Activity - doStartService()");
		startService(new Intent(this, RetroWatchService.class));
		bindService(new Intent(this, RetroWatchService.class), mServiceConn, Context.BIND_AUTO_CREATE);
	}
	
	private void doStopService() {
		Logs.d(TAG, "# Activity - doStopService()");
		mService.finalizeService();
		stopService(new Intent(this, RetroWatchService.class));
	}
	
	/**
	 * Initialization / Finalization
	 */
	private void initialize() {
		Logs.d(TAG, "# Activity - initialize()");
		mService.setupService(mActivityHandler);
		
		// If BT is not on, request that it be enabled.
		// RetroWatchService.setupBT() will then be called during onActivityResult
		if(!mService.isBluetoothEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
		}
		
		// Get notifications from NotificationListenerService
		mService.sendGetAllNotificationsSignal();
		
		// Get filters
		getFiltersAll();
	}
	
	private void finalizeActivity() {
		Logs.d(TAG, "# Activity - finalizeActivity()");
		
		if(mStopService)
			doStopService();
		
		unbindService(mServiceConn);

		RecycleUtils.recursiveRecycle(getWindow().getDecorView());
		System.gc();
	}
	
	/**
	 * Launch the DeviceListActivity to see devices and do scan
	 */
	private void doScan() {
		Intent intent = new Intent(this, DeviceListActivity.class);
		startActivityForResult(intent, Constants.REQUEST_CONNECT_DEVICE);
	}
	
	/**
	 * Launch notification settings screen
	 */
	private void setNotificationAccess() {
		Intent intent=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
		startActivity(intent);
	}
	
	/**
	 * Ensure this device is discoverable by others
	 */
	private void ensureDiscoverable() {
		if (mService.getBluetoothScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(intent);
		}
	}
	
	private void refreshContentObjects() {
		if(mService != null) {
			mService.sendGetAllNotificationsSignal();		// Delete cached notifications and set refresh signal
			ArrayList<ContentObject> contents = mService.refreshContentObjectList();	// Get cached contents
			
			MessageListFragment frg2 = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
			frg2.deleteMessageAll();
			frg2.addMessageAll(contents);
		}
	}
	
	
	
	/*****************************************************
	 * 
	 *	Public classes
	 *
	 ******************************************************/
	
	/**
	 * Receives result from external activity
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Logs.d(TAG, "onActivityResult " + resultCode);
		
		switch(requestCode) {
		case Constants.REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Attempt to connect to the device
				if(address != null && mService != null)
					mService.connectDevice(address);
			}
			break;
			
		case Constants.REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a BT session
				mService.setupBT();
			} else {
				// User did not enable Bluetooth or an error occured
				Logs.e(TAG, "BT is not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
			}
			break;
		}	// End of switch(requestCode)
	}
	
	private void getFiltersAll() {
		if(mService != null) {
			ArrayList<FilterObject> filterList = mService.getFiltersAll();
			FiltersFragment frg = (FiltersFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_FILTERS);
			frg.addFilterAll(filterList);
		}
	}
	
	private void getRssAll() {
		if(mService != null) {
			ArrayList<CPObject> cpoList = mService.getRssAll();
			RssFragment frg = (RssFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_RSS);
			frg.addRssAll(cpoList);
		}
	}
	
	
	
	/*****************************************************
	 * 
	 *	Handler, Callback, Sub-classes
	 *
	 ******************************************************/
	
	public class ActivityHandler extends Handler {
		@Override
		public void handleMessage(Message msg) 
		{
			switch(msg.what) {
			// BT state message
			case Constants.MESSAGE_BT_STATE_INITIALIZED:
				mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + 
						getResources().getString(R.string.bt_state_init));
				mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_invisible));
				break;
			case Constants.MESSAGE_BT_STATE_LISTENING:
				mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + 
						getResources().getString(R.string.bt_state_wait));
				mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_invisible));
				break;
			case Constants.MESSAGE_BT_STATE_CONNECTING:
				mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + 
						getResources().getString(R.string.bt_state_connect));
				mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_away));
				break;
			case Constants.MESSAGE_BT_STATE_CONNECTED:
				if(mService != null) {
					String deviceName = mService.getDeviceName();
					if(deviceName != null) {
						mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + 
								getResources().getString(R.string.bt_state_connected) + " " + deviceName);
						mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_online));
					}
				}
				break;
			case Constants.MESSAGE_BT_STATE_ERROR:
				mTextStatus.setText(getResources().getString(R.string.bt_state_error));
				mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_busy));
				break;
			
			// BT Command status
			case Constants.MESSAGE_CMD_ERROR_NOT_CONNECTED:
				mTextStatus.setText(getResources().getString(R.string.bt_cmd_sending_error));
				mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_busy));
				break;
				
			////////////////////////////////////////////
			// Contents changed
			////////////////////////////////////////////
			case Constants.MESSAGE_ADD_NOTIFICATION:
			{
				ContentObject obj = (ContentObject)msg.obj;
				MessageListFragment frg = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				if(frg != null)
					frg.addMessage(obj);
				break;
			}
			
			case Constants.MESSAGE_DELETE_NOTIFICATION:
			{
				int _id = msg.arg1;
				MessageListFragment frg = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				if(frg != null)
					frg.deleteMessage(_id);
				break;
			}
			
			case Constants.MESSAGE_GMAIL_UPDATED:
			{
				ContentObject obj = null;
				if(msg.obj != null) {
					obj = (ContentObject)msg.obj;
				}
				MessageListFragment frg = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				if(frg != null) {
					frg.deleteMessageByTypeAndName(ContentObject.CONTENT_TYPE_MESSAGING, ContentObject.GMAIL_PACKAGE_NAME);
					frg.addMessage(obj);
				}
				break;
			}
			
			// Disable: this case is deprecated
			case Constants.MESSAGE_SMS_RECEIVED:
			{
				ContentObject obj = null;
				if(msg.obj != null) {
					obj = (ContentObject)msg.obj;
				}
				MessageListFragment frg = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				if(frg != null) {
					frg.deleteMessageByTypeAndName(ContentObject.CONTENT_TYPE_MESSAGING, ContentObject.SMS_PACKAGE_NAME);
					frg.addMessage(obj);
				}
				break;
			}
			
			case Constants.MESSAGE_CALL_STATE_RECEIVED:
			case Constants.MESSAGE_RF_STATE_RECEIVED:
			{
				ContentObject obj = null;
				if(msg.obj != null) {
					obj = (ContentObject)msg.obj;
				}
				MessageListFragment frg = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				if(frg != null) {
					if(msg.what == Constants.MESSAGE_CALL_STATE_RECEIVED)
						frg.deleteMessageByTypeAndName(ContentObject.CONTENT_TYPE_EMERGENCY, ContentObject.TELEPHONY_CALL_PACKAGE_NAME);
					else
						frg.deleteMessageByTypeAndName(ContentObject.CONTENT_TYPE_EMERGENCY, ContentObject.TELEPHONY_RF_PACKAGE_NAME);
					frg.addMessage(obj);
				}
				break;
			}
			
			case Constants.MESSAGE_FEED_UPDATED:
			{
				ArrayList<ContentObject> feedList = null;
				if(msg.obj != null) {
					feedList = (ArrayList<ContentObject>)msg.obj;
				}
				MessageListFragment frg = (MessageListFragment) mSectionsPagerAdapter.getItem(RetroWatchFragmentAdapter.FRAGMENT_POS_MESSAGE_LIST);
				if(frg != null) {
					frg.deleteMessageByType(ContentObject.CONTENT_TYPE_FEED);
					if(feedList != null && feedList.size() > 0)
						frg.addMessageAll(feedList);
				}
				break;
			}
			
			default:
				break;
			}
			
			super.handleMessage(msg);
		}
	}	// End of class ActivityHandler
	
    /**
     * Auto-refresh Timer
     */
	private class RefreshTimerTask extends TimerTask {
		public RefreshTimerTask() {}
		
		public void run() {
			mActivityHandler.post(new Runnable() {
				public void run() {
					refreshContentObjects();
					
					mRefreshTimer = null;
				}
			});
		}
	}
	
}
