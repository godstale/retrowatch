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

import com.hardcopy.retrowatchle.contents.objects.FilterObject;
import com.hardcopy.retrowatchle.utils.Logs;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * This fragment shows user defined message filters.
 */
public class FiltersFragment extends Fragment implements OnClickListener, IAdapterListener {

	private static final String TAG = "FiltersFragment";
	
	// Context
	private Context mContext = null;
	private IFragmentListener mFragmentListener;
	
	// View
	private ListView mListFilter = null;
	private FiltersAdapter mFiltersAdapter = null;
	private Spinner mSpinnerFilterType = null;
	private Spinner mSpinnerCompareType = null;
	private Spinner mSpinnerReplaceType = null;
	private Spinner mSpinnerIconType = null;
	private EditText mEditOrigin = null;
	private EditText mEditReplace = null;
	private Button mButtonDelete = null;
	private Button mButtonAdd = null;
	private Button mButtonNew = null;
	
	// Data
	private FilterObject mCurrentFilterObject = null;
	private ArrayList<FilterObject> mFiltersCache = null;
	
	
	public FiltersFragment(Context c, IFragmentListener l) {
		mContext = c;
		mFragmentListener = l;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Logs.d(TAG, "# FiltersFragment - onCreateView()");
		
		View rootView = inflater.inflate(R.layout.fragment_filters, container, false);
		
		mListFilter = (ListView) rootView.findViewById(R.id.list_filters);
		if(mFiltersAdapter == null)
			mFiltersAdapter = new FiltersAdapter(mContext, R.layout.list_filters, null, this);
		mListFilter.setAdapter(mFiltersAdapter);
		if(mFiltersCache != null) {
			mFiltersAdapter.addFilterAll(mFiltersCache);
			mFiltersAdapter.notifyDataSetChanged();
			mFiltersCache = null;
		}
		
		// Filter object type
		mSpinnerFilterType = (Spinner) rootView.findViewById(R.id.spinner_type);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext, 
				R.array.filter_type_array, 
				R.layout.spinner_simple_item);
		adapter.setDropDownViewResource(R.layout.spinner_dropdown_simple_item);
		mSpinnerFilterType.setPrompt(mContext.getString(R.string.filter_object_type));
		mSpinnerFilterType.setAdapter(adapter);
		mSpinnerFilterType.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mCurrentFilterObject.mType = position;
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		// Compare type
		mSpinnerCompareType = (Spinner) rootView.findViewById(R.id.spinner_compare_type);
		ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(mContext, 
				R.array.filter_matching_type_array, 
				R.layout.spinner_simple_item);
		adapter2.setDropDownViewResource(R.layout.spinner_dropdown_simple_item);
		mSpinnerCompareType.setPrompt(mContext.getString(R.string.filter_compare_type));
		mSpinnerCompareType.setAdapter(adapter2);
		mSpinnerCompareType.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mCurrentFilterObject.mCompareType = position;
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		// Replace method
		mSpinnerReplaceType = (Spinner) rootView.findViewById(R.id.spinner_replace_type);
		ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(mContext, 
				R.array.filter_replace_type_array, 
				R.layout.spinner_simple_item);
		adapter3.setDropDownViewResource(R.layout.spinner_dropdown_simple_item);
		mSpinnerReplaceType.setPrompt(mContext.getString(R.string.filter_replace_type));
		mSpinnerReplaceType.setAdapter(adapter3);
		mSpinnerReplaceType.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mCurrentFilterObject.mReplaceType = position;
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		// Icon
		mSpinnerIconType = (Spinner) rootView.findViewById(R.id.spinner_icon_type);
		ArrayAdapter<CharSequence> adapter4 = ArrayAdapter.createFromResource(mContext, 
				R.array.filter_icon_type_array, 
				R.layout.spinner_simple_item2);
		adapter4.setDropDownViewResource(R.layout.spinner_dropdown_simple_item);
		mSpinnerIconType.setPrompt(mContext.getString(R.string.filter_icon));
		mSpinnerIconType.setAdapter(adapter4);
		mSpinnerIconType.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mCurrentFilterObject.mIconType = position - 1;
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		mEditOrigin = (EditText) rootView.findViewById(R.id.edit_origin);
		mEditReplace = (EditText) rootView.findViewById(R.id.edit_replace);
		
		mButtonDelete = (Button) rootView.findViewById(R.id.btn_delete);
		mButtonDelete.setOnClickListener(this);
		mButtonAdd = (Button) rootView.findViewById(R.id.btn_add);
		mButtonAdd.setOnClickListener(this);
		mButtonNew = (Button) rootView.findViewById(R.id.btn_new);
		mButtonNew.setOnClickListener(this);

		// Make new filter object and set data on widget
		makeDefaultFilterObject();
		
