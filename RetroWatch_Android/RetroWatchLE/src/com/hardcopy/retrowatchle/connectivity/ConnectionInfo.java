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

import com.hardcopy.retrowatchle.utils.Constants;

import android.content.Context;
import android.content.SharedPreferences;

public class ConnectionInfo {
	
	// Instance
	private static ConnectionInfo mInstance = null;
	
	private Context mContext;
	
	// Target device's MAC address
	private String mDeviceAddress = null;
	// Name of the connected device
	private String mDeviceName = null; 
	
	
	private ConnectionInfo(Context c) {
		mContext = c;
		
		SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
		mDeviceAddress = prefs.getString(Constants.PREFERENCE_CONN_INFO_ADDRESS, null);
		mDeviceName = prefs.getString(Constants.PREFERENCE_CONN_INFO_NAME, null);
	}
	
	public synchronized static ConnectionInfo getInstance(Context c) {
		if(mInstance == null) {
			if(c != null)
				mInstance = new ConnectionInfo(c);
			else
				return null;
		}
		return mInstance;
	}
	
	public void resetConnectionInfo() {
		mDeviceAddress = null;
		mDeviceName = null;
	}
	
	public String getDeviceName() {
		return mDeviceName;
	}
	
	public void setDeviceName(String name) {
		mDeviceName = name;
		
		// At this time, connection is established successfully.
		// Save connection info in shared preference.
		SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(Constants.PREFERENCE_CONN_INFO_ADDRESS, mDeviceAddress);
		editor.putString(Constants.PREFERENCE_CONN_INFO_NAME, mDeviceName);
		editor.commit();
	}
	
	public String getDeviceAddress() {
		return mDeviceAddress;
	}
	
	public void setDeviceAddress(String address) {
		mDeviceAddress = address;
	}
	
}
