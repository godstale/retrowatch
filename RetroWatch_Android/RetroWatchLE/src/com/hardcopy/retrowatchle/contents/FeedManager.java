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

package com.hardcopy.retrowatchle.contents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hardcopy.retrowatchle.connectivity.HttpAsyncTask;
import com.hardcopy.retrowatchle.connectivity.HttpInterface;
import com.hardcopy.retrowatchle.connectivity.HttpListener;
import com.hardcopy.retrowatchle.contents.objects.CPObject;
import com.hardcopy.retrowatchle.contents.objects.FeedObject;
import com.hardcopy.retrowatchle.database.DBHelper;
import com.hardcopy.retrowatchle.utils.Constants;
import com.hardcopy.retrowatchle.utils.Logs;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;

public class FeedManager {
	
	// Global
	public static final String TAG = "FeedManager";
	
	public static final long TIME_INTERVAL_UPDATE_REQUEST = 1000;
	public static final long REMOVE_CACHE_TIME = 2*24*60*60*1000;	// Cache data erasing time
	
	public static final int DB_QUERY_LIMIT_DEFAULT = 20;
	
	private long mLastFeedInitTime = 0L;
	private long mLastUpdateRequestTime = 0L;				// To prevent duplicated update request

	private ArrayList<CPObject> mCPObjectList = new ArrayList<CPObject>();
	private ArrayList<FeedObject> mFeedList = new ArrayList<FeedObject>();

	
	// Context, system
	private Context mContext;
	private DBHelper mDB;
	private HttpListener mHTTPListener;		// To receive HTTP response result
	private IFeedListener mFeedListener;	// Send callback to		

	private FeedParser mParser;
	private DataExtractThread mThread;
	
	
	// Constructor
	public FeedManager(Context c, IFeedListener l) {
		mContext = c;
		mFeedListener = l;
		mHTTPListener = new HttpResponseListener();		
		mParser = new FeedParser();
		initialize();
	}
	
	private void initialize()
	{
		Logs.d(TAG, "# ContentManager - initializing starts here");
		
		// Make DB helper
		mDB = new DBHelper(mContext);
		mDB.openWritable();
		
		// 1. get last updated time
		SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
		mLastFeedInitTime = prefs.getLong(Constants.PREFERENCE_KEY_LAST_LAST_INIT_TIME, 0);
		
		// 2. Check preference
		boolean isFirstExec = prefs.getBoolean(Constants.PREFERENCE_KEY_IS_FIRST_EXEC, true);		// Is this first time?
		if(isFirstExec) {
			setupApplicationData();
			
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(Constants.PREFERENCE_KEY_IS_FIRST_EXEC, false);
			editor.commit();
		}
		
		// 3. Get contents from DB (and caching it)
		makeAllContentsFromDB();
		
		// 4. Start thread. 
		// This thread send HTTP requests periodically.
		restartDataExtractThread();
	}
	
	public void finalize() {
		if(mDB != null) {
			mDB.close();
			mDB = null;
		}
		stopThreads();
	}
	
	public ArrayList<CPObject> getContentProviderList() {
		return mCPObjectList;
	}
	
	public ArrayList<FeedObject> getFeedList() {
		return mFeedList;		// Use this read-only. Or could cause concurrent modification exception
	}
	
	public void deleteCachedFeed(int type) {
		synchronized(mFeedList) {
			// Delete cached feed
			if(mFeedList != null) {
				for(int i = mFeedList.size() - 1; i > -1; i--) {
					FeedObject feed = mFeedList.get(i);
					if(feed.mType == type)
						mFeedList.remove(i);
				}
			}
		}
	}
	
	/**
	 * Update content provider
	 */
	public int addContentProvider(CPObject cp, boolean updateDB) {
		if(cp == null || mCPObjectList == null || mDB == null)
			return -1;
		
		if(updateDB) {
			long idnum = mDB.insertCPItem(cp);
			if( idnum <= -1 )
				return -1;
			
			cp.mId = (int)idnum;
		}

		mCPObjectList.add(cp);
		mThread.requestUpdateAll();
		return cp.mId;
	}
	
	public void deleteContentProvider(CPObject cp, boolean updateDB) {
		if(cp == null || mCPObjectList == null || mDB == null)
			return;
		
		if(updateDB) {
			mDB.deleteCP(cp.mId);
			mDB.deleteFeedWithType(cp.mId);	// Feed use Content Provider's ID as type
		}

		mCPObjectList.remove(cp);
	}
	
	public void deleteContentProvider(int cp_id, boolean updateDB) {
		if(cp_id < 0 || mCPObjectList == null || mDB == null)
			return;
		
		if(updateDB) {
			mDB.deleteCP(cp_id);
			mDB.deleteFeedWithType(cp_id);	// Feed use Content Provider's ID as type
		}

		for(int i=mCPObjectList.size()-1; i>-1; i--) {
			CPObject cpo = mCPObjectList.get(i);
			if(cpo.mId == cp_id)
				mCPObjectList.remove(i);
		} // End of for loop
	}
	
	public void updateContentProvider(CPObject cp, boolean updateDB) {
		if(cp == null || mDB == null) 
			return;
		for(int i=mCPObjectList.size()-1; i>-1; i--) {
			CPObject temp = mCPObjectList.get(i);
			if(temp.mId == cp.mId) {
				temp.softCopy(cp);					// update cache
			}
		}
		if(updateDB)
			mDB.updateCP(cp);
	}
	
	
	public ArrayList<FeedObject> makeContentsFromDB(int type, int count)
	{
		Logs.d(TAG, "# makeContentsFromDB() starts........");
		ArrayList<FeedObject> feedList = null;
		
		Cursor cursor = null; 
		try {
			if(mDB != null) {
				cursor = mDB.selectFeed(type, ( count<1 ? DB_QUERY_LIMIT_DEFAULT : count ) );
				if(cursor != null) Logs.d(TAG, "# Query result count =" + cursor.getCount());
				else Logs.d(TAG, "# Query result count = null");
			}
		} 
		catch (IllegalStateException e) { e.printStackTrace(); }		// HTTP Task could access this query even though application is terminated.
		
		if(cursor != null && cursor.getCount() > 0) {
			feedList = extractFeedFromCursor(cursor);		// Convert cursor to ArrayList<ParsedObjectResult>
			cursor.close();
		} else {
			if( cursor != null ) cursor.close();
		}

		return feedList;
	}	// End of makeContentsFromDB(int type)
	
