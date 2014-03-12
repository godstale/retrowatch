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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.hardcopy.retrowatchle.R;
import com.hardcopy.retrowatchle.contents.objects.CPObject;
import com.hardcopy.retrowatchle.contents.objects.ContentObject;
import com.hardcopy.retrowatchle.contents.objects.EmergencyObject;
import com.hardcopy.retrowatchle.contents.objects.FeedObject;
import com.hardcopy.retrowatchle.contents.objects.FilterObject;
import com.hardcopy.retrowatchle.contents.objects.NotificationObject;
import com.hardcopy.retrowatchle.database.DBHelper;
import com.hardcopy.retrowatchle.utils.Logs;
import com.hardcopy.retrowatchle.utils.Settings;
import com.hardcopy.retrowatchle.utils.Utils;

public class ContentManager {
	
	private static final String TAG = "ContentManager";
	
	private static final int RESPONSE_GENERAL_FAILURE = -1;
	private static final int RESPONSE_INVALID_PARAMETER = -2;
	
	private static final int FEED_SUBSTRING_SIZE = 50;
	
	private static ContentManager mContentManager = null;		// Singleton pattern
	
	private Context mContext;
	private IContentManagerListener mContentManagerListener;
	private DBHelper mDB = null;
	private FeedManager mFeedManager;
	
	private ArrayList<ContentObject> mContentList;		// Cache every type of contents
	
	private ArrayList<NotificationObject> mNotificationList;	// Notification type
	private ArrayList<ContentObject> mMessagingList;	// Messaging type
	private ArrayList<ContentObject> mEmergencyList;
	
	private ArrayList<FilterObject> mFilterList;
	
	private int mRFStatus = EmergencyObject.RF_STATE_IN_SERVICE;
	private int mWiFiStatus = EmergencyObject.WIFI_STATE_ACTIVATED;
	private int mBatteryGauge = 0;
	private int mBatteryCharging = EmergencyObject.BATT_STATE_UNKNOWN;
	
	
	// For Gmail info: Get the account list
	private String mGmailAddress = null;
	final String ACCOUNT_TYPE_GOOGLE = "com.google";
	final String[] FEATURES_MAIL = {"service_mail"};
	
	private int mGmailUnreadCount = 0;
	
	
	
	private ContentManager(Context c, IContentManagerListener l) {
		mContext = c;
		mContentManagerListener = l;
		
		mContentList = new ArrayList<ContentObject>();
		mNotificationList = new ArrayList<NotificationObject>();
		mMessagingList = new ArrayList<ContentObject>();
		mEmergencyList = new ArrayList<ContentObject>();
		
		mFilterList = new ArrayList<FilterObject>();
		
		//----- Make DB helper
		if(mDB == null) {
			mDB = new DBHelper(mContext).openWritable();
		}
		if(mFeedManager == null)
			mFeedManager = new FeedManager(mContext, mFeedListener);
		
		getFiltersFromDB();
		
		mGmailAddress = Settings.getInstance(mContext).getGmailAddress();
	}
	
	public synchronized static ContentManager getInstance(Context c, IContentManagerListener l) {
		if(mContentManager == null)
			mContentManager = new ContentManager(c, l);
		
		return mContentManager;
	}
	
	public synchronized void finalize() {
		if(mDB != null) {
			mDB.close();
			mDB = null;
		}
		mContentManager = null;
		if(mFeedManager != null) {
			mFeedManager.finalize();
		}
	}

	
	/*****************************************************
	 * 
	 *	Private methods
	 *
	 ******************************************************/
	private void getFiltersFromDB() {
		Cursor c = mDB.selectFilterAll();
		
		if(c != null && c.getCount() > 0) {
			c.moveToFirst();
			while(!c.isAfterLast()) {
				FilterObject filter = new FilterObject();
				filter.mId = c.getInt(DBHelper.INDEX_FILTER_ID);
				filter.mType = c.getInt(DBHelper.INDEX_FILTER_TYPE);
				filter.mIconType = c.getInt(DBHelper.INDEX_FILTER_ICON_TYPE);
				filter.mCompareType = c.getInt(DBHelper.INDEX_FILTER_MATCHING);
				filter.mReplaceType = c.getInt(DBHelper.INDEX_FILTER_REPLACE_TYPE);
				filter.mOriginalString = c.getString(DBHelper.INDEX_FILTER_ORIGINAL);
				filter.mReplaceString = c.getString(DBHelper.INDEX_FILTER_REPLACE);
				
				mFilterList.add(filter);
				c.moveToNext();
			}
		}
		
		if(c != null) c.close();
	}
	
