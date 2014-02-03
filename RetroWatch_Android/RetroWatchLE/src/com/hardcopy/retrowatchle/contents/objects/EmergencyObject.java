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

public class EmergencyObject {
	public static final int EMERGENCY_TYPE_NONE = 0;
	public static final int EMERGENCY_TYPE_CALL_STATE = 1;
	public static final int EMERGENCY_TYPE_RF_STATE = 2;
	public static final int EMERGENCY_TYPE_BATT_STATE = 3;
	public static final int EMERGENCY_TYPE_USER_DEFINED = 4;
	
	public static final int CALL_STATE_IDLE = 0;
	public static final int CALL_STATE_CALL_RECEIVED = 1;
	
	public static final int RF_STATE_IN_SERVICE = 0;
	public static final int RF_STATE_OUT_OF_SERVICE = 1;
	public static final int RF_STATE_EMERGENCY_ONLY = 2;
	public static final int RF_STATE_POWER_OFF = 3;
	
	public static final int WIFI_STATE_ACTIVATED = 0;
	public static final int WIFI_STATE_DISABLED = 1;
	
	public static final int BATT_STATE_UNKNOWN = 0;
	public static final int BATT_STATE_AC_CHARGING = 1;
	public static final int BATT_STATE_USB_CHARGING = 2;
	public static final int BATT_STATE_DISCHARGING = 3;
	public static final int BATT_STATE_NOT_CHARGING = 4;
	public static final int BATT_STATE_FULL = 5;
	
}
