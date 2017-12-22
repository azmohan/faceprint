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

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.Configuration;
import com.android.settings.R;
import android.util.Log;
import android.content.ContentUris;
import android.os.Build;

//*/ freeme.chenming. 20160930. when set the phone ringtones, stop music
import android.media.AudioManager;
//*/


/**
 * The {@link CustomRingtonePickerActivity} allows the user to choose one from all of the
 * available ringtones. The chosen ringtone's URI will be persisted as a string.
 *
 * @see CustomRingtoneManager#ACTION_CUSTOM_RINGTONE_PICKER
 */
public final class CustomRingtonePickerActivity extends AlertActivity implements
        AdapterView.OnItemSelectedListener, Runnable, DialogInterface.OnClickListener,
        AlertController.AlertParams.OnPrepareListViewListener {

    private static final String TAG = "CustomRingtonePickerActivity";

    private static final int DELAY_MS_SELECTION_PLAYED = 300;
    
    private CustomRingtoneManager mCustomRingtoneManager;
    
    private Cursor mCursor;
    private Handler mHandler;
    
    /** The position in the list of the last clicked item. */
    private int mClickedPos = -1;
    
    /** The position in the list of the ringtone to sample. */
    private int mSampleRingtonePos = -1;
    
    /** The Uri to place a checkmark next to. */
    private Uri mExistingUri;
    
    /** The number of static items in the list. */
    private int mStaticItemCount;
    
    //*/ Modified by tyd xiaocui 2012-08-16 [tyd00438191] listview disappear when only one music
    private Uri ringtoneUri;
    //*/
    
    private DialogInterface.OnClickListener mRingtoneClickListener =
            new DialogInterface.OnClickListener() {

        /*
         * On item clicked
         */
        public void onClick(DialogInterface dialog, int which) {
            // Save the position of most recently clicked item
            mClickedPos = which;
            
            // Play clip
            playRingtone(which, 0);
        }
        
    };


             //*/added by zhuangxuan  tyd00436202
                @Override 
	  	public void onConfigurationChanged(Configuration config) { 
		  super.onConfigurationChanged(config); 
		} 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        Intent intent = getIntent();

        // Give the Activity so it can do managed queries
        mCustomRingtoneManager = new CustomRingtoneManager(this);

        // Get the types of ringtones to show
        int types = intent.getIntExtra(CustomRingtoneManager.EXTRA_RINGTONE_TYPE, -1);
        if (types != -1) {
            mCustomRingtoneManager.setType(types);
        }
        
        mCursor = mCustomRingtoneManager.getCursor();
        
        // Add for Toast when no Audio File on SD Card by Yetta on 2012-02-16 Start.
        if (mCursor == null) {
            String message = getResources().getString(R.string.zzzz_no_audio_file);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            finish();
        }
        if (mCursor != null && mCursor.getCount() == 0) {
            //*/Modify by Jiangshouting 2015.12.23.  for bug[tyd00590927]
            //mCursor.close();
            try  
            {  
                if(Integer.parseInt(Build.VERSION.SDK) < 14){
                    mCursor.close();
                }
            }catch(Exception e){
                android.util.Log.e(TAG, "CustomerRingTone's Close Cursor Exception:"+e);  
            }  
            //*/
            String message = getResources().getString(R.string.zzzz_no_audio_file);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            finish();
        }
        //*/modified by tyd xiaocui 2012-07-17 for fix bug custom ring die when SD don't have media file 
        else
        {
        // Add for Toast when no Audio File on SD Card by Yetta on 2012-02-16 End.
        
        // The volume keys will control the stream that we are choosing a ringtone for
        setVolumeControlStream(mCustomRingtoneManager.inferStreamType());

        // Get the URI whose list item should have a checkmark
        mExistingUri = intent
                .getParcelableExtra(CustomRingtoneManager.EXTRA_RINGTONE_EXISTING_URI);
        
        //*/ Modified by tyd xiaocui 2012-08-16 [tyd00438191] listview disappear when only one music
        ringtoneUri = ContentUris.withAppendedId(Uri.parse(mCursor.getString(2)), mCursor
                .getLong(0));
        //*/

        final AlertController.AlertParams p = mAlertParams;
        p.mCursor = mCursor;
        p.mOnClickListener = mRingtoneClickListener;
        p.mLabelColumn = MediaStore.Audio.Media.TITLE;
        p.mIsSingleChoice = true;
        p.mOnItemSelectedListener = this;
        p.mPositiveButtonText = getString(com.android.internal.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(com.android.internal.R.string.cancel);
        p.mPositiveButtonListener = this;
        p.mOnPrepareListViewListener = this;

        p.mTitle = intent.getCharSequenceExtra(CustomRingtoneManager.EXTRA_RINGTONE_TITLE);
        if (p.mTitle == null) {
            p.mTitle = getString(com.android.internal.R.string.ringtone_picker_title);
        }
        
        setupAlert();

        //*/ freeme.chenming. 20160930. when set the phone ringtones, stop music
        ((AudioManager) getSystemService(AUDIO_SERVICE))
                .requestAudioFocus(null, AudioManager.STREAM_RING,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        //*/
     }
    }

    public void onPrepareListView(ListView listView) {
        if (mClickedPos == -1) {
            mClickedPos = getListPosition(mCustomRingtoneManager.getRingtonePosition(mExistingUri));
        }
        
        // Put a checkmark next to an item.
        mAlertParams.mCheckedItem = mClickedPos;
    }

    /*
     * On click of Ok/Cancel buttons
     */
    public void onClick(DialogInterface dialog, int which) {
        boolean positiveResult = which == DialogInterface.BUTTON_POSITIVE;
        
        // Stop playing the previous ringtone
        stopAnyPlayingRingtone();
        
        if (positiveResult) {
            Intent resultIntent = new Intent();
            Uri uri = null;
            //*/ Modified by tyd xiaocui 2012-08-16 [tyd00438191] listview disappear when only one music
            if((mCursor != null && mCursor.getCount() == 1))
            	uri = ringtoneUri;
            else
            //*/
            uri = mCustomRingtoneManager.getRingtoneUri(getRingtoneManagerPosition(mClickedPos));
            
            if(mClickedPos == -1){
            	setResult(RESULT_CANCELED);
            	return;
            }
            //Log.i("xiaocui33", "CustomRingtonePickActivity mClickedPos ="+mClickedPos);
            if (uri != null) {
                int type = mCustomRingtoneManager.getType();
                if (type == CustomRingtoneManager.TYPE_RINGTONE || type == CustomRingtoneManager.TYPE_VIDEO_CALL) {
                    ContentValues values = new ContentValues(1);
                  //Add for Custom Ringtone by tyd xiaocui on 2012-07-10  xiaocui0710
                    values.put(MediaStore.Audio.Media.IS_RINGTONE, "-1");
                    //*/
                    getContentResolver().update(uri, values, null, null);
                }
                resultIntent.putExtra(CustomRingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri);
                setResult(RESULT_OK, resultIntent);
            } else {
                setResult(RESULT_CANCELED);
            }
        } else {
            setResult(RESULT_CANCELED);
        }

        getWindow().getDecorView().post(new Runnable() {
            public void run() {
                mCursor.deactivate();
            }
        });

        finish();
    }
    
    /*
     * On item selected via keys
     */
    public void onItemSelected(AdapterView parent, View view, int position, long id) {
        playRingtone(position, DELAY_MS_SELECTION_PLAYED);
    }

    public void onNothingSelected(AdapterView parent) {
    }

    private void playRingtone(int position, int delayMs) {
        mHandler.removeCallbacks(this);
        mSampleRingtonePos = position;
        mHandler.postDelayed(this, delayMs);
    }
    
    public void run() {
        Ringtone ringtone = null;
        //*/
        try{
        //*/
            if(!mCursor.isClosed()){
                if((mCursor != null && mCursor.getCount() == 1)||(mSampleRingtonePos == 0)){
        	    ringtone = mCustomRingtoneManager.getRingtone(getRingtoneManagerPosition(mSampleRingtonePos),ringtoneUri);
                } else{
        	    ringtone = mCustomRingtoneManager.getRingtone(getRingtoneManagerPosition(mSampleRingtonePos),null);
                }
            }else{
        	ringtone = mCustomRingtoneManager.getRingtone(getRingtoneManagerPosition(mSampleRingtonePos),null);
            }
        //*/
        }catch(Exception e){
            e.printStackTrace();
        }
        //*/
           
        if (ringtone != null) {
            ringtone.play();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopAnyPlayingRingtone();

        //*/ freeme.chenming. 20160930. when set the phone ringtones, stop music
        ((AudioManager) getSystemService(AUDIO_SERVICE))
                 .abandonAudioFocus(null);
        //*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAnyPlayingRingtone();
    }

    private void stopAnyPlayingRingtone() {
        if (mCustomRingtoneManager != null) {
            mCustomRingtoneManager.stopPreviousRingtone();
        }
    }

    private int getRingtoneManagerPosition(int listPos) {
        return listPos - mStaticItemCount;
    }
    
    private int getListPosition(int ringtoneManagerPos) {
        
        // If the manager position is -1 (for not found), return that
        if (ringtoneManagerPos < 0) return ringtoneManagerPos;
        
        return ringtoneManagerPos + mStaticItemCount;
    }
    
}
