package com.freeme.settings.face;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceGroup;

import com.android.settings.ChooseLockGeneric;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.freeme.util.FreemeFeature;

public class FaceprintSettings extends Activity {
    private static final String TAG = "FaceprintSettings";

    public static final ComponentName FACEPRINT_SETTINGS = new ComponentName(
            "com.freeme.face.simple",
            "com.freeme.face.simple.settings.FaceprintSettings");

    public static final String KEY_FACEPRINT_SETTINGS = "faceprint_settings";

    private static final String KEY_LAUNCHED_CONFIRM = "launched_confirm";

    private static final int CONFIRM_REQUEST = 101;
    private static final int CHOOSE_LOCK_GENERIC_REQUEST = 102;

    protected static final int RESULT_FINISHED = RESULT_FIRST_USER;

    private byte[] mToken;
    private boolean mLaunchedConfirm;

    //private int mUserId;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (savedState != null) {
            mToken = savedState.getByteArray(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
            mLaunchedConfirm = savedState.getBoolean(
                    KEY_LAUNCHED_CONFIRM, false);
        }
/*        mUserId = getIntent().getIntExtra(
                Intent.EXTRA_USER_ID, UserHandle.myUserId());*/

        // Need to authenticate a session token if none
        if (mToken == null && mLaunchedConfirm == false) {
            mLaunchedConfirm = true;
            launchChooseOrConfirmLock();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_LOCK_GENERIC_REQUEST
                || requestCode == CONFIRM_REQUEST) {
            if (resultCode == RESULT_FINISHED || resultCode == RESULT_OK) {
                // The lock pin/pattern/password was set. Start enrolling!
                if (data != null) {
                    mToken = data.getByteArrayExtra(
                            ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                }
            }
        }

        if (mToken == null) {
            // Didn't get an authentication, finishing
            finish();
        } else {
            startActivity(new Intent()
                    .setComponent(FACEPRINT_SETTINGS)
                    .putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken)
                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
            finish();
        }
    }

    private void launchChooseOrConfirmLock() {
        Intent intent = new Intent();
        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(this);
        if (!helper.launchConfirmationActivity(CONFIRM_REQUEST,
                getString(R.string.security_settings_faceprint_preference_title),
                null, null, 0)) {
            intent.setClassName("com.android.settings", ChooseLockGeneric.class.getName());
            intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                    DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
            intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS,
                    true);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0);
            /*intent.putExtra(Intent.EXTRA_USER_ID, mUserId);*/
            startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);
        }
    }

    ///

    private static Preference getFaceprintPreferenceForUser(Context context, final int userId) {
        Preference fingerprintPreference = new Preference(context);
        fingerprintPreference.setKey(KEY_FACEPRINT_SETTINGS);
        fingerprintPreference.setTitle(R.string.security_settings_faceprint_preference_title);
        final String clazz = FaceprintSettings.class.getName();
        fingerprintPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Context context = preference.getContext();
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", clazz);
/*                intent.putExtra(Intent.EXTRA_USER_ID, userId);*/
                context.startActivity(intent);
                return true;
            }
        });
        return fingerprintPreference;
    }

    public static void maybeAddFaceprintPreference(PreferenceGroup securityCategory, int userId) {
        if (FreemeFeature.getLocalInt("config.faceprint.ui.entrypos", 0) != 0) {
            return;
        }
        Preference faceprintPreference = getFaceprintPreferenceForUser(
                securityCategory.getContext(), userId);
        if (faceprintPreference != null) {
            securityCategory.addPreference(faceprintPreference);
        }
    }

    ///

    public static void removeAllFaceprint(Context context) {
        context.startService(new Intent()
                .setClassName("com.freeme.face.simple",
                        "com.freeme.face.simple.FaceprintService")
                .putExtra("command", 104));
    }

    public static boolean hasNoEnrolledFace(Activity activity) {
        Intent intent = activity.registerReceiver(null, new IntentFilter("com.freeme.intent.action.face.STATE"));
        return intent == null || !intent.getBooleanExtra("com.freeme.intent.extra.face.HAS_ENROLLED_FACE", false);
    }
}
