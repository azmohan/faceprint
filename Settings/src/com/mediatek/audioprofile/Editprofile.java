/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.audioprofile;

import android.app.Activity;
import android.content.ContentQueryMap;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
//import android.net.sip.SipManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.mediatek.audioprofile.AudioProfileManager.Scenario;
//import com.mediatek.gemini.GeminiUtils;
import com.mediatek.settings.FeatureOption;

import java.util.Observable;
import java.util.Observer;

import com.mediatek.audioprofile.DefaultRingtonePreference;
import com.mediatek.audioprofile.DefaultPreference;

//*/freeme xiaocui, 20160526. 6.0 style audioprofile
import java.util.HashMap;
import android.preference.ListPreference;
import com.mediatek.common.audioprofile.AudioProfileListener;
import android.media.AudioAttributes;
import android.os.Vibrator;
 //*/

//*/ freeme.chenming, 20161014. Default vibrator magnitude.
import android.content.SharedPreferences;
import com.freeme.settings.FreemeSeekBarDialogPreference;
//*/

/**
 * Edit profile fragment, started when one profile setting button is clicked.
 */
public class Editprofile extends SettingsPreferenceFragment {
    public static final String KEY_VIBRATE = "phone_vibrate";
    //*/freeme xiaocui, 20160526. 6.0 style audioprofile
    public static final String KEY_VOLUME = "ring_volume";
    //*/
    public static final String KEY_MEDIA_VOLUME = "media_volume";
    public static final String KEY_ALARM_VOLUME = "alarm_volume";
    public static final String KEY_RING_VOLUME = "ring_volume";
    private static final String KEY_NOTIFICATION_VOLUME = "notification_volume";
    public static final String KEY_RINGTONE = "phone_ringtone";
    public static final String KEY_VIDEO_RINGTONE = "video_call_ringtone";
    public static final String KEY_SIP_RINGTONE = "sip_call_ringtone";
    public static final String KEY_NOTIFY = "notifications_ringtone";
    //*/ freeme. xiaocui,20160622. add enhancement switch
    public static final String KEY_SOUND_ENHANCEMENT = "sound_enhancement";
    //*/
    //*/freeme xiaocui, 20160526. Message Ringtone
    public final static String KEY_MESSAGE = "messages_ringtone";
    //*/
    
    //*/freeme xiaocui, 20160526. 6.0 style audioprofile
    public final static String KEY_RING_MODE="ringtonemode";
    ListPreference mRingtonemode;
    private String mDefaultKey;
    private String[] modeKey  =  {"mtk_audioprofile_general","mtk_audioprofile_meeting","mtk_audioprofile_silent"};
    private CharSequence[] modeValue;
    private  CharSequence[] modeSummaries;   
    //*/
    public static final String KEY_DTMF_TONE = "audible_touch_tones";
    public static final String KEY_SOUND_EFFECTS = "audible_selection";
    public static final String KEY_LOCK_SOUNDS = "screen_lock_sounds";
    public static final String KEY_HAPTIC_FEEDBACK = "haptic_feedback";
    private static final String ACTION_SIM_SETTINGS = "com.android.settings.sim.SELECT_SUB";
    //*/ freeme.menglingqiang, 20160928. add boot and shutdown ringtone.
    public static final String KEY_BOOT_SHUTDOWN_RINGTONE = "boot_shutdown_ringtone";
    //*/

    private static final String TAG = "AudioProfile/EditProfile";

    private TwoStatePreference mVibrat;
    private TwoStatePreference mDtmfTone;
    private TwoStatePreference mSoundEffects;
    private TwoStatePreference mHapticFeedback;
    private TwoStatePreference mLockSounds;
    //*/ freeme.menglingqiang, 20160928. add boot and shutdown ringtone.
    private TwoStatePreference mBootShutdownRingtone;
    //*/

    //*/freeme xiaocui, 20160526. 6.0 style audioprofile
    private RingerVolumePreference mVolumePref;
    //*/
    //*/freeme xiaocui, 20160526. Custom Ringtone
    DefaultPreference mVoiceRingtone;
    DefaultPreference mVideoRingtone;
    DefaultPreference mSipRingtone;
    DefaultPreference mNotify;
    /*/
    private DefaultRingtonePreference mVoiceRingtone;
    private DefaultRingtonePreference mVideoRingtone;
    private DefaultRingtonePreference mSipRingtone;
    private DefaultRingtonePreference mNotify;

    //*/   


    private static final int SAMPLE_CUTOFF = 2000;  // manually cap sample playback at 2 seconds
    private final VolumePreferenceCallback mVolumeCallback = new VolumePreferenceCallback();
    private final H mHandler = new H();

    private Context mContext;
    private boolean mVoiceCapable;

    private boolean mIsSilentMode;
    private AudioProfileManager mProfileManager;

    private boolean mIsMeetingMode;

    private ContentQueryMap mContentQueryMap;

    private Observer mSettingsObserver;
    private String mKey;

    private long mSimId = -1;
    private int mCurOrientation;
    private TelephonyManager mTeleManager;
    private Cursor mSettingsCursor;
    private static final int REQUEST_CODE = 0;

    private String mSIMSelectorTitle;

    private static final int SINGLE_SIMCARD = 1;
    private int mSelectRingtongType = -1;

    public static final int RINGTONE_INDEX = 1;
    public static final int VIDEO_RINGTONE_INDEX = 2;
    public static final int SIP_RINGTONE_INDEX = 3;
    public static final int CONFIRM_FOR_SIM_SLOT_ID_REQUEST = 124;