	public void makeAllContentsFromDB()
	{
		Logs.d(TAG, "# makeAllContentsFromDB() starts........");
		
		Cursor c = null; 
		try {
			if(mDB != null) {
				c = mDB.selectCP();
				if(c != null) Logs.d(TAG, "# Query result count =" + c.getCount());
				else Logs.d(TAG, "# Query result count = null");
			}
		} 
		catch (IllegalStateException e) { e.printStackTrace(); }		// HTTP Task could access this query even though application is terminated.
		
		ArrayList<CPObject> CPList = null;
		if(c != null && c.getCount() > 0) {
			CPList = extractCPFromCursor(c);		// Convert cursor to feedList
			c.close();
		} else {
			if( c != null ) c.close();
		}
		
		if(mCPObjectList != null && CPList != null) {
			for(int i=CPList.size()-1; i>-1; i--) {
				CPObject cpo = CPList.get(i);
				mCPObjectList.add(cpo);
			} // End of for loop
		} // End of if()
		
		
		Cursor cursor = null; 
		try {
			if(mDB != null) {
				cursor = mDB.selectFeedAll();
				if(cursor != null) Logs.d(TAG, "# Query result count =" + cursor.getCount());
				else Logs.d(TAG, "# Query result count = null");
			}
		} 
		catch (IllegalStateException e) { e.printStackTrace(); }		// HTTP Task could access this query even though application is terminated.
		
		ArrayList<FeedObject> feedList = null;
		if(cursor != null && cursor.getCount() > 0) {
			feedList = extractFeedFromCursor(cursor);		// Convert cursor to feedList
			cursor.close();
		} else {
			if( cursor != null ) cursor.close();
		}
		
		synchronized(mFeedList) {
			if(mFeedList != null && feedList != null) {
				for(int i=feedList.size()-1; i>-1; i--) {
					FeedObject pro = feedList.get(i);
					mFeedList.add(pro);
				} // End of for loop
			} // End of if()
		}
	}	// End of makeAllContentsFromDB()
	
	
	/**
	 * Thread management methods
	 */
	public boolean forcedUpdate()
	{
		long current = System.currentTimeMillis();
		if(mLastUpdateRequestTime > 0 && TIME_INTERVAL_UPDATE_REQUEST > current - mLastUpdateRequestTime) 	// Prevent duplicated request
			return false;
		
		// Set updated time as 0 to trigger update instantly
		mLastUpdateRequestTime = current;
		
		reserveUpdateAll();
		return true;
	}
	
	// Returns feed item count of selected content provider type
	public int getFeedCountFromDB(int type) {
		int result = -1;
		if(mDB != null) {
			result = mDB.getFeedCountWithType(type); 
		}
		return result; 
	}
	
	public void restartDataExtractThread() {
		if(mThread == null)	{
			mThread = new DataExtractThread();
			mThread.start();
		} else if(mThread.getThreadStatus() == DataExtractThread.THREAD_STATUS_SLEEP) {
			mThread.requestUpdateAll();
		}
	}
	
	public void reserveUpdateAll() 
	{
		mThread.requestUpdateAll();
	}
	
	public void stopThreads() 
	{
		if(mThread != null) {
			mThread.setKillSign(true);
			if(mThread.isAlive()) {
				mThread.interrupt();
			}
			mThread = null;
		}
	}
	
	
	/*****************************************************
	 *		Private methods
	 ******************************************************/
	
	private void addFeed(FeedObject feed) {
		synchronized(mFeedList) {
			mFeedList.add(feed);
		}
	}
	
	private void setUpdateStatus(boolean clearPrevAndMergeCount, int type, int parsingType, int count, String logoUrl) 
	{
		//ArrayList<CPObject> objList = mSettings.getContentProviderSettings();
		if(mCPObjectList==null) return;
		
		synchronized (mCPObjectList) 	// To prevent multi-thread collision
		{
			long current = System.currentTimeMillis();
			
			if(clearPrevAndMergeCount) {		// if true, add count
				int prevCount = 0;
				for( int index=mCPObjectList.size()-1; index >= 0; index-- ) {
					CPObject cp = mCPObjectList.get(index); 
					if(cp.mId == type) {
						prevCount = cp.mNewItemCount;
						cp.mNewItemCount = count+prevCount;
					}
				}
			}
			else {			// if false, set count
				for( int index=mCPObjectList.size()-1; index >= 0; index-- ) {
					CPObject cp = mCPObjectList.get(index); 
					if(cp.mId == type) {
						cp.mNewItemCount = count;
					}
				}
			}
			setUpdatedTime(type, current, count, logoUrl, true);
		}
	}
	
	private void setUpdatedTime(int type, long time, int newItemCount, String logoUrl, boolean updateDB) 
	{
		for(CPObject cp : mCPObjectList) {
			if(cp.mId == type) {
				cp.mLastUpdated = time;
				cp.mNewItemCount = newItemCount;
			}
		}
		
		if(updateDB && mDB != null) {
			mDB.updateLastUpdatedTime(type, time, newItemCount);
		}
	}	// End of setUpdateStatus()
	
	private void removeOldData() 
	{
		long current = System.currentTimeMillis();
		if(current - mLastFeedInitTime > REMOVE_CACHE_TIME) {
			if(current - mLastFeedInitTime < REMOVE_CACHE_TIME * 2) {
				// Do not erase DB when activity is running
				ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
				List<RunningTaskInfo> info = activityManager.getRunningTasks(20);
				
				for (Iterator<RunningTaskInfo> iterator = info.iterator(); iterator.hasNext();) {
					RunningTaskInfo runningTaskInfo = (RunningTaskInfo) iterator.next();
					String pkgName = runningTaskInfo.topActivity.getPackageName();
					String className = runningTaskInfo.topActivity.getClassName();
					if( pkgName.equals("com.tortuga.hotclip") && className.equals("com.tortuga.hotclip.HotClipMain") ) {
						return;
					}
				}
			}
			
			if(mDB == null) return;
			
			// 1. Erase DB
			mDB.deleteFeedAll();
			// 2. Erase image files
			// Utils.initFileDirectory();
			// 3. Set updated time as 0 to trigger update
			mLastFeedInitTime = current;
			
			SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putLong(Constants.PREFERENCE_KEY_LAST_LAST_INIT_TIME, mLastFeedInitTime);
			editor.commit();
		}
	}
	
