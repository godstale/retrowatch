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


public class CPObject {
	public int mId = 0;
	public String mURL = null;			// RSS request URL
	
	public String mName = null;
	public String mLink = null;
	public String mDescription = null;
	public String mLastBuildDate = null;
	public long mLastUpdated = 0L;	// in milli-second
	public int mTTL = 60*60;		// Update interval. Times in second
	
	public boolean mVisible = true;
	public int mVisibleCount = 10;
	public int mCachingCount = 20;
	public int mCategory = 0;
	public String mCategoryName = null;
	public boolean mBackgroundDownload = false;
	
	public int mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;		// This is same type with FeedObject mType
	public int mDisplayOrder = -1;
	public int mSystemProperty = -1;
	public boolean mShowInWidget = false;
	
	public int mNewItemCount = 0;
	public String mLogoImage = null;
	
	public void softCopy(CPObject cp) {
		this.mURL = cp.mURL;
		this.mName = cp.mName;
		this.mLink = cp.mLink;
		this.mDescription = cp.mDescription;
		this.mLastBuildDate = cp.mLastBuildDate;
		this.mLastUpdated = cp.mLastUpdated;
		this.mTTL = cp.mTTL;
		this.mVisible = cp.mVisible;
		this.mVisibleCount = cp.mVisibleCount;
		this.mCachingCount = cp.mCachingCount;
		this.mCategory = cp.mCategory;
		this.mCategoryName = cp.mCategoryName;
		this.mBackgroundDownload = cp.mBackgroundDownload;
		this.mParsingType = cp.mParsingType;
		this.mDisplayOrder = cp.mDisplayOrder;
		this.mSystemProperty = cp.mSystemProperty;
		this.mShowInWidget = cp.mShowInWidget;
		this.mNewItemCount = cp.mNewItemCount;
		this.mLogoImage = cp.mLogoImage;
	}
	
	public void copyTo(CPObject cp) {
		cp.mId = this.mId;
		cp.mURL = this.mURL;
		cp.mName = this.mName;
		cp.mLink = this.mLink;
		cp.mDescription = this.mDescription;
		cp.mLastBuildDate = this.mLastBuildDate;
		cp.mLastUpdated = this.mLastUpdated;
		cp.mTTL = this.mTTL;
		cp.mVisible = this.mVisible;
		cp.mVisibleCount = this.mVisibleCount;
		cp.mCachingCount = this.mCachingCount;
		cp.mCategory = this.mCategory;
		cp.mCategoryName = this.mCategoryName;
		cp.mBackgroundDownload = this.mBackgroundDownload;
		cp.mParsingType = this.mParsingType;
		cp.mDisplayOrder = this.mDisplayOrder;
		cp.mSystemProperty = this.mSystemProperty;
		cp.mShowInWidget = this.mShowInWidget;
		cp.mNewItemCount = this.mNewItemCount;
		cp.mLogoImage = this.mLogoImage;
	}
}