    public VolumeSeekBarPreference mVolume;
    
    //*/freeme xiaocui, 20160526. 6.0 style audioprofile
    private static final String GENERAL_MODE="mtk_audioprofile_general";
    private final HashMap<String,  Boolean> mSwitchState = new HashMap<String,  Boolean>(
    		modeKey.length);
    private Vibrator mVibrator;
    private boolean mHasVibrator;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder()
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
    .build();
    private AudioManager mAudioManager;
    //*/
    
    private final ContentObserver mRingtoneObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            mHandler.sendEmptyMessage(H.RINGTONE_CHANGE);
        }
    };

    /**
     * If Silent Mode, remove all sound selections, include Volume, Ringtone,
     * Notifications, touch tones, sound effects, lock sounds. For Volume,
     * Ringtone and Notifications, need to set the profile's Scenario.
     *
     * @param icicle
     *            the bundle which passed if the fragment recreated
     */
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContext = getActivity();

        addPreferencesFromResource(R.xml.edit_profile_prefs);
        mTeleManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mVoiceCapable = Utils.isVoiceCapable(mContext);

        final SettingsActivity parentActivity = (SettingsActivity) getActivity();
        Bundle bundle = this.getArguments();

        //Xlog.d(TAG, "onCreate activity = " + parentActivity + ",bundle = " + bundle + ",this = " + this);
       
        //*/freeme xiaocui, 20160526. 6.0 style audioprofile
        mDefaultKey = AudioProfileManager.PROFILE_PREFIX
                + Scenario.GENERAL.toString().toLowerCase();
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mHasVibrator = mVibrator != null && mVibrator.hasVibrator();
        mAudioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
         if(bundle != null){
         mKey = bundle.getString("profileKey");
         }
        /*/
               mKey = bundle.getString("profileKey");
                       
        //*/



        mProfileManager = (AudioProfileManager) getSystemService(Context.AUDIO_PROFILE_SERVICE);
        //*/freeme xiaocui, 20160526. 6.0 style audioprofile
        mKey = mProfileManager.getActiveProfileKey();
        //Xlog.i("xiaocui33","Editprofie mKey =" +mKey + "mDefaultKey =" +mDefaultKey);
        modeSummaries =mContext. getResources().getTextArray(R.array.sound_mode_name);
       //*/
        Scenario scenario = AudioProfileManager.getScenario(mKey);

        mIsSilentMode = scenario.equals(Scenario.SILENT);
        mIsMeetingMode = scenario.equals(Scenario.MEETING);
        mSIMSelectorTitle = getActivity().getString(R.string.settings_label);

        initPreference();
    }

    /**
     * return true if the current device support sms service.
     *
     * @return
     */
    private boolean isSmsCapable() {
        return mTeleManager != null && mTeleManager.isSmsCapable();
    }

    /**
     * Register a contentObserve for CMCC load to detect whether vibrate in
     * silent profile
     */
    @Override
    public void onStart() {
        super.onStart();

        // Observer ringtone and notification changed
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.RINGTONE), false,
                mRingtoneObserver);

        // listen for vibrate_in_silent settings changes
        mSettingsCursor = getContentResolver().query(
                Settings.System.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[] { AudioProfileManager.getVibrationKey(mKey) }, null);
        mContentQueryMap = new ContentQueryMap(mSettingsCursor,
                Settings.System.NAME, true, null);
    }

    /**
     * stop sampling and revert the volume(no save)for RingerVolumePreference
     * when the fragment is paused.
     */
    @Override
    public void onPause() {
        super.onPause();
        Log.d("@M_" + TAG, "onPause");
        mVolumeCallback.stopSample();
        if (mSettingsObserver != null) {
            mContentQueryMap.deleteObserver(mSettingsObserver);
        }
    //*/freeme xiaocui, 20160526. 6.0 style audioprofile
        //Settings.System.putInt(getContentResolver(), "on_editprofile", 0);
        if (mVolumePref != null) {
            //Xlog.d(TAG, "pref is not null");
            mVolumePref.stopPlaying();
            mVolumePref.revertVolume();
        }
    //*/
        if (mSettingsCursor != null) {
            mSettingsCursor.close();
            mSettingsCursor = null;
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        mContext.getContentResolver().unregisterContentObserver(mRingtoneObserver);
     }

    /**
     * Init the preference and remove some in the silent profile.
     */
    private void initPreference() {
        PreferenceScreen parent = getPreferenceScreen();
        /*/freeme xiaocui, 20160526. 6.0 style audioprofile
        initVolume(parent);
        //*/
        initRingtoneAndNotification(parent);
        initSystemAudio();
        //*/ freeme.menglingqiang, 20160928. add boot and shutdown ringtone.
        if (com.droi.feature.FeatureOption.FREEME_BOOT_AUDIO_SUPPORT &&
                com.droi.feature.FeatureOption.FREEME_SHUT_AUDIO_SUPPORT){
            mBootShutdownRingtone.setTitle(R.string.freeme_boot_shut_audio_title);
        } else if (com.droi.feature.FeatureOption.FREEME_BOOT_AUDIO_SUPPORT ||
                com.droi.feature.FeatureOption.FREEME_SHUT_AUDIO_SUPPORT) {
            mBootShutdownRingtone.setTitle(R.string.freeme_boot_audio_title);
        } else {
            parent.removePreference(parent.findPreference(KEY_BOOT_SHUTDOWN_RINGTONE));
        }
        //*/
        //*/freeme xiaocui, 20160526. 6.0 style audioprofile
        mVolumePref = (RingerVolumePreference) findPreference(KEY_VOLUME);
        //*/

        //mVolumePref = (RingerVolumePreference) findPreference(KEY_VOLUME);

	
        //*/freeme xiaocui, 20160526. Custom Ringtone
        DefaultPreference notify = (DefaultPreference) findPreference(KEY_NOTIFY);
        /*/
        DefaultRingtonePreference notify = (DefaultRingtonePreference) parentNotify
                .findPreference(KEY_NOTIFY);
        //*/

        /*/freeme xiaocui, 20160526. 6.0 style audioprofile
        if (mIsSilentMode || mIsMeetingMode) {
            removePrefWhenSilentOrMeeting(parent);
            return;
        }
        //*/
        
        //*/freeme xiaocui, 20160526. 6.0 style audioprofile
        if (mVolumePref != null) {
            mVolumePref.setProfile(mKey);
        }
        initRingtoneMode(parent);
        //*/

        if (mVoiceCapable) {
            initVoiceCapablePref(parent);
        } else {
            initNoVoiceCapablePref(parent);
        }
        
        //*/ freeme. xiaocui,20160622. add enhancement switch
        if(!FeatureOption.MTK_BESLOUDNESS_SUPPORT && !FeatureOption.MTK_BESSURROUND_SUPPORT ){
            parent.removePreference(parent.findPreference(KEY_SOUND_ENHANCEMENT));
        }
        //*/

        //*/ freeme.chenming, 20161014. Default vibrator magnitude.
        if (com.droi.feature.FeatureOption.FREEME_VIBRATOR_TUNER_SUPPORT) {
            addPreferencesFromResource(R.xml.vibrator_tuner_dialog);
            initVibratorTunner();
        }
        //*/
    }

    private void initVolume(PreferenceScreen parent) {
        initVolumePreference(KEY_ALARM_VOLUME, AudioManager.STREAM_ALARM);
        if (mVoiceCapable) {
            mVolume = initVolumePreference(KEY_RING_VOLUME, AudioManager.STREAM_RING);
            parent.removePreference(parent.findPreference(KEY_NOTIFICATION_VOLUME));
        } else {
            mVolume = initVolumePreference(KEY_NOTIFICATION_VOLUME,
                    AudioManager.STREAM_NOTIFICATION);
            parent.removePreference(parent.findPreference(KEY_RING_VOLUME));
        }
    }

    private void initRingtoneAndNotification(PreferenceScreen parent) {
        initNotification(parent);
        initRingtone(parent);
        //*/freeme xiaocui, 20160526. SIM2 Ringtone
        initMessage(parent);
        //*/
    }
    
    
    /*/add by tyd_wangalei 2015.9.21 for Customs hobby sort
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
    	// TODO Auto-generated method stub
    	Log.i("mylog", "==key=="+preference.getKey());
    	if (KEY_RINGTONE.equals(preference.getKey())) {
            CustomHobbyService mService=new CustomHobbyService(this.getActivity());
            if(mService.isExistData(R.string.notification_settings, R.string.ringtone_title)){
    			mService.update(R.string.notification_settings, R.string.ringtone_title);
    		}else{
    			mService.insert(R.string.notification_settings, R.string.ringtone_title,"com.mediatek.audioprofile.RingtoneProfile", 1, "com.android.settings");
    		}
		}else if(KEY_MESSAGE.equals(preference.getKey())){
			  CustomHobbyService mService=new CustomHobbyService(this.getActivity());
	            if(mService.isExistData(R.string.notification_settings, R.string.zzzz_message_sound_title)){
	    			mService.update(R.string.notification_settings, R.string.zzzz_message_sound_title);
	    		}else{
	    			mService.insert(R.string.notification_settings, R.string.zzzz_message_sound_title,"com.mediatek.audioprofile.RingtoneProfile", 1, "com.android.settings");
	    		}
		}else if(KEY_NOTIFY.equals(preference.getKey())){
			  CustomHobbyService mService=new CustomHobbyService(this.getActivity());
	            if(mService.isExistData(R.string.notification_settings, R.string.notification_sound_title)){
	    			mService.update(R.string.notification_settings, R.string.notification_sound_title);
	    		}else{
	    			mService.insert(R.string.notification_settings, R.string.notification_sound_title,"com.mediatek.audioprofile.RingtoneProfile", 1, "com.android.settings");
	    		}
		}
    	return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    
    //*/

    private void initNotification(PreferenceScreen parent) {
        //*/freeme xiaocui, 20160526. Custom Ringtone
        mNotify = (DefaultPreference) parent.findPreference(KEY_NOTIFY);
        /*/
        mNotify = (DefaultRingtonePreference) parent.findPreference(KEY_NOTIFY);
        //*/
        if (mNotify != null) {
            mNotify.setStreamType(DefaultRingtonePreference.NOTIFICATION_TYPE);
            mNotify.setProfile(mKey);
            mNotify.setRingtoneType(AudioProfileManager.TYPE_NOTIFICATION);
            mNotify.setNoNeedSIMSelector(true);
	        //*/freeme xiaocui, 20160526. Custom Ringtone
            mNotify.setActivity(getActivity());
            //*/
        }
    }
 //*/freeme xiaocui, 20160526. Message Ringtone
    private void initMessage(PreferenceScreen parent) {
           DefaultPreference mMessage = (DefaultPreference) findPreference(KEY_MESSAGE);
	if(mMessage!=null){
        mMessage.setRingtoneType(AudioProfileManager.TYPE_MESSAGE);
	    mMessage.setStreamType(DefaultRingtonePreference.RING_TYPE);
	    mMessage.setProfile(mKey);
	    mMessage.setActivity(getActivity());
	}
    }

 //*/

    private void initRingtone(PreferenceScreen parent) {
        //*/freeme xiaocui, 20160526. Custom Ringtone
    	mVoiceRingtone = (DefaultPreference) parent.findPreference(KEY_RINGTONE);
    	mVideoRingtone = (DefaultPreference) parent.findPreference(KEY_VIDEO_RINGTONE);
        mSipRingtone = (DefaultPreference) parent.findPreference(KEY_SIP_RINGTONE);
    	/*/
        mVoiceRingtone = (DefaultRingtonePreference) parent.findPreference(KEY_RINGTONE);
        mVideoRingtone = (DefaultRingtonePreference) parent.findPreference(KEY_VIDEO_RINGTONE);
        mSipRingtone = (DefaultRingtonePreference) parent.findPreference(KEY_SIP_RINGTONE);
        //*/
    }
    
    //*/freeme xiaocui, 20160526. 6.0 style audioprofile
    private void initRingtoneMode(PreferenceScreen parent){
    	
        mProfileManager.listenAudioProfie(mListener,
                AudioProfileListener.LISTEN_PROFILE_CHANGE);
    	mRingtonemode = (ListPreference) parent.findPreference(KEY_RING_MODE);
    	modeValue = mRingtonemode.getEntryValues();
    	mRingtonemode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
            	CharSequence[] values = mRingtonemode.getEntryValues();
            	
            	for (int i=0; i<values.length; i++)
            	  {
            	   if (values[i].equals(newValue))
            	   {
             		   mProfileManager.setActiveProfile(modeKey[i]);
            		   mRingtonemode.setSummary(modeSummaries[i]);
            		   mKey = modeKey[i];
            		   updateSwitchState();
            		   messageOnVibrate();
                       //Xlog.i("xiaocui33 ","onPreferenceChange modeKey =" + modeKey[i] + "newValue =" +newValue);
            	    break;
            	   }
            	  }
                return true;
            }
        });
    }
    
    private final AudioProfileListener mListener = new AudioProfileListener() {
        @Override
        public void onProfileChanged(String profileKey) {
            super.onProfileChanged(profileKey);
   		   //mProfileManager.setActiveProfile(profileKey);
   		   mKey = profileKey;
   		   updateSwitchState();
   		  // Xlog.i("xiaocui33","Editprofie onProfileChanged  mKey =" +mKey);
   		   for(int i = 0;i < modeKey.length; i++){
   			   if(modeKey[i].equals(profileKey)){
   				   mRingtonemode.setSummary(modeSummaries[i]);
   	        	//int key = mProfileKey.get(mProfileManager.getActiveProfileKey());
   	        	//Xlog.i("xiaocui33","onPreferenceTreeClick mProfileManager.getActiveProfileKey() ="+mProfileManager.getActiveProfileKey() + "key =" +key);
   	        	  mRingtonemode.setValueIndex(i);
   			   }
   		   }
            }
        };

    //*/

    private void initSystemAudio() {
        mVibrat = (TwoStatePreference) findPreference(KEY_VIBRATE);
        mDtmfTone = (TwoStatePreference) findPreference(KEY_DTMF_TONE);
        mSoundEffects = (TwoStatePreference) findPreference(KEY_SOUND_EFFECTS);
        mLockSounds = (TwoStatePreference) findPreference(KEY_LOCK_SOUNDS);
        mHapticFeedback = (TwoStatePreference) findPreference(KEY_HAPTIC_FEEDBACK);
        //*/ freeme.menglingqiang, 20160928. add boot and shutdown ringtone.
        mBootShutdownRingtone = (TwoStatePreference) findPreference(KEY_BOOT_SHUTDOWN_RINGTONE);
        //*/
        setPreferenceListener(KEY_VIBRATE, mVibrat);
        setPreferenceListener(KEY_DTMF_TONE, mDtmfTone);
        setPreferenceListener(KEY_SOUND_EFFECTS, mSoundEffects);
        setPreferenceListener(KEY_LOCK_SOUNDS, mLockSounds);
        setPreferenceListener(KEY_HAPTIC_FEEDBACK, mHapticFeedback);
        //*/ freeme.menglingqiang, 20160928. add boot and shutdown ringtone.
        setPreferenceListener(KEY_BOOT_SHUTDOWN_RINGTONE, mBootShutdownRingtone);
        //*/
    }

    private void removePrefWhenSilentOrMeeting(PreferenceScreen parent) {
        parent.removePreference(mDtmfTone);
        parent.removePreference(mSoundEffects);
        parent.removePreference(mLockSounds);
        parent.removePreference(mVoiceRingtone);
        parent.removePreference(mVideoRingtone);
        parent.removePreference(mSipRingtone);
        parent.removePreference(mNotify);
        //*/freeme xiaocui, 20160526. media volume and system volume
        parent.removePreference(mVolumePref);
        //*/
        mVibrat.setEnabled(false);
    }

    private void initVoiceCapablePref(PreferenceScreen parent) {
        parent.removePreference(mVideoRingtone);
        mVoiceRingtone.setTitle(R.string.ringtone_title);

        parent.removePreference(mSipRingtone);

        if (mVoiceRingtone != null) {
            mVoiceRingtone
                    .setStreamType(DefaultRingtonePreference.RING_TYPE);
            mVoiceRingtone.setProfile(mKey);
            mVoiceRingtone
                    .setRingtoneType(AudioProfileManager.TYPE_RINGTONE);
      		//*/freeme xiaocui, 20160526. Custom Ringtone
                mVoiceRingtone.setActivity(getActivity());
    		//*/ 
            if (!FeatureOption.MTK_MULTISIM_RINGTONE_SUPPORT) {
                mVoiceRingtone.setNoNeedSIMSelector(true);
            }
        }

        if (mVideoRingtone != null) {
            mVideoRingtone
                    .setStreamType(DefaultRingtonePreference.RING_TYPE);
            mVideoRingtone.setProfile(mKey);
            mVideoRingtone
                    .setRingtoneType(AudioProfileManager.TYPE_VIDEO_CALL);
             //*/freeme xiaocui, 20160526. Custom Ringtone
                mVideoRingtone.setActivity(getActivity());
    		//*/  
            if (!FeatureOption.MTK_MULTISIM_RINGTONE_SUPPORT) {
                mVideoRingtone.setNoNeedSIMSelector(true);
            }
        }
    }

    private void initNoVoiceCapablePref(PreferenceScreen parent) {
        if (FeatureOption.MTK_PRODUCT_IS_TABLET) {
            mVibrat.setSummary(R.string.sms_vibrate_summary);
        }
        if (!isSmsCapable()) {
            parent.removePreference(mVibrat);
        }
        parent.removePreference(mDtmfTone);
        parent.removePreference(mVoiceRingtone);
        parent.removePreference(mVideoRingtone);
        parent.removePreference(mSipRingtone);
    }

    /**
     * Update the preference checked status from framework in onResume().
     */
    private void updatePreference() {
        mVibrat.setChecked(mProfileManager.isVibrationEnabled(mKey));
        mDtmfTone.setChecked(mProfileManager.isDtmfToneEnabled(mKey));
        mSoundEffects.setChecked(mProfileManager.isSoundEffectEnabled(mKey));
        mLockSounds.setChecked(mProfileManager.isLockScreenEnabled(mKey));
        mHapticFeedback.setChecked(mProfileManager
                .isVibrateOnTouchEnabled(mKey));
        //*/ freeme.menglingqiang, 20160928. add boot and shutdown ringtone.
        final boolean isBootOff = SystemProperties.get("persist.freeme.boot_audio_off").equals("1");
        final boolean isShutOff = SystemProperties.get("persist.freeme.shut_audio_off").equals("1");
        if (com.droi.feature.FeatureOption.FREEME_BOOT_AUDIO_SUPPORT &&
                com.droi.feature.FeatureOption.FREEME_SHUT_AUDIO_SUPPORT) {
            boolean isChecked = !(isBootOff && isShutOff);
            mBootShutdownRingtone.setChecked(isChecked);
        } else if (com.droi.feature.FeatureOption.FREEME_BOOT_AUDIO_SUPPORT ||
                com.droi.feature.FeatureOption.FREEME_SHUT_AUDIO_SUPPORT) {
            boolean isChecked = !((com.droi.feature.FeatureOption.FREEME_BOOT_AUDIO_SUPPORT && isBootOff) ||
                    (isShutOff && com.droi.feature.FeatureOption.FREEME_SHUT_AUDIO_SUPPORT));
            mBootShutdownRingtone.setChecked(isChecked);
        }
        //*/
        
      //*/freeme xiaocui, 20160526. 6.0 style audioprofile
       if( mIsSilentMode){
    	   mRingtonemode.setSummary(modeSummaries[2]);
       }else if(mIsMeetingMode){
    	   mRingtonemode.setSummary(modeSummaries[1]);  	   
       }else{
    	   mRingtonemode.setSummary(modeSummaries[0]);
       }
        //*/
    }

    /**
     * Update the preference checked status.
     */
    @Override
    public void onResume() {
        super.onResume();
        updatePreference();
        if (mIsSilentMode) {
            if (mSettingsObserver == null) {
                mSettingsObserver = new Observer() {
                    public void update(Observable o, Object arg) {
                        Log.d("@M_" + TAG, "update");
                        if (mVibrat != null) {
                            final String name = AudioProfileManager.getVibrationKey(mKey);
                            Log.d("@M_" + TAG, "name " + name);
                            String vibrateEnabled = Settings.System.getString(
                                    getContentResolver(), name);
                            if (vibrateEnabled != null) {
                                mVibrat.setChecked("true"
                                        .equals(vibrateEnabled));
                                Log.d("@M_" + TAG,
                                        "vibrate setting is "
                                                + "true".equals(vibrateEnabled));
                            }

                        }
                    }
                };
                mContentQueryMap.addObserver(mSettingsObserver);
            }
        }
    }

    // === Volumes ===

    private VolumeSeekBarPreference initVolumePreference(String key, int stream) {
        Log.d("@M_" + TAG, "Init volume preference, key = " + key + ",stream = " + stream);
        final VolumeSeekBarPreference volumePref = (VolumeSeekBarPreference) findPreference(key);
        volumePref.setStream(stream);
        volumePref.setCallback(mVolumeCallback);
        volumePref.setProfile(mKey);

        return volumePref;
    }

    /**
     * Volume preference callback class.
     */
    private final class VolumePreferenceCallback implements VolumeSeekBarPreference.Callback {
        private SeekBarVolumizer mCurrent;

        @Override
        public void onSampleStarting(SeekBarVolumizer sbv) {
            if (mCurrent != null && mCurrent != sbv) {
                mCurrent.stopSample();
            }
            mCurrent = sbv;
            if (mCurrent != null) {
                mHandler.removeMessages(H.STOP_SAMPLE);
                mHandler.sendEmptyMessageDelayed(H.STOP_SAMPLE, SAMPLE_CUTOFF);
            }
        }

        public void onStreamValueChanged(int stream, int progress) {
            if (stream == AudioManager.STREAM_RING) {
                mHandler.removeMessages(H.UPDATE_RINGER_ICON);
                mHandler.obtainMessage(H.UPDATE_RINGER_ICON, progress, 0).sendToTarget();
            }
        }

        public void stopSample() {
            if (mCurrent != null) {
                mCurrent.stopSample();
            }
        }

        public void ringtoneChanged() {
            if (mCurrent != null) {
                mCurrent.ringtoneChanged();
            /*/freeme.chenhanyuan. 2016-09-21. 
            } else {
            /*/
            } else if(mVolume != null){
            //*/
                mVolume.getSeekBar().ringtoneChanged();
            }
        }
    };

    /**
     * called when the preference is clicked.
     *
     * @param preferenceScreen
     *            the clicked preference which will be attached to
     * @param preference
     *            the clicked preference
     * @return true
     */
