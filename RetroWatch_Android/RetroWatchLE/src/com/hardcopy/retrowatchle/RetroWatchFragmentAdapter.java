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

import java.util.Locale;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class RetroWatchFragmentAdapter extends FragmentPagerAdapter {

	public static final String TAG = "RetroWatchFragmentAdapter";
	
	// Total count
	public static final int FRAGMENT_COUNT = 4;
	
    // Fragment position
    public static final int FRAGMENT_POS_MESSAGE_LIST = 0;
    public static final int FRAGMENT_POS_FILTERS = 1;
    public static final int FRAGMENT_POS_RSS = 2;
    public static final int FRAGMENT_POS_WATCH_CONTROL = 3;
    
	public static final String ARG_SECTION_NUMBER = "section_number";
    
    // System
    private Context mContext = null;
    private IFragmentListener mFragmentListener = null;
    
    private Fragment mMessageListFragment = null;
    private Fragment mFiltersFragment = null;
    private Fragment mRssFragment = null;
    private Fragment mWatchControlFragment = null;
	
	public RetroWatchFragmentAdapter(FragmentManager fm, Context c, IFragmentListener l) {
		super(fm);
		mContext = c;
		mFragmentListener = l;
	}

	@Override
	public Fragment getItem(int position) {
		// getItem is called to instantiate the fragment for the given page.
		Fragment fragment;
		boolean needToSetArguments = false;
		
		if(position == FRAGMENT_POS_MESSAGE_LIST) {
			if(mMessageListFragment == null) {
				mMessageListFragment = new MessageListFragment(mContext, mFragmentListener);
				needToSetArguments = true;
			}
			fragment = mMessageListFragment;
			
		} else if(position == FRAGMENT_POS_FILTERS) {
			if(mFiltersFragment == null) {
				mFiltersFragment = new FiltersFragment(mContext, mFragmentListener);
				needToSetArguments = true;
			}
			fragment = mFiltersFragment;
			
		} else if(position == FRAGMENT_POS_RSS) {
			if(mRssFragment == null) {
				mRssFragment = new RssFragment(mContext, mFragmentListener);
				needToSetArguments = true;
			}
			fragment = mRssFragment;
			
		} else if(position == FRAGMENT_POS_WATCH_CONTROL) {
			if(mWatchControlFragment == null) {
				mWatchControlFragment = new WatchControlFragment(mContext, mFragmentListener);
				needToSetArguments = true;
			}
			fragment = mWatchControlFragment;
			
		} else {
			fragment = null;
		}
		
		// TODO: If you have something to notify to the fragment.
		if(needToSetArguments) {
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, position + 1);
			fragment.setArguments(args);
		}
		
		return fragment;
	}

	@Override
	public int getCount() {
		return FRAGMENT_COUNT;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		Locale l = Locale.getDefault();
		switch (position) {
		case FRAGMENT_POS_MESSAGE_LIST:
			return mContext.getString(R.string.title_messagelist).toUpperCase(l);
		case FRAGMENT_POS_FILTERS:
			return mContext.getString(R.string.title_filters).toUpperCase(l);
		case FRAGMENT_POS_RSS:
			return mContext.getString(R.string.title_rss).toUpperCase(l);
		case FRAGMENT_POS_WATCH_CONTROL:
			return mContext.getString(R.string.title_watchcontrol).toUpperCase(l);
		}
		return null;
	}
	
}

