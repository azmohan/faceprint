/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.statusbar.policy;

import com.android.systemui.R;
import com.android.systemui.settings.BrightnessController;
import com.android.systemui.settings.BrightnessController.BrightnessStateChangeCallback;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowView;

import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Controls showing and hiding of the brightness mirror.
 */
public class BrightnessMirrorController {

    public long TRANSITION_DURATION_OUT = 150;
    public long TRANSITION_DURATION_IN = 200;

    private final ScrimView mScrimBehind;
    private final View mBrightnessMirror;
    private final View mPanelHolder;
    private final int[] mInt2Cache = new int[2];
    
    //*/ Added by droi hanhao for customized 2016-01-12
    private boolean mCustomized;
    private ImageView mIcon;
    private TextView mLabel;
    private BrightnessController.BrightnessStateChangeCallback mCallback;
    
    public BrightnessStateChangeCallback getBrightnessStateChangeCallback() {
        return mCallback;
    }
    //*/

    public BrightnessMirrorController(StatusBarWindowView statusBarWindow) {
        mScrimBehind = (ScrimView) statusBarWindow.findViewById(R.id.scrim_behind);
        mBrightnessMirror = statusBarWindow.findViewById(R.id.brightness_mirror);
        mPanelHolder = statusBarWindow.findViewById(R.id.panel_holder);
        
        //*/ Added by droi hanhao for customized 2016-01-12
        mCustomized = statusBarWindow.getResources().getBoolean(R.bool.config_customized_brightness);
        if(null != mBrightnessMirror) {
            mIcon = (ImageView)mBrightnessMirror.findViewById(R.id.brightness_icon);
            mLabel = (TextView)mBrightnessMirror.findViewById(R.id.brightness_icon_title);
        }
        
        updateIconVisibility(mCustomized);
        if(false == mCustomized) {
            // not response when we not customized this.
            return;
        }
        mCallback = new BrightnessStateChangeCallback() {
            
            @Override
            public void onBrightnessLevelChanged() {
                // do nothing
            }
            
            @Override
            public void onBrightnessAutomaticChanged(boolean automatic) {
                if(null != mIcon) {
                    mIcon.setImageResource(automatic ?
                            R.drawable.droi_qs_brightness_icon_auto :
                            R.drawable.droi_qs_brightness_icon_manual);
                }
                if(null != mLabel) {
                    mLabel.setText(automatic ?
                            R.string.droi_qs_brightness_title_auto :
                            R.string.droi_qs_brightness_title_manual);
                }
            }
        };
        //*/
    }
    
    private void updateIconVisibility(boolean show) {
        if(null != mIcon) {
            mIcon.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if(null != mLabel) {
            mLabel.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public void showMirror() {
        mBrightnessMirror.setVisibility(View.VISIBLE);
        mScrimBehind.animateViewAlpha(0.0f, TRANSITION_DURATION_OUT, PhoneStatusBar.ALPHA_OUT);
        outAnimation(mPanelHolder.animate())
                .withLayer();
    }

    public void hideMirror() {
        mScrimBehind.animateViewAlpha(1.0f, TRANSITION_DURATION_IN, PhoneStatusBar.ALPHA_IN);
        inAnimation(mPanelHolder.animate())
                .withLayer()
                .withEndAction(new Runnable() {
            @Override
            public void run() {
                mBrightnessMirror.setVisibility(View.INVISIBLE);
            }
        });
    }

    private ViewPropertyAnimator outAnimation(ViewPropertyAnimator a) {
        return a.alpha(0.0f)
                .setDuration(TRANSITION_DURATION_OUT)
                .setInterpolator(PhoneStatusBar.ALPHA_OUT);
    }
    private ViewPropertyAnimator inAnimation(ViewPropertyAnimator a) {
        return a.alpha(1.0f)
                .setDuration(TRANSITION_DURATION_IN)
                .setInterpolator(PhoneStatusBar.ALPHA_IN);
    }


    public void setLocation(View original) {
        original.getLocationInWindow(mInt2Cache);

        // Original is slightly larger than the mirror, so make sure to use the center for the
        // positioning.
        int originalX = mInt2Cache[0] + original.getWidth()/2;
        int originalY = mInt2Cache[1];
        mBrightnessMirror.setTranslationX(0);
        mBrightnessMirror.setTranslationY(0);
        mBrightnessMirror.getLocationInWindow(mInt2Cache);
        int mirrorX = mInt2Cache[0] + mBrightnessMirror.getWidth()/2;
        int mirrorY = mInt2Cache[1];
        mBrightnessMirror.setTranslationX(originalX - mirrorX);
        mBrightnessMirror.setTranslationY(originalY - mirrorY);
    }

    public View getMirror() {
        return mBrightnessMirror;
    }

    public void updateResources() {
        FrameLayout.LayoutParams lp =
                (FrameLayout.LayoutParams) mBrightnessMirror.getLayoutParams();
        lp.width = mBrightnessMirror.getResources().getDimensionPixelSize(
                R.dimen.notification_panel_width);
        lp.gravity = mBrightnessMirror.getResources().getInteger(
                R.integer.notification_panel_layout_gravity);
        mBrightnessMirror.setLayoutParams(lp);

        int padding = mBrightnessMirror.getResources().getDimensionPixelSize(
                R.dimen.notification_side_padding);
        mBrightnessMirror.setPadding(padding, mBrightnessMirror.getPaddingTop(),
                padding, mBrightnessMirror.getPaddingBottom());
    }
}