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

import android.content.ComponentName;
import android.content.Intent;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

import android.util.Log;

/** Quick settings tile: More **/
public class DroiTileMore extends QSTile<QSTile.State> {

    public DroiTileMore(Host host) {
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
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.systemui", "com.droi.systemui.qs.order.QSTileOrderActivity"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mHost.startActivityDismissingKeyguard(intent);
    }
    
    @Override
    protected void handleUpdateState(State state, Object arg) {
        state.visible = true;
        state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_more);
        state.label = mContext.getString(R.string.droi_qs_more_label);
        state.contentDescription = mContext.getString(R.string.droi_qs_more_label);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_PANEL;
    }
}
