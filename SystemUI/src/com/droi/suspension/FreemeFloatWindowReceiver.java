package com.droi.suspension;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FreemeFloatWindowReceiver extends BroadcastReceiver {
    private static final String TAG = "FreemeSuspension";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = (intent != null) ? intent.getAction() : "";
        Log.v(TAG, "Receive broadcast: " + action);

        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_USER_PRESENT:
            case FreemeFloatManager.ACTION_SUSPENSION_TOGGLE:
                if (FreemeFloatManager.isEnabled(context) && !FreemeFloatManager.isRunning()) {
                    FreemeFloatManager.start(context);
                } else {
                    FreemeFloatManager.stop(context);
                }
                break;

            default:
                break;
        }
    }
}