	private ContentObject applyFilters(NotificationObject noti) {
		String strOrigin = null;
		if(noti.mText != null)
			strOrigin = noti.mText;
		else
			strOrigin = "";
				
		String strResult = applyFilters(FilterObject.FILTER_TYPE_NOTIFICATION, strOrigin, noti.mPackageName);
		
		ContentObject obj = null;
		if(strResult != null) {
			obj = new ContentObject(ContentObject.CONTENT_TYPE_NOTIFICATION, noti.mId, strOrigin, strResult);
			obj.mPackageName = noti.mPackageName;

			// If no filter is applied, mFilterIcon is -1. 
			if(mFilterIcon < 0) {
				obj.mIconType = ContentObject.ICON_TYPE_NORMAL_MESSAGE;		// set default icon
			} else {
				obj.mIconType = mFilterIcon;
				obj.mIsEnabled = true;		// Enable filtered notification
			}
		}
		return obj;
	}
	
	private ContentObject applyFilters(FeedObject feed) {
		String strOrigin = null;
		if(feed.mKeyword != null)
			strOrigin = feed.mKeyword.substring(0, 
					(feed.mKeyword.length() < FEED_SUBSTRING_SIZE) ? feed.mKeyword.length() : FEED_SUBSTRING_SIZE);
		else if(feed.mContent != null)
			strOrigin = feed.mContent.substring(0, 
					(feed.mContent.length() < FEED_SUBSTRING_SIZE) ? feed.mContent.length() : FEED_SUBSTRING_SIZE);
		else
			return null;
				
		String strResult = applyFilters(FilterObject.FILTER_TYPE_FEED, strOrigin, 
				ContentObject.FEED_PACKAGE_NAME + "." + Integer.toString(feed.mType));
		
		ContentObject obj = null;
		if(strResult != null && !strResult.isEmpty()) {
			obj = new ContentObject(ContentObject.CONTENT_TYPE_FEED, feed.mType, strOrigin, strResult);
			obj.mPackageName = ContentObject.FEED_PACKAGE_NAME + "." + feed.mType;
			
			// If no filter is applied, mFilterIcon is -1. 
			if(mFilterIcon < 0) {
				obj.mIconType = ContentObject.ICON_TYPE_RSS;		// set default icon
			} else {
				obj.mIconType = mFilterIcon;
				obj.mIsEnabled = true;		// Enable filtered notification
			}
			if(!strResult.equals(strOrigin))
				obj.mIsEnabled = true;
		}
		return obj;
	}
	
