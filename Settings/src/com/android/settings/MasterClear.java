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

package com.android.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;

import java.util.List;

//*/Modify by Jiangshouting 2015.12.29 for setting code transplant
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.PixelFormat;
import android.graphics.Canvas;
import com.mediatek.HobbyDB.CustomHobbyService;
//*/


/**
 * Confirm and execute a reset of the device to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a final strongly-worded "THIS WILL ERASE EVERYTHING
 * ON THE PHONE" prompt.  If at any time the phone is allowed to go to sleep, is
 * locked, et cetera, then the confirmation sequence is abandoned.
 *
 * This is the initial screen.
 */
public class MasterClear extends InstrumentedFragment {
    private static final String TAG = "MasterClear";

    private static final int KEYGUARD_REQUEST = 55;

    static final String ERASE_EXTERNAL_EXTRA = "erase_sd";

    private View mContentView;
    private Button mInitiateButton;
    private View mExternalStorageContainer;
    private CheckBox mExternalStorage;

    /**
     * Keyguard validation is run using the standard {@link ConfirmLockPattern}
     * component as a subactivity
     * @param request the request code to be returned once confirmation finishes
     * @return true if confirmation launched
     */
    private boolean runKeyguardConfirmation(int request) {
        Resources res = getActivity().getResources();
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(
                request, res.getText(R.string.master_clear_title));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != KEYGUARD_REQUEST) {
            return;
        }

