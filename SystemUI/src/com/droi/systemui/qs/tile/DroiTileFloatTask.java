package com.droi.systemui.qs.tile;

import android.content.Intent;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.droi.systemui.qs.SystemSetting;

/** Quick settings tile: Floattask **/
public class DroiTileFloatTask extends QSTile<QSTile.State> {
    private static final String TAG = "FloattaskTile";

    private final SystemSetting mSetting;

    private int currentState;

    private boolean mListening;


    public DroiTileFloatTask(Host host) {
        super(host);

        mSetting = new SystemSetting(mContext, mHandler, Settings.System.FREEME_SHOW_FLOATTASK, 0) {
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
    protected State newTileState() {
        return new State();
    }

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening) {
            return;
        } else {
            mListening = listening;
        }

        mSetting.setListening(listening);
    }

    @Override
    protected void handleClick() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent("com.freeme.floattask.IFloatTaskService");
                intent.setPackage("com.freeme.floattask");
                if(currentState == 1) { // close floattask
                    mSetting.setValue(0);
                    mContext.stopServiceAsUser(intent,UserHandle.CURRENT);
                    Log.d(TAG, "close float task...");
                } else { // open floarrask
                    mSetting.setValue(1);
                    mContext.startServiceAsUser(intent, UserHandle.CURRENT);
                    Log.d(TAG, "open float task...");
                }
            }
        });
    }

    @Override
    protected void handleUpdateState(State state, Object arg) {
        final int checkedOn = arg instanceof Integer ? (Integer)arg: mSetting.getValue();

        state.selected = checkedOn == 1;
        state.visible = true; 
        
        if(1 == checkedOn) {
            state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_floattask_on);
        } else {
            state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_floattask);
        }
        state.label = mContext.getString(R.string.droi_qs_floattask_label);
        state.contentDescription = mContext.getString(R.string.droi_qs_floattask_label);
        
        currentState = checkedOn;

        if(DEBUG) {
            Log.d(TAG, "current Float Task State is " + (currentState == 1 ? "enable" : "disable"));
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_PANEL;
    }

}