	private int mFilterIcon = -1;
	private String applyFilters(int filter_type, String strSrc, String strPackage) {
		if(strSrc == null || strSrc.isEmpty())
			return null;

		mFilterIcon = -1;
		strSrc.trim();
		String strResult = strSrc;
		
		if(mFilterList != null && mFilterList.size() > 0) {
			for(FilterObject filter : mFilterList) {
				// Use specified, package name or free-type filter
				if(filter.mType != FilterObject.FILTER_TYPE_ALL 
						&& filter.mType != FilterObject.FILTER_TYPE_PACKAGE_NAME
						&& filter.mType != filter_type)
					continue;
				// Check compare string
				if(filter.mOriginalString == null || filter.mOriginalString.isEmpty())
					continue;
				
				
				boolean isFound = false;
				
				if(filter.mType == FilterObject.FILTER_TYPE_PACKAGE_NAME) {
					// Package name filter
					if(strPackage == null || strPackage.isEmpty())
						continue;
					
					switch(filter.mCompareType) {
					case FilterObject.MATCHING_TYPE_WHOLE_WORD:
						if(strPackage.compareToIgnoreCase(filter.mOriginalString) == 0) {
							/*
							 * Disabled
							 * 
							// Exactly same string. so replace with filter's string.
							if(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) {
								strResult = "";
							} else {
								strResult = filter.mReplaceString;
								mFilterIcon = filter.mIconType;
								isFound = true;
							}
							*/
							if(filter.mReplaceType == FilterObject.REPLACE_TYPE_SAME_PART) {
								// Package matching supports 'Replace all' option only
								//strResult = strResult.replaceAll(filter.mOriginalString, 
								//		(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) ? "" : filter.mReplaceString );
								mFilterIcon = filter.mIconType;
								isFound = true;
							} else if(filter.mReplaceType == FilterObject.REPLACE_TYPE_ALL) {
								if(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) {
									strResult = "";
								} else {
									strResult = filter.mReplaceString;
									mFilterIcon = filter.mIconType;
									isFound = true;
								}
							}
						}
						break;
					case FilterObject.MATCHING_TYPE_START_WITH:
						if(strPackage.startsWith(filter.mOriginalString)) {
							if(filter.mReplaceType == FilterObject.REPLACE_TYPE_SAME_PART) {
								// Package matching supports 'Replace all' option only
								//strResult = strResult.replaceAll(filter.mOriginalString, 
								//		(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) ? "" : filter.mReplaceString );
								mFilterIcon = filter.mIconType;
								isFound = true;
							} else if(filter.mReplaceType == FilterObject.REPLACE_TYPE_ALL) {
								if(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) {
									strResult = "";
								} else {
									strResult = filter.mReplaceString;
									mFilterIcon = filter.mIconType;
									isFound = true;
								}
							}
						}
						break;
					case FilterObject.MATCHING_TYPE_END_WITH:
						if(strPackage.endsWith(filter.mOriginalString)) {
							if(filter.mReplaceType == FilterObject.REPLACE_TYPE_SAME_PART) {
								// Package matching supports 'Replace all' option only
								//strResult = strResult.replaceAll(filter.mOriginalString, 
								//		(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) ? "" : filter.mReplaceString );
								mFilterIcon = filter.mIconType;
								isFound = true;
							} else if(filter.mReplaceType == FilterObject.REPLACE_TYPE_ALL) {
								if(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) {
									strResult = "";
								} else {
									strResult = filter.mReplaceString;
									mFilterIcon = filter.mIconType;
									isFound = true;
								}
							}
						}
						break;
					case FilterObject.MATCHING_TYPE_PART_OF:
						if(strPackage.contains(filter.mOriginalString)) {
							if(filter.mReplaceType == FilterObject.REPLACE_TYPE_SAME_PART) {
								// Package matching supports 'Replace all' option only
								//strResult = strResult.replaceAll(filter.mOriginalString, 
								//		(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) ? "" : filter.mReplaceString );
								mFilterIcon = filter.mIconType;
								isFound = true;
							} else if(filter.mReplaceType == FilterObject.REPLACE_TYPE_ALL) {
								if(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) {
									strResult = "";
								} else {
									strResult = filter.mReplaceString;
									mFilterIcon = filter.mIconType;
									isFound = true;
								}
							}
						}
						break;
					default:
						break;
					}	// End of switch(filter.mCompareType)
					
				} else {
					
					switch(filter.mCompareType) {
					case FilterObject.MATCHING_TYPE_WHOLE_WORD:
						if(strSrc.compareToIgnoreCase(filter.mOriginalString) == 0) {
							// Exactly same string. so replace with filter's string.
							if(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) {
								strResult = "";
							} else {
								strResult = filter.mReplaceString;
								mFilterIcon = filter.mIconType;
								isFound = true;
							}
						}
						break;
					case FilterObject.MATCHING_TYPE_START_WITH:
						if(strSrc.startsWith(filter.mOriginalString)) {
							if(filter.mReplaceType == FilterObject.REPLACE_TYPE_SAME_PART) {
								strResult = strResult.replaceAll(filter.mOriginalString, 
										(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) ? "" : filter.mReplaceString );
								mFilterIcon = filter.mIconType;
								isFound = true;
							} else if(filter.mReplaceType == FilterObject.REPLACE_TYPE_ALL) {
								if(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) {
									strResult = "";
								} else {
									strResult = filter.mReplaceString;
									mFilterIcon = filter.mIconType;
									isFound = true;
								}
							}
						}
						break;
					case FilterObject.MATCHING_TYPE_END_WITH:
						if(strSrc.endsWith(filter.mOriginalString)) {
							if(filter.mReplaceType == FilterObject.REPLACE_TYPE_SAME_PART) {
								strResult = strResult.replaceAll(filter.mOriginalString, 
										(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) ? "" : filter.mReplaceString );
								mFilterIcon = filter.mIconType;
								isFound = true;
							} else if(filter.mReplaceType == FilterObject.REPLACE_TYPE_ALL) {
								if(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) {
									strResult = "";
								} else {
									strResult = filter.mReplaceString;
									mFilterIcon = filter.mIconType;
									isFound = true;
								}
							}
						}
						break;
					case FilterObject.MATCHING_TYPE_PART_OF:
						if(strSrc.contains(filter.mOriginalString)) {
							if(filter.mReplaceType == FilterObject.REPLACE_TYPE_SAME_PART) {
								strResult = strResult.replaceAll(filter.mOriginalString, 
										(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) ? "" : filter.mReplaceString );
								mFilterIcon = filter.mIconType;
								isFound = true;
							} else if(filter.mReplaceType == FilterObject.REPLACE_TYPE_ALL) {
								if(filter.mReplaceString == null || filter.mReplaceString.isEmpty()) {
									strResult = "";
								} else {
									strResult = filter.mReplaceString;
									mFilterIcon = filter.mIconType;
									isFound = true;
								}
							}
						}
						break;
					default:
						break;
					}	// End of switch(filter.mCompareType)
				}
				
				// If below code is enabled, apply only 1 filter
//				if(isFound)
//					break;
			}	// End of for loop
		}
		
		// Filtering completed. Check result string.
		if(strResult == null || strResult.trim().isEmpty()) {
			return null;
		}
		return strResult;
	}
	
