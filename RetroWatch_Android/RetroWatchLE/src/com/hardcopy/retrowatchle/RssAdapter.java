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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hardcopy.retrowatchle.contents.objects.CPObject;
import com.hardcopy.retrowatchle.utils.Utils;

public class RssAdapter extends ArrayAdapter<CPObject> {

	public static final String tag = "RssAdapter";
	
	private Context mContext = null;
	private IAdapterListener mAdapterListener = null;
	
	private ArrayList<CPObject> mCPList = null;
	
	
	public RssAdapter(Context c, int resId, ArrayList<CPObject> itemList, IAdapterListener l) {
		super(c, resId, itemList);
		mContext = c;
		mAdapterListener = l;
		
		if(itemList == null)
			mCPList = new ArrayList<CPObject>();
		else
			mCPList = itemList;
	}
	
	public void addRss(CPObject co) {
		mCPList.add(co);
	}
	
	public void addRssAll(ArrayList<CPObject> itemList) {
		if(itemList == null)
			return;
		for(int i=0; i<itemList.size(); i++)
			addRss(itemList.get(i));
	}
	
	public void editRss(CPObject co) {
		for(CPObject cpo : mCPList) {
			if(cpo.mId == co.mId) {
				cpo.mId = co.mId;
				cpo.mName = co.mName;
				cpo.mURL = co.mURL;
			}
		}
	}
	
	public void deleteRss(int id) {
		for(int i = mCPList.size() - 1; -1 < i; i--) {
			CPObject cpo = mCPList.get(i);
			if(cpo.mId == id) {
				mCPList.remove(i);
			}
		}
	}
	
	public void deleteRssAll() {
		if(mCPList != null)
			mCPList.clear();
	}
	
	@Override
	public int getCount() {
		return mCPList.size();
	}
	@Override
	public CPObject getItem(int position) { 
		return mCPList.get(position); 
	}
	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		View v = convertView;
		CPObject cpo = getItem(position);
		
		if(v == null) {
			LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = li.inflate(R.layout.list_rss, null);
			holder = new ViewHolder();
			
			holder.mPosition = position;
			holder.mLayoutContainer = (LinearLayout) v.findViewById(R.id.rss_item_container);
			holder.mLayoutContainer.setTag(cpo);
			holder.mLayoutContainer.setOnTouchListener(mListItemTouchListener);
			holder.mTextUrl = (TextView) v.findViewById(R.id.rss_url);
			holder.mTextTitle = (TextView) v.findViewById(R.id.rss_title);
			
			v.setTag(holder);
		} else {
			holder = (ViewHolder) v.getTag();
			holder.mLayoutContainer.setTag(cpo);
		}
		
		if (cpo != null && holder != null) {
			holder.mTextUrl.setText(cpo.mURL);
			holder.mTextTitle.setText(cpo.mName);
		}
		
		return v;
	}	// End of getView()
	
	
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
	
	private void processOnClickEvent(View v) {
		switch(v.getId())
		{
			case R.id.rss_item_container:
				CPObject cpo = (CPObject) v.getTag();
				mAdapterListener.OnAdapterCallback(IAdapterListener.CALLBACK_RSS_SELECTED, 0, 0, null, null, cpo);
				
				break;
		}	// End of switch()
	}
	
	public class ViewHolder {
		public int mPosition = -1;
		public LinearLayout mLayoutContainer = null;
		public TextView mTextTitle = null;
		public TextView mTextUrl = null;
	}
	
}

