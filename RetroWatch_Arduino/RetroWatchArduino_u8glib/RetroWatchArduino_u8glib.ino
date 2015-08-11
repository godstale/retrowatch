/*
    RetroWatch Arduino is a part of open source smart watch project.
    Copyright (C) 2014  Suh Young Bae

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see [http://www.gnu.org/licenses/].
*/
/*
Retro Watch Arduino v1.0 - u8g (supports u8glib)

  Get the latest version, android host app at 
  ------> https://github.com/godstale/retrowatch
  ------> or http://www.hardcopyworld.com

Written by Suh Young Bae (godstale@hotmail.com)
All text above must be included in any redistribution
*/

//#include <avr/pgmspace.h>
#include <SoftwareSerial.h>
#include <math.h>
#include "bitmap.h"

#include "U8glib.h"

///////////////////////////////////////////////////////////////////
//----- OLED instance

// IMPORTANT NOTE: The complete list of supported devices 
// with all constructor calls is here: http://code.google.com/p/u8glib/wiki/device

U8GLIB_SSD1306_128X64 u8g(U8G_I2C_OPT_NONE|U8G_I2C_OPT_DEV_0);	// I2C / TWI 
///////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////
//----- BT instance
SoftwareSerial BTSerial(2, 3); //Connect HC-06, RX, TX
///////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////
//----- Protocol

//----- Bluetooth transaction parsing
#define TR_MODE_IDLE 1
#define TR_MODE_WAIT_CMD 11
#define TR_MODE_WAIT_MESSAGE 101
#define TR_MODE_WAIT_TIME 111
#define TR_MODE_WAIT_ID 121
#define TR_MODE_WAIT_COMPLETE 201

#define TRANSACTION_START_BYTE 0xfc
#define TRANSACTION_END_BYTE 0xfd

#define CMD_TYPE_NONE 0x00
#define CMD_TYPE_RESET_EMERGENCY_OBJ 0x05
#define CMD_TYPE_RESET_NORMAL_OBJ 0x02
#define CMD_TYPE_RESET_USER_MESSAGE 0x03

#define CMD_TYPE_ADD_EMERGENCY_OBJ 0x11
#define CMD_TYPE_ADD_NORMAL_OBJ 0x12
#define CMD_TYPE_ADD_USER_MESSAGE 0x13

#define CMD_TYPE_DELETE_EMERGENCY_OBJ 0x21
#define CMD_TYPE_DELETE_NORMAL_OBJ 0x22
#define CMD_TYPE_DELETE_USER_MESSAGE 0x23

#define CMD_TYPE_SET_TIME 0x31
#define CMD_TYPE_REQUEST_MOVEMENT_HISTORY 0x32
#define CMD_TYPE_SET_CLOCK_STYLE 0x33
#define CMD_TYPE_SET_INDICATOR 0x34

#define CMD_TYPE_PING 0x51
#define CMD_TYPE_AWAKE 0x52
#define CMD_TYPE_SLEEP 0x53
#define CMD_TYPE_REBOOT 0x54

byte TRANSACTION_POINTER = TR_MODE_IDLE;
byte TR_COMMAND = CMD_TYPE_NONE;
///////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////
//----- Buffer, Parameters

//----- Message item buffer
#define MSG_COUNT_MAX 7
#define MSG_BUFFER_MAX 19
unsigned char msgBuffer[MSG_COUNT_MAX][MSG_BUFFER_MAX];
char msgParsingLine = 0;
char msgParsingChar = 0;
char msgCurDisp = 0;

//----- Emergency item buffer
#define EMG_COUNT_MAX 3
#define EMG_BUFFER_MAX 19
char emgBuffer[EMG_COUNT_MAX][EMG_BUFFER_MAX];
char emgParsingLine = 0;
char emgParsingChar = 0;
char emgCurDisp = 0;

//----- Time
#define UPDATE_TIME_INTERVAL 60000
byte iMonth = 1;
byte iDay = 1;
byte iWeek = 1;    // 1: SUN, MON, TUE, WED, THU, FRI,SAT
byte iAmPm = 0;    // 0:AM, 1:PM
byte iHour = 0;
byte iMinutes = 0;
byte iSecond = 0;

