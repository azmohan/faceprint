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

import android.content.Intent;
import android.os.Handler;
import android.util.Log;
//*/ freeme, Cheyunyi. 20160720. for screenrecorder state.
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
//*/

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

/** Quick settings tile: Screenrecorder **/
public class DroiTileScreenRecorder extends QSTile<QSTile.State> {
    private static final String TAG = "ScreenrecorderTile";

    //*/ freeme, Cheyunyi, 20160720. for screenrecorder state.
    private static final String ACTION_SCREEN_RECORDER_STATE = "com.freeme.systemui.action.SCREEN_RECORDER_STATE";
    private static boolean mSwitch = false;
    //*/

    public DroiTileScreenRecorder(Host host) {
        super(host);
        haveGuidance = mHost.haveGuidance("screenrecorder");
        if(DEBUG) {
            Log.d(TAG, "haveGuidance = " + haveGuidance);
        }

        //*/ freeme.Cheyunyi, 20160720.for receiver screenrecorder state.
        IntentFilter filter =
                new IntentFilter(ACTION_SCREEN_RECORDER_STATE);
        mContext.registerReceiver(new ScreenRecorderReceiver(), filter);
        //*/

    }

    @Override
    protected State newTileState() {
        return new State();
    }

    @Override
    public void setListening(boolean listening) {}

    @Override
    protected void handleClick() {
        if(!haveGuidance) {
            mHost.setGuidance("screenrecorder", true);
            haveGuidance = true;
            if(DEBUG) {
                Log.d(TAG, "ScreenrecorderTile's new feature guider finished ... (Only Once)");
            }
        }

        mHost.collapsePanels();
        //*/ freeme.Linguanrong, 20160614. Delay to start screenrecorder.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startScreenRecorder();
            }
        }, 250);
        //*/
    }

    //*/ freeme.Linguanrong, 20160614. Delay to start screenrecorder.
    private void startScreenRecorder() {
        Intent intent = new Intent("android.intent.action.ScreenRecorder");
        intent.setPackage("com.freeme.screenrecorder");
        mContext.startService(intent);
    }
    //*/

    @Override
    protected void handleUpdateState(State state, Object arg) {
        state.visible = true; 
        if(haveGuidance) {
            state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_screenrecorder);
        } else {
            state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_screenrecorder_no_guided);
        }
        state.label = mContext.getString(R.string.droi_qs_screenrecording_label);
        state.contentDescription = mContext.getString(R.string.droi_qs_screenrecording_label);

        //*/ freeme.Cheyunyi, 20160720.for display screenrecorder state.
        if(mSwitch) {
            state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_screenrecorder_pressed);
        }else{
            state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_screenrecorder_normal);
        }
        state.selected = mSwitch;
        //*/
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_PANEL;
    }

    //*/ freeme.Cheyunyi, 20160720.for receiver screenrecorder state.
    public class ScreenRecorderReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_SCREEN_RECORDER_STATE.equals(intent.getAction())) {
                mSwitch = intent.getBooleanExtra("isStart", false);
                refreshState();
            }
        }
    }
    //*/

}
