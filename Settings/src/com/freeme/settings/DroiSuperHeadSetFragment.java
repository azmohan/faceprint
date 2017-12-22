package com.freeme.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;
import com.freeme.settings.motion.SmartWakeStartupAppList;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.util.Log;
import java.lang.String;

/**
 * Created by luyangjie on 6/14/17.
 */
public class DroiSuperHeadSetFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String[] KEY_SETTING_MAP = {
            "click_function", "double_click_function", "Three_click_function", "long_click_function"
    };
    private SwitchPreference mHeadsetControl;
    private FreemeSwitchPreference mClickFunction;
    private FreemeSwitchPreference mDoubleClickFunction;
    private FreemeSwitchPreference mThreeClickFunction;
    private FreemeSwitchPreference mLongClickFunction;
    private static final FreemeSwitchPreference[] KEY_PREFERENCE_MAP = new FreemeSwitchPreference[KEY_SETTING_MAP.length];

    public static final String[] KEY_ACTION_SETTING_MAP = {
            Settings.System.FREEME_CLICK_FUNCTION_SETTINGS, Settings.System.FREEME_DOUBLE_CLICK_FUNCTION_SETTINGS,
            Settings.System.FREEME_THREE_CLICK_FUNCTION_SETTINGS, Settings.System.FREEME_LONG_CLICK_FUNCTION_SETTINGS,
    };
    private static final String KEY_SUPER_HEADSET_CONTROL = "super_headset_control";
    private static final String KEY_CLICK_FUNCTION_CONTROL = "click_function";
    private static final String KEY_DOUBLE_CLICK_FUNCTION_CONTROL = "double_click_function";
    private static final String KEY_THREE_CLICK_FUNCTION_CONTROL = "Three_click_function";
    private static final String KEY_LONG_CLICK_FUNCTION_CONTROL = "long_click_function";
    private Context mContext = null;
    private ContentResolver mContentResolver = null;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.freeme_super_headset);
        mContext = getActivity();
        mContentResolver = mContext.getContentResolver();
        mHeadsetControl = (SwitchPreference) findPreference(KEY_SUPER_HEADSET_CONTROL);
        mClickFunction = (FreemeSwitchPreference) findPreference(KEY_CLICK_FUNCTION_CONTROL);
        mDoubleClickFunction = (FreemeSwitchPreference) findPreference(KEY_DOUBLE_CLICK_FUNCTION_CONTROL);
        mThreeClickFunction = (FreemeSwitchPreference) findPreference(KEY_THREE_CLICK_FUNCTION_CONTROL);
        mLongClickFunction = (FreemeSwitchPreference) findPreference(KEY_LONG_CLICK_FUNCTION_CONTROL);
        mHeadsetControl.setOnPreferenceChangeListener(this);
        mClickFunction.setOnPreferenceChangeListener(this);
        mDoubleClickFunction.setOnPreferenceChangeListener(this);
        mThreeClickFunction.setOnPreferenceChangeListener(this);
        mLongClickFunction.setOnPreferenceChangeListener(this);
        for (int i = 0; i < KEY_SETTING_MAP.length; i++) {
            KEY_PREFERENCE_MAP[i] = (FreemeSwitchPreference) findPreference(KEY_SETTING_MAP[i]);
            //KEY_PREFERENCE_MAP[i].setOnPreferenceChangeListener(this);
            KEY_PREFERENCE_MAP[i].setOnPreferenceClickListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference == mHeadsetControl) {
                Boolean booleanValue = (Boolean) newValue;
                Settings.System.putInt(mContentResolver, Settings.System.FREEME_SUPER_HEADSET_CONTROL, booleanValue ? 1 : 0);
                return true;
            } else if (preference == mClickFunction) {
                Boolean booleanValue = (Boolean) newValue;
                Settings.System.putInt(mContentResolver, Settings.System.FREEME_CLICK_FUNCTION, booleanValue ? 1 : 0);
                return true;
            } else if (preference == mDoubleClickFunction) {
                Boolean booleanValue = (Boolean) newValue;
                Settings.System.putInt(mContentResolver, Settings.System.FREEME_DOUBLE_CLICK_FUNCTION, booleanValue ? 1 : 0);
                return true;
            } else if (preference == mThreeClickFunction) {
                Boolean booleanValue = (Boolean) newValue;
                Settings.System.putInt(mContentResolver, Settings.System.FREEME_THREE_CLICK_FUNCTION, booleanValue ? 1 : 0);
                return true;
            } else if (preference == mLongClickFunction) {
                Boolean booleanValue = (Boolean) newValue;
                Settings.System.putInt(mContentResolver, Settings.System.FREEME_LONG_CLICK_FUNCTION, booleanValue ? 1 : 0);
                return true;
            }
        return true;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.GESTURE_OPERATE;
    }

    @Override
    public void onResume() {
        super.onResume();
        for (int i = 0; i < KEY_PREFERENCE_MAP.length; i++) {
            String summary = "";
            String actionAll = android.provider.Settings.System.getString(mContentResolver, KEY_ACTION_SETTING_MAP[i]);
            if (actionAll != null) {
                String[] actionItem = actionAll.split(";");
                if (actionItem[0].equals("startupapp")) {
                    summary = getResources().getString(R.string.open_app_mode_title)
                            + "  " + filterDefaultName(actionItem[1]);
                }
            }
            KEY_PREFERENCE_MAP[i].setSummary(summary);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            KEY_PREFERENCE_MAP[requestCode].setSummary(data.getExtras().getString("ControlData"));
            android.provider.Settings.System.putString(mContentResolver, KEY_ACTION_SETTING_MAP[requestCode],
                    data.getExtras().getString("ActionData"));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Intent intent = new Intent(mContext, SmartWakeStartupAppList.class);
        for (int i = 0; i < KEY_PREFERENCE_MAP.length; i++) {
            if (preference == KEY_PREFERENCE_MAP[i]) {
                intent.putExtra("type", i);
                startActivityForResult(intent, i);
            }
        }
        return false;
    }

    private String filterDefaultName(String name) {
        return name;
    }

}
