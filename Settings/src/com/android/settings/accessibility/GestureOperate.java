package com.android.settings.accessibility;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.mediatek.HobbyDB.CustomHobbyService;
import com.mediatek.settings.FeatureOption;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

//*/Modify by Jiangshouting 2016.01.04 for setting code transplant
import com.android.internal.logging.MetricsLogger;
//*/
//*/freeme.luyangjie, 20170612, three_pointer_in_camera
import android.os.SystemProperties;
//*/

public class GestureOperate extends SettingsPreferenceFragment implements OnPreferenceChangeListener{
    
    //*/Added by tyd yuanchengye 20140814 for three pointer swipe action
    private static final String THREE_POINTER_TAKE_SCREEN_SHOT_KEY="three_pointer_take_screen_shot";
    private SwitchPreference mThreePointerTakeScreenShotPreference;
    private static final String THREE_POINTER_START_HOTNOT_KEY="three_pointer_start_hotnot";
    private SwitchPreference mThreePointerStartHotnotPreference;
    //*/

    //*/freeme.luyangjie, 20170612, three_pointer_in_camera
    private static final String THREE_POINTER_INTO_CAMERA_KEY="three_pointer_into_camera";
    private SwitchPreference mThreePointerIntoCameraPreference;
    //*/
    //*/freeme.luyangjie, 20170612, Double click home key lock the screen
    private static final String DOUBLE_CLICK_HOME_LOCK_SCREEN="double_click_home_lock_screen";
    private SwitchPreference mDoubleClickLockScreenPreference;
    //*/
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.gesture_operate);
        initializeAllPreferences();
        //*/add by tyd_wangalei 2015.9.21 for Customs hobby sort
        CustomHobbyService mService=new CustomHobbyService(getActivity());
        if(mService.isExistData(R.string.accessibility_settings_ext_title, R.string.gestures_operate)){
			mService.update(R.string.accessibility_settings_ext_title, R.string.gestures_operate);
		}else{
			mService.insert(R.string.accessibility_settings_ext_title, R.string.gestures_operate, this.getClass().getName(), 1, "");
		}
        //*/
    }
    
    private void initializeAllPreferences(){
        //*/Added by tyd yuanchengye 20140814 for three pointer swipe action
        mThreePointerTakeScreenShotPreference = (SwitchPreference) findPreference(THREE_POINTER_TAKE_SCREEN_SHOT_KEY);
        mThreePointerTakeScreenShotPreference.setChecked(Settings.System.getInt(getActivity().getContentResolver(), Settings.System.FREEME_THREE_POINTER_TAKE_SCREEN_SHOT, 1) == 1 ? true : false);
        mThreePointerTakeScreenShotPreference.setOnPreferenceChangeListener(this);
        //*/ freeme.luyangjie, 20170612, three pointer swipe action delete in xiaolajiao project
        if (SystemProperties.get("ro.freeme.xiaolajiao_version").equals("1")) {
            if (mThreePointerTakeScreenShotPreference != null) {
                getPreferenceScreen().removePreference(mThreePointerTakeScreenShotPreference);
            }
        }
        //*/
        /*/ removed by tyd liuchao
        mThreePointerStartHotnotPreference = (SwitchPreference) findPreference(THREE_POINTER_START_HOTNOT_KEY);
        mThreePointerStartHotnotPreference.setChecked(Settings.System.getInt(getActivity().getContentResolver(), Settings.System.THREE_POINTER_START_HOTNOT, 1) == 1 ? true : false);
        mThreePointerStartHotnotPreference.setOnPreferenceChangeListener(this);
        //*/
        //*/ freeme.zhiwei.zhang, 20160909. Smart wake up.
        if (!com.droi.feature.FeatureOption.FREEME_SCREEN_GESTURE_WAKEUP_SUPPORT) {
            Preference preference = findPreference("key_smart_wake_settings");
            if (preference != null) {
                getPreferenceScreen().removePreference(preference);
            }
        }
        //*/

        //*/freeme.luyangjie, 20170612, three pointer in camera
        mThreePointerIntoCameraPreference = (SwitchPreference) findPreference(THREE_POINTER_INTO_CAMERA_KEY);
        mThreePointerIntoCameraPreference.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.FREEME_THREE_POINTER_INTO_CAMERA, 1) == 1 ? true : false);
        mThreePointerIntoCameraPreference.setOnPreferenceChangeListener(this);
        if(/*SystemProperties.get("ro.freeme.xiaolajiao_version").equals("1")*/true){
            if(mThreePointerIntoCameraPreference != null){
                getPreferenceScreen().removePreference(mThreePointerIntoCameraPreference);
            }
        }
        //*/

        //*/freeme.luyangjie,20170612, Double click home key lock the screen
        mDoubleClickLockScreenPreference = (SwitchPreference) findPreference(DOUBLE_CLICK_HOME_LOCK_SCREEN);
        mDoubleClickLockScreenPreference.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.FREEME_DOUBLE_HOMEKEY_LOCK_SCREEN, 1) == 1 ? true : false);
        mDoubleClickLockScreenPreference.setOnPreferenceChangeListener(this);
        if(!SystemProperties.get("ro.freeme.xiaolajiao_version").equals("1")){
            if(mDoubleClickLockScreenPreference != null){
                getPreferenceScreen().removePreference(mDoubleClickLockScreenPreference);
            }
        }
        //*/
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        //*/Added by tyd yuanchengye 20140814 for three pointer swipe action
        if (preference == mThreePointerTakeScreenShotPreference){
            Boolean booleanValue = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.FREEME_THREE_POINTER_TAKE_SCREEN_SHOT, booleanValue ? 1 : 0);
            return true;
        }/*else if(preference == mThreePointerStartHotnotPreference){
            Boolean booleanValue = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.THREE_POINTER_START_HOTNOT, booleanValue ? 1 : 0);
            return true;
        }*/
        //*/freeme.luyangjie, 20170612, three pointer into camera
        else if(preference == mThreePointerIntoCameraPreference){
            Boolean booleanValue = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.FREEME_THREE_POINTER_INTO_CAMERA, booleanValue ? 1 : 0);
            return true;
        }
        //*/
        //*/freeme.luyangjie,20170612, Double click home key lock the screen
        else if(preference == mDoubleClickLockScreenPreference){
            Boolean booleanValue = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.FREEME_DOUBLE_HOMEKEY_LOCK_SCREEN, booleanValue ? 1 : 0);
            return true;
        }
        //*/
        return false;
    }

    //*/Added by Jiangshouting 2016.01.04 for setting code transplant
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.GESTURE_OPERATE;
    }
    //*/

}
