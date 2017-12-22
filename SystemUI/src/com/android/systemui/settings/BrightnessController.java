/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.systemui.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;

import com.android.systemui.R;

import java.util.ArrayList;
import android.os.SystemProperties;

public class BrightnessController implements ToggleSlider.Listener {
    private static final String TAG = "StatusBar.BrightnessController";
    //*/ Modifided by droi hanhao for customized 2016-01-12
    private static final boolean SHOW_AUTOMATIC_ICON = true; // default is false
    //*/

    /**
     * {@link android.provider.Settings.System#SCREEN_AUTO_BRIGHTNESS_ADJ} uses the range [-1, 1].
     * Using this factor, it is converted to [0, BRIGHTNESS_ADJ_RESOLUTION] for the SeekBar.
     */
    private static final float BRIGHTNESS_ADJ_RESOLUTION = 2048;
    //*/freeme.xuqian ,20170824 ,customized brightlight bar ,changed among 3 modes(min, mid, max)
    private static final int MODE_OTHER = -1;
    private static final int MODE_BEGIN = 0;
    private static final int MODE_MID = 1;
    private static final int MODE_END = 2;
    //*/

    private final int mMinimumBacklight;
    private final int mMaximumBacklight;

    private final Context mContext;
    private final ImageView mIcon;
    //*/ Added by droi hanhao for customized 2016-01-12
    private final TextView mLabel;
    //*/
    private final ToggleSlider mControl;
    private final boolean mAutomaticAvailable;
    private final IPowerManager mPower;
    private final CurrentUserTracker mUserTracker;
    private final Handler mHandler;
    private final BrightnessObserver mBrightnessObserver;
    
    //*/ Added by droi hanhao for customized 2016-01-12
    private boolean mCustomized;
    //*/

    private ArrayList<BrightnessStateChangeCallback> mChangeCallbacks =
            new ArrayList<BrightnessStateChangeCallback>();

    private boolean mAutomatic;
    private boolean mListening;
    private boolean mExternalChange;
    //*/freeme.xuqian ,20170824 ,customized brightlight bar ,changed among 3 modes(min, mid, max)
    private int mCustomMode = MODE_MID ;
    private boolean isDoPlusFlag = true ;
    //*/

    public interface BrightnessStateChangeCallback {
        public void onBrightnessLevelChanged();
        //*/ Added by droi hanhao for customized 2016-01-12
        public void onBrightnessAutomaticChanged(boolean automatic);
        //*/
    }

    /** ContentObserver to watch brightness **/
    private class BrightnessObserver extends ContentObserver {

