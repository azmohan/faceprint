package com.android.settings.accessibility;

import android.preference.SwitchPreference;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.HobbyDB.CustomHobbyService;
import com.mediatek.settings.FeatureOption;
import com.android.settings.R;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;

//*/Add by Jiangshouting 2016.01.04 for setting code transplant
import com.android.internal.logging.MetricsLogger;
//*/

import android.widget.Toast;


public class IntelligentOperateSettings extends SettingsPreferenceFragment  implements OnPreferenceChangeListener{

    //*/Added by Linguanrong adjust setting menu, 2013-8-17
    private static final String BUTTON_OTHERS_REVERSE_SILENT_KEY = "reverse_silent_key";
    private SwitchPreference mButtonMute;//reverse mute
    
    //*/add by tyd john 20131029 for Smart dial
    private static final String BUTTON_SMART_DIAL_KEY = "smart_dial_key";
    private SwitchPreference mButtonSmartDialType;
    
    private static final String BUTTON_SMART_ANSWER_CALL_KEY = "smart_answer_key";
    private SwitchPreference mButtonSmartAnswerCallType;
    //*/
    
    /*/ removed by tyd liuchao, 2015-07-09
    private static final String SMART_CATEGORY = "smart_category";
    private PreferenceCategory mSmartCategory;
    //*/
    
