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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.hardcopy.retrowatchle.utils.Logs;
import com.hardcopy.retrowatchle.utils.Utils;

import android.os.AsyncTask;


public class HttpFileAsyncTask extends AsyncTask<Void, Integer, String> implements HttpInterface {
	// Global variables
	public static final String tag = "HttpFileAsyncTask";
	
	private int mType;
	private String mID = null;
	private String mURL = null;
	private String mDir = null;
	private String mFileName = null;
	private int mResultStatus = MSG_HTTP_RESULT_CODE_OK;
	
	private static final int CONNECTION_TIMEOUT = 5000;
	
	// Context, system
	private HttpListener mListener;
	

	// Constructor
	public HttpFileAsyncTask(HttpListener l, int type, String id, String url, String directory, String filename) {
		mListener = l;
		mType = type;			// Not used in async task. will be used in callback
		mID = id;				// Not used in async task. will be used in callback
		mURL = url;
		mDir = directory;
		mFileName = filename;
	}

	
	protected String doInBackground(Void... unused) 
	{
		if(mListener==null || mID==null || mURL==null || mDir==null || mFileName==null) { 
			//Logs.d(tag, "###### Error!!! : Parameter is null. Check parameter");
			return ""; 
		}
					
		URL url = null;
		try {
			url = new URL(mURL);
		} 
		catch (MalformedURLException e1) { 
			e1.printStackTrace();
			mResultStatus = MSG_HTTP_RESULT_CODE_ERROR_REQUEST_EXCEPTION;
			Logs.d(tag, "# URL = "+url);
			//Logs.d(tag, "###### Error!!! : MalformedURLException ");
			return null;
		}
		
		if(Utils.checkFileExists(mDir, mFileName))
			return null;
		String filePathAndName = new String(mDir+"/"+mFileName);

		try {
			// Set up connection
			HttpURLConnection conn= (HttpURLConnection)url.openConnection(); 
			conn.setConnectTimeout(CONNECTION_TIMEOUT);
			conn.setReadTimeout(CONNECTION_TIMEOUT);
			conn.connect(); 
			
			// Copy input stream (one is for calculate bitmap size and another is for decode bitmap)
			InputStream inputStream = conn.getInputStream();
			
			// Make file and output stream
			File file = new File(filePathAndName);
			OutputStream outStream = new FileOutputStream(file);
			
			byte[] buf = new byte[1024];
			int len = 0;
			
			while ((len = inputStream.read(buf)) > 0) {
				outStream.write(buf, 0, len);
			}
			outStream.close();
			inputStream.close();
 			
			mResultStatus = MSG_HTTP_RESULT_CODE_OK;
 			
		} catch (Exception e) {
			mResultStatus = MSG_HTTP_RESULT_CODE_ERROR_UNKNOWN;
			//Logs.d(tag, "###### Error!!! : Cannot download file... ");
			e.printStackTrace();
			return null; 
		}

		return filePathAndName;
	}

	protected void onProgressUpdate(Integer... progress) {
		// TODO: set progress percentage
		// This code runs on UI thread
	}

	protected void onPostExecute(String filename) {
		// This code runs on UI thread
		mListener.OnReceiveFileResponse(mType, mID, filename, mURL, mResultStatus);
	}
	
}
