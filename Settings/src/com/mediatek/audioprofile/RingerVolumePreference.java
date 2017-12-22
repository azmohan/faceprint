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

package com.mediatek.audioprofile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference.BaseSavedState;
import android.preference.SeekBarDialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.settings.R;
import com.android.settings.Utils;

import com.mediatek.common.audioprofile.AudioProfileListener;
import com.mediatek.settings.ext.IAudioProfileExt;
import com.mediatek.settings.UtilsExt;
//*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
import android.os.Message;
import android.content.res.Configuration;
import java.util.Objects;
import android.media.AudioAttributes;
import android.os.SystemVibrator;
import android.os.Vibrator;
import android.media.ToneGenerator;
import android.view.WindowManager;
import android.os.Bundle;
import android.content.ContentResolver;
import android.app.Dialog;
//*/

/**
 * Special preference type that allows configuration of both the ring volume and
 * notification volume.
 */
public class RingerVolumePreference extends SeekBarDialogPreference {
    private static final String TAG = "Settings/VolPref";
    private static final boolean LOGV = true;

    private String mKey;
    private SeekBarVolumizer[] mSeekBarVolumizer;
    private VolumeReceiver mReceiver;
    private final AudioManager mAudioManager;
    private final AudioProfileManager mProfileManager;

    private boolean mIsDlgDismissed = true;

    private IAudioProfileExt mExt;
    
    //*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
    private boolean mSwitch = false;
    private Vibrator mVibrator;
    private View mView;
    private ContentResolver mContentResolver;
    private int mRingerMode = -1;
    private int mRingtoeVolume = 0;
    private static final boolean DEBUG = false;
    //*/
    
    //*/Add by tyd xiaocui 0723 for the system volume and media volume
    private static final int[] SEEKBAR_ID = new int[] {
    	    R.id.media_volume_seekbar,
    	    R.id.ringer_volume_seekbar,R.id.notification_volume_seekbar,
        	R.id.system_volume_seekbar,R.id.alarm_volume_seekbar };
    /*/
    private static final int[] SEEKBAR_ID = new int[] {
            R.id.notification_volume_seekbar, R.id.ringer_volume_seekbar,
            R.id.alarm_volume_seekbar };
   //*/
    
    //*/Add by tyd xiaocui 0723 for the system volume and media volume
    private static final int[] SEEKBAR_TYPE = new int[] {
    	    AudioProfileManager.STREAM_MEDIA,AudioProfileManager.STREAM_RING,
    	    AudioProfileManager.STREAM_NOTIFICATION,
        	AudioProfileManager.STREAM_SYSTEM,
        	AudioProfileManager.STREAM_ALARM };
    /*/
    private static final int[] SEEKBAR_TYPE = new int[] {
            AudioProfileManager.STREAM_NOTIFICATION,
            AudioProfileManager.STREAM_RING, AudioProfileManager.STREAM_ALARM };
    //*/

    //*/Add by tyd xiaocui 0723 for the system volume and media volume
    private static final int[] CHECKBOX_VIEW_ID = new int[] {
    	R.id.media_mute_button,
        R.id.ringer_mute_button, R.id.notification_mute_button,
        R.id.system_mute_button,R.id.alarm_mute_button };
    /*/
    private static final int[] CHECKBOX_VIEW_ID = new int[] {
            R.id.ringer_mute_button, R.id.notification_mute_button,
            R.id.alarm_mute_button };
   //*/       

    //*/ Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile //Add by tyd xiaocui 0723 for the system volume and media volume
    private static final int[] SEEKBAR_UNMUTED_RES_ID = new int[] {
    	R.drawable.ic_audio_media,
    	R.drawable.ic_audio_ring_notif,
        R.drawable.ic_audio_notification,
        R.drawable.ic_audio_ring_system,     
        R.drawable.ic_audio_alarm };

    /*/
    private static final int[] SEEKBAR_UNMUTED_RES_ID = new int[] {
            com.android.internal.R.drawable.ic_audio_ring_notif,
            com.android.internal.R.drawable.ic_audio_notification,
            com.android.internal.R.drawable.ic_audio_alarm };
   //*/         

    /**
     * bind the preference with the profile
     * 
     * @param key
     *            the profile key
     */
    public void setProfile(String key) {
        mKey = key;
    }

    /**
     * The RingerVolumePreference construct method
     * 
     * @param context
     *            context, the context which is associated with, through which
     *            it can access the theme and the resources
     * @param attrs
     *            the attributes of XML tag that is inflating the preferenc
     */
    public RingerVolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_dialog_ringervolume_audioprofile);
        mAudioManager = (AudioManager) context
                .getSystemService(Context.AUDIO_SERVICE);
        mProfileManager = (AudioProfileManager) context
                .getSystemService(Context.AUDIO_PROFILE_SERVICE);
        mSeekBarVolumizer = new SeekBarVolumizer[SEEKBAR_ID.length];
        mExt = UtilsExt.getAudioProfilePlugin(context);
    }

    /**
     * Bind views in the content view of the dialog to data
     * 
     * @param view
     *            The content view of the dialog
     */
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        Context context = getContext();
        mReceiver = new VolumeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
        context.registerReceiver(mReceiver, filter);
//*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
        mView = view;
        mRingerMode = mAudioManager.getRingerModeInternal();
        mContentResolver = context.getContentResolver();
