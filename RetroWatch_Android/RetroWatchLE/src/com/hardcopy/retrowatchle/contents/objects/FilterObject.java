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

public class FilterObject {
	
	public static final int FILTER_TYPE_NONE = 0;			// This means that filter is not available
	public static final int FILTER_TYPE_ALL = 1;			// Every item should pass this filter
	public static final int FILTER_TYPE_NOTIFICATION = 2;
	public static final int FILTER_TYPE_MESSAGING = 3;
	public static final int FILTER_TYPE_FEED = 4;
	public static final int FILTER_TYPE_EMERGENCY = 5;
	public static final int FILTER_TYPE_PACKAGE_NAME = 6;
	
	public static final int MATCHING_TYPE_NONE = 0;			// This means that matching type is not available
	public static final int MATCHING_TYPE_WHOLE_WORD = 1;
	public static final int MATCHING_TYPE_START_WITH = 2;
	public static final int MATCHING_TYPE_END_WITH = 3;
	public static final int MATCHING_TYPE_PART_OF = 4;
	
	public static final int REPLACE_TYPE_NONE = 0;		// This means that replace type is not available
	public static final int REPLACE_TYPE_ALL = 1;		// replace all
	public static final int REPLACE_TYPE_SAME_PART = 2;	// replace only same part
	
	public static final int ICON_TYPE_NONE_CODE = 0xf0;
	
	
	public int mId = -1;					// ID : matching with database primary key
	public int mType = FILTER_TYPE_NONE;			// Filter type : defines target object
	public int mIconType = 0;
	public int mCompareType = MATCHING_TYPE_NONE;	// Matching conditions: whole word, start with, end with, part of
	public int mReplaceType = REPLACE_TYPE_NONE;	// Replace methods : Replace all, Replace same part only
	public String mOriginalString = null;	// Original string
	public String mReplaceString = null;	// String to be shown on remote device
	
	
	public void copyTo(FilterObject target) {
		target.mId = this.mId;
		target.mType = this.mType;
		target.mIconType = this.mIconType;
		target.mCompareType = this.mCompareType;
		target.mReplaceType = this.mReplaceType;
		target.mOriginalString = new String(this.mOriginalString);
		target.mReplaceString = new String(this.mReplaceString);
	}
	
}
