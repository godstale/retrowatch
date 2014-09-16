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

package com.hardcopy.retrowatchle;

public interface IFragmentListener {
	public static final int CALLBACK_REQUEST_FILTERS = 1;
	public static final int CALLBACK_REQUEST_ADD_FILTER = 2;
	public static final int CALLBACK_REQUEST_EDIT_FILTER = 3;
	public static final int CALLBACK_REQUEST_DELETE_FILTER = 4;
	
	public static final int CALLBACK_REQUEST_RSS = 11;
	public static final int CALLBACK_REQUEST_ADD_RSS = 12;
	public static final int CALLBACK_REQUEST_EDIT_RSS = 13;
	public static final int CALLBACK_REQUEST_DELETE_RSS = 14;
	
	public static final int CALLBACK_REQUEST_SET_EMAIL_ADDRESS = 21;
	public static final int CALLBACK_REQUEST_CLOCK_STYLE = 23;
	public static final int CALLBACK_REQUEST_SHOW_INDICATOR = 24;
	public static final int CALLBACK_REQUEST_RUN_IN_BACKGROUND = 25;
	
	public void OnFragmentCallback(int msgType, int arg0, int arg1, String arg2, String arg3, Object arg4);
}
