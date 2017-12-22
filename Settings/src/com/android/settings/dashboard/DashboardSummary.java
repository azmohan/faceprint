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
 * limitations under the License.
 */

package com.android.settings.dashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.HelpUtils;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;

import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;

import java.util.List;

//*/Modify by Jiangshouting 2015.12.25 for setting code transplant
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.ContentValues;
import android.preference.Preference;
import android.telephony.TelephonyManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.android.settings.LockScreenAndPassword;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settings.accessibility.AccessibilitySettingsExt;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.os.UserHandle;
import com.mediatek.HobbyDB.CustomHobbyDB;
import com.mediatek.HobbyDB.CustomHobbyService;
import com.mediatek.audioprofile.AudioProfileSettings;
import com.mediatek.audioprofile.Editprofile;
import android.content.SharedPreferences;
import com.mediatek.HobbyDB.*;
import com.android.internal.telephony.PhoneConstants;
import android.telephony.SubscriptionInfo;
import java.util.ArrayList; 
import com.mediatek.audioprofile.Editprofile;
//*/
//*/ freeme:fanwuyang on: 2017/08/02 remove magic track setting on jingdong
import android.os.SystemProperties;
//*/
//*/freeme.zhangshaopiao,20170804,fix bug about Can not open the OwnerInfoSettings page correctly
import com.android.settings.OwnerInfoSettings;
//*/
public class DashboardSummary extends InstrumentedFragment implements View.OnClickListener{
    private static final String LOG_TAG = "DashboardSummary";

    private LayoutInflater mLayoutInflater;
    private ViewGroup mDashboard;
    //*/ Add by tyd jiangshouting 2015.08.07 for adding a button to change setting layout 
    private boolean ifGridMode = true;
    private static final int MAX_TILE_NUM = 14;
    private SharedPreferences gridSp;
    //*/
    //*/add by tyd_wangalei 2015,9,21 Customs hobby sort
    private List<ContentValues> values;
    private  String firstLink;
    private  int firstTitleId;
    private  int firstParentId;
    private  String firstComment;
    private  String secondLink;
    private  int secondTitleId;
    private  int secondParentId;
    private  String secondComment;
    private  String thirdLink;
    private  int thirdTitleId;
    private  int thirdParentId;
    private  String thirdComment;
    
    private int STATEBAR_SETTING=112233;
    private int STATEBAR_SETTING_SWITCH=112234;
    private int STATEBAR_SETTING_NOTIFY=112235;
    
    
    private int SCHEDULE_POWER_SETTING =121211;
    private int SCHEDULE_POWER_SETTING_VIBRATE=121212;
    private int SCHEDULE_POWER_SETTING__OFFLINE=121213;
    private int SCHEDULE_POWER_SETTING_SCHPWRONOFF=121214;
    
    private TelephonyManager mTeleManager;
    private static  SubscriptionManager subscriptionManager;
    private  int numSlots;
    private int isAirplaneMode;
    private boolean isAirplaneFlag;
    
    private int locationMode;
    
    private TextView commonAccessibilityExt;
    private TextView commonLock;
    private TextView commonNotification;
    //*/