        // If the user entered a valid keyguard trace, present the final
        // confirmation prompt; otherwise, go back to the initial state.
        if (resultCode == Activity.RESULT_OK) {
            showFinalConfirmation();
        } else {
            establishInitialState();
        }
    }

    private void showFinalConfirmation() {
        Bundle args = new Bundle();
        args.putBoolean(ERASE_EXTERNAL_EXTRA, mExternalStorage.isChecked());
        ((SettingsActivity) getActivity()).startPreferencePanel(MasterClearConfirm.class.getName(),
                args, R.string.master_clear_confirm_title, null, null, 0);
    }

    /**
     * If the user clicks to begin the reset sequence, we next require a
     * keyguard confirmation if the user has currently enabled one.  If there
     * is no keyguard available, we simply go to the final confirmation prompt.
     */
    private final Button.OnClickListener mInitiateListener = new Button.OnClickListener() {

        public void onClick(View v) {
            if (!runKeyguardConfirmation(KEYGUARD_REQUEST)) {
                showFinalConfirmation();
            }
        }
    };

    /**
     * In its initial state, the activity presents a button for the user to
     * click in order to initiate a confirmation sequence.  This method is
     * called from various other points in the code to reset the activity to
     * this base state.
     *
     * <p>Reinflating views from resources is expensive and prevents us from
     * caching widget pointers, so we use a single-inflate pattern:  we lazy-
     * inflate each view, caching all of the widget pointers we'll need at the
     * time, then simply reuse the inflated views directly whenever we need
     * to change contents.
     */
    private void establishInitialState() {
        mInitiateButton = (Button) mContentView.findViewById(R.id.initiate_master_clear);
        mInitiateButton.setOnClickListener(mInitiateListener);
        mExternalStorageContainer = mContentView.findViewById(R.id.erase_external_container);
        mExternalStorage = (CheckBox) mContentView.findViewById(R.id.erase_external);

        /*
         * If the external storage is emulated, it will be erased with a factory
         * reset at any rate. There is no need to have a separate option until
         * we have a factory reset that only erases some directories and not
         * others. Likewise, if it's non-removable storage, it could potentially have been
         * encrypted, and will also need to be wiped.
         */
        boolean isExtStorageEmulated = Environment.isExternalStorageEmulated();
        if (isExtStorageEmulated
                || (!Environment.isExternalStorageRemovable() && isExtStorageEncrypted())) {
            mExternalStorageContainer.setVisibility(View.GONE);

            final View externalOption = mContentView.findViewById(R.id.erase_external_option_text);
            externalOption.setVisibility(View.GONE);

            final View externalAlsoErased = mContentView.findViewById(R.id.also_erases_external);
            externalAlsoErased.setVisibility(View.VISIBLE);

            // If it's not emulated, it is on a separate partition but it means we're doing
            // a force wipe due to encryption.
            mExternalStorage.setChecked(!isExtStorageEmulated);
        } else {
            mExternalStorageContainer.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mExternalStorage.toggle();
                }
            });
        }

        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        loadAccountList(um);
        StringBuffer contentDescription = new StringBuffer();
        View masterClearContainer = mContentView.findViewById(R.id.master_clear_container);
        getContentDescription(masterClearContainer, contentDescription);
        masterClearContainer.setContentDescription(contentDescription);
    }

    private void getContentDescription(View v, StringBuffer description) {
       if (v instanceof ViewGroup) {
           ViewGroup vGroup = (ViewGroup) v;
           for (int i = 0; i < vGroup.getChildCount(); i++) {
               View nextChild = vGroup.getChildAt(i);
               getContentDescription(nextChild, description);
           }
       } else if (v instanceof TextView) {
           TextView vText = (TextView) v;
           description.append(vText.getText());
           description.append(","); // Allow Talkback to pause between sections.
       }
    }

    private boolean isExtStorageEncrypted() {
        String state = SystemProperties.get("vold.decrypt");
        return !"".equals(state);
    }

    private void loadAccountList(final UserManager um) {
        View accountsLabel = mContentView.findViewById(R.id.accounts_label);
        LinearLayout contents = (LinearLayout)mContentView.findViewById(R.id.accounts);
        contents.removeAllViews();

        Context context = getActivity();
        final List<UserInfo> profiles = um.getProfiles(UserHandle.myUserId());
        final int profilesSize = profiles.size();

        AccountManager mgr = AccountManager.get(context);

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        int accountsCount = 0;
        for (int profileIndex = 0; profileIndex < profilesSize; profileIndex++) {
            final UserInfo userInfo = profiles.get(profileIndex);
            final int profileId = userInfo.id;
            final UserHandle userHandle = new UserHandle(profileId);
            Account[] accounts = mgr.getAccountsAsUser(profileId);
            final int N = accounts.length;
            if (N == 0) {
                continue;
            }
            accountsCount += N;

            AuthenticatorDescription[] descs = AccountManager.get(context)
                    .getAuthenticatorTypesAsUser(profileId);
            final int M = descs.length;

            View titleView = Utils.inflateCategoryHeader(inflater, contents);
            final TextView titleText = (TextView) titleView.findViewById(android.R.id.title);
            titleText.setText(userInfo.isManagedProfile() ? R.string.category_work
                    : R.string.category_personal);
            contents.addView(titleView);

            for (int i = 0; i < N; i++) {
                Account account = accounts[i];
                AuthenticatorDescription desc = null;
                for (int j = 0; j < M; j++) {
                    if (account.type.equals(descs[j].type)) {
                        desc = descs[j];
                        break;
                    }
                }
                if (desc == null) {
                    Log.w(TAG, "No descriptor for account name=" + account.name
                            + " type=" + account.type);
                    continue;
                }
                Drawable icon = null;
                try {
                    if (desc.iconId != 0) {
                        Context authContext = context.createPackageContextAsUser(desc.packageName,
                                0, userHandle);
                        icon = context.getPackageManager().getUserBadgedIcon(
                                authContext.getDrawable(desc.iconId), userHandle);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, "Bad package name for account type " + desc.type);
                } catch (Resources.NotFoundException e) {
                    Log.w(TAG, "Invalid icon id for account type " + desc.type, e);
                }
                if (icon == null) {
                    icon = context.getPackageManager().getDefaultActivityIcon();
                }

                TextView child = (TextView)inflater.inflate(R.layout.master_clear_account,
                        contents, false);
                child.setText(account.name);
                /*/ freeme.menglingqiang, 20161009. modify master clear page icon size.
                //Add by tyd Jiangshouting 2015.11.09 for bug[tyd00584285]
                icon = zoomDrawable(icon, 180 ,180);
                /*/
                int master_clear_icon_size = (int)getResources().getDimension(R.dimen.master_clear_icon_size);
                icon = zoomDrawable(icon, master_clear_icon_size ,master_clear_icon_size);
                //*/
                child.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                //*/ freeme.menglingqiang, 20161012. modify master clear page icon space.
                int master_clear_icon_space = (int)getResources().getDimension(R.dimen.master_clear_icon_space);
                child.setPadding(0,master_clear_icon_space,0,master_clear_icon_space);
                //*/
                contents.addView(child);
            }
        }

        if (accountsCount > 0) {
            accountsLabel.setVisibility(View.VISIBLE);
            contents.setVisibility(View.VISIBLE);
        }
        // Checking for all other users and their profiles if any.
        View otherUsers = mContentView.findViewById(R.id.other_users_present);
        final boolean hasOtherUsers = (um.getUserCount() - profilesSize) > 0;
        otherUsers.setVisibility(hasOtherUsers ? View.VISIBLE : View.GONE);
    }

    //*/ Added by tyd Jiangshouting 2015.11.09 for bug[tyd00584285]
    private Drawable zoomDrawable(Drawable drawable, int w, int h) {  
        int width = drawable.getIntrinsicWidth();  
        int height = drawable.getIntrinsicHeight();  
        Bitmap oldbmp = drawableToBitmap(drawable);  
        Matrix matrix = new Matrix();  
        float scaleWidth = ((float) w / width);  
        float scaleHeight = ((float) h / height);  
        matrix.postScale(scaleWidth, scaleHeight);  
        Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height,  
                matrix, true);  
        return new BitmapDrawable(null, newbmp);  
    }  

    private Bitmap drawableToBitmap(Drawable drawable) {  
        
        int width = drawable.getIntrinsicWidth();  
        int height = drawable.getIntrinsicHeight();  
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888  
                : Bitmap.Config.RGB_565;  
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);  
        Canvas canvas = new Canvas(bitmap);  
        drawable.setBounds(0, 0, width, height);  
        drawable.draw(canvas);  
        return bitmap;  
    }  
    //*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        //*/Added by tyd_wangalei 2015.9.22 for Customs hobby sort
        CustomHobbyService mService=new CustomHobbyService(getActivity());
        if(mService.isExistData(R.string.privacy_settings, R.string.master_clear_title)){
            mService.update(R.string.privacy_settings, R.string.master_clear_title);
        }else{
            mService.insert(R.string.privacy_settings, R.string.master_clear_title, this.getClass().getName(), 1, "");
        }
        //*/
        if (!Process.myUserHandle().isOwner()
                || UserManager.get(getActivity()).hasUserRestriction(
                UserManager.DISALLOW_FACTORY_RESET)) {
            return inflater.inflate(R.layout.master_clear_disallowed_screen, null);
        }

        mContentView = inflater.inflate(R.layout.master_clear, null);

        establishInitialState();
        return mContentView;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.MASTER_CLEAR;
    }
}
