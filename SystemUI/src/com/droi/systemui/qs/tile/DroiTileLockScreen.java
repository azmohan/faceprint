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

import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IPowerManager;
import android.os.IPowerManager.Stub;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

import android.util.Log;

/** Quick settings tile: LockScreen **/
public class DroiTileLockScreen extends QSTile<QSTile.State> {
    private static final String TAG = "LockScreenTile";

    public DroiTileLockScreen(Host host) {
        super(host);
    }

    @Override
    protected State newTileState() {
        return new State();
    }

    @Override
    public void setListening(boolean listening) {}

    @Override
    protected void handleClick() {
        IPowerManager mIPowerManager = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE));
        try {
            mIPowerManager.goToSleep(SystemClock.uptimeMillis(), PowerManager.GO_TO_SLEEP_REASON_APPLICATION, 0);
        } catch (RemoteException localRemoteException) {
            Log.e(TAG, localRemoteException.toString());
        }
    }

    @Override
    protected void handleUpdateState(State state, Object arg) {
        state.visible = true; 
        state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_lockscreen);
        state.label = mContext.getString(R.string.droi_qs_lockscreen_label);
        state.contentDescription = mContext.getString(R.string.droi_qs_lockscreen_label);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_PANEL;
    }

}