/*/freeme xiaocui, 20160526. Custom Ringtone
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        Log.d("@M_" + TAG, "Key :" + preference.getKey());
        if ((preference.getKey()).equals(KEY_RINGTONE)) {
            setRingtongTypeAndStartSIMSelector(RINGTONE_INDEX);
        } else if ((preference.getKey()).equals(KEY_VIDEO_RINGTONE)) {
            setRingtongTypeAndStartSIMSelector(VIDEO_RINGTONE_INDEX);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
//*/
    private void setPreferenceListener(final String preferenceType, Preference p) {
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setPreferenceChangeToDatabase((Boolean) newValue, preferenceType);
                return true;
            }
        });
    }

    private void setPreferenceChangeToDatabase(boolean isChecked, String preferenceType) {
        Log.d("@M_" + TAG, "Preference type :" + preferenceType);
        if (preferenceType.equals(KEY_VIBRATE)) {
            mProfileManager.setVibrationEnabled(mKey, isChecked);
        } else if (preferenceType.equals(KEY_DTMF_TONE)) {
            mProfileManager.setDtmfToneEnabled(mKey, isChecked);
        } else if (preferenceType.equals(KEY_SOUND_EFFECTS)) {
            mProfileManager.setSoundEffectEnabled(mKey, isChecked);
        } else if (preferenceType.equals(KEY_LOCK_SOUNDS)) {
            mProfileManager.setLockScreenEnabled(mKey, isChecked);
        } else if (preferenceType.equals(KEY_HAPTIC_FEEDBACK)) {
            mProfileManager.setVibrateOnTouchEnabled(mKey, isChecked);
        }
        //*/ freeme.menglingqiang, 20160928. add boot and shutdown ringtone.
        else if (preferenceType.equals(KEY_BOOT_SHUTDOWN_RINGTONE)) {
            if (com.droi.feature.FeatureOption.FREEME_BOOT_AUDIO_SUPPORT) {
                SystemProperties.set("persist.freeme.boot_audio_off", isChecked?"0":"1");
            }
            if (com.droi.feature.FeatureOption.FREEME_SHUT_AUDIO_SUPPORT) {
                SystemProperties.set("persist.freeme.shut_audio_off", isChecked?"0":"1");
            }
        }
        //*/
    }
