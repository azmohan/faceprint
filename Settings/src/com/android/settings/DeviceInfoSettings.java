/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.mediatek.settings.deviceinfo.DeviceInfoSettingsExts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//*/Added by Jiangshouting 2015.12.29 for setting code transplant
import android.app.ActionBar;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.WifiConfiguration.Status;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.android.settings.ShareFreemeUtil;
import com.android.settings.deviceinfo.CpuStatPreference;
import com.mediatek.HobbyDB.CustomHobbyService;
import com.mediatek.settings.FeatureOption;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;

import com.freeme.os.FreemeBuild;
//*/




public class DeviceInfoSettings extends SettingsPreferenceFragment implements Indexable {

    private static final String LOG_TAG = "DeviceInfoSettings";
    private static final String FILENAME_PROC_VERSION = "/proc/version";
    private static final String FILENAME_MSV = "/sys/board_properties/soc/msv";

    private static final String KEY_REGULATORY_INFO = "regulatory_info";
    private static final String KEY_SYSTEM_UPDATE_SETTINGS = "system_update_settings";
    private static final String PROPERTY_URL_SAFETYLEGAL = "ro.url.safetylegal";
    private static final String PROPERTY_SELINUX_STATUS = "ro.build.selinux";
    private static final String KEY_KERNEL_VERSION = "kernel_version";
    private static final String KEY_BUILD_NUMBER = "build_number";
    private static final String KEY_DEVICE_MODEL = "device_model";
    private static final String KEY_SELINUX_STATUS = "selinux_status";
    private static final String KEY_BASEBAND_VERSION = "baseband_version";
    private static final String KEY_FIRMWARE_VERSION = "firmware_version";
    private static final String KEY_SECURITY_PATCH = "security_patch";
    private static final String KEY_UPDATE_SETTING = "additional_system_update_settings";
    private static final String KEY_EQUIPMENT_ID = "fcc_equipment_id";
    private static final String PROPERTY_EQUIPMENT_ID = "ro.ril.fccid";
    private static final String KEY_DEVICE_FEEDBACK = "device_feedback";
    private static final String KEY_SAFETY_LEGAL = "safetylegal";

    //*/Added by Jiangshouting 2015.12.29 for setting code transplant
    private static final String KEY_GOOGLE_OTA_UPDATE = "google_ota_update";
    private static final String KEY_STATUS_INFO = "status_info";
    private static final String KEY_SHARE_FREEME = "share_freeme_os";
    //*/

    static final int TAPS_TO_BE_A_DEVELOPER = 7;

    //*/Add by tyd chenruoquan 2015-11-06
    static final int TAPS_TO_BE_A_DEVELOPER_MORE = 7;
    //*/

    long[] mHits = new long[3];
    int mDevHitCountdown;
    Toast mDevHitToast;

    //*/Add by tyd chenruoquan 2015-11-06
    int mDevMorHitCountdown;
    Toast mDevMorHitToast;
    //*/

    private DeviceInfoSettingsExts mExts;

    //*/ Added by tyd Jack 20130522 for, show cpu & flash info
    private static final String KEY_CPU_INFO = "cpu_info";
    //*/
    //*/ Added by tyd Greg 2013-05-23 for, show cpu usage
    CpuStatPreference mCpuStatPreference;
    //*/