//*/
        mIsDlgDismissed = false;
        //Xlog.d(TAG, "set mIsDlgDismissed to false ");

        for (int i = 0; i < SEEKBAR_ID.length; i++) {
            ImageView imageview = (ImageView) view
                    .findViewById(CHECKBOX_VIEW_ID[i]);
            if (imageview != null) {
                imageview.setImageResource(SEEKBAR_UNMUTED_RES_ID[i]);
            }

            SeekBar seekBar = (SeekBar) view.findViewById(SEEKBAR_ID[i]);
            if (seekBar != null) {
                if (i == 0) {
                    seekBar.requestFocus();
                }
                mSeekBarVolumizer[i] = new SeekBarVolumizer(context, seekBar,
                        SEEKBAR_TYPE[i]);
                // seekBar.setOnKeyListener(this);  M:fix BT HID keyboard no response of keyevent issue
            }
        }

        view.setFocusableInTouchMode(true);

        // Disable either ringer+notifications or notifications
        
        /*/Add by tyd xiaocui 0723 for the system volume and media volume 
        int id;
        if (Utils.isVoiceCapable(getContext())) {
            id = R.id.notification_section;
            mSeekBarVolumizer[0].setVisible(false);
        } else {
            id = R.id.ringer_section;
            mSeekBarVolumizer[1].setVisible(false);
        }
        View hideSection = view.findViewById(id);

        hideSection.setVisibility(View.GONE);
        //*/

        mProfileManager.listenAudioProfie(mListener,
                AudioProfileListener.LISTEN_RINGER_VOLUME_CHANGED);
        
        //*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
        mAudioManager.listenRingerModeAndVolume(mRingerModeListener, AudioProfileListener.LISTEN_RINGERMODE_CHANGED);
        
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mHasVibrator = mVibrator != null && mVibrator.hasVibrator();
        //setVolumeState();
        updateVolumeUi();
        isRingNormalAndUpdate();
       // getSystemVolume();
        //*/
    }
  //*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
    @Override
    public Dialog getDialog() {
        return super.getDialog();
    }
 //*/

    /**
     * When the EditProfile is paused, stop sampling
     */
    public void stopPlaying() {

        if (mSeekBarVolumizer != null) {
            for (SeekBarVolumizer vol : mSeekBarVolumizer) {
                if (vol != null && vol.isPlaying()) {
                    //Xlog.d(TAG, "IsPlaying");
                    vol.stopSample();
                    //Xlog.d(TAG, "stopPlaying");
                }
            }
        }
    }

    /**
     * When the EditPorifle is paused, but the user changed the volume and have
     * not save it, revert it.
     */
    public void revertVolume() {
       // Xlog.d(TAG, "mIsDlgDismissed" + mIsDlgDismissed);
        if (mIsDlgDismissed) {
            return;
        }
        if (mSeekBarVolumizer != null) {
            for (SeekBarVolumizer vol : mSeekBarVolumizer) {
                if (vol != null) {
                    vol.revertVolume();
                    vol.resume();
                }
            }
        }
    }

    /**
     * Press the hw volume key, change the current focus seekbar volume
     * 
     * @param v
     *            the ringer volume preference
     * @param keyCode
     *            the keycode of pressed key
     * @param event
     *            the event of key
     * @return true, if press volume up or volume down
     */
/*   M:fix BT HID keyboard no response of keyevent issue: phase out for audio profile volume listener already added      
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // If key arrives immediately after the activity has been cleaned up.
        if (mSeekBarVolumizer == null) {
            return true;
        }

        boolean isdown = (event.getAction() == KeyEvent.ACTION_DOWN);
        for (SeekBarVolumizer vol : mSeekBarVolumizer) {
            if (vol != null && vol.getSeekBar() != null
                    && vol.getSeekBar().isFocused()) {
                switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (isdown) {
                        vol.changeVolumeBy(-1);
                    }
                    return true;
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (isdown) {
                        vol.changeVolumeBy(1);
                    }
                    return true;
                default:
                    return false;
                }
            }
        }
        return true;
    }
*/
    /**
     * when start sampling , firstly stop other sampling
     * 
     * @param volumizer
     *            the volumizer which will start sampling
     */
    protected void onSampleStarting(SeekBarVolumizer volumizer) {
        if (volumizer == null) {
            return;
        }
        for (SeekBarVolumizer vol : mSeekBarVolumizer) {
            if (vol != null && vol != volumizer) {
                vol.stopSample();
            }
        }
    }
