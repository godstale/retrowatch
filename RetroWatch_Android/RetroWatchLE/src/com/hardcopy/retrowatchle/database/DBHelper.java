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

package com.hardcopy.retrowatchle.database;

import java.util.ArrayList;

import com.hardcopy.retrowatchle.connectivity.HttpInterface;
import com.hardcopy.retrowatchle.contents.objects.CPObject;
import com.hardcopy.retrowatchle.contents.objects.FeedObject;
import com.hardcopy.retrowatchle.contents.objects.FilterObject;
import com.hardcopy.retrowatchle.utils.Logs;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DBHelper {
	
	private static final String TAG  ="DBHelper";
	
	private static final int DATABASE_VERSION = 4;
	public static final String DATABASE_NAME = "retrowatch";

	//----------- Filters table parameters
	public static final String TABLE_NAME_FILTERS = "filters";
	
	public static final String KEY_FILTER_ID = "_id";				// int		primary key, auto increment
	public static final String KEY_FILTER_TYPE = "type";			// int		not null
	public static final String KEY_FILTER_ICON_TYPE = "icontype";			// int
	public static final String KEY_FILTER_MATCHING = "matching";	// int		not null
	public static final String KEY_FILTER_REPLACE_TYPE = "replacetype";	// int		not null
	public static final String KEY_FILTER_ORIGINAL = "original";	// original string
	public static final String KEY_FILTER_REPLACE = "replace";		// replace string
	public static final String KEY_FILTER_ARG0 = "arg0";		// int
	public static final String KEY_FILTER_ARG1 = "arg1";		// int 
	public static final String KEY_FILTER_ARG2 = "arg2";		// string
	public static final String KEY_FILTER_ARG3 = "arg3";		// string
	
	public static final int INDEX_FILTER_ID = 0;			// int
	public static final int INDEX_FILTER_TYPE = 1;			// int
	public static final int INDEX_FILTER_ICON_TYPE = 2;			// int
	public static final int INDEX_FILTER_MATCHING = 3;		// int
	public static final int INDEX_FILTER_REPLACE_TYPE = 4;		// int
	public static final int INDEX_FILTER_ORIGINAL = 5;		// string
	public static final int INDEX_FILTER_REPLACE = 6;		// string
	public static final int INDEX_FILTER_ARG0 = 7;		// int
	public static final int INDEX_FILTER_ARG1 = 8;		// int 
	public static final int INDEX_FILTER_ARG2 = 9;		// string
	public static final int INDEX_FILTER_ARG3 = 10;		// string
	
	private static final String DATABASE_CREATE_FILTERS = "CREATE TABLE " +TABLE_NAME_FILTERS+ "("
													+ KEY_FILTER_ID +" Integer primary key autoincrement, "
													+ KEY_FILTER_TYPE + " Integer not null, "
													+ KEY_FILTER_ICON_TYPE + " Integer, "
													+ KEY_FILTER_MATCHING + " Integer not null, "
													+ KEY_FILTER_REPLACE_TYPE + " Integer not null, "
													+ KEY_FILTER_ORIGINAL + " Text, "
													+ KEY_FILTER_REPLACE + " Text, "
													+ KEY_FILTER_ARG0 + " integer, "
													+ KEY_FILTER_ARG1 + " integer, "
													+ KEY_FILTER_ARG2 + " Text, "
													+ KEY_FILTER_ARG3 + " Text"
													+ ")";
	private static final String DATABASE_DROP_FILTERS_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME_FILTERS;
	//----------- End of Filters table parameters

	//----------- Feed Item table
	public static final String TABLE_NAME_FEED_ITEM = "feed_item";
	
	public static final String KEY_FEED_ID = "id";				// int		primary key, auto increment
	public static final String KEY_FEED_TYPE = "type";		// int			not null
	public static final String KEY_FEED_STATUS = "status";			// int		not null
	public static final String KEY_FEED_IDSTRING = "idstring";		// string	not null
	public static final String KEY_FEED_NAME = "name";		// string
	public static final String KEY_FEED_LINK = "link";					// string
	public static final String KEY_FEED_KEYWORD = "keyword";	// string
	public static final String KEY_FEED_CONTENT = "content";		// string
	public static final String KEY_FEED_THUMBNAILURL = "thumbnailurl";	// string
	public static final String KEY_FEED_FILE_TYPE = "filetype";
	public static final String KEY_FEED_FILE_URL = "fileurl";
	public static final String KEY_FEED_RANK = "rank";		// string
	public static final String KEY_FEED_CLICK = "click";		// string
	public static final String KEY_FEED_LIKE = "like";			// int
	public static final String KEY_FEED_DATE = "date";			// int
	public static final String KEY_FEED_ARG0 = "arg0";		// int
	public static final String KEY_FEED_ARG1 = "arg1";		// int 
	public static final String KEY_FEED_ARG2 = "arg2";		// string
	public static final String KEY_FEED_ARG3 = "arg3";		// string
	
	public static final int INDEX_FEED_ID = 0;				// int
	public static final int INDEX_FEED_TYPE = 1;		// int			not null
	public static final int INDEX_FEED_STATUS = 2;			// int		not null
	public static final int INDEX_FEED_IDSTRING = 3;		// string	not null
	public static final int INDEX_FEED_NAME = 4;		// string
	public static final int INDEX_FEED_LINK = 5;					// string
	public static final int INDEX_FEED_KEYWORD = 6;	// string
	public static final int INDEX_FEED_CONTENT = 7;		// string
	public static final int INDEX_FEED_THUMBNAILURL = 8;	// string
	public static final int INDEX_FEED_FILE_TYPE = 9;
	public static final int INDEX_FEED_FILE_URL = 10;
	public static final int INDEX_FEED_RANK = 11;		// string
	public static final int INDEX_FEED_CLICK = 12;		// string
	public static final int INDEX_FEED_LIKE = 13;			// int
	public static final int INDEX_FEED_DATE = 14;			// int
	public static final int INDEX_FEED_ARG0 = 15;		// int
	public static final int INDEX_FEED_ARG1 = 16;		// int 
	public static final int INDEX_FEED_ARG2 = 17;		// string
	public static final int INDEX_FEED_ARG3 = 18;		// string
	
	private static final String DATABASE_CREATE_FEED = "CREATE TABLE " +TABLE_NAME_FEED_ITEM+ "("
													+ KEY_FEED_ID +" Integer primary key autoincrement, "
													+ KEY_FEED_TYPE + " Integer not null, "
													+ KEY_FEED_STATUS + " Integer not null, "
													+ KEY_FEED_IDSTRING + " Text not null, "
													+ KEY_FEED_NAME + " Text, "
													+ KEY_FEED_LINK + " Text, "
													+ KEY_FEED_KEYWORD + " Text, "
													+ KEY_FEED_CONTENT + " Text, "
													+ KEY_FEED_THUMBNAILURL + " Text, "
													+ KEY_FEED_FILE_TYPE + " Integer, "
													+ KEY_FEED_FILE_URL + " Text, "
													+ KEY_FEED_RANK + " Integer, "
													+ KEY_FEED_CLICK + " Integer, "
													+ KEY_FEED_LIKE + " Integer, "	// Every contents uses this as <Priority>. ContentManager thread updates this using Real-time keyword matching.
													+ KEY_FEED_DATE + " Text,"
													+ KEY_FEED_ARG0 + " integer, " 	// Keyword result uses this field as <Rank type>
													+ KEY_FEED_ARG1 + " integer, "	// Notice uses this as <version info>
													+ KEY_FEED_ARG2 + " Text, "		// 9 Gag object uses this field as <Full size image url>
													+ KEY_FEED_ARG3 + " Text"
													+ ")";
	private static final String DATABASE_DROP_FEED_ITEM_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME_FEED_ITEM;
	//----------- End of Feed table parameters
	
	//----------- Content Provider table
	public static final String TABLE_NAME_CONTENT_PROVIDER = "content_provider";
	
	public static final String KEY_CP_ID = "id";							// int		primary key, auto increment
	public static final String KEY_CP_URL = "url";						// String		not null
	public static final String KEY_CP_NAME = "name";				// String		not null
	public static final String KEY_CP_LINK = "link";					// string
	public static final String KEY_CP_DESC = "desc";				// string
	public static final String KEY_CP_LAST_BUILD = "lastbuild";				// string
	public static final String KEY_CP_LAST_UPDATED = "lastupdated";	// string
	public static final String KEY_CP_TTL = "ttl";				// int
	public static final String KEY_CP_VISIBLE = "visible";	// int
	public static final String KEY_CP_VISIBLE_COUNT = "visiblecount";		// int
	public static final String KEY_CP_CACHING_COUNT = "cachingcount";	// int
	public static final String KEY_CP_CATEGORY = "category";						// int
	public static final String KEY_CP_CATEGORYNAME = "categoryname";	// string
	public static final String KEY_CP_BG_DOWNLOAD = "bgdownload";						// int
	public static final String KEY_CP_DISPLAY_ORDER = "displayorder";						// int
	public static final String KEY_CP_PARSING_TYPE = "parsingtype";						// int
	public static final String KEY_CP_SYS_PROP = "sysprop";						// int
	public static final String KEY_CP_WIDGET_ITEM = "widget";						// int
	public static final String KEY_CP_ARG0 = "arg0";		// int	
	public static final String KEY_CP_ARG1 = "arg1";		// int 
	public static final String KEY_CP_ARG2 = "arg2";		// string
	public static final String KEY_CP_ARG3 = "arg3";		// string
	
	public static final int INDEX_CP_ID = 0;					// int
	public static final int INDEX_CP_URL  = 1;				// String
	public static final int INDEX_CP_NAME = 2;			// String
	public static final int INDEX_CP_LINK = 3;				// string
	public static final int INDEX_CP_DESC = 4;				// string
	public static final int INDEX_CP_LAST_BUILD = 5;			// string
	public static final int INDEX_CP_LAST_UPDATED = 6;		// string
	public static final int INDEX_CP_TTL = 7;						// int
	public static final int INDEX_CP_VISIBLE = 8;					// int
	public static final int INDEX_CP_VISIBLE_COUNT = 9;	// int
	public static final int INDEX_CP_CACHING_COUNT = 10;	// int
	public static final int INDEX_CP_CATEGORY = 11;			// int
	public static final int INDEX_CP_CATEGORYNAME = 12;	// string
	public static final int INDEX_CP_BG_DOWNLOAD = 13;			// int
	public static final int INDEX_CP_DISPLAY_ORDER = 14;			// int
	public static final int INDEX_CP_PARSING_TYPE = 15;			// int
	public static final int INDEX_CP_SYS_PROP = 16;			// int
	public static final int INDEX_CP_WIDGET_ITEM = 17;			// int
	public static final int INDEX_CP_ARG0 = 18;		// int	
	public static final int INDEX_CP_ARG1 = 19;		// int 
	public static final int INDEX_CP_ARG2 = 20;		// string
	public static final int INDEX_CP_ARG3 = 21;		// string
	
	private static final String DATABASE_CREATE_CP = "CREATE TABLE " +TABLE_NAME_CONTENT_PROVIDER+ "("
													+ KEY_CP_ID +" Integer primary key autoincrement, "
													+ KEY_CP_URL + " Text, "
													+ KEY_CP_NAME + " Text not null, "
													+ KEY_CP_LINK + " Text, "
													+ KEY_CP_DESC + " Text, "
													+ KEY_CP_LAST_BUILD + " Text, "
													+ KEY_CP_LAST_UPDATED + " Text, "
													+ KEY_CP_TTL + " Integer, "
													+ KEY_CP_VISIBLE + " Integer, "
													+ KEY_CP_VISIBLE_COUNT + " Integer, "
													+ KEY_CP_CACHING_COUNT + " Integer, "
													+ KEY_CP_CATEGORY + " Integer, "
													+ KEY_CP_CATEGORYNAME + " Text, "
													+ KEY_CP_BG_DOWNLOAD + " Integer, "
													+ KEY_CP_DISPLAY_ORDER + " Integer, "
													+ KEY_CP_PARSING_TYPE + " Integer, "
													+ KEY_CP_SYS_PROP + " Integer, "
													+ KEY_CP_WIDGET_ITEM + " Integer, "
													+ KEY_CP_ARG0 + " integer, "						// New item count
													+ KEY_CP_ARG1 + " integer, "
													+ KEY_CP_ARG2 + " Text, "			// Logo image URL
													+ KEY_CP_ARG3 + " Text"
													+ ")";
	private static final String DATABASE_DROP_CONTENT_PROVIDER_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME_CONTENT_PROVIDER;
	//----------- End of Content provider table parameters
	
	
	// Context, System
	private final Context mContext;
	private SQLiteDatabase mDb;
	private DatabaseHelper mDbHelper;
	
	// Constructor
	public DBHelper(Context context) {
		this.mContext = context;
	}
	
	
	//----------------------------------------------------------------------------------
	// Public classes
	//----------------------------------------------------------------------------------
	// DB open (Writable)
	public DBHelper openWritable() throws SQLException {
		mDbHelper = new DatabaseHelper(mContext);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}
	
	// DB open (Readable)
	public DBHelper openReadable() throws SQLException {
		mDbHelper = new DatabaseHelper(mContext);
		mDb = mDbHelper.getReadableDatabase();
		return this;
	}
	
	// Terminate DB
	public void close() {
		if(mDb != null) {
			mDb.close();
			mDb = null;
		}
		if(mDbHelper != null) {
			mDbHelper.close();
			mDbHelper = null;
		}
	}
	
	//----------------------------------------------------------------------------------
	// INSERT
	//----------------------------------------------------------------------------------
	public long insertFilter(FilterObject filter) throws SQLiteConstraintException 
	{
		if(filter.mType < 0 || filter.mCompareType < 0 || filter.mReplaceType < 0
				|| filter.mOriginalString == null || filter.mOriginalString.length() < 1)
			return -1;
		
		ContentValues insertValues = new ContentValues();
		insertValues.put(KEY_FILTER_TYPE, filter.mType);
		insertValues.put(KEY_FILTER_ICON_TYPE, filter.mIconType);
		insertValues.put(KEY_FILTER_MATCHING, filter.mCompareType);
		insertValues.put(KEY_FILTER_REPLACE_TYPE, filter.mReplaceType);
		insertValues.put(KEY_FILTER_ORIGINAL, filter.mOriginalString);
		insertValues.put(KEY_FILTER_REPLACE, filter.mReplaceString);
//		insertValues.put(KEY_FILTER_ARG0, 0);		// for future use
//		insertValues.put(KEY_FILTER_ARG1, 0);
//		insertValues.put(KEY_FILTER_ARG2, "");
//		insertValues.put(KEY_FILTER_ARG3, "");
		
		Logs.d(TAG, "+ Insert filter: type="+filter.mType+", icon="+filter.mIconType
				+", compare="+filter.mCompareType+", replace type"+filter.mReplaceType
				+", original="+filter.mOriginalString+", replace="+filter.mReplaceString);
		
		synchronized (mDb) {
			if(mDb == null) 
				return -1;
			return mDb.insertOrThrow(TABLE_NAME_FILTERS, null, insertValues);
		}
	}
	
	public long insertFeedItem(FeedObject feed) throws SQLiteConstraintException 
	{
		if(feed.mType <= FeedObject.REQUEST_TYPE_NONE || feed.mDownloadStatus <= HttpInterface.REQUESTER_STATUS_ERROR 
				|| feed.mId==null || feed.mId.length() < 1)
			return -1;
		
		Logs.d(TAG, "# insert new content item : type = "+feed.mType);
		
		boolean isDataExist = false;
		ContentValues insertInitialValues = new ContentValues();
		insertInitialValues.put(KEY_FEED_TYPE, feed.mType);
		insertInitialValues.put(KEY_FEED_STATUS, feed.mDownloadStatus);
		insertInitialValues.put(KEY_FEED_IDSTRING, feed.mId);
		if(feed.mName!= null && feed.mName.length()>0) {
			insertInitialValues.put(KEY_FEED_NAME, feed.mName);
		}
		if(feed.mLink!=null && feed.mLink.length()>0) {
			insertInitialValues.put(KEY_FEED_LINK, feed.mLink);
			isDataExist= true;
		}
		if(feed.mKeyword!=null && feed.mKeyword.length()>0) {
			insertInitialValues.put(KEY_FEED_KEYWORD, feed.mKeyword);
			isDataExist= true;
		}
		if(feed.mContent!=null && feed.mContent.length()>0) {
			insertInitialValues.put(KEY_FEED_CONTENT, feed.mContent);
			isDataExist= true;
		}
		if(feed.mThumbnailUrl!=null && feed.mThumbnailUrl.length()>0) {
			insertInitialValues.put(KEY_FEED_THUMBNAILURL, feed.mThumbnailUrl);
			isDataExist= true;
		}
		insertInitialValues.put(KEY_FEED_DATE, feed.mDate);
		insertInitialValues.put(KEY_FEED_RANK, feed.mRankUpAndDown);
		insertInitialValues.put(KEY_FEED_CLICK, feed.mCommentCount);
		insertInitialValues.put(KEY_FEED_ARG0, feed.mRankType);
		insertInitialValues.put(KEY_FEED_ARG1, feed.mVersion);
		insertInitialValues.put(KEY_FEED_ARG2, feed.mFullSizeImageURL);
		
		synchronized (mDb) {
			if(isDataExist == false) return -1;
			if(mDb == null) return -1;
			
			return mDb.insertOrThrow(TABLE_NAME_FEED_ITEM, null, insertInitialValues);
		}
	}
	
	public long insertCPItem(CPObject cp) throws SQLiteConstraintException 
	{
		if(cp.mName==null || cp.mName.length() < 1)
			return -1;
		
		Logs.d(TAG, "# insert new content provider : type = "+cp.mParsingType);
		
		boolean isDataExist = false;
		ContentValues insertInitialValues = new ContentValues();
		if(cp.mURL!= null && cp.mURL.length()>0) {
			insertInitialValues.put(KEY_CP_URL, cp.mURL);
		}
		if(cp.mName!= null && cp.mName.length()>0) {
			insertInitialValues.put(KEY_CP_NAME, cp.mName);
			isDataExist= true;
		}
		if(cp.mLink!=null && cp.mLink.length()>0) {
			insertInitialValues.put(KEY_CP_LINK, cp.mLink);
		}
		if(cp.mDescription!=null && cp.mDescription.length()>0) {
			insertInitialValues.put(KEY_CP_DESC, cp.mDescription);
		}
		if(cp.mLastBuildDate!=null && cp.mLastBuildDate.length()>0) {
			insertInitialValues.put(KEY_CP_LAST_BUILD, cp.mLastBuildDate);
		}
		insertInitialValues.put(KEY_CP_LAST_UPDATED, cp.mLastUpdated);
		insertInitialValues.put(KEY_CP_TTL, cp.mTTL);
		insertInitialValues.put(KEY_CP_VISIBLE, (cp.mVisible ? 1 : 0) );
		insertInitialValues.put(KEY_CP_VISIBLE_COUNT, cp.mVisibleCount);
		insertInitialValues.put(KEY_CP_CACHING_COUNT, cp.mCachingCount);
		insertInitialValues.put(KEY_CP_CATEGORY, cp.mCategory);
		if(cp.mCategoryName!=null && cp.mCategoryName.length()>0) {
			insertInitialValues.put(KEY_CP_CATEGORYNAME, cp.mCategoryName);
		}
		insertInitialValues.put(KEY_CP_BG_DOWNLOAD, (cp.mBackgroundDownload ? 1 : 0) );
		insertInitialValues.put(KEY_CP_DISPLAY_ORDER, cp.mDisplayOrder);
		insertInitialValues.put(KEY_CP_PARSING_TYPE, cp.mParsingType);
		insertInitialValues.put(KEY_CP_SYS_PROP, cp.mSystemProperty);
		insertInitialValues.put(KEY_CP_WIDGET_ITEM, (cp.mShowInWidget ? 1 : 0));
		insertInitialValues.put(KEY_CP_ARG0, 0 );								// New item count
		insertInitialValues.put(KEY_CP_ARG2, cp.mLogoImage );		// Logo image
		
		synchronized (mDb) {
			if(isDataExist == false) return -1;
			
			if(mDb == null) return -1;
			return mDb.insertOrThrow(TABLE_NAME_CONTENT_PROVIDER, null, insertInitialValues);
		}

	}
	
	public boolean insertBulkItems(ArrayList<FeedObject> feedList) 
	{
		long start = System.currentTimeMillis();
		int time = (int)(start / 1000);
		if(feedList==null || feedList.size()<1) 
			return false;
		
		Logs.d(TAG, "# Insert bulk : type="+feedList.get(0).mType+", item count = "+feedList.size());

		InsertHelper iHelp = new InsertHelper(mDb, TABLE_NAME_FEED_ITEM);
		int ktype 		= iHelp.getColumnIndex(KEY_FEED_TYPE);
		int kstatus 	= iHelp.getColumnIndex(KEY_FEED_STATUS);
		int kid 			= iHelp.getColumnIndex(KEY_FEED_IDSTRING);
		int kname 			= iHelp.getColumnIndex(KEY_FEED_NAME);
		int klink 		= iHelp.getColumnIndex(KEY_FEED_LINK);
		int kkeyword 	= iHelp.getColumnIndex(KEY_FEED_KEYWORD);
		int kcontent 	= iHelp.getColumnIndex(KEY_FEED_CONTENT);
		int kthumbnail 	= iHelp.getColumnIndex(KEY_FEED_THUMBNAILURL);
		int kdate 			= iHelp.getColumnIndex(KEY_FEED_DATE);
		int krank 			= iHelp.getColumnIndex(KEY_FEED_RANK);
		int kclick 			= iHelp.getColumnIndex(KEY_FEED_CLICK);
		int kranktype	= iHelp.getColumnIndex(KEY_FEED_ARG0);
		int kversion	= iHelp.getColumnIndex(KEY_FEED_ARG1);
		int kfullimage	= iHelp.getColumnIndex(KEY_FEED_ARG2);
		
		synchronized (mDb) {
			if(mDb == null) return false;
			try
			{
				mDb.beginTransaction();
				// First one is recent one. So insert oldest first.
				for(int i = feedList.size()-1 ; -1<i ; i--)
				{
					FeedObject feed = feedList.get(i);
					// need to tell the helper you are inserting (rather than replacing)
					iHelp.prepareForInsert();

					// do the equivalent of ContentValues.put("field","value") here
					iHelp.bind(ktype, feed.mType);
					iHelp.bind(kstatus, feed.mDownloadStatus);
					iHelp.bind(kid, feed.mId);
					if(feed.mName != null)
						iHelp.bind(kname, feed.mName);
					if(feed.mLink != null)
						iHelp.bind(klink, feed.mLink);
					if(feed.mKeyword != null)
						iHelp.bind(kkeyword, feed.mKeyword);
					if(feed.mContent != null)
						iHelp.bind(kcontent, feed.mContent);
					if(feed.mThumbnailUrl != null)
						iHelp.bind(kthumbnail, feed.mThumbnailUrl);
					if(feed.mDate != null)
						iHelp.bind(kdate, feed.mDate);
					else
						iHelp.bind(kdate, time);

					iHelp.bind(krank, feed.mRankUpAndDown);
					iHelp.bind(kclick, feed.mCommentCount);
					iHelp.bind(kranktype, feed.mRankType);
					iHelp.bind(kversion, feed.mVersion);

					if(feed.mFullSizeImageURL != null)
						iHelp.bind(kfullimage, feed.mFullSizeImageURL);
					
					//the db.insert() equilvalent
					iHelp.execute();
				}
				mDb.setTransactionSuccessful();
			}
			catch(Exception e) {
			}
			finally	{
				mDb.endTransaction();
			}
		}

		return true;
	}
	
	//----------------------------------------------------------------------------------
	// SELECT
	//----------------------------------------------------------------------------------
	public Cursor selectFilterAll() 
	{
		synchronized (mDb) {
			if(mDb == null) return null;
			return mDb.query(
					TABLE_NAME_FILTERS,	// Table : String
					null,		// Columns : String[]
					null,		// Selection : String
					null,		// Selection arguments: String[]
					null,		// Group by : String
					null,		// Having : String
					null,		// Order by : String
					null );		// Limit : String
		}
	}
	
	public Cursor selectFeed(int type, int count) 
	{
		synchronized (mDb) {
			if(mDb == null) return null;
			return mDb.query(
					TABLE_NAME_FEED_ITEM,		// Table : String
					null,						// Columns : String[]
					KEY_FEED_TYPE + "=" + Integer.toString(type),		// Selection 	: String
					null,			// Selection arguments: String[]
					null,			// Group by 	: String
					null,			// Having 		: String
					KEY_FEED_ID+" DESC",			// Order by 	: String
					Integer.toString(count) );		// Limit			: String
		}
	}
	
	public Cursor selectFeedAll() 
	{
		synchronized (mDb) {
			if(mDb == null) return null;
			return mDb.query(
					TABLE_NAME_FEED_ITEM,		// Table : String
					null,						// Columns : String[]
					null,			// Selection 	: String
					null,			// Selection arguments: String[]
					null,			// Group by 	: String
					null,			// Having 		: String
					null,			// Order by 	: String
					null );		// Limit			: String
		}
	}
	
	public Cursor selectCP() 
	{
		synchronized (mDb) {
			if(mDb == null) return null;
			return mDb.query(
					TABLE_NAME_CONTENT_PROVIDER,				// Table : String
					null,																// Columns : String[]
					null,			// Selection 	: String
					null,			// Selection arguments: String[]
					null,			// Group by 	: String
					null,			// Having 		: String
					KEY_CP_ARG0+" DESC",			// Order by 	: String	(unread count)
					null );		// Limit			: String
		}
	}
	
	//----------------------------------------------------------------------------------
	// Update
	//----------------------------------------------------------------------------------
	public int updateFilter(FilterObject filter) 
	{
		if(filter.mType < 0 || filter.mCompareType < 0 
				|| filter.mOriginalString == null || filter.mOriginalString.length() < 1)
			return -1;
		
		ContentValues insertValues = new ContentValues();
		insertValues.put(KEY_FILTER_TYPE, filter.mType);
		insertValues.put(KEY_FILTER_ICON_TYPE, filter.mIconType);
		insertValues.put(KEY_FILTER_MATCHING, filter.mCompareType);
		insertValues.put(KEY_FILTER_REPLACE_TYPE, filter.mReplaceType);
		insertValues.put(KEY_FILTER_ORIGINAL, filter.mOriginalString);
		insertValues.put(KEY_FILTER_REPLACE, filter.mReplaceString);
//		insertValues.put(KEY_FILTER_ARG0, 0);		// for future use
//		insertValues.put(KEY_FILTER_ARG1, 0);
//		insertValues.put(KEY_FILTER_ARG2, "");
//		insertValues.put(KEY_FILTER_ARG3, "");
		
		synchronized (mDb) {
			if(mDb == null) 
				return -1;
			return mDb.update( TABLE_NAME_FILTERS,		// table
					insertValues, 		// values
					KEY_FILTER_ID + "='" + filter.mId + "'", // whereClause
					null ); 			// whereArgs
		}
	}
	
	public int updateCP(CPObject cp) 
	{
		if(cp.mId <= FeedObject.REQUEST_TYPE_NONE || cp.mURL==null || cp.mURL.length() < 1)
			return 0;
		
		Logs.d(TAG, "# insert new content provider : type = "+cp.mId);
		
		boolean isDataExist = false;
		ContentValues insertValues = new ContentValues();
		if(cp.mURL!= null && cp.mURL.length()>0) {
			insertValues.put(KEY_CP_URL, cp.mURL);
			isDataExist= true;
		}
		if(cp.mName!= null && cp.mName.length()>0) {
			insertValues.put(KEY_CP_NAME, cp.mName);
		}
		if(cp.mLink!=null && cp.mLink.length()>0) {
			insertValues.put(KEY_CP_LINK, cp.mLink);
		}
		if(cp.mDescription!=null && cp.mDescription.length()>0) {
			insertValues.put(KEY_CP_DESC, cp.mDescription);
		}
		if(cp.mLastBuildDate!=null && cp.mLastBuildDate.length()>0) {
			insertValues.put(KEY_CP_LAST_BUILD, cp.mLastBuildDate);
		}
		insertValues.put(KEY_CP_LAST_UPDATED, cp.mLastUpdated);
		insertValues.put(KEY_CP_TTL, cp.mTTL);
		insertValues.put(KEY_CP_VISIBLE, (cp.mVisible ? 1 : 0) );
		insertValues.put(KEY_CP_VISIBLE_COUNT, cp.mVisibleCount);
		insertValues.put(KEY_CP_CACHING_COUNT, cp.mCachingCount);
		insertValues.put(KEY_CP_CATEGORY, cp.mCategory);
		if(cp.mCategoryName!=null && cp.mCategoryName.length()>0) {
			insertValues.put(KEY_CP_CATEGORYNAME, cp.mCategoryName);
		}
		insertValues.put(KEY_CP_BG_DOWNLOAD, (cp.mBackgroundDownload ? 1 : 0) );
		insertValues.put(KEY_CP_DISPLAY_ORDER, cp.mDisplayOrder);
		insertValues.put(KEY_CP_PARSING_TYPE, cp.mParsingType);
		insertValues.put(KEY_CP_SYS_PROP, cp.mSystemProperty);
		insertValues.put(KEY_CP_WIDGET_ITEM, (cp.mShowInWidget ? 1 : 0));
		insertValues.put(KEY_CP_ARG0, cp.mNewItemCount );						// New item count
		
		synchronized (mDb) {
			if(isDataExist == false) return 0;
			if(mDb == null) return 0;
			
			return mDb.update( TABLE_NAME_CONTENT_PROVIDER,		// table
									insertValues, 						// values
									KEY_CP_ID + "='" + cp.mId + "'", 	// whereClause
									null ); 				// whereArgs
		}
	}
	
	public int updateLastUpdatedTime(int type, long time, int newUpdatesCount) 
	{
		String where = null;
		if(type != FeedObject.REQUEST_TYPE_NONE)
			where = KEY_CP_ID + "='" + type + "'";
		
		Logs.d(TAG, "# update content provider table : type = "+type);
		Logs.d(TAG, "# query : "+where);
		
		ContentValues insertValues = new ContentValues();
		insertValues.put(KEY_CP_LAST_UPDATED, time);
		insertValues.put(KEY_CP_ARG0, newUpdatesCount);					// New item count
		
		synchronized (mDb) {
			if(mDb == null) return -1;
			return mDb.update( TABLE_NAME_CONTENT_PROVIDER,		// table
									insertValues, 	// values
									where, 			// whereClause
									null ); 		// whereArgs
		}
	}
	
	//----------------------------------------------------------------------------------
	// Delete
	//----------------------------------------------------------------------------------

	public void deleteFilterWithID(int id) {
		if(mDb == null) return;
		
		synchronized (mDb) {
			int count = mDb.delete(TABLE_NAME_FILTERS, 
					KEY_FILTER_ID + "=" + id, // whereClause
					null); 			// whereArgs
			Logs.d(TAG, "- Delete filter : id="+id+", count="+count);
		}
	}
	
	public void deleteFilterWithType(int type) {
		if(mDb == null) return;
		
		synchronized (mDb) {
			int count = mDb.delete(TABLE_NAME_FILTERS, 
					KEY_FILTER_TYPE + "='" + type + "'", // whereClause
					null); 			// whereArgs
			Logs.d(TAG, "- Delete filter : type="+type+", count="+count);
		}
	}
	
	public void deleteFilterAll() {
		if(mDb == null) return;
		
		synchronized (mDb) {
			mDb.delete(TABLE_NAME_FILTERS, null, null);
		}
	}
	
	public void deleteFeedWithType(int type) {	// Delete records that has same type
		synchronized (mDb) {
			if(mDb == null) return;
			mDb.delete(TABLE_NAME_FEED_ITEM, 
								KEY_FEED_TYPE + "='" + Integer.toString(type) + "'",
								null);
		}
	}
	public void deleteFeedWithID(String id) {	// Delete a record that has same ID
		synchronized (mDb) {
			if(mDb == null) return;
			mDb.delete(TABLE_NAME_FEED_ITEM, 
								KEY_FEED_IDSTRING + "='" + id + "'",
								null);
		}
	}
	public void deleteFeedAll(){				// Delete all items.
		synchronized (mDb) {
			if(mDb == null) return;
			mDb.delete(TABLE_NAME_FEED_ITEM, null, null);
		}
	}
	
	public void deleteCP(int id) {				// Delete a record that has same ID
		synchronized (mDb) {
			if(mDb == null) return;
			mDb.delete(TABLE_NAME_CONTENT_PROVIDER, 
								KEY_CP_ID + "='" + id + "'",
								null);
		}
	}
	
	//----------------------------------------------------------------------------------
	// Count
	//----------------------------------------------------------------------------------
	public int getFilterCount() {
		String query = "select count(*) from " + TABLE_NAME_FILTERS;
		Cursor c = mDb.rawQuery(query, null);
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		return count;
	}
	
	public int getFeedCountWithType(int type) {
		String query = "select count(*) from " + TABLE_NAME_FEED_ITEM + " where " + KEY_FEED_TYPE + "=" + Integer.toString(type);
		Cursor c = mDb.rawQuery(query, null);
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		return count;
	}
	
	public int getFeedCount() {
		String query = "select count(*) from "+TABLE_NAME_FEED_ITEM;
		Cursor c = mDb.rawQuery(query, null);
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		return count;
	}
	
	public int getCPCount() {
		String query = "select count(*) from " + TABLE_NAME_CONTENT_PROVIDER;
		Cursor c = mDb.rawQuery(query, null);
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		return count;
	}
	

	//----------------------------------------------------------------------------------
	// SQLiteOpenHelper
	//----------------------------------------------------------------------------------
	private static class DatabaseHelper extends SQLiteOpenHelper 
	{
		// Constructor
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			// TODO Auto-generated constructor stub
		}

		// Will be called one time at first access
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE_FILTERS);
			db.execSQL(DATABASE_CREATE_FEED);
			db.execSQL(DATABASE_CREATE_CP);
		}

		// Will be called when the version is increased
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Keep previous data
			db.execSQL(DATABASE_DROP_FILTERS_TABLE);
			db.execSQL(DATABASE_DROP_FEED_ITEM_TABLE);
			db.execSQL(DATABASE_DROP_CONTENT_PROVIDER_TABLE);
			
			db.execSQL(DATABASE_CREATE_FILTERS);
			db.execSQL(DATABASE_CREATE_FEED);
			db.execSQL(DATABASE_CREATE_CP);
		}
		
	}	// End of class DatabaseHelper
	
}
