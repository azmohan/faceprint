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

//import com.mediatek.audioprofile.AudioProfile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.media.RingtoneManager;

//*/freeme xiaocui, 20160526. SIM2 Ringtone
import com.mediatek.settings.FeatureOption;
import android.telephony.TelephonyManager;
import com.android.settings.R;
import android.telephony.SubscriptionManager;
import android.os.SystemProperties;
import java.util.List;
import android.telephony.SubscriptionInfo;
import com.android.internal.telephony.PhoneConstants;
//*/

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class DefaultPreference extends Preference {
    private static final String TAG = "DefaultPreference";
    private String mKey;
    private int mRingtoneType;
    private String mStreamType;
    private Activity mActivity;
    private long mSimId = -1;
    private boolean mNoNeedSIMSelector = false;
    private static final int SINGLE_SIMCARD = 1;

    //*/freeme xiaocui, 20160526. SIM2 Ringtone
    private TelephonyManager mTeleManager;
    private  int numSlots;
    public static final boolean TYD_MULTISIM_RINGTONE_SUPPORT = SystemProperties.get("ro.tyd_freeme_multi_sim").equals("1");
    private static  SubscriptionManager subscriptionManager;
    //*/
    
    private boolean isFirst = false;
    
    private SharedPreferences mPrefs = null;

    public DefaultPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

       //*/freeme xiaocui, 20160526. SIM2 Ringtone
        mTeleManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        subscriptionManager = SubscriptionManager.from(context);
        numSlots = subscriptionManager.getActiveSubscriptionInfoCount();
       
        if (null == mPrefs) {
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }        

            int mLayoutResId = R.layout.noimg_preference;
        	setLayoutResource(mLayoutResId);
       //*/
    }


    @Override
    public void setLayoutResource(int layoutResId){
    	super.setLayoutResource(layoutResId);
    }
    
    public void setProfile(String key){
    	mKey = key;
    }
    
    public void setRingtoneType(int ringtoneType) {
        mRingtoneType = ringtoneType;
    }
    
    public void setActivity(Activity activity) {
    	mActivity = activity;
    }
    public void setStreamType(String streamType) {
    	mStreamType = streamType;

        if((streamType.equals(DefaultRingtonePreference.NOTIFICATION_TYPE)) || (mRingtoneType == AudioProfileManager.TYPE_MESSAGE)){
            return;
        }
        if(numSlots >= 2){
        	isFirst =  mPrefs.getBoolean("First",true);
        }

        if(isFirst){
            int mLayoutResId = R.layout.img_preference;
        	 setLayoutResource(mLayoutResId);
        }
    }

    
    void simSelectorOnClick() {
        super.onClick();
    }



    public boolean isNoNeedSIMSelector() {
        return mNoNeedSIMSelector;
    }

    public void setNoNeedSIMSelector(boolean mNoNeedSIMSelector) {
        this.mNoNeedSIMSelector = mNoNeedSIMSelector;
    }

    /**
     * set the select sim id
     * @param mSimId
     *           the selected sim id
     */
    public void setSimId(long simId) {
        
        this.mSimId = simId;     
    }
    
    //*/freeme xiaocui, 20160526. SIM2 Ringtone
    @Override
    protected void onClick() {
        if(numSlots >= 2)
        {
            int mLayoutResId = R.layout.noimg_preference;
        	setLayoutResource(mLayoutResId);
            SharedPreferences.Editor ed = mPrefs.edit();
            ed.putBoolean("First",false);
            ed.commit();
            super.notifyChanged();
        }
    	setRingtongTypeAndStartSIMSelector();
    }
   /*/
    @Override
    protected void onClick() {
        Intent ringtoneProfileIntent = new Intent(mActivity, RingtoneProfile.class);
        ringtoneProfileIntent.putExtra("Profile", mKey);
        if (mRingtoneType == RingtoneManager.TYPE_RINGTONE) {
            ringtoneProfileIntent.putExtra("Ringtone", "voice");
        } else if (mRingtoneType == RingtoneManager.TYPE_VIDEO_CALL) {
            ringtoneProfileIntent.putExtra("Ringtone", "video");
        } else if (mRingtoneType == RingtoneManager.TYPE_NOTIFICATION) {
            ringtoneProfileIntent.putExtra("Ringtone", "notification");
        } else if (mRingtoneType == RingtoneManager.TYPE_MESSAGE) {
            ringtoneProfileIntent.putExtra("Ringtone", "message");
        }
        mActivity.startActivity(ringtoneProfileIntent);
    }
   //*/

   //*/Add for SIM2 Ringtone by tyd xiaocui 2015-08-05
   private void startSingleCardActivity(){
        Intent ringtoneProfileIntent = new Intent(mActivity, RingtoneProfile.class);
        Log.i("xiaocui33","DefaultPreference onClick mRingtoneType =" +mRingtoneType);
        ringtoneProfileIntent.putExtra("Profile", mKey);
        if (mRingtoneType == RingtoneManager.TYPE_RINGTONE) {
            ringtoneProfileIntent.putExtra("Ringtone", "voice");
        } else if (mRingtoneType == RingtoneManager.TYPE_VIDEO_CALL) {
            ringtoneProfileIntent.putExtra("Ringtone", "video");
        } else if (mRingtoneType == RingtoneManager.TYPE_NOTIFICATION) {
            ringtoneProfileIntent.putExtra("Ringtone", "notification");
        } else if (mRingtoneType == RingtoneManager.TYPE_MESSAGE) {
            ringtoneProfileIntent.putExtra("Ringtone", "message");
        }
        ringtoneProfileIntent.putExtra(PhoneConstants.SLOT_KEY,getSimId());
        mActivity.startActivity(ringtoneProfileIntent);
    }

    private static final int REQUEST_CODE = 0;
    private static final String ACTION_SIM_SETTINGS = "com.android.settings.sim.select";
    private void setRingtongTypeAndStartSIMSelector() {
        //Xlog.d(TAG, "Selected ringtone type index = " + keyIndex);
        Log.d("xiaocui33"," setRingtongTypeAndStartSIMSelector  numSlots =" +numSlots );
        if (TYD_MULTISIM_RINGTONE_SUPPORT) {
            if ((numSlots > SINGLE_SIMCARD)&&((mRingtoneType ==RingtoneManager.TYPE_RINGTONE )||(mRingtoneType ==RingtoneManager.TYPE_MESSAGE ))) {
                startSIMCardSelectorActivity(mRingtoneType);
            }else{
            	startSingleCardActivity();
            }
        }else{
        	  startSingleCardActivity();
        }
    }


    private void startSIMCardSelectorActivity(int type) {
        Intent intent = new Intent();
        intent.setAction(ACTION_SIM_SETTINGS);
        
        if (mRingtoneType == RingtoneManager.TYPE_RINGTONE) {
        	intent.putExtra("Ringtone", "voice");
        	intent.putExtra(Intent.EXTRA_TITLE,mActivity.getString( R.string.ringtone_title));
        	Log.i("xiaocui33","startSIMCardSelectorActivity =" + mActivity.getString( R.string.ringtone_title));
        } else if (mRingtoneType == RingtoneManager.TYPE_VIDEO_CALL) {
        	intent.putExtra("Ringtone", "video");
        } else if (mRingtoneType == RingtoneManager.TYPE_NOTIFICATION) {
        	intent.putExtra("Ringtone", "notification");
        } else if (mRingtoneType == RingtoneManager.TYPE_MESSAGE) {
        	intent.putExtra("Ringtone", "message");
        	intent.putExtra(Intent.EXTRA_TITLE,mActivity.getString( R.string.zzzz_message_sound_title));
        	
        }
       
        intent.putExtra("Profile", mKey);
        mActivity.startActivity(intent);
    }
    
    
    private void setRingtoneSIMId(long simId) {
               setSimId(simId);
               simSelectorOnClick();
      
    }
    
    public static int getSimId() {
    	int simid;
        final List<SubscriptionInfo> subInfoList =
        		subscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            final int subInfoLength = subInfoList.size();
            
            for (int i = 0; i < subInfoLength; ++i) {
                final SubscriptionInfo sir = subInfoList.get(i);
                if (sir != null) {
                	simid = sir.getSimSlotIndex();
                   Log.i("xiaocui33","DefaultPreference getSimId  simid =" +simid + "subInfoLength =" +subInfoLength);
                    return  simid;
                }
            }
        }

        return -1;
    }
//*/

}
