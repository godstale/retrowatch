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

import android.util.Log;

public class Logs {
	
	private static final String TAG = "RetroWatch";
	public static boolean mIsEnabled = true;
	
	
	public static void v(String msg) {
		if(mIsEnabled) {
			Log.v(TAG, msg);
		}
	}
	
	public static void v(String tag, String msg) {
		if(mIsEnabled) {
			Log.v(tag, msg);
		}
	}
	
	public static void d(String msg) {
		if(mIsEnabled) {
			Log.d(TAG, msg);
		}
	}
	
	public static void d(String tag, String msg) {
		if(mIsEnabled) {
			Log.d(tag, msg);
		}
	}
	
	public static void e(String msg) {
		if(mIsEnabled) {
			Log.e(TAG, msg);
		}
	}
	
	public static void e(String tag, String msg) {
		if(mIsEnabled) {
			Log.e(tag, msg);
		}
	}
	
	public static void i(String msg) {
		if(mIsEnabled) {
			Log.e(TAG, msg);
		}
	}
	
	public static void i(String tag, String msg) {
		if(mIsEnabled) {
			Log.e(tag, msg);
		}
	}
	
}
