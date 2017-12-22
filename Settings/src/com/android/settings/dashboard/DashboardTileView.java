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

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.ProfileSelectDialog;
import com.android.settings.R;
import com.android.settings.Utils;
//*/Modify by Jiangshouting 2015.12.25 for setting code transplant
import android.accounts.DroiAccount;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;
import com.android.settings.SettingsActivity;
//*/
//*/ Add by tyd fanwuyang 2017/08/07 for default setting layout jingdong
import android.os.SystemProperties;
//*/

public class DashboardTileView extends FrameLayout implements View.OnClickListener {

    private static final int DEFAULT_COL_SPAN = 1;

    private ImageView mImageView;
    private TextView mTitleTextView;
    private TextView mStatusTextView;
    private View mDivider;

    private int mColSpan = DEFAULT_COL_SPAN;

    private DashboardTile mTile;

    //*/ Add by tyd jiangshouting 2015.08.15 for listmode layout new Style 
    private boolean ifGridMode = true;
    private SharedPreferences gridSp;
    //*/

    public DashboardTileView(Context context) {
        this(context, null);
    }

    public DashboardTileView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //*/ Motify by tyd jiangshouting 2015.08.15 for listmode layout new Style 
        //final View view = LayoutInflater.from(context).inflate(R.layout.dashboard_tile, this);
        View view;
        gridSp = context.getSharedPreferences(SettingsActivity.IFGRIDMODESP, Context.MODE_PRIVATE);
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
        if(!ifGridMode){
            view = LayoutInflater.from(context).inflate(R.layout.dashboard_tile_listmode, this);
        }else{
            view = LayoutInflater.from(context).inflate(R.layout.dashboard_tile, this);
        }
        //*/

        mImageView = (ImageView) view.findViewById(R.id.icon);
        mTitleTextView = (TextView) view.findViewById(R.id.title);
        mStatusTextView = (TextView) view.findViewById(R.id.status);
        mDivider = view.findViewById(R.id.tile_divider);

        setOnClickListener(this);
        setBackgroundResource(R.drawable.dashboard_tile_background);
        setFocusable(true);