//*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
    @Override
    public void onActivityDestroy(){
    	//if(DEBUG)
    	//Xlog.i("xiaocui33", "onActivityDestroy");
    	super.onActivityDestroy();
    }
    
    @Override
    protected void onClick() {
    	super.onClick();
    }
    @Override
    protected void showDialog(Bundle state){
    	super.showDialog(state);
    	Dialog mDialog = getDialog();
    	if(mDialog != null){
            getDialog().getWindow().addFlagsEx(WindowManager.LayoutParams.FLAG_EX_NOSHOW_VOLUME);
    	}
    }
 //*/
    /**
     * Called when close the adjust volume dialog
     * 
     * @param positiveResult
     *            whether the pressed item is the positive button
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (mSeekBarVolumizer == null) {
            return;
        }
        for (SeekBarVolumizer vol : mSeekBarVolumizer) {
            vol.stopSample();
        }
        if (positiveResult) {
            for (SeekBarVolumizer vol : mSeekBarVolumizer) {
                if (vol != null && vol.getVisible()) {
               //*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
                	if(isNormalActive()){
                        vol.saveVolume();
                	}
                //*/
                    vol.getSeekBar().setOnKeyListener(null);
                    vol.stop();
                    vol = null;
                }
            }
        } else {
           // Xlog.d(TAG, "Cacel: Original checked.");
            for (SeekBarVolumizer vol : mSeekBarVolumizer) {
                if (vol != null && vol.getVisible()) {
                    vol.revertVolume();
                    vol.getSeekBar().setOnKeyListener(null);
                    vol.stop();
                    vol = null;
                }
            }
        }
        mIsDlgDismissed = true;
        //Xlog.d(TAG, "set mIsDlgDismissed to true");
        getContext().unregisterReceiver(mReceiver);
        mProfileManager.listenAudioProfie(mListener,
                AudioProfileListener.STOP_LISTEN);
    }

    /**
     * Save the current volume store object including the current volume,
     * original volume and the system volume
     * 
     * @return A parcelable object containing the current dynamic state of this
     *         preference
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        if (mSeekBarVolumizer != null) {
            VolumeStore[] volumeStore = myState
                    .getVolumeStore(SEEKBAR_ID.length);
            for (int i = 0; i < SEEKBAR_ID.length; i++) {
                SeekBarVolumizer vol = mSeekBarVolumizer[i];
                if (vol != null) {
                    vol.onSaveInstanceState(volumeStore[i]);
                }
            }
        }
        return myState;
    }

    /**
     * Allowing a preference to re-apply a presentation of its internal state.
     * 
     * @param state
     *            the volumeStore object returned by onSaveInstanceState()
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        if (mSeekBarVolumizer != null) {
            VolumeStore[] volumeStore = myState
                    .getVolumeStore(SEEKBAR_ID.length);
            for (int i = 0; i < SEEKBAR_ID.length; i++) {
                SeekBarVolumizer vol = mSeekBarVolumizer[i];
                if (vol != null) {
                    vol.onRestoreInstanceState(volumeStore[i]);
                }
            }
        }
    }

    public static class VolumeStore {
        public int mVolume = -1;
        public int mOriginalVolume = -1;
        public int mSystemVolume = -1;
    }

    private static class SavedState extends BaseSavedState {
        VolumeStore[] mVolumeStore;

        public SavedState(Parcel source) {
            super(source);
            mVolumeStore = new VolumeStore[SEEKBAR_ID.length];
            for (int i = 0; i < SEEKBAR_ID.length; i++) {
                mVolumeStore[i] = new VolumeStore();
                mVolumeStore[i].mVolume = source.readInt();
                mVolumeStore[i].mOriginalVolume = source.readInt();
                mVolumeStore[i].mSystemVolume = source.readInt();
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            for (int i = 0; i < SEEKBAR_ID.length; i++) {
                dest.writeInt(mVolumeStore[i].mVolume);
                dest.writeInt(mVolumeStore[i].mOriginalVolume);
                dest.writeInt(mVolumeStore[i].mSystemVolume);
            }
        }

        VolumeStore[] getVolumeStore(int count) {
            if (mVolumeStore == null || mVolumeStore.length != count) {
                mVolumeStore = new VolumeStore[count];
                for (int i = 0; i < count; i++) {
                    mVolumeStore[i] = new VolumeStore();
                }
            }
            return mVolumeStore;
        }

        public SavedState(Parcelable superState) {
            super(superState);

        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public class SeekBarVolumizer implements OnSeekBarChangeListener, Runnable {

        private Context mContext;
        private Handler mHandler = new Handler();

        private final int mStreamType;
        public Ringtone mRingtone;
        public int mSystemVolume = -1;
        private int mOriginalVolume = -1;
        private int mLastProgress = -1;
        private final SeekBar mSeekBar;
        private Uri mDefaultUri = null;

        public boolean mProfileIsActive = false;

        private boolean mIsVisible = true;

        /**
         * SeekbarVolumizer construct method
         * 
         * @param context
         *            the context which is associated with, through which it can
         *            access the theme and the resources
         * @param seekBar
         * @param streamType
         *            the streamTYpe of the current seekbar
         */
        public SeekBarVolumizer(Context context, SeekBar seekBar, int streamType) {
            mContext = context;

            mStreamType = streamType;
            mSeekBar = seekBar;

            initSeekBar(seekBar);
        }

        /**
         * Init the seekbar about the max volume , current volume, default Uri
         * 
         * @param seekBar
         */
        private void initSeekBar(SeekBar seekBar) {

            seekBar.setMax(mProfileManager.getStreamMaxVolume(mStreamType));

            mSystemVolume = mAudioManager.getStreamVolume(mStreamType);
            //Xlog.d(TAG, "" + mStreamType + " get Original SYSTEM Volume: "
            //        + mSystemVolume);

            mOriginalVolume = mProfileManager
                    .getStreamVolume(mKey, mStreamType);
           // Xlog.d(TAG, "" + mStreamType + " get Original Volume: "
            //        + mOriginalVolume);

            mProfileIsActive = mProfileManager.isActiveProfile(mKey);
            // if the volume is changed to 1 for ringer mode changed and we
            // can't receive the
            // broadcast to adjust the volume, sync the profile volume with the
            // system
            if (mProfileIsActive) {
                if (mSystemVolume != mOriginalVolume) {
                    //Xlog.d(TAG, " sync " + mStreamType + " original Volume to"
                    //        + mSystemVolume);
                    mOriginalVolume = mSystemVolume;
                }
            }

            mLastProgress = mOriginalVolume;
            seekBar.setProgress(mLastProgress);
            seekBar.setOnSeekBarChangeListener(this);
//*/Add by tyd xiaocui 0723 for the system volume and media volume 
            
            if ((mStreamType == AudioProfileManager.STREAM_RING)||(mStreamType == AudioProfileManager.STREAM_MEDIA)) {            
/*/
            if (mStreamType == AudioProfileManager.STREAM_RING) {
//*/
                mDefaultUri = mProfileManager.getRingtoneUri(mKey,
                        AudioProfileManager.TYPE_RINGTONE);
            } else if (mStreamType == AudioProfileManager.STREAM_NOTIFICATION) {
                mDefaultUri = mProfileManager.getRingtoneUri(mKey,
                        AudioProfileManager.TYPE_NOTIFICATION);
            } else if (mStreamType == AudioProfileManager.STREAM_ALARM) {
                mDefaultUri = Settings.System.DEFAULT_ALARM_ALERT_URI;
            }

            mRingtone = RingtoneManager.getRingtone(mContext, mDefaultUri);
            if (mRingtone != null) {
                mRingtone.setStreamType(mStreamType);
            }
            //*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
            if(mSeekBar != null && mSeekBar.getProgress() == 0)
            iconSwitchIndex(mStreamType);
            //*/
        }

        /**
         * Set the seekbar visible or unvisible
         * 
         * @param visible
         *            true, the seekbar visible
         */
        public void setVisible(boolean visible) {
            mIsVisible = visible;
        }

        /**
         * get the seekbar whether visible
         * 
         * @return the seekbar visible status
         */
        public boolean getVisible() {
            return mIsVisible;
        }

        /**
         * called when the adjust volume dialog closed unreister the Seekbar
         * change listener
         */
        public void stop() {
            mSeekBar.setOnSeekBarChangeListener(null);
            mContext = null;
            mHandler = null;
        }

        /**
         * return whether the seekbar is sampling
         * 
         * @return the sampling status of the seekbar
         */
        public boolean isPlaying() {

            if (mRingtone != null) {
                return mRingtone.isPlaying();
            }

            return false;
        }

        /**
         * After reverting the volume , sync the system volume, original volume
         * of the seekbar with AudioProfileManager framework
         */
        public void resume() {

            mSystemVolume = mAudioManager.getStreamVolume(mStreamType);
            //Xlog.d(TAG, "" + mStreamType + " get Original SYSTEM Volume: "
            //        + mSystemVolume);

            mOriginalVolume = mProfileManager
                    .getStreamVolume(mKey, mStreamType);
            //Xlog.d(TAG, "" + mStreamType + " get Original Volume: "
             //       + mOriginalVolume);

            mProfileIsActive = mProfileManager.isActiveProfile(mKey);
            // if the volume is changed to 1 for ringer mode changed and we
            // can't receive the
            // broadcast to adjust the volume, sync the profile volume with the
            // system
            if (mProfileIsActive) {
                if (mSystemVolume != mOriginalVolume) {
                   // Xlog.d(TAG, " sync " + mStreamType + " original Volume to"
                    //        + mSystemVolume);
                    mOriginalVolume = mSystemVolume;
                }
            }

            mLastProgress = mOriginalVolume;
            if (mSeekBar != null) {
                mSeekBar.setProgress(mLastProgress);
            }
        }

        /**
         * When click the "Cancel" button or pause the volume dialog revert the
         * volume
         */
        public void revertVolume() {
            //Xlog.d(TAG, "" + mStreamType + " revert Last Volume "
           //         + mOriginalVolume);

            // if(mProfileManager.isActive(mKey)) {
            mProfileManager.setStreamVolume(mKey, mStreamType, mOriginalVolume);
            
            /*/Add by tyd xiaocui 0723 for the system volume and media volume
            if (mStreamType == AudioProfileManager.STREAM_RING) {
                mProfileManager.setStreamVolume(mKey,
                        AudioProfileManager.STREAM_NOTIFICATION,
                        mOriginalVolume);
            }
            //*/
            // }
          
         //*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
            if (mProfileManager.isActiveProfile(mKey) && !isSilentProfileActive()) {
                //Xlog.d(TAG, "" + mStreamType + " Active, Revert system Volume "
               //         + mOriginalVolume);
                if(mOriginalVolume != 0 && mStreamType != AudioProfileManager.STREAM_RING)
                setVolume(mStreamType, mOriginalVolume, false);
            } else {
                if (!isSilentProfileActive()) {
                    //Xlog.d(TAG, "" + mStreamType
                   //         + " not Active, Revert system Volume "
                    //        + mSystemVolume);
                    setVolume(mStreamType, mSystemVolume, false);
                }
            }
        }

        /**
         * When click the "Ok" button, set the volume to system
         */
        public void saveVolume() {
            //Xlog.d(TAG, "" + mStreamType + " Save Last Volume " + mLastProgress);

            mProfileManager.setStreamVolume(mKey, mStreamType, mLastProgress);
            /*/Add by tyd xiaocui 0723 for the system volume and media volume
            if (mStreamType == AudioProfileManager.STREAM_RING) {
                mProfileManager.setStreamVolume(mKey,
                        AudioProfileManager.STREAM_NOTIFICATION, mLastProgress);
            }
			//*/
            if (mProfileManager.isActiveProfile(mKey)) {
                //Xlog.d(TAG, "" + mStreamType + " Active, save system Volume "
                //        + mLastProgress);
                setVolume(mStreamType, mLastProgress, false);
            } else {
                if (!isSilentProfileActive()) {
                    setVolume(mStreamType, mSystemVolume, false);
                }
            }

        }

        /**
         * According to the streamType, volume ,flag, set the volume to the
         * system
         * 
         * @param streamType
         *            The StreamType of the volume which will be set
         * @param volume
         *            the volume value which will be set
         * @param flag
         *            true, set the volume by calling
         *            AudioManager.setAudioProfileStreamVolume, in this API,
         *            even though the volume is set to 0, the ringer Mode will
         *            not change, it is useful because in the general profile of
         *            common load, set the volume to 0 and not save, in this
         *            case we need not to change the ringermode, we change the
         *            ringermode if click the "ok" button.The same case is about
         *            the CMCC load, in CMCC, we need not to change the ringer
         *            mode no matter the volume we set is 0 even though we click
         *            "ok" button.
         */
        private void setVolume(int streamType, int volume, boolean flag) {
            if (streamType == AudioProfileManager.STREAM_RING) {

                if (flag) {
                    mAudioManager.setAudioProfileStreamVolume(mStreamType,
                            volume, 0);
                    //*/Add by tyd xiaocui 0723 for the system volume and media volume
                    mAudioManager.setAudioProfileStreamVolume(
                            AudioProfileManager.STREAM_RING, volume, 0);
                    /*/
                    mAudioManager.setAudioProfileStreamVolume(
                            AudioProfileManager.STREAM_NOTIFICATION, volume, 0);
                    //*/
                } else {
                   // mExt.setRingerVolume(mAudioManager, volume);
                    mAudioManager.setStreamVolume(AudioProfileManager.STREAM_RING, volume, 0);
                    mAudioManager.setStreamVolume(AudioProfileManager.STREAM_NOTIFICATION, volume, 0);
                }

            } else {
               //*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
                if( volume == 0 ){
                	iconSwitch(streamType,false);
                }else{
                	iconSwitch(streamType,true);
                }
               //*/
                if (flag) {
                    mAudioManager.setAudioProfileStreamVolume(streamType,
                            volume, 0);
                } else {
                    //mExt.setVolume(mAudioManager, streamType, volume);
                    mAudioManager.setStreamVolume(streamType, volume, 0);
                }
            }
        }

        /**
         * Get whether the current ringermode is normal
         * 
         * @return true, the current ringer mode is VIBRATE or Silent,
         *         corresponding to the MEETING or Silent profile
         */
        private boolean isSilentProfileActive() {
            return mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
        }

        /** 
         * just for override 
         * @param seekBar the changed volume seekbar
         */
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        /**
         * Called when the progress of the seekbar changed
         * 
         * @param seekBar
         *            the seekbar whose progress is changed
         * @param progress
         *            the current progress level
         * @param fromTouch
         *            true if from the user's touch
         */
        public void onProgressChanged(SeekBar seekBar, int progress,
                boolean fromTouch) {
          //  Xlog.d(TAG, "onProgressChanged" + ": progress" + progress
          //          + " : fromTouch" + fromTouch);
            
            //*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
            if(fromTouch){
                if( progress == 0 ){
                	iconSwitch(mStreamType,false);
                }else{
                	iconSwitch(mStreamType,true);
                }
            }
            if(mStreamType == AudioProfileManager.STREAM_RING && fromTouch){
            	
                //Xlog.d(TAG, "xiaocui33 onProgressChanged" + ": progress" + progress
                //        + " : mLastProgress" + mLastProgress + "mSwitch =" +mSwitch + "SilentProfileActive =" + SilentProfileActive());
//                if((SilentProfileActive() || isVibrateProfileActive()) && mLastProgress != progress){
//                	mSwitch = false;
//                    numsg = mSwitchHandler.obtainMessage(SWITCH_RING_MODE, mSwitch);
//                    mSwitchHandler.removeMessages(SWITCH_RING_MODE);
//                    mSwitchHandler.sendMessageDelayed(numsg,0);	
//                    
//                }else{
//                	if(progress == 0 && mLastProgress != progress){
//                        mSwitch = true;
//                        numsg = mSwitchHandler.obtainMessage(SWITCH_RING_MODE, mSwitch);
//                        mSwitchHandler.removeMessages(SWITCH_RING_MODE);
//                        mSwitchHandler.sendMessageDelayed(numsg,0);
//                        //postVolumeChanged(sc.streamType, AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_VIBRATE);
//                    }else if (mSwitch == true){
//                    	mSwitch = false;
//                        numsg = mSwitchHandler.obtainMessage(SWITCH_RING_MODE, mSwitch);
//                        mSwitchHandler.removeMessages(SWITCH_RING_MODE);
//                        mSwitchHandler.sendMessageDelayed(numsg,0);
//                    }
//                }
//           }
                
                if((mLastProgress == 0) && progress !=0){
                        mSwitch = false;
                        numsg = mSwitchHandler.obtainMessage(SWITCH_RING_MODE, mSwitch);
                        mSwitchHandler.removeMessages(SWITCH_RING_MODE);
                        mSwitchHandler.sendMessageDelayed(numsg,0);
                	}else if(((mLastProgress != 0) && progress ==0)){
                        mSwitch = true;
                        setVolumeState();
                        numsg = mSwitchHandler.obtainMessage(SWITCH_RING_MODE, mSwitch);
                        mSwitchHandler.removeMessages(SWITCH_RING_MODE);
                        mSwitchHandler.sendMessageDelayed(numsg,0);
                	}
                }

            //*/
            
            mLastProgress = progress;
            if (!fromTouch) {
                return;
            }
            postSetVolume(progress);
        }

        /**
         * Post a runnable to start sampling when changing the volume
         * 
         * @param progress
         */
        void postSetVolume(int progress) {
            // Do the volume changing separately to give responsive UI
            mHandler.removeCallbacks(this);
            mHandler.post(this);
        }

        /**
         * Called when the user's touch leave the seekbar
         * 
         * @param seekBar
         *            the touched seekbar
         */
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (mRingtone != null && !mRingtone.isPlaying()) {
                sample();
            }