	private int onAccountResults(Account[] accounts) {
		Logs.d(TAG, "received accounts: " + Arrays.toString(accounts));
		int unreadCount = 0;
		
		if (mGmailAddress != null && !mGmailAddress.isEmpty() && accounts != null && accounts.length > 0) {
			// Pick the the user specified account
			for(int i=0; i<accounts.length; i++) {
				if(accounts[i].name.equalsIgnoreCase(mGmailAddress)) {
					// Query for the list of labels
					final Uri labelsUri = GmailContract.Labels.getLabelsUri(accounts[i].name);
					Cursor labelsCursor = mContext.getContentResolver().query(labelsUri, null, null, null, null);
					
					if (labelsCursor != null) {
						int unreadColumn = labelsCursor.getColumnIndex(GmailContract.Labels.NUM_UNREAD_CONVERSATIONS);
						int nameColumn = labelsCursor.getColumnIndex(GmailContract.Labels.CANONICAL_NAME);
						while (labelsCursor.moveToNext()) {
							String name = labelsCursor.getString(nameColumn);
							if (GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX_CATEGORY_PRIMARY.equals(name)) {
								unreadCount = labelsCursor.getInt(unreadColumn);
							}
						}
						labelsCursor.close();
					}
				}
			}	// End of for() loop
		}
		return unreadCount;
	}
	
	private void removeContentObject(int type) {
		for(int i = mContentList.size() - 1; i > -1; i--) {
			ContentObject obj = mContentList.get(i);
			if(obj.mContentType == type) {
				mContentList.remove(i);
			}
		}
	}
	
	private void removeContentObject(int type, int id) {
		for(int i = mContentList.size() - 1; i > -1; i--) {
			ContentObject obj = mContentList.get(i);
			if(obj.mContentType == type && obj.mId == id) {
				mContentList.remove(i);
			}
		}
	}
	
	private void removeContentObject(int type, String packageName) {
		for(int i = mContentList.size() - 1; i > -1; i--) {
			ContentObject obj = mContentList.get(i);
			if(obj.mContentType == type) {
				if(obj.mPackageName != null && obj.mPackageName.contains(packageName))
					mContentList.remove(i);
			}
		}
	}
	
	private void removeContentObject(int type, String packageName, ArrayList<ContentObject> arrayList) {
		if(arrayList == null)
			return;
		for(int i = arrayList.size() - 1; i > -1; i--) {
			ContentObject obj = arrayList.get(i);
			if(obj.mContentType == type) {
				if(obj.mPackageName != null && obj.mPackageName.contains(packageName))
					arrayList.remove(i);
			}
		}
	}
	
	private void deleteCachedFeed(int type) {
		mFeedManager.deleteCachedFeed(type);
	}
	
	
	
	
	/*****************************************************
	 * 
	 *	Public methods
	 *
	 ******************************************************/
	
	public synchronized ArrayList<ContentObject> getContentObjectList() {
		return mContentList;
	}
	
	public synchronized ArrayList<FeedObject> getFeedObjectList() {
		return mFeedManager.getFeedList();		// Use this read-only. Or could cause concurrent modification exception
	}
	
	public synchronized ArrayList<FilterObject> getFilterObjectList() {
		return mFilterList;
	}
	
	public synchronized ArrayList<CPObject> getCPObjectList() {
		return mFeedManager.getContentProviderList();
	}
	
	public synchronized ArrayList<ContentObject> refreshContentObjectList() {
		mContentList.clear();
		
		// Add notifications
		if(mNotificationList != null && mNotificationList.size() > 0) {
			for(NotificationObject noti : mNotificationList) {
				ContentObject content = applyFilters(noti);
				if(content == null)
					continue;
				mContentList.add(content);
			}
		}
		
		// Refresh messaging list
		if(mMessagingList != null && mMessagingList.size() > 0) {
			for(ContentObject obj : mMessagingList) {
				obj.mFilteredString = applyFilters(FilterObject.FILTER_TYPE_MESSAGING, obj.mOriginalString, obj.mPackageName);
				obj.mIsEnabled = true;		// Enable this object (will be shown on watch)
				obj.mIconType = mFilterIcon;
				if(obj.mFilteredString == null || obj.mFilteredString.isEmpty())
					continue;
				mContentList.add(obj);
			}
		}
		
		// Refresh emergency list
		if(mEmergencyList != null && mEmergencyList.size() > 0) {
			for(ContentObject obj : mEmergencyList) {
				obj.mFilteredString = applyFilters(FilterObject.FILTER_TYPE_EMERGENCY, obj.mOriginalString, obj.mPackageName);
				
				if(obj.mFilteredString == null || obj.mFilteredString.isEmpty())
					continue;
				
				if(mFilterIcon < 0) {
					// This case means no filter applied
					if(obj.mContentType == ContentObject.EMERGENCY_ID_BATT_STATE) {
						obj.mIsEnabled = false;
					}
				} else {
					obj.mIsEnabled = true;		// Enable this object (will be shown on watch)
					obj.mIconType = mFilterIcon;
				}
				if(obj.mContentType == ContentObject.EMERGENCY_ID_BATT_STATE && mBatteryGauge < 25) {
					obj.mIsEnabled = true;		// Enable this object (will be shown on watch)
				}

				mContentList.add(obj);
			}
		}
		
		// Refresh feed list
		refreshFeedList();
		
		// Get gmail unread count. This method makes asynchronous call.
		// Result should be handled in addGmailToContentList()
		queryGmailLabels();
		
		// Query WiFi status
		queryWiFiStatus();
		
		return mContentList;
	}
	
