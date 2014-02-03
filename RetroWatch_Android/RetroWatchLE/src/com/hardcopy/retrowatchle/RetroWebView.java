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

import java.net.URISyntaxException;

import org.apache.http.util.EncodingUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class RetroWebView {
	// Global variables
	private static final String TAG = "RetroWebView";
	
	public static final String URLPrefix = "http://";
	
	// Context, System
	private Context mContext = null;
	private IWebViewListener mWebViewListener = null;
	private InputMethodManager mIMM = null;

	// Layout
	private WebView mWebView = null;
	private ProgressBar mProgressLoading = null;
	private EditText mEditURL = null;
	
	private String mMsgWait = null;

	// WebView components
	private WebSettings mWebViewSettings = null;
	private RetroWebViewClient mWebViewClient = null;
	private RetroChromeClient mChromeClient = null;
	
	
	/*****************************************************
	 *		Initializing methods
	 ******************************************************/
	public void setParametersForInit(WebView webview, ProgressBar p, EditText e){
		mWebView = webview;
		mProgressLoading = p;
		mEditURL = e;
	}
	
	public void clearHistory() {
		mWebView.clearHistory();
	}
	
	public void initialize(Context c, IWebViewListener l) {
		mContext = c;
		mWebViewListener = l;
		mIMM = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
		
		//-----------------------------------------------------------------------------
		// Initialize layout
		//-----------------------------------------------------------------------------
		mEditURL.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_GO) {
					actionLoadURL(null);
					// InputMethodManager.HIDE_IMPLICIT_ONLY as the second parameter to ensure 
					// you only hide the keyboard when the user didn't explicitly force it to appear (by holding down menu)
					mIMM.hideSoftInputFromWindow(mEditURL.getWindowToken(), 0);
				}
				return true;
			}
		});

		mMsgWait = mContext.getString(R.string.warning_loading_wait);
				
		//-----------------------------------------------------------------------------
		// Create Web-View and set clients
		//-----------------------------------------------------------------------------
		mWebViewClient = new RetroWebViewClient();
		mChromeClient = new RetroChromeClient();
		mWebView.setWebViewClient(mWebViewClient);
		mWebView.setWebChromeClient(mChromeClient);
		
		
		// Set Web-View features
		mWebViewSettings = mWebView.getSettings();
		mWebViewSettings.setJavaScriptEnabled(true);
		mWebView.addJavascriptInterface(new AndroidBridge(), "hotclip");	// param 2 : JavaScript use this parameter
																			// ex) javascript call: function getResult() { window.hotclip.setResult('ERROR'); }

		mWebViewSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		mWebViewSettings.setBuiltInZoomControls(true);
		mWebViewSettings.setLoadWithOverviewMode(true);
		mWebViewSettings.setUseWideViewPort(true);
		//mWebViewSettings.setDatabaseEnabled(true);
		mWebViewSettings.setDomStorageEnabled(true);

		// Set cache size to 8 mb by default. should be more than enough
//		mWebViewSettings.setAppCacheMaxSize(1024*1024*8);
		 
		// Enable HTML5 Application Cache
//		String appCachePath = mContext.getCacheDir().getAbsolutePath();
//		mWebViewSettings.setAppCachePath(appCachePath);
//		mWebViewSettings.setAllowFileAccess(true);
//		mWebViewSettings.setAppCacheEnabled(true);
//		mWebViewSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
		
