/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.droi.recent;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.database.Cursor;
import android.provider.Settings;
import android.service.wallpaper.WallpaperService;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.systemui.R;
//import com.android.systemui.recents.AlternateRecentsComponent;
import com.droi.recents.RecentsConfiguration;
import com.droi.recent.RecentsMemCircularView;
import com.droi.recents.model.Task;
import com.droi.recents.model.TaskStack;
//import com.zhuoyi.security.numbers.marks.IMarksNumberSecurityService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
//Added by TYD Sean 2015-11-25 for changing visibility when invisible recent begin
import com.droi.recents.model.RecentsTaskLoader;
import com.droi.recents.misc.SystemServicesProxy;
import android.os.SystemClock;
import com.droi.recents.Recents;
//Added by TYD Sean 2015-11-25 for changing visibility when invisible recent end



public class RecentsActivity extends Activity {
    public static final boolean DEBUG = DebugUtils.DEBUG;
    public static final String TAG = "RecentsActivity";
    public static final String TOGGLE_RECENTS_INTENT = "com.android.systemui.recent.action.TOGGLE_RECENTS";
    public static final String PRELOAD_INTENT = "com.android.systemui.recent.action.PRELOAD";
    public static final String CANCEL_PRELOAD_INTENT = "com.android.systemui.recent.CANCEL_PRELOAD";
    public static final String CLOSE_RECENTS_INTENT = "com.android.systemui.recent.action.CLOSE";
    public static final String WINDOW_ANIMATION_START_INTENT = "com.android.systemui.recent.action.WINDOW_ANIMATION_START";
    public static final String PRELOAD_PERMISSION = "com.android.systemui.recent.permission.PRELOAD";
    public static final String WAITING_FOR_WINDOW_ANIMATION_PARAM = "com.android.systemui.recent.WAITING_FOR_WINDOW_ANIMATION";
    private static final String WAS_SHOWING = "was_showing";

    final static Uri CONTENT_URI =  Uri.parse("content://com.freeme.sc.common.db.uninstall.app/WhiteList");
    final static String COLUMN_NAME = "white_packageName";

    private RecentsPanelView mRecentsPanel;
    //Added by Sean 2015-12-08 for exit rencents screen when other app started ,such phone begin
    private RecentsHorizontalScrollView mRecentsContainer;
    //Added by Sean 2015-12-08 for exit rencents screen when other app started ,such phone end
    //*/ freeeme.xuqian ,20170915 , tap blank quick onekey-clean
    private View mRecentLineView ;
    //*/
    private IntentFilter mIntentFilter;
    private boolean mShowing;
    private boolean mForeground;

    View mClearAllTaskView;
    private RecentsMemCircularView mCircularView;
    TextView mMemInfo;
    private Handler handler = null;
    private static String mMemStr = "0 M / 0 M";
    private float mSweep = 0;
    long mUndoTime = 0;
    boolean isClicked = false;
    private RecentsConfiguration mConfig;

