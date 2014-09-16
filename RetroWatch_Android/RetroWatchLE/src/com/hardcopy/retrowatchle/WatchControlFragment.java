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

import com.hardcopy.retrowatchle.utils.Settings;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * This fragment shows user defined message filters.
 */
public class WatchControlFragment extends Fragment {

	private Context mContext = null;
	private IFragmentListener mFragmentListener = null;
	
	private EditText mEditGmailAddr = null;
	private Spinner mSpinnerClockStyle = null;
	private Spinner mSpinnerIndicator = null;
	private CheckBox mCheckBackground;
	
	private int mPresetClockStyle = -1;
	private int mPresetIndicator = -1;

	public WatchControlFragment(Context c, IFragmentListener l) {
		mContext = c;
		mFragmentListener = l;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_watch_control, container, false);
		
		mEditGmailAddr = (EditText) rootView.findViewById(R.id.edit_email_addr);
		String defaultAddr = Settings.getInstance(mContext).getGmailAddress();
		if(defaultAddr != null && !defaultAddr.isEmpty())
			mEditGmailAddr.setText(defaultAddr);
		mEditGmailAddr.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String addr = s.toString();
				if(addr != null && !addr.isEmpty()) {
					mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_REQUEST_SET_EMAIL_ADDRESS, 
							0, 0, addr, null, null);
				}
			}
			@Override public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override public void afterTextChanged(Editable s) {}
		});
		
		mSpinnerClockStyle = (Spinner) rootView.findViewById(R.id.spinner_clock_style);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext, 
				R.array.clock_style_array, 
				R.layout.spinner_simple_item2);
		adapter.setDropDownViewResource(R.layout.spinner_dropdown_simple_item);
		mSpinnerClockStyle.setPrompt(mContext.getString(R.string.clock_style_title));
		mSpinnerClockStyle.setAdapter(adapter);
		mSpinnerClockStyle.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if(mPresetClockStyle > -1 && mPresetClockStyle != position) {
					mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_REQUEST_CLOCK_STYLE, 
							position, 0, null, null, null);
				}
				mPresetClockStyle = position;
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		mSpinnerIndicator = (Spinner) rootView.findViewById(R.id.spinner_show_indicator);
		ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(mContext, 
				R.array.clock_indicator_array, 
				R.layout.spinner_simple_item2);
		adapter.setDropDownViewResource(R.layout.spinner_dropdown_simple_item);
		mSpinnerIndicator.setPrompt(mContext.getString(R.string.clock_indicator_title));
		mSpinnerIndicator.setAdapter(adapter2);
		mSpinnerIndicator.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if(mPresetIndicator > -1 && mPresetIndicator != position) {
					mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_REQUEST_SHOW_INDICATOR, 
							position, 0, null, null, null);
				}
				mPresetIndicator = position;
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		// 'Run in background' setting
		mCheckBackground = (CheckBox) rootView.findViewById(R.id.check_background_service);
		mCheckBackground.setChecked(Settings.getInstance(mContext).getRunInBackground());
		mCheckBackground.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Settings.getInstance(mContext).setRunInBackground(isChecked);
				mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_REQUEST_RUN_IN_BACKGROUND, 0, 0, null, null,null);
			}
		});
		
		return rootView;
	}
	
	
}
