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

package com.hardcopy.retrowatchle.connectivity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.hardcopy.retrowatchle.utils.Logs;

import android.os.AsyncTask;


public class HttpAsyncTask extends AsyncTask<Void, Integer, String> implements HttpInterface
{
	// Global variables
	public static final String tag = "HttpAsyncTask";
	
//	private Map<String, String> mMap;	// Disabled
	private int mType;
	private String mURL = null;
	private int mResultStatus = MSG_HTTP_RESULT_CODE_OK;
	private int mRequestType = REQUEST_TYPE_GET;
	
	// Context, system
	private HttpListener mListener;
	
	// Constructor
	public HttpAsyncTask(HttpListener listener, int type, String url, int requestType) {
		mListener = listener;
		mType = type;		// Not used in async task. will be used in callback
		mURL = url;
		mRequestType = requestType;
	}
	

	protected String doInBackground(Void... unused) 
	{
		Logs.d(tag, "###### HttpAsyncTask :: Starting HTTP request task ");
		String resultString = null;
		HttpRequester httpRequester = new HttpRequester();
		
		if(mListener==null || mURL==null) { 
			Logs.d(tag, "###### Error!!! : mListener==null or mURL==null ");
			return null; 
		} else {
			Logs.d(tag, "###### Request URL = "+mURL);
		}
		
		URL url = null;
		try {
			url = new URL(mURL);
		} 
		catch (MalformedURLException e1) { 
			e1.printStackTrace();
			mResultStatus = MSG_HTTP_RESULT_CODE_ERROR_REQUEST_EXCEPTION;
			Logs.d(tag, "###### Error!!! : MalformedURLException ");
			return "";
		}
		
		// Determine request type
		String reqType = null;
		if(mRequestType == REQUEST_TYPE_GET)
			reqType = REQUEST_TYPE_GET_STRING;
		else if(mRequestType == REQUEST_TYPE_POST)
			reqType = REQUEST_TYPE_POST_STRING;
		else
			reqType = REQUEST_TYPE_GET_STRING;
		
		// TODO: Manually set response encoding type.
		// Some page doesn't support UTF-8
		String encType = null;
		if(true) {
			encType = ENCODING_TYPE_UTF_8;
		} 
//		else if(  ) {
//			encType = ENCODING_TYPE_EUC_KR;
//		}
		
		// Request
		try {
			resultString = httpRequester.request(url, encType, reqType, null);
			// publishProgress(int);
		} catch (IOException e) { 
			e.printStackTrace();
			mResultStatus = MSG_HTTP_RESULT_CODE_ERROR_REQUEST_EXCEPTION;
			Logs.d(tag, "###### Error!!! : HttpRequester makes IOException ");
			return "";
		}

		// Check result string
		if(resultString == null || resultString.length() < 1) {
			mResultStatus = MSG_HTTP_RESULT_CODE_ERROR_UNKNOWN;
			Logs.d(tag, "###### Error!!! : resultString - invalid result ");
			return "";
		}
		
		mResultStatus = MSG_HTTP_RESULT_CODE_OK;
		return resultString;
	}

	protected void onProgressUpdate(Integer... progress) {
		// TODO: set progress percentage
		// This code runs on UI thread
	}

	protected void onPostExecute(String result) {
		// This code runs on UI thread
		if(mListener != null) {
			mListener.OnReceiveHttpResponse(mType, result, mResultStatus);
		}
	}

	
}
