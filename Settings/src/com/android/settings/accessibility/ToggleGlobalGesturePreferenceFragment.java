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

package com.android.settings.accessibility;

import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.widget.ToggleSwitch;
import com.android.settings.widget.ToggleSwitch.OnBeforeCheckedChangeListener;

//*/Add by Jiangshouting 2015.12.25 for setting code transplant
import com.mediatek.HobbyDB.CustomHobbyService;
import com.android.settings.R;
import android.os.Bundle;
//*/


public class ToggleGlobalGesturePreferenceFragment
        extends ToggleFeaturePreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        //*/ADD  by tyd wangalei 2015.9.22 for Customs hobby sort
        CustomHobbyService mService=new CustomHobbyService(getActivity());
        if(mService.isExistData(R.string.accessibility_settings, R.string.accessibility_global_gesture_preference_title)){
            mService.update(R.string.accessibility_settings, R.string.accessibility_global_gesture_preference_title);
        }else{
            mService.insert(R.string.accessibility_settings, R.string.accessibility_global_gesture_preference_title, "com.android.settings.accessibility.ToggleGlobalGesturePreferenceFragment", 1,"");
        }
        //*/
    }
    
    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Global.putInt(getContentResolver(),
                Settings.Global.ENABLE_ACCESSIBILITY_GLOBAL_GESTURE_ENABLED, enabled ? 1 : 0);
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        mToggleSwitch.setOnBeforeCheckedChangeListener(new OnBeforeCheckedChangeListener() {
                @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                mSwitchBar.setCheckedInternal(checked);
                getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, checked);
                onPreferenceToggled(mPreferenceKey, checked);
                return false;
            }
        });
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.ACCESSIBILITY_TOGGLE_GLOBAL_GESTURE;
    }
}