//*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
            if(isNormalActive()){
            	setVolumeState();
            }
            //if(DEBUG)
            //Xlog.i("xiaocui33","onStopTrackingTouch !!!!!");
//*/
            
        }

        /**
         * implements the runnable in which start sampling
         */
        public void run() {
            sample();
        }

        /**
         * Set the current volume to system and start playing ringtone
         */
        private void sample() {
            onSampleStarting(this);

           // Xlog.d(TAG, "sample, set system Volume " + mLastProgress);
           //*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
                setVolume(mStreamType, mLastProgress, true);
           /*/
            if (!isSilentProfileActive()) {
                setVolume(mStreamType, mLastProgress, true);
            }
           //*/

            if (mRingtone != null) {
               // Xlog.d(TAG, "stream type " + mStreamType + " play sample");
            	//*/Add by tyd xiaocui 0723 for the system volume and media volume
            	
                AudioManager audioManager = (AudioManager) mContext.getSystemService(
                        Context.AUDIO_SERVICE);
            	if(mStreamType == AudioProfileManager.STREAM_SYSTEM)
            	{
                    if (audioManager != null)
                    {
                        audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK);
                    }
            	}
            	else
                //*/ 
                mRingtone.play();
            }
        }

        /**
         * Stop playing ringtone
         */
        public void stopSample() {
            if (mRingtone != null) {
                //Xlog.d(TAG, "stream type " + mStreamType + " stop sample");
                mRingtone.stop();
            }
        }

        /**
         * get the seekbar object in the SeekbarVolumizer
         * 
         * @return the seekbar object
         */
        public SeekBar getSeekBar() {
            return mSeekBar;
        }

        /**
         * Change the volume by amount
         * 
         * @param amount
         *            the volume changed
         */
        public void changeVolumeBy(int amount) {
            mSeekBar.incrementProgressBy(amount);
            postSetVolume(mSeekBar.getProgress());
        }

        /**
         * Allowing a preference to re-apply a presentation of its internal
         * state.
         * 
         * @param volumeStore
         *            including last progress etc.
         */
        public void onSaveInstanceState(VolumeStore volumeStore) {
            if (mLastProgress >= 0) {
                volumeStore.mVolume = mLastProgress;
                volumeStore.mOriginalVolume = mOriginalVolume;
                volumeStore.mSystemVolume = mSystemVolume;
            }
        }

        /**
         * Allowing a preference to re-apply a presentation of its internal
         * state.
         * 
         * @param volumeStore
         *            including last progress etc.
         */
        public void onRestoreInstanceState(VolumeStore volumeStore) {
            if (volumeStore.mVolume != -1) {
                mLastProgress = volumeStore.mVolume;
                mOriginalVolume = volumeStore.mOriginalVolume;
                mSystemVolume = volumeStore.mSystemVolume;
                //*/ modified by tyd Jack 20150915 for, fixed bug when screen orientation changed
                //postSetVolume(mLastProgress);
                //*/
            }
        }
    }

    /**
     * In the volume adjust dialog, change the system volume, if the current
     * profile is active, adjust the seekbar volume
     * 
     * @author mtk54151
     * 
     */
    private class VolumeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(AudioManager.VOLUME_CHANGED_ACTION)) {
                int streamType = intent.getIntExtra(
                        AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                if (streamType != AudioManager.STREAM_RING) {
                    return;
                }
                if (mSeekBarVolumizer[1] != null) {
                    SeekBar seekBar = mSeekBarVolumizer[1].getSeekBar();
                    if (seekBar == null) {
                        return;
                    }
                    int volume = mAudioManager.getStreamVolume(streamType);
                   // Xlog.d(TAG, "AudioManager Volume " + volume);
                   // Xlog.d(TAG, "seekbar progress " + seekBar.getProgress());
                    if (seekBar.getProgress() != volume) {
                        if (volume >= 0) {
                            mSeekBarVolumizer[1].mSystemVolume = volume;
                            //Xlog.d(TAG, "is SystemVolume Changed " + volume);
                        }
                    }
                }
            }
        }
    }

    /**
     * Receiving the profile volume change from framework
     */
    private final AudioProfileListener mListener = new AudioProfileListener() {
        @Override
        public void onRingerVolumeChanged(int oldVolume, int newVolume,
                String extra) {
            //Xlog.d(TAG, extra + " :onRingerVolumeChanged from " + oldVolume
           //         + " to " + newVolume);
            if (mKey.equals(extra) && mSeekBarVolumizer[1] != null) {
                SeekBar seekBar = mSeekBarVolumizer[1].getSeekBar();
                if (seekBar == null) {
                    return;
                }
                if (seekBar.getProgress() != newVolume && newVolume >= 0) {
                    seekBar.setProgress(newVolume);
                }
            }
        }
    };
    
    //*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
    Message numsg = null;
    private static final String GENERAL_MODE="mtk_audioprofile_general";
    private final Object mSettingsLock = new Object();
    private static final int SWITCH_RING_MODE = 0;
    private static final int MSG_VIBRATE = 1;
    private static final int MSG_PLAY_SOUND = 2;
    private static final int MSG_STOP_SOUNDS = 3;
    public static final int VIBRATE_DELAY = 300;
    private static final int VIBRATE_DURATION = 300;
    private static final int BEEP_DURATION = 150;
    private boolean mHasVibrator;
	int [] mVolumeState = new int[SEEKBAR_TYPE.length];
	private static final int STREAM_MEDIA_ID = 0;
	private static final int STREAM_RING_ID = 1;
	private static final int STREAM_NOTI_ID = 2;
	private static final int STREAM_SYSTEM_ID = 3;
	private static final int STREAM_ALARM_ID = 4;
	private int num = 0;
	private int ringToneVolume = 0;
    
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder()
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
    .build();
    
    
    
    private final AudioProfileListener mRingerModeListener = new AudioProfileListener() {
        @Override
        public void onRingerModeChanged(int newRingerMode) {
        	int ringerMode = mAudioManager.getRingerModeInternal();
        	 if(mRingerMode == ringerMode){
        		 return;
        	 }
            switch (ringerMode) {
			case AudioManager.RINGER_MODE_NORMAL:
           	    messageOnPlay();
           	    iconSwitchUpdate(AudioManager.RINGER_MODE_NORMAL);
           	    mRingerMode = AudioManager.RINGER_MODE_NORMAL;
           	    getVolumeState();
           	    setSeekBarState(true);
           	    setNolmalIcon();
				break;
			case AudioManager.RINGER_MODE_VIBRATE:
				iconSwitchUpdate(AudioManager.RINGER_MODE_VIBRATE);
				mRingerMode = AudioManager.RINGER_MODE_VIBRATE;
				messageOnVibrate();
				break;
			case AudioManager.RINGER_MODE_SILENT:
				iconSwitchUpdate(AudioManager.RINGER_MODE_SILENT);
				mRingerMode = AudioManager.RINGER_MODE_SILENT;
		         SeekBar seekBar = mSeekBarVolumizer[1].getSeekBar();
		         if (seekBar != null) {
		             seekBar.setProgress(0);
		         }
				setMuteVolume();
		        setSeekBarState(false);
				break;

			default:
				break;
			}
            ringToneVolumeUpdate(ringerMode);
            //getSystemVolume();
            //if(DEBUG)
        	//Xlog.i("xiaocui33","onRingerModeChanged !!!!!!newRingerMode =" +newRingerMode + "ringerMode =" +ringerMode);
        }
    };
    
    private final Handler mSwitchHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SWITCH_RING_MODE:
                	boolean  mswitch = (boolean)msg.obj;
                    //if(DEBUG)
                	//Xlog.i("xiaocui33","handleMessage mSwitch = " + mSwitch);
                	synchronized(mSettingsLock){
                    	if(mSwitch){
                    		//setVolumeState();
                            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
                            mRingerMode = AudioManager.RINGER_MODE_VIBRATE;
                            messageOnVibrate();
                    	}else{
                       	    mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
                       	    mRingerMode = AudioManager.RINGER_MODE_NORMAL;
                       	    getVolumeState();
                       	    setSeekBarState(true);
                       	    setNolmalIcon();
                    	}
                	}
                    break;
                case MSG_VIBRATE:
                	onVibrate();
                case MSG_PLAY_SOUND:
                	onPlaySound();
                	break;
            }
        }
    };
    
    protected void onVibrate() {

        // Make sure we ended up in vibrate ringer mode
        if (mAudioManager.getRingerModeInternal() != AudioManager.RINGER_MODE_VIBRATE) {
            return;
        }
        if (mVibrator != null && mHasVibrator) {
            mVibrator.vibrate(VIBRATE_DURATION, VIBRATION_ATTRIBUTES);
            setMuteVolume();
        }
        iconSwitchUpdate(AudioManager.RINGER_MODE_VIBRATE);
        setSeekBarState(false);
    }
    
    protected void onPlaySound() {}
    
    public void messageOnVibrate(){
       // if(DEBUG)
    	//Xlog.i("xiaocui33","messageOnVibrate");
    	mSwitchHandler.removeMessages(MSG_VIBRATE);
    	mSwitchHandler.sendMessageDelayed(mSwitchHandler.obtainMessage(MSG_VIBRATE), VIBRATE_DELAY);
    }
    
    public void messageOnPlay(){
        //if(DEBUG)
    	//Xlog.i("xiaocui33","messageOnVibrate");
    	mSwitchHandler.removeMessages(MSG_PLAY_SOUND);
    	mSwitchHandler.sendMessageDelayed(mSwitchHandler.obtainMessage(MSG_PLAY_SOUND), VIBRATE_DELAY);
    }
    
    private void getVolumeState(){
    	
    	int mVolume = 0;
    	
    	for(int i = 0; i < SEEKBAR_TYPE.length; i++){
    		if(iSChangeVolume(i)){
            	//mVolume = mVolumeState[i];
            	SeekBar seekBar = mSeekBarVolumizer[i].getSeekBar();
    			if(i == STREAM_NOTI_ID ){
    				mVolume = Settings.System.getInt(mContentResolver, "normal_notifi", 10);
       			}
       			if(i == STREAM_SYSTEM_ID){
          			mVolume = Settings.System.getInt(mContentResolver, "normal_system", 8);
       			}
                //if(DEBUG)
               // Xlog.i("xiaocui33","getVolumeState mVolumeState =" +mVolume + "mKey =  " +mKey);
                if (seekBar == null) {
                    return;
                }
                if (mVolume >= 0) {
                    seekBar.setProgress(mVolume);
                 }
    		}
    		}
    	}
    
    private void setMuteVolume(){
    	
    	int mVolume = 0;
    	for(int i = 0; i < SEEKBAR_TYPE.length; i++){
    		if(iSChangeVolume(i)){
            	SeekBar seekBar = mSeekBarVolumizer[i].getSeekBar();
                //if(DEBUG)
                //Xlog.i("xiaocui33","setMuteVolume mVolume =" +mVolume + "mKey =  " +mKey);
                if (seekBar == null) {
                    return;
                }
                if (mVolume >= 0) {
                    seekBar.setProgress(0);
                 }
    		}
    		}
    	}
    
    private void  ringToneVolumeUpdate(int ringerMode){
    	switch (ringerMode) {
		case AudioManager.RINGER_MODE_NORMAL:

			break;
		case AudioManager.RINGER_MODE_VIBRATE:
			//mSeekBarVolumizer[1].getSeekBar().setProgress(0);
			break;
		case AudioManager.RINGER_MODE_SILENT:
			iconSwitchUpdate(AudioManager.RINGER_MODE_SILENT);
			mSeekBarVolumizer[1].getSeekBar().setProgress(0);
			break;

		default:
			break;
		}
    }
    private void setVolumeState(){
    	for(int i = 0; i < SEEKBAR_TYPE.length; i++){
    		if(iSChangeVolume(i)){
    			int  mVolume = mSeekBarVolumizer[i].getSeekBar().getProgress();
    			//mVolumeState[i] = mVolume;
    			if(i == STREAM_NOTI_ID ){
       			 Settings.System.putInt(mContentResolver, "normal_notifi", mVolume);
                 //if(DEBUG)
       			// Xlog.i("xiaocui33","setVolumeState mVolumeState normal_notifi= " + mVolume);
    			}
    			if(i == STREAM_SYSTEM_ID){
       			 Settings.System.putInt(mContentResolver, "normal_system", mVolume);
                 //if(DEBUG)
       			 //Xlog.i("xiaocui33","setVolumeState mVolumeState normal_system= " + mVolume);
    			}
    		}

    		}
    }
    
    private void setSeekBarState(boolean isSound){
    	for(int i = 0; i < SEEKBAR_TYPE.length; i++){
    		if(iSChangeVolume(i)){
    			SeekBar  mSeekBar = mSeekBarVolumizer[i].getSeekBar();
    			mSeekBar.setEnabled(isSound);
    		}
    		}
    }
 private boolean iSChangeVolume(int type){
	 if((type == STREAM_RING_ID) || (type == STREAM_MEDIA_ID ) || (type == STREAM_ALARM_ID )){
		 return false;
	 }else{
		 return true;
	 }
		 
 } 
 
 private void iconSwitchUpdate(int mode){
     //if(DEBUG)
	// Xlog.i("xiaocui33","iconSwitchUpdate mode =" + mode);
	 if(mView == null){
		 return;
	 }
    // if(DEBUG)
	 //Xlog.i("xiaocui33","iconSwitchUpdate mView mode =" + mode);
     switch (mode){
     case AudioManager.RINGER_MODE_VIBRATE:
    	 for(int i = 1; i < SEEKBAR_TYPE.length; i++){
    		 if(R.id.alarm_mute_button != CHECKBOX_VIEW_ID[i]){
        	     ImageView imageview = (ImageView) mView
        	             .findViewById(CHECKBOX_VIEW_ID[i]);
        	            if (imageview != null) {
        	                imageview.setImageResource(R.drawable.vibrator);
        	            }	
    		 }
    	 }
         break;
     case AudioManager.RINGER_MODE_NORMAL:
    	 for(int i = 1; i < SEEKBAR_TYPE.length; i++){
    		 
    	     ImageView imageview = (ImageView) mView
    	             .findViewById(CHECKBOX_VIEW_ID[i]);
    	            if (imageview != null) {
    	               // imageview.setImageResource(SEEKBAR_UNMUTED_RES_ID[i]);
    	            }	
    	 }
    	 break;
     case AudioManager.RINGER_MODE_SILENT:
    	 for(int i = 1; i < SEEKBAR_TYPE.length; i++){
    		 if(R.id.alarm_mute_button != CHECKBOX_VIEW_ID[i]){
        	     ImageView imageview = (ImageView) mView
        	             .findViewById(CHECKBOX_VIEW_ID[i]);
        	            if (imageview != null) {
        	                imageview.setImageResource(R.drawable.ic_audio_ring_mute);
        	            }	 
    		 }
    	 }
     	break;
 }
 }
 
 private void setNolmalIcon(){
	 
	 if(mView == null){
		 return;
	 }
	 for(int i = 1; i < SEEKBAR_TYPE.length; i++){
		 
	     ImageView imageview = (ImageView) mView
	             .findViewById(CHECKBOX_VIEW_ID[i]);
	            if (imageview != null) {
	               imageview.setImageResource(SEEKBAR_UNMUTED_RES_ID[i]);
	            }	
	 }
 }

 private void iconSwitch(int streamtype,boolean isnormal){
    // if(DEBUG)
	// Xlog.i("xiaocui33","iconSwitch streamtype =" +streamtype + "isnormal =" +isnormal);
	 if(mView == null){
		 return;
	 }
	 
	 for(int i = 0; i < SEEKBAR_TYPE.length; i++){
		 if((streamtype == SEEKBAR_TYPE[i]) && streamtype != AudioProfileManager.STREAM_RING){
		     ImageView imageview = (ImageView) mView
		             .findViewById(CHECKBOX_VIEW_ID[i]);
	            if (imageview != null) {
	            	if(isnormal){
		                imageview.setImageResource(SEEKBAR_UNMUTED_RES_ID[i]);
	            	}else{
		                imageview.setImageResource(R.drawable.ic_audio_ring_mute);
	            	}
	            }
		 }
	 }
 }
 
 private void iconSwitchIndex(int streamtype){
	 int id = 0;
     // if(DEBUG)
	 //Xlog.i("xiaocui33","iconSwitchIndex streamtype =" +streamtype);
	 if(mView == null){
		 return;
	 }
	 switch (streamtype) {
	case AudioProfileManager.STREAM_RING:
		id = R.id.ringer_mute_button;
		break;
	case AudioProfileManager.STREAM_MEDIA:
		id = R.id.media_mute_button;
		break;
		
	case AudioProfileManager.STREAM_NOTIFICATION:
		id = R.id.notification_mute_button;
		break;
		
	case AudioProfileManager.STREAM_SYSTEM:
		id = R.id.system_mute_button;
		break;
		
	case AudioProfileManager.STREAM_ALARM:
		id = R.id.alarm_mute_button;
		break;

	default:
		break;
	}
	 
     ImageView imageview = (ImageView) mView
             .findViewById(id);
	
  if(imageview != null){
      //if(DEBUG)
	  //Xlog.i("xiaocui33","iconSwitchIndex streamtype2 =" +streamtype + "id =" + id);
	  imageview.setImageResource(R.drawable.ic_audio_ring_mute);
  }	
 }
 
 private void SeticonSilent(){
	 if(mView == null){
		 return;
	 }
	 for(int i = 1; i < SEEKBAR_TYPE.length; i++){
		 
	     ImageView imageview = (ImageView) mView
	             .findViewById(CHECKBOX_VIEW_ID[i]);
	            if (imageview != null) {
	                imageview.setImageResource(R.drawable.ic_audio_ring_mute);
	            }
	 }
 }
 
 private void updateVolumeUi(){
	 if(isVibrateProfileActive()){
		 iconSwitchUpdate(AudioManager.RINGER_MODE_VIBRATE);
		 //setVolumeState();
		 setMuteVolume();
		 setSeekBarState(false);
		 //mSwitch = true;
	 }else if(SilentProfileActive()){
		 iconSwitchUpdate(AudioManager.RINGER_MODE_SILENT);
		 setMuteVolume();
		 setSeekBarState(false);
	 }else{
		 iconSwitchUpdate(AudioManager.RINGER_MODE_NORMAL);
		 //setMuteVolume();
		 setSeekBarState(true);
	 }
	//getSystemVolume();
 }
 
 private boolean isVibrateProfileActive() {
     return mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE;
 }
 
 private boolean SilentProfileActive(){
	    return mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
 }
 
 private boolean isNormalActive(){
	    return mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
}
 private boolean isRingNormalAndUpdate(){
//	 boolean  isNormal = false;
//	 isNormal =  mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
//	 if(isNormal){
//		 return true;
//	 }
//	 
//	 //getSystemVolume();
//	 if(SilentProfileActive()){
//		 SeticonSilent();
//	 }
	 return false;
 }
 
 
 private void getSystemVolume(){

 	for(int i = 0; i < SEEKBAR_TYPE.length; i++){
 		int mSystemVol = 0;
 		if(iSChangeVolume(i)){
 			mSystemVol=  mProfileManager .getStreamVolume(GENERAL_MODE, SEEKBAR_TYPE[i]);
 			mVolumeState[i] = mSystemVol;
            //if(DEBUG)
 			 //Xlog.i("xiaocui33","getSystemVolume mVolumeState =" +mVolumeState[i] );
 		}

 		}
 
 }
 
 private void setnormalVolumeState(int normalNotifi,int systemVol){
	 Settings.System.putInt(mContentResolver, "normal_notifi", normalNotifi);
	 Settings.System.putInt(mContentResolver, "normal_system", systemVol);
 }
 

    //*/

 

}