	private ArrayList<CPObject> extractCPFromCursor(Cursor cursor) 
	{
		if(cursor==null) return null;
		
		ArrayList<CPObject> cpList = new ArrayList<CPObject>();

		cursor.moveToFirst();
		while(!cursor.isAfterLast())
		{
			CPObject obj = new CPObject();
			obj.mId = cursor.getInt(DBHelper.INDEX_CP_ID);				// ID column is used as Contents Provider's type value
			obj.mURL = cursor.getString(DBHelper.INDEX_CP_URL);
			obj.mName = cursor.getString(DBHelper.INDEX_CP_NAME);
			obj.mLink = cursor.getString(DBHelper.INDEX_CP_LINK);
			obj.mDescription = cursor.getString(DBHelper.INDEX_CP_DESC);
			obj.mLastBuildDate = cursor.getString(DBHelper.INDEX_CP_LAST_BUILD);
			String updated = cursor.getString(DBHelper.INDEX_CP_LAST_UPDATED);
			if(updated != null && updated.length() > 0)
				obj.mLastUpdated = Long.parseLong( updated );
			else 
				obj.mLastUpdated = 0L;
			obj.mTTL = cursor.getInt(DBHelper.INDEX_CP_TTL);
			obj.mVisible = ( cursor.getInt(DBHelper.INDEX_CP_VISIBLE) == 0 ? false : true );
			obj.mVisibleCount = cursor.getInt(DBHelper.INDEX_CP_VISIBLE_COUNT);
			obj.mCachingCount = cursor.getInt(DBHelper.INDEX_CP_CACHING_COUNT);
			obj.mCategory = cursor.getInt(DBHelper.INDEX_CP_CATEGORY);
			obj.mCategoryName = cursor.getString(DBHelper.INDEX_CP_CATEGORYNAME);
			
			obj.mBackgroundDownload = ( cursor.getInt(DBHelper.INDEX_CP_BG_DOWNLOAD) == 0 ? false : true );
			obj.mDisplayOrder = cursor.getInt(DBHelper.INDEX_CP_DISPLAY_ORDER);
			obj.mParsingType = cursor.getInt(DBHelper.INDEX_CP_PARSING_TYPE);
			obj.mSystemProperty = cursor.getInt(DBHelper.INDEX_CP_SYS_PROP);
			obj.mShowInWidget = ( cursor.getInt(DBHelper.INDEX_CP_WIDGET_ITEM) == 0 ? false : true );
			obj.mNewItemCount = cursor.getInt(DBHelper.INDEX_CP_ARG0);		// New item count
			obj.mLogoImage = cursor.getString(DBHelper.INDEX_CP_ARG2);		// Logo image
			
			cpList.add(obj);
			cursor.moveToNext();
		}
		
		return cpList;
	}
	
	private ArrayList<FeedObject> extractFeedFromCursor(Cursor cursor) 
	{
		if(cursor==null) return null;
		
		ArrayList<FeedObject> feedList = new ArrayList<FeedObject>();
		
		cursor.moveToFirst();
		while(!cursor.isAfterLast())
		{
			int requestType = cursor.getInt(DBHelper.INDEX_FEED_TYPE);
			String id = cursor.getString(DBHelper.INDEX_FEED_IDSTRING);		// WARNING: BE CAREFUL !!! (Not ID...., IDString)
			String name = cursor.getString(DBHelper.INDEX_FEED_NAME);
			String link = cursor.getString(DBHelper.INDEX_FEED_LINK);
			String keyword = cursor.getString(DBHelper.INDEX_FEED_KEYWORD);
			String content = cursor.getString(DBHelper.INDEX_FEED_CONTENT);
			String thumbnail = cursor.getString(DBHelper.INDEX_FEED_THUMBNAILURL);
			String date = cursor.getString(DBHelper.INDEX_FEED_DATE);
			int status = cursor.getInt(DBHelper.INDEX_FEED_STATUS);
			int rankUpAndDown = cursor.getInt(DBHelper.INDEX_FEED_RANK);
			int commentCount = cursor.getInt(DBHelper.INDEX_FEED_CLICK);
			int rankType = cursor.getInt(DBHelper.INDEX_FEED_ARG0);
			int version = cursor.getInt(DBHelper.INDEX_FEED_ARG1);
			String fullImage = cursor.getString(DBHelper.INDEX_FEED_ARG2);
			
			FeedObject feed = new FeedObject(requestType, id, link, keyword, content, thumbnail);
			feed.mName = name;
			feed.mDate = date;
			feed.mDownloadStatus = status;
			feed.setRankInfo(rankType, rankUpAndDown, commentCount);
			feed.setVersion(version);
			if(fullImage != null && fullImage.length() > 0)
				feed.setFullSizeImageURL(fullImage);
			
			if(id != null && id.length() > 0)	// Add object to list. Beware that adding sequence is same with recent date order
				feedList.add(feed);
			
			cursor.moveToNext();
		}
		
		return feedList;
	}
	
	
	/*****************************************************
	 *		Etc
	 ******************************************************/
	public int checkItemDBSize() {
		int count = -1;
		if(mDB == null) return count;
		try {
			count = mDB.getFeedCount();			
		} catch (IllegalStateException e) { e.printStackTrace(); }		// HTTP Task could access this query even though application is terminated.

		return count;
	}
	
	/*****************************************************
	 *		Sub classes
	 ******************************************************/
	
