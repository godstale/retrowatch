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

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.hardcopy.retrowatchle.contents.objects.CPObject;
import com.hardcopy.retrowatchle.contents.objects.FeedObject;
import com.hardcopy.retrowatchle.utils.Logs;

import android.text.Html;
import android.util.Log;

public class FeedParser {

	// Global
	public static final String TAG = "FeedParser";
	
	//---------- Parsing tags
	private static final String PARSING_TAG_ID = "id";
	private static final String PARSING_TAG_WORD = "word";
	private static final String PARSING_TAG_RANK = "rank";
	private static final String PARSING_TAG_KEYWORD = "keyword";
	private static final String PARSING_TAG_VALUE = "value";
	private static final String PARSING_TAG_TYPE = "type";
	private static final String PARSING_TAG_LINK = "link";
	private static final String PARSING_TAG_LINKS = "links";
	private static final String PARSING_TAG_ORIGINAL_LINK = "originallink";
	private static final String PARSING_TAG_LINKURL = "linkurl";
	private static final String PARSING_TAG_URL = "url";
	private static final String PARSING_TAG_CONTENT = "content";
	private static final String PARSING_TAG_RESULT = "result";
	private static final String PARSING_TAG_RESULTS = "results";
	private static final String PARSING_TAG_ITEM = "item";
	private static final String PARSING_TAG_SOCIALPICK = "socialpick";
	private static final String PARSING_TAG_RANKED_TWIT_LIST = "rankedTwitList";
	private static final String PARSING_TAG_OWNER = "owner";
	private static final String PARSING_TAG_BODY = "body";
	private static final String PARSING_TAG_IMAGE = "image";
	private static final String PARSING_TAG_IMAGES = "images";
	private static final String PARSING_TAG_THUMBNAIL_URL = "thumbnailUrl";
	private static final String PARSING_TAG_CHANNEL = "channel";
	private static final String PARSING_TAG_TITLE = "title";
	private static final String PARSING_TAG_DESCRIPTION = "description";
	private static final String PARSING_TAG_FROM_USER_NAME = "from_user_name";
	private static final String PARSING_TAG_TEXT = "text";
	private static final String PARSING_TAG_DATA = "data";
	private static final String PARSING_TAG_CAPTION = "caption";
	private static final String PARSING_TAG_SMALL = "small";
	private static final String PARSING_TAG_NORMAL = "normal";
	private static final String PARSING_TAG_NEW = "new";
	private static final String PARSING_TAG_PP = "++";
	private static final String PARSING_TAG_COMMENT_CNT = "comment_cnt";
	private static final String PARSING_TAG_AUTHOR = "author";
	private static final String PARSING_TAG_PUBDATE = "pubDate";
	private static final String PARSING_TAG_UPDATED = "a10:updated";
	private static final String PARSING_TAG_NOTICE = "notice";
	private static final String PARSING_TAG_APP_VERSION = "appversion";
	private static final String PARSING_TAG_GUID = "guid";
	private static final String PARSING_TAG_THUMBNAIL = "thumbnail";
	private static final String PARSING_TAG_ENCLOSURE = "enclosure";
	
	private static final String PARSING_TAG_K = "K";
	private static final String PARSING_TAG_S = "S";
	private static final String PARSING_TAG_V = "V";
	
	public static final int RANK_TYPE_NONE = 0;
	public static final int RANK_TYPE_NEW = 1;
	
	public static final int RANK_MODIFIER_DAUM_REALTIME_KEYWORD = 10;
	public static final int RANK_MODIFIER_NAVER_REALTIME_KEYWORD = 100;
	public static final int RANK_MODIFIER_DAUM_SOCIALPICK = 1000;
	
	public static final String ENCODING_TYPE_EUC_KR = "euc-kr";
	public static final String ENCODING_TYPE_UTF_8 = "utf-8";
	
	private static final int PARSER_ID_SUBSTRING_MAX = 240;
	