	public ArrayList<ContentObject> refreshFeedList() {
		ArrayList<ContentObject> array = null;
		final ArrayList<FeedObject> feedList = mFeedManager.getFeedList();
		if(feedList != null) {
			array = new ArrayList<ContentObject>();
			
			for(FeedObject feed : feedList) {
				ContentObject content = applyFilters(feed);
				if(content == null)
					continue;
				mContentList.add(content);
				array.add(content);
			}
		}
		return array;
	}
	
	public void queryWiFiStatus() {
		WifiManager wManager;
		wManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wInfo = wManager.getConnectionInfo();
		if (wManager.isWifiEnabled() == true && wInfo.getSSID() != null) {
			// WiFi activated
			mWiFiStatus = EmergencyObject.WIFI_STATE_ACTIVATED;
		} else {
			// Cannot use wifi
			mWiFiStatus = EmergencyObject.WIFI_STATE_DISABLED;
		}
		
		removeContentObject(ContentObject.CONTENT_TYPE_EMERGENCY, ContentObject.WIFI_PACKAGE_NAME);	// Remove from content object list
		removeContentObject(ContentObject.CONTENT_TYPE_EMERGENCY, 
				ContentObject.WIFI_PACKAGE_NAME, mEmergencyList);	// Remove from messaging list
		
		String wifi_msg = null;
		if(mWiFiStatus == EmergencyObject.WIFI_STATE_ACTIVATED)
			wifi_msg = "WiFi is on";
		else 
			wifi_msg = "Cannot use WiFi";
		String strResult = applyFilters(FilterObject.FILTER_TYPE_EMERGENCY,
				wifi_msg,	// Default message string
				ContentObject.WIFI_PACKAGE_NAME);
		
		if(strResult != null && !strResult.isEmpty()) {
			ContentObject obj = new ContentObject(ContentObject.CONTENT_TYPE_EMERGENCY, 
					ContentObject.EMERGENCY_ID_WIFI, 					// Fixed ID
					wifi_msg, 	// Default message string
					strResult);			// Set replace message. This message will be sent to remote
			obj.mPackageName = ContentObject.WIFI_PACKAGE_NAME;
			// If no filter is applied, mFilterIcon is -1. 
			if(mFilterIcon < 0) {
				obj.mIconType = ContentObject.ICON_TYPE_RF_STATE;		// set default wifi icon
			} else {
				obj.mIconType = mFilterIcon;
				obj.mIsEnabled = true;
			}
			
			mContentList.add(obj);
			mEmergencyList.add(obj);
		}
	}
	
	public int getWiFiStatus() {
		return mWiFiStatus;
	}
	
	public synchronized void setGmailAddress(String addr) {
		Settings settings = Settings.getInstance(mContext);
		settings.setGmailAddress(addr);
		mGmailAddress = addr;
	}
	
	public synchronized void queryGmailLabels() {
		// Get the account list, and pick the user specified address
		AccountManager.get(mContext).getAccountsByTypeAndFeatures(ACCOUNT_TYPE_GOOGLE, FEATURES_MAIL,
				new AccountManagerCallback<Account[]>() {
			@Override
			public void run(AccountManagerFuture<Account[]> future) {
				Account[] accounts = null;
				try {
					accounts = future.getResult();
				} catch (OperationCanceledException oce) {
					Logs.e(TAG, "Got OperationCanceledException: "+oce.toString());
				} catch (IOException ioe) {
					Logs.e(TAG, "Got OperationCanceledException: "+ioe.toString());
				} catch (AuthenticatorException ae) {
					Logs.e(TAG, "Got OperationCanceledException: "+ae.toString());
				}
				mGmailUnreadCount = onAccountResults(accounts);
				addGmailToContentList(mGmailUnreadCount);
				Logs.d(TAG, "# Gmail unread count = "+ mGmailUnreadCount);
			}
		}, null /* handler */);
	}
	