	//---------- Parsing result (would be sent to UI thread)
	public static final int PARSING_RESULT_OK = 1;
	public static final int PARSING_RESULT_NONE = 0;
	public static final int PARSING_RESULT_NO_RESULT_DATA = -1;
	public static final int PARSING_RESULT_PARSING_ERROR = -2;
	public static final int PARSING_RESULT_INSERT_DB_ERROR = -3;
	public static final int PARSING_RESULT_NO_NEW_ITEM = -4;
	public static final int PARSING_RESULT_CANNOT_FIND_SETTING = -5;
	
	class HttpResponseListener implements HttpListener 
	{
		@Override
		public void OnReceiveHttpResponse(int type, String strResult, int resultCode) 
		{
			int resultCodeToUI = PARSING_RESULT_OK;
			
			if(strResult != null && strResult.length() > 0 
					&& resultCode == HttpInterface.MSG_HTTP_RESULT_CODE_OK){
				// 1. Get content provider setting
				CPObject cp_obj = null;
				for(CPObject cp : mCPObjectList) {
					if(cp.mId == type)
						cp_obj = cp;
				}
				
				if(cp_obj == null) {
					resultCodeToUI = PARSING_RESULT_CANNOT_FIND_SETTING;
					Logs.d(TAG, "###### Cannot find Content Provider object...");
				} else {
					// 2. parse result string
					ArrayList<FeedObject> feedList = mParser.parseResultString(cp_obj, strResult);
						
					if(feedList != null && feedList.size() > 0) 
					{
						try {
							if(mDB != null) {
								// 3. Delete previous and insert contents to DB
								mDB.deleteFeedWithType( type );
								mDB.insertBulkItems( feedList );
								
								synchronized(mFeedList) {
									// 4. Delete previous cached
//									for(FeedObject _feed: mFeedList) {
//										if(_feed.mType == cp_obj.mId) {
//											mFeedList.remove(_feed);
//										}
//									}
									deleteCachedFeed(type);
									
									// 5. Update status
									setUpdateStatus(true, type, cp_obj.mParsingType, feedList.size(), cp_obj.mLogoImage);
									
									// 6. Add new items to cache
									mFeedList.addAll(feedList);
								}
								
								// 9. Send message to callback
								resultCodeToUI = PARSING_RESULT_OK;
								mFeedListener.OnFeedCallback(IFeedListener.MSG_FEED_UPDATED, type, PARSING_RESULT_OK, null, null, feedList);
							}
							else {
								Logs.d(TAG, "###### DBHelper is null...");
								resultCodeToUI = PARSING_RESULT_INSERT_DB_ERROR;
							}

						} catch (Exception e) {
							e.printStackTrace(); 
							resultCodeToUI = PARSING_RESULT_INSERT_DB_ERROR; 
						}
					}
					else {
						Logs.d(TAG, "###### Cannot parse result...");
						resultCodeToUI = PARSING_RESULT_PARSING_ERROR; 
					}
				}
			}
			else {
				Logs.d(TAG, "###### Result string is null. Cannot show keyword result...");
				resultCodeToUI = PARSING_RESULT_NO_RESULT_DATA;
			}


		}	// End of OnReceiveHttpRequestResult()
		
		@Override
		public void OnReceiveFileResponse(int type, String id, String filepath, String url, int resultCode) {
			// Disabled
		}
		
	}	// End of class HPHttpListener
	
	
	
	public class DataExtractThread extends Thread {
		//---------- Thread status
		public static final int THREAD_STATUS_ERROR = -1;
		public static final int THREAD_STATUS_IDLE = 0;
		public static final int THREAD_STATUS_UPDATING = 1;
		public static final int THREAD_STATUS_WAITING = 2;
		public static final int THREAD_STATUS_SLEEP = 100;
		//---------- Content refresh time
		public static final long EXTRACT_THREAD_SLEEP_TIME = 5*60*1000;
		public static final long REQUEST_INTERVAL_TIME = 500;	// Wait a moment after sending request
		public static final long THREAD_WAITING_TIME_UNIT = 1000;	// Wait a moment after sending request
		public static final int TEXT_REQUEST_MAX = 10;
		
		private int mStatus = THREAD_STATUS_IDLE;
		private boolean mKillSign = false;
		private boolean mTextRequestReceived = false;
		private long mSleepTime = 0;
		
		public DataExtractThread() {
		}

		@Override
		public void run() 
		{
			while(!Thread.interrupted())
			{
				mStatus = THREAD_STATUS_UPDATING;
				
				Logs.d(TAG, "# DataExtractThread() - loop start....");
				int tcount = getTextContents();
				// int mcount = checkAndRequestMediaDownload();		// Disabled
				
				Logs.d(TAG, "# Requested "+tcount+" contents");

				if(tcount > 0) {		// Images are downloading. Wait a moment and run again 
					mStatus = THREAD_STATUS_WAITING;
					try {
						Thread.sleep(THREAD_WAITING_TIME_UNIT);		// To prevent excessive request, sleep here.
						continue;
					} catch (InterruptedException e) {
						e.printStackTrace();
						return;
					}
				}

				mStatus = THREAD_STATUS_SLEEP;
				while(mSleepTime < EXTRACT_THREAD_SLEEP_TIME && mTextRequestReceived == false) {
					//Logs.d(TAG, "# DataExtractThread() - sleeping....");
					if(mKillSign)
						return;
					try {
						Thread.sleep(THREAD_WAITING_TIME_UNIT);
					} catch (InterruptedException e) {
						e.printStackTrace();
						return;
					}
					mSleepTime+=THREAD_WAITING_TIME_UNIT;
				}
				
				// Delete caching file and DB, once a day
				if(mSleepTime > EXTRACT_THREAD_SLEEP_TIME)
					removeOldData();
				
				mSleepTime = 0;					// Initialize sleep time.
				mTextRequestReceived = false;
				
			}	// End of while() loop
		}	// End of run()
		
