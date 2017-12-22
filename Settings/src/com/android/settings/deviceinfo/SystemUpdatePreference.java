package com.android.settings.deviceinfo;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.android.settings.R;

public class SystemUpdatePreference extends Preference {

    public static class SystemUpdateNotifyReceiver extends BroadcastReceiver {
        private static final String LOG_TAG = "SystemUpdateNotifyReceiver";

        private static final String RECOMMEND_PACKAGE = "com.freeme.ota";

	    @Override
        public void onReceive(Context context, Intent intent) {
            ComponentName cn = (ComponentName) intent.getExtra(Intent.EXTRA_UNREAD_COMPONENT);
            if (cn != null && cn.getPackageName() != null
                    && cn.getPackageName().startsWith(RECOMMEND_PACKAGE)) {
                boolean systemUpdateShowGuide = (int) intent.getExtra(Intent.EXTRA_UNREAD_NUMBER) != 0;

                SharedPreferences spf = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
                spf.edit().putBoolean("systemUpdateShowNotify", systemUpdateShowGuide).commit();

                if (systemUpdateShowGuide || ( !spf.getBoolean("showGuide", true) && !spf.getBoolean("notificationShowGuide", true))) {
                    cn = new ComponentName(context.getPackageName(), "com.android.settings.Settings");
                    com.android.settings.dashboard.DashboardTileView
                            .sendRecommendIconShowChanged(context, cn, systemUpdateShowGuide);
                }

                Log.i(LOG_TAG, "SystemUpdateNotify: " + systemUpdateShowGuide + "("+ context.getPackageName() + ")");
            }
        }
    }

    // -------------------------------------------------------------------------

	public SystemUpdatePreference(Context context) {
        this(context, null);
	}

	public SystemUpdatePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayoutResource(R.layout.system_update_preference);
	}

	public SystemUpdatePreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setLayoutResource(R.layout.system_update_preference);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
        Context context = getContext();
        SharedPreferences spf = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);

        View notifier = (LinearLayout)view.findViewById(R.id.system_update_recommend);
		notifier.setVisibility(spf.getBoolean("systemUpdateShowNotify", false) ? View.VISIBLE : View.GONE);
	}
}
