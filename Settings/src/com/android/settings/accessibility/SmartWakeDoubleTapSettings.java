package com.android.settings.accessibility;

import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.HobbyDB.CustomHobbyService;
import com.droi.feature.FeatureOption;

public class SmartWakeDoubleTapSettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String KEY_KEEP_SCREEN_ENABLED = "keep_screen_enabled";
    private static final String KEY_DOUBLE_TAP_SCREEN_WAKE = "double_tap_screen_wake";
    private static final String KEY_DOUBLE_TAP_HOME_WAKE = "double_tap_home_wake";
    private static final String DOUBLE_TAP_HOME_POWER_OFF_KEY = "double_tap_home_off";

    private SwitchPreference mFreemeKeepScreenEnabled;
    private SwitchPreference mDoubleTapScreenWake;
    private SwitchPreference mDoubleTapHomeWake;
    private SwitchPreference mDoubleTapHomePowerOff;

    private ContentResolver mContentResolver;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContentResolver = getContentResolver();

        addPreferencesFromResource(R.xml.smart_wakeup_preference);
        initializeAllPreferences();

        //*/add by tyd_wangalei 2015.9.21 for Customs hobby sort
        CustomHobbyService mService = new CustomHobbyService(getActivity());
        if (mService.isExistData(R.string.accessibility_settings_ext_title, R.string.smart_wakeup_settings)) {
            mService.update(R.string.accessibility_settings_ext_title, R.string.smart_wakeup_settings);
        } else {
            mService.insert(R.string.accessibility_settings_ext_title, R.string.smart_wakeup_settings, this.getClass().getName(), 1, "");
        }
        //*/
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Boolean booleanValue = (Boolean) newValue;
        if (preference == mFreemeKeepScreenEnabled) {
            Settings.System.putInt(mContentResolver, Settings.System.FREEME_KEEP_SCREEN_ENABLED, booleanValue ? 1 : 0);
            return true;
        }
        if (preference == mDoubleTapScreenWake) {
            Settings.System.putInt(mContentResolver, Settings.System.FREEME_SCREEN_DOUBLETAP_WAKEUP_ENABLED, booleanValue ? 1 : 0);
            return true;
        }
        if (preference == mDoubleTapHomeWake) {
            Settings.System.putInt(mContentResolver, Settings.System.FREEME_HOME_DOUBLETAP_WAKEUP_ENABLED, booleanValue ? 1 : 0);
            return true;
        }
        if (preference == mDoubleTapHomePowerOff) {
            Settings.System.putInt(mContentResolver, Settings.System.FREEME_HOME_DOUBLETAP_POWEROFF_ENABLED, booleanValue ? 1 : 0);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void initializeAllPreferences() {
        mFreemeKeepScreenEnabled = (SwitchPreference) findPreference(KEY_KEEP_SCREEN_ENABLED);
        if (mFreemeKeepScreenEnabled != null) {
            if (false) { // TODO: add a feature controller.
                mFreemeKeepScreenEnabled.setChecked(Settings.System.getInt(mContentResolver,
                        Settings.System.FREEME_KEEP_SCREEN_ENABLED, 0) == 1);
                mFreemeKeepScreenEnabled.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(mFreemeKeepScreenEnabled);
            }
        }

        mDoubleTapScreenWake = (SwitchPreference) findPreference(KEY_DOUBLE_TAP_SCREEN_WAKE);
        if (mDoubleTapScreenWake != null) {
            if (FeatureOption.FREEME_SCREEN_DOUBLETAP_WAKEUP_SUPPORT) {
                mDoubleTapScreenWake.setChecked(Settings.System.getInt(mContentResolver,
                        Settings.System.FREEME_SCREEN_DOUBLETAP_WAKEUP_ENABLED, 0) == 1);
                mDoubleTapScreenWake.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(mDoubleTapScreenWake);
            }
        }

        mDoubleTapHomeWake = (SwitchPreference) findPreference(KEY_DOUBLE_TAP_HOME_WAKE);
        if (mDoubleTapHomeWake != null) {
            if (FeatureOption.FREEME_HOME_DOUBLETAP_WAKEUP_SUPPORT) {
                mDoubleTapHomeWake.setChecked(Settings.System.getInt(mContentResolver,
                        Settings.System.FREEME_HOME_DOUBLETAP_WAKEUP_ENABLED, 0) == 1);
                mDoubleTapHomeWake.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(mDoubleTapHomeWake);
            }
        }

        mDoubleTapHomePowerOff = (SwitchPreference) findPreference(DOUBLE_TAP_HOME_POWER_OFF_KEY);
        if (mDoubleTapHomePowerOff != null) {
            if (FeatureOption.FREEME_HOME_DOUBLETAP_WAKEUP_SUPPORT) {
                mDoubleTapHomePowerOff.setChecked(Settings.System.getInt(mContentResolver,
                        Settings.System.FREEME_HOME_DOUBLETAP_POWEROFF_ENABLED, 0) == 1);
                mDoubleTapHomePowerOff.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(mDoubleTapHomePowerOff);
            }
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.SMARTWAKEUPSETTING;
    }
}