		return rootView;
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}
	
	@Override 
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.btn_delete:
			if(mCurrentFilterObject == null || mCurrentFilterObject.mId < 0)
				break;
			mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_REQUEST_DELETE_FILTER, 
					0, 0, null, null, (Object)mCurrentFilterObject);
			
			// Close soft keyboard
			InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mEditOrigin.getWindowToken(), 0);
			imm.hideSoftInputFromWindow(mEditReplace.getWindowToken(), 0);
			break;
			
		case R.id.btn_add:
			if(mCurrentFilterObject != null) {
				String original = mEditOrigin.getText().toString();
				String replace = mEditReplace.getText().toString();
				
				// Check input fields
				if(mCurrentFilterObject.mType < FilterObject.FILTER_TYPE_ALL) {
					Toast.makeText(mContext, 
							mContext.getString(R.string.warning_select_type), 
							Toast.LENGTH_SHORT).show();
				} else if(mCurrentFilterObject.mCompareType <= FilterObject.MATCHING_TYPE_NONE) {
					Toast.makeText(mContext, 
							mContext.getString(R.string.warning_select_compare_type), 
							Toast.LENGTH_SHORT).show();
				} else if(mCurrentFilterObject.mReplaceType <= FilterObject.REPLACE_TYPE_NONE) {
					Toast.makeText(mContext, 
							mContext.getString(R.string.warning_select_replace_type), 
							Toast.LENGTH_SHORT).show();
				} else if(original == null || original.isEmpty()) {
					Toast.makeText(mContext, 
							mContext.getString(R.string.warning_type_target), 
							Toast.LENGTH_SHORT).show();
				} else {
					mCurrentFilterObject.mOriginalString = original;
					mCurrentFilterObject.mReplaceString = replace;
					
					if(mButtonAdd.getText().equals(mContext.getString(R.string.command_add))) {
						// Add filter
						mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_REQUEST_ADD_FILTER, 0, 0, null, null, 
								mCurrentFilterObject);
						makeDefaultFilterObject();		// Make new filter object and set data on widget
					} else {
						// Edit filter
						mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_REQUEST_EDIT_FILTER, 0, 0, null, null, 
								mCurrentFilterObject);
					}
				}
				
				// Close soft keyboard
				InputMethodManager imm2 = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm2.hideSoftInputFromWindow(mEditOrigin.getWindowToken(), 0);
				imm2.hideSoftInputFromWindow(mEditReplace.getWindowToken(), 0);
			}
			break;
			
		case R.id.btn_new:
			makeDefaultFilterObject();		// Make new filter object and set data on widget
			
			// Close soft keyboard
			InputMethodManager imm3 = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm3.hideSoftInputFromWindow(mEditOrigin.getWindowToken(), 0);
			imm3.hideSoftInputFromWindow(mEditReplace.getWindowToken(), 0);
			break;
		}	// End of switch()
	}	// End of onClick()
	
	@Override
	public void OnAdapterCallback(int msgType, int arg0, int arg1, String arg2, String arg3, Object arg4) {
		switch(msgType) {
		case IAdapterListener.CALLBACK_FILTER_SELECTED:
			if(arg4 != null) {
				FilterObject filter = (FilterObject) arg4;
				selectFilter(filter);
			}
			break;
			
		default:
			break;
		}
	}
	
	
	/**
	 * Make a filter instance and initialize input field
	 */
	private void makeDefaultFilterObject() {
		mCurrentFilterObject = new FilterObject();
		setFilterInfoOnWidget(mCurrentFilterObject);
		
		mButtonAdd.setEnabled(true);
		mButtonAdd.setText(mContext.getString(R.string.command_add));
		mButtonDelete.setEnabled(false);
	}
	
	/**
	 * Fill input field with specified filter object
	 * @param filter	filter object
	 */
	private void setFilterInfoOnWidget(FilterObject filter) {
		mSpinnerFilterType.setSelection(filter.mType);
		mSpinnerCompareType.setSelection(filter.mCompareType);
		mSpinnerReplaceType.setSelection(filter.mReplaceType);
		if(filter.mIconType < 0)
			mSpinnerIconType.setSelection(0);
		else 
			mSpinnerIconType.setSelection(filter.mIconType + 1);
		
		if(filter.mOriginalString != null)
			mEditOrigin.setText(filter.mOriginalString);
		else
			mEditOrigin.setText("");
		if(filter.mReplaceString != null)
			mEditReplace.setText(filter.mReplaceString);
		else
			mEditReplace.setText("");
	}
	
	
	/**
	 * User selected a filter from list
	 * @param filter	selected filter object
	 */
	public void selectFilter(FilterObject filter) {
		filter.copyTo(mCurrentFilterObject);
		setFilterInfoOnWidget(mCurrentFilterObject);
		
		mButtonAdd.setText(mContext.getString(R.string.command_edit));
		mButtonDelete.setEnabled(true);
	}
	
	/**
	 * Add filter to list adapter.
	 * @param object	filter object to add
	 */
	public void addFilter(FilterObject object) {
		if(object != null && mFiltersAdapter != null) {
			mFiltersAdapter.addFilter(object);
			mFiltersAdapter.notifyDataSetChanged();
			if(object.mId == mCurrentFilterObject.mId)
				makeDefaultFilterObject();		// Make new filter object and set data on widget
		}
	}
	
	/**
	 * Add all filters to list adapter.
	 * @param objList	Array list of filter object
	 */
	public void addFilterAll(ArrayList<FilterObject> objList) {
		if(mFiltersAdapter == null) {
			mFiltersCache = objList;
			return;
		}
		if(objList != null) {
			mFiltersAdapter.addFilterAll(objList);
			mFiltersAdapter.notifyDataSetChanged();
		}
	}
	
	/**
	 * Edit current editing filter object with user input
	 * @param object	filter object with user input
	 */
	public void editFilter(FilterObject object) {
		if(object != null && mFiltersAdapter != null) {
			mFiltersAdapter.editFilter(object);
			mFiltersAdapter.notifyDataSetChanged();
			if(object.mId == mCurrentFilterObject.mId)
				setFilterInfoOnWidget(object);
		}
	}
	
	/**
	 * Delete selected filter object
	 * @param id	filter object's ID
	 */
	public void deleteFilter(int id) {
		mFiltersAdapter.deleteFilter(id);
		mFiltersAdapter.notifyDataSetChanged();
		if(id == mCurrentFilterObject.mId)
			makeDefaultFilterObject();		// Make new filter object and set data on widget
	}
	
	/**
	 * Clear list
	 */
	public void deleteFilterAll() {
		mFiltersAdapter.deleteFilterAll();
		mFiltersAdapter.notifyDataSetChanged();
	}
	
	
}
