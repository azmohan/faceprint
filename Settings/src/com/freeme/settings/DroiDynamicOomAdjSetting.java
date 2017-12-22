package com.freeme.settings;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.android.settings.R;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.mediatek.common.MPlugin;
import com.mediatek.common.amsplus.ICustomizedOomExt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Created by xupeng on 3/31/16.
 */
public class DroiDynamicOomAdjSetting extends Activity {
    private static final String TAG = "DroiOomAdjSetting";
    private static final boolean DEBUG = false;
    /**
     * connect apps in the text file
     */
    private static final String APP_CONNECTOR = ",";
    /**
     * connect app and it's adj
     */
    private static final String ADJ_CONNECTOR = "-";
    private ListView mAppListView;
    ICustomizedOomExt mCustomizedOomExt = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.freeme_activity_oom_adj);
        mCustomizedOomExt =
                MPlugin.createInstance(ICustomizedOomExt.class.getName(), this);
        mAppListView = (ListView) findViewById(R.id.lv_set_oom_adj);
        MyListViewAdapter listAdapter = new MyListViewAdapter(this, getAppOomList());
        mAppListView.setAdapter(listAdapter);
    }

    private ArrayList<String> getAppOomList() {
        ArrayList<String> listFromSystem = new ArrayList<>();
        if (mCustomizedOomExt != null) {
        listFromSystem = mCustomizedOomExt.getAppOomAdjList();
        }
        if (listFromSystem == null || listFromSystem.size() == 0) {
            Log.e(TAG, "can not get app lists from ICustomizedOomExt");
        } else {
            if (DEBUG) {
                for (int i = 0; i < listFromSystem.size(); i++) {
                    Log.d(TAG, "listFromSystem[" + i + "] = " + listFromSystem.get(i));
                }
            }
        }
        return listFromSystem;
    }

    class MyListViewAdapter extends BaseAdapter {
        LayoutInflater mInflater;
        private ArrayList<String> mAppList;
        private OomListHolder holder;
        private int selectedIndex = -1;
        private int currentSelection = 0;
        private int preAdj = 16;
        private HashMap<String, Integer> appAdjMap = new HashMap<String, Integer>();

        public MyListViewAdapter(Context context, ArrayList<String> list) {
            mInflater = LayoutInflater.from(context);
            mAppList = list;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.freeme_item_oom_adj_list, null);
                holder = new OomListHolder();
                holder.itemTvName = (TextView) convertView.findViewById(R.id.item_tv_name);
                holder.itemEtAdj = (EditText) convertView.findViewById(R.id.item_et_adj);
                holder.itemBtSet = (Button) convertView.findViewById(R.id.item_bt_set);
                convertView.setTag(holder);
            } else {
                holder = (OomListHolder) convertView.getTag();
            }

            String appOomInfo = mAppList.get(position);
            if (appOomInfo != null) {
                if (DEBUG) Log.d(TAG, "position: " + position + ", appOomInfo = " + appOomInfo);
                final String[] app = appOomInfo.split(ADJ_CONNECTOR);
                if (app != null && app.length == 2) {
                    if (DEBUG) Log.d(TAG, "app info: " + app[0] + ", " + app[1]);
                    holder.itemTvName.setText(app[0]);
                    holder.itemEtAdj.setText(app[1]);
                    holder.itemEtAdj.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count,
                                                      int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before,
                                                  int count) {
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                             if (s != null && !s.toString().isEmpty()) {
                                String adj = s.toString();
                                Pattern pattern = Pattern.compile("^[-+]?[0-9]+");
                                if(pattern.matcher(adj).matches()){
                                    if (DEBUG) Log.d(TAG, "afterTextChanged: " + adj + ", app-adj: " +
                                            (app[0] + ADJ_CONNECTOR + adj));
                                    mAppList.remove(position);
                                    mAppList.add(position, app[0] + ADJ_CONNECTOR + adj);
                                } else {
                                    Log.e(TAG, "input oom adj is not a number");
                                }
                            }
                        }
                    });
                    holder.itemEtAdj.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View view, boolean foc) {
                            Log.d(TAG, "foc = " + foc);
                            if (foc == false) {
                                selectedIndex = position;
                                currentSelection = ((EditText) view).getSelectionStart();
                                if (DEBUG) Log.i(TAG, "Item position: "+position +", focus position: "
                                    + currentSelection + "preAdj = " + preAdj);
                            }
                        }
                    });
                    if (position == selectedIndex) {
                        holder.itemEtAdj.requestFocus();
                        holder.itemEtAdj.setSelection(currentSelection);
                    }
                    holder.itemBtSet.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mCustomizedOomExt != null) {
                                int tempAdj = Integer.valueOf(mAppList.get(position).split(ADJ_CONNECTOR)[1]);
                                if (DEBUG)
                                    Log.d(TAG, "set button clicked, oomAdjInfo = " + app[0] + ADJ_CONNECTOR
                                        + holder.itemEtAdj.getText() + "(" + tempAdj + ")");
                                if (tempAdj > 0 && tempAdj < 17) {
                                    if (tempAdj != preAdj) {
                                        preAdj = tempAdj;
                                        mCustomizedOomExt.setAppOomAdj(app[0] + ADJ_CONNECTOR + tempAdj);
                                    } else {
                                        Log.i(TAG, "set the same adj, ignore");
                                    }
                                } else {
                                    Toast.makeText(DroiDynamicOomAdjSetting.this, "the adj you input must"
                                         + "between 0 to 17!", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });
                }
            } else {
                    Log.e(TAG, "string in the file (" + appOomInfo + ") is not xxx-x");
            }
            return convertView;
        }

        @Override
        public int getCount() {
            return mAppList.size();
        }

        @Override
        public Object getItem(int position) {
            return mAppList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public class OomListHolder {
            TextView itemTvName;
            EditText itemEtAdj;
            Button itemBtSet;
        }
    }
}