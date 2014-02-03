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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.hardcopy.retrowatchle.R;

public class Utils {
	private Context mContext;
	
	private static final String TAG = "Utils";  
	
	private static String[] mMessageTypeString = null;
	private static String[] mFilterTypeString = null;
	private static String[] mFilterMatchingTypeString = null;
	private static String[] mFilterReplaceTypeString = null;
	
	private static String[] mCallTypeString = null;
	private static String[] mRFTypeString = null;
	
	private static String mBatteryString = null;
	
	public Utils(Context c) {
		if(mContext == null) {
			mContext = c;
			initialize();
		}
	}
	
	
	private void initialize() {
		// Load strings
		mMessageTypeString = mContext.getResources().getStringArray(R.array.message_type_array);
		mFilterTypeString = mContext.getResources().getStringArray(R.array.filter_type_array);
		mFilterMatchingTypeString = mContext.getResources().getStringArray(R.array.filter_matching_type_array);
		mFilterReplaceTypeString = mContext.getResources().getStringArray(R.array.filter_replace_type_array);
		
		mCallTypeString = mContext.getResources().getStringArray(R.array.call_type_array);
		mRFTypeString = mContext.getResources().getStringArray(R.array.rf_type_array);
		
		mBatteryString = mContext.getResources().getString(R.string.batt_type_string);
	}
	
	
	//============================================================
	// App string resource
	//============================================================
	
	public static String getMessageTypeString(int type) {
		if(type > -1 && type < mMessageTypeString.length) {
			return mMessageTypeString[type];
		}
		return null;
	}
	
	public static String getFilterTypeString(int type) {
		if(type > -1 && type < mFilterTypeString.length) {
			return mFilterTypeString[type];
		}
		return null;
	}
	
	public static String getMatchingTypeString(int type) {
		if(type > -1 && type < mFilterMatchingTypeString.length) {
			return mFilterMatchingTypeString[type];
		}
		return null;
	}
	
	public static String getReplaceTypeString(int type) {
		if(type > -1 && type < mFilterReplaceTypeString.length) {
			return mFilterReplaceTypeString[type];
		}
		return null;
	}
	
	public static String getCallTypeString(int type) {
		if(type > -1 && type < mCallTypeString.length) {
			return mCallTypeString[type];
		}
		return null;
	}
	
	public static String getRFTypeString(int type) {
		if(type > -1 && type < mRFTypeString.length) {
			return mRFTypeString[type];
		}
		return null;
	}
	
	public static String getBatteryLevelString(int level) {
		StringBuilder sb = new StringBuilder();
		sb.append(mBatteryString).append(level);
		return sb.toString();
	}
	
	//============================================================
	// Directory, File handling
	//============================================================
	
    public static void initFileDirectory(String path)
	{
		File directory = new File(path);
		if( !directory.exists() ) {
			directory.mkdirs();
		}
	}
	
	public static void deleteDirectory(String path) {
		if(path==null) return;
		if(Utils.isFileExist(new File(path))) {
			deleteFileDirRecursive(path);
		}
	}

	public static void deleteFileDirRecursive(String path) {
		File file = new File(path);
		File[] childFileList = file.listFiles();
		for(File childFile : childFileList)
		{
			if(childFile.isDirectory()) {
				deleteFileDirRecursive(childFile.getAbsolutePath());     //하위 디렉토리 루프 
			}
			else {
				childFile.delete();    //하위 파일삭제
			}
		}
		file.delete();    //root 삭제 
	}
	
	public static boolean checkFileExists(String directory, String filename)
	{
		if(directory==null || filename==null)
			return false;
		File kFile = new File(directory+"/"+filename);
		if(kFile != null) {
			return kFile.exists();
		}
		return false;
	}
	
	public static File makeDirectory(String dir_path){
        File dir = new File(dir_path);
        if (!dir.exists())
        {
            dir.mkdirs();
            Log.i(TAG , "!dir.exists");
        }else{
            Log.i(TAG , "dir.exists");
        }
 
        return dir;
    }
 