    //*/ Added by droi Sean 20160315
    private ArrayList<String> whitePackages = null;
    private List<String> killPkgList = new ArrayList<String>();
    private List<String> killPidList = new ArrayList<String>();
    private List<HashMap<String, String>> killPidInfoList = new ArrayList<HashMap<String, String>>();
    private ArrayList<String> mCleanTaskIgnoreApps;
    //*/

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CLOSE_RECENTS_INTENT.equals(intent.getAction())) {
                if (mRecentsPanel != null && mRecentsPanel.isShowing()) {
                    if (mShowing && !mForeground) {
                        // Captures the case right before we transition to
                        // another activity
                        mRecentsPanel.show(false);
                    }
                }
            } else if (WINDOW_ANIMATION_START_INTENT.equals(intent.getAction())) {
                if (mRecentsPanel != null) {
                    mRecentsPanel.onWindowAnimationStart();
                }
            }
        }
    };

    @Override
    public void onPause() {
        overridePendingTransition(
                R.anim.recents_return_to_launcher_enter,
                R.anim.recents_return_to_launcher_exit);
        mForeground = false;
        super.onPause();

        //Added by Sean 2015-12-08 for exit rencents screen when other app started ,such phone begin
        Recents recents  = Recents.getInstanceAndStartIfNeeded(getApplicationContext());
        if(recents != null){
            recents.setmLastToggleTime(SystemClock.elapsedRealtime());
        }
        //Added by Sean 2015-12-08 for exit rencents screen when other app started ,such phone end
    }

    @Override
    public void onStop() {
        mShowing = false;
        if (mRecentsPanel != null) {
            mRecentsPanel.onUiHidden();
        }
        //*/ Added by TYD hongchang.han 2010-10-21
        synchronizeWhitePackagesTo();
        // Unregister the RecentsService receiver
        unregisterReceiver(mServiceBroadcastReceiver);
        //*/ TYD
        //Added by TYD Sean 2015-11-25 for tyd00582864, changing visibility when invisible recent begin
        /*
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        SystemServicesProxy ssp = loader.getSystemServicesProxy();
        AlternateRecentsComponent.notifyVisibilityChanged(this, ssp, false);
        */
        //Added by TYD Sean 2015-11-25 for tyd00582864, changing visibility when invisible recent end

        super.onStop();
    }

    private void updateWallpaperVisibility(boolean visible) {
        int wpflags = visible ? WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER : 0;
        int curflags = getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
        if (wpflags != curflags) {
            getWindow().setFlags(wpflags, WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        }
    }

    public static boolean forceOpaqueBackground(Context context) {
        return WallpaperManager.getInstance(context).getWallpaperInfo() != null;
    }

    @Override
    public void onStart() {

        //*/ Added by TYD hongchang.han 2010-10-21
        initDefaultWhitePackage();
        synchronizeWhitePackagesFrom();
        //*/
        // Hide wallpaper if it's not a static image
        if (forceOpaqueBackground(this)) {
            updateWallpaperVisibility(false);
        } else {
            updateWallpaperVisibility(true);
        }
        mShowing = true;
        if (mRecentsPanel != null) {
            // Call and refresh the recent tasks list in case we didn't preload
            // tasks
            // or in case we don't get an onNewIntent
            mRecentsPanel.refreshRecentTasksList();
            mRecentsPanel.refreshViews();
        }

        //*/ Added by TYD hongchang.han 2010-10-21
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                handleIntent(getIntent(), true);
                if(View.VISIBLE == mClearAllTaskView.getVisibility()){
                    isClicked = false;
                    mSweep = 0;
                    mCircularView.setSweep(0, 0);
                    refreshMemInfo();
                    mCircularView.drawAnim();
                    mUndoTime = System.currentTimeMillis();
                }
            }
        }, 200);

        // Register the broadcast receiver to handle messages from our service
        IntentFilter filter = new IntentFilter();
        filter.addAction(Recents.ACTION_HIDE_RECENTS_ACTIVITY);
        filter.addAction(Recents.ACTION_TOGGLE_RECENTS_ACTIVITY);
        filter.addAction(Recents.ACTION_START_ENTER_ANIMATION);
        registerReceiver(mServiceBroadcastReceiver, filter);
        //*/
        super.onStart();
    }

    @Override
    public void onResume() {
        mForeground = true;
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        dismissAndGoBack();
    }

    public void dismissAndGoHome() {
        if(DEBUG) {
            Log.d(TAG, "dismissAndGoHome");
        }
        //*/Modified by tyd Sean 20151223 for: go home if recents is the top most task
        SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
        if (ssp.isRecentsTopMost(ssp.getTopMostTask(), null)) {
            // Return to Home
            if(DEBUG) {
                Log.d(TAG, "dismissAndGoHome  mRecentsPanel = " + mRecentsPanel);
            }
            if (mRecentsPanel != null) {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN, null);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivityAsUser(homeIntent, new UserHandle(UserHandle.USER_CURRENT));
                mRecentsPanel.show(false);
            }

        }
        //*/
    }

    public void dismissAndGoBack() {
        if (mRecentsPanel != null) {
            final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

            final List<ActivityManager.RecentTaskInfo> recentTasks =
                    am.getRecentTasks(2,
                            ActivityManager.RECENT_WITH_EXCLUDED |
                                    ActivityManager.RECENT_IGNORE_UNAVAILABLE |
                                    ActivityManager.RECENT_INCLUDE_PROFILES);
            if (recentTasks.size() > 1 &&
                    mRecentsPanel.simulateClick(recentTasks.get(1).persistentId)) {
                // recents panel will take care of calling show(false) through simulateClick
                return;
            }
            mRecentsPanel.show(false);
        }
        // freeme.qiuyi 20170729 fix return freemesuperpowersave when recents be deleted empty
        boolean isPowerSaveEnable = Settings.System.getInt(getContentResolver(),
                Settings.System.FREEME_SUPPER_POWER_SAVE_ENABLE, 0) == 1 ? true : false;
        boolean isBiglauncher = defaultLauncherisBiglauncher(); //freeme jianglingfeng added
        if (isPowerSaveEnable || isBiglauncher){
            dismissAndGoHome();
        }
        finish();
        //end qiuyi
    }

    //*/freeme jianglingfeng 20170731 fix bug for biglauncher
    private boolean defaultLauncherisBiglauncher() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo res = getApplicationContext().getPackageManager().resolveActivity(intent, 0);
        if (res.activityInfo == null) {
            return false;
        }
        if (res.activityInfo.packageName.equals("android")) {
            return false;
        } else {
            return "com.freemelite.biglauncher".equals(res.activityInfo.packageName);
        }
    }
    //*/end freeme
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addPrivateFlags(
                WindowManager.LayoutParams.PRIVATE_FLAG_INHERIT_TRANSLUCENT_DECOR);

        setContentView(R.layout.droi_status_bar_recent_panel);
        mRecentsPanel = (RecentsPanelView) findViewById(R.id.droi_recents_root);

        //Added by Sean 2015-12-08 for exit rencents screen when other app started ,such phone begin
        mRecentsContainer = (RecentsHorizontalScrollView)findViewById(R.id.droi_recents_container);
        //Added by Sean 2015-12-08 for exit rencents screen when other app started ,such phone end
        //*/ freeeme.xuqian ,20170915 , tap blank quick onekey-clean
        mRecentLineView = findViewById(R.id.droi_recents_linear_layout);
        mRecentsPanel.setOnClickListener(mClearAllTaskListener);
        mRecentLineView.setOnClickListener(mClearAllTaskListener);
        //*/
        mRecentsPanel.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.STATUS_BAR_DISABLE_EXPAND);

        final RecentTasksLoader recentTasksLoader = RecentTasksLoader.getInstance(this);
        recentTasksLoader.setRecentsPanel(mRecentsPanel, mRecentsPanel);
        mRecentsPanel.setMinSwipeAlpha(
                getResources().getInteger(R.integer.config_recent_item_min_alpha) / 100f);

        if (savedInstanceState == null ||
                savedInstanceState.getBoolean(WAS_SHOWING)) {
            handleIntent(getIntent(), (savedInstanceState == null));
        }
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(CLOSE_RECENTS_INTENT);
        mIntentFilter.addAction(WINDOW_ANIMATION_START_INTENT);
        registerReceiver(mIntentReceiver, mIntentFilter);
        super.onCreate(savedInstanceState);
        mClearAllTaskView = (View) findViewById(R.id.droi_clear_all_recents_view);
        mClearAllTaskView.setOnClickListener(mClearAllTaskListener);
        ((ImageButton) findViewById(R.id.droi_clear_all_recent_button))
                .setOnClickListener(mClearAllTaskListener);
        mMemInfo = (TextView) findViewById(R.id.droi_mem_info);
        mMemInfo.setText(mMemStr);
        mMemInfo.setOnClickListener(mClearAllTaskListener);

        mCircularView = (RecentsMemCircularView) findViewById(R.id.droi_circular_view);
        mCircularView.setSweep(0, 0);

        // */ Added by hanhao for oneKeyClear 2015-06-23
        whitePackages = new ArrayList<String>();
        // */
        //Added by Sean 2015-12-17 for handle screen off Action begin
        // Register the broadcast receiver to handle messages when the screen is turned off
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mSystemBroadcastReceiver, filter);
        //Added by Sean 2015-12-17 for handle screen off Action end
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(WAS_SHOWING, mRecentsPanel.isShowing());
    }

    @Override
    protected void onDestroy() {
        RecentTasksLoader.getInstance(this).setRecentsPanel(null, mRecentsPanel);
        unregisterReceiver(mIntentReceiver);
        //Added by Sean 2015-12-17 for handle screen off Action begin
        // Unregister the system broadcast receivers
        unregisterReceiver(mSystemBroadcastReceiver);
        //Added by Sean 2015-12-17 for handle screen off Action end
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent, true);
    }

    private void handleIntent(Intent intent,
                              boolean checkWaitingForAnimationParam) {
        super.onNewIntent(intent);
        if (TOGGLE_RECENTS_INTENT.equals(intent.getAction())) {
            if (mRecentsPanel != null) {
                if (mRecentsPanel.isShowing()) {
                    dismissAndGoBack();
                } else {
                    final RecentTasksLoader recentTasksLoader = RecentTasksLoader
                            .getInstance(this);
                    boolean waitingForWindowAnimation = checkWaitingForAnimationParam
                            && intent.getBooleanExtra(
                            WAITING_FOR_WINDOW_ANIMATION_PARAM, false);
                    mRecentsPanel.show(true,
                            recentTasksLoader.getLoadedTasks(),
                            recentTasksLoader.isFirstScreenful(),
                            waitingForWindowAnimation);
                }
            }
        }
    }

    boolean isForeground() {
        return mForeground;
    }

    boolean isActivityShowing() {
        return mShowing;
    }

    /** mark the tasks which were in white-list from zhuoyi safecenter */
    private boolean synchronizeWhitePackagesFrom() {

        ArrayList<String> safeCenterList = new ArrayList<String>();

        String safeCenterString = getWhiteListFromSafeCenter();
        Collections.addAll(safeCenterList, safeCenterString.split(","));

        if(false) { // for Debug
            for(String a : safeCenterList) {
                Log.d(TAG, "safeCenter Package: " + a);
            }
            for(String a : whitePackages) {
                Log.d(TAG, "defaut Package: " + a);
            }
        }

        for(String syncPkg : safeCenterList){
            if(whitePackages.contains(syncPkg)) {
                continue;
            } else {
                whitePackages.add(syncPkg);
            }
        }

        ArrayList<TaskDescription> tasks = RecentTasksLoader.getInstance(this).getLoadedTasks();
        if (tasks != null && tasks.size() != 0) {

            for (int i=0; i<tasks.size(); i++) {
                checkAndSyncWhitelist((TaskDescription)(tasks.get(i)));
            }
            mRecentsPanel.refreshViews();
        }

        return true;
    }


    private void synchronizeWhitePackagesTo() {
        createNewWhiteList();
        String whiltList = transferArray(whitePackages);
        if(DEBUG) {
            Log.d(TAG, "WhitePackageNames[" + whiltList + "]");
        }

        if(null != whiltList && !TextUtils.isEmpty(whiltList)) {

            int colum = sendWhiteListToSafeCenter(whiltList);
            if(-1 != colum) {
                Log.d(TAG, "sync whitelist success...");
            } else {
                Log.d(TAG, "sync whitelist failed...");
            }

            backupWhitePackage(whiltList);
        } else {
            Log.w(TAG, "whiteList is null, so we do not sync it...");
        }
    }

    public int sendWhiteListToSafeCenter(String whitePackages) {
        int colums = -1;
        if(null == whitePackages || TextUtils.isEmpty(whitePackages)) {
            Log.d(TAG, "white list is null, so we do not sync to safecenter");
            return colums;
        }

        try {
            long updateTime = System.nanoTime();
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME, whitePackages);

            colums = getContentResolver().update(CONTENT_URI, values, null, null);

            if(DEBUG) {
                Log.d(TAG, "udpate time " + (System.nanoTime() - updateTime));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, " send white list failed ..." + e.toString());
        }

        return colums;
    }

    String transferArray(ArrayList<String> lockedList) {
        StringBuilder builder = null;
        String result = "";

        if(null == lockedList || 0 == lockedList.size()) {
            return result;
        }

        builder = new StringBuilder();
        for(String pkgName : lockedList) {
            if(!pkgName.isEmpty()) {
                builder.append(pkgName + ",");
            }
        }

        result = builder.toString();
        if(result.length() > 0) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    public String getWhiteListFromSafeCenter() {
        String whitePackages = "";

        Cursor cursor = null;

        try{
            long startTime = System.nanoTime();
            cursor = getContentResolver().query(CONTENT_URI, null, null, null, null);

            if(null != cursor && cursor.getCount() > 0) {
                if(cursor.moveToNext()) {
                    whitePackages = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
                    Log.d(TAG, "get white list from Safecenter{" + whitePackages + "}.");
                }
            }
            if(cursor != null) {
                cursor.close();
            }

            if(DEBUG) {
                Log.d(TAG, "query time = " + (System.nanoTime() - startTime));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "" + e.toString());
        } finally {
            if(null != cursor && !cursor.isClosed()) {
                cursor.close();
            }
        }

        if("".equals(whitePackages)) {
            // get white list from backup file
            whitePackages = getBackupWhitePackage();
            if(DEBUG) {
                Log.d(TAG, "get whitePackages from local backup[" + whitePackages + "]");
            }
        }
        return whitePackages;
    }

    private void backupWhitePackage(String whiteList) {
        SharedPreferences sp = getSharedPreferences("LockedRecentsBackup", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        editor.putString("backup", whiteList);
        editor.apply();
        if(DEBUG) {
            Log.d(TAG, "backup white list ok");
        }
    }

    private String getBackupWhitePackage() {
        String backup = "";
        SharedPreferences sp = getSharedPreferences("LockedRecentsBackup", Context.MODE_PRIVATE);
        backup = sp.getString("backup", "");
        return backup;
    }

    private void onAllTaskViewsDismissed() {

    }

    private View.OnClickListener mClearAllTaskListener = new View.OnClickListener() {
        public void onClick(View v) {
            /*/ freeeme.xuqian ,20170915 , tap blank quick onekey-clean
            if (v == mClearAllTaskView) {
                return;
            }
            /*/
            if (v!=mMemInfo && v.getId()!=R.id.droi_clear_all_recent_button) {
                finish();
                return;
            }
            //*/
            if (mRecentsPanel.getCount() == 0) {
                notifySafeCenter();
                onAllTaskViewsDismissed();
                //*/ Added by TYD hongchang.han 2015-10-30
                // freeme.qiuyi 20170728 fix return freemesuperpowersave when recentslist have no recent
                boolean isPowerSaveEnable = Settings.System.getInt(getContentResolver(),
                        Settings.System.FREEME_SUPPER_POWER_SAVE_ENABLE, 0) == 1 ? true : false;
                boolean isBiglauncher = defaultLauncherisBiglauncher(); //freeme jianglingfeng added
                if (isPowerSaveEnable || isBiglauncher){
                    dismissAndGoHome();
                }
                finish();
                //end qiuyi
                //*/ TYD
                return;
            }
            hideVisibleViews();
            if (System.currentTimeMillis() - mUndoTime > 1500) {
                isClicked = true;
                refreshMemInfo();
                mCircularView.drawAnim();
                mUndoTime = System.currentTimeMillis();
            }
        }
    };

    public void refreshCircularView() {

    }

    public void refreshMemInfo() {
        DroiRecentWorker.post(new UpdateMemInfo());
    }

    class UpdateMemInfo implements Runnable {
        private MemHandler handler;

        public UpdateMemInfo() {
            handler = new MemHandler();
        }

        @Override
        public void run() {
            loadMemInfo();
            handler.sendEmptyMessageDelayed(0, 100);
        }
    }

    class MemHandler extends Handler {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(DebugUtils.DEBUG){
                DebugUtils.i(TAG, "set MemInfo " + mMemStr);
            }
            mMemInfo.setText(mMemStr);
            if (!isClicked) {
                Log.i(TAG, "setSweep  mSweep= " + mSweep);
                mCircularView.setSweep((int) (mSweep * 360),
                        mCircularView.getHeight());
            } else {
                mCircularView.upateTotal((int) (mSweep * 360));
                isClicked = false;
            }
        }
    };

    //*/ Added by Linguanrong for memory info 2013-10-09
    private void loadMemInfo() {
        ActivityManager mAm = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        mAm.getMemoryInfo(memInfo);

        //*/ freeme.xupeng, 20160903, fake rom and ram
        long mTotalSize = getDisplayRamSize(memInfo.totalMem);
        if (SystemProperties.get("ro.freeme.fake_rom_ram").equals("1")) {
            mTotalSize = memInfo.totalMem;
        }
        //*/
        if ("1".equals(SystemProperties.get("ro.tyd_extreme_lk_support"))
                && SystemProperties.getBoolean("persist.sys.ams.extreme_lk",
                true)) {
            //long mAvailable = (long) ((memInfo.availMem + (mTotalSize - memInfo.totalMem)) * 1.1);
            long mAvailable = (long) (memInfo.availMem + (mTotalSize - memInfo.totalMem) * 0.5); //370=>550
            //*/ freeme.xupeng, 20160903, fake rom and ram
            if (SystemProperties.get("ro.freeme.fake_rom_ram").equals("1")) {
                mAvailable = (long) memInfo.availMem;
            }
            long mUsed = mTotalSize - mAvailable;
            if(DEBUG) {
                Log.i(TAG, "memInfo.totalMem = " + memInfo.totalMem);
                Log.i(TAG, "memInfo.availMem = " + memInfo.availMem);
                Log.i(TAG, "mTotalSize= " + mTotalSize);
                Log.i(TAG, "mAvailable = " + mAvailable);
                Log.i(TAG, "mUsed = " + mUsed);
            }
            mMemStr = Formatter.formatShortFileSize(this, mAvailable) + " "
                    + getResources().getString(R.string.droi_ram_available) + " / "
                    + Formatter.formatFileSize(this, mTotalSize);

            Log.i(TAG, "mMemStr = " + mMemStr);

            mSweep = (float) (mUsed) / (mTotalSize);
            Log.i(TAG, "mSweep = " + mSweep);
        } else {
            long mUsed = memInfo.totalMem - memInfo.availMem;
            if(DEBUG) {
                Log.i(TAG, "memInfo.totalMem = " + memInfo.totalMem);
                Log.i(TAG, "memInfo.availMem = " + memInfo.availMem);
                Log.i(TAG, "mTotalSize= " + mTotalSize);
                Log.i(TAG, "mUsed = " + mUsed);
            }
            mMemStr = Formatter.formatShortFileSize(this, mUsed) + " / "
                    + Formatter.formatFileSize(this, mTotalSize);

            Log.i(TAG, "mMemStr = " + mMemStr);

            mSweep = (float) (mUsed / (1024 * 1024))
                    / (mTotalSize / (1024 * 1024));

            Log.i(TAG, "mSweep = " + mSweep);
        }

    }
    //*/

    void clearAllTasks() {
        // Filter home app
        if (!whitePackages.contains("com.freeme.home"))
            whitePackages.add("com.freeme.home");
        //Added by Sean 2015-12-09 for clear all recent task begin
        RecentTasksLoader.getInstance(this).clearUpRecentTasks(whitePackages);

        //*/ Modified by tyd Jack 20150518 for, Extreme Low memory killer
        if ("1".equals(SystemProperties.get("ro.tyd_extreme_lk_support"))
                && SystemProperties.getBoolean("persist.sys.ams.extreme_lk",
                true)) {
            ActivityManager mAm = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<RunningAppProcessInfo> runningAppProcesses = mAm
                    .getRunningAppProcesses();
            int runningSize = runningAppProcesses.size();

            //Added by Linguanrong clear list before get package info,2015-6-12
            killPkgList.clear();
            killPidList.clear();
            killPidInfoList.clear();

            for (int i = 0; i < runningSize; i++) {
                RunningAppProcessInfo info = runningAppProcesses.get(i);

                for (String pkg : info.pkgList) {
                    HashMap<String, String> killPidInfo = new HashMap<String, String>();
                    killPidInfo.put("pid", String.valueOf(info.pid));
                    killPidInfo.put("pkg", pkg);
                    killPidInfoList.add(killPidInfo);
                }

                if (info.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE
                        && info.pkgList != null) {
                    for (String pkg : info.pkgList) {
                        if (!whitePackages.contains(pkg)
                                && !checkIsCleanTaskIgnoreApps(pkg, info)) {
                            Log.d(TAG, "---------------pkg: "+ pkg + ", pid:" + info.pid);
                            killPkgList.add(pkg);
                            killPidList.add(String.valueOf(info.pid));
                        }
                    }
                }
            }
            if(DEBUG) {
                Log.i(TAG, "killPkgList = " + killPkgList);
                Log.i(TAG, "killPidList = " + killPidList);
            }

            getProcessTree(killPidList);
            if(DEBUG) {
                Log.i(TAG, "update process tree, killPidList = " + killPidList);
            }

        }

        //*/ Added by Linguanrong for statusbar bottom panel, 2014-9-25
        mRecentsPanel.postDelayed(new Runnable() {

            @Override
            public void run() {
                if("1".equals(SystemProperties.get("ro.tyd_extreme_lk_support")) &&
                        SystemProperties.getBoolean("persist.sys.ams.extreme_lk", true)){

                    try {
                        ActivityManager mAm = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                        mAm.forceKillProcessGroup(killPkgList, killPidList);
                    }catch(Exception e){
                        e.printStackTrace();
                        Log.e(TAG, "Exception = " + e);
                    }
                }
                notifySafeCenter();
            }
        }, 250);

        dismissAndGoHome();
        //*/
    }

    public void hideVisibleViews() {
        if (mRecentsPanel != null) {
            mRecentsPanel.hideVisibleViews();
        }
    }

    void notifySafeCenter() {
        Log.d(TAG, "notify safe center onecleartask finished ...");
        Intent intent = new Intent("android.intent.action.CLEAN_TASK_ZHUOYI_RECEIVER");
        /*/ freeme wanbao.zhang;Don't send white list to security center.
        intent.putStringArrayListExtra("WListFSystemUI", createNewWhiteList());
        //*/
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        sendBroadcast(intent);
    }

    private boolean checkIsCleanTaskIgnoreApps(String pkgName,
                                               RunningAppProcessInfo info) {

        if (mCleanTaskIgnoreApps == null) {
            mCleanTaskIgnoreApps = new ArrayList<String>(
                    Arrays.asList(getResources().getStringArray(
                            R.array.clean_task_ignore_apps)));
            addLiveWallpaperToIgnoreApps();

            Log.i(TAG, "mCleanTaskIgnoreApps = " + mCleanTaskIgnoreApps);
        }

        if (mCleanTaskIgnoreApps != null
                && mCleanTaskIgnoreApps.contains(pkgName)) {
            return true;
        }

        if (info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
                && "com.android.incallui".equals(info.processName)) {
            return true;
        }
        return false;
    }

    private void addLiveWallpaperToIgnoreApps() {

        PackageManager mPackageManager = getPackageManager();

        List<ResolveInfo> list = mPackageManager.queryIntentServices(
                new Intent(WallpaperService.SERVICE_INTERFACE),
                PackageManager.GET_META_DATA);
        for (ResolveInfo resolveInfo : list) {
            WallpaperInfo info = null;
            try {
                info = new WallpaperInfo(this, resolveInfo);
            } catch (Exception e) {
                Log.w(TAG, "Skipping wallpaper " + resolveInfo.serviceInfo, e);
                continue;
            }

            mCleanTaskIgnoreApps.add(info.getPackageName());
        }

        ComponentName currentDefaultHome = mPackageManager
                .getHomeActivities(list);

        if (currentDefaultHome != null) {
            mCleanTaskIgnoreApps.add(currentDefaultHome.getPackageName());
        }
    }

    private String getProcessTree(List<String> pidList) {
        if (pidList == null || pidList.size() == 0)
            return null;

        List<String> killUidList = new ArrayList<String>();
        List<String[]> pinfoList = new ArrayList<String[]>();
        BufferedReader br = null;
        try {
            java.lang.Process p = Runtime.getRuntime().exec("ps");
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;

            while ((line = br.readLine()) != null) {
                int i = 0;
                String[] pinfo = new String[3];
                StringTokenizer st = new StringTokenizer(line);
                while (st.hasMoreElements() && i <= 2) {
                    pinfo[i] = st.nextToken();
                    i++;
                }

                pinfoList.add(pinfo);

                if (pidList.contains(pinfo[1]) && !"system".equals(pinfo[0])
                        && pinfo[0].startsWith("u0_"))
                    killUidList.add(0, pinfo[0]);

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if(DEBUG) {
            Log.i(TAG, "getProcessTree : killUidList = " + killUidList);
            Log.i(TAG, "getProcessTree : killPidInfoList = " + killPidInfoList);
        }
        for (String[] pinfo : pinfoList) {
            if (!pidList.contains(pinfo[1]) && killUidList.contains(pinfo[0])) {
                String pid = pinfo[1];

                boolean ignore = false;
                for (HashMap<String, String> killPidInfo : killPidInfoList) {
                    ignore = pid.equals(killPidInfo.get("pid"))
                            && (mCleanTaskIgnoreApps != null && mCleanTaskIgnoreApps
                            .contains(killPidInfo.get("pkg")));
                    if (ignore) {
                        Log.i(TAG, "getProcessTree : ignore pidInfo = " + killPidInfo);
                        break;
                    }
                }
                if (!ignore)
                    pidList.add(0, pid);
            }
        }

        String result = "";
        for (String pid : pidList) {
            result += " " + pid;
        }
        return result;
    }

    // */Added by tyd YuanChengye 20131115, for get the int total ram size
    private static final long GIGA_BYTE_SIZE = 1024L * 1024 * 1024;

    /**
     * get the displayRamSize
     *
     * @param realSize
     * @return Modified by tyd hanhao 2015-07-28
     */
    private long getDisplayRamSize(long realSize) {
        // RAM size:
        // 0.0 < x <= 0.5 --> x = 0.5
        // 0.5 < x <= 1.0 --> x = 1.0
        // 1.0 < x <= 1.5 --> x = 1.5
        // 1.5 < x <= 2.0 --> x = 2.0
        if ((((float) realSize) / ((float) GIGA_BYTE_SIZE) - realSize
                / GIGA_BYTE_SIZE) >= 0.5) {
            return ((realSize / GIGA_BYTE_SIZE) + 1) * GIGA_BYTE_SIZE;
            //*/freeme.xuqian,20170911,for fake ram
        }else if((((float) realSize) / ((float) GIGA_BYTE_SIZE) - realSize
                / GIGA_BYTE_SIZE) == 0){
            return realSize;
            //*/
        } else {
            return (long) (((realSize / GIGA_BYTE_SIZE) + 0.5) * GIGA_BYTE_SIZE);
        }
    }

    // */

    ArrayList<String> createNewWhiteList() {
        // ArrayList<TaskStack> taskStacks = mRecentsView.getTaskStacks();

        ArrayList<TaskStack> taskStacks = null;
        if (null == taskStacks || taskStacks.size() <= 0) {
            Log.w(TAG, "recentTasks size == 0 ......");
            return whitePackages;
        }

        for (TaskStack stack : taskStacks) {
            ArrayList<Task> tasks = stack.getTasks();
            for (Task task : tasks) {
                String packageName = task.key.baseIntent.getComponent()
                        .getPackageName();
                boolean hasBeenIncluded = whitePackages.contains(packageName);
                boolean isLocked = task.isLockedToWhiteList;

                // */ [fix bug tyd00569505]
                if (isLocked && hasBeenIncluded) { // lockede by user and
                    // already existed in
                    // whitelist
                    continue;
                } else if (isLocked) { // locked by user and not exist in
                    // whitelist
                    whitePackages.add(packageName);
                } else if (hasBeenIncluded) { // not locked by user, but existed
                    // in whitelist
                    whitePackages.remove(packageName);
                } else {
                    // not locked by user either existed in whitlist, so we do
                    // nothing
                }
                // */
            }
        }

        return whitePackages;
    }

    private void initDefaultWhitePackage() {
        SharedPreferences sp = getSharedPreferences("LockedRecents",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        whitePackages.clear();
        if (!sp.getBoolean("isFirstRun", false)) {
            String whiteList[] = getResources().getStringArray(
                    R.array.clean_whitelist_default);
            for (String item : whiteList) {
                whitePackages.add(item);
            }

            editor.putBoolean("isFirstRun", true);
        }

        editor.commit();
        Log.d(TAG, ". . .");

        for (String a : whitePackages) {
            Log.d(TAG, ". " + a);
        }
    }

    public void updateCircleViewAlpha(float alpha) {
        mClearAllTaskView.setAlpha(alpha);
    }

    public void checkAndSyncWhitelist(TaskDescription td) {
        if (whitePackages != null && whitePackages.size() != 0) {
            for(String name : whitePackages) {
                if (td.packageName.equals(name)) {
                    td.mIsLocked = true;
                }
            }
        }
    }

    public void updateWhiteList(String packageName, boolean locked) {
        if(null == whitePackages) {
            whitePackages = new ArrayList<String>();
        }

        if(locked && !whitePackages.contains(packageName)) {
            whitePackages.add(packageName);
        } else if (!locked && whitePackages.contains(packageName)) {
            whitePackages.remove(packageName);
        }
    }

    /**
     * Broadcast receiver to handle messages from AlternateRecentsComponent.
     */
    final BroadcastReceiver mServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Recents.ACTION_HIDE_RECENTS_ACTIVITY)) {
                if (intent.getBooleanExtra(Recents.EXTRA_TRIGGERED_FROM_ALT_TAB, false)) {
                    // If we are hiding from releasing Alt-Tab, dismiss Recents to the focused app
                    //dismissRecentsToFocusedTaskOrHome(false);
                } else if (intent.getBooleanExtra(Recents.EXTRA_TRIGGERED_FROM_HOME_KEY, false)) {
                    // Otherwise, dismiss Recents to Home
                    //dismissRecentsToHome(true);
                    dismissAndGoHome();
                } else {
                    // Do nothing, another activity is being launched on top of Recents
                }
            } else if (action.equals(Recents.ACTION_TOGGLE_RECENTS_ACTIVITY)) {
                // If we are toggling Recents, then first unfilter any filtered stacks first
                //dismissRecentsToFocusedTaskOrHome(true);
                dismissAndGoHome();
            } else if (action.equals(Recents.ACTION_START_ENTER_ANIMATION)) {
                // Trigger the enter animation
                //onEnterAnimationTriggered();
                // Notify the fallback receiver that we have successfully got the broadcast
                // See AlternateRecentsComponent.onAnimationStarted()
                setResultCode(Activity.RESULT_OK);
            }

        }
    };
    //*/ tyd hongchang.han end


    //Added by Sean 2015-12-17 for handle screen off Action begin
    final BroadcastReceiver mSystemBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                // When the screen turns off, dismiss Recents to Home
                dismissAndGoHome();
            }
        }
    };
    //Added by Sean 2015-12-17 for handle screen off Action end

}
