/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
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

package com.droi.systemui.qs.tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.recents.model.Task;

/** Quick settings tile: Optimization **/
public class DroiTileOptimization extends QSTile<QSTile.State> {
    private static final String TAG = "OptimizationTile";

    final static String ACTION_CLEAR_FROM_FREEME = "com.android.systemui.CLEAR_RECENTS_APP";
    final static Uri CONTENT_URI =  Uri.parse("content://com.freeme.sc.common.db.uninstall.app/WhiteList");
    private static final int MAX_TASKS = 21;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(ACTION_CLEAR_FROM_FREEME.equals(action)) {
                    Log.d(TAG, "clear recents app from 3rd...");
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            onHandleClick();
                        }
                    });
                }
            }
    };

    public DroiTileOptimization(Host host) {
        super(host);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CLEAR_FROM_FREEME);
        mContext.registerReceiver(mReceiver, filter);
    }

    @Override
    protected State newTileState() {
        return new State();
    }

    @Override
    public void setListening(boolean listening) {
    }

    @Override
    protected void handleClick() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                onHandleClick();
            }
        });
    }

    @Override
    protected void handleUpdateState(State state, Object arg) {
        state.visible = true; 
        state.icon = ResourceIcon.get(R.drawable.droi_ic_qs_system_optimization);
        state.label = mContext.getString(R.string.droi_qs_oneclear_label);
        state.contentDescription = mContext.getString(R.string.droi_qs_oneclear_label);
    }

    private void onHandleClick() {
        final ActivityManager mAm = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RecentTaskInfo> recentTasks = getRecentTasks(mAm);

        ArrayList<String> whitePackages = new ArrayList<String>();
        
        String whiteList = getWhiteListFromSafeCenter();
        if(DEBUG) {
            Log.d(TAG,"white-list from ZhuoyiSafeCenter: ["+ whiteList + "]");
        }
        Collections.addAll(whitePackages, whiteList.split(","));
        //*/

        for(ActivityManager.RecentTaskInfo rt: recentTasks) {
            String packageName = rt.baseIntent.getComponent().getPackageName();
            Log.d(TAG, "**** clear task:" + packageName);
            if(whitePackages.contains(packageName)) {
                Log.d(TAG, "skip package:" + packageName + " which is in ZhuoyiSafeCenter White-list");
                continue;
            }
            
            if (mAm != null) mAm.removeTask(rt.persistentId);
        }

        final ArrayList<String> whiteListFilter = whitePackages;
        new Handler().postDelayed(new Runnable() {
            
            @Override
            public void run() {
                Intent intent = new Intent("android.intent.action.CLEAN_TASK_ZHUOYI_RECEIVER");
                intent.putStringArrayListExtra("WListFSystemUI", whiteListFilter);
                intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                mContext.sendBroadcastAsUser(intent,UserHandle.CURRENT);
                mHost.collapsePanels();
            }
        }, 300);

    }

    private List<ActivityManager.RecentTaskInfo> getRecentTasks(final ActivityManager am){
        List<ActivityManager.RecentTaskInfo> tasks = am.getRecentTasksForUser(MAX_TASKS,
                ActivityManager.RECENT_IGNORE_HOME_STACK_TASKS |
                ActivityManager.RECENT_IGNORE_UNAVAILABLE |
                ActivityManager.RECENT_INCLUDE_PROFILES |
                ActivityManager.RECENT_WITH_EXCLUDED, UserHandle.CURRENT.getIdentifier());
        // Break early if we can't get a valid set of tasks
        if (tasks == null) {
            if (DEBUG) {
                Log.d(TAG, "getRecentTasks: getRecentTasksForUser is null");
            }
            return new ArrayList<ActivityManager.RecentTaskInfo>();
        }

        boolean isFirstValidTask = true;
        Iterator<ActivityManager.RecentTaskInfo> iter = tasks.iterator();
        while (iter.hasNext()) {
            ActivityManager.RecentTaskInfo t = iter.next();

            // NOTE: The order of these checks happens in the expected order of the traversal of the
            // tasks
            //
            // Check the first non-recents task, include this task even if it is marked as excluded
            // from recents if we are currently in the app.  In other words, only remove excluded
            // tasks if it is not the first active task.
            boolean isExcluded = (t.baseIntent.getFlags() & Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        == Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
            if (DEBUG) {
                Task.TaskKey taskKey = new Task.TaskKey(t.id, t.persistentId, t.baseIntent, t.userId,
                t.firstActiveTime, t.lastActiveTime);
                Log.d(TAG, "getRecentTasks:TASK = " + taskKey.toString()
                       + "/isExcluded = " + isExcluded
                       + "/isFirstValidTask = " + isFirstValidTask
                       + "/t.id = " + t.id);
            }
            /// M: [ALPS01794032] Remove all excluded task @{
            if (isExcluded) {
                iter.remove();
                continue;
            }
            /// M: [ALPS01794032] Remove all excluded task @}
            isFirstValidTask = false;
        }
        
        return tasks.subList(0, Math.min(tasks.size(), MAX_TASKS));
    }

    public String getWhiteListFromSafeCenter() {
        String whitePackages = "";
        Cursor cursor = null;
        
        try{
            long startTime = System.nanoTime();
            cursor = mContext.getContentResolver().query(CONTENT_URI, null, null, null, null);
            
            if(null != cursor && cursor.getCount() > 0) {
                if(cursor.moveToNext()) {
                    whitePackages = cursor.getString(cursor.getColumnIndex("white_packageName"));
                    Log.d(TAG, "get white list from Safecenter{" + whitePackages + "}.");
                }
            }
            
            cursor.close();
            
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
            Log.d(TAG, "get whitePackages from local backup[" + whitePackages + "]");
        }
        return whitePackages;
    }

    private String getBackupWhitePackage() {
        String backup = "";
        SharedPreferences sp = mContext.getSharedPreferences("LockedRecentsBackup", Context.MODE_PRIVATE);
        
        backup = sp.getString("backup", "");
        
        return backup;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_PANEL;
    }
}