    public static File makeFile(File dir , String file_path){
        File file = null;
        boolean isSuccess = false;
        if(dir.isDirectory()){
            file = new File(file_path);
            if(file!=null&&!file.exists()){
                Log.i(TAG , "!file.exists");
                try {
                    isSuccess = file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally{
                    Log.i(TAG, "파일생성 여부 = " + isSuccess);
                }
            }else{
                Log.i(TAG , "file.exists");
            }
        }
        return file;
    }
 
    public static String getAbsolutePath(File file){
        return ""+file.getAbsolutePath();
    }
 
    public static boolean deleteFile(File file){
        boolean result;
        if(file!=null&&file.exists()){
            file.delete();
            result = true;
        }else{
            result = false;
        }
        return result;
    }
 
    public static boolean isFile(File file){
        boolean result;
        if(file!=null&&file.exists()&&file.isFile()){
            result=true;
        }else{
            result=false;
        }
        return result;
    }
 
    public static boolean isDirectory(File dir){
        boolean result;
        if(dir!=null&&dir.isDirectory()){
            result=true;
        }else{
            result=false;
        }
        return result;
    }
 
    public static boolean isFileExist(File file){
        boolean result;
        if(file!=null&&file.exists()){
            result=true;
        }else{
            result=false;
        }
        return result;
    }
     
    public static boolean reNameFile(File file , File new_name){
        boolean result;
        if(file!=null&&file.exists()&&file.renameTo(new_name)){
            result=true;
        }else{
            result=false;
        }
        return result;
    }
     
    public static String[] getList(File dir){
        if(dir!=null&&dir.exists())
            return dir.list();
        return null;
    }
 
    public static boolean writeFile(File file , byte[] file_content){
        boolean result;
        FileOutputStream fos;
        
        if(file!=null&&file_content!=null){
            try {
                fos = new FileOutputStream(file);
                try {
                    fos.write(file_content);
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            result = true;
        }else{
        	Logs.d(TAG, "##### writeFile :: file is null or file does not exists or content is null ");
            result = false;
        }
        return result;
    }
 
    public static String readFile(File file){
    	StringBuilder sb = new StringBuilder();
        int readcount=0;
        
        if( file!=null && file.exists() ){
            try {
                FileInputStream fis = new FileInputStream(file);
                Reader in = new InputStreamReader(fis, "UTF-8");
                readcount = (int)file.length();
                char[] tempByte = new char[readcount];
                in.read(tempByte);
//                readcount = (int)file.length();
//                byte[] buffer = new byte[readcount];
//                fis.read(buffer);
                fis.close();
                sb.append(tempByte);
            } catch (Exception e) {
                e.printStackTrace();
                Logs.d(TAG, "##### writeFile :: Exception while FILE IO ");
            }
        } else {
        	Logs.d(TAG, "##### writeFile :: file is null or file does not exists or content is null ");
        }
        return sb.toString();
    }
     
    public static boolean copyFile(File file , String save_file){
        boolean result;
        if(file!=null&&file.exists()){
            try {
                FileInputStream fis = new FileInputStream(file);
                FileOutputStream newfos = new FileOutputStream(save_file);
                int readcount=0;
                byte[] buffer = new byte[1024];
                while((readcount = fis.read(buffer,0,1024))!= -1){
                    newfos.write(buffer,0,readcount);
                }
                newfos.close();
                fis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            result = true;
        }else{
            result = false;
        }
        return result;
    }
     
	//============================================================
	// URL, File name
	//============================================================
	
	// url 파일이름 추출 (with extension)
	public static String convertUrlToFileName(String url)
	{
		String name = new File(url).getName();
		return name;
	}
	
	// TODO: url 파일이름 추출 (without extension)
	public static String convertUrlToFileNameWithoutExt(String url)
	{
		String name = new File(url).getName();
		// TODO
		return name;
	}
	
	
	public static final String REG_EXP_IMAGE_URL = "(?i)http://[a-zA-Z0-9_.\\-%&=?!:;@\"'/]*(?i)(.gif|.jpg|.png|.jpeg)";
	
    // TODO: Not working correctly
    public static List<String> getImageURL(String str) {
		Pattern nonValidPattern = Pattern.compile(REG_EXP_IMAGE_URL);

		List<String> result = new ArrayList<String>();
		Matcher matcher = nonValidPattern.matcher(str);
		while (matcher.find()) {
			result.add(matcher.group(0));
			break;
		}
		return result;
	}
	
    public static Comparator<Object> KeyStringSort = new Comparator<Object>() {
        public int compare(Object s1, Object s2) {
            String ss1 = (String)s1;
            String ss2 = (String)s2;
            return (-1) * ss2.compareTo(ss1);
        }
    };
    
    public static Bitmap getResizedBitmapFromFile(String filePath, int screenW, int screenH, float resizeRatio) 
	{
		//----- Load image as small as possible to reduce memory overhead
        Bitmap pic = null;		
        try 
        {
        	File tempFile = new File(filePath);
        	if( isFileExist(tempFile) == false ) return null;
        	
            // Decode image size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            
            InputStream is = new FileInputStream(filePath);
            BitmapFactory.decodeStream(is, null, options);
    		is.close();

    		// Target bitmap size
    		int smallerW = (screenW > screenH ? screenH : screenW);	// Get screen width
    		int bitmapW = options.outWidth;			// image size Width
    		int bitmapH = options.outHeight;		// image size Height
    		int imageW = (int)( (float)smallerW * resizeRatio );	// Target bitmap width : Resize image according to resize ratio
    		int imageH = (imageW * bitmapH / bitmapW);	// Target bitmap height
            
            int scale = 1;
            int scaleW = bitmapW / imageW;
//            int scaleH = bitmapH / imageH;
            while( scaleW / 2 >= 1 ) {
            	scale = scale * 2;
            	scaleW = scaleW / 2;
            }

            if (scale > 1) {
                // scale to max possible inSampleSize that still yields an image
                // larger than target
                options = new BitmapFactory.Options();
                options.inSampleSize = scale;
                
                InputStream is2 = new FileInputStream(filePath);
                pic = BitmapFactory.decodeStream(is2, null, options);
                is2.close();

                // resize to desired dimensions
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(pic, (int) imageW, (int) imageH, true);
                pic.recycle();
                pic = scaledBitmap;

                System.gc();
            } else {
            	InputStream is2 = new FileInputStream(filePath);
                pic = BitmapFactory.decodeStream(is2);
                is2.close();
            }

        } catch (Exception e) {
            pic = null;
        }
        return pic;		
	}

    
	//============================================================
	// Regular expression
	//============================================================
	
	public static String removeSpecialChars(String str){       
		String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
		str =str.replaceAll(match, " ");
		return str;
	}
	
	
}
