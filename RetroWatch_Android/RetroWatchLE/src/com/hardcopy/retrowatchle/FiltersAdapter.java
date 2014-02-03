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
import com.hardcopy.retrowatchle.utils.Utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FiltersAdapter extends ArrayAdapter<FilterObject> {

	public static final String tag = "FiltersAdapter";
	
	private Context mContext = null;
	private IAdapterListener mAdapterListener = null;
	
	private ArrayList<FilterObject> mFilterList = null;
	
	
	public FiltersAdapter(Context c, int resId, ArrayList<FilterObject> itemList, IAdapterListener l) {
		super(c, resId, itemList);
		mContext = c;
		mAdapterListener = l;
		
		if(itemList == null)
			mFilterList = new ArrayList<FilterObject>();
		else
			mFilterList = itemList;
	}
	
	public void addFilter(FilterObject co) {
		mFilterList.add(co);
	}
	
	public void addFilterAll(ArrayList<FilterObject> itemList) {
		if(itemList == null)
			return;
		for(int i=0; i<itemList.size(); i++)
			addFilter(itemList.get(i));
	}
	
	public void editFilter(FilterObject co) {
		for(FilterObject fo : mFilterList) {
			if(fo.mId == co.mId) {
				fo.mId = co.mId;
				fo.mType = co.mType;
				fo.mCompareType = co.mCompareType;
				fo.mReplaceType = co.mReplaceType;
				fo.mOriginalString = co.mOriginalString;
				fo.mReplaceString = co.mReplaceString;
			}
		}
	}
	
	public void deleteFilter(int id) {
		for(int i = mFilterList.size() - 1; -1 < i; i--) {
			FilterObject fo = mFilterList.get(i);
			if(fo.mId == id) {
				mFilterList.remove(i);
			}
		}
	}
	
	public void deleteFilterAll() {
		mFilterList.clear();
	}
	
	@Override
	public int getCount() {
		return mFilterList.size();
	}
	@Override
	public FilterObject getItem(int position) { 
		return mFilterList.get(position); 
	}
	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		View v = convertView;
		FilterObject filter = getItem(position);
		
		if(v == null) {
			LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = li.inflate(R.layout.list_filters, null);
			holder = new ViewHolder();
			
			holder.mPosition = position;
			holder.mLayoutContainer = (LinearLayout) v.findViewById(R.id.filter_item_container);
			holder.mLayoutContainer.setTag(filter);
			holder.mLayoutContainer.setOnTouchListener(mListItemTouchListener);
			holder.mTextOrigin = (TextView) v.findViewById(R.id.filter_origin);
			holder.mTextConverted = (TextView) v.findViewById(R.id.filter_converted);
			
			v.setTag(holder);
		} else {
			holder = (ViewHolder) v.getTag();
			holder.mLayoutContainer.setTag(filter);
		}
		
		if (filter != null && holder != null) {
			holder.mTextOrigin.setText(new StringBuilder()
					.append(Utils.getFilterTypeString(filter.mType))
					.append(", ")
					.append(Utils.getMatchingTypeString(filter.mCompareType))
					.append(" : ").toString());
			if(filter.mOriginalString != null) {
				holder.mTextOrigin.append(filter.mOriginalString);
			}
			if(filter.mReplaceString != null) {
				holder.mTextConverted.setText( Utils.getReplaceTypeString(filter.mReplaceType) + ": "
						+ ((filter.mReplaceString == null || filter.mReplaceString.isEmpty()) ? 
								mContext.getString(R.string.filter_text_blank) : filter.mReplaceString));
			} else {
				holder.mTextConverted.setText(" ");
			}
		}
		
		return v;
	}	// End of getView()
	
	/**
	 * Sometimes onClick listener misses event.
	 * Uses touch listener instead.
	 */
	private OnTouchListener mListItemTouchListener = new OnTouchListener() {
		private float startx = 0;
		private float starty = 0;
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if(event.getAction()==MotionEvent.ACTION_DOWN){
				startx = event.getX();
				starty = event.getY();
			}
			if(event.getAction()==MotionEvent.ACTION_UP){
				// if action-up occurred within 30px from start, process as click event. 
				if( (startx - event.getX())*(startx - event.getX()) + (starty - event.getY())*(starty - event.getY()) < 900 ) {
					processOnClickEvent(v);
				}
			}
			return true;
		}
	};	// End of new OnTouchListener
	
	/**
	 * Process click event
	 * @param v		Clicked view
	 */
	private void processOnClickEvent(View v) {
		switch(v.getId())
		{
			case R.id.filter_item_container:
				FilterObject filter = (FilterObject) v.getTag();
				mAdapterListener.OnAdapterCallback(IAdapterListener.CALLBACK_FILTER_SELECTED, 0, 0, null, null, filter);
				
				break;
		}	// End of switch()
	}
	
	/**
	 * Hold every child view of each list item.
	 */
	public class ViewHolder {
		public int mPosition = -1;
		public LinearLayout mLayoutContainer = null;
		public TextView mTextOrigin = null;
		public TextView mTextConverted = null;
	}
	
}

