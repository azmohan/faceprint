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

/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.mediatek.audioprofile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;

import com.mediatek.settings.ext.IAudioProfileExt;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import android.content.res.Resources;
import android.media.Ringtone;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;
import com.android.settings.R;

/**
 * Default ringtone preference.
 */
public class DefaultRingtonePreference extends RingtonePreference {
    private static final String TAG = "Settings/Rt_Pref";

    public static final String RING_TYPE = "RING";
    public static final String NOTIFICATION_TYPE = "NOTIFICATION";

    private final AudioProfileManager mProfileManager;
    private String mKey;
    private String mStreamType;
    private IAudioProfileExt mExt;
    private long mSimId = -1;
    private boolean mNoNeedSIMSelector = false;
    private static final int SINGLE_SIMCARD = 1;
    private static final String PREF_SIM_ID_VALUME = "SimIdValume";

    //*/Add for Custom Ringtone by xiaocui on 2013-08-30 Start
    private RingtoneProfile mActivity;
    private Uri mUri;
    private Context mContext;
    //*/
    /**
     * set the select sim id.
     *
     * @param simId
     *            the selected sim id
     */
    public void setSimId(long simId) {
        SharedPreferences prefs = this.getContext().getSharedPreferences(
                "DefaultRingtonePreference", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(PREF_SIM_ID_VALUME, simId);
        editor.commit();
        this.mSimId = simId;
        Log.d("@M_" + TAG, "setSimId   simId= " + simId  + " this.mSimId = " + this.mSimId);
    }

    /**
     * the DefaultRingtonePreference construct method.
     *
     * @param context
     *            the context which is associated with, through which it can
     *            access the theme and the resources
     * @param attrs
     *            the attributes of XML tag that is inflating the preference
     */
    public DefaultRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        //*/Add for Custom Ringtone by xiaocui on 2013-08-30 xiaocui0710
        mContext = context;
        //*/
        mProfileManager = (AudioProfileManager) context
                .getSystemService(Context.AUDIO_PROFILE_SERVICE);
        mExt = UtilsExt.getAudioProfilePlugin(context);
    }

    /**
     * bind the defaultRingtonePreference with the profile key.
     *
     * @param key
     *            the profile key
     */
    public void setProfile(String key) {
        mKey = key;
    }
    
    //*/Add for Custom Ringtone by xiaocui on 2013-08-30 
    public void setActivity(RingtoneProfile activity) {
    	mActivity = activity;
    }

    public void setRingtone(Uri uri) {
        mUri = uri;
    }
    //*/

    /**
     * Set the defaultRingtonePreference with some stream type for
     * STREAM_RINGER, STREAM_NOTIFICATION etc.
     *
     * @param streamType
     *            New stream type
     */
    public void setStreamType(String streamType) {
        mStreamType = streamType;
    }

    /**
     * Prepare the intent to launch the ringtone picker For Ring, hide the
     * "default Ringtone" item For CMCC, add the "More Ringtone" item.
     *
     * @param ringtonePickerIntent
     */
    @Override
    protected void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {
        super.onPrepareRingtonePickerIntent(ringtonePickerIntent);
        /*
         * Since this preference is for choosing the default ringtone, it
         * doesn't make sense to show a 'Default' item.
         */
        ringtonePickerIntent.putExtra(
                RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        
        //*/Add for Custom Ringtone by xiaocui on 2013-08-30
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, mUri);
        //*/

        if (mStreamType.equals(RING_TYPE)) {
            ringtonePickerIntent.putExtra(
                    RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        }

        mExt.setRingtonePickerParams(ringtonePickerIntent);
    }

    /**
     * Called when the ringtone is choosen, set the selected ringtone uri to framework
     *
     * @param ringtoneUri
     *            the selected ringtone uri
     */
    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        SharedPreferences prefs = this.getContext().getSharedPreferences(
                "DefaultRingtonePreference", Context.MODE_PRIVATE);
        mSimId = prefs.getLong(PREF_SIM_ID_VALUME, -1);
        mProfileManager.setRingtoneUri(mKey, getRingtoneType(), mSimId, ringtoneUri);
    }