//		GeoClient geo = new GeoClient();
//		webview.setWebChromeClient(geo);
//		mWebViewSettings.setGeolocationEnabled(true);
//		mWebViewSettings.setGeolocationDatabasePath(<path>);
		
	}	// End of initialize()
	
	public void finalize() {
		//clearApplicationCache(null);			// Clear all cache files
	} 
	
	
	/*****************************************************
	 *		Public methods
	 ******************************************************/
	public void actionLoadURL(String inputURL) 
	{
		String url;
		if(inputURL == null || inputURL.length() < 1 || inputURL.equalsIgnoreCase(URLPrefix)) {
			url = mEditURL.getText().toString().trim();
		} else {
			inputURL = inputURL.trim();
			url = inputURL;
		}
		
		if(!url.contains(URLPrefix)) {
			url = URLPrefix + url;
		}

		try {
			mWebView.loadUrl(url);
			// if you want to set HTML code directly
			// mWebView.loadData("<html><body>Hello, world!</body></html>", "text/html", "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void actionBack() {
		if(mWebView.canGoBack()) {
			mWebView.goBack();
		} else {
			
		}
	}
	
	public WebView getWebViewInstance() {
		return mWebView;
	}
	
	/*****************************************************
	 *		Private methods
	 ******************************************************/
	private void updateEditText(String str) {
		mEditURL.setText(str);
		mIMM.hideSoftInputFromWindow(mEditURL.getWindowToken(), 0);
	}
	
	// postdata : "param1=value1&param2=value2..."
	private void actionLoadWithPost(String url, String postData) {
		mWebView.postUrl(url, EncodingUtils.getBytes(postData, "BASE64"));
	}
	
	private void startLoading() {
		// Set progress bar
		mProgressLoading.setVisibility(View.VISIBLE);
		mProgressLoading.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 15));
		
		updateEditText(mMsgWait);					// Set loading message in URL box
	}
	
	private void setOnLoading(int progress) {
		mProgressLoading.setProgress(progress);
	}
	
	private void endLoading() {
		mProgressLoading.setVisibility(View.INVISIBLE);
		mProgressLoading.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
	}
	
	// Delete all cache files
	private void clearApplicationCache(java.io.File dir){
		if(dir==null)
			dir = mContext.getCacheDir();
		
		if(dir==null)
		    return;

		java.io.File[] children = dir.listFiles();
		try{
			for(int i=0;i<children.length;i++) {
				if(children[i].isDirectory())
					clearApplicationCache(children[i]);
				else children[i].delete();
			}
		}
		catch(Exception e){}
    }
	
	
	/*****************************************************
	 *		Etc
	 ******************************************************/
	
	
	/*****************************************************
	 *		Sub classes
	 ******************************************************/
	class RetroChromeClient extends WebChromeClient {
		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			// Page loading progress
			if(newProgress < 100) {
				setOnLoading(newProgress);
			} else {
				// If you want to call JS function while loading, use below code 
				// When page loading finished, call JS function like javascript:[method]
				//view.loadUrl("javascript:getResult");
				endLoading();
			}
		}
		
		@Override
		public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
			Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
			result.confirm();
			return true;
		}
	}	// End of class HPChromeClient
 
	
	class RetroWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			
			startLoading();				// Set loading screen or tasks

			//---------------------------------------------------------------------------------------------------
			// Connect RTSP, Call number and Email link to local application
			//---------------------------------------------------------------------------------------------------
			String origin_url = url;
			String temp_url = origin_url.substring(origin_url.length() - 3, origin_url.length());

			if (temp_url.endsWith("mp4")) {
				// Handle media file
				try {
					Intent i = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
					mContext.startActivity(i);
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} else if (origin_url.startsWith("tel:")) {
				// Call link
				Intent call_phone = new Intent(Intent.ACTION_VIEW, Uri.parse(origin_url));
				// Recursively call current activity with URL
				mContext.startActivity(call_phone);
				return true;
				
			} else if (origin_url.startsWith("mailto:")) {
				// Email link
				String email = origin_url.replace("mailto:", "");
				final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
				intent.setType("plain/text");
				intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { email });
				intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Title");
				intent.putExtra(android.content.Intent.EXTRA_TEXT, "Message");
				mContext.startActivity(Intent.createChooser(intent, "Send email"));

			} else {
				view.loadUrl(url);
			}

			return true;
		}	// End of shouldOverrideUrlLoading()
 
		@Override
		public void onPageFinished(WebView view, String url) {
			// RetroWebView.this.setTitle(view.getTitle());			// Get current page's title
			updateEditText(url);
			
			super.onPageFinished(view, url);
		}
	}	// End of class HPWebViewClient
	
	
	/*******************************************
	 * Below methods will be connected with Javascript function
	 *******************************************/	
	public class AndroidBridge 
	{
		private final Handler mBridgeHandler = new Handler();			// Android bridge needs a Handler 
		
		// After Android 4.2(JellyBean MR1) release, 
		// interface method must use @JavascriptInterface prefix
		
		@JavascriptInterface
		public void addNewFeed(final String title, final String desc, final String url)	{	// parameters must be final 
			//----- Runnable instance.
			JSRunnable r = new JSRunnable(null, IWebViewListener.WEBVIEW_CALLBACK_ADD_FEED, 0, title, desc, url, null);
			
			//----- Send runnable to handler
			mBridgeHandler.post(r);
		}
	}
	
	private class JSRunnable implements Runnable {
		private String ID = null;
		private int TYPE = 0;
		private int ARG0 = 0;
		private String ARG1 = null;
		private String ARG2 = null;
		private String ARG3 = null;
		private String ARG4 = null;
		
		@Override
		public void run() {
			mWebViewListener.OnReceiveCallback(ID, TYPE,
					ARG0,
					ARG1,		// Title 
					ARG2, 		// Desc
					ARG3, 		// URL
					ARG4);
		}
		
		public JSRunnable(String id, int type, int arg0, String arg1, String arg2, String arg3, String arg4) {
			ID = id;
			TYPE = type;
			ARG0 = arg0; 
			ARG1 = arg1;
			ARG2 = arg2;
			ARG3 = arg3;
			ARG4 = arg4;
		}
	}
}
