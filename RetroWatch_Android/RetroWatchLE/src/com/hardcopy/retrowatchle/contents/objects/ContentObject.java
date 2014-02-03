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

package com.hardcopy.retrowatchle.contents.objects;

public class ContentObject {
	
	public static final int CONTENT_TYPE_NOTIFICATION = 1;
	public static final int CONTENT_TYPE_MESSAGING = 2;
	public static final int CONTENT_TYPE_EMERGENCY = 3;
	public static final int CONTENT_TYPE_FEED = 4;
	
	public static final int MESSAGING_ID_GMAIL = 1;
	public static final int MESSAGING_ID_SMS = 2;
	
	public static final int EMERGENCY_ID_WIFI = 1;
	public static final int EMERGENCY_ID_CALL_STATE = 2;
	public static final int EMERGENCY_ID_BATT_STATE = 3;
	public static final int EMERGENCY_ID_RF_STATE = 4;
	
	public static final int ICON_TYPE_NORMAL_MESSAGE = 0;
	public static final int ICON_TYPE_EMAIL = 28;
	public static final int ICON_TYPE_CALL = 10;
	public static final int ICON_TYPE_EMERGENCY = 60;
	public static final int ICON_TYPE_BATT_FULL = 4;
	public static final int ICON_TYPE_BATT_MEDIUM = 5;
	public static final int ICON_TYPE_BATT_LOW = 6;
	public static final int ICON_TYPE_RF_STATE = 64;
	public static final int ICON_TYPE_RSS = 46;
	
	public static final String SMS_PACKAGE_NAME = "android.telephony.sms";
	public static final String GMAIL_PACKAGE_NAME = "com.google.android.gm";
	public static final String TELEPHONY_CALL_PACKAGE_NAME = "android.telephony.call";
	public static final String TELEPHONY_RF_PACKAGE_NAME = "android.telephony.rfstatus";
	public static final String FEED_PACKAGE_NAME = "com.hardcopy.feed";
	public static final String WIFI_PACKAGE_NAME = "android.net.wifi";
	public static final String BATT_PACKAGE_NAME = "android.os.battery";
	
	public int mContentType;			// Content type
	public int mIconType;				// Icon type
	public int mId;						// ID
	public boolean mIsEnabled = false;	// Watch shows enabled items only
	public String mOriginalString;		// Original string
	public String mFilteredString;		// String to be shown on remote device
	public String mExtraData = null;	// Extra data defined by each content type
	public String mPackageName = null;
	
	
	public ContentObject(int type, int id, String strOrigin, String strFiltered) {
		mContentType = type;
		mIconType = -1;
		mId = id;
		mOriginalString = strOrigin;
		mFilteredString = strFiltered;
	}
	
}