		private int getTextContents()
		{
			int count = 0;
			long now = System.currentTimeMillis();
			
			Logs.d(TAG, "# ");
			Logs.d(TAG, "# getTextContents() start....");

			if(mCPObjectList == null) return count;
			
			for(int i=mCPObjectList.size()-1; i>-1; i--) 
			{
				CPObject obj = mCPObjectList.get(i);
				Logs.d(TAG, "# Time interval="+((now - obj.mLastUpdated)/1000)+", TTL="+obj.mTTL);
				Logs.d(TAG, "# ");
				
				if(now - obj.mLastUpdated > obj.mTTL * 1000)
				{
					if( requestContentsWithType(obj.mId, obj.mURL) ) {
						setUpdatedTime(obj.mId, now, obj.mNewItemCount, obj.mLogoImage, false);		// Set update time to prevent recursive request
						
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}
						count++;
						
						if(count >= TEXT_REQUEST_MAX)		// Text request limit
							return count;
					}
				}
			}
			
			return count;
		}
		
		private int Type = FeedObject.REQUEST_TYPE_NONE;
		private String URL = null;
		private boolean requestContentsWithType(int type, String requestURL)
		{

			if(requestURL == null || requestURL.length() < 1)
				return false;
			
			Type = type;
			URL = requestURL;
			Logs.d(TAG, "# HTTP Request... type = "+Type);
			
			HttpAsyncTask task = new HttpAsyncTask(mHTTPListener, type, requestURL, HttpInterface.REQUEST_TYPE_GET);
			task.execute();
			
			try {
				Thread.sleep(REQUEST_INTERVAL_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}		// To prevent excessive request
			
			return true;
		}
		
		//---------- Public methods
		public int getThreadStatus() {
			return mStatus;
		}
	
		public void requestUpdateAll(){
			mTextRequestReceived = true;
			mSleepTime = EXTRACT_THREAD_SLEEP_TIME;
		}
		
		public void setKillSign(boolean is) {
			mKillSign = is;
		}
	}	// End of class DataExtractThread
	
