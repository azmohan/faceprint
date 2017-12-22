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

import android.app.ActivityManagerNative;
import android.content.Intent;
import android.os.Handler;
import android.os.UserHandle;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

/** Quick settings tile: Reboot **/
public class DroiTileReboot extends QSTile<QSTile.State> {
    private static final String TAG = "RebootTile";

    public DroiTileReboot(Host host) {
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
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (ActivityManagerNative.isSystemReady()) {
                    Intent intent = new Intent(Intent.ACTION_REBOOT);
                    intent.putExtra(Intent.EXTRA_KEY_CONFIRM, false);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                }
            }
        });
    }

    @Override
    protected void handleUpdateState(State state, Object arg) {
        state.visible = true; 
        state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_reboot);
        state.label = mContext.getString(R.string.droi_qs_reboot_label);
        state.contentDescription = mContext.getString(R.string.droi_qs_reboot_label);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_PANEL;
    }

}
