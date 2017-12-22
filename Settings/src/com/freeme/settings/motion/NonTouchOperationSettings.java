package com.freeme.settings.motion;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;

public class NonTouchOperationSettings extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "NonTouchOperation";

    private Preference[] mGesFuncPointPrefs;

    private static final String KEY_MOTION_PRIVATE_CATEGORY = "private_mode_key";
    private PreferenceCategory mMotionPrivateCategory;
    
    private static final String KEY_MOTION_GENERIC_SETTING = "motion_gesture_generic_setting";
    private SwitchPreference mMotionGenericSetting;

    private static final String KEY_MOTION_UNLOCK_SETTING = "motion_unlock_setting";
    private SwitchPreference mMotionUnlockSetting;

    private static final String KEY_MOTION_EBOOK_SETTING = "motion_ebook_setting";
    private SwitchPreference mMotionEBookSetting;

    private static final String KEY_MOTION_GALIMG_SETTING = "motion_galimg_setting";
    private SwitchPreference mMotionGalImgSetting;

    private static final String KEY_MOTION_LNCRSLIDE_SETTING = "motion_lncrslide_setting";
    private SwitchPreference mMotionLncrSetting;

    //*/Added by tyd YuanChengye 20131101, for motion video setting
    private static final String KEY_MOTION_VIDEO_SETTING = "motion_video_setting";
    private SwitchPreference mMotionVideoSetting;
    //*/

    //*/Added by tyd linliangliang 20131104, for motion music setting
    private static final String KEY_MOTION_MUSIC_SETTING = "motion_music_setting";
    private SwitchPreference mMotionMusicSetting;
    //*/

    //*/Added by tyd jlwang 20140823, for motion phone setting
    private static final String KEY_MOTION_PHONE_SETTING = "motion_phone_setting";
    private SwitchPreference mMotionPhoneSetting;
    private static final String MOTION_PHONE_SETTING_CHANGE="motion_phone_setting_change";
    private ListPreference mMotionPhoneSettingChange;
    //*/
    
    private SwitchPreference mOnOff;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.non_touch_operation);
        
        mOnOff = (SwitchPreference) findPreference("motion_on_off");
        if (mOnOff.isChecked()) {
            mOnOff.setTitle(mOnOff.getSwitchTextOn());
        } else {
        	mOnOff.setTitle(mOnOff.getSwitchTextOff());
        }
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);

        /// Find Views.
        mMotionPrivateCategory = (PreferenceCategory) findPreference(KEY_MOTION_PRIVATE_CATEGORY);
        // for central control
        mMotionGenericSetting = (SwitchPreference) findPreference(KEY_MOTION_GENERIC_SETTING);
        mMotionUnlockSetting = (SwitchPreference) findPreference(KEY_MOTION_UNLOCK_SETTING);
        mMotionGalImgSetting = (SwitchPreference) findPreference(KEY_MOTION_GALIMG_SETTING);
        mMotionLncrSetting = (SwitchPreference) findPreference(KEY_MOTION_LNCRSLIDE_SETTING);
        mMotionEBookSetting = (SwitchPreference) findPreference(KEY_MOTION_EBOOK_SETTING);
        //*/Added by tyd YuanChengye 20131101, for motion video setting
        mMotionVideoSetting = (SwitchPreference) findPreference(KEY_MOTION_VIDEO_SETTING);
        //*/
        //*/Added by tyd linliangliang 20131104, for motion music setting
        mMotionMusicSetting = (SwitchPreference) findPreference(KEY_MOTION_MUSIC_SETTING);
        //*/
        //*/Added by tyd jlwang 20140823, for motion phone setting
        mMotionPhoneSetting = (SwitchPreference) findPreference(KEY_MOTION_PHONE_SETTING);
        mMotionPhoneSettingChange = (ListPreference) findPreference(MOTION_PHONE_SETTING_CHANGE);
        //*/

        mGesFuncPointPrefs = new Preference[] {
                mMotionUnlockSetting,
                mMotionGalImgSetting,
                mMotionLncrSetting,
                mMotionVideoSetting,
                mMotionMusicSetting,
                mMotionPhoneSetting,
                mMotionPhoneSettingChange
        };

        /// Set Checkbox checked or.
        final ContentResolver cr = getContentResolver();
        
        mMotionUnlockSetting.setChecked(Settings.System.getBoolbit(cr, 
                Settings.System.FREEME_GESTURE_SETS,
                Settings.System.FREEME_GESTURE_LOCKSCR_UNLOCK | Settings.System.FREEME_GESTURE_SETS_ENABLE,
                false));
        mMotionGalImgSetting.setChecked(Settings.System.getBoolbit(cr, 
                Settings.System.FREEME_GESTURE_SETS,
                Settings.System.FREEME_GESTURE_GALLERY_SLIDE | Settings.System.FREEME_GESTURE_SETS_ENABLE,
                false));
        mMotionLncrSetting.setChecked(Settings.System.getBoolbit(cr, 
                Settings.System.FREEME_GESTURE_SETS,
                Settings.System.FREEME_GESTURE_LAUNCHER_SLIDE | Settings.System.FREEME_GESTURE_SETS_ENABLE,
                false));

        // ignore
        mMotionEBookSetting.setChecked(Settings.System.getInt(cr, "tyd_ebook_setting", 0) != 0);

        //*/Added by tyd YuanChengye 20131101, for motion video setting
        mMotionVideoSetting.setChecked(Settings.System.getBoolbit(cr, 
                Settings.System.FREEME_GESTURE_SETS,
                Settings.System.FREEME_GESTURE_VIDEO_CONTROL | Settings.System.FREEME_GESTURE_SETS_ENABLE,
                false));
        //*/
        //*/Added by tyd linliangliang 20131104, for motion music setting
        mMotionMusicSetting.setChecked(Settings.System.getBoolbit(cr, 
                Settings.System.FREEME_GESTURE_SETS,
                Settings.System.FREEME_GESTURE_MUSIC_CONTROL | Settings.System.FREEME_GESTURE_SETS_ENABLE,
                false));
        //*/

        //*/Added by tyd jlwang 20140823, for motion phone setting
        mMotionPhoneSetting.setChecked(Settings.System.getBoolbit(cr, 
                Settings.System.FREEME_GESTURE_SETS,
                Settings.System.FREEME_GESTURE_PHONE_CONTROL | Settings.System.FREEME_GESTURE_SETS_ENABLE,
                false));

        String value = Settings.System.getString(cr,
                Settings.System.FREEME_GESTURE_PHONE_CONTROL_VALUE_SETTING);
        if (value == null) value = "1";
        mMotionPhoneSettingChange.setValue(value);
        mMotionPhoneSettingChange.setOnPreferenceChangeListener(this);
        mMotionPhoneSettingChange.setSummary(mMotionPhoneSettingChange.getEntry());
        mMotionPhoneSettingChange.setEnabled(mMotionPhoneSetting.isChecked());
        //*/

        /// check to disable
        gesGenericModeForceCheck();

        /// Set PreferenceChangeListener.
        mMotionGenericSetting.setOnPreferenceChangeListener(this);

        mMotionUnlockSetting.setOnPreferenceChangeListener(this);
        mMotionGalImgSetting.setOnPreferenceChangeListener(this);
        mMotionLncrSetting.setOnPreferenceChangeListener(this);
        mMotionEBookSetting.setOnPreferenceChangeListener(this);
        //*/Added by tyd YuanChengye 20131101, for motion video setting
        mMotionVideoSetting.setOnPreferenceChangeListener(this);
        //*/
        //*/Added by tyd linliangliang 20131104, for motion music setting
        mMotionMusicSetting.setOnPreferenceChangeListener(this);
        //*/
        //*/Added by tyd jlwang 20140823, for motion phone setting
        mMotionPhoneSetting.setOnPreferenceChangeListener(this);
        //*/

        /// Remove Views.
        // remove ebook setting
        mMotionPrivateCategory.removePreference(mMotionEBookSetting);

        // Remove generic mode
        getPreferenceScreen().removePreference(findPreference("generic_mode_key"));

        // remove mMotionLncrSetting
        mMotionPrivateCategory.removePreference(mMotionLncrSetting);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // do nothing if called as a result of a state machine event
        if (mStateMachingEvent) {
            return true;
        }
        Log.d(TAG, "non-touch operation onCheckedChanged " + preference.getKey());
        
        //*/Added by tyd jlwang 20140823, for motion phone setting change
        if (preference == mMotionPhoneSettingChange){
            if(newValue.toString().equals("1")){
                Settings.System.putString(getContentResolver(), Settings.System.FREEME_GESTURE_PHONE_CONTROL_VALUE_SETTING, "1");
               mMotionPhoneSettingChange.setValue("1");
            }else if(newValue.toString().equals("2")){
                Settings.System.putString(getContentResolver(), Settings.System.FREEME_GESTURE_PHONE_CONTROL_VALUE_SETTING, "2");//SHAKE_OPEN_APP_VALUE_SETTING
               mMotionPhoneSettingChange.setValue("2");
            }

            mMotionPhoneSettingChange.setSummary(mMotionPhoneSettingChange.getEntry());
            return true;
        }

        return true;
        //*/
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Log.d(TAG, "non-touch operation onPreferenceTreeClick " + preference.getKey());
        
        final ContentResolver cr = getContentResolver();
        
        if (preference == mMotionGenericSetting) {
            if (!handleClickEvent(mMotionGenericSetting,
                    R.string.learn_touch_control_generic_title,
                    R.string.learn_touch_control_generic_msg,
                    R.anim.anim_learn_mr_unlock)) {
                return false;
            }
            gesGenericModeForceToggle(mMotionGenericSetting.isChecked());
        } else if (preference == mOnOff) {
            mOnOff.setTitle(mOnOff.isChecked() ? mOnOff.getSwitchTextOn() : mOnOff.getSwitchTextOff());
            gesGenericModeForceToggle(!mOnOff.isChecked());
        } else if (preference == mMotionUnlockSetting)  {
            if (!handleClickEvent(mMotionUnlockSetting, 
                    R.string.learn_unlock_title,
                    R.string.learn_unlock_msg,
                    R.anim.anim_learn_mr_unlock)) {
                return false;
            }
            Settings.System.putBoolbit(cr, Settings.System.FREEME_GESTURE_SETS, Settings.System.FREEME_GESTURE_LOCKSCR_UNLOCK, mMotionUnlockSetting.isChecked());
        } else if (preference == mMotionGalImgSetting) {
            if (!handleClickEvent(mMotionGalImgSetting, 
                    R.string.learn_touch_control_gallery_title,
                    R.string.learn_touch_control_gallery_msg,
                    R.anim.anim_learn_standby_slide)) {
                return false;
            }
            Settings.System.putBoolbit(cr, Settings.System.FREEME_GESTURE_SETS, Settings.System.FREEME_GESTURE_GALLERY_SLIDE, mMotionGalImgSetting.isChecked());
        } else if (preference == mMotionLncrSetting) {
            if (!handleClickEvent(mMotionLncrSetting, 
                    R.string.learn_touch_control_standby_interface_title,
                    R.string.learn_touch_control_standby_interface_msg,
                    R.anim.anim_learn_standby_slide)) {
                return false;
            }
            Settings.System.putBoolbit(cr, Settings.System.FREEME_GESTURE_SETS, Settings.System.FREEME_GESTURE_LAUNCHER_SLIDE, mMotionLncrSetting.isChecked());
        } else if (preference == mMotionEBookSetting) {
            if (!handleClickEvent(mMotionEBookSetting, 
                    R.string.learn_touch_control_electronic_book_title,
                    R.string.learn_touch_control_electronic_book_msg,
                    R.anim.anim_learn_mr_ebook)) {
                return false;
            }
            Settings.System.putInt(cr, "tyd_ebook_setting", mMotionEBookSetting.isChecked() ? 1 : 0);
        //*/ freeme.YuanChengye, 20131101. motion video setting
        } else if (preference == mMotionVideoSetting) {
            if (!handleClickEvent(mMotionVideoSetting, 
                    R.string.learn_touch_control_video_title,
                    R.string.learn_touch_control_video_msg,
                    R.anim.anim_learn_standby_slide)) {
                return false;
            }
            Settings.System.putBoolbit(cr, Settings.System.FREEME_GESTURE_SETS, Settings.System.FREEME_GESTURE_VIDEO_CONTROL, mMotionVideoSetting.isChecked());
        //*/
        //*/ freeme.linliangliang, 20131104. motion music setting
        } else if (preference == mMotionMusicSetting) {
            if (!handleClickEvent(mMotionMusicSetting, 
                    R.string.learn_touch_control_music_title,
                    R.string.learn_touch_control_music_msg,
                    R.anim.anim_learn_standby_slide)) {
                return false;
            }
            Settings.System.putBoolbit(cr, Settings.System.FREEME_GESTURE_SETS, Settings.System.FREEME_GESTURE_MUSIC_CONTROL, mMotionMusicSetting.isChecked());
        //*/
        //*/ freeme.jlwang, 20140823. motion phone setting
        } else if (preference == mMotionPhoneSetting) {
            if (!handleClickEvent(mMotionPhoneSetting, 
                    R.string.learn_touch_control_phone_title,
                    R.string.learn_touch_control_phone_msg,
                    R.anim.anim_learn_standby_slide)) {
                return false;
            }
            mMotionPhoneSettingChange.setEnabled(mMotionPhoneSetting.isChecked());
            Settings.System.putBoolbit(cr, Settings.System.FREEME_GESTURE_SETS, Settings.System.FREEME_GESTURE_PHONE_CONTROL, mMotionPhoneSetting.isChecked());
        //*/
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private boolean mStateMachingEvent;

    private void setTitleSwitchChecked(boolean checked) {
        if (checked != mOnOff.isChecked()) {
            mStateMachingEvent = true;
            mOnOff.setChecked(checked);
            mStateMachingEvent = false;
        }
    }

    private void setGesFuncPointPrefsEnabled(boolean enabled) {
        for (Preference pref : mGesFuncPointPrefs) {
            if (pref != null){
                if (pref != mMotionPhoneSettingChange || mMotionPhoneSetting.isChecked()){
                    pref.setEnabled(enabled);
                }else{
                    mMotionPhoneSettingChange.setEnabled(false);
                }
            }
        }
    }
    private void gesGenericModeForceCheck() {
        boolean gesPrvModeChecked = Settings.System.getBoolbit(
                getContentResolver(),
                Settings.System.FREEME_GESTURE_SETS,
                Settings.System.FREEME_GESTURE_SETS_ENABLE,
                false);

        Log.d(TAG, "gesGenericModeForceCheck(), gesPrvModeChecked = " + gesPrvModeChecked);
        setTitleSwitchChecked(gesPrvModeChecked);
        setGesFuncPointPrefsEnabled(gesPrvModeChecked);
    }

    private void gesGenericModeForceToggle(boolean enabled) {
        Settings.System.putBoolbit(getContentResolver(),
                Settings.System.FREEME_GESTURE_SETS,
                Settings.System.FREEME_GESTURE_SETS_ENABLE, !enabled);
        setGesFuncPointPrefsEnabled(!enabled);
    }

    private boolean callNotice() {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private boolean handleClickEvent(final SwitchPreference preference,
            int contentTitle, int contentTextId, int contentAnimId) {
        if (!preference.isChecked()) return true;
        if (callNotice()) return false;
        
        preference.setEnabled(false);
        
        View content = LayoutInflater.from(this).inflate(R.layout.learn_mr_dialog, null);
        TextView contentText = (TextView) content.findViewById(R.id.learn_motion_recognition_tip);
        contentText.setText(contentTextId);
        ImageView contentImage = (ImageView) content.findViewById(R.id.learn_motion_recognition_img);
        contentImage.setBackgroundResource(contentAnimId);

        new AlertDialog.Builder(this)
            .setView(content)
            .setTitle(contentTitle)
            .setNegativeButton(R.string.mr_cancel, null)
            .show();
        
        AnimationDrawable anim = (AnimationDrawable) contentImage.getBackground();
        if (anim != null) {
            anim.stop();
            anim.start();
        }
        
        contentText.postDelayed(new Runnable() {
            @Override
            public void run() {
                preference.setEnabled(true);
            }
        }, 500);
        
        return true;
    }
}
