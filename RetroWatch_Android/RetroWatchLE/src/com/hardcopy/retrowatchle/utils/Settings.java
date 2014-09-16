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

package com.hardcopy.retrowatchle.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
	
	private static Settings mSettings = null;
	
	private Context mContext;
	
	
	
	public synchronized static Settings getInstance(Context c) {
		if(mSettings == null) {
			mSettings = new Settings(c);
		}
		return mSettings;
	}
	
	public Settings(Context c) {
		if(mContext == null) {
			mContext = c;
			initialize();
		}
	}
	
	
	private void initialize() {
	}
	
	
	public synchronized void finalize() {
		mContext = null;
		mSettings = null;
	}
	
	public synchronized void setGmailAddress(String addr) {
		if(addr != null && !addr.isEmpty()) {
			SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(Constants.PREFERENCE_KEY_GMAIL_ADDRESS, addr);
			editor.commit();
		}
	}
	
	public synchronized String getGmailAddress() {
		SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
		return prefs.getString(Constants.PREFERENCE_KEY_GMAIL_ADDRESS, null);
	}
	
	public synchronized void setRunInBackground(boolean isTrue) {
		SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.PREFERENCE_KEY_RUN_IN_BG, isTrue);
		editor.commit();
	}
	
	public synchronized boolean getRunInBackground() {
		SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
		return prefs.getBoolean(Constants.PREFERENCE_KEY_RUN_IN_BG, false);
	}
	
	
}
