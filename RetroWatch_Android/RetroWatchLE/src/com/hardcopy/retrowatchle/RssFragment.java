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

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.hardcopy.retrowatchle.contents.objects.CPObject;
import com.hardcopy.retrowatchle.utils.Constants;
import com.hardcopy.retrowatchle.utils.Logs;

public class RssFragment extends Fragment implements OnClickListener, IAdapterListener {
	private static final String TAG = "RssFragment";
	
	private Context mContext = null;
	private IFragmentListener mFragmentListener;
	
	private ListView mListRss = null;
	private RssAdapter mRssAdapter = null;
	
	private TextView mBtnRssSearch = null;
	private EditText mEditUrl = null;
	private EditText mEditTitle = null;
	private Button mButtonDelete = null;
	private Button mButtonAdd = null;
	private Button mButtonNew = null;
	
	private CPObject mCurrentRssObject = null;
	
	
	public RssFragment(Context c, IFragmentListener l) {
		mContext = c;
		mFragmentListener = l;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Logs.d(TAG, "# RssFragment - onCreateView()");
		
		View rootView = inflater.inflate(R.layout.fragment_rss, container, false);
		
		mListRss = (ListView) rootView.findViewById(R.id.list_rss);
		if(mRssAdapter == null)
			mRssAdapter = new RssAdapter(mContext, R.layout.list_rss, null, this);
		mListRss.setAdapter(mRssAdapter);
		
		mBtnRssSearch = (Button) rootView.findViewById(R.id.btn_find_more);
		mBtnRssSearch.setOnClickListener(this);
		mEditTitle = (EditText) rootView.findViewById(R.id.edit_title);
		mEditUrl = (EditText) rootView.findViewById(R.id.edit_url);
		
		mButtonDelete = (Button) rootView.findViewById(R.id.btn_delete);
		mButtonDelete.setOnClickListener(this);
		mButtonAdd = (Button) rootView.findViewById(R.id.btn_add);
		mButtonAdd.setOnClickListener(this);
		mButtonNew = (Button) rootView.findViewById(R.id.btn_new);
		mButtonNew.setOnClickListener(this);

		// Make new RSS object and set data on widget
		makeDefaultRssObject();
		
		return rootView;
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		// Request RSS list
		requestRssUpdate();
	}
	
	@Override 
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.btn_delete:
			if(mCurrentRssObject == null || mCurrentRssObject.mId < 0)
				break;
			mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_REQUEST_DELETE_RSS, 
					0, 0, null, null, (Object)mCurrentRssObject);
			
			// Close soft keyboard
			InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mEditUrl.getWindowToken(), 0);
			imm.hideSoftInputFromWindow(mEditTitle.getWindowToken(), 0);
			break;
			
		case R.id.btn_add:
			if(mCurrentRssObject != null) {
				String url = mEditUrl.getText().toString();
				String title = mEditTitle.getText().toString();
				
				// Check input fields
				if(url == null || url.isEmpty() || title == null || title.isEmpty()) {
					Toast.makeText(mContext, 
							mContext.getString(R.string.warning_type_rss_field), 
							Toast.LENGTH_SHORT).show();
				} else {
					mCurrentRssObject.mName = title;
					mCurrentRssObject.mURL = url;
					
					if(mButtonAdd.getText().equals(mContext.getString(R.string.command_add))) {
						// Add RSS
						mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_REQUEST_ADD_RSS, 0, 0, null, null, 
								mCurrentRssObject);
						// makeDefaultRssObject();		// Make new RSS object and set data on widget
					} else {
						// Edit RSS
						mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_REQUEST_EDIT_RSS, 0, 0, null, null, 
								mCurrentRssObject);
						// makeDefaultRssObject();		// Make new RSS object and set data on widget
					}
				}
				
				// Close soft keyboard
				InputMethodManager imm2 = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm2.hideSoftInputFromWindow(mEditTitle.getWindowToken(), 0);
				imm2.hideSoftInputFromWindow(mEditUrl.getWindowToken(), 0);
			}
			break;
			
		case R.id.btn_new:
			makeDefaultRssObject();		// Make new RSS object and set data on widget
			
			// Close soft keyboard
			InputMethodManager imm3 = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm3.hideSoftInputFromWindow(mEditTitle.getWindowToken(), 0);
			imm3.hideSoftInputFromWindow(mEditUrl.getWindowToken(), 0);
			break;
			
		case R.id.btn_find_more:
			Intent i2 = new Intent(mContext, RetroWebViewActivity.class);
			i2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			i2.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			Uri u = Uri.parse(Constants.PROJECT_FEED_SEARCH_URL); 
			i2.setData(u);
			startActivity(i2);
			break;
			
		}	// End of switch()
	}	// End of onClick()
	
	@Override
	public void OnAdapterCallback(int msgType, int arg0, int arg1, String arg2, String arg3, Object arg4) {
		switch(msgType) {
		case IAdapterListener.CALLBACK_RSS_SELECTED:
			if(arg4 != null) {
				CPObject cp = (CPObject) arg4;
				selectRss(cp);
			}
			break;
			
		default:
			break;
		}
	}
	
	
	
	private void makeDefaultRssObject() {
		mCurrentRssObject = new CPObject();
		setRssInfoOnWidget(mCurrentRssObject);
		
		mButtonAdd.setEnabled(true);
		mButtonAdd.setText(mContext.getString(R.string.command_add));
		mButtonDelete.setEnabled(false);
	}
	
	private void setRssInfoOnWidget(CPObject cpo) {
		if(mEditTitle == null || mEditUrl == null)
			return;
		
		if(cpo.mName != null)
			mEditTitle.setText(cpo.mName);
		else
			mEditTitle.setText("");
		if(cpo.mURL != null)
			mEditUrl.setText(cpo.mURL);
		else
			mEditUrl.setText("");
	}
	
	private void requestRssUpdate() {
		mRssAdapter.deleteRssAll();
		mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_REQUEST_RSS, 0, 0, null, null, null);
	}
	
	
	
	public void selectRss(CPObject cpo) {
		cpo.copyTo(mCurrentRssObject);
		setRssInfoOnWidget(mCurrentRssObject);
		
		mButtonAdd.setText(mContext.getString(R.string.command_edit));
		mButtonDelete.setEnabled(true);
	}
	
	public void addRss(CPObject object) {
		if(object != null) {
			mRssAdapter.addRss(object);
			mRssAdapter.notifyDataSetChanged();
			makeDefaultRssObject();		// Make new RSS object and set data on widget
		}
	}
	
	public void addRssAll(ArrayList<CPObject> objList) {
		if(objList != null) {
			mRssAdapter.addRssAll(objList);
			mRssAdapter.notifyDataSetChanged();
		}
	}
	
	public void editRss(CPObject object) {
		if(object != null) {
			mRssAdapter.editRss(object);
			mRssAdapter.notifyDataSetChanged();
			if(object.mId == mCurrentRssObject.mId)
				setRssInfoOnWidget(object);
		}
	}
	
	public void deleteRss(int id) {
		mRssAdapter.deleteRss(id);
		mRssAdapter.notifyDataSetChanged();
		if(id == mCurrentRssObject.mId)
			makeDefaultRssObject();		// Make new RSS object and set data on widget
	}
	
	public void deleteRssAll() {
		mRssAdapter.deleteRssAll();
		mRssAdapter.notifyDataSetChanged();
	}
	
}
