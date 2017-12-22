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

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import android.content.Intent;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.droi.systemui.qs.SystemSetting;

/** Quick settings tile: Timeout **/
public class DroiTileSleep extends QSTile<QSTile.LevelState> {
    private static final String TAG = "TimeoutTile";

    /** If there is no setting in the provider, use this. */
    private static final int MINIMUM_TIMEOUT = 15000;
    private static final int MEDIUM_TIMEOUT = 30000;
    private static final int MAXIMUM_TIMEOUT = 60000;

    private final SystemSetting mSetting;

    private int currentLevel;

    private boolean mListening;

    public DroiTileSleep(Host host) {
        super(host);

        mSetting = new SystemSetting(mContext, mHandler, SCREEN_OFF_TIMEOUT, MEDIUM_TIMEOUT) {
            @Override
            protected void handleValueChanged(int value) {
                if (DEBUG) {
                     Log.d(TAG, "handleValueChanged: " + value);
                }
                refreshState(value);
            }
        };
    }

    @Override
    protected LevelState newTileState() {
        return new LevelState();
    }

    @Override
    public void handleClick() {
        toggleValueChanged(currentLevel);
    }

    @Override
    protected void handleLongClick() {
        Intent intent = new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //intent.setPackage("com.android.settings");
        mHost.startActivityDismissingKeyguard(intent);
    }

    @Override
    protected void handleUpdateState(LevelState state, Object arg) {
        final int level = arg instanceof Integer ? ((Integer)arg == -1 ? getTimeoutLevel() : (Integer)arg): getTimeoutLevel();
        state.visible = true;
        state.selected = true;

        if (DEBUG) {
            Log.d(TAG, "handleUpdateState: " + "timeoutLevel = " + arg + ", fix level = " + level);
        }

        state.level = currentLevel = level;
        state.label = mContext.getString(R.string.droi_qs_sleep_label);

        switch(currentLevel) {
            case MINIMUM_TIMEOUT:
                state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_timeout_on_minimum);
                break;
            case MEDIUM_TIMEOUT:
                state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_timeout_on_medium);
                break;
            case MAXIMUM_TIMEOUT:
                state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_timeout_on_maximum);
                break;

            default:
                state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_timeout);
                break;
        }

        state.contentDescription = mContext.getString(R.string.droi_qs_sleep_label);
    }

    public void toggleValueChanged(int timeoutLevel) {
        int timeout = -1;
        if (timeoutLevel <= MINIMUM_TIMEOUT) {
            timeout = MEDIUM_TIMEOUT;
        } else if (timeoutLevel <= MEDIUM_TIMEOUT) {
            timeout = MAXIMUM_TIMEOUT;
        } else {
            timeout = MINIMUM_TIMEOUT;
        }

        mSetting.setValue(timeout);
    }

    public void setListening(boolean listening) {
        if (mListening == listening) {
            return;
        } else {
            mListening = listening;
        }

        mSetting.setListening(listening);
    }

    private int getTimeoutLevel() {
        try {
            int timeout = mSetting.getValue();
            if (timeout <= MINIMUM_TIMEOUT) {
                timeout = MINIMUM_TIMEOUT;
            } else if (timeout <= MEDIUM_TIMEOUT) {
                timeout = MEDIUM_TIMEOUT;
            } else if(timeout <= MAXIMUM_TIMEOUT) {
                timeout = MAXIMUM_TIMEOUT;
            }
            return timeout;
        } catch (Exception e) {
            Log.d(TAG, "getTimeout: " + e);
        }
        return MEDIUM_TIMEOUT;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_PANEL;
    }

}
