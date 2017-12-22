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

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

/** Quick settings tile: SuperShot **/
public class DroiTileSuperShot extends QSTile<QSTile.State> {
    private static final String TAG = "SupershotTile";

    public DroiTileSuperShot(Host host) {
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
        mHost.collapsePanels();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startSuperShot();
            }
        }, 500);
    }

    private void startSuperShot() {
        Intent service = new Intent("com.freeme.supershot.MainMenu");
        service.setPackage("com.freeme.supershot");
        mContext.startService(service);
    }

    @Override
    protected void handleUpdateState(State state, Object arg) {
        state.visible = true; 
        state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_supershot);
        state.label = mContext.getString(R.string.droi_qs_supershot_label);
        state.contentDescription = mContext.getString(R.string.droi_qs_supershot_label);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_PANEL;
    }

}
