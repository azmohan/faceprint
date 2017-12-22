package com.android.settings.accessibility;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.DevelopmentSettings;
import com.android.settings.GuideAlertActivity;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.mediatek.settings.FeatureOption;

import com.freeme.internal.server.INativeMiscService;
//add by tyd.zhanglingzeng 20140819 tp mini screen mode.
import android.content.res.Configuration;
import android.content.ContentResolver;
import android.widget.Toast;
import android.view.WindowManager;
import android.app.ActivityManager;
//add end
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;
import android.content.res.Resources;
import com.android.settings.search.SearchIndexableRaw;
import android.content.pm.PackageManager;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.pm.ServiceInfo;

//*/Add by Jiangshouting 2016.01.04 for setting code transplant
import com.android.internal.logging.MetricsLogger;
//*/


public class AccessibilitySettingsExt extends SettingsPreferenceFragment  implements
    //*/Modify by Tyd Jiangshouting 2015.11.19 for Setting search function lacking some data.
    //OnPreferenceChangeListener {
    OnPreferenceChangeListener , Indexable{
    //*/
	
	private static final String TAG = "AccessibilitySettingsExt";
	
	//*/add by tyd cch 20131029 for pocket mode
    private static final String BUTTON_OTHERS_POCKET_MODE_KEY = "pocket_mode_ring_key";
    private SwitchPreference mButtonPocketModeType;
    //*/

    //*/Droi.zhangweinan, 20161201. for add tts option
    private static final String TTS_KEY = "tts_key";
    private SwitchPreference mButtonTtsPreference;
    private int mTtstatus = 0;
    //*/

  //*/ tyd.biantao 20131105. tp glove mode.
    private static final String TP_GLOVE_MODE_KEY = "tp_glove_mode_key";
    private SwitchPreference mTpGloveModePreference;
    private INativeMiscService mNativeMiscService;
    //*/
    
  //add by huangyiquan 20140317 for touch-protect mode
    private static final String SCREEN_ON_PROXIMITY_SENSOR = "screen_on_proximity_sensor";
    private SwitchPreference mScreenOnProximitySensor;
    //end by huangyiquan
    
  /*/Added by tyd Greg 2013-11-20,for smart wake settings
    private static final String SMART_CATEGORY = "smart_category";
    private PreferenceCategory mSmartCategory;
    //*/
    
    /*/Added by tyd liuchao 2015-04-13,for new style
    private static final String GESTURES_CATEGORY = "gestures_category";
    private PreferenceCategory mgesturesCategory;
    //*/
    
    //*/ Added by Linguanrong for supershot, 2015-6-15
    private static final String KEY_SUPERSHOT = "supershot_key";
    private SwitchPreference mSuperShotPreference;
    //*/

    //*/ freeme:fanwuyang on: 2017/08/02 add screenshot guide
    private static final String KEY_SCREENSHOT_GUIDE = "key_screenshot_guide";
    private Preference mScreenshotGuidePreference;
    //*/

    //*/ freeme.huzhongtao 20161208 for suspension
    private static final String KEY_SUSPENSION = "suspension_key";
    private SwitchPreference mSuspensionPreference;
    //*/

    //*/ freeme.luyangjie.20170816,for gestures operate
    private static final String KEY_GESTURES_OPERATE = "key_gestures_operate";
    private Preference mGesturesOperate;
    //*/
    
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

	//*/Added by tyd liuchao for guide, 2015-08-17
        SharedPreferences sp = getActivity().getSharedPreferences("FirstRun", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        boolean isFirstRun = sp.getBoolean("isFirstRun", true);

        /*/ freeme.xupeng, 20170615. remove accessibility guide for XLJ
        if(isFirstRun){
            editor.putBoolean("isFirstRun", false);
            editor.commit();
            Intent startGuideDialog = new Intent(getActivity(), GuideAlertActivity.class);
            startActivity(startGuideDialog);
        }
	    //*/

		//*/ tyd.biantao 20131105. tp glove mode.
        mNativeMiscService = INativeMiscService.Stub.asInterface(
                android.os.ServiceManager.getService(INativeMiscService.SERVICE_NAME));
        //*/
        
		addPreferencesFromResource(R.xml.accessibility_settings_ext);
		initializeAllPreferences();
    }

	private void initializeAllPreferences(){
		//mSmartCategory = (PreferenceCategory) findPreference(SMART_CATEGORY);
		//mgesturesCategory = (PreferenceCategory) findPreference(GESTURES_CATEGORY);
		//*/add by tyd cch 20131029 for pocket mode
        mButtonPocketModeType = (SwitchPreference)findPreference(BUTTON_OTHERS_POCKET_MODE_KEY);
		if (mButtonPocketModeType != null) {
			if (Settings.System.getInt(this.getContentResolver(),
					Settings.System.FREEME_POCKET_MODE_KEY, 0) == 1) {
				mButtonPocketModeType.setChecked(true);
			} else {
				mButtonPocketModeType.setChecked(false);
			}
			mButtonPocketModeType.setOnPreferenceChangeListener(this);
		}
        //*/
		
		//*/ tyd.biantao 20131105. tp glove mode.
        mTpGloveModePreference = (SwitchPreference) findPreference(TP_GLOVE_MODE_KEY);
        if (FeatureOption.TYD_TP_GLOVE_SUPPORT) {
            boolean tpGloveChecked;
            try {
                tpGloveChecked = (mNativeMiscService.devStateCtrl(INativeMiscService.DSC_DEV_TP_GLOV,
                    INativeMiscService.DSC_OPR_FETCH, 0) == INativeMiscService.DSC_MODE_ENABLE);
            } catch (Exception e) {
                tpGloveChecked = false;
                android.util.Log.e(TAG, e.toString());
            }
            mTpGloveModePreference.setChecked(tpGloveChecked);
		    mTpGloveModePreference.setOnPreferenceChangeListener(this);
        } else {
            if (mTpGloveModePreference != null) {
                getPreferenceScreen().removePreference(mTpGloveModePreference);
            }
        }
        
        // freeme.huangyiquan, 20140317. TouchProtect.
        mScreenOnProximitySensor = (SwitchPreference) findPreference(SCREEN_ON_PROXIMITY_SENSOR);
        if (com.droi.feature.FeatureOption.FREEME_TOUCH_PROTECT) {
            boolean enabled = Settings.System.getInt(getContentResolver(),
                        SCREEN_ON_PROXIMITY_SENSOR, 0) == 1;
            if (mScreenOnProximitySensor != null) {
                mScreenOnProximitySensor.setChecked(enabled);
                mScreenOnProximitySensor.setOnPreferenceChangeListener(this);
            }
        } else {
            if (mScreenOnProximitySensor != null) {
                getPreferenceScreen().removePreference(mScreenOnProximitySensor);
            }
        }
        //*/
        
        if (!FeatureOption.FREEME_NON_TOUCH_OPERATION_SUPPORT) {
            Preference preference = findPreference("key_non_touch_settings");
            if (preference != null) {
                getPreferenceScreen().removePreference(preference);
            }
        }
        
        //*/ Added by Linguanrong for supershot, 2015-6-15
        mSuperShotPreference = (SwitchPreference) findPreference(KEY_SUPERSHOT);
        mSuperShotPreference.setChecked(Settings.System.getInt(getContentResolver(), 
                Settings.System.SUPERSHOT_MODE_DEFAULT, 0) == 1);
        mSuperShotPreference.setOnPreferenceChangeListener(this);
        //*/

        //*/ freeme:fanwuyang on: 2017/08/02 add screenshot guide
        mScreenshotGuidePreference = (Preference) findPreference(KEY_SCREENSHOT_GUIDE);
        if(!SystemProperties.get("ro.freeme.xlj_jingdong").equals("1")) {
            if (mScreenshotGuidePreference!= null) {
                getPreferenceScreen().removePreference(mScreenshotGuidePreference);
            }
        }
        //*/

        //*/ freeme.luyangjie.20170816,remove gestures operate
        mGesturesOperate = (Preference) findPreference(KEY_GESTURES_OPERATE);
        if(SystemProperties.get("ro.freeme.xlj_jingdong").equals("1")) {
            if (mGesturesOperate!= null) {
                getPreferenceScreen().removePreference(mGesturesOperate);
            }
        }
        //*/

        //*/ freeme.zhangshaopiao.20171106,remove gestures operate
        mGesturesOperate = (Preference) findPreference(KEY_GESTURES_OPERATE);
        if(SystemProperties.get("ro.freeme.xlj_bv303a").equals("1")) {
            if (mGesturesOperate!= null) {
                getPreferenceScreen().removePreference(mGesturesOperate);
            }
        }
        //*/

        //*/ freeme.huzhongtao 20161208 for suspension
        mSuspensionPreference = (SwitchPreference) findPreference(KEY_SUSPENSION);
        if (com.droi.feature.FeatureOption.FREEME_SUSPENSION_SUPPORT) {
            mSuspensionPreference.setChecked(Settings.System.getInt(getContentResolver(), 
                    Settings.System.SUSPENSION_MODE_DEFAULT, 0) == 1);
            mSuspensionPreference.setOnPreferenceChangeListener(this);
        } else {
            if (mSuspensionPreference != null) {
                getPreferenceScreen().removePreference(mSuspensionPreference);
            }
        }
        //*/

        //*/ freeme.zhiwei.zhang, 20160818. NavigationBar Show/Hide.
        if (!com.droi.feature.FeatureOption.FREEME_NAVIGATIONBAR_MIN) {
            Preference preference = findPreference("navigationbar_preference");
            if (preference != null) {
                getPreferenceScreen().removePreference(preference);
            }
        }
        //*/
        //*/ freeme.zhiwei.zhang, 20160909. Smart wake up.
        if (!com.droi.feature.FeatureOption.FREEME_SCREEN_DOUBLETAP_WAKEUP_SUPPORT &&
                !com.droi.feature.FeatureOption.FREEME_HOME_DOUBLETAP_WAKEUP_SUPPORT) {
            Preference preference = findPreference("smart_wakeup_preference");
            if (preference != null) {
                getPreferenceScreen().removePreference(preference);
            }
        }
        //*/

        //*/ freeme. xiaocui,20160622. add floattask switch
        if (!FeatureOption.FREEME_FLOATTASK_SUPPORT) {
        	Preference preference = findPreference("toggle_floattask_preference");
        	if (preference != null) {
        		getPreferenceScreen().removePreference(preference);
        	}
        }
        //*/       

        //*/ freeme.xupeng, 20170614. add p-sensor control
        if (!com.droi.feature.FeatureOption.FREEME_HW_SENSOR_PROXIMITY) {
            if (mButtonPocketModeType != null)
                getPreferenceScreen().removePreference(mButtonPocketModeType);
            if (mScreenOnProximitySensor != null)
                getPreferenceScreen().removePreference(mScreenOnProximitySensor);
        }
        if (com.droi.feature.FeatureOption.FREEME_XLJ_VERSION) {
            Preference intelligentPref = findPreference("key_intelligent_operate");
            if (intelligentPref != null) {
                getPreferenceScreen().removePreference(intelligentPref);
            }
        }
        //*/

        //*/Droi.zhangweinan, 20161201. for add tts option
        mButtonTtsPreference = (SwitchPreference)findPreference(TTS_KEY);
        if (mButtonTtsPreference != null) {
            if (!"1".equals(SystemProperties.get("ro.tts_support"))) {
                getPreferenceScreen().removePreference(mButtonTtsPreference);
            }
            if (Settings.System.getInt(this.getContentResolver(),
                    Settings.System.FREEME_TTS_KEY, 0) == 1) {
                mButtonTtsPreference.setChecked(true);
            } else {
                mButtonTtsPreference.setChecked(false);
            }
            mButtonTtsPreference.setOnPreferenceChangeListener(this);
        }
        //*/

	}
	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
	
	 //*/add by tyd cch 20131029 for pocket mode
        if(preference == mButtonPocketModeType){
        	if(Settings.System.getInt(getContentResolver(),
                    Settings.System.FREEME_POCKET_MODE_KEY, 0) == 1){
        		    mButtonPocketModeType.setChecked(false);
                    Settings.System.putInt(getContentResolver(), Settings.System.FREEME_POCKET_MODE_KEY, 0);
                } else {
                	mButtonPocketModeType.setChecked(true);
                    Settings.System.putInt(getContentResolver(), Settings.System.FREEME_POCKET_MODE_KEY, 1);
                }
        }
        
      //*/ tyd.biantao 20131105. tp glove mode.
        if (preference == mTpGloveModePreference) {
            Boolean booleanValue = (Boolean) newValue;
            try {
                mNativeMiscService.devStateCtrl(INativeMiscService.DSC_DEV_TP_GLOV,
                        INativeMiscService.DSC_OPR_CONFIG,
                        booleanValue ? INativeMiscService.DSC_MODE_ENABLE : INativeMiscService.DSC_MODE_DISABLE);
            } catch (RemoteException e) {
                android.util.Log.e(TAG, e.toString());
            }
            return true;
        }
        
      //add by huangyiquan 20140317 for touch-protect mode
        if (preference == mScreenOnProximitySensor) {
            Boolean booleanValue = (Boolean) newValue;
            try {
                Settings.System.putInt(getActivity().getContentResolver(), SCREEN_ON_PROXIMITY_SENSOR, booleanValue ? 1 : 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }//end by huangyiquan

        //*/ Added by Linguanrong for supershot, 2015-6-15
        if(preference == mSuperShotPreference) {
            Boolean booleanValue = (Boolean) newValue;
            try {
                Settings.System.putInt(getContentResolver(), 
                        Settings.System.SUPERSHOT_MODE_DEFAULT, booleanValue ? 1 : 0);
                mSuperShotPreference.setChecked(booleanValue);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //*/

        //*/ freeme.huzhongtao 20161208 for suspension
        if(preference == mSuspensionPreference) {
            Boolean booleanValue = (Boolean) newValue;
            try {
                Settings.System.putInt(getContentResolver(), 
                        Settings.System.SUSPENSION_MODE_DEFAULT, booleanValue ? 1 : 0);
                mSuspensionPreference.setChecked(booleanValue);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Intent intent = new Intent("com.freeme.intent.action.SUSPENSION_TOGGLE");
            getActivity().getApplicationContext().sendBroadcast(intent);
        }
        //*/

        //*/Droi.zhangweinan, 20161201. for add tts option
        if(preference == mButtonTtsPreference){
            if(Settings.System.getInt(getContentResolver(),
                    Settings.System.FREEME_TTS_KEY, 0) == 1){
                mButtonTtsPreference.setChecked(false);
                Settings.System.putInt(getContentResolver(), Settings.System.FREEME_TTS_KEY, 0);
            } else {
                mButtonTtsPreference.setChecked(true);
                Settings.System.putInt(getContentResolver(), Settings.System.FREEME_TTS_KEY, 1);
            }
        }
        //*/

        return false;
	}
	
	public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                //*/Add by Tyd Jiangshouting 2015.11.19 for Setting search function lacking some data.
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                    List<SearchIndexableRaw> indexables = new ArrayList<SearchIndexableRaw>();
        
                    PackageManager packageManager = context.getPackageManager();
                    AccessibilityManager accessibilityManager = (AccessibilityManager)
                            context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        
                    String screenTitle = context.getResources().getString(
                            R.string.accessibility_settings_ext_title);
        
                    // Indexing all services, regardless if enabled.
                    List<AccessibilityServiceInfo> services = accessibilityManager
                            .getInstalledAccessibilityServiceList();
                    final int serviceCount = services.size();
                    for (int i = 0; i < serviceCount; i++) {
                        AccessibilityServiceInfo service = services.get(i);
                        if (service == null || service.getResolveInfo() == null) {
                            continue;
                        }
        
                        ServiceInfo serviceInfo = service.getResolveInfo().serviceInfo;
                        ComponentName componentName = new ComponentName(serviceInfo.packageName,
                                serviceInfo.name);
        
                        SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                        indexable.key = componentName.flattenToString();
                        indexable.title = service.getResolveInfo().loadLabel(packageManager).toString();
                        indexable.screenTitle = screenTitle;
                        indexables.add(indexable);
                    }
        
                    return indexables;
                }
                //*/
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.accessibility_settings_ext;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    if (!FeatureOption.FREEME_NON_TOUCH_OPERATION_SUPPORT) {
                        result.add("key_non_touch_settings");
                    }
                    return result;
                }
            };

    //*/Added by Jiangshouting 2016.01.04 for setting code transplant
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.ACCESSIBILITYEXT;
    }
    //*/

    //*/ freeme.zhaozehong, 20160926. show Settings
    @Override
    public void onResume() {
        initializeAllPreferences();
        super.onResume();
    }
    //*/
}