    private static final int MSG_REBUILD_UI = 1;
    private ISettingsMiscExt mExt;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REBUILD_UI: {
                    final Context context = getActivity();
                    rebuildUI(context);
                } break;
            }
        }
    };

    private class HomePackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            rebuildUI(context);
        }
    }
    private HomePackageReceiver mHomePackageReceiver = new HomePackageReceiver();

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DASHBOARD_SUMMARY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, R.string.help_uri_dashboard,
                getClass().getName());
    }

    @Override
    public void onResume() {
        super.onResume();
        //*/ Added by tyd liuchao for show guide,20150810
        showGuide = mSharedPref.getBoolean("showGuide", true);
        notificationShowGuide = mSharedPref.getBoolean("notificationShowGuide", true);
        //*/
        //*/ freeme.chenming, 20160414. notify for system-update
        systemUpdateShowGuide = mSharedPref.getBoolean("systemUpdateShowNotify", false);
        //*/
        //*/add by tyd wangalei 2015.10.14 for adjust  mode
        isAirplaneMode =    android.provider.Settings.System.getInt(getActivity().getContentResolver(),  
                android.provider.Settings.System.AIRPLANE_MODE_ON, 0) ; 
        isAirplaneFlag=(isAirplaneMode == 1)?true:false;
        locationMode =  android.provider.Settings.Secure.getInt(getActivity().getContentResolver(),  android.provider.Settings.Secure.LOCATION_MODE,
                android.provider.Settings.Secure.LOCATION_MODE_OFF);
        //*/
        //*/add by tyd wangalei 2015.10.14 for second menu sort
        if(ifGridMode){
             switch (values.size()) {
             case 3:
                setTitleBackground(commonLock, thirdParentId, thirdTitleId);
             case 2:
                setTitleBackground(commonNotification, secondParentId, secondTitleId);
             case 1:
                setTitleBackground(commonAccessibilityExt, firstParentId, firstTitleId);
                    break;
            default:
                break;
            }
        }
        //*/

        sendRebuildUI();

        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        getActivity().registerReceiver(mHomePackageReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mHomePackageReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mExt = UtilsExt.getMiscPlugin(this.getActivity());
        mLayoutInflater = inflater;
 
        //*/ MOtify by tyd jiangshouting 2015.08.07 for adding a button to change setting layout 
        gridSp = getActivity().getSharedPreferences(SettingsActivity.IFGRIDMODESP, Context.MODE_PRIVATE);
        /*/ Add by tyd fanwuyang 2017/08/07 for default setting layout jingdong
        if (SystemProperties.get("ro.freeme.xlj_jingdong").equals("1")) {
            ifGridMode = gridSp.getBoolean(SettingsActivity.IFGRIDMODESP,SettingsActivity.IFGRIDMODE_JINGDONG);
        } else {
            ifGridMode = gridSp.getBoolean(SettingsActivity.IFGRIDMODESP,SettingsActivity.IFGRIDMODE);
        }
        //*/
        //*/freeme.shancong,2017/08/07,for default setting layout jingdong and telecom
        if (SystemProperties.get("ro.freeme.xlj_jingdong").equals("1") || SystemProperties.get("ro.freeme.xlj_telecom").equals("1")
                || SystemProperties.get("ro.freeme.xlj_bv3b3").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3a").equals("1")
                || SystemProperties.get("ro.freeme.xlj_bv303z").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3h").equals("1") || SystemProperties.get("ro.freeme.xlj_bv3a3g").equals("1")) {
            ifGridMode = gridSp.getBoolean(SettingsActivity.IFGRIDMODESP,SettingsActivity.IFGRIDMODE_JINGDONG);
        } else {
            ifGridMode = gridSp.getBoolean(SettingsActivity.IFGRIDMODESP,SettingsActivity.IFGRIDMODE);
        }
        //*/
        View rootView = inflater.inflate(R.layout.dashboard_grid, container, false);
        if(ifGridMode){
            //final View rootView = inflater.inflate(R.layout.dashboard_grid, container, false);
            //*/ Added by tyd liuchao for single screen,20150810 
       
            commonAccessibilityExt = (TextView)rootView.findViewById(R.id.common_accessibility_ext);
            commonAccessibilityExt.setOnClickListener(this);
            //*/ freeme:fanwuyang on: 2017/08/02 remove magic track setting on jingdong
            if(SystemProperties.get("ro.freeme.xlj_jingdong").equals("1")) {
                commonAccessibilityExt.setVisibility(View.GONE);
            }
            //*/
            commonLock = (TextView)rootView.findViewById(R.id.common_lock);
            commonLock.setOnClickListener(this);
            commonNotification = (TextView)rootView.findViewById(R.id.common_notification);
            commonNotification.setOnClickListener(this);
            //*/
          
            //*/add by tyd wangalei 2015.10.13 for get sim total num
            mTeleManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            subscriptionManager = SubscriptionManager.from(getActivity());
            numSlots = subscriptionManager.getActiveSubscriptionInfoCount();
            Log.i("mylog", "==num=="+numSlots);
            //*/
            //*/add by tyd wangalei 2015.10.14 for adjust  mode
            isAirplaneMode = 	android.provider.Settings.System.getInt(getActivity().getContentResolver(),  
                android.provider.Settings.System.AIRPLANE_MODE_ON, 0) ; 
            isAirplaneFlag=(isAirplaneMode == 1)?true:false;
            locationMode =  android.provider.Settings.Secure.getInt(getActivity().getContentResolver(),  android.provider.Settings.Secure.LOCATION_MODE,
                android.provider.Settings.Secure.LOCATION_MODE_OFF);
            //*/
            //*/add by tyd_wangalei 2015.9.21 for second menu sort
            firstLink = AccessibilitySettingsExt.class.getName();
            firstTitleId= R.string.accessibility_settings_ext_title;
            firstParentId=R.string.accessibility_settings_ext_title;
            firstComment="";
            secondLink= Editprofile.class.getName();
            secondTitleId= R.string.notification_settings;
            secondParentId=R.string.notification_settings;
            secondComment="";
            thirdLink=LockScreenAndPassword.class.getName();
            thirdTitleId=R.string.lock_pwd_title;
            thirdParentId=R.string.lock_pwd_title;
            thirdComment="";
            CustomHobbyService mService=new CustomHobbyService(getActivity());
            values= mService.queryTopThree();
            switch (values.size()) {
            case 3:
            thirdLink= (String)values.get(2).get(CustomHobbyUtils.LINK);
                thirdTitleId=  (int) values.get(2).get(CustomHobbyUtils.CONTENT);
                thirdParentId=  (int) values.get(2).get(CustomHobbyUtils.PARENT_TITLE);
                thirdComment= (String)values.get(2).get(CustomHobbyUtils.COMMENT);
                setTitleText(commonLock, thirdParentId, thirdTitleId);
            case 2:
                secondLink= (String)values.get(1).get(CustomHobbyUtils.LINK);
                secondTitleId=  (int) values.get(1).get(CustomHobbyUtils.CONTENT);
                secondParentId=  (int) values.get(1).get(CustomHobbyUtils.PARENT_TITLE);
                secondComment= (String)values.get(1).get(CustomHobbyUtils.COMMENT);
                setTitleText(commonNotification, secondParentId, secondTitleId);
            case 1:
                firstLink= (String)values.get(0).get(CustomHobbyUtils.LINK);
                firstTitleId=  (int) values.get(0).get(CustomHobbyUtils.CONTENT);
                firstParentId=  (int) values.get(0).get(CustomHobbyUtils.PARENT_TITLE);
                firstComment=(String)values.get(0).get(CustomHobbyUtils.COMMENT);
                setTitleText(commonAccessibilityExt, firstParentId, firstTitleId);
                break;
            default:
               break;
            }
            //*/
        }else{
            rootView = inflater.inflate(R.layout.dashboard, container, false);
        }
        //*/
        mDashboard = (ViewGroup) rootView.findViewById(R.id.dashboard_container);

        //*/ Added by tyd liuchao for show guide,20150810
        mSharedPref = getActivity().getSharedPreferences(getActivity().getPackageName(), Context.MODE_PRIVATE);
        //*/
        return rootView;
    }


    //*/add by tyd wangalei 2015 10 13 for second menu sort 
    private void setTitleText(TextView Ext,int parentId,int titleId){
    	  if (STATEBAR_SETTING==parentId) {
	        	if(STATEBAR_SETTING_NOTIFY==titleId){
	        		Ext.setText(getActivity().getResources().getString(R.string.statebar_setting_notify));
	        	}else if(STATEBAR_SETTING_SWITCH==titleId){
	        		Ext.setText(getActivity().getResources().getString(R.string.statebar_setting_switch_sort));
	        	}
			}else if (SCHEDULE_POWER_SETTING==parentId) {
	        	if(SCHEDULE_POWER_SETTING_VIBRATE==titleId){
	        		Ext.setText(getActivity().getResources().getString(R.string.schedule_power_setting_vibrate));
	        	}else if(SCHEDULE_POWER_SETTING__OFFLINE==titleId){
	        		Ext.setText(getActivity().getResources().getString(R.string.schedule_power_setting_offline));
	        	}else if(SCHEDULE_POWER_SETTING_SCHPWRONOFF==titleId){
	        		Ext.setText(getActivity().getResources().getString(R.string.schedule_power_setting_schpwronoff));
	        	} 
			}else{
				Ext.setText(getActivity().getResources().getString(titleId));
			}
    	  Ext.setCompoundDrawablesWithIntrinsicBounds(null,getActivity().getResources().getDrawable(getCommonDrawable(parentId,titleId)),null,null);				
    }
    private void setTitleBackground(TextView Ext,int parentId,int titleId){
    	  Ext.setCompoundDrawablesWithIntrinsicBounds(null,getActivity().getResources().getDrawable(getCommonDrawable(parentId,titleId)),null,null);
    }
    //*/
    
    //*/add by tyd wangalei 2015.9.23 for sort common picture
    private int getCommonDrawable(int parentid,int titleid){
        Log.i("mylog",parentid+".."+titleid);
    	switch (parentid) {
		case R.string.wifi_settings_title:
			return R.drawable.common_wifi_settings_title;
		case R.string.radio_control_sim_title:
			if (numSlots==0&&titleid==R.string.network_settings_title) {
				return R.drawable.no_common_radio_sim_title;
			}else if(isAirplaneFlag&&titleid==R.string.network_settings_title){
				return R.drawable.no_common_radio_sim_title;
			}else{
				return R.drawable.common_radio_control_sim_title;
			}
		case R.string.bluetooth_settings_title:
			return R.drawable.common_bluetooth_settings_title;
		case R.string.login_account_droi:
			return R.drawable.common_login_account_droi;
		case R.string.account_settings_title:
			return R.drawable.common_account_settings_title;
		case R.string.location_settings_title:
			if (0==locationMode) {
				return R.drawable.no_common_location_settings_title;
			}else{
				return R.drawable.common_location_settings_title;
			}
		case R.string.lock_pwd_title:
			if (numSlots==0&&titleid==R.string.sim_lock_settings_category) {
				return R.drawable.no_common_lock_pwd_title;
			}else if(isAirplaneFlag&&titleid==R.string.sim_lock_settings_category){
				return R.drawable.no_common_lock_pwd_title;
			}else{
				return R.drawable.common_lock_pwd_title;
			}
		case R.string.security_settings_title:
			return R.drawable.common_security_settings_title;
		case R.string.privacy_settings:
			return R.drawable.common_privacy_settings;
		case R.string.display_settings:
			return R.drawable.common_display_settings;
		case R.string.notification_settings:
			return R.drawable.common_notification_settings;
		case R.string.statusbar_settings:
			return R.drawable.common_statusbar_settings;
		case R.string.accessibility_settings_ext_title:
			return R.drawable.common_accessibility_settings_ext_title;
		case R.string.hotknot_settings_title:
			return R.drawable.common_hotknot_settings_title;
		case R.string.storage_battery_settings:
			return R.drawable.common_storage_battery_settings;
		case R.string.applications_settings:
			return R.drawable.common_applications_settings;
		case R.string.date_and_time_settings_title:
			return R.drawable.common_date_and_time_settings_title;
		case R.string.language_settings:
			return R.drawable.common_language_settings;
		case R.string.development_settings_title:
			return R.drawable.common_development_settings_title;
		case R.string.about_settings:
			return R.drawable.common_about_settings;
		case  R.string.accessibility_settings:
			return R.drawable.common_accessibility_settings;
		case  R.string.print_settings:
			return R.drawable.common_print_settings;
		case  R.string.schedule_power_on_off_settings_title:
			return R.drawable.common_schedule_power_on_off_settings_title;
		case  R.string.sensor_calibration_title:
			return R.drawable.common_sensor_calibration_title;
		case R.string.radio_controls_title:
			switch (titleid) {
			case  R.string.accessibility_settings:
				return R.drawable.common_accessibility_settings;
			case  R.string.print_settings:
				return R.drawable.common_print_settings;
			case  R.string.schedule_power_on_off_settings_title:
				return R.drawable.common_schedule_power_on_off_settings_title;
			case  R.string.sensor_calibration_title:
				return R.drawable.common_sensor_calibration_title;
			default:
				return R.drawable.common_accessibility_settings;
			}
		case  112233:
			return R.drawable.common_statusbar_settings;
		case  121211:
			return R.drawable.common_schedule_power_on_off_settings_title;
		default:
			return R.drawable.common_accessibility_settings;
		}
    }
    //*/

    private void rebuildUI(Context context) {
        if (!isAdded()) {
            Log.w(LOG_TAG, "Cannot build the DashboardSummary UI yet as the Fragment is not added");
            return;
        }

        long start = System.currentTimeMillis();
        final Resources res = getResources();

        mDashboard.removeAllViews();

        List<DashboardCategory> categories =
                ((SettingsActivity) context).getDashboardCategories(true);

        final int count = categories.size();
        
        //*/Added by tyd liuchao
        ArrayList<String> tileList = new ArrayList<String>();
        //*/

        for (int n = 0; n < count; n++) {
            DashboardCategory category = categories.get(n);
            
            //*/Motify by tyd jiangshouting 2015.09.15 for Setting new UI style.
            //View categoryView = mLayoutInflater.inflate(R.layout.dashboard_category, mDashboard,
                    //false);
            View categoryView;
            if(!ifGridMode){
                categoryView = mLayoutInflater.inflate(R.layout.dashboard_category_listmode, mDashboard,
                    false);
            }else{
                categoryView = mLayoutInflater.inflate(R.layout.dashboard_category, mDashboard,
                    false);
            }
            //*/

            TextView categoryLabel = (TextView) categoryView.findViewById(R.id.category_title);
            categoryLabel.setText(category.getTitle(res));

            ViewGroup categoryContent =
                    (ViewGroup) categoryView.findViewById(R.id.category_content);

            final int tilesCount = category.getTilesCount();
            for (int i = 0; i < tilesCount; i++) {
                DashboardTile tile = category.getTile(i);

                DashboardTileView tileView = new DashboardTileView(context);
                updateTileView(context, res, tile, tileView.getImageView(),
                        tileView.getTitleTextView(), tileView.getStatusTextView());

                tileView.setTile(tile);

                categoryContent.addView(tileView);
                //*/ MOtify by tyd jiangshouting 2015.08.07 for adding a button to change setting layout 
                if(ifGridMode){
                    tileList.add(tileView.getTitleTextView().getText().toString());
                    if (categoryContent.getChildCount() >= MAX_TILE_NUM) {
                        break;
                    }
                }
                //*/
            }
            //*/ Motify by tyd jiangshouting 2015.08.07 for adding a button to change setting layout 
            if((n == count - 1) && ifGridMode) {
                DashboardTileView tileAboutView = new DashboardTileView(context);
                DashboardTile tileAboutSetting = new DashboardTile();
                tileAboutSetting.title = getString(R.string.about_settings);
                tileAboutSetting.iconRes = R.drawable.ic_settings_about_2;
                tileAboutSetting.fragment = "com.android.settings.DeviceInfoSettings";
                updateTileView(context, res, tileAboutSetting, tileAboutView.getImageView(),
                        tileAboutView.getTitleTextView(), tileAboutView.getStatusTextView());
                tileAboutView.setTile(tileAboutSetting);
                categoryContent.addView(tileAboutView);

                String[] titles = new String[MAX_TILE_NUM];
                for(int i = 0; i < tileList.size(); i++) {
                    titles[i] = tileList.get(i);
                }
                Bundle bundle = new Bundle();
                bundle.putStringArray("dashBoardTile", titles);

                DashboardTileView tileMoreView = new DashboardTileView(context);
                DashboardTile tileMoreSetting = new DashboardTile();
                tileMoreSetting.title = getString(R.string.radio_controls_title);
                tileMoreSetting.iconRes = R.drawable.ic_system_settings_more_2;
                tileMoreSetting.fragment = "com.android.settings.MoreSystemSettings";
                tileMoreSetting.fragmentArguments = bundle;
                updateTileView(context, res, tileMoreSetting, tileMoreView.getImageView(),
                        tileMoreView.getTitleTextView(), tileMoreView.getStatusTextView());
                tileMoreView.setTile(tileMoreSetting);
                categoryContent.addView(tileMoreView);
                //*/
            }

            // Add the category
            mDashboard.addView(categoryView);
        }
        long delta = System.currentTimeMillis() - start;
        Log.d(LOG_TAG, "rebuildUI took: " + delta + " ms");
    }

    private void updateTileView(Context context, Resources res, DashboardTile tile,
            ImageView tileIcon, TextView tileTextView, TextView statusTextView) {

        if (!TextUtils.isEmpty(tile.iconPkg)) {
            try {
                Drawable drawable = context.getPackageManager()
                        .getResourcesForApplication(tile.iconPkg).getDrawable(tile.iconRes, null);
                if (!tile.iconPkg.equals(context.getPackageName()) && drawable != null) {
                    // If this drawable is coming from outside Settings, tint it to match the color.
                    TypedValue tintColor = new TypedValue();
                    context.getTheme().resolveAttribute(com.android.internal.R.attr.colorAccent,
                            tintColor, true);
                    drawable.setTint(tintColor.data);
                }
                tileIcon.setImageDrawable(drawable);
            } catch (NameNotFoundException | Resources.NotFoundException e) {
                tileIcon.setImageDrawable(null);
                tileIcon.setBackground(null);
            }
        } else if (tile.iconRes > 0) {
            tileIcon.setImageResource(tile.iconRes);
        } else {
            tileIcon.setImageDrawable(null);
            tileIcon.setBackground(null);
            mExt.customizeDashboardTile(tile, tileIcon);
        }

        ///M: feature replace sim to uim
        tileTextView.setText(mExt.customizeSimDisplayString(
            tile.getTitle(res).toString(), SubscriptionManager.INVALID_SUBSCRIPTION_ID));

        //*/ Added by tyd liuchao for show guide,20150810
        if( tileTextView.getText().equals(res.getString(R.string.accessibility_settings_ext_title)) ) {
            tileTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, showGuide ? R.drawable.hight_light : 0, 0);
        } else if (tileTextView.getText().equals(res.getString(R.string.notification_settings))) {
            tileTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, notificationShowGuide ? R.drawable.hight_light : 0, 0);
        }
	    //*/
        //*/ freeme.chenming, 20160414. notify for system-update
        else if (tileTextView.getText().equals(res.getString(R.string.about_settings))) {
            tileTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, systemUpdateShowGuide ? R.drawable.hight_light : 0, 0);
        }
        //*/

        CharSequence summary = tile.getSummary(res);
        if (!TextUtils.isEmpty(summary)) {
            statusTextView.setVisibility(View.VISIBLE);
            statusTextView.setText(summary);
        } else {
            statusTextView.setVisibility(View.GONE);
        }
    }

    private void sendRebuildUI() {
        if (!mHandler.hasMessages(MSG_REBUILD_UI)) {
            mHandler.sendEmptyMessage(MSG_REBUILD_UI);
        }
    }

    //*/ Added by tyd liuchao for show guide,20150810
    private boolean showGuide = false;
    private SharedPreferences mSharedPref;
    private boolean notificationShowGuide = false;
    //*/
    //*/ freeme.chenming, 20160414. notify for system-update
    private boolean systemUpdateShowGuide = false;
    //*/

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id){
            case R.id.common_accessibility_ext:
            	/*/modify by tyd _wangalei 2015 10.13 for second menu sort
            	Utils.startWithFragment(getActivity(), AudioProfileSettings.class.getName(), null, null, 0,
                        R.string.notification_settings, null);
                 //*/
            	//*/add bu tyd wangalei 2015.10.13 for  second menu sort
                startLink(firstLink,firstParentId ,firstTitleId, firstComment);
                //*/
            	break;
            case R.id.common_notification:
            	/*/modify by tyd _wangalei 2015 10.13 for second menu sort
            	Utils.startWithFragment(getActivity(), AudioProfileSettings.class.getName(), null, null, 0,
                        R.string.notification_settings, null);
                 //*/
             	//*/add by tyd wangalei 2015.10.13 for  second menu sort
                startLink(secondLink, secondParentId,secondTitleId, secondComment);
                //*/
            	break;
            /*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
            case R.id.common_notification:
                Utils.startWithFragment(getActivity(), Editprofile.class.getName(), null, null, 0,
                        R.string.notification_settings, null);
                startLink(secondLink, secondParentId,secondTitleId, secondComment);
                break;
            case R.id.common_notification:
                Utils.startWithFragment(getActivity(), AudioProfileSettings.class.getName(), null, null, 0,
                        R.string.notification_settings, null);
                break;
            //*/
            case R.id.common_lock:
            	/*/modify by tyd _wangalei 2015 10.13 for second menu sort
            	Utils.startWithFragment(getActivity(), AudioProfileSettings.class.getName(), null, null, 0,
                        R.string.notification_settings, null);
                 //*/
             	//*/add bu tyd wangalei 2015.10.13 for  second menu sort
                startLink(thirdLink, thirdParentId,thirdTitleId, thirdComment);
                //*/
            	break;
        }
    }
  //*/add by tyd wangalei 2015.10.13 for  second menu sort
    private void startLink(String link,int parentid,int titleid,String comment){
    	if(!comment.equals("")){
    		if (comment.equals("activity")) {
    			  Intent actionIntent = new Intent(link);
    			  if(R.string.account_settings_title==parentid) {
    				  switch (titleid) {
    				  case R.string.add_account_label:
    					  /*/add by  tyd wangalei 2015 10 15 for second menu sort
							UserManager  um =	 (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
							 List<UserInfo> profiles = um.getProfiles(UserHandle.myUserId());
							   UserInfo userInfo =profiles.get(0);
							   actionIntent.putExtra("android.intent.extra.USER",userInfo.getUserHandle());
						 //*/
							break;
    				  default:
							break;
					}
					startActivity(actionIntent);  
				}else{
					  startActivity(actionIntent);  
				}
			}else{
				Intent packageIntent= new Intent(); 
				ComponentName componetName = new ComponentName(comment,link);  
				packageIntent.setComponent(componetName);  
				if (SCHEDULE_POWER_SETTING==parentid) {
					switch (titleid) {
					case 121212:
						packageIntent.putExtra("flag", 1);
						break;
					case 121213:
						packageIntent.putExtra("flag", 2);
						break;
					default:
						break;
					}
					startActivity(packageIntent);  
				}else if (R.string.notification_settings==parentid) {
					if(numSlots!=2){
						packageIntent.putExtra("Profile", "mtk_audioprofile_general");
						packageIntent.putExtra(PhoneConstants.SLOT_KEY,getSimId());
						switch (titleid) {
						case  R.string.ringtone_title:
							packageIntent.putExtra("Ringtone", "voice");
							break;
						case  R.string.zzzz_message_sound_title:
							packageIntent.putExtra("Ringtone", "message");
							break;
						case  R.string.notification_sound_title:
							packageIntent.putExtra("Ringtone", "notification");
							break;
						default:
							packageIntent.putExtra("Ringtone", "voice");
							break;
						}
					}else if(numSlots==2){
						packageIntent=null;
						packageIntent=new Intent("com.android.settings.sim.select");
						packageIntent.putExtra("Profile", "mtk_audioprofile_general");
						switch (titleid) {
						case  R.string.ringtone_title:
							packageIntent.putExtra("Ringtone", "voice");
							packageIntent.putExtra(Intent.EXTRA_TITLE,getActivity().getString( R.string.ringtone_title));
							break;
						case  R.string.zzzz_message_sound_title:
							packageIntent.putExtra("Ringtone", "message");
							packageIntent.putExtra(Intent.EXTRA_TITLE,getActivity().getString( R.string.zzzz_message_sound_title));
							break;
						case  R.string.notification_sound_title:
							packageIntent=null;
							packageIntent= new Intent(); 
							packageIntent.setComponent(componetName);  
							packageIntent.putExtra("Profile", "mtk_audioprofile_general");
							packageIntent.putExtra(PhoneConstants.SLOT_KEY,getSimId());
							packageIntent.putExtra("Ringtone", "notification");
							break;
						default:
							packageIntent.putExtra("Ringtone", "voice");
							packageIntent.putExtra(Intent.EXTRA_TITLE,getActivity().getString( R.string.ringtone_title));
							break;
						}
					}
					startActivity(packageIntent);  
				}else if(R.string.radio_control_sim_title==parentid){
					if (numSlots==0&&titleid==R.string.network_settings_title) {
						Toast.makeText(getActivity(), R.string.no_sim_card, Toast.LENGTH_SHORT).show();
					}else if(isAirplaneFlag&&titleid==R.string.network_settings_title){
						Toast.makeText(getActivity(), R.string.no_click_in_airmode, Toast.LENGTH_SHORT).show();
					}else{
						startActivity(packageIntent);  
					}
				}else if(R.string.lock_pwd_title==parentid){
					if (numSlots==0&&titleid==R.string.sim_lock_settings_category) {
						Toast.makeText(getActivity(), R.string.no_sim_card, Toast.LENGTH_SHORT).show();
					}else if(isAirplaneFlag&&titleid==R.string.sim_lock_settings_category){
						Toast.makeText(getActivity(), R.string.no_click_in_airmode, Toast.LENGTH_SHORT).show();
					}else{
						startActivity(packageIntent);  
						}
				}else{
					startActivity(packageIntent);  
				}
			}
    		 //*/add by tyd_wangalei 2015.9.21 for second menu sort
            CustomHobbyService mService=new CustomHobbyService(getActivity());
            if(mService.isExistData(parentid, titleid)){
    			mService.update(parentid, titleid);
    		}else{
    			mService.insert(parentid, titleid,link, 1, comment);
    		}
            //*/
    	}else{
    		if(R.string.accessibility_settings==parentid){
    			String EXTRA_TITLE = "title";
			    String EXTRA_SUMMARY = "summary";
			    String EXTRA_CHECKED = "checked";
				Bundle extras = new Bundle();
    			switch (titleid) {
				case R.string.accessibility_screen_magnification_title:
					    extras.putString(EXTRA_TITLE, getActivity().getString(
					                R.string.accessibility_screen_magnification_title));
					    extras.putCharSequence(EXTRA_SUMMARY, getActivity().getResources().getText(
					                R.string.accessibility_screen_magnification_summary));
					    extras.putBoolean(EXTRA_CHECKED, android.provider.Settings.Secure.getInt(getActivity().getContentResolver(),
					    		android.provider.Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0) == 1);
					break;
				case  R.string.accessibility_global_gesture_preference_title:
				        extras.putString(EXTRA_TITLE, getString(
				                R.string.accessibility_global_gesture_preference_title));
				        extras.putString(EXTRA_SUMMARY, getString(
				                R.string.accessibility_global_gesture_preference_description));
				        extras.putBoolean(EXTRA_CHECKED, android.provider.Settings.Global.getInt(getActivity().getContentResolver(),
				        		android.provider.Settings.Global.ENABLE_ACCESSIBILITY_GLOBAL_GESTURE_ENABLED, 0) == 1);
					break;
				default:
					break;
				}
    			Utils.startWithFragment(getActivity(),link, extras, null, 0,
  					  titleid, null);
    		}else if(R.string.location_settings_title==parentid){
    			if (0==locationMode) {
    				Toast.makeText(getActivity(), R.string.no_click_in_location_mode_off, Toast.LENGTH_SHORT).show();
				}else{
					Utils.startWithFragment(getActivity(),link, null, null, 0,
		  					  titleid, null);
				}
    		}else{
    		 //*/freeme.zhangshaopiao,20170804,fix bug about Can not open the OwnerInfoSettings page correctly
                if(link.equals("com.android.settings.OwnerInfoSettings")){
                    OwnerInfoSettings.show(DashboardSummary.this);
                    return;
                }
                //*/
                Utils.startWithFragment(getActivity(),link, null, null, 0, titleid, null);
    		}
    	}
    }
    //*/
    //*/add by tyd wangalei 2015.10.13 for get sim index 
    public static int getSimId() {
    	int simid;
        final List<SubscriptionInfo> subInfoList =
        		subscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            final int subInfoLength = subInfoList.size();
            
            for (int i = 0; i < subInfoLength; ++i) {
                final  SubscriptionInfo sir = subInfoList.get(i);
                if (sir != null) {
                	simid = sir.getSimSlotIndex();
                    return  simid;
                }
            }
        }
        return -1;
    }
    //*/
}
