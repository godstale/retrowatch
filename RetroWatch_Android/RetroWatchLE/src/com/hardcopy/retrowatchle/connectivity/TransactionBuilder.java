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

import java.util.Calendar;

import android.os.Handler;

import com.hardcopy.retrowatchle.utils.Constants;
import com.hardcopy.retrowatchle.utils.Logs;

public class TransactionBuilder {
	private static final String TAG = "TransactionBuilder";
	
	private BluetoothManager mBTManager = null;
	private Handler mHandler = null;
	
	public TransactionBuilder(BluetoothManager bm, Handler errorHandler) {
		mBTManager = bm;
		mHandler = errorHandler;
	}
	
	public Transaction makeTransaction() {
		return new Transaction();
	}
	
	public class Transaction {
		
		public static final int MAX_MESSAGE_LENGTH = 16;
		
		// Command types
		public static final int COMMAND_TYPE_NONE = 0x00;
		public static final int COMMAND_TYPE_RESET_EMERGENCY_OBJ = 0x05;
		public static final int COMMAND_TYPE_RESET_NORMAL_OBJ = 0x02;
		public static final int COMMAND_TYPE_RESET_USER_MESSAGE = 0x03;
		
		public static final int COMMAND_TYPE_ADD_EMERGENCY_OBJ = 0x11;
		public static final int COMMAND_TYPE_ADD_NORMAL_OBJ = 0x12;
		public static final int COMMAND_TYPE_ADD_USER_MESSAGE = 0x13;
		
		public static final int COMMAND_TYPE_DELETE_EMERGENCY_OBJ = 0x21;
		public static final int COMMAND_TYPE_DELETE_NORMAL_OBJ = 0x22;
		public static final int COMMAND_TYPE_DELETE_USER_MESSAGE = 0x23;
		
		public static final int COMMAND_TYPE_SET_TIME = 0x31;
		public static final int COMMAND_TYPE_REQUEST_MOVEMENT_HISTORY = 0x32;
		public static final int COMMAND_TYPE_SET_CLOCK_STYLE = 0x33;
		public static final int COMMAND_TYPE_SHOW_INDICATOR = 0x34;
		
		public static final int COMMAND_TYPE_PING = 0x51;
		public static final int COMMAND_TYPE_AWAKE = 0x52;
		public static final int COMMAND_TYPE_SLEEP = 0x53;
		public static final int COMMAND_TYPE_REBOOT = 0x54;
		
		// byte definitions for buffer setting
		private static final byte TRANSACTION_START_BYTE = (byte)0xfc;
		private static final byte TRANSACTION_END_BYTE = (byte)0xfd;

//		private static final int BYTE_RESET_EMERGENCY_OBJ = (byte)0x01;
//		private static final int BYTE_RESET_NORMAL_OBJ = (byte)0x02;
//		private static final int BYTE_RESET_USER_MESSAGE = (byte)0x03;
//		private static final int BYTE_ADD_EMERGENCY_OBJ = (byte)0x11;
//		private static final int BYTE_ADD_NORMAL_OBJ = (byte)0x12;
//		private static final int BYTE_ADD_USER_MESSAGE = (byte)0x13;
//		private static final int BYTE_DELETE_EMERGENCY_OBJ = (byte)0x21;
//		private static final int BYTE_DELETE_NORMAL_OBJ = (byte)0x22;
//		private static final int BYTE_DELETE_USER_MESSAGE = (byte)0x22;
//		private static final int BYTE_SET_TIME = (byte)0x31;
//		private static final int BYTE_REQUEST_MOVEMENT_HISTORY = (byte)0x31;
//		private static final int BYTE_PING = (byte)0x51;
//		private static final int BYTE_AWAKE = (byte)0x52;
//		private static final int BYTE_SLEEP = (byte)0x53;
//		private static final int BYTE_REBOOT = (byte)0x54;
		
		// Icon types
		public static final int ICON_TYPE_NONE = 0;
		public static final int ICON_TYPE_SMS = 1;
		public static final int ICON_TYPE_CALL = 2;
		public static final int ICON_TYPE_APP_NOTI = 3;
		public static final int ICON_TYPE_BATTERY = 4;
		public static final int ICON_TYPE_WARNING = 5;
		public static final int ICON_TYPE_CHAT = 6;
		public static final int ICON_TYPE_EMAIL = 7;

