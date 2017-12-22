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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.MediaStore;
import android.util.Log;
import com.mediatek.audioprofile.DefaultRingtonePreference;
import com.android.internal.telephony.PhoneConstants;

import com.android.settings.R;
//import com.mediatek.audioprofile.AudioProfile;
///import com.mediatek.audioprofile.AudioProfileImpl;
import com.mediatek.audioprofile.AudioProfileManager;
//import com.mediatek.audioprofile.AudioProfileManagerImpl;
import com.mediatek.audioprofile.AudioProfileManager.Scenario;
import android.telephony.SubscriptionManager;

public class RingtoneProfile extends PreferenceActivity {

    public final static String KEY_DEFAULT = "ringtone_default";
    public final static String KEY_CUSTOM = "ringtone_custom";
    private static final String TAG = "RingtoneProfile";

    public DefaultRingtonePreference mDefault;
    public CustomRingtonePreference mCustom;

    private String mRingtone;
    private String defaultRingtone;
    private int ringtone_type;
    private Resources mResources;
    private ContentResolver mResolver;
    //private AudioProfileImpl mProfile;
    private Uri mUri;
    private AudioProfileManager mProfileManager;
    private int mSimId;
    private final static int SIM_ID_NONE= -1;
    private final static int SIM_ID_0 = 0;
    private final static int  SIM_ID_1 = 1;
    private String key ;
    //include default ringtone, custom ringtone, need to set the profile's scenario.
    public void onCreate(Bundle icicle){
        
        
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.ringtone_profile_prefs);
        Intent intent = getIntent();
        mProfileManager = (AudioProfileManager)this.getSystemService(Context.AUDIO_PROFILE_SERVICE);
        key = intent.getStringExtra("Profile");
        String call = intent.getStringExtra("Ringtone");
        mDefault = (DefaultRingtonePreference) findPreference(KEY_DEFAULT);
        //*/Add by tyd xiaocui 20120706
        mDefault.setStreamType(DefaultRingtonePreference.RING_TYPE);
        //*/
        mSimId = intent.getIntExtra(PhoneConstants.SLOT_KEY,
                -1);
        //mDefault.setSimId(mSimId);
        mDefault.setProfile(key);
        mDefault.setActivity(this);
        mCustom = (CustomRingtonePreference) findPreference(KEY_CUSTOM);
        mCustom.setProfile(key);
        mCustom.setActivity(this);
        mResources = getResources();
        mResolver = getContentResolver();
      //  mProfile = (AudioProfileImpl)AudioProfileManagerImpl.getInstance(this).getProfile(key);
        mRingtone = null;
        mUri = null;
        //Log.i("xiaocui","RingtoneProfile oncreate key ="+key + "call =" + call + "mSimId =" +mSimId + "isSingleSim =" + isSingleSim);
        String title = null;
        if (call.equals("voice")) {
        	if((mSimId == SIM_ID_0) ||(mSimId == SIM_ID_NONE) ){
                mDefault.setRingtoneType(RingtoneManager.TYPE_RINGTONE);
                mCustom.setRingtoneType(RingtoneManager.TYPE_RINGTONE);
                mUri = mProfileManager.getRingtoneUri(key, RingtoneManager.TYPE_RINGTONE);
            	//*/by tyd wangalei 2015.9.17 for ringtone bug
                ringtone_type=RingtoneManager.TYPE_RINGTONE;
                //*/
        	}else{
                mDefault.setRingtoneType(RingtoneManager.TYPE_RINGTONE_SIM2);
                mCustom.setRingtoneType(RingtoneManager.TYPE_RINGTONE_SIM2);
                mUri = mProfileManager.getRingtoneUri(key, RingtoneManager.TYPE_RINGTONE_SIM2);
            	//*/by tyd wangalei 2015.9.17 for ringtone bug
                ringtone_type=RingtoneManager.TYPE_RINGTONE_SIM2;
                //*/
        	}
        	  mRingtone = Settings.System.getString(mResolver, Settings.System.CUSTOM_RINGTONE);
        	//*/by tyd wangalei 2015.9.15 for ringtone bug
        	  defaultRingtone = Settings.System.getString(mResolver, RingtoneManager.KEY_DEFAULT_RINGTONE);
            //*/
            //*/ [tyd00436370] by tyd xiaocui 2012-08-01 for custom profile separate from general profile 
            title = mResources.getString(R.string.voice_call_title);
        } else if (call.equals("video")) {
            mDefault.setRingtoneType(RingtoneManager.TYPE_VIDEO_CALL);
            mCustom.setRingtoneType(RingtoneManager.TYPE_VIDEO_CALL);
            mRingtone = Settings.System.getString(mResolver, Settings.System.CUSTOM_VIDEO_CALL);
          //*/ [tyd00436370] by tyd xiaocui 2012-08-01 for custom profile separate from general profile 
            mUri = mProfileManager.getRingtoneUri(key,RingtoneManager.TYPE_VIDEO_CALL);
            title = mResources.getString(R.string.video_call_title);
          //*/by tyd wangalei 2015.9.15 for ringtone bug
            defaultRingtone = Settings.System.getString(mResolver, RingtoneManager.KEY_DEFAULT_VIDEO_CALL);
            //*/
          //*/by tyd wangalei 2015.9.17 for ringtone bug
            ringtone_type=RingtoneManager.TYPE_VIDEO_CALL;
            //*/
        } else if (call.equals("notification")) {
        	//*/[tyd00473302]
            mDefault.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
            mCustom.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
            mRingtone = Settings.System.getString(mResolver, Settings.System.CUSTOM_NOTIFICATION);
          //*/ [tyd00436370] by tyd xiaocui 2012-08-01 for custom profile separate from general profile 
            mUri = mProfileManager.getRingtoneUri(key,RingtoneManager.TYPE_NOTIFICATION);
            title = mResources.getString(R.string.notification_sound_title);
            //*/by tyd wangalei 2015.9.15 for ringtone bug
            defaultRingtone = Settings.System.getString(mResolver, RingtoneManager.KEY_DEFAULT_NOTIFICATION);
            //*/
            //*/by tyd wangalei 2015.9.17 for ringtone bug
            ringtone_type=RingtoneManager.TYPE_NOTIFICATION;
            //*/
        } else if (call.equals("message")) {
        	//*/[tyd00473302]
        	if((mSimId == SIM_ID_0) ||(mSimId == SIM_ID_NONE) ){
                mDefault.setRingtoneType(RingtoneManager.TYPE_MESSAGE);
                mCustom.setRingtoneType(RingtoneManager.TYPE_MESSAGE);
                mUri = mProfileManager.getRingtoneUri(key,RingtoneManager.TYPE_MESSAGE);
                //*/by tyd wangalei 2015.9.17 for ringtone bug
                ringtone_type=RingtoneManager.TYPE_MESSAGE;
                //*/
        	}else{
                mDefault.setRingtoneType(RingtoneManager.TYPE_MESSAGE_SIM2);
                mCustom.setRingtoneType(RingtoneManager.TYPE_MESSAGE_SIM2);
                mUri = mProfileManager.getRingtoneUri(key,RingtoneManager.TYPE_MESSAGE_SIM2);
                //*/by tyd wangalei 2015.9.17 for ringtone bug
                ringtone_type=RingtoneManager.TYPE_MESSAGE_SIM2;
                //*/
        	}
            mRingtone = Settings.System.getString(mResolver, Settings.System.CUSTOM_MESSAGE);
          //*/ [tyd00436370] by tyd xiaocui 2012-08-01 for custom profile separate from general profile 
            title = mResources.getString(R.string.zzzz_message_sound_title);
            //*/by tyd wangalei 2015.9.15 for ringtone bug
            defaultRingtone = Settings.System.getString(mResolver, RingtoneManager.KEY_DEFAULT_MESSAGE);
            //*/
        }
 	setTitle(title);
        setRingtone();
    }

    private void setRingtone() {
        if (mUri != null) {
            Ringtone ringtone = RingtoneManager.getRingtone(this, mUri);
            Cursor cursor = mResolver.query(mUri, new String[] { MediaStore.Audio.Media.IS_MUSIC }, null, null, null);
            if (cursor != null && cursor.getCount() == 1) {
                cursor.moveToFirst();
                if (cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)) == 1) {
                    if (ringtone != null) {
                        mDefault.setSummary(mResources.getString(R.string.zzzz_ringtone_default_summary));
                        mCustom.setSummary(ringtone.getTitle(this));
                    } else {
                        mUri = Uri.parse(mRingtone);
                        ringtone = RingtoneManager.getRingtone(this, mUri);
                        mDefault.setSummary(ringtone.getTitle(this));
                        mCustom.setSummary(mResources.getString(R.string.zzzz_ringtone_custom_summary));
                    }
                } else {
                    mDefault.setSummary(ringtone.getTitle(this));
                    mCustom.setSummary(mResources.getString(R.string.zzzz_ringtone_custom_summary));
                }
            } else {
            	  //*/by tyd wangalei 2015.9.15 for ringtone bug
                mUri = Uri.parse(defaultRingtone);
                //*/
                //*/by tyd wangalei 2015.9.17  for ringtone bug
                mProfileManager.setRingtoneUri(key,ringtone_type,mUri);
                //*/
                ringtone = RingtoneManager.getRingtone(this, mUri);
                mDefault.setSummary(ringtone.getTitle(this));
                mCustom.setSummary(mResources.getString(R.string.zzzz_ringtone_custom_summary));
            }
            if (cursor != null) {
                cursor.close();
            }
        } else {
            mDefault.setSummary(mResources.getString(com.android.internal.R.string.ringtone_silent));
            mCustom.setSummary(mResources.getString(R.string.zzzz_ringtone_custom_summary));
        }
        mDefault.setRingtone(mUri);
        mCustom.setRingtone(mUri);
    }
}
