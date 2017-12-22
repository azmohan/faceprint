
package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
//*/ freeme.xuqian ,20170911 ,for system update
import android.preference.PreferenceGroup;
//*/
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mediatek.HobbyDB.CustomHobbyService;

//*/Add by Jiangshouting 2016.01.04 for setting code transplant
import com.android.internal.logging.MetricsLogger;
//*/

public class MoreSystemSettings extends SettingsPreferenceFragment {
    
    private Preference mPowerSettingsPreference;
    private static final String KEY_POWER_SETTINGS = "power_settings";
    
    private Preference mPrintSettingsPreference;
    private static final String KEY_PRINT_SETTINGS="print_settings";
    
    private Preference mDevelopmentSettingsPreference;
    private static final String KEY_DEVELOPMENT_SETTINGS="development_settings";
    
    private SharedPreferences mDevelopmentPreferences;

    private List<String> mTitleList;
    //*/ Add by tyd jiangshouting 2015.08.07 for adding a button to change setting layout 
    private boolean ifGridMode = true;
    private SharedPreferences gridSp;
    //*/

    //*/ freeme.luyangjie,20170609, for Application startup management
    private static final String KEY_APP_MANAGE_START = "manage_app_start";
    private Preference mManageStartPreference;
    //*/

    //*/ freeme.luyangjie, 20170614, add Super headset
    private static final String KEY_SUPER_HEADSET = "super_headset";
    private Preference mSuperHeadsetPreference;
    //*/

    //*/ freeme.fanwuyang, 2017/08/02, for Application Security 
    private static final String KEY_MULITIPLE_ACCOUNTS = "mulitiple_accounts";
    private Preference mMulitipleAccounts;
    //*/

    //*/ freeme.fanwuyang, 2017/08/31, for CChelper 
    private static final String KEY_CCHELPER = "cchelper_settings";
    private Preference mCChelperPreference;
    //*/

    //*/ freeme.xuqian, 2017/08/30, for instructions
    private static final String KEY_INSTRUCTIONS = "instructions";
    private Preference mInstructions;
    //*/ freeme.xuqian, 2017/09/11, for system update
    private static final String KEY_SYSTEM_UPDATE_SETTINGS = "system_update_settings";
    private Preference mSystemUpdate;
    //*/

    //*/ freeme.fanwuyang, 2017/08/02, for Application Security 
    private static final String KEY_APP_SECURITY = "app_security";
    private Preference mAppSecurityPreference;
    //*/

    //*/ freeme.zhangjunxiang, 2017/08/03, for auto clean background 
    private static final String KEY_AUTO_CLEAN_BACKGROUND = "auto_clean_background";
    private Preference mAutoCleanBackgroundPreference;
    //*/