        //*/ Added by tyd liuchao for show guide,20150810
        mSharedPref = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        mEditor = mSharedPref.edit();
        //*/
    }

    public TextView getTitleTextView() {
        return mTitleTextView;
    }

    public TextView getStatusTextView() {
        return mStatusTextView;
    }

    public ImageView getImageView() {
        return mImageView;
    }

    public void setTile(DashboardTile tile) {
        mTile = tile;
    }

    public void setDividerVisibility(boolean visible) {
        mDivider.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    void setColumnSpan(int span) {
        mColSpan = span;
    }

    int getColumnSpan() {
        return mColSpan;
    }

    @Override
    public void onClick(View v) {
        showGuide = mSharedPref.getBoolean("showGuide", true);
        notificationShowGuide = mSharedPref.getBoolean("notificationShowGuide", true);
        
        if (mTile.fragment != null) {
            Utils.startWithFragment(getContext(), mTile.fragment, mTile.fragmentArguments, null, 0,
                    mTile.titleRes, mTile.getTitle(getResources()));
            //*/ Added by tyd liuchao for show guide,20150810
            if( mTile.getTitle(getResources()).equals(
                    getContext().getResources().getString(R.string.accessibility_settings_ext_title)) ) {
                if(showGuide){
                    mEditor.putBoolean("showGuide", false);
                    mEditor.commit();
                }
            } else if ( mTile.getTitle(getResources()).equals(
                    getContext().getResources().getString(R.string.notification_settings)) ) {
                if (notificationShowGuide) {
                    mEditor.putBoolean("notificationShowGuide", false);
                    mEditor.commit();
                }
            }

            // freeme.chenming, 20160414. Refctor for notify for system-update
            {
                ComponentName cn = new ComponentName(getContext(),getContext().getClass());
                sendRecommendIconShowChanged(getContext(), cn, (
                           mSharedPref.getBoolean("showGuide", true)
                        || mSharedPref.getBoolean("notificationShowGuide", true)
                        || mSharedPref.getBoolean("systemUpdateShowNotify", false)
                    ));
            }
	        //*/
        } else if (mTile.intent != null) {
            int numUserHandles = mTile.userHandle.size();
            if (numUserHandles > 1) {
                ProfileSelectDialog.show(((Activity) getContext()).getFragmentManager(), mTile);
            } else if (numUserHandles == 1) {
                getContext().startActivityAsUser(mTile.intent, mTile.userHandle.get(0));
            } else {
                getContext().startActivity(mTile.intent);
            }
       } else if (mTile.id == R.id.zhuoyi_account_settings) {
           //*/ Added by tyd liuchao for zhuoyi account login,20150819
            DroiAccount droiAccount = DroiAccount.getInstance(getContext());
            Intent intent = droiAccount.getLoginIntent();
            Context context = getContext();
            if(context != null && intent != null){
                context.startActivity(intent);
            }
            //*/
        }else if (mTile.id == R.id.manage_app_start) {
            //*/ freeme.luyangjie,20170609, for Application startup management
            Intent intent = new Intent();
            intent.setAction("android.intent.action.AutoRunManager");
            getContext().startActivity(intent);
            //*/
        //*/ freeme.fanwuyang, 2017/08/31, for CChelper 
        } else if (mTile.id == R.id.cchelper_settings) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.cutecomm.cchelper.xiaolajiao", "com.cutecomm.cchelper.xiaolajiao.activity.LoginActivity"));
            intent.setPackage("com.cutecomm.cchelper.xiaolajiao");
            Context context = getContext();
            if(context != null && intent != null){
                context.startActivity(intent);
            }
        //*/    
        //*/ freeme.fanwuyang, 2017/08/26, for Mulitiple Accounts 
        } else if (mTile.id == R.id.mulitiple_accounts) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.ptns.da.zy", "com.ptns.da.ui.Mainzytivityhi"));
            intent.setPackage("com.ptns.da.zy");
            Context context = getContext();
            if(context != null && intent != null){
                context.startActivity(intent);
            }
        //*/    
        //*/ freeme.fanwuyang, 2017/08/02, for Application Security 
        } else if (mTile.id == R.id.app_security) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.zhuoyi.security.lite", "com.freeme.sc.soft.lock.SL_SetPassWord"));
            intent.setPackage("com.zhuoyi.security.lite");
            Context context = getContext();
            if(context != null && intent != null){
                context.startActivity(intent);
            }
        //*/    
        //*/freeme.zhangjunxiang,20170803. for auto clean background
        } else if(mTile.id == R.id.auto_clean_background){
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.zhuoyi.security.lite", "com.freeme.sc.clean.task.CT_Settings"));
            intent.setPackage("com.zhuoyi.security.lite");
            Context context = getContext();
            if(context != null && intent != null){
                context.startActivity(intent);
            }
            //*/

        //*/ freeme.xuqian ,20170830 ,for  instructions
        } else if(mTile.id == R.id.instructions){

            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.droi.ebook", "com.droi.ebook.WebViewActivity"));
            intent.setPackage("com.droi.ebook");
            Context context = getContext();
            if(context != null && intent != null){
                context.startActivity(intent);
            }
        }
        //*/

    }

    //*/ Added by tyd liuchao for show guide,20150810
    public static void sendRecommendIconShowChanged(Context context, ComponentName comp, boolean show) {
        Intent intent = new Intent(TYD_ACTION_RECOMMEND_CHANGED);
        intent.putExtra(TYD_EXTRA_RECOMMEND_COMPONENT, comp);
        intent.putExtra(TYD_EXTRA_RECOMMEND_SHOW, show ? 1 : 0);
        android.provider.Settings.System
                .putInt(context.getContentResolver(), comp.flattenToShortString(), show ? 1 : 0);
        context.sendBroadcast(intent);
    }
    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mEditor;
    private boolean showGuide;
    private boolean notificationShowGuide;
    public static final String TYD_ACTION_RECOMMEND_CHANGED = "com.tydtech.action.RECOMMEND_CHANGED";
    public static final String TYD_EXTRA_RECOMMEND_COMPONENT = "com.tydtech.intent.extra.RECOMMEND_COMPONENT";
    public static final String TYD_EXTRA_RECOMMEND_SHOW = "com.tydtech.intent.extra.RECOMMEND_SHOW";
    //*/
}