        private final Uri BRIGHTNESS_MODE_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE);
        private final Uri BRIGHTNESS_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
        private final Uri BRIGHTNESS_ADJ_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ);

        public BrightnessObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (selfChange) return;
            try {
                mExternalChange = true;
                if (BRIGHTNESS_MODE_URI.equals(uri)) {
                    updateMode();
                    updateSlider();
                } else if (BRIGHTNESS_URI.equals(uri) && !mAutomatic) {
                    updateSlider();
                } else if (BRIGHTNESS_ADJ_URI.equals(uri) && mAutomatic) {
                    updateSlider();
                } else {
                    updateMode();
                    updateSlider();
                }
                for (BrightnessStateChangeCallback cb : mChangeCallbacks) {
                    cb.onBrightnessLevelChanged();
                }
            } finally {
                mExternalChange = false;
            }
        }

        public void startObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
            cr.registerContentObserver(
                    BRIGHTNESS_MODE_URI,
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(
                    BRIGHTNESS_URI,
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(
                    BRIGHTNESS_ADJ_URI,
                    false, this, UserHandle.USER_ALL);
        }

        public void stopObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
        }

    }
    
    //*/ Added by droi hanhao for customized 2016-01-12
    private OnClickListener mClickListener = new OnClickListener() {
        
        @Override
        public void onClick(View arg0) {
            /*/ freeme.xuqian ,20170824 ,customized brightlight bar ,changed among 3 modes(min, mid, max)
            if(mAutomatic) {
                setMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            } else {
                setMode(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            }
            /*/
            if(SystemProperties.get("ro.freeme.xlj_jingdong").equals("1")){
                setMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Log.v(TAG,"OnClick ,mCustomMode:"+mCustomMode);
                setBright();
            }else{
                if(mAutomatic) {
                    setMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                } else {
                    setMode(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                }
            }
            //*/




        }
    };
    //*/
    //*/ freeme.xuqian ,20170824 ,customized brightlight bar ,changed among 3 modes(min, mid, max)
    private int nextMode(){

        if(mCustomMode == MODE_BEGIN){
            isDoPlusFlag = true ;
        }else if(mCustomMode == MODE_END){
            isDoPlusFlag = false ;
        }
        Log.v(TAG,"nextMode(),mCustomMode:"+mCustomMode +",isDoPlusFlag:"+isDoPlusFlag);

        if(isDoPlusFlag){
            mCustomMode ++ ;
        }else{
            mCustomMode -- ;
        }
        return  mCustomMode ;

    }

    private void validateMode(){
        mCustomMode = mCustomMode < MODE_BEGIN ? MODE_BEGIN : mCustomMode ;
        mCustomMode = mCustomMode%(MODE_END+1);
    }


    private boolean isBeginPos(){

        return  mControl.getValue() == 0;

    }
    private boolean isEndPos(){

        return  mControl.getValue() == (mMaximumBacklight - mMinimumBacklight);

    }

    private boolean isDefaultPos(){

        return  mControl.getValue() == (mMaximumBacklight - mMinimumBacklight)/2;

    }

    private void setBright(){

        validateMode();
        int brightValue =  (mMaximumBacklight - mMinimumBacklight)/2 ;
        if(mCustomMode == MODE_BEGIN){

            brightValue = 0 ;

        }else if(mCustomMode == MODE_END){

            brightValue = mMaximumBacklight - mMinimumBacklight ;
        }

        final int val = brightValue + mMinimumBacklight;
        Log.v(TAG,"setBright,brightValue:"+brightValue+",mMinimumBacklight:"+mMinimumBacklight+",mMaximumBacklight:"+mMaximumBacklight+",val:"+val);
        setBrightness(val);
        AsyncTask.execute(new Runnable() {
                public void run() {
                    Settings.System.putIntForUser(mContext.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS, val,
                            UserHandle.USER_CURRENT);
                }
        });
        updateSlider();
        updateLabel(mAutomatic);
        nextMode();

    }
    //*/


    
    /** Constructor by droi hanhao , for cusomized 2016-01-12 */
    public BrightnessController(Context context, ImageView icon, TextView label, ToggleSlider control) {
        mContext = context;
        mIcon = icon;
        //*/ Added by droi hanhao for customized 2016-01-12
        mLabel = label;
        //*/
        mControl = control;
        mHandler = new Handler();
        mUserTracker = new CurrentUserTracker(mContext) {
            @Override
            public void onUserSwitched(int newUserId) {
                updateMode();
                updateSlider();
            }
        };
        mBrightnessObserver = new BrightnessObserver(mHandler);

        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mMinimumBacklight = pm.getMinimumScreenBrightnessSetting();
        mMaximumBacklight = pm.getMaximumScreenBrightnessSetting();

        mAutomaticAvailable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
        mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
        
        //*/ Added by droi hanhao for customized 2016-01-12
        mCustomized = context.getResources().getBoolean(
                                       R.bool.config_customized_brightness);
        //*/ freeme, gouzhouping, 20160928, for refactor.
        /*/freeme.xuqian ,20170824 ,customized brightlight bar ,changed among 3 modes(min, mid, max)
        if(mAutomaticAvailable) {
        	if(null != mIcon) {
                mIcon.setOnClickListener(mClickListener);
                mIcon.setVisibility(mCustomized ? View.VISIBLE : View.GONE);
            }
            if(null != mLabel) {
                mLabel.setOnClickListener(mClickListener);
                mLabel.setVisibility(mCustomized ? View.VISIBLE : View.GONE);
            }
        }
        /*/
        if(SystemProperties.get("ro.freeme.xlj_jingdong").equals("1")){
                if(null != mIcon) {
                    mIcon.setOnClickListener(mClickListener);
                    mIcon.setVisibility(mCustomized ? View.VISIBLE : View.GONE);
                }
                if(null != mLabel) {
                    mLabel.setOnClickListener(mClickListener);
                    mLabel.setVisibility(mCustomized ? View.VISIBLE : View.GONE);
                }
                updateSlider();
                updateMode();
        }else{

            if(mAutomaticAvailable) {

                if(null != mIcon) {
                    mIcon.setOnClickListener(mClickListener);
                    mIcon.setVisibility(mCustomized ? View.VISIBLE : View.GONE);
                }
                if(null != mLabel) {
                    mLabel.setOnClickListener(mClickListener);
                    mLabel.setVisibility(mCustomized ? View.VISIBLE : View.GONE);
                }
            }
        }
        //*/
        //*/
        //*/
    }

    public BrightnessController(Context context, ImageView icon, ToggleSlider control) {
        this(context, icon, null, control);
    }

    public void addStateChangedCallback(BrightnessStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
    }

    public boolean removeStateChangedCallback(BrightnessStateChangeCallback cb) {
        return mChangeCallbacks.remove(cb);
    }

    @Override
    public void onInit(ToggleSlider control) {
        // Do nothing
    }

    public void registerCallbacks() {
        if (mListening) {
            return;
        }

        mBrightnessObserver.startObserving();
        mUserTracker.startTracking();

        // Update the slider and mode before attaching the listener so we don't
        // receive the onChanged notifications for the initial values.
        updateMode();
        updateSlider();

        mControl.setOnChangedListener(this);
        mListening = true;
    }

    /** Unregister all call backs, both to and from the controller */
    public void unregisterCallbacks() {
        if (!mListening) {
            return;
        }

        mBrightnessObserver.stopObserving();
        mUserTracker.stopTracking();
        mControl.setOnChangedListener(null);
        mListening = false;
    }

    @Override
    public void onChanged(ToggleSlider view, boolean tracking, boolean automatic, int value,
            boolean stopTracking) {
        updateIcon(mAutomatic);
        updateLabel(mAutomatic);
        if (mExternalChange) return;

        if (!mAutomatic) {
            final int val = value + mMinimumBacklight;
            if (stopTracking) {
                MetricsLogger.action(mContext, MetricsLogger.ACTION_BRIGHTNESS, val);
            }
            setBrightness(val);
            if (!tracking) {
                AsyncTask.execute(new Runnable() {
                        public void run() {
                            Settings.System.putIntForUser(mContext.getContentResolver(),
                                    Settings.System.SCREEN_BRIGHTNESS, val,
                                    UserHandle.USER_CURRENT);
                        }
                    });
            }
        } else {
            final float adj = value / (BRIGHTNESS_ADJ_RESOLUTION / 2f) - 1;
            if (stopTracking) {
                MetricsLogger.action(mContext, MetricsLogger.ACTION_BRIGHTNESS_AUTO, value);
            }
            setBrightnessAdj(adj);
            if (!tracking) {
                AsyncTask.execute(new Runnable() {
                    public void run() {
                        Settings.System.putFloatForUser(mContext.getContentResolver(),
                                Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, adj,
                                UserHandle.USER_CURRENT);
                    }
                });
            }
        }

        for (BrightnessStateChangeCallback cb : mChangeCallbacks) {
            cb.onBrightnessLevelChanged();
        }
    }

    private void setMode(int mode) {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode,
                mUserTracker.getCurrentUserId());
    }

    private void setBrightness(int brightness) {
        try {
            mPower.setTemporaryScreenBrightnessSettingOverride(brightness);
        } catch (RemoteException ex) {
        }
    }

    private void setBrightnessAdj(float adj) {
        try {
            mPower.setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(adj);
        } catch (RemoteException ex) {
        }
    }

    private void updateIcon(boolean automatic) {
        if (mIcon != null) {
            //*/ Modified by droi hanhao for customized 2016-01-12
            /*/freeme.xuqian ,20170824 ,customized brightlight bar ,changed among 3 modes(min, mid, max)
            mIcon.setImageResource(automatic && SHOW_AUTOMATIC_ICON ?
                    R.drawable.droi_qs_brightness_icon_auto :
                    R.drawable.droi_qs_brightness_icon_manual);
            /*/
            if(SystemProperties.get("ro.freeme.xlj_jingdong").equals("1")){
                if(isBeginPos()){
                    mIcon.setImageResource(R.drawable.droi_qs_brightness_icon_min);
                }else if(isDefaultPos()){
                    mIcon.setImageResource(R.drawable.droi_qs_brightness_icon_default);
                }else if(isEndPos()) {
                    mIcon.setImageResource(R.drawable.droi_qs_brightness_icon_max);
                }else {
                    mIcon.setImageResource(automatic && SHOW_AUTOMATIC_ICON ?
                            R.drawable.droi_qs_brightness_icon_auto :
                            R.drawable.droi_qs_brightness_icon_manual);
                }

            }else{

                mIcon.setImageResource(automatic && SHOW_AUTOMATIC_ICON ?
                        R.drawable.droi_qs_brightness_icon_auto :
                        R.drawable.droi_qs_brightness_icon_manual);
            }
            //*/
            //*/
        }
    }
    
    /**
     * Added by droi hanhao for customized 2016-01-12
     */
    private void updateLabel(boolean automatic) {
        if(null != mLabel) {
              /*/ freeme.xuqian ,20170824 ,customized brightlight bar ,changed among 3 modes(min, mid, max)
               mLabel.setText(automatic && SHOW_AUTOMATIC_ICON ?
                    R.string.droi_qs_brightness_title_auto :
                    R.string.droi_qs_brightness_title_manual);
              /*/
               if(SystemProperties.get("ro.freeme.xlj_jingdong").equals("1")){
                   if(isBeginPos()){
                       mLabel.setText(R.string.droi_qs_brightness_title_begin);
                   }else if(isDefaultPos()){
                       mLabel.setText(R.string.droi_qs_brightness_title_default);
                   }else if(isEndPos()){
                       mLabel.setText(R.string.droi_qs_brightness_title_end);
                   }else{
                       mLabel.setText(automatic && SHOW_AUTOMATIC_ICON ?
                               R.string.droi_qs_brightness_title_auto :
                               R.string.droi_qs_brightness_title_manual);
                   }
               }else{

                   mLabel.setText(automatic && SHOW_AUTOMATIC_ICON ?
                           R.string.droi_qs_brightness_title_auto :
                           R.string.droi_qs_brightness_title_manual);
               }
              //*/

        }
    }

    /** Fetch the brightness mode from the system settings and update the icon */
    private void updateMode() {
        if (mAutomaticAvailable) {
            int automatic;
            automatic = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                    UserHandle.USER_CURRENT);
            mAutomatic = automatic != Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
            updateIcon(mAutomatic);
            //*/ Added by droi hanhao for customized 2016-01-12
            updateLabel(mAutomatic);
            //*/
        } else {
            mControl.setChecked(false);
            updateIcon(false /*automatic*/);
            //*/ Added by droi hanhao for customized 2016-01-12
            updateLabel(false /*automatic*/);
            //*/
        }
        
        //*/ freeme, gouzhouping, 20160929, when sliding the toggleslider the icon and label change
        for (BrightnessStateChangeCallback cb : mChangeCallbacks) {
            cb.onBrightnessAutomaticChanged(mAutomatic);
        }
        //*/
    }

    /** Fetch the brightness from the system settings and update the slider */
    private void updateSlider() {
        if (mAutomatic) {
            float value = Settings.System.getFloatForUser(mContext.getContentResolver(),
                    Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, 0,
                    UserHandle.USER_CURRENT);
            mControl.setMax((int) BRIGHTNESS_ADJ_RESOLUTION);
            mControl.setValue((int) ((value + 1) * BRIGHTNESS_ADJ_RESOLUTION / 2f));
        } else {
            int value;
            /*/ freeme.xuqian ,20170830 ,change default bright value
            value = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, mMaximumBacklight,
                    UserHandle.USER_CURRENT);
            /*/
            if(SystemProperties.get("ro.freeme.xlj_jingdong").equals("1")){

                value = Settings.System.getIntForUser(mContext.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, (mMaximumBacklight - mMinimumBacklight)/2,
                        UserHandle.USER_CURRENT);
            }else{
                value = Settings.System.getIntForUser(mContext.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, mMaximumBacklight,
                        UserHandle.USER_CURRENT);
            }

            //*/
            mControl.setMax(mMaximumBacklight - mMinimumBacklight);
            mControl.setValue(value - mMinimumBacklight);
        }
    }

}