/*/freeme xiaocui, 20160526. SIM2 Ringtone
    private void setRingtongTypeAndStartSIMSelector(int keyIndex) {
        Log.d("@M_" + TAG, "Selected ringtone type index = " + keyIndex);
        if (FeatureOption.MTK_MULTISIM_RINGTONE_SUPPORT) {
            //final int numSlots = mTeleManager.getSimCount();
            final int numSlots = SubscriptionManager.from(mContext)
                    .getActiveSubscriptionInfoCount();
            int simNum = numSlots;
            Log.d("@M_" + TAG, "simList.size() == " + simNum);

            if (simNum > SINGLE_SIMCARD) {
                mSelectRingtongType = keyIndex;
                setRingtoneType(keyIndex);
                startSIMCardSelectorActivity();
            }
        }
    }

    private void setRingtoneType(int keyIndex) {
        switch(keyIndex) {
            case RINGTONE_INDEX:
                mVoiceRingtone.setRingtoneType(AudioProfileManager.TYPE_RINGTONE);
                break;
            case VIDEO_RINGTONE_INDEX:
                mVideoRingtone.setRingtoneType(AudioProfileManager.TYPE_VIDEO_CALL);
                break;
            default:
                break;
        }
    }

    private void startSIMCardSelectorActivity() {
        Intent intent = new Intent();
        intent.setAction(ACTION_SIM_SETTINGS);
        startActivityForResult(intent, REQUEST_CODE);
    }
//*/

    /**
     * called when rotate the screen.
     *
     * @param newConfig
     *            the current new config
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d("@M_" + TAG, "onConfigurationChanged: newConfig = " + newConfig
                + ",mCurOrientation = " + mCurOrientation + ",this = " + this);
        super.onConfigurationChanged(newConfig);
        if (newConfig != null && newConfig.orientation != mCurOrientation) {
            mCurOrientation = newConfig.orientation;
        }
        this.getListView().clearScrapViewsIfNeeded();
    }
/*/freeme xiaocui, 20160526. SIM2 Ringtone
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("@M_" + TAG, "onActivityResult " + "requestCode " + requestCode + " "
                + resultCode + "resultCode");
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mSimId = data.getLongExtra(PhoneConstants.SUBSCRIPTION_KEY,
                                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                setRingtoneSIMId(mSimId);
            }
            Log.v("@M_" + TAG, "Select SIM id = " + mSimId);
        }
    }

    private void setRingtoneSIMId(long simId) {
        switch(mSelectRingtongType) {
            case RINGTONE_INDEX:
                mVoiceRingtone.setSimId(simId);
                mVoiceRingtone.simSelectorOnClick();
                break;
            case VIDEO_RINGTONE_INDEX:
                mVideoRingtone.setSimId(simId);
                mVideoRingtone.simSelectorOnClick();
                break;
            default:
                break;
        }
    }
