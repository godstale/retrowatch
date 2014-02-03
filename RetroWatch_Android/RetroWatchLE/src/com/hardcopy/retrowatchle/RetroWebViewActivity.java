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

import com.hardcopy.retrowatchle.contents.objects.CPObject;
import com.hardcopy.retrowatchle.service.RetroWatchService;
import com.hardcopy.retrowatchle.utils.RecycleUtils;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class RetroWebViewActivity extends Activity implements OnClickListener {
	
	// Global
	public static final String tag = "RetroWebViewActivity";
	
	// Context, System
	private Context mContext;
	private RetroWatchService mService;
	
	private IWebViewListener mWebViewListener;
	
	// Layout
	//----- Web view
	private RetroWebView mWebView;
	private ProgressBar mProgressLoading = null;
	private EditText mEditURL = null;
	private Button mBtnGoURL = null;
	
	
	/*****************************************************
	*		Overrided methods
	******************************************************/
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		//----- System, Context
		mContext = getApplicationContext();
		mWebViewListener = new WebViewListenerImpl();
		
		doBindService();
		
		//----- Layout
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		setContentView(R.layout.activity_web_view);
		
		mProgressLoading = (ProgressBar)this.findViewById(R.id.progress_loading);
		mEditURL = (EditText) this.findViewById(R.id.edit_url);
		mBtnGoURL = (Button) this.findViewById(R.id.btn_go_url);
		mBtnGoURL.setOnClickListener(this);
		
		mWebView = new RetroWebView();
		WebView webview = (WebView) this.findViewById(R.id.layout_webview);
		mWebView.setParametersForInit(webview, mProgressLoading, mEditURL);	// This method must be called first
		mWebView.initialize(mContext, mWebViewListener);
		
		//----- Initialize data, settings
		Intent i = getIntent();
		Uri uri = i.getData();
		String url = uri.toString();
		if(url != null && !url.isEmpty()) {
			openURL(url);
		}
		clearHistory();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// getMenuInflater().inflate(R.menu.activity_wev_view, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		finalizeActivity();
	}
	
	@Override
	public void onLowMemory (){		// onDestroy is not always called when applications are finished by Android system.
		super.onLowMemory();
		finalizeActivity();
	}
	
	
	private void openURL(String url) {
		mWebView.actionLoadURL(url);
	}
	
	private void clearHistory() {
		mWebView.clearHistory();
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.btn_go_url:
			openURL("");
			break;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if(mWebView.getWebViewInstance().canGoBack()) {
				mWebView.getWebViewInstance().goBack();
			} else {
				onBackPressed();
			}
			return true;
		}
		return false;
	}
	
	
	/*****************************************************
	*		Private methods
	******************************************************/
	public void goToMainActivity() {
		Intent i = new Intent(this, RetroWatchActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(i);
		finish();
	}	
	
	private void finalizeActivity() {
		doStopService();
		if(mWebView != null) {
			mWebView.finalize();
			mWebView = null;
		}
		RecycleUtils.recursiveRecycle(getWindow().getDecorView());
	}
	
	private void doBindService() {
		bindService( new Intent( this , RetroWatchService.class ), mServiceConn, Context.BIND_AUTO_CREATE);
	}
	
	private void doStopService() {
		unbindService(mServiceConn);
	}
	
	private static final int TOAST_DISPLAY_TIME = 2000;
	private boolean insertCPSetting(CPObject cp) {
		if(cp == null || mService == null) 
			return false;

		int _id = mService.addRss(cp);
		if(_id > -1) {
			Toast.makeText(mContext, "1 " + mContext.getResources().getString(R.string.msg_rss_update_success), TOAST_DISPLAY_TIME).show();
		} else {
			Toast.makeText(mContext, "1 " + mContext.getResources().getString(R.string.warning_error_while_adding_rss), TOAST_DISPLAY_TIME).show();
			return false;
		}
		return true;
	}
	
	
	/*****************************************************
	*		Private classes
	******************************************************/
	
	//---------------------------------------------------------------------
	//	Service connection
	//---------------------------------------------------------------------
	private ServiceConnection mServiceConn = new ServiceConnection() 
	{
		public void onServiceConnected(ComponentName className, IBinder binder) {
			mService = ((RetroWatchService.RetroWatchServiceBinder) binder).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};
	
	//---------------------------------------------------------------------
	//	Listeners
	//---------------------------------------------------------------------
	class WebViewListenerImpl implements IWebViewListener 
	{
		public static final int SETTINGS_FEED_DEFAULT_VISIBLE_COUNT = 20;
		public static final int SETTINGS_FEED_DEFAULT_REFRESH_TIME = 60;			// in minutes
		public static final int SETTINGS_FEED_MAX_TITLE_LEN = 256;
		public static final int SETTINGS_FEED_MAX_DESC_LEN = 256;
		public static final int SETTINGS_FEED_MAX_URL_LEN = 1024;
		
		@Override
		public void OnReceiveDialogEvent(int type, int arg0, String arg1, String arg2) 
		{
			switch(type)
			{
				default:
					break;
			}
		}	// End of OnReceiveMediaResult()
		
		@Override
		public void OnReceiveCallback(String id, int type, int arg0, String arg1, String arg2, String arg3, String arg4)
		{
			switch(type)
			{
				case IWebViewListener.WEBVIEW_CALLBACK_ADD_FEED:
					if(arg1==null || arg2==null || arg3==null) return;
					if(arg1.length() > SETTINGS_FEED_MAX_TITLE_LEN) arg1 = arg1.substring(0, SETTINGS_FEED_MAX_TITLE_LEN);
					if(arg2.length() > SETTINGS_FEED_MAX_DESC_LEN) arg1 = arg1.substring(0, SETTINGS_FEED_MAX_DESC_LEN);
					if(arg3.length() > SETTINGS_FEED_MAX_URL_LEN) arg1 = arg1.substring(0, SETTINGS_FEED_MAX_URL_LEN);
					
					CPObject cp = new CPObject();
					cp.mName = arg1;
					cp.mDescription = arg2;
					cp.mURL = arg3;
					cp.mVisibleCount = 20;
					cp.mCachingCount = 30;
					cp.mTTL = 2*60*60;
					
					// Insert to DB and mSettings
					insertCPSetting(cp);
					break;
				default:
					break;
			}
		}
		
	}	// End of class CallbackListener
	
	
	
	
	
	
}