#define TIME_BUFFER_MAX 6
char timeParsingIndex = 0;
char timeBuffer[6] = {-1, -1, -1, -1, -1, -1};
PROGMEM const char* weekString[] = {"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
PROGMEM const char* ampmString[] = {"AM", "PM"};
///////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////
//----- Display features
#define DISPLAY_MODE_START_UP 0
#define DISPLAY_MODE_CLOCK 1
#define DISPLAY_MODE_EMERGENCY_MSG 2
#define DISPLAY_MODE_NORMAL_MSG 3
#define DISPLAY_MODE_IDLE 11
byte displayMode = DISPLAY_MODE_START_UP;

#define CLOCK_STYLE_SIMPLE_ANALOG  0x01
#define CLOCK_STYLE_SIMPLE_DIGIT  0x02
#define CLOCK_STYLE_SIMPLE_MIX  0x03
byte clockStyle = CLOCK_STYLE_SIMPLE_MIX;

#define INDICATOR_ENABLE 0x01
boolean updateIndicator = true;

byte centerX = 64;
byte centerY = 32;
byte iRadius = 28;

#define IDLE_DISP_INTERVAL 60000
#define CLOCK_DISP_INTERVAL 60000
#define EMERGENCY_DISP_INTERVAL 5000
#define MESSAGE_DISP_INTERVAL 3000
unsigned long prevClockTime = 0;
unsigned long prevDisplayTime = 0;

unsigned long next_display_interval = 0;
unsigned long mode_change_timer = 0;
#define CLOCK_DISPLAY_TIME 300000
#define EMER_DISPLAY_TIME 10000
#define MSG_DISPLAY_TIME 5000

PROGMEM const char* strIntro[] = {"Retro", "Watch", "Arduino v1.0"};
///////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////
//----- Button control
int buttonPin = 5;
boolean isClicked = HIGH;
///////////////////////////////////////////////////////////////////


void setup()   {
  //Serial.begin(9600);    // WARNING: Do not enable this if there is not enough memory
  //Serial.println(F("RetroWatch v1.0 u8g"));

  //----- Set button
  pinMode(buttonPin, INPUT);  // Defines button pin
  
  //----- Initialize message buffer
  init_emg_array();
  init_msg_array();
  
  //----- Set display features
  centerX = u8g.getWidth() / 2;
  centerY = u8g.getHeight() / 2;
  iRadius = centerY - 2;

  //----- Setup serial connection with BT
  BTSerial.begin(9600);  // set the data rate for the BT port
  
  //----- Show logo
  drawStartUp();    // Show RetroWatch Logo
  delay(3000);
}


void loop() {
  boolean isReceived = false;
  unsigned long current_time = 0;
  
  // Get button input
  if(digitalRead(buttonPin) == LOW) isClicked = LOW;
  
  // Receive data from remote and parse
  isReceived = receiveBluetoothData();
  
  // Update clock time
  current_time = millis();
  updateTime(current_time);
  
  // Display routine
  onDraw(current_time);
  
  // If data doesn't arrive, wait for a while to save battery
  if(!isReceived)
    delay(300);
}




///////////////////////////////////
//----- Utils
///////////////////////////////////
void init_msg_array() {
  for(int i=0; i<MSG_COUNT_MAX; i++) {
    for(int j=0; j<MSG_BUFFER_MAX; j++) {
      msgBuffer[i][j] = 0x00;
    }
  }
  msgParsingLine = 0;
  msgParsingChar = 0;    // First 2 byte is management byte
  msgCurDisp = 0;
}

void init_emg_array() {
  for(int i=0; i<EMG_COUNT_MAX; i++) {
    for(int j=0; j<EMG_BUFFER_MAX; j++) {
      emgBuffer[i][j] = 0x00;
    }
  }
  emgParsingLine = 0;
  emgParsingChar = 0;    // First 2 byte is management byte
  emgCurDisp = 0;
}

///////////////////////////////////
//----- Time functions
///////////////////////////////////
void setTimeValue() {
  iMonth = timeBuffer[0];
  iDay = timeBuffer[1];
  iWeek = timeBuffer[2];    // 1: SUN, MON, TUE, WED, THU, FRI,SAT
  iAmPm = timeBuffer[3];    // 0:AM, 1:PM
  iHour = timeBuffer[4];
  iMinutes = timeBuffer[5];
}

void updateTime(unsigned long current_time) {
  if(iMinutes >= 0) {
    if(current_time - prevClockTime > UPDATE_TIME_INTERVAL) {
      // Increase time
      iMinutes++;
      if(iMinutes >= 60) {
        iMinutes = 0;
        iHour++;
        if(iHour > 12) {
          iHour = 1;
          (iAmPm == 0) ? iAmPm=1 : iAmPm=0;
          if(iAmPm == 0) {
            iWeek++;
            if(iWeek > 7)
              iWeek = 1;
            iDay++;
            if(iDay > 30)  // Yes. day is not exact.
              iDay = 1;
          }
        }
      }
      prevClockTime = current_time;
    }
  }
  else {
    displayMode = DISPLAY_MODE_START_UP;
  }
}

///////////////////////////////////
//----- BT, Data parsing functions
///////////////////////////////////

// Parsing packet according to current mode
boolean receiveBluetoothData() {
  int isTransactionEnded = false;
  while(!isTransactionEnded) {
    if(BTSerial.available()) {
      byte c = BTSerial.read();
      
      //if(c == 0xFE && TRANSACTION_POINTER != TR_MODE_WAIT_MESSAGE) return false;
      
      if(TRANSACTION_POINTER == TR_MODE_IDLE) {
        parseStartSignal(c);
      }
      else if(TRANSACTION_POINTER == TR_MODE_WAIT_CMD) {
        parseCommand(c);
      }
      else if(TRANSACTION_POINTER == TR_MODE_WAIT_MESSAGE) {
        parseMessage(c);
      }
      else if(TRANSACTION_POINTER == TR_MODE_WAIT_TIME) {
        parseTime(c);
      }
      else if(TRANSACTION_POINTER == TR_MODE_WAIT_ID) {
        parseId(c);
      }
      else if(TRANSACTION_POINTER == TR_MODE_WAIT_COMPLETE) {
        isTransactionEnded = parseEndSignal(c);
      }
      
    }  // End of if(BTSerial.available())
    else {
      isTransactionEnded = true;
    }
  }  // End of while()
  return true;
}  // End of receiveBluetoothData()

void parseStartSignal(byte c) {
  //drawLogChar(c);
  if(c == TRANSACTION_START_BYTE) {
    TRANSACTION_POINTER = TR_MODE_WAIT_CMD;
    TR_COMMAND = CMD_TYPE_NONE;
  }
}

void parseCommand(byte c) {
  if(c == CMD_TYPE_RESET_EMERGENCY_OBJ || c == CMD_TYPE_RESET_NORMAL_OBJ || c == CMD_TYPE_RESET_USER_MESSAGE) {
    TRANSACTION_POINTER = TR_MODE_WAIT_COMPLETE;
    TR_COMMAND = c;
    processTransaction();
  }
  else if(c == CMD_TYPE_ADD_EMERGENCY_OBJ || c == CMD_TYPE_ADD_NORMAL_OBJ || c == CMD_TYPE_ADD_USER_MESSAGE) {
    TRANSACTION_POINTER = TR_MODE_WAIT_MESSAGE;
    TR_COMMAND = c;
    if(c == CMD_TYPE_ADD_EMERGENCY_OBJ) {
      //Serial.println("# start -  ADD_EMERGENCY_OBJ");
      emgParsingChar = 0;
      if(emgParsingLine >= MSG_COUNT_MAX || emgParsingLine < 0)
        emgParsingLine = 0;
      for(int i=3; i<EMG_BUFFER_MAX; i++)
        emgBuffer[emgParsingLine][i] = 0x00;
    }
    else if(c == CMD_TYPE_ADD_NORMAL_OBJ) {
      //Serial.println("# start -  ADD_NORMAL_OBJ");
      msgParsingChar = 0;
      if(msgParsingLine >= MSG_COUNT_MAX || msgParsingLine < 0)
        msgParsingLine = 0;
      for(int i=3; i<MSG_BUFFER_MAX; i++)
        msgBuffer[msgParsingLine][i] = 0x00;
    }
  }
  else if(c == CMD_TYPE_DELETE_EMERGENCY_OBJ || c == CMD_TYPE_DELETE_NORMAL_OBJ || c == CMD_TYPE_DELETE_USER_MESSAGE) {
    TRANSACTION_POINTER = TR_MODE_WAIT_COMPLETE;
    TR_COMMAND = c;
  }
  else if(c == CMD_TYPE_SET_TIME) {
    TRANSACTION_POINTER = TR_MODE_WAIT_TIME;
    TR_COMMAND = c;
  }
  else if(c == CMD_TYPE_SET_CLOCK_STYLE || c == CMD_TYPE_SET_INDICATOR) {
    TRANSACTION_POINTER = TR_MODE_WAIT_ID;
    TR_COMMAND = c;
  }
  else {
    TRANSACTION_POINTER = TR_MODE_IDLE;
    TR_COMMAND = CMD_TYPE_NONE;
  }
}

void parseMessage(byte c) {
  if(c == TRANSACTION_END_BYTE) {
    processTransaction();
    TRANSACTION_POINTER = TR_MODE_IDLE;
  }
  
  if(TR_COMMAND == CMD_TYPE_ADD_EMERGENCY_OBJ) {
    if(emgParsingChar < EMG_BUFFER_MAX - 1) {
      if(emgParsingChar > 1) {
        emgBuffer[emgParsingLine][emgParsingChar] = c;
      }
      emgParsingChar++;
    }
    else {
      TRANSACTION_POINTER = TR_MODE_IDLE;
      processTransaction();
    }
  }
  else if(TR_COMMAND == CMD_TYPE_ADD_NORMAL_OBJ) {
    if(msgParsingChar < MSG_BUFFER_MAX - 1) {
      if(msgParsingChar > 1) {
        msgBuffer[msgParsingLine][msgParsingChar] = c;
      }
      msgParsingChar++;
    }
    else {
      TRANSACTION_POINTER = TR_MODE_IDLE;
      processTransaction();
    }
  }
  else if(TR_COMMAND == CMD_TYPE_ADD_USER_MESSAGE) {
    // Not available yet.
    TRANSACTION_POINTER = TR_MODE_WAIT_COMPLETE;
  }
}

void parseTime(byte c) {
  if(TR_COMMAND == CMD_TYPE_SET_TIME) {
    if(timeParsingIndex >= 0 && timeParsingIndex < TIME_BUFFER_MAX) {
      timeBuffer[timeParsingIndex] = (int)c;
      timeParsingIndex++;
    }
    else {
      processTransaction();
      TRANSACTION_POINTER = TR_MODE_WAIT_COMPLETE;
    }
  }
}

void parseId(byte c) {
  if(TR_COMMAND == CMD_TYPE_SET_CLOCK_STYLE) {
    clockStyle = c;
    processTransaction();
  }
  else if(TR_COMMAND == CMD_TYPE_SET_INDICATOR) {
    if(c == INDICATOR_ENABLE)
      updateIndicator = true;
    else
      updateIndicator = false;
    processTransaction();
  }
  TRANSACTION_POINTER = TR_MODE_WAIT_COMPLETE;
}

boolean parseEndSignal(byte c) {
  if(c == TRANSACTION_END_BYTE) {
    TRANSACTION_POINTER = TR_MODE_IDLE;
    return true;
  }
  return false;
}

void processTransaction() {
  if(TR_COMMAND == CMD_TYPE_RESET_EMERGENCY_OBJ) {
    init_emg_array();//init_msg_array();
  }
  else if(TR_COMMAND == CMD_TYPE_RESET_NORMAL_OBJ) {
    init_msg_array();//init_emg_array();
  }
  else if(TR_COMMAND == CMD_TYPE_RESET_USER_MESSAGE) {
    // Not available yet.
  }
  else if(TR_COMMAND == CMD_TYPE_ADD_NORMAL_OBJ) {
    //Serial.println("# processTransaction() - ADD_NORMAL_OBJ");
    msgBuffer[msgParsingLine][0] = 0x01;
    msgBuffer[msgParsingLine][MSG_BUFFER_MAX-1] = 0x00;
    msgParsingChar = 0;
    msgParsingLine++;
    if(msgParsingLine >= MSG_COUNT_MAX)
      msgParsingLine = 0;
    setNextDisplayTime(millis(), 0);  // update screen immediately
  }
  else if(TR_COMMAND == CMD_TYPE_ADD_EMERGENCY_OBJ) {
    //Serial.println("# processTransaction() - ADD_EMERGENCY_OBJ");
    emgBuffer[emgParsingLine][0] = 0x01;
    emgBuffer[emgParsingLine][EMG_BUFFER_MAX - 1] = 0x00;
    emgParsingChar = 0;
    emgParsingLine++;
    if(emgParsingLine >= EMG_COUNT_MAX)
      emgParsingLine = 0;
    startEmergencyMode();
    setNextDisplayTime(millis(), 2000);
  }
  else if(TR_COMMAND == CMD_TYPE_ADD_USER_MESSAGE) {
  }
  else if(TR_COMMAND == CMD_TYPE_DELETE_EMERGENCY_OBJ || TR_COMMAND == CMD_TYPE_DELETE_NORMAL_OBJ || TR_COMMAND == CMD_TYPE_DELETE_USER_MESSAGE) {
    // Not available yet.
  }
  else if(TR_COMMAND == CMD_TYPE_SET_TIME) {
    setTimeValue();
    timeParsingIndex = 0;
    setNextDisplayTime(millis(), 0);  // update screen immediately
  }
  if(TR_COMMAND == CMD_TYPE_SET_CLOCK_STYLE || CMD_TYPE_SET_INDICATOR) {
    setNextDisplayTime(millis(), 0);  // update screen immediately
  }
}

///////////////////////////////////
//----- Drawing methods
///////////////////////////////////

// Main drawing routine.
// Every drawing except splash screen starts here.
// displayMode parameter determines which page to be shown.
// Internal, external event and user input by button changes displayMode
// To avoid too fast redraw most page has display interval
//
void onDraw(unsigned long currentTime) {
  if(!isDisplayTime(currentTime))    // Do not re-draw at every tick
    return;
  
  if(displayMode == DISPLAY_MODE_START_UP) {
    drawStartUp();
  }
  else if(displayMode == DISPLAY_MODE_CLOCK) {
    if(isClicked == LOW) {    // User input received
      startEmergencyMode();
      setPageChangeTime(0);    // Change mode with no page-delay
      setNextDisplayTime(currentTime, 0);    // Do not wait next re-draw time
    }
    else {
      drawClock();
      
      if(isPageChangeTime(currentTime)) {  // It's time to go into idle mode
        startIdleMode();
        setPageChangeTime(currentTime);  // Set a short delay
      }
      setNextDisplayTime(currentTime, CLOCK_DISP_INTERVAL);
    }
  }
  else if(displayMode == DISPLAY_MODE_EMERGENCY_MSG) {
    if(findNextEmerMessage()) {
      drawEmergency();
      emgCurDisp++;
      if(emgCurDisp >= EMG_COUNT_MAX) {
        emgCurDisp = 0;
        startMessageMode();
      }
      setNextDisplayTime(currentTime, EMERGENCY_DISP_INTERVAL);
    }
    // There's no message left to display. Go to normal message mode.
    else {
      startMessageMode();
      //setPageChangeTime(0);
      setNextDisplayTime(currentTime, 0);  // with no re-draw interval
    }
  }
  else if(displayMode == DISPLAY_MODE_NORMAL_MSG) {
    if(findNextNormalMessage()) {
      drawMessage();
      msgCurDisp++;
      if(msgCurDisp >= MSG_COUNT_MAX) {
        msgCurDisp = 0;
        startClockMode();
      }
      setNextDisplayTime(currentTime, MESSAGE_DISP_INTERVAL);
    }
    // There's no message left to display. Go to clock mode.
    else {
      startClockMode();
      setPageChangeTime(currentTime);
      setNextDisplayTime(currentTime, 0);  // with no re-draw interval
    }
  }
  else if(displayMode == DISPLAY_MODE_IDLE) {
    if(isClicked == LOW) {    // Wake up watch if there's an user input
      startClockMode();
      setPageChangeTime(currentTime);
      setNextDisplayTime(currentTime, 0);
    } 
    else {
      drawIdleClock();
      setNextDisplayTime(currentTime, IDLE_DISP_INTERVAL);
    }
  }
  else {
    startClockMode();    // This means there's an error
  }
  
  isClicked = HIGH;
}  // End of onDraw()


// To avoid re-draw on every drawing time
// wait for time interval according to current mode 
// But user input(button) breaks this sleep
boolean isDisplayTime(unsigned long currentTime) {
  if(currentTime - prevDisplayTime > next_display_interval) {
    return true;
  }
  if(isClicked == LOW) {
    delay(500);
    return true;
  }
  return false;
}

// Set next re-draw time 
void setNextDisplayTime(unsigned long currentTime, unsigned long nextUpdateTime) {
  next_display_interval = nextUpdateTime;
  prevDisplayTime = currentTime;
}

// Decide if it's the time to change page(mode)
boolean isPageChangeTime(unsigned long currentTime) {
  if(displayMode == DISPLAY_MODE_CLOCK) {
    if(currentTime - mode_change_timer > CLOCK_DISPLAY_TIME)
      return true;
  }
  return false;
}

// Set time interval to next page(mode)
void setPageChangeTime(unsigned long currentTime) {
  mode_change_timer = currentTime;
}

// Check if available emergency message exists or not
boolean findNextEmerMessage() {
  if(emgCurDisp < 0 || emgCurDisp >= EMG_COUNT_MAX) emgCurDisp = 0;
  while(true) {
    if(emgBuffer[emgCurDisp][0] == 0x00) {  // 0x00 means disabled
      emgCurDisp++;
      if(emgCurDisp >= EMG_COUNT_MAX) {
        emgCurDisp = 0;
        return false;
      }
    }
    else {
      break;
    }
  }  // End of while()
  return true;
}

// Check if available normal message exists or not
boolean findNextNormalMessage() {
  if(msgCurDisp < 0 || msgCurDisp >= MSG_COUNT_MAX) msgCurDisp = 0;
  while(true) {
    if(msgBuffer[msgCurDisp][0] == 0x00) {
      msgCurDisp++;
      if(msgCurDisp >= MSG_COUNT_MAX) {
        msgCurDisp = 0;
        return false;
      }
    }
    else {
      break;
    }
  }  // End of while()
  return true;
}

// Count all available emergency messages
int countEmergency() {
  int count = 0;
  for(int i=0; i<EMG_COUNT_MAX; i++) {
    if(emgBuffer[i][0] != 0x00)
      count++;
  }
  return count;
}

// Count all available normal messages
int countMessage() {
  int count = 0;
  for(int i=0; i<MSG_COUNT_MAX; i++) {
    if(msgBuffer[i][0] != 0x00)
      count++;
  }
  return count;
}

void startClockMode() {
  displayMode = DISPLAY_MODE_CLOCK;
}

void startEmergencyMode() {
  displayMode = DISPLAY_MODE_EMERGENCY_MSG;
  emgCurDisp = 0;
}

void startMessageMode() {
  displayMode = DISPLAY_MODE_NORMAL_MSG;
  msgCurDisp = 0;
}

void startIdleMode() {
  displayMode = DISPLAY_MODE_IDLE;
}

// Draw indicator. Indicator shows count of emergency and normal message
void drawIndicator() {
  if(updateIndicator) {
    int msgCount = countMessage();
    int emgCount = countEmergency();
    int drawCount = 1;
    
    if(msgCount > 0) {
      u8g.drawBitmapP( 127 - 8, 1, 1, 8, IMG_indicator_msg);
      
      // Show message count
//      String strTemp = String(msgCount);
//      char buff[strTemp.length()+1];
//      for(int i=0; i<strTemp.length()+1; i++) buff[i] = 0x00;
//      strTemp.toCharArray(buff, strTemp.length());
//      buff[strTemp.length()] = 0x00;
//      
//      u8g.setFont(u8g_font_fixed_v0);
//      u8g.setFontRefHeightExtendedText();
//      u8g.setDefaultForegroundColor();
//      u8g.setFontPosTop();
//      u8g.drawStr(127 - 15, 1, buff);

      drawCount++;
    }
    
    if(emgCount > 0) {
      u8g.drawBitmapP( 127 - 8*drawCount - 7*(drawCount-1), 1, 1, 8, IMG_indicator_emg);
      // Show message count
//      String strTemp2 = String("");
//      strTemp2 += emgCount;
//      char buff2[strTemp2.length()+1];
//      for(int i=0; i<strTemp2.length()+1; i++) buff2[i] = 0x00;
//      strTemp2.toCharArray(buff2, strTemp2.length());
//      buff2[strTemp2.length()] = 0x00;
//      
//      u8g.setFont(u8g_font_fixed_v0);
//      u8g.setFontRefHeightExtendedText();
//      u8g.setDefaultForegroundColor();
//      u8g.setFontPosTop();
//      u8g.drawStr(127 - 8*drawCount - 7*drawCount, 1, buff2);
    }
  }
}

// RetroWatch splash screen
void drawStartUp() {
  // picture loop  
  u8g.firstPage();  
  do {
    u8g_prepare();
    // draw logo
    u8g.drawBitmapP( 10, 15, 3, 24, IMG_logo_24x24);
    // show text
    u8g.setFont(u8g_font_courB14);
    u8g.setFontRefHeightExtendedText();
    u8g.setDefaultForegroundColor();
    u8g.setFontPosTop();
    u8g.drawStr(45, 12, (char*)pgm_read_word(&(strIntro[0])));
    u8g.drawStr(45, 28, (char*)pgm_read_word(&(strIntro[1])));
    
    u8g.setFont(u8g_font_fixed_v0);
    u8g.setFontRefHeightExtendedText();
    u8g.setDefaultForegroundColor();
    u8g.setFontPosTop();
    u8g.drawStr(45, 45, (char*)pgm_read_word(&(strIntro[2])));
  } while( u8g.nextPage() );
  
  startClockMode();
}

// Draw emergency message page
void drawEmergency() {
  // get icon index
  int icon_num = 60;
  if(emgBuffer[emgCurDisp][2] > -1 && emgBuffer[emgCurDisp][2] < ICON_ARRAY_SIZE)
    icon_num = (int)(emgBuffer[emgCurDisp][2]);

  // picture loop  
  u8g.firstPage();  
  do {
    u8g_prepare();
    // draw indicator
    if(updateIndicator)
      drawIndicator();
    // draw icon and string
    drawIcon(centerX - 8, centerY - 20, icon_num);
    
    int string_start_x = u8g.getStrPixelWidth((char *)(emgBuffer[emgCurDisp]+3)) / 2;
    if(string_start_x > 0) {
      if(string_start_x > centerX) 
        string_start_x = 0;
      else 
        string_start_x = centerX - string_start_x;
        
      u8g.setFont(u8g_font_fixed_v0);
      u8g.setFontRefHeightExtendedText();
      u8g.setDefaultForegroundColor();
      u8g.setFontPosTop();
      u8g.drawStr( string_start_x, centerY + 10, (char *)(emgBuffer[emgCurDisp]+3) );
    }
  } while( u8g.nextPage() );
}

// Draw normal message page
void drawMessage() {
  // get icon index
  int icon_num = 0;
  if(msgBuffer[msgCurDisp][2] > -1 && msgBuffer[msgCurDisp][2] < ICON_ARRAY_SIZE)
    icon_num = (int)(msgBuffer[msgCurDisp][2]);
  
  // picture loop  
  u8g.firstPage();  
  do {
    u8g_prepare();
    // draw indicator
    if(updateIndicator)
      drawIndicator();
    // draw icon and string
    drawIcon(centerX - 8, centerY - 20, icon_num);    
    
    int string_start_x = u8g.getStrPixelWidth((char *)(msgBuffer[msgCurDisp] + 3)) / 2;
    if(string_start_x > 0) {
      if(string_start_x > centerX) 
        string_start_x = 0;
      else 
        string_start_x = centerX - string_start_x;
        
      u8g.setFont(u8g_font_fixed_v0);
      u8g.setFontRefHeightExtendedText();
      u8g.setDefaultForegroundColor();
      u8g.setFontPosTop();
      u8g.drawStr( string_start_x, centerY + 10, (char *)(msgBuffer[msgCurDisp] + 3) );
    }
  } while( u8g.nextPage() );
}

// Draw main clock screen
// Clock style changes according to user selection
void drawClock() {
  
  // picture loop  
  u8g.firstPage();  
  do {
    u8g_prepare();
    // draw indicator
    if(updateIndicator)
      drawIndicator();
    // draw clock
    // CLOCK_STYLE_SIMPLE_DIGIT
    if(clockStyle == CLOCK_STYLE_SIMPLE_DIGIT) {
      // Show text
      u8g.setFont(u8g_font_courB14);
      u8g.setFontRefHeightExtendedText();
      u8g.setDefaultForegroundColor();
      u8g.setFontPosTop();
      u8g.drawStr(centerX - 34, centerY - 17, (const char*)pgm_read_word(&(weekString[iWeek])));
      u8g.drawStr(centerX + 11, centerY - 17, (const char*)pgm_read_word(&(ampmString[iAmPm])));
      
      String strDate = String("");
      char buff[6];
      if(iHour < 10)
        strDate += "0";
      strDate += iHour;
      strDate += ":";
      if(iMinutes < 10)
        strDate += "0";
      strDate += iMinutes;
      strDate.toCharArray(buff, 6);
      buff[5] = 0x00;
      
      u8g.setFont(u8g_font_courB14);
      u8g.setFontRefHeightExtendedText();
      u8g.setDefaultForegroundColor();
      u8g.setFontPosTop();
      u8g.drawStr(centerX - 29, centerY + 6, buff);
    }
    // CLOCK_STYLE_SIMPLE_MIX
    else if(clockStyle == CLOCK_STYLE_SIMPLE_MIX) {
      u8g.drawCircle(centerY, centerY, iRadius - 6);
      showTimePin(centerY, centerY, 0.1, 0.4, iHour*5 + (int)(iMinutes*5/60));
      showTimePin(centerY, centerY, 0.1, 0.70, iMinutes);
      
      // Show text
      u8g.setFont(u8g_font_fixed_v0);
      u8g.setFontRefHeightExtendedText();
      u8g.setDefaultForegroundColor();
      u8g.setFontPosTop();
      u8g.drawStr(centerX + 3, 23, (const char*)pgm_read_word(&(weekString[iWeek])));
      u8g.drawStr(centerX + 28, 23, (const char*)pgm_read_word(&(ampmString[iAmPm])));
      
      String strDate = String("");
      char buff[6];
      if(iHour < 10)
        strDate += "0";
      strDate += iHour;
      strDate += ":";
      if(iMinutes < 10)
        strDate += "0";
      strDate += iMinutes;
      strDate.toCharArray(buff, 6);
      buff[5] = 0x00;
      
      u8g.setFont(u8g_font_courB14);
      u8g.setFontRefHeightExtendedText();
      u8g.setDefaultForegroundColor();
      u8g.setFontPosTop();
      u8g.drawStr(centerX, 37, buff);
    }
    else {
      // CLOCK_STYLE_SIMPLE_ANALOG.
      u8g.drawCircle(centerX, centerY, iRadius);
      showTimePin(centerX, centerY, 0.1, 0.5, iHour*5 + (int)(iMinutes*5/60));
      showTimePin(centerX, centerY, 0.1, 0.78, iMinutes);
      
      iSecond++;
      if(iSecond > 60) iSecond = 0;
    }
  } while( u8g.nextPage() );
  
}

// Draw idle page
void drawIdleClock() {
  // picture loop  
  u8g.firstPage();  
  do {
    u8g_prepare();
    // draw indicator
    if(updateIndicator)
      drawIndicator();
    // show time
    String strDate = String("");
    char buff[6];
    if(iHour < 10)
      strDate += "0";
    strDate += iHour;
    strDate += ":";
    if(iMinutes < 10)
      strDate += "0";
    strDate += iMinutes;
    strDate.toCharArray(buff, 6);
    buff[5] = 0x00;
    
    u8g.setFont(u8g_font_courB14);
    u8g.setFontRefHeightExtendedText();
    u8g.setDefaultForegroundColor();
    u8g.setFontPosTop();
    u8g.drawStr(centerX - 29, centerY - 4, buff);
  } while( u8g.nextPage() );

}

// Returns starting point of normal string to display
int getCenterAlignedXOfMsg(int msgIndex) {
  int pointX = centerX;
  for(int i=3; i<MSG_BUFFER_MAX; i++) {
    char curChar = msgBuffer[msgIndex][i];
    if(curChar == 0x00) break;
    if(curChar >= 0xf0) continue;
    pointX -= 3;
  }
  if(pointX < 0) pointX = 0;
  return pointX;
}

// Returns starting point of emergency string to display
int getCenterAlignedXOfEmg(int emgIndex) {
  int pointX = centerX;
  for(int i=3; i<EMG_BUFFER_MAX; i++) {
    char curChar = emgBuffer[emgIndex][i];
    if(curChar == 0x00) break;
    if(curChar >= 0xf0) continue;
    pointX -= 3;
  }
  if(pointX < 0) pointX = 0;
  return pointX;
}

// Calculate clock pin position
double RAD=3.141592/180;
double LR = 89.99;
void showTimePin(int center_x, int center_y, double pl1, double pl2, double pl3) {
  double x1, x2, y1, y2;
  x1 = center_x + (iRadius * pl1) * cos((6 * pl3 + LR) * RAD);
  y1 = center_y + (iRadius * pl1) * sin((6 * pl3 + LR) * RAD);
  x2 = center_x + (iRadius * pl2) * cos((6 * pl3 - LR) * RAD);
  y2 = center_y + (iRadius * pl2) * sin((6 * pl3 - LR) * RAD);
  
  u8g.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
}

// Icon drawing tool
void drawIcon(int posx, int posy, int icon_num) {
  if(icon_num < 0 || icon_num >= ICON_ARRAY_SIZE)
    return;

  u8g.drawBitmapP( posx, posy, 2, 16, (const unsigned char*)pgm_read_word(&(bitmap_array[icon_num])));
}

// U8glib Initialization
void u8g_prepare(void) {
  // Add display setting code here
  //u8g.setFont(u8g_font_6x10);
  //u8g.setFontRefHeightExtendedText();
  //u8g.setDefaultForegroundColor();
  //u8g.setFontPosTop();
}

