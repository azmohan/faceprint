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

package com.android.systemui.qs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.settings.BrightnessController;
import com.android.systemui.settings.BrightnessController.BrightnessStateChangeCallback;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;

/**
 * Wrapper view with background which contains {@link QSPanel}
 */
/*/ Modified by tyd hanhao for customized qsTile 2015-06-23
public class QSContainer extends FrameLayout {
//*/
public class QSContainer extends LinearLayout {
//*/

    private int mHeightOverride = -1;
    private QSPanel mQSPanel;

    //Added by tyd hanhao for customized qsTile 2015-06-23
    private View mBrightnessView;
    private BrightnessController mBrightnessController;
    //*/

    public QSContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mQSPanel = (QSPanel) findViewById(R.id.quick_settings_panel);

        //*/ Added by tyd hanhao for customized qsTile 2015-06-23
        mBrightnessView = findViewById(R.id.brightness_dialog);

        mBrightnessController = new BrightnessController(getContext(),
                (ImageView) mBrightnessView.findViewById(R.id.brightness_icon),
                (TextView) mBrightnessView.findViewById(R.id.brightness_icon_title),
                (ToggleSlider) mBrightnessView.findViewById(R.id.brightness_slider));
        //*/
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateBottom();
    }

//*/ Added by tyd hanhao for customized qsTile 2015-06-23
    public void setListening(boolean listening) {
        if (listening) {
            mBrightnessController.registerCallbacks();
        } else {
            mBrightnessController.unregisterCallbacks();
        }
    }

    public void setBrightnessMirror(BrightnessMirrorController c) {
        super.onFinishInflate();
        ToggleSlider brightnessSlider = (ToggleSlider) mBrightnessView.findViewById(R.id.brightness_slider);
        ToggleSlider mirror = (ToggleSlider) c.getMirror().findViewById(R.id.brightness_slider);
        brightnessSlider.setMirror(mirror);
        brightnessSlider.setMirrorController(c);
        
        BrightnessStateChangeCallback callback = c.getBrightnessStateChangeCallback();
        if(null != mBrightnessController && null != callback) {
            mBrightnessController.addStateChangedCallback(callback);
        }
    }

    public void switchBrightnessViewDisplay(boolean show) {
        if(show) {
            mBrightnessView.setVisibility(View.VISIBLE);
        } else {
            mBrightnessView.setVisibility(View.GONE);

        }
    }
//*/

    /**
     * Overrides the height of this view (post-layout), so that the content is clipped to that
     * height and the background is set to that height.
     *
     * @param heightOverride the overridden height
     */
    public void setHeightOverride(int heightOverride) {
        mHeightOverride = heightOverride;
        updateBottom();
    }

    /**
     * The height this view wants to be. This is different from {@link #getMeasuredHeight} such that
     * during closing the detail panel, this already returns the smaller height.
     */
    public int getDesiredHeight() {
        if (mQSPanel.isClosingDetail()) {
            return mQSPanel.getGridHeight() + getPaddingTop() + getPaddingBottom();
        } else {
            return getMeasuredHeight();
        }
    }

    private void updateBottom() {
        int height = mHeightOverride != -1 ? mHeightOverride : getMeasuredHeight();
        setBottom(getTop() + height);
    }
}
