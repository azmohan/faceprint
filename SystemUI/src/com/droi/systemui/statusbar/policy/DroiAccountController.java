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

package com.droi.systemui.statusbar.policy;

import java.io.File;
import java.util.ArrayList;

import android.accounts.DroiAccount;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.android.systemui.BitmapHelper;
import com.android.systemui.R;

public final class DroiAccountController {

    private static final boolean DEBUG = true;
    private static final String TAG = "DroiAccountController";

    // User logout
    private static final String INTENT_ACCOUNT_DELETED = "droi.account.intent.action.ACCOUNT_DELETED";
    // User changed
    private static final String INTENT_ACCOUNT_UPDATED = "droi.account.intent.action.ACCOUNT_UPDATED";
    // User login
    private static final String INTENT_ACCOUNT_LOGIN = "droi.account.intent.action.ACCOUNT_LOGIN";

    private final Context mContext;
    private final ArrayList<OnDroiAccountChangedListener> mCallbacks =
            new ArrayList<OnDroiAccountChangedListener>();
    private AsyncTask<Void, Void, Pair<String, Drawable>> mDroiAccountTask;

    private String mUserName;
    private Drawable mUserDrawable;

    public DroiAccountController(Context context) {
        mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_ACCOUNT_DELETED);
        filter.addAction(INTENT_ACCOUNT_UPDATED);
        filter.addAction(INTENT_ACCOUNT_LOGIN);
        mContext.registerReceiver(mReceiver, filter);

    }

    public void addListener(OnDroiAccountChangedListener callback) {
        mCallbacks.add(callback);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            
            if(DEBUG) Log.d(TAG, " droiAccount action = " + action);
            if (INTENT_ACCOUNT_UPDATED.equals(action)) { // changed
                reloadDroiAccountInfo();
            } else if (INTENT_ACCOUNT_LOGIN.equals(action)) { // Login
                reloadDroiAccountInfo();
            } else if (INTENT_ACCOUNT_DELETED.equals(action)) { // Logout
                reloadDroiAccountInfo();
            }
        }
    };

    public void reloadDroiAccountInfo() {
        if (mDroiAccountTask != null) {
            mDroiAccountTask.cancel(false);
            mDroiAccountTask = null;
        }
        try {
            queryForUserInformation();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "" + e);

            mUserName = "Unknown";
            mUserDrawable = mContext.getResources().getDrawable(R.drawable.droi_ic_account_circle_logout);
        }
    }

    private void queryForUserInformation() {

        final Resources res = mContext.getResources();
        final ContentResolver resolver = mContext.getContentResolver(); 
        final int avatarSize = res.getDimensionPixelSize(R.dimen.droi_multi_user_avatar_size);
                //Math.max(
                //res.getDimensionPixelSize(R.dimen.multi_user_avatar_expanded_size),
                //res.getDimensionPixelSize(R.dimen.multi_user_avatar_keyguard_size));

        mDroiAccountTask = new AsyncTask<Void, Void, Pair<String, Drawable>>() {
            @Override
            protected Pair<String, Drawable> doInBackground(Void... params) {
                final DroiAccount account = DroiAccount.getInstance(mContext);

                boolean isUserLogin = account.checkAccount();
                String name = account.getUserName();
                String nick = account.getNickName();
                Drawable avatar = null;
                Bitmap rawAvatar = null;
                String avatarUrl = account.getAvatarUrl();

                if(isUserLogin) {
                    avatar = res.getDrawable(R.drawable.droi_ic_account_circle_login);
                } else {
                    avatar = res.getDrawable(R.drawable.droi_ic_account_circle_logout);
                }

                
                if(!TextUtils.isEmpty(avatarUrl)) {
                    try {  
                        File file = new File(avatarUrl);  
                        if(file.exists()){  
                            rawAvatar = BitmapFactory.decodeFile(avatarUrl);  
                        }  
                    } catch (Exception e) {  
                        Log.w(TAG, "Couldn't load Avatar ", e);
                        rawAvatar = null;
                    }
                }

                if (rawAvatar != null) {
                    avatar = new BitmapDrawable(mContext.getResources(),
                            BitmapHelper.createCircularClip(rawAvatar, avatarSize, avatarSize));
                }

                if(false) {
                    Log.d(TAG, "DroiAccountInfo\n[\nuserName:" + name + "\n" + "nicName:" + nick + "\navatarUrl:" + avatarUrl + "\n]");
                }

                return new Pair<String, Drawable>(name, avatar);
            }

            @Override
            protected void onPostExecute(Pair<String, Drawable> result) {
                mUserName = result.first;
                mUserDrawable = result.second;
                mDroiAccountTask = null;
                notifyChanged();
            }
        };
        mDroiAccountTask.execute();
    }

    private void notifyChanged() {
        for (OnDroiAccountChangedListener listener : mCallbacks) {
            listener.onDroiAccountChanged(mUserName, mUserDrawable);
        }
    }

    public interface OnDroiAccountChangedListener {
        public void onDroiAccountChanged(String name, Drawable picture);
    }
}