		// Transaction instance status
		private static final int STATE_NONE = 0;		// Instance created
		private static final int STATE_BEGIN = 1;		// Initialize transaction
		private static final int STATE_SETTING_FINISHED = 2;	// End of setting parameters 
		private static final int STATE_TRANSFERED = 3;	// End of sending transaction data
		private static final int STATE_ERROR = -1;		// Error occurred
		
		// Transaction parameters
		private int mState = STATE_NONE;

		private byte[] mBuffer = null;

		private int mCommandType = COMMAND_TYPE_NONE;
		private int mIconType = ICON_TYPE_NONE;
		private int mId = 0x00;
		private byte mDateMonth = 0x00; 
		private byte mDateDay = 0x00;
		private byte mDateWeek = 0x00;
		private byte mDateNoon = 0x00;
		private byte mDateHour = 0x00;
		private byte mDateMinute = 0x00;
		
		private String mMsg = null;
		
		
		public void begin() {
			mState = STATE_BEGIN;
		
			mCommandType = COMMAND_TYPE_NONE;
			mIconType = ICON_TYPE_NONE;
			mId = 0x00;
			
			mDateMonth = 0x00;
			mDateDay = 0x00;
			mDateWeek = 0x00;
			mDateNoon = 0x00;
			mDateHour = 0x00;
			mDateMinute = 0x00;
			mMsg = null;
			
			mBuffer = null;
		}
		
		public void setCommand(int cmd) {
			switch(cmd) {
			case COMMAND_TYPE_RESET_EMERGENCY_OBJ:
			case COMMAND_TYPE_RESET_NORMAL_OBJ:
			case COMMAND_TYPE_RESET_USER_MESSAGE:
			
			case COMMAND_TYPE_ADD_EMERGENCY_OBJ:
			case COMMAND_TYPE_ADD_NORMAL_OBJ:
			case COMMAND_TYPE_ADD_USER_MESSAGE:
			
			case COMMAND_TYPE_DELETE_EMERGENCY_OBJ:
			case COMMAND_TYPE_DELETE_NORMAL_OBJ:
			case COMMAND_TYPE_DELETE_USER_MESSAGE:
			
			case COMMAND_TYPE_SET_TIME:
			case COMMAND_TYPE_REQUEST_MOVEMENT_HISTORY:
			
			case COMMAND_TYPE_PING:
			case COMMAND_TYPE_AWAKE:
			case COMMAND_TYPE_SLEEP:
			case COMMAND_TYPE_REBOOT:
				
			case COMMAND_TYPE_SET_CLOCK_STYLE:
			case COMMAND_TYPE_SHOW_INDICATOR:
				mCommandType = cmd;
				break;
			default:
				mCommandType = COMMAND_TYPE_NONE;
				break;
			}
		}
		
		public void setDate(int month, int day, int week, int noon, int hour, int minute) {
			mDateMonth = (byte)month;
			mDateDay = (byte)day;
			mDateWeek = (byte)week;
			mDateNoon = (byte)noon;
			mDateHour = (byte)hour;
			mDateMinute = (byte)minute;
		}
		
		public void setDate() {
	    	Calendar c = Calendar.getInstance();
	    	int month = c.get(Calendar.MONTH);
	    	int day = c.get(Calendar.DAY_OF_MONTH);
	    	int week = c.get(Calendar.DAY_OF_WEEK);
	    	int noon = c.get(Calendar.AM_PM);
	    	int hour = c.get(Calendar.HOUR);
	    	int minute = c.get(Calendar.MINUTE);
			
			mDateMonth = (byte)month;
			mDateDay = (byte)day;
			mDateWeek = (byte)week;
			mDateNoon = (byte)noon;
			mDateHour = (byte)hour;
			mDateMinute = (byte)minute;
		}
		
		public void setId(int id) {
			mId = id;
		}
		
		/**
		 * Set string message to send
		 * @param id	Identifier - WARNING: use lower 1 byte only
		 * @param msg	String to send
		 */
		public void setMessage(int id, String msg) {
			mId = id;
			mMsg = msg;
		}
		
		public void setIcon(int iconType) {
			mIconType = iconType;
		}
		
