/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.settings.R;

public class NavigationBarType extends Preference implements ListView.OnItemClickListener{

    private Context mContext;
    private ListView mListView;
    private ListViewAdapter mAdapter;
    private int mCurrent = 0;

    private int[] mResource = new int[] {
            R.drawable.navigationbar_default,
            R.drawable.navigationbar_second
    };

    public NavigationBarType(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NavigationBarType(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mContext = context;
        setLayoutResource(R.layout.navigationbar_type);

        mCurrent = Settings.System.getInt(context.getContentResolver(),
                Settings.System.FREEME_NAVIGATIONBAR_TYPE, 0);
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        mListView = (ListView) view.findViewById(R.id.listview);

        mAdapter = new ListViewAdapter(mContext, mResource);
        mListView.setAdapter(mAdapter);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setOnItemClickListener(this);
        setListViewHeightBasedOnChildren(mListView);
    }

    public void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        updateData(i);
    }

    private void updateData(int index) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.FREEME_NAVIGATIONBAR_TYPE, index);
        mCurrent = index;
        mAdapter.notifyDataSetChanged();
    }

    public class ListViewAdapter extends BaseAdapter {
        private Context context;
        private int[] res;
        private SparseArray<Boolean> checkList = new SparseArray<Boolean>();

        class ViewHolder {
            TextView tvName;
            RadioButton radioBtn;
        }

        public ListViewAdapter(Context context, int[] res) {
            this.res = res;
            this.context = context;
        }

        @Override
        public int getCount() {
            return res.length;
        }

        @Override
        public Object getItem(int position) {
            return res[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.navigationbar_type_item, null);
                holder = new ViewHolder();
                holder.tvName = (TextView) convertView.findViewById(R.id.tv_device_name);
                holder.radioBtn = (RadioButton) convertView.findViewById(R.id.rb_light);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.tvName.setBackgroundResource(res[position]);
            holder.radioBtn.setChecked(mCurrent == position);
            holder.radioBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    updateData(position);
                }
            });

            return convertView;
        }
    }
}