	public synchronized void addGmailToContentList(int unreadCount) {
		ContentObject obj = null;
		
		removeContentObject(ContentObject.CONTENT_TYPE_MESSAGING, ContentObject.GMAIL_PACKAGE_NAME);	// Remove from content object list
		removeContentObject(ContentObject.CONTENT_TYPE_MESSAGING, 
				ContentObject.GMAIL_PACKAGE_NAME, mMessagingList);	// Remove from messaging list
		
		String msgString = null;
		String gmailAddr = Settings.getInstance(mContext).getGmailAddress();
		if(gmailAddr == null || gmailAddr.isEmpty())
			msgString = mContext.getResources().getString(R.string.noti_set_gmail_addr);
		else
			msgString = Integer.toString(unreadCount) + " unread email";
		
		String strResult = applyFilters(FilterObject.FILTER_TYPE_MESSAGING, 
				msgString,	// Default message string
				ContentObject.GMAIL_PACKAGE_NAME);
		
		if(strResult != null && !strResult.isEmpty()) {
			obj = new ContentObject(ContentObject.CONTENT_TYPE_MESSAGING, 
					ContentObject.MESSAGING_ID_GMAIL, 							// Fixed ID
					msgString,	// Default message string
					strResult);					// Set replaced message. This message will be sent to remote
			obj.mPackageName = ContentObject.GMAIL_PACKAGE_NAME;
			// If no filter is applied, mFilterIcon is -1.
			if(mFilterIcon < 0) {
				obj.mIconType = ContentObject.ICON_TYPE_EMAIL;		// set email icon
			} else {
				obj.mIconType = mFilterIcon;
				obj.mIsEnabled = true;
			}
			if(unreadCount > 0) {
				obj.mIsEnabled = true;		// Enable this object (will be shown on watch)
			}
			
			mContentList.add(obj);
			mMessagingList.add(obj);
		}
		
		// This method runs in asynchronous mode
		// So we have to notify to callback
		mContentManagerListener.OnContentCallback(IContentManagerListener.CALLBACK_GMAIL_UPDATED, 0, 0, null, null, obj);
	}
	
	public synchronized ContentObject addSMSObject(int count) {
		ContentObject obj = null;
		
		// Delete cached contents
		removeContentObject(ContentObject.CONTENT_TYPE_MESSAGING, ContentObject.SMS_PACKAGE_NAME);	// Remove from content object list
		removeContentObject(ContentObject.CONTENT_TYPE_MESSAGING, 
				ContentObject.SMS_PACKAGE_NAME, mMessagingList);	// Remove from messaging list
		
		if(count > 0) {
			String strResult = applyFilters(FilterObject.FILTER_TYPE_MESSAGING, 
					Integer.toString(count) + " new SMS",	// Default message string
					ContentObject.SMS_PACKAGE_NAME);
			
			if(strResult != null && !strResult.isEmpty()) {
				obj = new ContentObject(ContentObject.CONTENT_TYPE_MESSAGING, 
						ContentObject.MESSAGING_ID_SMS, 					// Fixed ID
						Integer.toString(count) + " new SMS", 	// Default message string
						strResult);			// Set replace message. This message will be sent to remote
				obj.mPackageName = ContentObject.SMS_PACKAGE_NAME;
				// If no filter is applied, mFilterIcon is -1.
				if(mFilterIcon < 0)
					obj.mIconType = ContentObject.ICON_TYPE_EMAIL;		// set email icon
				else
					obj.mIconType = mFilterIcon;
				obj.mIsEnabled = true;		// Enable this object (will be shown on watch)
				
				mContentList.add(obj);
				mMessagingList.add(obj);
			}
		}
		return obj;
	}
	
	public synchronized ContentObject addCallObject(int state, String number) {
		Logs.d(TAG, "# Call state changed to "+state+", number="+number);
		ContentObject obj = null;
		
		// Delete cached contents
		removeContentObject(ContentObject.CONTENT_TYPE_EMERGENCY, ContentObject.TELEPHONY_CALL_PACKAGE_NAME);	// Remove from content object list
		removeContentObject(ContentObject.CONTENT_TYPE_EMERGENCY, 
				ContentObject.TELEPHONY_CALL_PACKAGE_NAME, mEmergencyList);	// Remove from emergency list
		
		if(state == EmergencyObject.CALL_STATE_IDLE)	// In idle state, do not add call contents
			return null;
		
		String strResult = applyFilters(FilterObject.FILTER_TYPE_EMERGENCY, 
				Utils.getCallTypeString(state),		// Default message string
				ContentObject.TELEPHONY_CALL_PACKAGE_NAME);
		
		if(strResult != null && !strResult.isEmpty()) {
			obj = new ContentObject(ContentObject.CONTENT_TYPE_EMERGENCY, 
					ContentObject.EMERGENCY_ID_CALL_STATE, 					// Fixed ID
					Utils.getCallTypeString(state), 	// Default message string
					strResult);			// Set replace message. This message will be sent to remote
			obj.mExtraData = number;
			obj.mPackageName = ContentObject.TELEPHONY_CALL_PACKAGE_NAME;
			// If no filter is applied, mFilterIcon is -1.
			if(mFilterIcon < 0)
				obj.mIconType = ContentObject.ICON_TYPE_CALL;		// set call icon
			else
				obj.mIconType = mFilterIcon;
			obj.mIsEnabled = true;		// Always enable this object (will be shown on watch)
			
			mContentList.add(obj);
			mEmergencyList.add(obj);
		}
		return obj;
	}
	