		public void settingFinished() {
			mState = STATE_SETTING_FINISHED;
			
			switch(mCommandType) {
			// Command byte only
			case COMMAND_TYPE_RESET_EMERGENCY_OBJ:
			case COMMAND_TYPE_RESET_NORMAL_OBJ:
			case COMMAND_TYPE_RESET_USER_MESSAGE:
			case COMMAND_TYPE_PING:
			case COMMAND_TYPE_AWAKE:
			case COMMAND_TYPE_SLEEP:
			case COMMAND_TYPE_REBOOT:
			case COMMAND_TYPE_REQUEST_MOVEMENT_HISTORY:
				// Make buffer
				// [Transaction start signal : 1byte : 0xfd] 
				// [command type : 1byte] 
				// [Transaction end signal : 1byte : 0xfe]
				mBuffer = new byte[3];
				
				mBuffer[0] = TRANSACTION_START_BYTE;	// Transaction start signal
				mBuffer[1] = (byte)mCommandType;	// Command
				mBuffer[2] = TRANSACTION_END_BYTE;
				
				break;
			
			// Command byte + Message bytes
			case COMMAND_TYPE_ADD_EMERGENCY_OBJ:
			case COMMAND_TYPE_ADD_NORMAL_OBJ:
			case COMMAND_TYPE_ADD_USER_MESSAGE:
				if(mMsg == null || mMsg.length() < 1) {
					mState = STATE_ERROR;
					break;
				}
				
				byte[] strBuffer = mMsg.getBytes();
				if(strBuffer != null && strBuffer.length > 0) {
					for(int i=0; i<strBuffer.length; i++) {
						if(strBuffer[i] == (byte)0x00)
							strBuffer[i] = (byte)0xFD;
					}
				}
				
				// Make buffer
				// [Transaction start signal : 1byte : 0xfd] 
				// [command type : 1byte] 
				// [ID : 1byte] 
				// [icon type : 1byte] 
				// [data packet : various size : Currently max 16 byte] 
				// [Transaction end signal : 1byte : 0xfe]
				int stringSize = (strBuffer.length > MAX_MESSAGE_LENGTH) ? MAX_MESSAGE_LENGTH : strBuffer.length;
				mBuffer = new byte[6+stringSize];
				
				mBuffer[0] = TRANSACTION_START_BYTE;	// Transaction start signal
				mBuffer[1] = (byte)mCommandType;		// Command
				mBuffer[2] = (byte)0xF0;			// This is reserved for Arduino management
				mBuffer[3] = (byte)mId;				// Message ID
				mBuffer[4] = (byte)mIconType;		// mIconType : Icon type
				System.arraycopy(strBuffer, 0, mBuffer, 5, stringSize);
				mBuffer[mBuffer.length - 1] = TRANSACTION_END_BYTE;
				
				break;
			
			// Command byte + Date bytes
			case COMMAND_TYPE_SET_TIME:
				// Make buffer
				// [Transaction start signal : 1byte : 0xfd] 
				// [command type : 1byte] 
				// [data packet : month(1byte), day(1byte), week(1byte), noon(1byte), hour(1byte), min(1byte) ]
				// [Transaction end signal : 1byte : 0xfe]
				mBuffer = new byte[9];
				
				mBuffer[0] = TRANSACTION_START_BYTE;	// Transaction start signal
				mBuffer[1] = (byte)mCommandType;	// Command
				mBuffer[2] = mDateMonth;
				mBuffer[3] = mDateDay;
				mBuffer[4] = mDateWeek;
				mBuffer[5] = mDateNoon;
				mBuffer[6] = mDateHour;
				mBuffer[7] = mDateMinute;
				mBuffer[8] = TRANSACTION_END_BYTE;
				
				break;
			
			// Command byte + Data bytes
			case COMMAND_TYPE_DELETE_EMERGENCY_OBJ:
			case COMMAND_TYPE_DELETE_NORMAL_OBJ:
			case COMMAND_TYPE_DELETE_USER_MESSAGE:
			case COMMAND_TYPE_SET_CLOCK_STYLE:
			case COMMAND_TYPE_SHOW_INDICATOR:
				// Make buffer
				// [Transaction start signal : 1byte : 0xfd] 
				// [command type : 1byte]
				// [data packet : 1byte integer]
				// [Transaction end signal : 1byte : 0xfe]
				mBuffer = new byte[4];
				
				mBuffer[0] = TRANSACTION_START_BYTE;	// Transaction start signal
				mBuffer[1] = (byte)mCommandType;	// Command
				mBuffer[2] = (byte)mId;			// Arduino uses 2 byte integer
				mBuffer[3] = TRANSACTION_END_BYTE;
				break;
				
			default:
				mState = STATE_ERROR;
				break;
			}
		}
		
