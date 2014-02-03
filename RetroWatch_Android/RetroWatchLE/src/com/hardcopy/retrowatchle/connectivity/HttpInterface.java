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

package com.hardcopy.retrowatchle.connectivity;

public interface HttpInterface {
	
	//---------- HTTP Requester status
	public static final int REQUESTER_STATUS_ERROR = -1;
	public static final int REQUESTER_STATUS_START = 1;
	public static final int REQUESTER_STATUS_BUSY = 2;
	public static final int REQUESTER_STATUS_COMPLETE = 3;
	public static final int REQUESTER_STATUS_PENDING = 4;
	
	//---------- HTTP Request message type
	public static final int MSG_HTTP_RESULT_FAIL = 0;
	public static final int MSG_HTTP_RESULT_OK = 1;
	
	//---------- HTTP response code
	public static final int MSG_HTTP_RESULT_CODE_OK = 0;
	public static final int MSG_HTTP_RESULT_CODE_TIMEOUT = 1;
	public static final int MSG_HTTP_RESULT_CODE_INVALID_URL = 2;
	public static final int MSG_HTTP_RESULT_CODE_INVALID_REQUEST = 3;
	public static final int MSG_HTTP_RESULT_CODE_SERVER_NOT_FOUND = 4;
	public static final int MSG_HTTP_RESULT_CODE_INTERNAL_SERVER_ERROR = 5;
	public static final int MSG_HTTP_RESULT_CODE_ERROR_UNKNOWN = 6;
	public static final int MSG_HTTP_RESULT_CODE_ERROR_REQUEST_EXCEPTION = 7;
	
	//---------- Request type (GET or POST or FILE)
	public static final int REQUEST_TYPE_GET = 1;
	public static final int REQUEST_TYPE_POST = 2;
	public static final int REQUEST_TYPE_FILE = 3;
	
	public static final String REQUEST_TYPE_GET_STRING = "GET";
	public static final String REQUEST_TYPE_POST_STRING = "POST";
	
	//---------- HTTP Request encoding type
	public static final String ENCODING_TYPE_EUC_KR = "euc-kr";
	public static final String ENCODING_TYPE_UTF_8 = "utf-8";
	
	
}
