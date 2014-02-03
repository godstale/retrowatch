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

import com.hardcopy.retrowatchle.utils.Logs;


public class FeedObject {
	
	private static final String TAG = "FeedObject";
	
	//---------- Parsing type (Regular request)
	public static final int REQUEST_TYPE_NONE = -1;
	
	public static final int REQUEST_TYPE_DAUM_REALTIME_KEYWORDS = 5;
	public static final int REQUEST_TYPE_DAUM_SOCIAL_PICK = 6;
	
	public static final int REQUEST_TYPE_NAVER_REALTIME_KEYWORDS = 11;
	public static final int REQUEST_TYPE_NAVER_RELATED_KEYWORDS = 12;

	public static final int REQUEST_TYPE_TWITTER_HOTTEST = 22;
	public static final int REQUEST_TYPE_TWITTER_REALTIME = 23;
	public static final int REQUEST_TYPE_TWITTER_TODAY = 24;
	public static final int REQUEST_TYPE_TWITTER_IMAGE = 25;

	public static final int REQUEST_TYPE_9GAG_HOT = 71;
	public static final int REQUEST_TYPE_9GAG_TREND = 72;
	
	public static final int REQUEST_TYPE_IMAGE = 101;				// File download
	public static final int REQUEST_TYPE_LOGO_IMAGE = 102;
	public static final int REQUEST_TYPE_9GAG_FULL_IMAGE_FOR_DIALOG = 111;	
	
	public static final int REQUEST_TYPE_NOTICE = 301;				// Notice
	
	public static final int REQUEST_TYPE_RSS_DEFAULT = 1001;		// RSS
	public static final int REQUEST_TYPE_RSS_FEED43 = 1101;			// RSS - feed43
	
	//---------- Rank parameter
	public static final int RANK_TYPE_NONE = 0;
	public static final int RANK_TYPE_NEW = 1;
	
	//---------- Content downloading status
	public static final int CONTENT_DOWNLOAD_STATUS_ERROR = -1;
	public static final int CONTENT_DOWNLOAD_STATUS_INIT = 0;
	public static final int CONTENT_DOWNLOAD_STATUS_DOWNLOADING = 1;
	public static final int CONTENT_DOWNLOAD_STATUS_DONE = 100;
	
	public String mId;			// 
	public int mType = REQUEST_TYPE_NONE;	// What kind of feed?
	public String mName = null;
	public String mLink;
	public String mKeyword;
	public String mContent;
	public String mThumbnailUrl;
	public String mDate;
	public int mDownloadStatus;
	
	public int mRankType = RANK_TYPE_NONE;	// for real-time keyword ranking
	public int mRankUpAndDown = 0;			// -10~0~10
	public int mCommentCount = 0;			// 0~10 		(1 means 1,000 comments)
	public int mVersion = 0;
	
	public String mFullSizeImageURL = null;
	
	// Constructor
	public FeedObject(int type, String id, String link, String keyword, String content, String thumbnail) {
		mType = type;
		mId = id;
		mLink = link;
		mKeyword = keyword;
		mContent = content;
		mThumbnailUrl = thumbnail;
		mDownloadStatus = CONTENT_DOWNLOAD_STATUS_INIT;
	}
	
	public void setName(String name) {
		mName = name;
	}
	
	public void setDate(String date) {
		mDate = date;
	}
	
	public void setDownloadStatus(int status) {
		mDownloadStatus = status;
	}
	
	public void setRankInfo(int rankType, int rankUpDown, int commentCount) {
		mRankType = rankType;
		mRankUpAndDown = rankUpDown;
		mCommentCount = commentCount;
	}
	
	public void setFullSizeImageURL(String url) {
		mFullSizeImageURL = url;
	}
	
	public void setVersion(int ver) {
		mVersion = ver;
	}
	
	public void printLog() {
		Logs.d(TAG, "[+] mType = " + mType);
		Logs.d(TAG, "[+] mId = " + mId);
	}
}
