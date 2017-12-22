/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.droi.systemui.statusbar;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import com.android.internal.widget.ScrollNumberPicker;
import com.android.systemui.R;

public class QuickSettingsPreferenceActivity extends PreferenceActivity
    implements Preference.OnPreferenceChangeListener {
    
    private static final String TAG = "QuickSettingsPreferenceActivity";
    private static final String TOGGLE_NETWORK_PREFERENCE = "toggle_network_speed";
    private static final String NOTIFICATION_CUSTOM_PREFERENCE = "notifications_custom";
    private static final String TOGGLE_CUSTOM_PREFERENCE = "toggles_custom";
    private static final String TOGGLE_CARRIER_LABEL_PREFERENCE = "toggle_carrier_label";
    private static final String TOGGLE_UNEXPAND_STATUSBAR_PREFERENCE = "toggle_unexpand_statusbar";
    private static final String TOGGLE_SHOW_BATTERY_PREFERENCE = "toggle_battery_percent";   
    private static final String TOGGLE_STATUSBAR_DATA_USAGE = "toggle_statusbar_data_usage";
    private static final String TOGGLE_STATUSBAR_BATTERY_MODE = "toggle_statusbar_battery_mode";

    private static final String SHOW_STATUSBAR_CARRIER = "ShowStatusBarCarrierLabel";
    private static final String SHOW_STATUSBAR_NETWORK_SPEED = "ShowStatusBarNetworkSpeed";
    private static final String SHOW_STATUSBAR_DATA_USAGE = "show_statusbar_data_usage";
    private static final String SHOW_STATUSBAR_BATTERY_MODE = "show_statusbar_battery_mode";
    private static final String SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";

    private static final String NOTIFICATION_SET = "com.android.settings.ACTION_START_NOTIFICATION_SET";
    private static final String NETWORK_REFRESH_RATE = "network_speed_refresh_rate";
    private static final String CUSTOM_CARRIER_LABEL = "custom_carrier_label";

    private Context mContext;
    private SharedPreferences mSp;
    private Editor editor;

    private SwitchPreference mNetWorkPreference;
    private SwitchPreference mCarrierPreference;
    private SwitchPreference mUnexpandSBPreference;
    private SwitchPreference mBatteryPreference;
    private SwitchPreference mDataUsagePreference;
    private SwitchPreference mBatteryModePreference;
    private Preference mCustomNotifications;
    private Preference mCustomToggles;
    private Preference mNetworkRefreshRate;
    private Preference mCusCarrierPreference;
    private PreferenceCategory mNotificationCategory;
    private PreferenceCategory mStatusBarCategory;
    private PreferenceCategory mSettingsCategory;

    private AlertDialog mDialog;
    private AlertDialog mConfirmDialog;
    private ScrollNumberPicker mNum_sec;
    private static int mTime_val = 3;
    private EditText mCarrierEditText;

    private ActionBar mActionBar;
    
    //*/add by tyd wangalei 2015929 for second menu sort
    private ContentResolver mResolver;
    //*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);   

        mContext = this;
        mResolver=this.getContentResolver();
        mActionBar = getActionBar();
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        mSp = getSharedPreferences("QuickSettings", Context.MODE_PRIVATE);
        editor = mSp.edit();
        
        addPreferencesFromResource(R.xml.droi_quicksettings_settings);

        mNotificationCategory = (PreferenceCategory) findPreference("notification_category");
        mStatusBarCategory = (PreferenceCategory) findPreference("statusbar_category");
        mSettingsCategory = (PreferenceCategory) findPreference("settings_category");

        mCustomNotifications = (Preference) findPreference(NOTIFICATION_CUSTOM_PREFERENCE);
        mCustomNotifications.setOnPreferenceChangeListener(this);

        mCustomToggles = (Preference) findPreference(TOGGLE_CUSTOM_PREFERENCE);
        mCustomToggles.setOnPreferenceChangeListener(this);

        mBatteryModePreference = (SwitchPreference) findPreference(TOGGLE_STATUSBAR_BATTERY_MODE);
        mBatteryModePreference.setOnPreferenceChangeListener(this);
        mNotificationCategory.removePreference(mBatteryModePreference);

        mUnexpandSBPreference = (SwitchPreference) findPreference(TOGGLE_UNEXPAND_STATUSBAR_PREFERENCE);
        mUnexpandSBPreference.setOnPreferenceChangeListener(this);
        mNotificationCategory.removePreference(mUnexpandSBPreference);

        mDataUsagePreference = (SwitchPreference) findPreference(TOGGLE_STATUSBAR_DATA_USAGE);
        mDataUsagePreference.setOnPreferenceChangeListener(this);

        mCarrierPreference = (SwitchPreference) findPreference(TOGGLE_CARRIER_LABEL_PREFERENCE);
        mCarrierPreference.setOnPreferenceChangeListener(this);

        mNetWorkPreference = (SwitchPreference) findPreference(TOGGLE_NETWORK_PREFERENCE);
        mNetWorkPreference.setOnPreferenceChangeListener(this);        
        
        mBatteryPreference = (SwitchPreference) findPreference(TOGGLE_SHOW_BATTERY_PREFERENCE);
        mBatteryPreference.setOnPreferenceChangeListener(this);

        mNetworkRefreshRate = (Preference) findPreference(NETWORK_REFRESH_RATE);
        mTime_val = mSp.getInt("RefreshTime", mTime_val);
        mNetworkRefreshRate.setSummary(this.getResources()
                .getString(R.string.quicksettings_refresh_interval_summary, mTime_val));

        mCusCarrierPreference = (Preference) findPreference(CUSTOM_CARRIER_LABEL);
        String cusCarrier = mSp.getString("CustomCarrierLabel", "");
        if(!"".equals(cusCarrier)) {
            mCusCarrierPreference.setSummary(cusCarrier);
        } else {
            mCusCarrierPreference.setSummary(R.string.custom_carrier_summary);
        }

        CreateDialog();
        CreateConfirmDialog();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        
        return false;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        /*
        if(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.UNEXPAND_STATUSBAR, 0) != 0){
            mUnexpandSBPreference.setChecked(true);
        } else {
            mUnexpandSBPreference.setChecked(false);
        }*/
        
        if(Settings.System.getInt(mContext.getContentResolver(),
                SHOW_STATUSBAR_CARRIER, 0) != 0){
            mCarrierPreference.setChecked(true);
        } else {
            mCarrierPreference.setChecked(false);
        }
        
        if(Settings.System.getInt(mContext.getContentResolver(),
                SHOW_STATUSBAR_NETWORK_SPEED, 0) != 0){
            mNetWorkPreference.setChecked(true);
        } else {
            mNetWorkPreference.setChecked(false);
        }

        if(Settings.System.getInt(mContext.getContentResolver(),
                SHOW_BATTERY_PERCENT, 0) != 0){
            mBatteryPreference.setChecked(true);
        } else {
            mBatteryPreference.setChecked(false);
        }
        
        if(Settings.System.getInt(mContext.getContentResolver(),
                SHOW_STATUSBAR_DATA_USAGE, 0) != 0){
            mDataUsagePreference.setChecked(true);
        } else {
            mDataUsagePreference.setChecked(false);
        }
        
        if(Settings.System.getInt(mContext.getContentResolver(),
                SHOW_STATUSBAR_BATTERY_MODE, 0) != 0){
            mBatteryModePreference.setChecked(true);
        } else {
            mBatteryModePreference.setChecked(false);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mCarrierPreference == preference) {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    SHOW_STATUSBAR_CARRIER,
                    mCarrierPreference.isChecked() ? 1 : 0);
            return true;
            
        } else if (mNetWorkPreference == preference) {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    SHOW_STATUSBAR_NETWORK_SPEED,
                    mNetWorkPreference.isChecked() ? 1 : 0);
            return true;
        } else if (mUnexpandSBPreference == preference) {
            /*
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.UNEXPAND_STATUSBAR,
                    mUnexpandSBPreference.isChecked() ? 1 : 0);
            return true;
            */
        } else if (mBatteryPreference == preference) {
            onBatteryPreferenceClick();
            return true;
        } else if (mBatteryModePreference == preference) {
            Settings.System.putInt(mContext.getContentResolver(),
                    SHOW_STATUSBAR_BATTERY_MODE,
                    mBatteryModePreference.isChecked() ? 1 : 0);
            return true;
            
        } else if (mDataUsagePreference == preference) {
            Settings.System.putInt(mContext.getContentResolver(),
                    SHOW_STATUSBAR_DATA_USAGE,
                    mDataUsagePreference.isChecked() ? 1 : 0);
            return true;
        }

        return false;
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (mCarrierPreference == preference) {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    SHOW_STATUSBAR_CARRIER,
                    mCarrierPreference.isChecked() ? 1 : 0);
            return true;
        } else if (mNetWorkPreference == preference) {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    SHOW_STATUSBAR_NETWORK_SPEED,
                    mNetWorkPreference.isChecked() ? 1 : 0);
            return true;  
        } else if (mBatteryModePreference == preference) {
            Settings.System.putInt(mContext.getContentResolver(),
                    SHOW_STATUSBAR_BATTERY_MODE,
                    mBatteryModePreference.isChecked() ? 1 : 0);
            return true;
        } else if (mUnexpandSBPreference == preference) {
            /*
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.UNEXPAND_STATUSBAR,
                    mUnexpandSBPreference.isChecked() ? 1 : 0);
            */
            return true;
        } else if (mDataUsagePreference == preference) {
            Settings.System.putInt(mContext.getContentResolver(),
                    SHOW_STATUSBAR_DATA_USAGE,
                    mDataUsagePreference.isChecked() ? 1 : 0);
            return true;
        } else if (mBatteryPreference == preference) {
            onBatteryPreferenceClick();
            return true;
        } else if(mCustomNotifications == preference){
            Intent intent = new Intent(NOTIFICATION_SET);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
            //*/add by tyd wangalei 2015929 for second menu sort
            Uri uri= Uri.parse("content://com.android.settings.customhobbyprovider/customhobby/1");
            ContentValues value=new ContentValues();
            value.put("parent_title", "112233");
            value.put("content", "112235");
            value.put("link", "com.freeme.sc.light.push.LN_PushActivity");
            value.put("comment", "com.zhuoyi.security.lite");
            mResolver.insert(uri,value);
            //*/
            return true;
        } else if(mCustomToggles == preference){
            try{
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.android.systemui",
                        "com.android.systemui.qs.order.QSTileOrderActivity"));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                //*/add by tyd wangalei 2015929 for second menu sort
                Uri uri= Uri.parse("content://com.android.settings.customhobbyprovider/customhobby/1");
                ContentValues value=new ContentValues();
                value.put("parent_title", "112233");
                value.put("content", "112234");
                value.put("link", "com.android.systemui.qs.order.QSTileOrderActivity");
                value.put("comment", "com.android.systemui");
                mResolver.insert(uri,value);
                //*/
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
            return true;
        } else if(mNetworkRefreshRate == preference) {
            mNum_sec.setScrollItemPositionByRange(mTime_val);
            mDialog.show();
        } else if(mCusCarrierPreference == preference) {
            requestInputMethod(mConfirmDialog);
            mConfirmDialog.show();
        }
        
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void onBatteryPreferenceClick() {
        int state = mBatteryPreference.isChecked() ? 1 : 0;
        Settings.System.putInt(mContext.getContentResolver(),
                SHOW_BATTERY_PERCENT, state);
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override 
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void CreateDialog() {
        final Resources res = this.getResources();
        View view = LayoutInflater.from(mContext).inflate(R.layout.droi_network_speed_interval, null);
        mNum_sec = (ScrollNumberPicker) view.findViewById(R.id.number_sec);
        mNum_sec.setRange(1, 10, 3);
        mNum_sec.setScrollItemPositionByRange(mTime_val);
        
        mDialog = new AlertDialog.Builder(mContext)
            .setTitle(R.string.quicksettings_refresh_interval)
            .setView(view)
            .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        mTime_val = Integer.parseInt(mNum_sec.getSelectItemText());
                        mNetworkRefreshRate.setSummary(
                            res.getString(R.string.quicksettings_refresh_interval_summary, mTime_val));
                        editor.putInt("RefreshTime", mTime_val);
                        editor.commit();
                    }
                })
            .create();
    }

    private void CreateConfirmDialog() {
        final Resources res = this.getResources();
        View view = LayoutInflater.from(mContext).inflate(R.layout.droi_status_bar_carrier_label, null);
        mCarrierEditText = (EditText) view.findViewById(R.id.custom_carrier_label);
        mCarrierEditText.setText(mSp.getString("CustomCarrierLabel", ""));
        if(res.getConfiguration().locale.getCountry().equals("CN")
            || res.getConfiguration().locale.getCountry().equals("TW")) {
            mCarrierEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
        } else {
            mCarrierEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        }  
        
        mConfirmDialog = new AlertDialog.Builder(mContext)
            .setTitle(R.string.custom_carrier_title)
            .setView(view)
            .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        String text = mCarrierEditText.getText().toString();
                        if(!"".equals(text)) {
                            mCusCarrierPreference.setSummary(text);
                        } else {
                            mCusCarrierPreference.setSummary(R.string.custom_carrier_summary);
                        }
                        editor.putString("CustomCarrierLabel", text);
                        editor.commit();
                    }
                })
            .setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        mCarrierEditText.setText(mSp.getString("CustomCarrierLabel", ""));
                    }
                })
            .create();
    }

    private void requestInputMethod(Dialog dialog) {
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
}