	public synchronized ContentObject setBatteryInfo(int level, int chargingState) {
		mBatteryGauge = level;
		mBatteryCharging = chargingState;
		ContentObject obj = addBatteryObject(chargingState, level);
		return obj;
	}
	
	public int getBatteryLevel() {
		return mBatteryGauge;
	}
	
	public int getBatteryChargingState() {
		return mBatteryCharging;
	}
	
	public synchronized ContentObject addBatteryObject(int state, int level) {
		Logs.d(TAG, "# Battery state changed to "+state+", level="+level);
		ContentObject obj = null;
		
		// Delete cached contents
		removeContentObject(ContentObject.CONTENT_TYPE_EMERGENCY, ContentObject.BATT_PACKAGE_NAME);	// Remove from content object list
		removeContentObject(ContentObject.CONTENT_TYPE_EMERGENCY, 
				ContentObject.BATT_PACKAGE_NAME, mEmergencyList);	// Remove from emergency list
		
		String battString = Utils.getBatteryLevelString(level);
		String strResult = applyFilters(FilterObject.FILTER_TYPE_EMERGENCY, 
				battString,				// Default message string
				ContentObject.BATT_PACKAGE_NAME);
		
		if(strResult != null && !strResult.isEmpty()) {
			obj = new ContentObject(ContentObject.CONTENT_TYPE_EMERGENCY, 
					ContentObject.EMERGENCY_ID_BATT_STATE, 		// Fixed ID
					battString, 		// Default message string
					strResult);			// Set replace message. This message will be sent to remote
			obj.mExtraData = null;
			obj.mPackageName = ContentObject.BATT_PACKAGE_NAME;
			// If no filter is applied, mFilterIcon is -1.
			if(mFilterIcon < 0)
				obj.mIconType = ContentObject.ICON_TYPE_BATT_LOW;		// set call icon
			else {
				obj.mIconType = mFilterIcon;
				obj.mIsEnabled = true;		// Enable this object (will be shown on watch)
			}
			if(level < 25) {
				obj.mIsEnabled = true;		// Enable this object (will be shown on watch)
			}
			
			mContentList.add(obj);
			mEmergencyList.add(obj);
		}
		return obj;
	}
	
	public synchronized ContentObject addRFStateObject(int state) {
		Logs.d(TAG, "# RF state changed to "+state);
		ContentObject obj = null;
		
		mRFStatus = state;
		
		// Delete cached contents
		removeContentObject(ContentObject.CONTENT_TYPE_EMERGENCY, ContentObject.TELEPHONY_RF_PACKAGE_NAME);
		removeContentObject(ContentObject.CONTENT_TYPE_EMERGENCY, 
				ContentObject.TELEPHONY_RF_PACKAGE_NAME, mEmergencyList);	// Remove from emergency list
		
		if(state == EmergencyObject.RF_STATE_IN_SERVICE)	// In service state, do not add RF contents
			return null;
		
		String strResult = applyFilters(FilterObject.FILTER_TYPE_EMERGENCY, 
				Utils.getRFTypeString(state),		// Default message string
				ContentObject.TELEPHONY_RF_PACKAGE_NAME);
		
		if(strResult != null && !strResult.isEmpty()) {
			obj = new ContentObject(ContentObject.CONTENT_TYPE_EMERGENCY, 
					ContentObject.EMERGENCY_ID_RF_STATE, 					// Fixed ID
					Utils.getRFTypeString(state), 	// Default message string
					strResult);			// Set replace message. This message will be sent to remote
			obj.mIsEnabled = true;		// Enable this object (will be shown on watch)
			obj.mPackageName = ContentObject.TELEPHONY_RF_PACKAGE_NAME;
			// If no filter is applied, mFilterIcon is -1.
			if(mFilterIcon < 0)
				obj.mIconType = ContentObject.ICON_TYPE_RF_STATE;		// set call icon
			else
				obj.mIconType = mFilterIcon;
			
			mContentList.add(obj);
			mEmergencyList.add(obj);
		}
		return obj;
	}
	