    //*/ freeme.shancong, 20170804, add StorageSettings,BatterySettinbgs and StorageBatterySettings
    private static final String KEY_STORAGE_SETTINGS = "storage_settings";
    private Preference mStorageSettings;
    private static final String KEY_BATTERY_SETTINGS = "battery_settings";
    private Preference mBatterySettings;
    private static final String KEY_STORAGE_BATTERY_SETTINGS = "storage_battery_settings";
    private Preference mStorageBatterySettings;
    //*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDevelopmentPreferences = getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE);
        //*/ Motify by tyd jiangshouting 2015.08.07 for adding a button to change setting layout 
        gridSp = getActivity().getSharedPreferences(SettingsActivity.IFGRIDMODESP, Context.MODE_PRIVATE);
        /*/ Add by tyd fanwuyang 2017/08/07 for default setting layout jingdong
        if (SystemProperties.get("ro.freeme.xlj_jingdong").equals("1")) {
            ifGridMode = gridSp.getBoolean(SettingsActivity.IFGRIDMODESP,SettingsActivity.IFGRIDMODE_JINGDONG);
        } else {
            ifGridMode = gridSp.getBoolean(SettingsActivity.IFGRIDMODESP,SettingsActivity.IFGRIDMODE);
        }
        //*/
        //*/freeme.shancong,2017/11/29,for default setting layout jingdong and telecom
        if (SystemProperties.get("ro.freeme.xlj_jingdong").equals("1") || SystemProperties.get("ro.freeme.xlj_telecom").equals("1")
                || SystemProperties.get("ro.freeme.xlj_bv3b3").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3a").equals("1")
                || SystemProperties.get("ro.freeme.xlj_bv303z").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3h").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3g").equals("1")) {
            ifGridMode = gridSp.getBoolean(SettingsActivity.IFGRIDMODESP,SettingsActivity.IFGRIDMODE_JINGDONG);
        } else {
            ifGridMode = gridSp.getBoolean(SettingsActivity.IFGRIDMODESP,SettingsActivity.IFGRIDMODE);
        }
        //*/
        if(ifGridMode){
            addPreferencesFromResource(R.xml.more_system_settings_grid);
        }else{
            addPreferencesFromResource(R.xml.more_system_settings);
        }

        if(ifGridMode){
            String[] strings = getArguments().getStringArray("dashBoardTile");
            mTitleList = Arrays.asList(strings);
        }
        //*/
        initializeAllPreferences();
    }
    
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
  	  /*/add by tyd_wangalei 2015.9.21 for Customs hobby sort
    	if (preference.getKey().equals(KEY_POWER_SETTINGS)) {
            CustomHobbyService mService=new CustomHobbyService(getActivity());
            if(mService.isExistData(R.string.radio_controls_title, R.string.schedule_power_on_off_settings_title)){
    			mService.update(R.string.radio_controls_title, R.string.schedule_power_on_off_settings_title);
    		}else{
    			mService.insert(R.string.radio_controls_title, R.string.schedule_power_on_off_settings_title,"com.freeme.freemecrontab.Crontab", 1, "com.freeme.freemecrontab");
    		}
    	}
    	 //*/
    	return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    
    private void initializeAllPreferences(){
        
        mPowerSettingsPreference = (Preference)findPreference(KEY_POWER_SETTINGS);
        Intent intent = new Intent("com.android.settings.SCHEDULE_POWER_ON_OFF_SETTING");
        List<ResolveInfo> apps = getPackageManager() .queryIntentActivities(intent, 0);
        if (apps != null && apps.size() != 0) {
            if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
                getPreferenceScreen().removePreference(mPowerSettingsPreference);
            }
        } else {
            getPreferenceScreen().removePreference(mPowerSettingsPreference);
        }
        
        mPrintSettingsPreference = (Preference)findPreference(KEY_PRINT_SETTINGS);
        boolean hasPrintingSupport = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_PRINTING);
        if (!hasPrintingSupport) {
            getPreferenceScreen().removePreference(mPrintSettingsPreference);
        }

        mDevelopmentSettingsPreference = (Preference) findPreference(KEY_DEVELOPMENT_SETTINGS);
        if(mDevelopmentSettingsPreference != null) {
            getPreferenceScreen().removePreference(mDevelopmentSettingsPreference);
            if (showDevelopment()) {
                getPreferenceScreen().addPreference(mDevelopmentSettingsPreference);
            }
        }

        //*/ Motify by tyd jiangshouting 2015.08.07 for adding a button to change setting layout 
        if(ifGridMode){
            ArrayList<Preference> list = new ArrayList<Preference>();
            Preference preference;
            for(int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
                preference = getPreferenceScreen().getPreference(i);
                if(mTitleList.contains(preference.getTitle())) {
                    list.add(preference);
                }
            }

            for(Preference pre : list) {
                getPreferenceScreen().removePreference(pre);
            }
        }
        //*/

        //*/freeme.luyangjie,20170609, for Application startup management
        mManageStartPreference = (Preference)findPreference(KEY_APP_MANAGE_START);
        if(!SystemProperties.get("ro.freeme.xiaolajiao_version").equals("1") && ifGridMode){
            if(mManageStartPreference != null){
                getPreferenceScreen().removePreference(mManageStartPreference);
            }
        }
        //*/

        //*/freeme.luyangjie, 20170614, add Super headset
        mSuperHeadsetPreference = (Preference)findPreference(KEY_SUPER_HEADSET);
        //*/ freeme:fanwuyang on: 2017/11/02 remove Super headset for bv303a
        if((!SystemProperties.get("ro.freeme.xiaolajiao_version").equals("1") || SystemProperties.get("ro.freeme.xlj_bv303a").equals("1")) && ifGridMode) {
        /*/
        if(!SystemProperties.get("ro.freeme.xiaolajiao_version").equals("1") && ifGridMode){
        //*/ 
            if(mSuperHeadsetPreference != null){
                getPreferenceScreen().removePreference(mSuperHeadsetPreference);
            }
        }
        //*/

        //*/ freeme.fanwuyang, 2017/08/31, for CChelper 
        mCChelperPreference = (Preference)findPreference(KEY_CCHELPER);
        if(!SystemProperties.get("ro.freeme.xlj_jingdong").equals("1") && ifGridMode){
            if(mCChelperPreference!= null){
                getPreferenceScreen().removePreference(mCChelperPreference);
            }
        }
        //*/ freeme.fanwuyang, 2017/08/02, for Application Security 
        mMulitipleAccounts = (Preference)findPreference(KEY_MULITIPLE_ACCOUNTS);
        if(!SystemProperties.get("ro.freeme.xiaolajiao_version").equals("1") && ifGridMode){
            if(mMulitipleAccounts!= null){
                getPreferenceScreen().removePreference(mMulitipleAccounts);
            }
        }
        //*/
        //*/ freeme.fanwuyang, 2017/08/02, for Application Security 
        mAppSecurityPreference = (Preference)findPreference(KEY_APP_SECURITY);
        if(!SystemProperties.get("ro.freeme.xlj_jingdong").equals("1") && ifGridMode){
            if(mAppSecurityPreference != null){
                getPreferenceScreen().removePreference(mAppSecurityPreference);
            }
        }
        //*/

        //*/ freeme.xuqian, 2017/08/30, for Instructions
        mInstructions = (Preference)findPreference(KEY_INSTRUCTIONS);
        if(!SystemProperties.get("ro.freeme.xiaolajiao_version").equals("1") && ifGridMode){
            if(mInstructions != null){
                getPreferenceScreen().removePreference(mInstructions);
            }
        }
        //*/

        //*/ freeme.xuqian, 2017/09/11, for system update
        PreferenceGroup parentPreference = getPreferenceScreen();
        if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
            Utils.updatePreferenceToSpecificActivityOrRemove(getActivity(), parentPreference,
                    KEY_SYSTEM_UPDATE_SETTINGS,
                    Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
            mSystemUpdate = (Preference)findPreference(KEY_SYSTEM_UPDATE_SETTINGS);
            //*/freeme.zhangshaopiao, 20171115, add system update for xiaolajiao
            if(!SystemProperties.get("ro.freeme.xiaolajiao_version").equals("1") && ifGridMode){
            /*/
            if(!SystemProperties.get("ro.freeme.xlj_jingdong").equals("1") && ifGridMode){
            //*/
                if(mSystemUpdate != null){
                    getPreferenceScreen().removePreference(mSystemUpdate);
                }
            }
        } else {
            // Remove for secondary users
            mSystemUpdate = (Preference)findPreference(KEY_SYSTEM_UPDATE_SETTINGS);
            if(mSystemUpdate != null){
                getPreferenceScreen().removePreference(mSystemUpdate);
            }
        }
        //*/

        //*/ freeme.zhangjunxiang, 2017/08/03, for auto clean background
        mAutoCleanBackgroundPreference = (Preference)findPreference(KEY_AUTO_CLEAN_BACKGROUND);
        if(!SystemProperties.get("ro.freeme.xlj_jingdong").equals("1") && ifGridMode){
            if(mAutoCleanBackgroundPreference != null){
                getPreferenceScreen().removePreference(mAutoCleanBackgroundPreference);
            }
        }
        //*/

        //*/freeme.shancong, 20170804, add StorageSettings,BatterySettings and StorageBatterySettings
        mStorageSettings = (Preference)findPreference(KEY_STORAGE_SETTINGS);
        mBatterySettings = (Preference)findPreference(KEY_BATTERY_SETTINGS);
        mStorageBatterySettings = (Preference)findPreference(KEY_STORAGE_BATTERY_SETTINGS);
        if(SystemProperties.get("ro.freeme.xlj_jingdong").equals("1") && ifGridMode){
            if(mStorageBatterySettings != null){
                getPreferenceScreen().removePreference(mStorageBatterySettings);
            }
        }
        if(!SystemProperties.get("ro.freeme.xlj_jingdong").equals("1") && ifGridMode){
            if(mStorageSettings != null){
                getPreferenceScreen().removePreference(mStorageSettings);
            }
            if(mBatterySettings != null){
                getPreferenceScreen().removePreference(mBatterySettings);
            }
        }
        //*/
    }
    
    @Override
    public void onResume(){
        super.onResume();
    }

    private boolean showDevelopment() {
        // Don't enable developer options for secondary users.
        if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
            return false;
        }

        UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        if(um.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)) {
            return false;
        }

        //*/freeme.luyangjie,default show Development,2017.06.07
        if(SystemProperties.get("ro.freeme.xiaolajiao_version").equals("1")) {
            return true;
        }
        //*/
        boolean showDev = mDevelopmentPreferences.getBoolean(
                DevelopmentSettings.PREF_SHOW,
                android.os.Build.TYPE.equals("eng"));

        return showDev;
    }

    //*/Added by Jiangshouting 2016.01.04 for setting code transplant
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.INTELLIGENTOPERATESETTINGS;
    }
    //*/
}