//*/

    /**
     * Edit profile hanlder.
     */
    private final class H extends Handler {
        private static final int UPDATE_PHONE_RINGTONE = 1;
        private static final int UPDATE_NOTIFICATION_RINGTONE = 2;
        private static final int STOP_SAMPLE = 3;
        private static final int UPDATE_RINGER_ICON = 4;
        private static final int RINGTONE_CHANGE = 5;

        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STOP_SAMPLE:
                    mVolumeCallback.stopSample();
                    break;
                case RINGTONE_CHANGE:
                    Log.d("@M_" + TAG, "Ringtone changed.");
                    mVolumeCallback.ringtoneChanged();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected int getMetricsCategory() {
        return InstrumentedFragment.METRICS_AUDIOPROFILE;
    }

    
   //*/freeme xiaocui, 20160526. 6.0 style audioprofile
    private void setSwitchState(String mkey,boolean ischeck){
    	mSwitchState.put(mkey, ischeck);
    }
    
    private void updateSwitchState(){
    	
        Scenario scenario = AudioProfileManager.getScenario(mKey);
        mIsSilentMode = scenario.equals(Scenario.SILENT);
        mIsMeetingMode = scenario.equals(Scenario.MEETING);
        //*/freeme.luyangjie,20170710,fix bug about boot no Ringtone if It`s SilentMode or MeetingMode

        //*/freeme.zhangshaopiao,20170804,add defult "0"
        final boolean isBoot = SystemProperties.get("persist.freeme.boot_audio_off","0").equals("0");
        /*/
        final boolean isBoot = SystemProperties.get("persist.freeme.boot_audio_off").equals("0");
        // */

        //*/
    	//Xlog.i("xiaocui33","updateSwitchState mIsSilentMode = " + mIsSilentMode + "mIsMeetingMode = " + mIsMeetingMode + "mkey =" + mKey);
    	if(mVolumePref != null){
        	mVolumePref.setProfile(mKey);	
        	//Xlog.i("xiaocui33","updateSwitchState mKey =" +mKey);
    	}
    	if(mIsSilentMode){
        	setPreferenceChangeToDatabase(false, KEY_VIBRATE);
        	setPreferenceChangeToDatabase(false, KEY_DTMF_TONE);
        	setPreferenceChangeToDatabase(false, KEY_SOUND_EFFECTS);
        	setPreferenceChangeToDatabase(false, KEY_LOCK_SOUNDS);
        	setPreferenceChangeToDatabase(false, KEY_HAPTIC_FEEDBACK);

            mVibrat.setChecked(mProfileManager.isVibrationEnabled(mKey));
            mDtmfTone.setChecked(mProfileManager.isDtmfToneEnabled(mKey));
            mSoundEffects.setChecked(mProfileManager.isSoundEffectEnabled(mKey));
            mLockSounds.setChecked(mProfileManager.isLockScreenEnabled(mKey));
            mHapticFeedback.setChecked(mProfileManager
                    .isVibrateOnTouchEnabled(mKey));
            
            mVibrat.setEnabled(false);
            mDtmfTone.setEnabled(false);
            mSoundEffects.setEnabled(false);
            mLockSounds.setEnabled(false);
            mHapticFeedback.setEnabled(false);
            //*/freeme.luyangjie,20170710,fix bug about boot no Ringtone if It`s SilentMode or MeetingMode
            mBootShutdownRingtone.setChecked(false);
            mBootShutdownRingtone.setEnabled(false);
            //*/
    	}else if(mIsMeetingMode){
        	//setPreferenceChangeToDatabase(true, KEY_VIBRATE);
        	setPreferenceChangeToDatabase(false, KEY_DTMF_TONE);
        	setPreferenceChangeToDatabase(false, KEY_SOUND_EFFECTS);
        	setPreferenceChangeToDatabase(false, KEY_LOCK_SOUNDS);
        	//setPreferenceChangeToDatabase(true, KEY_HAPTIC_FEEDBACK);
        	
            mVibrat.setChecked(mProfileManager.isVibrationEnabled(mKey));
            mDtmfTone.setChecked(mProfileManager.isDtmfToneEnabled(mKey));
            mSoundEffects.setChecked(mProfileManager.isSoundEffectEnabled(mKey));
            mLockSounds.setChecked(mProfileManager.isLockScreenEnabled(mKey));
            mHapticFeedback.setChecked(mProfileManager
                    .isVibrateOnTouchEnabled(mKey));
            
            mVibrat.setEnabled(true);
            mDtmfTone.setEnabled(false);
            mSoundEffects.setEnabled(false);
            mLockSounds.setEnabled(false);
            mHapticFeedback.setEnabled(true);
            //*/freeme.luyangjie,20170710,fix bug about boot no Ringtone if It`s SilentMode or MeetingMode
            mBootShutdownRingtone.setChecked(false);
            mBootShutdownRingtone.setEnabled(false);
            //*/
    	}else{
            mVibrat.setChecked(mProfileManager.isVibrationEnabled(mKey));
            mDtmfTone.setChecked(mProfileManager.isDtmfToneEnabled(mKey));
            mSoundEffects.setChecked(mProfileManager.isSoundEffectEnabled(mKey));
            mLockSounds.setChecked(mProfileManager.isLockScreenEnabled(mKey));
            mHapticFeedback.setChecked(mProfileManager
                    .isVibrateOnTouchEnabled(mKey));
            
            mVibrat.setEnabled(true);
            mDtmfTone.setEnabled(true);
            mSoundEffects.setEnabled(true);
            mLockSounds.setEnabled(true);
            mHapticFeedback.setEnabled(true);
            //*/freeme.luyangjie,20170710,fix bug about boot no Ringtone if It`s SilentMode or MeetingMode
            mBootShutdownRingtone.setChecked(isBoot);
            mBootShutdownRingtone.setEnabled(true);
            //*/
    	}
    	
    }
    

 private static final int VIBRATE_HANDLER = 0;
 public static final int VIBRATE_DELAY = 300;
 private static final int VIBRATE_DURATION = 300;
 private final Handler vibrateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case VIBRATE_HANDLER:
                	onVibrate();
                	break;
            }
        }
    };
    
    protected void onVibrate() {
    	
        if (mAudioManager.getRingerModeInternal() != AudioManager.RINGER_MODE_VIBRATE) {
            return;
        }
        if (mVibrator != null && mHasVibrator) {
            mVibrator.vibrate(VIBRATE_DURATION, VIBRATION_ATTRIBUTES);
        }
    }
    
    public void messageOnVibrate(){
    	//Xlog.i("xiaocui33","messageOnVibrate");
    	vibrateHandler.removeMessages(VIBRATE_HANDLER);
    	vibrateHandler.sendMessageDelayed(vibrateHandler.obtainMessage(VIBRATE_HANDLER), VIBRATE_DELAY);
    }
   //*/

    //*/ freeme.chenming, 20161014. Default vibrator magnitude.
    private static final int UNDEFINED_PREFERENCE_VALUE = -1;
    private static final int VIBRATOR_MAGNITUDE_DEFAULT = 5;

    private String VIBRATOR_MAGNITUDE_SETTING = android.provider.Settings.System.FREEME_VIBRATOR_MAGNITUDE;
    private String VIBRATOR_MAGNITUDE_PREF = VIBRATOR_MAGNITUDE_SETTING;
    private String VIB_TUNNER_PRE = "pref_vibratortuner";

    private void initVibratorTunner() {
        int mdefVibratorMagValue = android.provider.Settings.System.getInt(
                mContext.getContentResolver(),
                VIBRATOR_MAGNITUDE_SETTING,
                VIBRATOR_MAGNITUDE_DEFAULT);

        final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        if (prefs.getInt(VIB_TUNNER_PRE, UNDEFINED_PREFERENCE_VALUE) == UNDEFINED_PREFERENCE_VALUE) {
            prefs.edit().remove(VIBRATOR_MAGNITUDE_PREF).apply();
            prefs.edit().putInt(VIBRATOR_MAGNITUDE_PREF, mdefVibratorMagValue).apply();
        }


        final FreemeSeekBarDialogPreference pref = (FreemeSeekBarDialogPreference)findPreference(VIB_TUNNER_PRE);
        pref.setInterface(new FreemeSeekBarDialogPreference.ValueProxy() {

            @Override
            public void writeValue(final int value, final String key) {
                android.provider.Settings.System.putInt(mContext.getContentResolver(), VIBRATOR_MAGNITUDE_SETTING, value);
                prefs.edit().putInt(key, value).apply();
            }

            @Override
            public void writeDefaultValue(final String key) {
                int def = prefs.getInt(VIBRATOR_MAGNITUDE_PREF, VIBRATOR_MAGNITUDE_DEFAULT);
                android.provider.Settings.System.putInt(mContext.getContentResolver(), VIBRATOR_MAGNITUDE_SETTING, def);

                prefs.edit().remove(key).apply();
            }

            @Override
            public int readValue(final String key) {
                int storeValue = prefs.getInt(key, UNDEFINED_PREFERENCE_VALUE);
                int def = prefs.getInt(VIBRATOR_MAGNITUDE_PREF, VIBRATOR_MAGNITUDE_DEFAULT);
                return (storeValue != UNDEFINED_PREFERENCE_VALUE ? storeValue : def);
            }

            @Override
            public int readDefaultValue(final String key) {
                return prefs.getInt(VIBRATOR_MAGNITUDE_PREF, VIBRATOR_MAGNITUDE_DEFAULT);
            }

            @Override
            public void feedbackValue(final int value) {
            }

            @Override
            public String getValueText(final int value) {
                return String.valueOf(value);
            }
        });
    }
    //*/

}
