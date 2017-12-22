package com.android.settings.accessibility;

import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.HobbyDB.CustomHobbyService;

//*/Add by Jiangshouting 2016.01.04 for setting code transplant
import com.android.internal.logging.MetricsLogger;
//*/

public class NavigationBarPreference extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String TAG = "NavigationBarPreference";

    private static final String NAVIGATIONBAR_PREFERENCE = "toggle_navigationbar_preference";
    private SwitchPreference mNavigationBarPreference;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.navigationbar_preference);
        initializeAllPreferences();

        //*/add by tyd_wangalei 2015.9.21 for Customs hobby sort
        CustomHobbyService mService = new CustomHobbyService(getActivity());
        if (mService.isExistData(R.string.accessibility_settings_ext_title, R.string.navigationbar_settings)) {
            mService.update(R.string.accessibility_settings_ext_title, R.string.navigationbar_settings);
        } else {
            mService.insert(R.string.accessibility_settings_ext_title, R.string.navigationbar_settings, this.getClass().getName(), 1, "");
        }
        //*/
    }

    @Override
    public void onResume() {
        super.onResume();

        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.FREEME_NAVIGATIONBAR_CAN_HIDE),
                true, mNavigationBarChangeObserver);

        boolean check = Settings.System.getInt(getContentResolver(),
                Settings.System.FREEME_NAVIGATIONBAR_CAN_HIDE, 0) != 0;
        mNavigationBarPreference.setChecked(check);
    }

    @Override
    public void onPause() {
        super.onPause();

        getContentResolver().unregisterContentObserver(mNavigationBarChangeObserver);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        if (mNavigationBarPreference == preference) {
            ToggleNavigationBarPreferenceClick();
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void initializeAllPreferences() {
        mNavigationBarPreference = (SwitchPreference) findPreference(NAVIGATIONBAR_PREFERENCE);
        if (Settings.System.getInt(getContentResolver(),
                Settings.System.FREEME_NAVIGATIONBAR_CAN_HIDE, 0) != 0) {
            mNavigationBarPreference.setChecked(true);
        }
    }

    private void ToggleNavigationBarPreferenceClick() {
        Settings.System.putInt(getContentResolver(), Settings.System.FREEME_NAVIGATIONBAR_CAN_HIDE,
                mNavigationBarPreference.isChecked() ? 1 : 0);
    }

    private ContentObserver mNavigationBarChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            boolean checked = Settings.System.getInt(getContentResolver(),
                    Settings.System.FREEME_NAVIGATIONBAR_CAN_HIDE, 0) != 0;
            mNavigationBarPreference.setChecked(checked);
        }
    };

    //*/Added by Jiangshouting 2016.01.04 for setting code transplant
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NAVIGATIONBARPREFERENCE;
    }
    //*/
}

