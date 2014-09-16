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

public class Constants {

	
	// Notification intent action string
	public static final String NOTIFICATION_LISTENER_SERVICE = "com.hardcopy.retrowatch.NOTIFICATION_LISTENER_SERVICE";
	public static final String NOTIFICATION_LISTENER = "com.hardcopy.retrowatch.NOTIFICATION_LISTENER";
	
	// Service handler message key
	public static final String SERVICE_HANDLER_MSG_KEY_DEVICE_NAME = "device_name";
	public static final String SERVICE_HANDLER_MSG_KEY_DEVICE_ADDRESS = "device_address";
	public static final String SERVICE_HANDLER_MSG_KEY_TOAST = "toast";
	
	// Intent request codes
	public static final int REQUEST_CONNECT_DEVICE = 1;
	public static final int REQUEST_ENABLE_BT = 2;
	
    // Message types sent from Service to Activity
    public static final int MESSAGE_CMD_ERROR_NOT_CONNECTED = -50;
    
    public static final int MESSAGE_BT_STATE_INITIALIZED = 1;
    public static final int MESSAGE_BT_STATE_LISTENING = 2;
    public static final int MESSAGE_BT_STATE_CONNECTING = 3;
    public static final int MESSAGE_BT_STATE_CONNECTED = 4;
    public static final int MESSAGE_BT_STATE_ERROR = 10;
    
    public static final int MESSAGE_ADD_NOTIFICATION = 101;
    public static final int MESSAGE_DELETE_NOTIFICATION = 105;
    public static final int MESSAGE_GMAIL_UPDATED = 111;
    public static final int MESSAGE_SMS_RECEIVED = 121;
    public static final int MESSAGE_CALL_STATE_RECEIVED = 131;
    public static final int MESSAGE_RF_STATE_RECEIVED = 141;
    public static final int MESSAGE_FEED_UPDATED = 151;
    
    public static final int RESPONSE_ADD_FILTER_FAILED = -1;
    public static final int RESPONSE_EDIT_FILTER_FAILED = -1;
    public static final int RESPONSE_DELETE_FILTER_FAILED = -1;
    
    public static final int RESPONSE_ADD_RSS_FAILED = -1;
    public static final int RESPONSE_EDIT_RSS_FAILED = -1;
    public static final int RESPONSE_DELETE_RSS_FAILED = -1;
    
    
    // Preference
	public static final String PREFERENCE_NAME = "RetroWatchPref";
	
	public static final String PREFERENCE_CONN_INFO_ADDRESS = "device_address";
	public static final String PREFERENCE_CONN_INFO_NAME = "device_name";
	
	public static final String PREFERENCE_KEY_LAST_LAST_INIT_TIME = "LastInitData";
	public static final String PREFERENCE_KEY_IS_FIRST_EXEC = "IsFirstExec";
	public static final String PREFERENCE_KEY_GMAIL_ADDRESS = "GmailAddress";
	public static final String PREFERENCE_KEY_RUN_IN_BG = "RunInBackground";
    
	//---------- Request parameter
	public static final String REQUEST_URL_NOTICE = "http://tortuga.ipdisk.co.kr/search/hotclip_notice_json.php";
	public static final String REQUEST_URL_DAUM_REALTIME_KEYWORDS = "http://img.search.hanmail.net/jumpkeyword/API/REALTIME_ISSUE_TOTAL.xml";
	public static final String REQUEST_URL_NAVER_REALTIME_KEYWORDS = "http://openapi.naver.com/search?key=eb694e29be59aefd8a6eff7af02c5456&query=nexearch&target=rank";
	public static final String REQUEST_URL_DAUM_SOCIAL_PICK = "http://apis.daum.net/socialpick/search?n=20&output=json";

	public static final String REQUEST_URL_TWITTER_HOTTEST = "http://www.followkr.com/rank/hot.php?";
	public static final String REQUEST_URL_TWITTER_REALTIME = "http://www.followkr.com/rank/realtime.php?";
	public static final String REQUEST_URL_TWITTER_TODAY = "http://www.followkr.com/rank/today.php?";
	public static final String REQUEST_URL_TWITTER_IMAGE = "http://www.followkr.com/rank/image.php?";
	
	public static final String REQUEST_URL_NAVER_RELATED_KEYWORD = "http://openapi.naver.com/search?key=eb694e29be59aefd8a6eff7af02c5456&target=recmd&query=";
	public static final String REQUEST_URL_DAUM_WEB_SEARCH = "http://apis.daum.net/search/web?apikey=91e2de5c56bf93d4e5573a9664b07eb1362d3ce2&result=10&pageno=1&output=json&q=";
	public static final String REQUEST_URL_TWITTER_SEARCH = "http://search.twitter.com/search.json?q=";
	public static final String REQUEST_URL_NAVER_SEARCH_NEWS = "http://openapi.naver.com/search?target=news&start=1&display=10&key=eb694e29be59aefd8a6eff7af02c5456&query=";
	
	public static final String REQUEST_URL_9GAG_HOT = "http://infinigag.eu01.aws.af.cm/2/hot/0.json";
	public static final String REQUEST_URL_9GAG_TREND = "http://infinigag.eu01.aws.af.cm/2/trending/0.json";
	
	public static final String REQUEST_URL_CLIEN_IT_NEWS = "http://feeds.feedburner.com/Clien--news?format=xml";		// "http://tortuga.ipdisk.co.kr/search/rss_today_book.php";
	public static final String REQUEST_URL_DAUM_BEST_MOVIE = "http://tvpot.daum.net/rss/RssBestDay.do?range=0";		// "http://ch.yes24.com/feed/column/2178";
	public static final String REQUEST_URL_SBS_SPORTS = "http://api.sbs.co.kr/xml/news/rss.jsp?pmDiv=sports";
	public static final String REQUEST_URL_SBS_CULTURE = "http://api.sbs.co.kr/xml/news/rss.jsp?pmDiv=culture";

	public static final String REQUEST_URL_DAUM_MOVIE = "http://feed43.com/2562637706132070.xml";
	public static final String REQUEST_URL_DAUM_CAR = "http://feed43.com/1675621482076855.xml";
	public static final String REQUEST_URL_DAUM_FOOD = "http://feed43.com/2677154471362207.xml";
	public static final String REQUEST_URL_DAUM_BOOK = "http://feed43.com/3614346481548243.xml";
	
	public static final String REQUEST_URL_NEWS_SBS = "http://api.sbs.co.kr/xml/news/rss.jsp?pmDiv=external";
	public static final String REQUEST_URL_NEWS_MBC = "http://imnews.imbc.com/rss/news/news_00.xml";
	public static final String REQUEST_URL_NEWS_HKR = "http://www.hani.co.kr/rss";
	
	public static final String CP_DAUM_URL = "http://www.daum.net";
	public static final String CP_NAVER_URL = "http://www.naver.com";
	public static final String CP_9GAG_URL = "http://9gag.com";
	public static final String CP_CLIEN_URL = "http://m.clien.net/cs3/";
	public static final String CP_DAUM_MOVIE_URL = "http://tvpot.daum.net/best/Top.do";
	public static final String CP_SBS_URL = "http://www.sbs.co.kr";
    
	//---------- Web view
	public static final String PROJECT_FEED_SEARCH_URL = "http://tortuga.ipdisk.co.kr/hotclip/";
    
}