    /**
     * Called when the chooser is about to shown, get the current selected profile URI
     *
     * @return the ringtone uri that need to be choosen
     */
    @Override
    protected Uri onRestoreRingtone() {
        int type = getRingtoneType();
        Log.d("@M_" + TAG, "onRestoreRingtone: type = " + type + " mKey = " + mKey
                + "  mSimId= " + mSimId);

        Uri uri = mProfileManager.getRingtoneUri(mKey, type, mSimId);
        Log.d("@M_" + TAG,
                "onRestoreRingtone: uri = "
                        + (uri == null ? "null" : uri.toString()));

        return uri;
    }

    @Override
    protected void onClick() {
        // M: Set different SIM ringtone
        // modified by mtk54031
        final TelephonyManager mTeleManager = (TelephonyManager) getContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        //int simNum = mTeleManager.getSimCount();
        int simNum = SubscriptionManager.from(getContext()).getActiveSubscriptionInfoCount();
        Log.d("@M_" + TAG, "onClick  : isNoNeedSIMSelector = " + isNoNeedSIMSelector()
                + "simNum <= SINGLE_SIMCARD: simNum = " + simNum);

        if (FeatureOption.MTK_MULTISIM_RINGTONE_SUPPORT && simNum == SINGLE_SIMCARD) {
           int subId = SubscriptionManager.from(getContext()).getActiveSubscriptionIdList()[0];
           setSimId(subId);
           //setSimId(1);
        }

       //*/Add for Custom Ringtone by xiaocui on 2013-08-30
          super.onClick();
       /*/
        if (isNoNeedSIMSelector() || simNum <= SINGLE_SIMCARD) {
            super.onClick();
        }
       //*/
    }

    void simSelectorOnClick() {
        Log.d("@M_" + TAG, "onClick  : simSelectorOnClick  ");
        super.onClick();
    }

    public boolean isNoNeedSIMSelector() {
        return mNoNeedSIMSelector;
    }

    public void setNoNeedSIMSelector(boolean mNoNeedSIMSelector) {
        this.mNoNeedSIMSelector = mNoNeedSIMSelector;
    }


    
    /**
     * Add for Custom Ringtone by xiaocui on 2013-08-30 . xiaocui0710
     */
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        

            
            if (data != null) {
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                
                //*/ freeme.chenming, 20161209. do nothing, if no ringtone picked
                if (uri == null) {
                    return false;
                }
                //*/

                if (callChangeListener(uri != null ? uri.toString() : "")) {
                    onSaveRingtone(uri);
                }
                
                Resources resources = mContext.getResources();
                if (uri != null) {
                    Ringtone ringtone = RingtoneManager.getRingtone(mContext, uri);
                    ContentResolver resolver = mContext.getContentResolver();
                    Cursor cursor = resolver.query(uri, new String[] {MediaStore.Audio.Media.IS_MUSIC}, null, null, null);
                    if (cursor != null && cursor.getCount() == 1) {
                        cursor.moveToFirst();
                        if (cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)) == 1) {
                            setSummary(resources.getString(R.string.zzzz_ringtone_default_summary));
                            mActivity.mCustom.setSummary(ringtone.getTitle(mActivity));
                        } else {
                            setSummary(ringtone.getTitle(mActivity));
                            mActivity.mCustom.setSummary(resources.getString(R.string.zzzz_ringtone_custom_summary));
                        }
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                } else {
                    setSummary(resources.getString(com.android.internal.R.string.ringtone_silent));
                    mActivity.mCustom.setSummary(resources.getString(R.string.zzzz_ringtone_custom_summary));
                }
                setRingtone(uri);
                mActivity.mCustom.setRingtone(uri);
            }
            
            return true;

    }    

}