	private static final String REG_EXP_REMOVE_TAG = "\\<.*?\\>";
	private static final String REG_EXP_URL = "((http|https|ftp|mms):\\/\\/[0-9a-z-]+(\\.[_0-9a-z-]+)+(:[0-9]{2,4})?\\/?)";
	private static final String REG_EXP_IMAGE_URL = "(?i)http://[a-zA-Z0-9_.\\-%&=?!:;@\"'/]*(?i)(.gif|.jpg|.png|.jpeg)";
	private static final String REG_EXP_REMOVE_NEWLINE = "\\n\\n";
	private static final String STRING_RSS_SPLIT_TAG = "\\[\\[:\\+:\\]\\]";
	
	// Context, system
	
	
	// Constructor
	public FeedParser() {}
	
	
	/*****************************************************
	 *		Public methods
	 ******************************************************/
	
	public ArrayList<FeedObject> parseResultString(CPObject CpObj, String strResult)
	{
		if(CpObj == null) return null;
		
		int type = CpObj.mId;
		ArrayList<FeedObject> feedList = new ArrayList<FeedObject>();
		
		Logs.d(TAG, "# Parsing string :: CP type = "+CpObj.mId+", parsing type = "+CpObj.mParsingType);
		
		switch(CpObj.mParsingType)
		{
			case FeedObject.REQUEST_TYPE_DAUM_REALTIME_KEYWORDS:
			{
				try{
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = factory.newDocumentBuilder();
					
					InputSource is = new InputSource();
					is.setCharacterStream(new StringReader(strResult));
					
					Document doc = db.parse(is); 
					Element element = doc.getDocumentElement();

					NodeList items = element.getElementsByTagName(PARSING_TAG_WORD);	// <realtime/>[ <word/> {<rank/><keyword/><value/><type/><linkurl/>} ]

					for(int i=0 ; i < items.getLength() ; i++)
					{
						String link = null;
						String keyword = null;
						String content = null;
						String thumbnail = null;
						int rankType = RANK_TYPE_NONE;
						int rankUpAndDown = 0;
						int commentCount = 0;
						
						Node item = items.item(i);					// 알고 싶은 Node의 인덱스 번호를 전달한다.
						NodeList children = item.getChildNodes();	// 자식 Node를 가져옴
						
						for(int j=0; j < children.getLength(); j++)
						{
							Node child = children.item(j);
							String nodeName = child.getNodeName();
							if(nodeName.equalsIgnoreCase(PARSING_TAG_KEYWORD)) {
								String temp = child.getFirstChild().getNodeValue();
								if(temp != null)
									keyword = new String( temp.getBytes() );
							}
							else if(nodeName.equalsIgnoreCase(PARSING_TAG_LINKURL)) {
								String temp = child.getFirstChild().getNodeValue();
								if(temp != null)
									link = new String( temp.getBytes(), ENCODING_TYPE_UTF_8 );
							}
							else if(nodeName.equalsIgnoreCase(PARSING_TAG_TYPE)) {
								String temp = child.getFirstChild().getNodeValue();
								if(temp != null) {
									String temp2 = new String( temp.getBytes(), ENCODING_TYPE_UTF_8 );		// <type> value : "new" or "++"
									if(temp2.equalsIgnoreCase(PARSING_TAG_NEW))	
										rankType = RANK_TYPE_NEW;
								}
							}
							else if(nodeName.equalsIgnoreCase(PARSING_TAG_VALUE)) {
								try {
									String temp = child.getFirstChild().getNodeValue();
									if(temp != null && temp.length() > 0) {
										String temp2 = new String( temp.getBytes(), ENCODING_TYPE_UTF_8 );	// <value> : 
										rankUpAndDown = Integer.parseInt(temp2) / RANK_MODIFIER_DAUM_REALTIME_KEYWORD;	// TODO:
										if( rankUpAndDown > 10 ) rankUpAndDown = 10;
									}
								} catch (Exception e) {}
							}
						}	// End of for loop
						
						if(keyword!=null && link!=null) 
						{
							String idStr = keyword;
							FeedObject feed = new FeedObject(type, idStr, link, keyword, null, null);
							feed.mDownloadStatus = FeedObject.CONTENT_DOWNLOAD_STATUS_INIT;
							feed.setRankInfo(rankType, rankUpAndDown, commentCount);
							feedList.add(feed);
						}
						
						if(i >= CpObj.mCachingCount - 1) {		// Break loop if count has reached caching count
							break;
						}
					}	// End of for loop

				}catch (Exception e) {
					Logs.d(TAG, e.getMessage()==null ? "Unknown error while parsing xml" : e.getMessage() );
					e.printStackTrace();
					feedList = null;
				}
				break;
			}
			
			case FeedObject.REQUEST_TYPE_NAVER_REALTIME_KEYWORDS:
			{
				try{
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = factory.newDocumentBuilder();
					
					InputSource is = new InputSource();
					is.setCharacterStream(new StringReader(strResult));
					
					Document doc = db.parse(is); 
					Element element = doc.getDocumentElement();

					NodeList temp = element.getElementsByTagName(PARSING_TAG_ITEM);		// result -> item 
					NodeList items = temp.item(0).getChildNodes();			// List contains R1, R2...

					//반복문을 돌면서 각각의 Node 읽어오기
					for(int i=0 ; i < items.getLength() ; i++)
					{
						String link = null;
						String keyword = null;
						String content = null;
						String thumbnail = null;
						int rankType = RANK_TYPE_NONE;
						int rankUpAndDown = 0;
						int commentCount = 0;
						
						Node item = items.item(i);								// <Rn> : 	n-th R tag
						NodeList children = item.getChildNodes();		// <k> <s> <v>
						
						for(int j=0; j < children.getLength(); j++)
						{
							Node child = children.item(j);
							String nodeName = child.getNodeName();
							if(nodeName.equalsIgnoreCase(PARSING_TAG_K)) {
								String tempStr = child.getFirstChild().getNodeValue();
								if(tempStr != null)
									keyword = new String( tempStr.getBytes() );
							}
							else if(nodeName.equalsIgnoreCase(PARSING_TAG_V)) {
								String tempStr = child.getFirstChild().getNodeValue();
								rankUpAndDown = Integer.parseInt(tempStr) / RANK_MODIFIER_NAVER_REALTIME_KEYWORD;	// TODO: 
								if( rankUpAndDown > 10 ) rankUpAndDown = 10;
							}
						}
						
						if(keyword!=null) 
						{
							String idStr = keyword;
							FeedObject feed = new FeedObject(type, idStr, null, keyword, null, null);
							feed.mDownloadStatus = FeedObject.CONTENT_DOWNLOAD_STATUS_INIT;
							feed.setRankInfo(rankType, rankUpAndDown, commentCount);
							feedList.add(feed);
						}
						
						if(i >= CpObj.mCachingCount - 1) {		// Break loop if count has reached caching count
							break;
						}
					}	// End of for loop
					
				}catch (Exception e) {
					Logs.d(TAG, e.getMessage()==null ? "Unknown error while parsing xml" : e.getMessage() );
					e.printStackTrace();
					feedList = null;
				}
				break;
			}
			
			case FeedObject.REQUEST_TYPE_DAUM_SOCIAL_PICK:
			{
				try {
					JSONObject	jsonRoot = new JSONObject(strResult);		// root { 
					JSONObject	jsonObjSocialPick = jsonRoot.getJSONObject(PARSING_TAG_SOCIALPICK);		// { socialpick :
					JSONArray 	jsonArray = jsonObjSocialPick.getJSONArray(PARSING_TAG_ITEM);			// {item: [
					
					for(int i=0; i<jsonArray.length(); i++)
					{
						String link = null;
						String keyword = null;
						String content = null;
						String thumbnail = null;
						int rankType = RANK_TYPE_NONE;
						int rankUpAndDown = 0;
						int commentCount = 0;
						
						JSONObject obj = jsonArray.getJSONObject(i);		// {rank:1, link:"xx", keyword:"xx", content:"xx", count:30000, quotation_cnt:1345, comment_cnt:13816, rank_diff:3, category:"c" }
						link = obj.getString(PARSING_TAG_LINK);
						keyword = obj.getString(PARSING_TAG_KEYWORD);
						content = obj.getString(PARSING_TAG_CONTENT);
						commentCount = obj.getInt(PARSING_TAG_COMMENT_CNT) / RANK_MODIFIER_DAUM_SOCIALPICK;
						commentCount = commentCount * 2;					// To encourage rank priority
						if( commentCount > 10 ) commentCount = 10;
						
						if(link!=null && keyword!=null && content!=null) 
						{
							String idStr = keyword;
							FeedObject feed = new FeedObject(type, idStr, link, keyword, content, null);
							feed.mDownloadStatus = FeedObject.CONTENT_DOWNLOAD_STATUS_INIT;
							feed.setRankInfo(rankType, rankUpAndDown, commentCount);
							feedList.add(feed);
						}
						
						if(i >= CpObj.mCachingCount - 1) {		// Break loop if count has reached caching count
							break;
						}
					}	// End of for loop
				} catch (JSONException e) {
					Logs.d(TAG, "##### JSON Parsing stopped with error :");
					e.printStackTrace();
					feedList = null;
				}
				break;
			}
			
			case FeedObject.REQUEST_TYPE_TWITTER_HOTTEST:
			case FeedObject.REQUEST_TYPE_TWITTER_REALTIME:
			case FeedObject.REQUEST_TYPE_TWITTER_TODAY:
			case FeedObject.REQUEST_TYPE_TWITTER_IMAGE:
			{
				String link = null;
				String keyword = null;
				String content = null;
				String thumbnail = null;
				
				try {
					JSONObject	jsonRoot = new JSONObject(strResult);		// root { 
					JSONArray	jsonObjList = jsonRoot.getJSONArray(PARSING_TAG_RANKED_TWIT_LIST);	// { rankedTwitList : [
					
					for(int i=0; i<jsonObjList.length(); i++)
					{
						JSONObject obj = jsonObjList.getJSONObject(i);		// id, owner, body, rtRank, rtCount, registDate, links:{}, rtTwitCount, twitId
						
						//---------- make content
						String idStr = obj.getString(PARSING_TAG_ID);
						String name = obj.getString(PARSING_TAG_OWNER);
						content = obj.getString(PARSING_TAG_BODY);

						JSONObject linkObj = obj.getJSONObject(PARSING_TAG_LINKS);
						if(linkObj != null) {
							try{			// If there is no <image> object, skip below routine
								JSONArray imageObjList = linkObj.getJSONArray(PARSING_TAG_IMAGE);
								if(imageObjList != null) {
									// Multiple images can be found, but we'll take only first one.
									JSONObject objImg = imageObjList.getJSONObject(0);
									thumbnail = objImg.getString(PARSING_TAG_THUMBNAIL_URL);
									link = objImg.getString(PARSING_TAG_URL);
								}
							} catch(Exception e) {}
						}
						
						if(idStr != null && content != null) 
						{
							FeedObject feed = new FeedObject(type, idStr, link, keyword, content, thumbnail);
							if(name != null)
								feed.setName(name);
							else Log.d(TAG, "+++ Parsing :: id is null");
							
							feed.mDownloadStatus = FeedObject.CONTENT_DOWNLOAD_STATUS_INIT;
							feedList.add(feed);
						}
						
						if(i >= CpObj.mCachingCount - 1) {		// Break loop if count has reached caching count
							break;
						}
					}	// End of for loop
				} catch (JSONException e) {
					Logs.d(TAG, "##### JSON Parsing stopped with error :");
					e.printStackTrace();
					feedList = null;
				}
				break;
			}
			
			case FeedObject.REQUEST_TYPE_NAVER_RELATED_KEYWORDS:
			{
				try{
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = factory.newDocumentBuilder();
					
					InputSource is = new InputSource();
					is.setCharacterStream(new StringReader(strResult));
					
					// Document와 Element 는 w3c dom에 있는것을 임포트 한다.
					Document doc = db.parse(is); 
					Element element = doc.getDocumentElement();

					NodeList temp = element.getElementsByTagName(PARSING_TAG_ITEM);		// result -> item 

					//반복문을 돌면서 각각의 Node 읽어오기
					for(int i=0 ; i < temp.getLength() ; i++)
					{
						String keyword = null;
						Node item = temp.item(i);
						String tempStr = item.getFirstChild().getNodeValue();
						if(tempStr != null && tempStr.length() > 0)
							keyword = new String( tempStr.getBytes() );
						
						if(keyword!=null) 
						{
							FeedObject feed = new FeedObject(type, keyword, null, keyword, null, null);
							feedList.add(feed);
						}
						
						if(i >= CpObj.mCachingCount - 1) {		// Break loop if count has reached caching count
							break;
						}
					}	// End of for loop

				}catch (Exception e) {
					Logs.d(TAG, e.getMessage()==null ? "Unknown error while parsing xml" : e.getMessage() );
					e.printStackTrace();
					feedList = null;
				}
				break;
			}
			
			case FeedObject.REQUEST_TYPE_9GAG_HOT:
			case FeedObject.REQUEST_TYPE_9GAG_TREND:
			{
				try {
					JSONObject	jsonRoot = new JSONObject(strResult);				// root { 
					JSONArray 	jsonArray = jsonRoot.getJSONArray(PARSING_TAG_DATA);			// {data: [
					
					long current = System.currentTimeMillis();
					for(int i=0; i<jsonArray.length(); i++)
					{
						String id = null;
						String link = null;
						String keyword = null;
						String content = null;
						String thumbnail = null;
						String fullSizeImage = null;
						// {id:xx, from:{name:xx}, caption:xx, images:{small:xx, normal:xx, large:xx}, link:xx, action:{like:xx, dislike:xx, unlike:xx}, vote:{count:xx}
						JSONObject obj = jsonArray.getJSONObject(i); 
						id = obj.getString(PARSING_TAG_ID);
						link = obj.getString(PARSING_TAG_LINK);
						keyword = obj.getString(PARSING_TAG_CAPTION);
						JSONObject objImage = obj.getJSONObject(PARSING_TAG_IMAGES);
						thumbnail = objImage.getString(PARSING_TAG_SMALL);
						fullSizeImage = objImage.getString(PARSING_TAG_NORMAL);
						
						if(id != null && link != null && keyword != null && thumbnail != null) 
						{
							FeedObject feed = new FeedObject(type, id, link, keyword, null, thumbnail);
							feed.setFullSizeImageURL(fullSizeImage);
							feedList.add(feed);
						}
						
						if(i >= CpObj.mCachingCount - 1) {		// Break loop if count has reached caching count
							break;
						}
					}	// End of for loop
				} catch (JSONException e) {
					Logs.d(TAG, "##### JSON Parsing stopped with error :");
					e.printStackTrace();
					feedList = null;
				}
				break;
			}
			
			case FeedObject.REQUEST_TYPE_NOTICE:
			{
				try {
					JSONObject	jsonRoot = new JSONObject(strResult);				// root { 
					JSONArray 	jsonArray = jsonRoot.getJSONArray(PARSING_TAG_NOTICE);			// {notice: [
					
					long current = System.currentTimeMillis();
					for(int i=0; i<jsonArray.length(); i++)
					{
						int version = 0;
						String id = null;
						String link = null;
						String name = null;
						String keyword = null;
						String content = null;
						String thumbnail = null;
						String fullSizeImage = null;
						// {title:XXX, LINK:XXX, description:XXX, author:XXX, appversion:XXX, pubDate:XXX
						JSONObject obj = jsonArray.getJSONObject(i); 

						keyword = obj.getString(PARSING_TAG_TITLE);
						link = obj.getString(PARSING_TAG_LINK);
						content = obj.getString(PARSING_TAG_DESCRIPTION);
						name = obj.getString(PARSING_TAG_AUTHOR);
						version = obj.getInt(PARSING_TAG_APP_VERSION);
						id = obj.getString(PARSING_TAG_PUBDATE);
						
						if(id != null && link != null && keyword != null) 
						{
							FeedObject feed = new FeedObject(type, id, link, keyword, content, null);
							feed.setName(name);
							feed.setDate(id);
							feed.setVersion(version);
							feedList.add(feed);
						}
						
						if(i >= CpObj.mCachingCount - 1) {		// Break loop if count has reached caching count
							break;
						}
					}	// End of for loop
				} catch (JSONException e) {
					Logs.d(TAG, "##### JSON Parsing stopped with error :");
					e.printStackTrace();
					feedList = null;
				}
				break;
			}
			
			case FeedObject.REQUEST_TYPE_RSS_DEFAULT:
			case FeedObject.REQUEST_TYPE_RSS_FEED43:
			{
				try{
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = factory.newDocumentBuilder();
					
					InputSource is = new InputSource();
					is.setCharacterStream(new StringReader(strResult));
					
					// Document와 Element 는 w3c dom에 있는것을 임포트 한다.
					Document doc = db.parse(is);
					Element element = doc.getDocumentElement();

					// Extract logo image
					try {
						NodeList logoImage = element.getElementsByTagName(PARSING_TAG_IMAGE);
						if(logoImage != null && logoImage.getLength() > 0) {
							Node logoitem = logoImage.item(0);
							NodeList logochildren = logoitem.getChildNodes();
							for(int j=0; j < logochildren.getLength(); j++) {
								Node logochild = logochildren.item(j);
								String logoName = logochild.getNodeName();
								if(logoName.equalsIgnoreCase(PARSING_TAG_URL)) {
									String tempStr = logochild.getFirstChild().getNodeValue();
									if(tempStr != null && tempStr.length() > 0)
										CpObj.mLogoImage = tempStr;
								}
							}
						}
					} catch(Exception e) {}
					
					NodeList temp = element.getElementsByTagName(PARSING_TAG_ITEM);		// channel -> item 

					long current = System.currentTimeMillis();
					for(int i=0 ; i < temp.getLength() ; i++)
					{
						int version = 0;
						String guid = null;
						String name = null;
						String date = null;
						String link = null;
						String keyword = null;
						String content = null;
						String thumbnail = null;
						Node item = temp.item(i);							// <item> : 	n-th item tag
						NodeList children = item.getChildNodes();			// <title> <author> <link> <description> <pubDate>
						
						for(int j=0; j < children.getLength(); j++)
						{
							try {		// If error occurs in this block, skip current item parsing and go next
								Node child = children.item(j);
								String nodeName = child.getNodeName();
								if(nodeName.equalsIgnoreCase(PARSING_TAG_LINK)) {
									String tempStr = child.getFirstChild().getNodeValue();
									if(tempStr != null)
										link = new String( tempStr.getBytes() );
								}
								else if(nodeName.equalsIgnoreCase(PARSING_TAG_TITLE)) {
									String tempStr = child.getFirstChild().getNodeValue();
									if(tempStr != null) {
										keyword = new String( tempStr.getBytes() );
									}
								} 
								else if(nodeName.equalsIgnoreCase(PARSING_TAG_DESCRIPTION)) {
									String tempStr = child.getFirstChild().getNodeValue();
									if(tempStr != null) {
										String[] strArray = tempStr.split(STRING_RSS_SPLIT_TAG);
										if(strArray[0] != null && strArray[0].length() > 0) {
											strArray[0] = Html.fromHtml(strArray[0]).toString();
											strArray[0] = strArray[0].replaceAll(REG_EXP_REMOVE_TAG, "");
											strArray[0] = strArray[0].replaceAll(REG_EXP_REMOVE_NEWLINE, "");
											content = strArray[0];
										}
										if(strArray.length > 1 && strArray[1] != null && strArray[1].length() > 0) {
											thumbnail = strArray[1];
										}
									}
								}
								else if(nodeName.equalsIgnoreCase(PARSING_TAG_AUTHOR)) {
									if(child != null && child.getFirstChild() != null) {
										String tempStr = child.getFirstChild().getNodeValue();
										if(tempStr != null)
											name = new String( tempStr.getBytes() );
									}
								}
								else if(nodeName.equalsIgnoreCase(PARSING_TAG_PUBDATE)) {
									if(child != null && child.getFirstChild() != null) {
										String tempStr = child.getFirstChild().getNodeValue();
										if(tempStr != null)
											date = new String( tempStr.getBytes() );
									}
								}
								else if(nodeName.equalsIgnoreCase(PARSING_TAG_GUID)) {
									if(child != null && child.getFirstChild() != null) {
										String tempStr = child.getFirstChild().getNodeValue();
										if(tempStr != null)
											guid = new String( tempStr.getBytes() );
									}
								}
								else if(nodeName.equalsIgnoreCase(PARSING_TAG_APP_VERSION)) {
									if(child != null && child.getFirstChild() != null) {
										String tempStr = child.getFirstChild().getNodeValue();
										if(tempStr != null && tempStr.length() > 0)
											version = Integer.parseInt(tempStr);
									}
								}
								else if(nodeName.contains(PARSING_TAG_THUMBNAIL)) {
									try {
										if(child != null && child.hasAttributes()) {
											NamedNodeMap attrs = child.getAttributes();
											for(int k=0; k<attrs.getLength(); k++) {
												Node attr = attrs.item(k);
												if(attr.getNodeName().equalsIgnoreCase(PARSING_TAG_URL)) {
													thumbnail = attr.getNodeValue();
												}
											}
										}
									} catch(Exception e) {
										thumbnail = null;
									}

								}
								else if(nodeName.contains(PARSING_TAG_ENCLOSURE)) {
									try {
										if(child != null && child.hasAttributes()) {
											NamedNodeMap attrs = child.getAttributes();
											for(int k=0; k<attrs.getLength(); k++) {
												Node attr = attrs.item(k);
												if(attr.getNodeName().equalsIgnoreCase(PARSING_TAG_URL)) {
													thumbnail = attr.getNodeValue();
												}
											}
										}
									} catch(Exception e) {
										thumbnail = null;
									}

								}
								
							}
							catch(Exception e) {
								Logs.d(TAG, e.getMessage()==null ? "Unknown error while parsing xml" : e.getMessage() );
								e.printStackTrace();
							}
							
						}	// End of for loop.... parsing each item
						
						if(link != null && keyword != null && content != null) 
						{
							StringBuilder sb = new StringBuilder();
							if(guid != null) {
								sb.append("rss_").append(removeSpecialChars(guid));
							} 
							else if(date != null) {
								sb.append("rss_").append(date.replace(",", "").replace(":", "").trim());
							} else {
								String temp1 = null;
								if(keyword.length() > 50) temp1 = keyword.substring(0, 50);
								else temp1 = keyword;
								temp1 = URLEncoder.encode(temp1, ENCODING_TYPE_UTF_8).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
								if(temp1.length() > PARSER_ID_SUBSTRING_MAX)
									temp1 = temp1.substring(0, PARSER_ID_SUBSTRING_MAX - 1);
								temp1 = temp1.trim();
								sb.append("rss_").append(temp1);
							}
							
							FeedObject feed = new FeedObject(type, sb.toString(), link, keyword, content, thumbnail);
							feed.mDownloadStatus = FeedObject.CONTENT_DOWNLOAD_STATUS_INIT;
							feed.setDate(date);
							feed.setName(name);
							if(CpObj.mParsingType == FeedObject.REQUEST_TYPE_NOTICE && version > 0)
								feed.setVersion(version);
							feedList.add(feed);
						}
						
						if(i >= CpObj.mCachingCount - 1) {		// Break loop if count has reached caching count
							break;
						}
					}	// End of for loop

				}catch (Exception e) {
					Logs.d(TAG, e.getMessage()==null ? "Unknown error while parsing xml" : e.getMessage() );
					e.printStackTrace();
					feedList = null;
				}
				break;
			}	// End of default block

		}	// End of switch(type)
		
		return feedList;
		
	}	// End of parseResultString()
	
	public static String removeSpecialChars(String str) {
		String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
		str =str.replaceAll(match, " ");
		return str;
	}

	
}