    /**
     * Added by tyd liuchao 20150804 for, add freeme logo
     */
    private ImageView mLogo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.device_info, container, false);
        //*/ Added by tyd liuchao for device logo
        mLogo = (ImageView) view.findViewById(R.id.logo);
        boolean land = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        mLogo.setImageResource(land ? R.drawable.device_logo_landscape : R.drawable.device_logo);
        mLogo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //*/freeme.luyangjie,20170613, xiaolajiao project delete logo onclick 
                if(!SystemProperties.get("ro.freeme.xiaolajiao_version").equals("1")){
                    Intent intent = new Intent("com.freeme.action.ACTION_FREEME_OS_CARD");
                    startActivity(intent);
                }
                /*/
                Intent intent = new Intent("com.freeme.action.ACTION_FREEME_OS_CARD");
                startActivity(intent);
                //*/
            }
        });
        //*/ end
        return view;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVICEINFO;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_about;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.device_info_settings);

        //*/Modify by Jiangshouting 2015.12.29 for setting code transplant
        //setStringSummary(KEY_FIRMWARE_VERSION, Build.VERSION.RELEASE);
        setStringSummary(KEY_FIRMWARE_VERSION, getString(R.string.system_version,
                FreemeBuild.getFreemeOSLabel(), FreemeBuild.VERSION.FREEMEOS, Build.VERSION.RELEASE));
        //*/
        findPreference(KEY_FIRMWARE_VERSION).setEnabled(true);
        //*/freeme.xupeng, 20170606. add XLJ hardware info
        if (SystemProperties.get("ro.freeme.xiaolajiao_version").equals("1")) {
            findPreference("hardware_info").setEnabled(true);
        } else {
            getPreferenceScreen().removePreference(findPreference("hardware_info"));
            getPreferenceScreen().removePreference(findPreference("product_name"));
        }
        //*/
	    //*/freeme.jianglingfeng, 20170619. add hardware version
	    if (SystemProperties.get("ro.freeme.need_hw_version").equals("1")) {
	        findPreference(KEY_BUILD_NUMBER).setTitle(getString(R.string.custom_version));
	        setStringSummary("hw_version", "U9Y_V1.1");
    	    setStringSummary("custom_build_number","U9Y81W.NYX.V8.01");
        } else if ("1".equals(SystemProperties.get("ro.freeme.n005_fy_customized"))) {
            findPreference(KEY_BUILD_NUMBER).setTitle(getString(R.string.custom_version));
	        findPreference(KEY_FIRMWARE_VERSION).setTitle(getString(R.string.os_version));
	        setStringSummary(KEY_FIRMWARE_VERSION,
                    SystemProperties.get("persist.freeme.os.version",Build.VERSION.RELEASE));
	        setStringSummary("carrier_aggregation", "CAT4");
	        setStringSummary("hw_version", "V1.0");
	        setStringSummary("custom_build_number",
                    SystemProperties.get("persist.freeme.sw.version",
                            SystemProperties.get("ro.freeme.sw.version")));
	    } else {
	        getPreferenceScreen().removePreference(findPreference("carrier_aggregation"));
	        getPreferenceScreen().removePreference(findPreference("hw_version"));
	        getPreferenceScreen().removePreference(findPreference("custom_build_number"));
	    }
	    //*/

        String patch = Build.VERSION.SECURITY_PATCH;
        if (!"".equals(patch)) {
            try {
                SimpleDateFormat template = new SimpleDateFormat("yyyy-MM-dd");
                Date patchDate = template.parse(patch);
                String format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy");
                patch = DateFormat.format(format, patchDate).toString();
            } catch (ParseException e) {
                // broken parse; fall through and use the raw string
            }
            setStringSummary(KEY_SECURITY_PATCH, patch);
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_SECURITY_PATCH));

        }
        setValueSummary(KEY_BASEBAND_VERSION, "gsm.version.baseband");
        setStringSummary(KEY_DEVICE_MODEL, Build.MODEL + getMsvSuffix());
        setValueSummary(KEY_EQUIPMENT_ID, PROPERTY_EQUIPMENT_ID);
        setStringSummary(KEY_DEVICE_MODEL, Build.MODEL);
        setStringSummary(KEY_BUILD_NUMBER, Build.DISPLAY);
        findPreference(KEY_BUILD_NUMBER).setEnabled(true);
        findPreference(KEY_KERNEL_VERSION).setSummary(getFormattedKernelVersion());

        //*/Added by tyd chenruoquan 2015-11-06
        if(getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE).getBoolean(DevelopmentSettings.PREF_SHOW,
                        android.os.Build.TYPE.equals("eng"))){
            findPreference(KEY_DEVICE_MODEL).setEnabled(true);
        }else{
            findPreference(KEY_DEVICE_MODEL).setEnabled(false);
        }
        findPreference(KEY_SHARE_FREEME).setEnabled(true);
        //*/

        //*/freeme.xupeng, 20170606. add XLJ hardware info
        if (SystemProperties.get("ro.freeme.xiaolajiao_version").equals("1") && !SystemProperties.get("ro.freeme.xlj_bv303z").equals("1") && !SystemProperties.get("ro.freeme.xlj_bv3a3a").equals("1") && !SystemProperties.get("ro.freeme.xlj_bv3a3h").equals("1") && !SystemProperties.get("ro.freeme.xlj_bv3a3g").equals("1")) {
            setStringSummary(KEY_BUILD_NUMBER, "20170605Q_V1.0");
        }
        //*/
        //*/freeme.luyangjie, 20171025. add BV303A hardware info
        if (SystemProperties.get("ro.freeme.xlj_bv303a").equals("1")) {
            setStringSummary(KEY_BUILD_NUMBER, "20160926Q-CT-V5.0");
        }
        //*/
        //*/freeme.fanwuyang, 20170606. add XLJ hardware info
        if (SystemProperties.get("ro.freeme.xlj_bv3b3").equals("1") || SystemProperties.get("ro.freeme.xlj_bv303z").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3a").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3h").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3g").equals("1")) {
            setStringSummary(KEY_BUILD_NUMBER, "20170608S_V1.0");
        }
        //*/

        if (!SELinux.isSELinuxEnabled()) {
            String status = getResources().getString(R.string.selinux_status_disabled);
            setStringSummary(KEY_SELINUX_STATUS, status);
        } else if (!SELinux.isSELinuxEnforced()) {
            String status = getResources().getString(R.string.selinux_status_permissive);
            setStringSummary(KEY_SELINUX_STATUS, status);
        }

        // Remove selinux information if property is not present
        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_SELINUX_STATUS,
                PROPERTY_SELINUX_STATUS);

        // Remove Safety information preference if PROPERTY_URL_SAFETYLEGAL is not set
        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_SAFETY_LEGAL,
                PROPERTY_URL_SAFETYLEGAL);

        // Remove Equipment id preference if FCC ID is not set by RIL
        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_EQUIPMENT_ID,
                PROPERTY_EQUIPMENT_ID);

        // Remove Baseband version if wifi-only device
        if (Utils.isWifiOnly(getActivity())) {
            getPreferenceScreen().removePreference(findPreference(KEY_BASEBAND_VERSION));
        }

        // Dont show feedback option if there is no reporter.
        if (TextUtils.isEmpty(getFeedbackReporterPackage(getActivity()))) {
            getPreferenceScreen().removePreference(findPreference(KEY_DEVICE_FEEDBACK));
        }

        /*
         * Settings is a generic app and should not contain any device-specific
         * info.
         */
        final Activity act = getActivity();

        // These are contained by the root preference screen
        PreferenceGroup parentPreference = getPreferenceScreen();
        if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
            Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference,
                    KEY_SYSTEM_UPDATE_SETTINGS,
                    Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
            //*/ freeme.xuqian ,20170911 ,remove system_update from about device
            //*/freeme.zhangshaopiao, 20171115, add system update for xiaolajiao
            if(SystemProperties.get("ro.freeme.xiaolajiao_version").equals("1")){
            /*/
            if(SystemProperties.get("ro.freeme.xlj_jingdong").equals("1")){
            //*/
                removePreference(KEY_SYSTEM_UPDATE_SETTINGS);
            }
            //*/
        } else {
            // Remove for secondary users
            removePreference(KEY_SYSTEM_UPDATE_SETTINGS);
        }
        
        // Read platform settings for additional system update setting
        removePreferenceIfBoolFalse(KEY_UPDATE_SETTING,
                R.bool.config_additional_system_update_setting_enable);

        // Remove regulatory information if none present.
        final Intent intent = new Intent(Settings.ACTION_SHOW_REGULATORY_INFO);
        if (getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
            Preference pref = findPreference(KEY_REGULATORY_INFO);
            if (pref != null) {
                getPreferenceScreen().removePreference(pref);
            }
        }

        //*/ Added by tyd Greg 2013-05-23 for, show cpu usage
        mCpuStatPreference = (CpuStatPreference)findPreference("cpu_info");
        //*/
        
        //*/ Added by tyd Jack 20130522 for, show cpu & flash info
        int cpuCores = getCpuCores();
        long cpuFreq = getMaxCpuFreq();

        int resId = act.getResources().getIdentifier("cpu_cores_"+cpuCores,"string",getActivity().getPackageName()); 
        String cpuCoresString = getActivity().getString(resId);
        //*/ Modified by tyd liuchao for cpu info, 20150602
        String cpuInfoSummary = "";
        //String cpuInfoSummary = getString(R.string.cpu_cores_freq, cpuCoresString, (float)cpuFreq/(1000*1000));
        if (FeatureOption.TYD_CPU_INFO_BITS) {
            cpuInfoSummary = cpuCoresString + " " + getString(R.string.bits);
        } else {
            //*/ freeme jianglingfeng for nyx_n005 fake 8 1.5GHZ
            if (FeatureOption.FREEME_CPU_INFO_FAKE) {
                cpuFreq = 1500000;
            }
            //*/
            //*/ freeme:fanwuyang on: 2017/10/24 telecom project to change Frep to 1.25Ghz
            if (SystemProperties.get("ro.freeme.xlj_telecom").equals("1") && !SystemProperties.get("ro.freeme.xlj_bv303a").equals("1") 
                    && !SystemProperties.get("ro.freeme.xlj_bv3b3").equals("1") && !SystemProperties.get("ro.freeme.xlj_bv303z").equals("1") && !SystemProperties.get("ro.freeme.xlj_bv3a3a").equals("1") && !SystemProperties.get("ro.freeme.xlj_bv3a3h").equals("1") && !SystemProperties.get("ro.freeme.xlj_bv3a3g").equals("1")) {
                cpuFreq = 1250000;
            }else if(SystemProperties.get("ro.freeme.xlj_bv303a").equals("1")){
                cpuFreq = 1100000;
            }else if(SystemProperties.get("ro.freeme.xlj_bv3b3").equals("1") || SystemProperties.get("ro.freeme.xlj_bv303z").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3a").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3h").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3g").equals("1")){
                cpuFreq = 1300000;
            }
            //*/
            cpuInfoSummary = getString(R.string.cpu_cores_freq, cpuCoresString, (float)cpuFreq/(1000*1000));

        }
        //*/
                
        Preference cpuInfo = parentPreference.findPreference(KEY_CPU_INFO);
        cpuInfo.setSummary(cpuInfoSummary);
        
        if("1".equals(SystemProperties.get("ro.freeme.n005_fy_customized"))) {
            String cpufakehz = SystemProperties.get("persist.freeme.cpu.model",
                    SystemProperties.get("ro.freeme.cpu.model"));
            if("6735".equals(cpufakehz)) {
                cpuInfo.setSummary("四核 1.0GHz");
            } else if ("9832".equals(cpufakehz)) {
                cpuInfo.setSummary("四核 1.3GHz");
            } else if ("6737".equals(cpufakehz)) {
                cpuInfo.setSummary("四核 1.1GHz");
            }
        }
        
        //*/ freeme:fanwuyang on: 2017/11/29
        if(SystemProperties.get("ro.freeme.xlj_bv3b3").equals("1") || SystemProperties.get("ro.freeme.xlj_bv303z").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3a").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3h").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3g").equals("1")){
            getPreferenceScreen().removePreference(findPreference(KEY_CPU_INFO));
        }
        //*/

        ///M:
        mExts = new DeviceInfoSettingsExts(getActivity(), this);
        mExts.initMTKCustomization(getPreferenceScreen());
    }

    @Override
    public void onResume() {
        super.onResume();

        //*/ Added by tyd liuchao for new style
        Activity activity = getActivity();
        Window win = activity.getWindow();
        WindowManager.LayoutParams params = win.getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        win.setAttributes(params);

        ActionBar actionBar = activity.getActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        //*/
        
        mDevHitCountdown = getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE).getBoolean(DevelopmentSettings.PREF_SHOW,
                        android.os.Build.TYPE.equals("eng")) ? -1 : TAPS_TO_BE_A_DEVELOPER;
        mDevHitToast = null;

        //*/Add by tyd chenruoquan 2015-11-06
        mDevMorHitCountdown = getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE_MORE,
                Context.MODE_PRIVATE).getBoolean(DevelopmentSettings.PREF_SHOW_MORE,
                        android.os.Build.TYPE.equals("eng")) ? -1 : TAPS_TO_BE_A_DEVELOPER_MORE;
        mDevHitToast = null;
        //*/
        //*/ Added by tyd Greg 2013-05-23 for, show cpu usage
        mCpuStatPreference.startDrawCpuStat();
        //*/
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(KEY_FIRMWARE_VERSION)) {
            System.arraycopy(mHits, 1, mHits, 0, mHits.length-1);
            mHits[mHits.length-1] = SystemClock.uptimeMillis();
            if (mHits[0] >= (SystemClock.uptimeMillis()-500)) {
                UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
                if (um.hasUserRestriction(UserManager.DISALLOW_FUN)) {
                    Log.d(LOG_TAG, "Sorry, no fun for you!");
                    return false;
                }

                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("android",
                        com.android.internal.app.PlatLogoActivity.class.getName());
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Unable to start activity " + intent.toString());
                }
            }
        } else if (preference.getKey().equals(KEY_BUILD_NUMBER)) {
            // Don't enable developer options for secondary users.
            if (UserHandle.myUserId() != UserHandle.USER_OWNER) return true;

            // Don't enable developer options until device has been provisioned
            if (Settings.Global.getInt(getActivity().getContentResolver(),
                    Settings.Global.DEVICE_PROVISIONED, 0) == 0) {
                return true;
            }

            final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
            if (um.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)) return true;

            if (mDevHitCountdown > 0) {
                mDevHitCountdown--;
                if (mDevHitCountdown == 0) {
                    //*/Add by tyd chenruoquan 2015-11-07
                    findPreference(KEY_DEVICE_MODEL).setEnabled(true);
                    //*/
                    getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                            Context.MODE_PRIVATE).edit().putBoolean(
                                    DevelopmentSettings.PREF_SHOW, true).apply();
                    if (mDevHitToast != null) {
                        mDevHitToast.cancel();
                    }
                    mDevHitToast = Toast.makeText(getActivity(), R.string.show_dev_on,
                            Toast.LENGTH_LONG);
                    mDevHitToast.show();
                    // This is good time to index the Developer Options
                    Index.getInstance(
                            getActivity().getApplicationContext()).updateFromClassNameResource(
                                    DevelopmentSettings.class.getName(), true, true);

                } else if (mDevHitCountdown > 0
                        && mDevHitCountdown < (TAPS_TO_BE_A_DEVELOPER-2)) {
                    if (mDevHitToast != null) {
                        mDevHitToast.cancel();
                    }
                    mDevHitToast = Toast.makeText(getActivity(), getResources().getQuantityString(
                            R.plurals.show_dev_countdown, mDevHitCountdown, mDevHitCountdown),
                            Toast.LENGTH_SHORT);
                    mDevHitToast.show();
                }
            } else if (mDevHitCountdown < 0) {
                if (mDevHitToast != null) {
                    mDevHitToast.cancel();
                }
                mDevHitToast = Toast.makeText(getActivity(), R.string.show_dev_already,
                        Toast.LENGTH_LONG);
                mDevHitToast.show();
            }
        } else if (preference.getKey().equals(KEY_DEVICE_FEEDBACK)) {
            sendFeedback();
        } else if(preference.getKey().equals(KEY_SYSTEM_UPDATE_SETTINGS)) {
            CarrierConfigManager configManager =
                    (CarrierConfigManager) getSystemService(Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle b = configManager.getConfig();
            if (b.getBoolean(CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_BOOL)) {
                ciActionOnSysUpdate(b);
            }
        }else if(preference.getKey().equals(KEY_GOOGLE_OTA_UPDATE)){
             //*/ADD  by tyd wangalei 2015.9.22 for Customs hobby sort
             CustomHobbyService mService=new CustomHobbyService(getActivity());
             if(mService.isExistData(R.string.about_settings, R.string.mtk_system_update)){
                mService.update(R.string.about_settings, R.string.mtk_system_update);
            }else{
                mService.insert(R.string.about_settings, R.string.mtk_system_update,"com.mediatek.GoogleOta.GoogleOtaMainActivity", 1, "com.mediatek.GoogleOta");
            }
        }else if(preference.getKey().equals(KEY_STATUS_INFO)){
              CustomHobbyService mService=new CustomHobbyService(getActivity());
              if(mService.isExistData(R.string.about_settings, R.string.device_status)){
                    mService.update(R.string.about_settings, R.string.device_status);
              }else{
                    mService.insert(R.string.about_settings, R.string.device_status, "com.android.settings.deviceinfo.Status", 1,"com.android.settings");
              }   
        }else if (preference.getKey().equals(KEY_SHARE_FREEME)) {
            ShareFreemeUtil.shareFreemeOS(getActivity());
        }
        //*/Add by tyd chenruoquan 2015-11-06
        else if (preference.getKey().equals(KEY_DEVICE_MODEL)) {
            // Don't enable developer options for secondary users.
            if (UserHandle.myUserId() != UserHandle.USER_OWNER) return true;

            final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
            if (um.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)) return true;

            if (mDevMorHitCountdown > 0) {
                mDevMorHitCountdown--;
                if (mDevMorHitCountdown == 0) {
                    getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE_MORE,
                            Context.MODE_PRIVATE).edit().putBoolean(
                                    DevelopmentSettings.PREF_SHOW_MORE, true).apply();
                    if (mDevMorHitToast != null) {
                        mDevMorHitToast.cancel();
                    }
                    mDevMorHitToast = Toast.makeText(getActivity(), R.string.show_more_dev_on,
                            Toast.LENGTH_LONG);
                    mDevMorHitToast.show();
                    // This is good time to index the Developer Options
                    Index.getInstance(
                            getActivity().getApplicationContext()).updateFromClassNameResource(
                                    DevelopmentSettings.class.getName(), true, true);

                } else if (mDevMorHitCountdown > 0
                        && mDevMorHitCountdown < (TAPS_TO_BE_A_DEVELOPER_MORE-2)) {
                    if (mDevMorHitToast != null) {
                    	mDevMorHitToast.cancel();
                    }
                    mDevMorHitToast = Toast.makeText(getActivity(), getResources().getQuantityString(
                            R.plurals.show_more_dev_countdown, mDevMorHitCountdown, mDevMorHitCountdown),
                            Toast.LENGTH_SHORT);
                    mDevMorHitToast.show();
                }
            } else if (mDevMorHitCountdown < 0) {
                if (mDevMorHitToast != null) {
                    mDevMorHitToast.cancel();
                }
                mDevMorHitToast = Toast.makeText(getActivity(),R.string.show_more_dev_already,
                        Toast.LENGTH_LONG);
                mDevMorHitToast.show();
            }
        }
        ///M:
        mExts.onCustomizedPreferenceTreeClick(preference);
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * Trigger client initiated action (send intent) on system update
     */
    private void ciActionOnSysUpdate(PersistableBundle b) {
        String intentStr = b.getString(CarrierConfigManager.
                KEY_CI_ACTION_ON_SYS_UPDATE_INTENT_STRING);
        if (!TextUtils.isEmpty(intentStr)) {
            String extra = b.getString(CarrierConfigManager.
                    KEY_CI_ACTION_ON_SYS_UPDATE_EXTRA_STRING);
            String extraVal = b.getString(CarrierConfigManager.
                    KEY_CI_ACTION_ON_SYS_UPDATE_EXTRA_VAL_STRING);

            Intent intent = new Intent(intentStr);
            if (!TextUtils.isEmpty(extra)) {
                intent.putExtra(extra, extraVal);
            }
            Log.d(LOG_TAG, "ciActionOnSysUpdate: broadcasting intent " + intentStr +
                    " with extra " + extra + ", " + extraVal);
            getActivity().getApplicationContext().sendBroadcast(intent);
        }
    }

    private void removePreferenceIfPropertyMissing(PreferenceGroup preferenceGroup,
            String preference, String property ) {
        if (SystemProperties.get(property).equals("")) {
            // Property is missing so remove preference from group
            try {
                preferenceGroup.removePreference(findPreference(preference));
            } catch (RuntimeException e) {
                Log.d(LOG_TAG, "Property '" + property + "' missing and no '"
                        + preference + "' preference");
            }
        }
    }

    private void removePreferenceIfBoolFalse(String preference, int resId) {
        if (!getResources().getBoolean(resId)) {
            Preference pref = findPreference(preference);
            if (pref != null) {
                getPreferenceScreen().removePreference(pref);
            }
        }
    }

    private void setStringSummary(String preference, String value) {
        try {
            findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(preference).setSummary(
                getResources().getString(R.string.device_info_default));
        }
    }

    private void setValueSummary(String preference, String property) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property,
                            getResources().getString(R.string.device_info_default)));
        } catch (RuntimeException e) {
            // No recovery
        }
    }

    private void sendFeedback() {
        String reporterPackage = getFeedbackReporterPackage(getActivity());
        if (TextUtils.isEmpty(reporterPackage)) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_BUG_REPORT);
        intent.setPackage(reporterPackage);
        startActivityForResult(intent, 0);
    }

    /**
     * Reads a line from the specified file.
     * @param filename the file to read from
     * @return the first line, if any.
     * @throws IOException if the file couldn't be read
     */
    private static String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    public static String getFormattedKernelVersion() {
        try {
            //*/freeme.zhangjunxiang,20171205 for modify kernel version
            if (SystemProperties.get("ro.freeme.xlj_bv3b3").equals("1") || SystemProperties.get("ro.freeme.xlj_bv303z").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3a").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3h").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3g").equals("1")) {
                String n1 = "3.10.65-svn1513"; 
                String n2 = "server@droi #1";
                String n3 = "Mon Nov 27 14:29:02 CST 2017"; 
                return n1 + "\n" + n2 + "\n" + n3;
            } else {
                return formatKernelVersion(readLine(FILENAME_PROC_VERSION));
            }
            /*/
            return formatKernelVersion(readLine(FILENAME_PROC_VERSION));
            //*/
        } catch (IOException e) {
            Log.e(LOG_TAG,
                "IO Exception when getting kernel version for Device Info screen",
                e);

            return "Unavailable";
        }
    }

    public static String formatKernelVersion(String rawKernelVersion) {
        // Example (see tests for more):
        // Linux version 3.0.31-g6fb96c9 (android-build@xxx.xxx.xxx.xxx.com) \
        //     (gcc version 4.6.x-xxx 20120106 (prerelease) (GCC) ) #1 SMP PREEMPT \
        //     Thu Jun 28 11:02:39 PDT 2012

        final String PROC_VERSION_REGEX =
            "Linux version (\\S+) " + /* group 1: "3.0.31-g6fb96c9" */
            "\\((\\S+?)\\) " +        /* group 2: "x@y.com" (kernel builder) */
            "(?:\\(gcc.+? \\)) " +    /* ignore: GCC version information */
            "(#\\d+) " +              /* group 3: "#1" */
            "(?:.*?)?" +              /* ignore: optional SMP, PREEMPT, and any CONFIG_FLAGS */
            "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; /* group 4: "Thu Jun 28 11:02:39 PDT 2012" */

        Matcher m = Pattern.compile(PROC_VERSION_REGEX).matcher(rawKernelVersion);
        if (!m.matches()) {
            Log.e(LOG_TAG, "Regex did not match on /proc/version: " + rawKernelVersion);
            return "Unavailable";
        } else if (m.groupCount() < 4) {
            Log.e(LOG_TAG, "Regex match on /proc/version only returned " + m.groupCount()
                    + " groups");
            return "Unavailable";
        }
        return m.group(1) + "\n" +                 // 3.0.31-g6fb96c9
            m.group(2) + " " + m.group(3) + "\n" + // x@y.com #1
            m.group(4);                            // Thu Jun 28 11:02:39 PDT 2012
    }

    /**
     * Returns " (ENGINEERING)" if the msv file has a zero value, else returns "".
     * @return a string to append to the model number description.
     */
    private String getMsvSuffix() {
        // Production devices should have a non-zero value. If we can't read it, assume it's a
        // production device so that we don't accidentally show that it's an ENGINEERING device.
        try {
            String msv = readLine(FILENAME_MSV);
            // Parse as a hex number. If it evaluates to a zero, then it's an engineering build.
            if (Long.parseLong(msv, 16) == 0) {
                return " (ENGINEERING)";
            }
        } catch (IOException ioe) {
            // Fail quietly, as the file may not exist on some devices.
        } catch (NumberFormatException nfe) {
            // Fail quietly, returning empty string should be sufficient
        }
        return "";
    }

    private static String getFeedbackReporterPackage(Context context) {
        final String feedbackReporter =
                context.getResources().getString(R.string.oem_preferred_feedback_reporter);
        if (TextUtils.isEmpty(feedbackReporter)) {
            // Reporter not configured. Return.
            return feedbackReporter;
        }
        // Additional checks to ensure the reporter is on system image, and reporter is
        // configured to listen to the intent. Otherwise, dont show the "send feedback" option.
        final Intent intent = new Intent(Intent.ACTION_BUG_REPORT);

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolvedPackages =
                pm.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo info : resolvedPackages) {
            if (info.activityInfo != null) {
                if (!TextUtils.isEmpty(info.activityInfo.packageName)) {
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(info.activityInfo.packageName, 0);
                        if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                            // Package is on the system image
                            if (TextUtils.equals(
                                        info.activityInfo.packageName, feedbackReporter)) {
                                return feedbackReporter;
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                         // No need to do anything here.
                    }
                }
            }
        }
        return null;
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.device_info_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<String>();
                if (isPropertyMissing(PROPERTY_SELINUX_STATUS)) {
                    keys.add(KEY_SELINUX_STATUS);
                }
                if (isPropertyMissing(PROPERTY_URL_SAFETYLEGAL)) {
                    keys.add(KEY_SAFETY_LEGAL);
                }
                if (isPropertyMissing(PROPERTY_EQUIPMENT_ID)) {
                    keys.add(KEY_EQUIPMENT_ID);
                }
                // Remove Baseband version if wifi-only device
                if (Utils.isWifiOnly(context)) {
                    keys.add((KEY_BASEBAND_VERSION));
                }
                // Dont show feedback option if there is no reporter.
                if (TextUtils.isEmpty(getFeedbackReporterPackage(context))) {
                    keys.add(KEY_DEVICE_FEEDBACK);
                }
                if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
                    keys.add(KEY_SYSTEM_UPDATE_SETTINGS);
                }
                if (!context.getResources().getBoolean(
                        R.bool.config_additional_system_update_setting_enable)) {
                    keys.add(KEY_UPDATE_SETTING);
                }
                return keys;
            }

            private boolean isPropertyMissing(String property) {
                return SystemProperties.get(property).equals("");
            }
        };
    

    /** Added by tyd Jack 20130522 for, show cpu & flash info
     * @return Cpu Cores num
     */
    public static int getCpuCores() {
        // Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                // Check if filename is "cpu", followed by a single digit number
                if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }
	//*/ freeme jianglingfeng for nyx_n005 fake 8 cores
	if (FeatureOption.FREEME_CPU_INFO_FAKE) {
	    if("1".equals(SystemProperties.get("ro.freeme.n005_fy_customized"))) {
	        return 4;
	    }
		return 8;
	}
	//*/
        try {
            // Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            // Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            // Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            e.printStackTrace();
            // Default to return 1 core
            return 1;
        }
    }
    
    /** Added by tyd Jack 20130522 for, show cpu & flash info
     * @return Cpu freq
     */
    public static long getMaxCpuFreq() {
        long result = 1 * 1000 * 1000;
        try {
            String[] args = { "/system/bin/cat", "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" };
            ProcessBuilder cmd = new ProcessBuilder(args);
            Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[24];
            in.read(re);
            result = Long.valueOf(new String(re).trim());
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return result;
    }
    
    //*/ Added by tyd Greg 2013-05-23 for, show cpu usage
    @Override
    public void onPause() {
        super.onPause();
        mCpuStatPreference.stopDrawCpuStat();
        Log.i("GoogleOta","DeviceInfoSettings:onDestroy");
    }
    //*/

    //*/ Added by tyd liuchao for device logo
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        boolean land = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        mLogo.setImageResource(land ? R.drawable.device_logo_landscape : R.drawable.device_logo);
    }
    //*/ end

}

