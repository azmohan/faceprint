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

package com.mediatek.settings;

import android.app.Activity;
import android.app.ActionBar;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.settings.DateTimeSettings;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.ZonePicker;
import com.android.settings.widget.SwitchBar;
import com.android.settings.wifi.WifiSettings;
//import com.mediatek.gemini.SimInfoEditor;
//import com.mediatek.gemini.SimManagement;

public class SetupWifiForOobe extends Activity {
    //implement OnClickListener
    private static final String TAG = "SetupWizardForOobe";
    private static final String EXTRA_IS_OOBE = "extra_is_oobe";

    private static final String EXTRA_WIFI_SETTINGS = "extra_wifi_settings";

    private static final String EXTRA_OOBE_SETTINGS = "extra_oobe_settings";

    private static final int ID_WIFI_SETTINGS = 7;

    private static final String OOBE_BASIC_STEP_TOTAL = "oobe_step_total";
    private static final String OOBE_BASIC_STEP_INDEX = "oobe_step_index";
    private static final String OOBE_HAS_RUN_KEY = "oobe_has_run";

    private static final int RESULT_OOBE_NEXT = 20;
    private static final int RESULT_OOBE_BACK = 21;
    private static final int SLOT_ALL = -1;
    private boolean mFirstRunMode = false;
    
    /*/ Added by Tyd Linguanrong for koobee style guide, 2014-5-28
    private ActionBar mActionBar;
    //*/

    private OnClickListener mBackListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            finishActivityByResult(RESULT_OOBE_BACK);
        }
    };
    private OnClickListener mNextListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            finishActivityByResult(RESULT_OOBE_NEXT);
        }
    };
    
    private boolean isFirstRun(){
    	SharedPreferences preferences = getSharedPreferences("wifi_oobe_state", Context.MODE_PRIVATE);
    	return preferences.getBoolean("is_first", true);
    }
     private void setFirstRunState(){
    	 SharedPreferences preferences = getSharedPreferences("wifi_oobe_state", Context.MODE_PRIVATE);
    	 Editor editor = preferences.edit();
    	 editor.putBoolean("is_first", false);
    	 editor.commit();
     }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*/ by Tyd Linguanrong for koobee style guide, 2014-5-28
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //*/
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setupwifi_for_oobe_layout);
        WifiManager mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if(isFirstRun()){
        	mWifiManager.setWifiEnabled(true);
        	setFirstRunState();
        }else{
        	int state = mWifiManager.getWifiState();
        	if(state == WifiManager.WIFI_STATE_ENABLED || state == WifiManager.WIFI_STATE_ENABLING){
        		mWifiManager.setWifiEnabled(true);
        	}else if(state == WifiManager.WIFI_STATE_DISABLED || state == WifiManager.WIFI_STATE_DISABLING){
        		mWifiManager.setWifiEnabled(false);
        	}else{
        		mWifiManager.setWifiEnabled(true);
        	}
        }

        /*/ Added by Tyd Linguanrong for koobee style guide, 2014-5-28
        mActionBar = getActionBar();
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        mActionBar.setDisplayHomeAsUpEnabled(true); 
        //*/

        /*/ Disabled by Linguanrong for oobe enalbe back first run, 2014-9-27
        mFirstRunMode = Settings.System.getInt(getContentResolver(), OOBE_HAS_RUN_KEY, 0) == 0;
        //*/
//        mExt = Utils.getMiscPlugin(this);
        mSwitchBar = (SwitchBar) findViewById(R.id.switch_bar);
        mSwitchBar.setChecked(true);
        initLayout();
    }
    private SwitchBar mSwitchBar;
    public SwitchBar getSwitchBar() {
        return mSwitchBar;
    }

    private void initLayout() {
//        FragmentManager manager = getFragmentManager();
//        FragmentTransaction transaction = manager.beginTransaction();
////        TextView title = (TextView)findViewById(R.id.title);
//        int stepId = getIntent().getIntExtra(EXTRA_OOBE_SETTINGS, -1);
//        switch (stepId) {
//            case ID_WIFI_SETTINGS :
////                title.setText(R.string.wifi_setup_wizard_title);
//                WifiSettings wifiSettings = new WifiSettings();
//                transaction.replace(R.id.fragment_container, wifiSettings);
//                break;
//            default :
//                break;
//        }
//        transaction.commit();

        //*/ Added by Tyd Linguanrong for koobee style guide, 2014-5-28
//        mActionBar.setTitle(title.getText().toString());
        Button BtnNext = (Button)findViewById(R.id.btn_next);
        BtnNext.setOnClickListener(mNextListener);
        Button BtnBack = (Button)findViewById(R.id.btn_previous);
        BtnBack.setOnClickListener(mBackListener);
        //*/
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finishActivityByResult(RESULT_OOBE_BACK);
        }
        return super.onKeyDown(keyCode, event);
    }

    private void finishActivityByResult(int resultCode) {
        Intent intent = new Intent();
        setResult(resultCode, intent);
//        if (resultCode == RESULT_OOBE_NEXT) {
//        	overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
//        } else {
//        	overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
//        }
        finish();
    }
}
