package com.android.settings.accessibility;

import android.preference.SwitchPreference;
import com.android.settings.R;

import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;

//*/Modify by Jiangshouting 2015.12.31 for setting code transplant
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.HobbyDB.CustomHobbyService;
import com.android.internal.logging.MetricsLogger;
//*/

public class FloatTaskPreference extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener{
	
	private static final String TAG = "FloatTaskPreference";
	
    private static final String FLOATTASK_PREFERENCE = "toggle_floattask_preference";
    private static final String QUICK_FLOATTASK_PREFERENCE = "toggle_quick_floattask_preference";
    private SwitchPreference mFloatTaskPreference;
    private SwitchPreference mQuickFloatTaskPreference;
    
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
        
		addPreferencesFromResource(R.xml.floattask_preference);
		initializeAllPreferences();
		
	    //*/add by tyd_wangalei 2015.9.21 for Customs hobby sort
        CustomHobbyService mService=new CustomHobbyService(getActivity());
        if(mService.isExistData(R.string.accessibility_settings_ext_title, R.string.floattask_settings)){
			mService.update(R.string.accessibility_settings_ext_title, R.string.floattask_settings);
		}else{
			mService.insert(R.string.accessibility_settings_ext_title, R.string.floattask_settings, this.getClass().getName(), 1, "");
		}
        //*/
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.FREEME_SHOW_FLOATTASK),
                true, mFloatTaskChangeObserver);
        
        if(Settings.System.getInt(this.getContentResolver(), Settings.System.FREEME_SHOW_FLOATTASK, 0) != 0){
            mFloatTaskPreference.setChecked(true);
        } else {
            mFloatTaskPreference.setChecked(false);
        }
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
        getContentResolver().unregisterContentObserver(mFloatTaskChangeObserver);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		
		return false;
	}
	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (mFloatTaskPreference == preference) {
            ToggleFloatTaskPreferenceClick();
            return true;
        } else if (mQuickFloatTaskPreference == preference) {
            ToggleQuickFloatTaskPreferenceClick();
            return true;
        }
		
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
	
	private void initializeAllPreferences(){
        mFloatTaskPreference = (SwitchPreference) findPreference(FLOATTASK_PREFERENCE);
        if(Settings.System.getInt(this.getContentResolver(),
                Settings.System.FREEME_SHOW_FLOATTASK, 0) != 0){
            mFloatTaskPreference.setChecked(true);
        }

        mQuickFloatTaskPreference = (SwitchPreference) findPreference(QUICK_FLOATTASK_PREFERENCE);
        if(Settings.System.getInt(this.getContentResolver(),
                Settings.System.FREEME_QUICK_SHOW_FLOATTASK, 0) != 0) {
            mQuickFloatTaskPreference.setChecked(true);
        }
    }
    
    private void ToggleFloatTaskPreferenceClick(){
    	boolean FloatTaskEnable = mFloatTaskPreference.isChecked();

    	Intent service = new Intent("com.freeme.floattask.IFloatTaskService");
    	service.setPackage("com.freeme.floattask");
    	if(FloatTaskEnable == true){
    	    service.putExtra("showMenuDefault", true);
    		getActivity().startService(service);
    	}else{
    		getActivity().stopService(service);
    	}
        
    	Settings.System.putInt(getContentResolver(), Settings.System.FREEME_SHOW_FLOATTASK,
                mFloatTaskPreference.isChecked() ? 1 : 0);
    }

    private void ToggleQuickFloatTaskPreferenceClick(){
    	Settings.System.putInt(getContentResolver(),
                Settings.System.FREEME_QUICK_SHOW_FLOATTASK,
                mQuickFloatTaskPreference.isChecked() ? 1 : 0);
    }

    private ContentObserver mFloatTaskChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            boolean FloatTaskEnable = Settings.System.getInt(getContentResolver(),
					Settings.System.FREEME_SHOW_FLOATTASK, 0) != 0;
            mFloatTaskPreference.setChecked(FloatTaskEnable);
        }
    };

    //*/Added by Jiangshouting 2016.01.04 for setting code transplant
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.FLOAT_TASK;
    }
    //*/
}

