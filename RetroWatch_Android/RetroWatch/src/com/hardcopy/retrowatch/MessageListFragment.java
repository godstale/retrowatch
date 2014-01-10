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

package com.hardcopy.retrowatch;

import java.util.ArrayList;

import com.hardcopy.retrowatch.contents.objects.ContentObject;
import com.hardcopy.retrowatch.utils.Logs;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * This fragment shows messages to be sent to watch.
 */
public class MessageListFragment extends Fragment {

	private static final String TAG = "MessageListFragment";
	
	private Context mContext = null;
	private IFragmentListener mFragmentListener;
	
	private ListView mListMessage = null;
	private MessageListAdapter mMessageListAdapter = null;
	
	public MessageListFragment(Context c, IFragmentListener l) {
		mContext = c;
		mFragmentListener = l;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Logs.d(TAG, "# MessageListFragment - onCreateView()");
		
		View rootView = inflater.inflate(R.layout.fragment_message_list, container, false);

		mListMessage = (ListView) rootView.findViewById(R.id.list_message);
		if(mMessageListAdapter == null)
			mMessageListAdapter = new MessageListAdapter(mContext, R.layout.list_message_list, null);
		mListMessage.setAdapter(mMessageListAdapter);
		
		return rootView;
	}
	
	public void addMessage(ContentObject object) {
		if(object != null) {
			mMessageListAdapter.addMessage(object);
			mMessageListAdapter.notifyDataSetChanged();
		}
	}
	
	public void addMessageAll(ArrayList<ContentObject> objList) {
		if(objList != null) {
			mMessageListAdapter.addMessageAll(objList);
			mMessageListAdapter.notifyDataSetChanged();
		}
	}
	
	public void deleteMessage(int id) {
		mMessageListAdapter.deleteMessage(id);
		mMessageListAdapter.notifyDataSetChanged();
	}
	
	public void deleteMessageByType(int type) {
		mMessageListAdapter.deleteMessageByType(type);
		mMessageListAdapter.notifyDataSetChanged();
	}
	
	public void deleteMessageByTypeAndName(int type, String packageName) {
		mMessageListAdapter.deleteMessageByTypeAndName(type, packageName);
		mMessageListAdapter.notifyDataSetChanged();
	}
	
	public void deleteMessageAll() {
		mMessageListAdapter.deleteMessageAll();
		mMessageListAdapter.notifyDataSetChanged();
	}
	
}