		public byte[] getPacket() {
			if(mState == STATE_SETTING_FINISHED) {
				return mBuffer;
			}
			return null;
		}
		
		public boolean sendTransaction() {
			if(mBuffer == null) {
				Logs.e(TAG, "##### Ooooooops!! No sending buffer!! Check command!!");
				return false;
			}
			
			// For debug
			if(mBuffer.length > 0) {
				StringBuilder sb = new StringBuilder();
				
				switch(mBuffer[1]) {
				case COMMAND_TYPE_RESET_EMERGENCY_OBJ:
					sb.append("COMMAND_TYPE_RESET_EMERGENCY_OBJ : ");
					break;
				case COMMAND_TYPE_RESET_NORMAL_OBJ:
					sb.append("COMMAND_TYPE_RESET_NORMAL_OBJ : ");
					break;
				case COMMAND_TYPE_RESET_USER_MESSAGE:
					sb.append("COMMAND_TYPE_RESET_USER_MESSAGE : ");
					break;
				case COMMAND_TYPE_ADD_EMERGENCY_OBJ:
					sb.append("COMMAND_TYPE_ADD_EMERGENCY_OBJ : ");
					break;
				case COMMAND_TYPE_ADD_NORMAL_OBJ:
					sb.append("COMMAND_TYPE_ADD_NORMAL_OBJ : ");
					break;
				case COMMAND_TYPE_ADD_USER_MESSAGE:
					sb.append("COMMAND_TYPE_ADD_USER_MESSAGE : ");
					break;
				
				case COMMAND_TYPE_DELETE_EMERGENCY_OBJ:
					sb.append("COMMAND_TYPE_DELETE_EMERGENCY_OBJ : ");
					break;
				case COMMAND_TYPE_DELETE_NORMAL_OBJ:
					sb.append("COMMAND_TYPE_DELETE_NORMAL_OBJ : ");
					break;
				case COMMAND_TYPE_DELETE_USER_MESSAGE:
					sb.append("COMMAND_TYPE_DELETE_USER_MESSAGE : ");
					break;
				
				case COMMAND_TYPE_SET_TIME:
					sb.append("COMMAND_TYPE_SET_TIME : ");
					break;
				case COMMAND_TYPE_REQUEST_MOVEMENT_HISTORY:
					sb.append("COMMAND_TYPE_REQUEST_MOVEMENT_HISTORY : ");
					break;
				case COMMAND_TYPE_PING:
					sb.append("COMMAND_TYPE_PING : ");
					break;
				case COMMAND_TYPE_AWAKE:
					sb.append("COMMAND_TYPE_AWAKE : ");
					break;
				case COMMAND_TYPE_SLEEP:
					sb.append("COMMAND_TYPE_SLEEP : ");
					break;
				case COMMAND_TYPE_REBOOT:
					sb.append("COMMAND_TYPE_REBOOT : ");
					break;
					
				case COMMAND_TYPE_SET_CLOCK_STYLE:
					sb.append("COMMAND_TYPE_SET_CLOCK_STYLE : ");
					break;
					
				default:
					break;
				}
				
				for(int i=0; i<mBuffer.length; i++) {
					sb.append(String.format("%02X, ", mBuffer[i]));
				}
				
				Logs.d(" ");
				Logs.d(TAG, sb.toString());
			}
			
			if(mState == STATE_SETTING_FINISHED) {
				if(mBTManager != null) {
					// Check that we're actually connected before trying anything
					if (mBTManager.getState() == BluetoothManager.STATE_CONNECTED) {
						// Check that there's actually something to send
						if (mBuffer.length > 0) {
							// Get the message bytes and tell the BluetoothChatService to write
							mBTManager.write(mBuffer);
							
							mState = STATE_TRANSFERED;
							return true;
						}
						mState = STATE_ERROR;
					}
					mHandler.obtainMessage(Constants.MESSAGE_CMD_ERROR_NOT_CONNECTED).sendToTarget();
				}
			}
			return false;
		}
	}	// End of class Transaction

}