	private void setupApplicationData() 
	{
		Resources res = mContext.getResources();
		// Make content provider info
		CPObject temp = new CPObject();

/*
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  Constants.REQUEST_URL_DAUM_REALTIME_KEYWORDS;
		temp.mName = "DAUM realtime keywords";
		temp.mLink = Constants.CP_DAUM_URL;
		temp.mDescription = "DAUM realtime keywords";
		temp.mLastUpdated = 0L;
		temp.mTTL = 60*60;
		temp.mVisible = false;
		temp.mVisibleCount = 10;
		temp.mCachingCount = 10;
		temp.mParsingType = FeedObject.REQUEST_TYPE_DAUM_REALTIME_KEYWORDS;
		temp.mDisplayOrder = 10000;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  Constants.REQUEST_URL_NAVER_REALTIME_KEYWORDS;
		temp.mName = "Naver realtime keywords";
		temp.mLink = Constants.CP_NAVER_URL;
		temp.mDescription = "Naver realtime keywords";
		temp.mLastUpdated = 0L;
		temp.mTTL = 60*60;
		temp.mVisible = false;
		temp.mVisibleCount = 10;
		temp.mCachingCount = 10;
		temp.mParsingType = FeedObject.REQUEST_TYPE_NAVER_REALTIME_KEYWORDS;
		temp.mDisplayOrder = 10000;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  Constants.REQUEST_URL_DAUM_SOCIAL_PICK;
		temp.mName = "DAUM Social Pick";
		temp.mLink = Constants.CP_DAUM_URL;
		temp.mDescription = "DAUM Social Pick";
		temp.mLastUpdated = 0L;
		temp.mTTL = 60*60;
		temp.mVisible = false;
		temp.mVisibleCount = 30;
		temp.mCachingCount = 30;
		temp.mParsingType = FeedObject.REQUEST_TYPE_DAUM_SOCIAL_PICK;
		temp.mDisplayOrder = 10000;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  Constants.REQUEST_URL_CLIEN_IT_NEWS;
		temp.mName = "Clien IT News";
		temp.mLink = Constants.CP_CLIEN_URL;
		temp.mDescription = "Clien IT News";
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60 + 20;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_FEED43;
		temp.mDisplayOrder = 2;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "http://clien.net/cs2/img/footer_logo2.gif";
		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  Constants.REQUEST_URL_DAUM_BEST_MOVIE;
		temp.mName = "DAUM Best Movie";
		temp.mLink = Constants.CP_DAUM_MOVIE_URL;
		temp.mDescription = "DAUM Best Movie";
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60 + 30;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_FEED43;
		temp.mDisplayOrder = 3;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  Constants.REQUEST_URL_SBS_SPORTS;
		temp.mName = "SBS Sports";
		temp.mLink = Constants.CP_SBS_URL;
		temp.mDescription = "SBS Sports";
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 4;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  Constants.REQUEST_URL_SBS_CULTURE;
//		temp.mName = "SBS Culture";
//		temp.mLink = Constants.CP_SBS_URL;
//		temp.mDescription = "SBS Culture";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 60*60 + 10;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 5;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  Constants.REQUEST_URL_9GAG_HOT;
		temp.mName = "9Gag Hot";
		temp.mLink = Constants.CP_9GAG_URL;
		temp.mDescription = "9Gag Hot";
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_9GAG_HOT;
		temp.mDisplayOrder = 6;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "http://upload.wikimedia.org/wikipedia/commons/thumb/9/97/9GAG_new_logo.svg/170px-9GAG_new_logo.svg.png";
		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  Constants.REQUEST_URL_9GAG_TREND;
//		temp.mName = "9Gag trend";
//		temp.mLink = Constants.CP_9GAG_URL;
//		temp.mDescription = "9Gag trend";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60 + 10;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_9GAG_TREND;
//		temp.mDisplayOrder = 7;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "http://upload.wikimedia.org/wikipedia/commons/thumb/9/97/9GAG_new_logo.svg/170px-9GAG_new_logo.svg.png";
//		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  Constants.REQUEST_URL_DAUM_MOVIE;
//		temp.mName = "DAUM Movie";
//		temp.mLink = Constants.CP_DAUM_URL;
//		temp.mDescription = "DAUM Movie";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 60*60 + 20;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 8;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  Constants.REQUEST_URL_DAUM_CAR;
//		temp.mName = "DAUM Car";
//		temp.mLink = Constants.CP_DAUM_URL;
//		temp.mDescription = "DAUM Car";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 60*60 + 30;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 9;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  Constants.REQUEST_URL_DAUM_FOOD;
//		temp.mName = "DAUM Food";
//		temp.mLink = Constants.CP_DAUM_URL;
//		temp.mDescription = "DAUM Food";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 60*60 + 40;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 10;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  Constants.REQUEST_URL_DAUM_BOOK;
//		temp.mName = "DAUM Book";
//		temp.mLink = Constants.CP_DAUM_URL;
//		temp.mDescription = "DAUM Book";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 11;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://tistory.com/category/issue/rss";
		temp.mName = "?°ìŠ¤? ë¦¬ ?ˆê? - ?´ìŠˆ";
		temp.mLink = "http://tistory.com";
		temp.mDescription = "[?¼ë°˜] ?°ìŠ¤? ë¦¬ ?ˆê? - ?´ìŠˆ";
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 13;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://api.sbs.co.kr/xml/news/rss.jsp?pmDiv=entertainment";
//		temp.mName = "SBS NEWS ?°ì˜ˆ";
//		temp.mLink = "http://sbs.co.kr";
//		temp.mDescription = "[?°ì˜ˆ] SBS ?´ìŠ¤ - ?°ì˜ˆ";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 14;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://bbs.danawa.com/RSS/rss2.0.php?nSiteC=22";
		temp.mName = "?¤ë‚˜?€ ?´ìŠ¤ - ë¬¸í™” ?í™œ ?¨ì…˜";
		temp.mLink = "http://danawa.com";
		temp.mDescription = "[ë¬¸í™”] [?í™œ] [?¨ì…˜] ?¤ë‚˜?€ ?´ìŠ¤ ?…ë‹ˆ??";
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 15;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "http://img.danawa.com/new/newmain/img/main_logo.gif";
		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://nbiz.heraldcorp.com/common_prog/rssdisp.php?ct=010500000000.xml";
		temp.mName = "?¤ëŸ´?œê²½???¼ì´??;
		temp.mLink = "http://heraldcorp.com/";
		temp.mDescription = "[ë¬¸í™”] [?í™œ] ?¤ëŸ´?œê²½???¼ì´??;
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 16;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://blog.rss.naver.com/jheui13.xml";
//		temp.mName = "ë² ë¹„ë¡œì¦ˆ??cooking and living";
//		temp.mLink = "http://blog.rss.naver.com/jheui13.xml";
//		temp.mDescription = "[?”ë¦¬] ë² ë¹„ë¡œì¦ˆ??cooking and living";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 12*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 17;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://tistory.com/category/travel/rss";
		temp.mName = "?°ìŠ¤? ë¦¬ ?ˆê? - ?¬í–‰";
		temp.mLink = "http://tistory.com";
		temp.mDescription = "[?¬í–‰] ?°ìŠ¤? ë¦¬ ?ˆê? - ?¬í–‰";
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 18;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://tistory.com/category/photo/rss";
//		temp.mName = "?°ìŠ¤? ë¦¬ ?ˆê? - ?¬ì§„";
//		temp.mLink = "http://tistory.com";
//		temp.mDescription = "[?¬ì§„] ?°ìŠ¤? ë¦¬ ?ˆê? - ?¬ì§„";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 19;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://bbs.danawa.com/RSS/rss2.0.php?nSiteC=6";
//		temp.mName = "?¤ë‚˜?€ ?´ìŠ¤ - ê²Œìž„";
//		temp.mLink = "http://danawa.com";
//		temp.mDescription = "[ê²Œìž„] ?¤ë‚˜?€ ?´ìŠ¤ ?…ë‹ˆ??";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 20;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://blog.rss.naver.com/knockya.xml";
//		temp.mName = "ë§›ì°¾?¬ì˜ ë§›ì§‘ ?´ì•¼ê¸?;
//		temp.mLink = "http://naver.com";
//		temp.mDescription = "[ë§›ì§‘] ë§›ì°¾?¬ì˜ ë§›ì§‘ ?´ì•¼ê¸?;
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 12*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 21;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://olpost.com/rss/?type=cate_top&value=&period=today&target";
		temp.mName = "?¬í¬?¤íŠ¸ ì¢…í•© - ???œê° ?œì„ ì§‘ì¤‘";
		temp.mLink = "http://olpost.com/";
		temp.mDescription = "[?¼ë°˜] ?¬í¬?¤íŠ¸ ì¢…í•© - ???œê° ?œì„ ì§‘ì¤‘";
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 22;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://khan.feedsportal.com/c/34755/f/640291/index.rss";
//		temp.mName = "ê²½í–¥? ë¬¸ ?¤í¬ì¸?;
//		temp.mLink = "http://khan.feedsportal.com/";
//		temp.mDescription = "[?¤í¬ì¸? ê²½í–¥? ë¬¸ RSS ?œë¹„??| ?¤í¬ì¸?;
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 23;
//		temp.mSystemProperty = 1;
//		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://feeds.nationalgeographic.com/ng/photography/photo-of-the-day/";
		temp.mName = "National Geographic Photo of the Day";
		temp.mLink = "http://nationalgeographic.com";
		temp.mDescription = "National Geographic Photo of the Day";
		temp.mLastUpdated = 0L;
		temp.mTTL = 12*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 24;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "http://metaversemodsquad.com/wp-content/uploads/2013/03/national-geographic-projects-logo.jpg";
		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://gdata.youtube.com/feeds/base/standardfeeds/KR/most_viewed?client=ytapi-youtube-browse&alt=rss&time=today";
		temp.mName = "? íŠœë¸??¸ê¸° ë¹„ë””??;
		temp.mLink = "http://youtube.com";
		temp.mDescription = "[?™ì˜?? [?¼ë°˜] Most Popular video";
		temp.mLastUpdated = 0L;
		temp.mTTL = 4*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 26;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://zdnetkorea.feedsportal.com/c/34249/f/622753/index.rss";
//		temp.mName = "ì§€?”ë„·ì½”ë¦¬??- ê²Œìž„";
//		temp.mLink = "http://zdnetkorea.feedsportal.com/";
//		temp.mDescription = "[ê²Œìž„] ZDNet Korea - ê²Œìž„";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 27;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://zdnetkorea.feedsportal.com/c/34249/f/622759/index.rss";
		temp.mName = "ì§€?”ë„·ì½”ë¦¬??- ?¸í„°??;
		temp.mLink = "http://zdnetkorea.feedsportal.com";
		temp.mDescription = "[IT] ZDNet Korea - ?¸í„°??;
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 28;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://rss.kbench.com/kbench.xml";
//		temp.mName = "ì¼€?´ë²¤ì¹??„ì²´ê¸°ì‚¬";
//		temp.mLink = "http://rss.kbench.com/kbench.xml";
//		temp.mDescription = "[?´ìŠ¤] [IT] ì¼€?´ë²¤ì¹??„ì²´ê¸°ì‚¬";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 30;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://feeds.feedburner.com/Bloter";
		temp.mName = "Bloter.net";
		temp.mLink = "http://bloter.net";
		temp.mDescription = "[IT] [?´ìŠ¤] Bloter.net ë¸”ë¡œ?°ë‹·??;
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 31;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "http://www.bloter.net/wp-content/themes/bloterv3/images/site/bloter_logo.jpg";
		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://feeds.feedburner.com/DesignlogOfMaru?format=xml";
		temp.mName = "?”ìž?¸ë¡œê·?;
		temp.mLink = "http://feeds.feedburner.com/DesignlogOfMaru?format=xml";
		temp.mDescription = "[?”ìž?? Design resources, tutorials, tips and Digital trend news";
		temp.mLastUpdated = 0L;
		temp.mTTL = 6*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 32;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://homedesigning.tumblr.com/rss";
//		temp.mName = "Home Designing";
//		temp.mLink = "http://homedesigning.tumblr.com/rss";
//		temp.mDescription = "[?”ìž?? Interior Design + Architecture + Random Awesome Things";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 33;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://valley.egloos.com/theme/rss/recent/movie";
//		temp.mName = "?´ê?ë£¨ìŠ¤ ?í™” ?Œë§ˆ ìµœê·¼ê¸€";
//		temp.mLink = "http://egloos.com";
//		temp.mDescription = "[?í™”] ?´ê?ë£¨ìŠ¤ ?í™” ?Œë§ˆ ìµœê·¼ê¸€";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 34;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "http://md.egloos.com/img/www/main_v2011/ico_egloos_logo.gif";
//		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://v.daum.net/best/rss";
//		temp.mName = "Daum view ê¸€ ë² ìŠ¤??;
//		temp.mLink = "http://daum.net";
//		temp.mDescription = "[?¼ë°˜] ?¤ìŒ view ê¸€ ë² ìŠ¤??;
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 35;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "http://icon.daumcdn.net/w/c/12/11/10192021148946703.png";
//		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://valley.egloos.com/theme/rss/recent/book";
//		temp.mName = "?´ê?ë£¨ìŠ¤ ?„ì„œ ?Œë§ˆ ìµœê·¼ê¸€";
//		temp.mLink = "http://egloos.com";
//		temp.mDescription = "[ì±? ?´ê?ë£¨ìŠ¤ ?„ì„œ ?Œë§ˆ ìµœê·¼ê¸€";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 36;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "http://md.egloos.com/img/www/main_v2011/ico_egloos_logo.gif";
//		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://feeds.feedburner.com/ideakeyword?format=xml";
		temp.mName = "?„ì´?”ì–´ ë°•ë¬¼ê´€";
		temp.mLink = "http://feeds.feedburner.com/ideakeyword?format=xml";
		temp.mDescription = "[ê´‘ê³ ] [?”ìž?? ë§¤ì¼ ë§¤ì¼ ?ë‡Œ??? ì„ ???œë ¥??;
		temp.mLastUpdated = 0L;
		temp.mTTL = 6*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 38;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://valley.egloos.com/theme/rss/recent/history";
//		temp.mName = "?´ê?ë£¨ìŠ¤ ??‚¬ ?Œë§ˆ ìµœê·¼ê¸€";
//		temp.mLink = "http://egloos.com";
//		temp.mDescription = "[ë¬¸í™”] ?´ê?ë£¨ìŠ¤ ??‚¬ ?Œë§ˆ ìµœê·¼ê¸€";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 39;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "http://md.egloos.com/img/www/main_v2011/ico_egloos_logo.gif";
//		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://valley.egloos.com/theme/rss/recent/performance";
		temp.mName = "?´ê?ë£¨ìŠ¤ ê³µì—° ?„ì‹œ ?Œë§ˆ ìµœê·¼ê¸€";
		temp.mLink = "http://egloos.com";
		temp.mDescription = "[ë¬¸í™”] ?´ê?ë£¨ìŠ¤ ê³µì—° ?„ì‹œ ?Œë§ˆ ìµœê·¼ê¸€";
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 40;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "http://md.egloos.com/img/www/main_v2011/ico_egloos_logo.gif";
		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://valley.egloos.com/theme/rss/recent/animation";
//		temp.mName = "?´ê?ë£¨ìŠ¤ ? ë‹ˆë©”ì´???Œë§ˆ ìµœê·¼ê¸€";
//		temp.mLink = "http://egloos.com";
//		temp.mDescription = "[ë¬¸í™”] ?´ê?ë£¨ìŠ¤ ? ë‹ˆë©”ì´???Œë§ˆ ìµœê·¼ê¸€";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 41;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "http://md.egloos.com/img/www/main_v2011/ico_egloos_logo.gif";
//		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://valley.egloos.com/theme/rss/recent/movie";
		temp.mName = "?´ê?ë£¨ìŠ¤ ?í™” ?Œë§ˆ ìµœê·¼ê¸€";
		temp.mLink = "http://egloos.com";
		temp.mDescription = "[?í™”] ?´ê?ë£¨ìŠ¤ ?í™” ?Œë§ˆ ìµœê·¼ê¸€";
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 42;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "http://md.egloos.com/img/www/main_v2011/ico_egloos_logo.gif";
		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://valley.egloos.com/theme/rss/recent/travel";
//		temp.mName = "?´ê?ë£¨ìŠ¤ ?¬í–‰ ?Œë§ˆ ìµœê·¼ê¸€";
//		temp.mLink = "http://egloos.com";
//		temp.mDescription = "[?¬í–‰] ?´ê?ë£¨ìŠ¤ ?¬í–‰ ?Œë§ˆ ìµœê·¼ê¸€";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 43;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "http://md.egloos.com/img/www/main_v2011/ico_egloos_logo.gif";
//		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://extmovie.com/zbxe/?mid=shockimg&act=rss";
//		temp.mName = "?¬ë? ?½ê¸°?¬ì§„";
//		temp.mLink = "http://extmovie.com/zbxe/?mid=shockimg&act=rss";
//		temp.mDescription = "[? ë¨¸] ?¬ë? ?½ê¸°?¬ì§„";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 44;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://media.daum.net/syndication/culture.rss";
		temp.mName = "ë¯¸ë””?´ë‹¤??- ë¬¸í™”/?í™œTop RSS";
		temp.mLink = "http://daum.net";
		temp.mDescription = "[ë¬¸í™”] [?í™œ] ë¯¸ë””?´ë‹¤??- ë¬¸í™”/?í™œTop RSS";
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 45;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "http://icon.daumcdn.net/w/c/12/11/10192021148946703.png";
		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://www.issuebriefing.com/?feed=rss2";
		temp.mName = "?¼ê°„ì§€ ?´ìŠ¤ ?”ì•½ ?•ë¦¬";
		temp.mLink = "http://www.issuebriefing.com";
		temp.mDescription = "[?´ìŠ¤] ?˜ë£¨ ??ë²??…ë°?´íŠ¸, ?˜ë£¨ ?´ìŠ¤ë¥??”ì•½ ?œê³µ";
		temp.mLastUpdated = 0L;
		temp.mTTL = 12*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 47;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://feeds.feedburner.com/terzeron/cstory?format=xml";
		temp.mName = "?¤ì´ë²„ì˜??- ì»¤ë²„?¤í† ë¦?;
		temp.mLink = "http://naver.com";
		temp.mDescription = "[?í™”] ?¤ì´ë²„ì˜??- ì»¤ë²„?¤í† ë¦?;
		temp.mLastUpdated = 0L;
		temp.mTTL = 4*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 48;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "http://sstatic.naver.net/search/img3/h1_naver.gif";
		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://www.oneaday.co.kr/rss.php";
		temp.mName = "?˜ë£¨???œê?ì§€ë§??ë§¤ ?ì–´?°ì´";
		temp.mLink = "http://www.oneaday.co.kr";
		temp.mDescription = "[?¼í•‘] ?˜ë£¨???œê?ì§€ë§??ë§¤ ?ì–´?°ì´ ?¼í•‘";
		temp.mLastUpdated = 0L;
		temp.mTTL = 12*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 49;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "http://cdn.oneaday.co.kr/share/images/common/logo_1.jpg";
		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://feeds.feedburner.com/onedays";
//		temp.mName = "êµ¬ë£¨??ì§€ë¦„ë„?°ë?";
//		temp.mLink = "http://feeds.feedburner.com/onedays";
//		temp.mDescription = "[?¼í•‘] ?˜ë£¨???œê?ì§€ ?¼í•‘ëª??œëˆˆ??ë³´ê¸°";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 12*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 50;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://imnews.imbc.com/rss/news/news_00.xml";
		temp.mName = "MBC?´ìŠ¤ :: ?´ìŠ¤(?„ì²´)";
		temp.mLink = "http://imbc.com";
		temp.mDescription = "[?´ìŠ¤] MBC?´ìŠ¤ :: ?´ìŠ¤(?„ì²´)";
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 51;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "http://img.imbc.com/2012_main/images/logo_m.jpg";
		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://feeds.feedburner.com/naver_news_popular";
//		temp.mName = "?¤ì´ë²?ê°€??ë§Žì´ ë³??´ìŠ¤";
//		temp.mLink = "http://naver.com";
//		temp.mDescription = "[?´ìŠ¤] ?¤ì´ë²?ê°€??ë§Žì´ ë³??´ìŠ¤";
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 2*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 52;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "http://sstatic.naver.net/search/img3/h1_naver.gif";
//		mDB.insertCPItem(temp);
		
//		temp.mId = 0;			// will be reset by DB
//		temp.mURL =  "http://blog.rss.naver.com/yummycook.xml";
//		temp.mName = "?”ë¦¬ì²œì‚¬???‰ë³µë°¥ìƒ";
//		temp.mLink = "http://blog.rss.naver.com/yummycook.xml";
//		temp.mDescription = "[?”ë¦¬] ?”ë¦¬ê°€ ?¸ìƒ??ë°ê²Œ ?˜ëŠ” ê²ƒì„ ë¯¿ê³  ?”ë¦¬?˜ëŠ” ì¦ê±°?€???? ë¹ ì¡Œ?µë‹ˆ??;
//		temp.mLastUpdated = 0L;
//		temp.mTTL = 12*60*60;
//		temp.mVisible = true;
//		temp.mVisibleCount = 20;
//		temp.mCachingCount = 20;
//		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
//		temp.mDisplayOrder = 53;
//		temp.mSystemProperty = 1;
//		temp.mLogoImage = "";
//		mDB.insertCPItem(temp);
		
		temp.mId = 0;			// will be reset by DB
		temp.mURL =  "http://zdnetkorea.feedsportal.com/c/34249/f/622758/index.rss";
		temp.mName = "ì§€?”ë„·ì½”ë¦¬??- ??ëª¨ë°”??;
		temp.mLink = "http://zdnetkorea.feedsportal.com/c/34249/f/622758/index.rss";
		temp.mDescription = "[IT] ZDNet Korea - ??ëª¨ë°”??;
		temp.mLastUpdated = 0L;
		temp.mTTL = 2*60*60;
		temp.mVisible = true;
		temp.mVisibleCount = 20;
		temp.mCachingCount = 20;
		temp.mParsingType = FeedObject.REQUEST_TYPE_RSS_DEFAULT;
		temp.mDisplayOrder = 54;
		temp.mSystemProperty = 1;
		temp.mLogoImage = "";
		mDB.insertCPItem(temp);
		
*/
		
	}
	
}

