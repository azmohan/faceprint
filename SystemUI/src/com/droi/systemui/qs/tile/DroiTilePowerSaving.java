/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.droi.systemui.qs.tile;

import java.text.NumberFormat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

/** Quick settings tile: PowerSaving **/
public class DroiTilePowerSaving extends QSTile<QSTile.State> {
    private static final String TAG = "PowerSavingTile";

    private static final String ACTION_REQUEST_CHANGE = "com.zhuoyi.security.PowerSaving_SWITCH";
    private static final String ACTION_STATE_CHANGED = "com.zhuoyi.security.PowerSaving_STATE";
    private static final String ACTION_SHOW_POWERMANAGER = "com.android.settings.ACTION_POWER_MANAGER";
    private static final String ACTION_POWER_USAGE = "com.zhuoyi.security.POWER_USAGE_SUMMARY_CHOOSE";

    private static final String KEY_POWERSAVING__STATE = "powerSavingState";
    
    private boolean mIsCharing = false;
    private boolean mIsPowerSaving = false;
    private int mStatus = -1;
    private int mBatteryLevel = 0;
    
    private SharedPreferences getPrefs() {
        return mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE);
    }
    
    private boolean getStatus() {
        return getPrefs().getBoolean(KEY_POWERSAVING__STATE, false);
    }
    
    private void setStatus(boolean newStatus){
        getPrefs().edit().putBoolean(KEY_POWERSAVING__STATE, newStatus).apply();
    }

    public DroiTilePowerSaving(Host host) {
        super(host);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ACTION_STATE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
        
        // Initialize orginal status
        mIsPowerSaving = getStatus();
        refreshState();
    }

    @Override
    protected State newTileState() {
        return new State();
    }

    @Override
    public void setListening(boolean listening) {}

    @Override
    protected void handleClick() {
        if(DEBUG) {
            Log.d(TAG, "handleClick...");
        }
        // refelect to ZhuiyiSafeCenter
        boolean newState = !mIsPowerSaving;
        Intent intent = new Intent(ACTION_REQUEST_CHANGE);
        intent.putExtra("state", newState);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        if(DEBUG) {
            Log.d(TAG, "request powerSaving state =>" + newState);
        }
        mIsPowerSaving = newState;
        refreshState();
    }

    @Override
    protected void handleLongClick() {
        if(DEBUG) {
            Log.d(TAG, "handleLongClick...");
        }

        try{
            mHost.startActivityDismissingKeyguard(new Intent());
        } catch (Exception e) {}
        
        Intent intent = new Intent(ACTION_POWER_USAGE);
        mContext.sendBroadcast(intent);

        //mHost.collapsePanels();
    }

    @Override
    protected void handleUpdateState(State state, Object arg) {
        final String percentage = NumberFormat.getPercentInstance().format((double) mBatteryLevel / 100.0);
        state.visible = true;
        state.selected = mIsPowerSaving;

        if(mIsPowerSaving) {
            if(true == mIsCharing) {
                state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_powersaving_charing);
                state.label = mContext.getString(R.string.qs_tile_battery_percent_format, percentage);
            } else {
                state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_powersaving_on);
                state.label = mContext.getString(R.string.droi_qs_powersaving_label);
            }
        } else {
            state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_powersaving_off);
            state.label = mContext.getString(R.string.droi_qs_powersaving_label);
        }

        state.contentDescription = mContext.getString(R.string.droi_qs_powersaving_label);
    }
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(DEBUG) {
                Log.d(TAG, "receive: action-" + action);
            }
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                boolean levelChanged = false;
                int newBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int newStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
                if(DEBUG) {
                    Log.d(TAG, "BatteryLevel[ orginal = " + mBatteryLevel + ",new = " + newBatteryLevel + "]");
                    Log.d(TAG, "Status[ orginal = " + mStatus + ",new = " + newStatus + "]");
                }

                levelChanged = mBatteryLevel != newBatteryLevel;
                mBatteryLevel = newBatteryLevel;
                
                mIsCharing = newStatus == BatteryManager.BATTERY_STATUS_CHARGING || newStatus == BatteryManager.BATTERY_STATUS_FULL;

                if(mIsPowerSaving && (levelChanged || mStatus != newStatus)) {
                    refreshState();
                }
                
                mStatus = newStatus;
            } else if (ACTION_STATE_CHANGED.equals(action)){
                boolean newState = intent.getBooleanExtra("state", false);
                if(DEBUG) {
                    Log.d(TAG, "get powerSaving state =>" + newState + "," + " orginal State " + mIsPowerSaving);
                }
                
                setStatus(newState);
                if(newState == mIsPowerSaving) {
                    return;
                } else {
                    mIsPowerSaving = newState;
                }
                refreshState();
            }

        }
    };

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_PANEL;
    }
}