    //*/Added by tyd sxp 20140716 for shake open app
    private static final String SHAKE_OPEN_APP_KEY="shake_open_app_key";
    private SwitchPreference mButtonShake;
    private static final String SHAKE_OPEN_APP_VALUE_KEY="shake_open_app_value_key";
    private ListPreference mButtonShakeApp;
    //*/end

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.intelligent_operate);
        initializeAllPreferences();
        //*/add by tyd_wangalei 2015.9.21 for Customs hobby sort
        CustomHobbyService mService=new CustomHobbyService(getActivity());
        if(mService.isExistData(R.string.accessibility_settings_ext_title, R.string.intelligent_operate)){
            mService.update(R.string.accessibility_settings_ext_title, R.string.intelligent_operate);
        }else{
            mService.insert(R.string.accessibility_settings_ext_title, R.string.intelligent_operate, this.getClass().getName(), 1, "");
        }
        //*/
    }

    private void initializeAllPreferences(){
        //mSmartCategory = (PreferenceCategory) findPreference(SMART_CATEGORY);
        //*/droi.duanzhiquan, 20161222,reverse dial silent option
        mButtonMute = (SwitchPreference) findPreference(BUTTON_OTHERS_REVERSE_SILENT_KEY);
        if (FeatureOption.FREEME_REVERSE_DIAL_SILENT) {
            if(Settings.System.getInt(this.getContentResolver(),
                    Settings.System.FREEME_REVERSE_SILENT_SETTING, 0) == 1){
                mButtonMute.setChecked(true);
            } else {
                mButtonMute.setChecked(false);
            }
            mButtonMute.setOnPreferenceChangeListener(this);
        } else {
            getPreferenceScreen().removePreference(mButtonMute);
        }
        //*/

        //*/add by tyd john 20140117 for smart dial and answer call
        mButtonSmartDialType = (SwitchPreference)findPreference(BUTTON_SMART_DIAL_KEY);
        if (!FeatureOption.FREEME_SMART_DIAL_ANSWER) {
            if (mButtonSmartDialType != null) {
                getPreferenceScreen().removePreference(mButtonSmartDialType);
            }
        }
        if (mButtonSmartDialType != null) {
            if (Settings.System.getInt(this.getContentResolver(),
                    Settings.System.FREEME_SMART_DIAL_KEY, 0) == 1) {
                mButtonSmartDialType.setChecked(true);
            } else {
                mButtonSmartDialType.setChecked(false);
            }
            mButtonSmartDialType.setOnPreferenceChangeListener(this);
        }

        mButtonSmartAnswerCallType = (SwitchPreference)findPreference(BUTTON_SMART_ANSWER_CALL_KEY);
        if (!FeatureOption.FREEME_SMART_DIAL_ANSWER) {
            if (mButtonSmartAnswerCallType != null) {
                getPreferenceScreen().removePreference(mButtonSmartAnswerCallType);
            }
        }
        if (mButtonSmartAnswerCallType != null) {
            if (Settings.System.getInt(this.getContentResolver(),
                    Settings.System.FREEME_SMART_ANSWER_KEY, 0) == 1) {
                mButtonSmartAnswerCallType.setChecked(true);
            } else {
                mButtonSmartAnswerCallType.setChecked(false);
            }
            mButtonSmartAnswerCallType.setOnPreferenceChangeListener(this);
        }
        //*/

        if(!FeatureOption.MTK_VOICE_UI_SUPPORT){
            Preference voiceUIPreference = getPreferenceScreen().findPreference("voice_ui");
            if(voiceUIPreference != null){
                getPreferenceScreen().removePreference(voiceUIPreference);
            }
        }

        //*/add by sxp 20140716 for shake open app
        mButtonShake = (SwitchPreference) findPreference(SHAKE_OPEN_APP_KEY);
        mButtonShakeApp = (ListPreference) findPreference(SHAKE_OPEN_APP_VALUE_KEY);
        if (!FeatureOption.TYD_SHAKE_OPEN_APP_SUPPORT) {
            if (mButtonShake != null)
                getPreferenceScreen().removePreference(mButtonShake);
            if (mButtonShakeApp != null)
                getPreferenceScreen().removePreference(mButtonShakeApp);
        } else {
            boolean enable_shake_open = Settings.System.getInt(getContentResolver(),
                    Settings.System.FREEME_SHAKE_OPEN_APP_SETTING, 0) == 1 ? true : false;
            mButtonShake.setChecked(enable_shake_open);
            mButtonShake.setOnPreferenceChangeListener(this);
            
            //*/ Added by tyd liuchao for hide Setting app preference when Shake to open app preference is unChecked
            if (enable_shake_open) {
                getPreferenceScreen().addPreference(mButtonShakeApp);
            } else {
                getPreferenceScreen().removePreference(mButtonShakeApp);
            }
            //*/

            String value;
            try {
                value = Settings.System.getString(getContentResolver(), Settings.System.FREEME_SHAKE_OPEN_APP_VALUE_SETTING);
            } catch (Exception e) {
                value = "1";
            }
            if (value == null) {
                value = "1";
            }
            if (value.equals("1")) {
                //*/add by chenhanyuan 20151201 for share open application
                Settings.System.putString(getContentResolver(), Settings.System.FREEME_SHAKE_OPEN_APP_VALUE_SETTING, "1");
                //*/
                mButtonShakeApp.setValue(value);
            } else if (value.equals("2")) {
                //*/add by chenhanyuan 20151201 for share open application
                Settings.System.putString(getContentResolver(), Settings.System.FREEME_SHAKE_OPEN_APP_VALUE_SETTING, "2");
                //*/
                mButtonShakeApp.setValue(value);
            }
            mButtonShakeApp.setOnPreferenceChangeListener(this);
            mButtonShakeApp.setSummary(mButtonShakeApp.getEntry());
        }
        //*/
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        //*/add by tyd john 20140117 for smart dial and answer call
        if(preference == mButtonSmartDialType){
            if(Settings.System.getInt(getContentResolver(),
                    Settings.System.FREEME_SMART_DIAL_KEY, 0) == 1){
                mButtonSmartDialType.setChecked(false);
                Settings.System.putInt(getContentResolver(), Settings.System.FREEME_SMART_DIAL_KEY, 0);
            } else {
                mButtonSmartDialType.setChecked(true);
                Settings.System.putInt(getContentResolver(), Settings.System.FREEME_SMART_DIAL_KEY, 1);
            }
        }else if(preference == mButtonSmartAnswerCallType){
            if(Settings.System.getInt(getContentResolver(),
                    Settings.System.FREEME_SMART_ANSWER_KEY, 0) == 1){
                mButtonSmartAnswerCallType.setChecked(false);
                Settings.System.putInt(getContentResolver(), Settings.System.FREEME_SMART_ANSWER_KEY, 0);
            } else {
                mButtonSmartAnswerCallType.setChecked(true);
                Settings.System.putInt(getContentResolver(), Settings.System.FREEME_SMART_ANSWER_KEY, 1);
                //*/freeme.gejun, 20160706 smart answer will not work when non-touch open
                Toast.makeText(getActivity(), R.string.smart_anser_tip, Toast.LENGTH_SHORT).show();
                //*/
            }
        }
        //*/
        //*/Added by Linguanrong adjust setting menu, 2013-8-17 start
        if(preference == mButtonMute) {
            if(Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.FREEME_REVERSE_SILENT_SETTING, 0) == 1){
                mButtonMute.setChecked(false);
                Settings.System.putInt(getActivity().getContentResolver(), Settings.System.FREEME_REVERSE_SILENT_SETTING, 0);
            } else {
                mButtonMute.setChecked(true);
                Settings.System.putInt(getActivity().getContentResolver(), Settings.System.FREEME_REVERSE_SILENT_SETTING, 1);
            }
            return true;
        }
        //Added by Linguanrong end*/
        //*/add by tyd sxp 20140416 for shake open app
        if (preference == mButtonShake) {
            Boolean value = (Boolean) newValue;
            try {
                Settings.System.putInt(getActivity().getContentResolver(), Settings.System.FREEME_SHAKE_OPEN_APP_SETTING,
                        value ? 1 : 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            //*/ Added by tyd liuchao for hide Setting app preference when Shake to open app preference is unChecked
            if (value) {
                getPreferenceScreen().addPreference(mButtonShakeApp);
            } else {
                getPreferenceScreen().removePreference(mButtonShakeApp);
            }
            //*/
            return true;
        }
        if (preference == mButtonShakeApp) {
            if (newValue.toString().equals("1")) {
                Settings.System.putString(getContentResolver(), Settings.System.FREEME_SHAKE_OPEN_APP_VALUE_SETTING, "1");
                mButtonShakeApp.setValue("1");
            } else if (newValue.toString().equals("2")) {
                Settings.System.putString(getContentResolver(), Settings.System.FREEME_SHAKE_OPEN_APP_VALUE_SETTING, "2");
                mButtonShakeApp.setValue("2");
            }

            mButtonShakeApp.setSummary(mButtonShakeApp.getEntry());
            return true;
        }
        //*/
        return false;
    }


    //*/Added by Jiangshouting 2016.01.04 for setting code transplant
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.INTELLIGENTOPERATESETTINGS;
    }
    //*/
}