	public int getRFState() {
		return mRFStatus;
	}
	
	public synchronized ContentObject addNotification(int id, String packageName, String textTicker) {
		if(id < 0 || packageName == null || packageName.length() < 1)
			return null;
		
		// Make notification instance
		NotificationObject noti = new NotificationObject(id, packageName, textTicker);
		mNotificationList.add(noti);
		
		ContentObject content = applyFilters(noti);
		if(content == null)
			return null;

		mContentList.add(content);
		return content;
	}
	
	public synchronized void deleteNotification(int id) {
		for(int i = mNotificationList.size() - 1; i > -1; i--) {
			NotificationObject noti = mNotificationList.get(i);
			if(noti.mId == id) {
				mNotificationList.remove(i);
				removeContentObject(ContentObject.CONTENT_TYPE_NOTIFICATION, id);
			}
		}
	}
	
	public synchronized void clearAllNotifications() {
		mNotificationList.clear();
		removeContentObject(ContentObject.CONTENT_TYPE_NOTIFICATION);
	}
	
	public synchronized int addCPObject(CPObject cpo) {
		if(cpo == null || cpo.mName == null || cpo.mName.isEmpty() || cpo.mURL == null || cpo.mURL.isEmpty()) {
			return RESPONSE_INVALID_PARAMETER;
		}
		return mFeedManager.addContentProvider(cpo, true);
	}
	
	public synchronized int updateCPObject(CPObject cpo) {
		if(cpo == null || cpo.mName == null || cpo.mName.isEmpty() || cpo.mURL == null || cpo.mURL.isEmpty()) {
			return RESPONSE_INVALID_PARAMETER;
		}
		mFeedManager.updateContentProvider(cpo, true);
		return cpo.mId;
	}
	
	public synchronized int deleteCPObject(int cp_id) {
		if(cp_id < 0)
			return RESPONSE_INVALID_PARAMETER;
		
		deleteCachedFeed(cp_id);
		mFeedManager.deleteContentProvider(cp_id, true);
		return cp_id;
	}
	
	
	public synchronized int addFilter(FilterObject filter) {
		if(filter == null || filter.mType <= FilterObject.FILTER_TYPE_NONE 
				|| filter.mCompareType <= FilterObject.MATCHING_TYPE_NONE
				|| filter.mReplaceType <= FilterObject.REPLACE_TYPE_NONE
				|| filter.mOriginalString == null
				|| filter.mOriginalString.isEmpty() ) {
			return RESPONSE_INVALID_PARAMETER;
		}
		try {
			long id = mDB.insertFilter(filter);
			filter.mId = (int)id;
			mFilterList.add(filter);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return filter.mId;
	}
	
	public synchronized int editFilter(FilterObject filter) {
		if(filter.mType <= FilterObject.FILTER_TYPE_NONE 
				|| filter.mCompareType <= FilterObject.MATCHING_TYPE_NONE
				|| filter.mReplaceType <= FilterObject.REPLACE_TYPE_NONE
				|| filter.mOriginalString == null
				|| filter.mOriginalString.isEmpty() ) {
			return RESPONSE_INVALID_PARAMETER;
		}
		
		int count = mDB.updateFilter(filter);
		if(count > 0) {
			for(FilterObject object : mFilterList) {
				if(object.mId == filter.mId)
					filter.copyTo(object);
			}
		}

		return filter.mId;
	}
	
	public synchronized int deleteFilter(int filter_id) {
		if(filter_id < 0)
			return RESPONSE_INVALID_PARAMETER;
		
		// remove from cached list
		for(int i = mFilterList.size() - 1; i > -1; i--) {
			FilterObject obj = mFilterList.get(i);
			if(obj.mId == filter_id) {
				mFilterList.remove(i);
			}
		}
		
		// remove from DB
		try {
			mDB.deleteFilterWithID(filter_id);
		} catch(Exception e) {
			Logs.d(e.toString());
		}

		return filter_id;
	}
	
	private IFeedListener mFeedListener = new IFeedListener() {
		@Override
		public void OnFeedCallback(int msgType, int arg0, int arg1, String arg2, String arg3, Object arg4) {
			switch(msgType) {
			case IFeedListener.MSG_FEED_UPDATED:
				int type = arg0;
				int resultCode = arg1;
				
				if(resultCode == FeedManager.PARSING_RESULT_OK) {
					// notify to callback
					mContentManagerListener.OnContentCallback(IContentManagerListener.CALLBACK_FEED_UPDATED, type, 0, null, null, null);
				}
				break;
				
			}	// End of switch()
			
		}	// End of OnFeedCallback()
	};

	
	
	
}
