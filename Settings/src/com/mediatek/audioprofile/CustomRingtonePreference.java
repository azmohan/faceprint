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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
//import com.mediatek.audioprofile.AudioProfile;
//import com.mediatek.audioprofile.AudioProfileImpl;
//import com.mediatek.audioprofile.AudioProfileManagerImpl;
import com.mediatek.audioprofile.AudioProfileManager;
import com.mediatek.audioprofile.AudioProfileManager.Scenario;

/**
 * A {@link Preference} that allows the user to choose a ringtone from those on the device. 
 * The chosen ringtone's URI will be persisted as a string.
 * <p>
 * If the user chooses the "Default" item, the saved string will be one of
 * {@link System#DEFAULT_RINGTONE_URI},
 * {@link System#DEFAULT_NOTIFICATION_URI}, or
 * {@link System#DEFAULT_ALARM_ALERT_URI}. If the user chooses the "Silent"
 * item, the saved string will be an empty string.
 * 
 * @attr ref android.R.styleable#RingtonePreference_ringtoneType
 * @attr ref android.R.styleable#RingtonePreference_showDefault
 * @attr ref android.R.styleable#RingtonePreference_showSilent
 */
public class CustomRingtonePreference extends Preference implements
        PreferenceManager.OnActivityResultListener {

    private static final String TAG = "CustomRingtonePreference";

    private int mRingtoneType;
    private boolean mShowDefault;
    private boolean mShowSilent;
    
    private int mRequestCode;

    private AudioProfileManager mProfileManager;
    private String mKey;
    private Context mContext;
    private RingtoneProfile mActivity;
    private Uri mUri;
    //*/ Add for Custom Ringtone by tyd xiaocui 2012-07-11 fixed bug
    private String mExternalStorage;
    //*/
    
    //*/ [tyd00437257]modified by tyd xiaocui 2012-08-06 for set customringtone when no SD
    private CustomRingtoneManager mCustomRingtoneManager;
    private Cursor mCursor;
    //*/

    public CustomRingtonePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        mProfileManager = (AudioProfileManager)context.getSystemService(Context.AUDIO_PROFILE_SERVICE);
        TypedArray a = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.RingtonePreference, defStyle, 0);
        mRingtoneType = a.getInt(com.android.internal.R.styleable.RingtonePreference_ringtoneType,
                CustomRingtoneManager.TYPE_RINGTONE);
        mShowDefault = a.getBoolean(com.android.internal.R.styleable.RingtonePreference_showDefault,
                true);
        mShowSilent = a.getBoolean(com.android.internal.R.styleable.RingtonePreference_showSilent,
                true);
        a.recycle();
    }

    public CustomRingtonePreference(Context context, AttributeSet attrs, boolean isMedia) {
        this(context, attrs, com.android.internal.R.attr.ringtonePreferenceStyle);
    }

    public CustomRingtonePreference(Context context) {
        this(context, null);
    }

    public CustomRingtonePreference(Context context, AttributeSet attrs) {
        this(context, attrs, true);
        mContext = context;
    }

    public void setProfile(String key){
    	mKey = key;
    }

    public void setActivity(RingtoneProfile activity) {
        mActivity = activity;
    }

    public void setRingtone(Uri uri) {
        mUri = uri;
    }

    /**
     * Returns the sound type(s) that are shown in the picker.
     * 
     * @return The sound type(s) that are shown in the picker.
     * @see #setRingtoneType(int)
     */
    public int getRingtoneType() {
        return mRingtoneType;
    }

    /**
     * Sets the sound type(s) that are shown in the picker.
     * 
     * @param type The sound type(s) that are shown in the picker.
     * @see CustomRingtoneManager#EXTRA_RINGTONE_TYPE
     */
    public void setRingtoneType(int type) {
        mRingtoneType = type;
    }

    /**
     * Returns whether to a show an item for the default sound/ringtone.
     * 
     * @return Whether to show an item for the default sound/ringtone.
     */
    public boolean getShowDefault() {
        return mShowDefault;
    }

    /**
     * Sets whether to show an item for the default sound/ringtone. The default
     * to use will be deduced from the sound type(s) being shown.
     * 
     * @param showDefault Whether to show the default or not.
     * @see CustomRingtoneManager#EXTRA_RINGTONE_SHOW_DEFAULT
     */
    public void setShowDefault(boolean showDefault) {
        mShowDefault = showDefault;
    }

    /**
     * Returns whether to a show an item for 'Silent'.
     * 
     * @return Whether to show an item for 'Silent'.
     */
    public boolean getShowSilent() {
        return mShowSilent;
    }

    /**
     * Sets whether to show an item for 'Silent'.
     * 
     * @param showSilent Whether to show 'Silent'.
     * @see CustomRingtoneManager#EXTRA_RINGTONE_SHOW_SILENT
     */
    public void setShowSilent(boolean showSilent) {
        mShowSilent = showSilent;
    }
    
    /*/ Add for Custom Ringtone by tyd xiaocui 2012-07-11 fixed bug xiaocui for 6589
	public boolean isStorageMounted() {
		mExternalStorage = Environment.getExternalStorageDirectorySd().toString();
		//return Environment.getExternalStorageStateSd().equals(Environment.MEDIA_MOUNTED);
		return Environment.getStorageState(mExternalStorage).equals(Environment.MEDIA_MOUNTED);
	}
    //*/

    @Override
    protected void onClick() {
        String status = Environment.getExternalStorageState();
        //*/ xiaocui0806
        mCustomRingtoneManager = new CustomRingtoneManager(mContext);
        mCursor = mCustomRingtoneManager.getCursor();
        //*/

        //*/ [tyd00437257]modified by tyd xiaocui 2012-08-06 for set customringtone when no SD
        if ((mCursor == null)||status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            String message = mContext.getResources().getString(R.string.zzzz_no_audio_file);
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        }
        else if (mCursor != null && mCursor.getCount() == 0) {
            mCursor.close();
            String message = mContext.getResources().getString(R.string.zzzz_no_audio_file);
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        }else
        //*/	
        {
            // Launch the ringtone picker
            Intent intent = new Intent(CustomRingtoneManager.ACTION_CUSTOM_RINGTONE_PICKER);
            onPrepareRingtonePickerIntent(intent);
            mActivity.startActivityForResult(intent, mRequestCode);
        }

    }

    /**
     * Prepares the intent to launch the ringtone picker. This can be modified
     * to adjust the parameters of the ringtone picker.
     * 
     * @param ringtonePickerIntent The ringtone picker intent that can be
     *            modified by putting extras.
     */
    protected void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {

        ringtonePickerIntent.putExtra(CustomRingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                onRestoreRingtone());
        
        ringtonePickerIntent.putExtra(CustomRingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, mShowDefault);
        if (mShowDefault) {
            ringtonePickerIntent.putExtra(CustomRingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                    CustomRingtoneManager.getDefaultUri(getRingtoneType()));
        }

        ringtonePickerIntent.putExtra(CustomRingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, mShowSilent);
        ringtonePickerIntent.putExtra(CustomRingtoneManager.EXTRA_RINGTONE_TYPE, mRingtoneType);

        /*
         * Since this preference is for choosing the default ringtone, it
         * doesn't make sense to show a 'Default' item.
         */
        ringtonePickerIntent.putExtra(CustomRingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        ringtonePickerIntent.putExtra(CustomRingtoneManager.EXTRA_RINGTONE_EXISTING_URI, mUri);
    }
    
    /**
     * Called when a ringtone is chosen.
     * <p>
     * By default, this saves the ringtone URI to the persistent storage as a
     * string.
     * 
     * @param ringtoneUri The chosen ringtone's {@link Uri}. Can be null.
     */
    protected void onSaveRingtone(Uri ringtoneUri) {
    	mProfileManager.setRingtoneUri(mKey, getRingtoneType(), ringtoneUri);
    }

    /**
     * Called when the chooser is about to be shown and the current ringtone
     * should be marked. Can return null to not mark any ringtone.
     * <p>
     * By default, this restores the previous ringtone URI from the persistent
     * storage.
     * 
     * @return The ringtone to be marked as the current ringtone.
     */
    protected Uri onRestoreRingtone() {
    	;
    	if(mProfileManager!=null){
            Uri uri = mProfileManager.getRingtoneUri(AudioProfileManager.getProfileKey(Scenario.GENERAL), getRingtoneType());
            if(uri != null && Settings.AUTHORITY.equals(uri.getAuthority())) {
            	return CustomRingtoneManager.getActualDefaultRingtoneUri(getContext(),
       				 getRingtoneType());
            } else {
            	return uri;
            }
    	}
    	return CustomRingtoneManager.getActualDefaultRingtoneUri(getContext(), getRingtoneType());
    }
    
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValueObj) {
        String defaultValue = (String) defaultValueObj;
        
        /*
         * This method is normally to make sure the internal state and UI
         * matches either the persisted value or the default value. Since we
         * don't show the current value in the UI (until the dialog is opened)
         * and we don't keep local state, if we are restoring the persisted
         * value we don't need to do anything.
         */
        if (restorePersistedValue) {
            return;
        }
        
        // If we are setting to the default value, we should persist it.
        if (!TextUtils.isEmpty(defaultValue)) {
            onSaveRingtone(Uri.parse(defaultValue));
        }
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        
        //*/ xiaocui for 6589
        //mRequestCode = preferenceManager.createPreferenceScreen(this);
        /*/
        mRequestCode = preferenceManager.doAttachedToHierarchy(this);
        //*/
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        
        if (requestCode == mRequestCode) {
            
            if (data != null) {
                Uri uri = data.getParcelableExtra(CustomRingtoneManager.EXTRA_RINGTONE_PICKED_URI);

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
                    Ringtone ringtone = CustomRingtoneManager.getRingtone(mContext, uri);
                    ContentResolver resolver = mContext.getContentResolver();
                    Cursor cursor = resolver.query(uri, new String[] { MediaStore.Audio.Media.IS_MUSIC }, null, null, null);
                    if (cursor != null && cursor.getCount() == 1) {
                        cursor.moveToFirst();
                        if (cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)) == 1) {
                            mActivity.mDefault.setSummary(resources.getString(R.string.zzzz_ringtone_default_summary));
                            setSummary(ringtone.getTitle(mActivity));
                        } else {
                            mActivity.mDefault.setSummary(ringtone.getTitle(mActivity));
                            setSummary(resources.getString(R.string.zzzz_ringtone_custom_summary));
                        }
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                } else {
                    mActivity.mDefault.setSummary(resources.getString(com.android.internal.R.string.ringtone_silent));
                    setSummary(resources.getString(R.string.zzzz_ringtone_custom_summary));
                }
                mActivity.mDefault.setRingtone(uri);
                setRingtone(uri);
            }
            
            return true;
        }
        
        return false;
    }
}
