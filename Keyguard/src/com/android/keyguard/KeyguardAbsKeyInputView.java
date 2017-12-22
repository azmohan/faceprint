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

package com.android.keyguard;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;

/**
 * Base class for PIN and password unlock screens.
 */
public abstract class KeyguardAbsKeyInputView extends LinearLayout
        implements KeyguardSecurityView, EmergencyButton.EmergencyButtonCallback {
    protected KeyguardSecurityCallback mCallback;
    protected LockPatternUtils mLockPatternUtils;
    protected AsyncTask<?, ?, ?> mPendingLockCheck;
    protected SecurityMessageDisplay mSecurityMessageDisplay;
    protected View mEcaView;
    protected boolean mEnableHaptics;

    // To avoid accidental lockout due to events while the device in in the pocket, ignore
    // any passwords with length less than or equal to this length.
    protected static final int MINIMUM_PASSWORD_LENGTH_BEFORE_REPORT = 3;

    public KeyguardAbsKeyInputView(Context context) {
        this(context, null);
    }

    public KeyguardAbsKeyInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        mCallback = callback;
    }

    @Override
    public void setLockPatternUtils(LockPatternUtils utils) {
        mLockPatternUtils = utils;
        mEnableHaptics = mLockPatternUtils.isTactileFeedbackEnabled();
    }

    @Override
    public void reset() {
        // start fresh
        resetPasswordText(false /* animate */);
        // if the user is currently locked out, enforce it.
        long deadline = mLockPatternUtils.getLockoutAttemptDeadline(
                KeyguardUpdateMonitor.getCurrentUser());
        if (shouldLockout(deadline)) {
            handleAttemptLockout(deadline);
        } else {
            resetState();
        }
    }

    // Allow subclasses to override this behavior
    protected boolean shouldLockout(long deadline) {
        return deadline != 0;
    }

    protected abstract int getPasswordTextViewId();
    protected abstract void resetState();

    @Override
    protected void onFinishInflate() {
        mLockPatternUtils = new LockPatternUtils(mContext);
        mSecurityMessageDisplay = KeyguardMessageArea.findSecurityMessageDisplay(this);
        mEcaView = findViewById(R.id.keyguard_selector_fade_container);

        EmergencyButton button = (EmergencyButton) findViewById(R.id.emergency_call_button);
        if (button != null) {
            button.setCallback(this);
        }
        
      //*/Added by Freeme Greg 2015-08-28,for auto unlock lockscreen
        mPasswordLength = getLockPasswordLength();
        mVisitorModeState = Settings.System.getString(this.getContext().getContentResolver(), VisitorModeUtil.VISTOR_MODE_PASSWORD_STATE);
        if (mVisitorModeState != null && !VisitorModeUtil.VISTOR_MODE_PASSWORD_STATE_NONE.equals(mVisitorModeState)) {
            String temp = Settings.System.getString(this.getContext().getContentResolver(), VisitorModeUtil.VISTOR_MODE_PASSWORD_LENGTH);
            if (temp == null || temp.isEmpty())
                mPasswordLengthVisitor = -1;
            else
                mPasswordLengthVisitor = Integer.parseInt(temp);
        }

        Log.d(TAG, "pwd len = " + mPasswordLength + "  visitor mode state = " + mVisitorModeState + " visitor pwd len = " + mPasswordLengthVisitor);
        //*/
    }

    @Override
    public void onEmergencyButtonClickedWhenInCall() {
        mCallback.reset();
    }

    /*
     * Override this if you have a different string for "wrong password"
     *
     * Note that PIN/PUK have their own implementation of verifyPasswordAndUnlock and so don't need this
     */
    protected int getWrongPasswordStringId() {
        return R.string.kg_wrong_password;
    }

    protected void verifyPasswordAndUnlock() {
        final String entry = getPasswordText();
        setPasswordEntryInputEnabled(false);
        if (mPendingLockCheck != null) {
            mPendingLockCheck.cancel(false);
        }

        if (entry.length() <= MINIMUM_PASSWORD_LENGTH_BEFORE_REPORT) {
            // to avoid accidental lockout, only count attempts that are long enough to be a
            // real password. This may require some tweaking.
            setPasswordEntryInputEnabled(true);
            onPasswordChecked(false /* matched */, 0, false /* not valid - too short */,entry);
            return;
        }

        mPendingLockCheck = LockPatternChecker.checkPassword(
                mLockPatternUtils,
                entry,
                KeyguardUpdateMonitor.getCurrentUser(),
                new LockPatternChecker.OnCheckCallback() {
                    @Override
                    public void onChecked(boolean matched, int timeoutMs) {
                        setPasswordEntryInputEnabled(true);
                        mPendingLockCheck = null;
                        onPasswordChecked(matched, timeoutMs, true /* isValidPassword */,entry);
                    }
                });
    }

    private void onPasswordChecked(boolean matched, int timeoutMs, boolean isValidPassword,String entry) {
        if (matched) {
            mCallback.reportUnlockAttempt(true, 0);
            mCallback.dismiss(true);
            
            //*/Added by freeme Greg 2014-05-26,for visitor mode
            Context context = getContext();
            Intent intent = new Intent(VisitorModeUtil.VISTOR_MODE_OFF);
            intent.putExtra(VisitorModeUtil.VISTOR_MODE_STATE_EXTRA, VisitorModeUtil.getVistorModelState(context));
            context.sendBroadcast(intent);
            VisitorModeUtil.putVistorModelState(context, 0);
            //*/
        }else if(VisitorModeUtil.VISTOR_MODE_PASSWORD_STATE_PASSWORD.equals(VisitorModeUtil
                .getVistorModelType(getContext()))){
            Context context = getContext();
            final String vistorPassword = VisitorModeUtil.getVistorSettingsValue(context,
                    VisitorModeUtil.VISTOR_MODE_PASSWORD);
            final String inputValue = VisitorModeUtil.getMd5Value(entry);
            if ((vistorPassword != null) && (vistorPassword.equals(inputValue))) {
                Intent intent = new Intent(VisitorModeUtil.VISTOR_MODE_ON);
                intent.putExtra(VisitorModeUtil.VISTOR_MODE_STATE_EXTRA, VisitorModeUtil.getVistorModelState(context));
                context.sendBroadcast(intent);
                VisitorModeUtil.putVistorModelState(context, 1);
                mCallback.reportUnlockAttempt(true,0);
                mCallback.dismiss(true);
            } else {
                if (isValidPassword) {
                    mCallback.reportUnlockAttempt(false, timeoutMs);
                    if (timeoutMs > 0) {
                        long deadline = mLockPatternUtils.setLockoutAttemptDeadline(
                                KeyguardUpdateMonitor.getCurrentUser(), timeoutMs);
                        handleAttemptLockout(deadline);
                    }
                }
                if (timeoutMs == 0) {
                    mSecurityMessageDisplay.setMessage(getWrongPasswordStringId(), true);
                }
            }
         
            
        }else {
            if (isValidPassword) {
                mCallback.reportUnlockAttempt(false, timeoutMs);
                if (timeoutMs > 0) {
                    long deadline = mLockPatternUtils.setLockoutAttemptDeadline(
                            KeyguardUpdateMonitor.getCurrentUser(), timeoutMs);
                    handleAttemptLockout(deadline);
                }
            }
            if (timeoutMs == 0) {
                mSecurityMessageDisplay.setMessage(getWrongPasswordStringId(), true);
            }
        }
        resetPasswordText(true /* animate */);
    }
    
    //*/Added by Freeme Greg 2015-08-28,for auto unlock lockscreen
    public  static final boolean AUTO_UNLOCK_DEBUG = true;
    private static final String TAG = "KeyguardAbsKeyInputView";

    protected static final int UNLOCK_TO_MASTER = 10;
    protected static final int UNLOCK_TO_VISITOR = 11;

    private int mPasswordLength = -1;
    private int mPasswordLengthVisitor = -1;;
    private String mVisitorModeState = null;

    private Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            unlockPasswordForAuto(msg.arg1);
        }

    };

    public void verifyPasswordAuto() {
        if (AUTO_UNLOCK_DEBUG) {
            Log.d(TAG, "verifyPasswordAuto");
        }
        if (AntiTheftManager.getInstance(null, null, null).isAntiTheftLocked()) {
            Log.d(TAG, "in anti theft mode,so we can't auto unlock!");
            return;
        }
        String entry = getPasswordText();
        if (!TextUtils.isEmpty(entry)) {
            if (mVisitorModeState != null && !VisitorModeUtil.VISTOR_MODE_PASSWORD_STATE_NONE.equals(mVisitorModeState)) {
                if (mPasswordLength != -1 && mPasswordLengthVisitor != -1) {
                    mHandler.removeMessages(1);
                    if (mPasswordLength == mPasswordLengthVisitor) {
                        if (entry.length() == mPasswordLength) {
                            verifyPasswordAndUnlock();
                        }
                    } else {
                        int min = Math.min(mPasswordLength, mPasswordLengthVisitor);
                        int max = Math.max(mPasswordLength, mPasswordLengthVisitor);
                        if (AUTO_UNLOCK_DEBUG) {
                            Log.d(TAG, "verifyPasswordNotUnlock mEditableString.length() = " + entry.length() + ", min = " +
                                    "" + min + ", max = " + max);
                        }
                        if (entry.length() == min) {
                            int result = verifyPasswordNotUnlock();
                            Log.i(TAG, "verifyPasswordNotUnlock result = " + result);
                            if (result != -1) {
                                Message msg = mHandler.obtainMessage(1);
                                msg.arg1 = result;
                                mHandler.sendMessageDelayed(msg, 2000);
                            }
                        } else if (entry.length() == max) {
                            verifyPasswordAndUnlock();
                        }
                    }
                } else {
                    if (AUTO_UNLOCK_DEBUG) {
                        Log.e(TAG, "master password or visitor password is not setted and throw error");
                    }
                }

            } else {
                if (mPasswordLength != -1 && entry.length() == mPasswordLength) {
                    verifyPasswordAndUnlock();
                }
            }
        }
    }

    protected int verifyPasswordNotUnlock() {
        int result = -1;
        String entry = getPasswordText();
        
        boolean check =false;
        try {
             check =mLockPatternUtils.checkPassword(entry,KeyguardUpdateMonitor.getCurrentUser());
            
        } catch (Exception e) {

        }
        if (check) {
            result = UNLOCK_TO_MASTER;
        } else if (VisitorModeUtil.VISTOR_MODE_PASSWORD_STATE_PASSWORD.equals(VisitorModeUtil
                .getVistorModelType(getContext()))) {
            Context context = getContext();
            String vistorPassword = VisitorModeUtil.getVistorSettingsValue(context,
                    VisitorModeUtil.VISTOR_MODE_PASSWORD);
            String inputValue = VisitorModeUtil.getMd5Value(entry);
            if ((vistorPassword != null) && (vistorPassword.equals(inputValue))) {
                result = UNLOCK_TO_VISITOR;

            }
        }
        if (AUTO_UNLOCK_DEBUG) {
            Log.d(TAG,"verifyPasswordNotUnlock result = " + result);
        }
        return result;
    }

    protected void unlockPasswordForAuto(int resultcode) {
        if (resultcode == UNLOCK_TO_MASTER) {
            mCallback.reportUnlockAttempt(true,0);
            mCallback.dismiss(true);
            Context context = getContext();
            Intent intent = new Intent(VisitorModeUtil.VISTOR_MODE_OFF);
            intent.putExtra(VisitorModeUtil.VISTOR_MODE_STATE_EXTRA, VisitorModeUtil.getVistorModelState(context));
            context.sendBroadcast(intent);
            VisitorModeUtil.putVistorModelState(context, 0);
        } else if (resultcode == UNLOCK_TO_VISITOR) {
            Intent intent = new Intent(VisitorModeUtil.VISTOR_MODE_ON);
            intent.putExtra(VisitorModeUtil.VISTOR_MODE_STATE_EXTRA, VisitorModeUtil.getVistorModelState(getContext()));
            getContext().sendBroadcast(intent);
            VisitorModeUtil.putVistorModelState(getContext(), 1);
            mCallback.reportUnlockAttempt(true,0);
            mCallback.dismiss(true);

        }
        resetPasswordText(true /* animate */);
    }

    public int getLockPasswordLength() {
        String temp = mLockPatternUtils.getPasswordLength(LockPatternUtils.PASSWORD_KEY_LENGTH,KeyguardUpdateMonitor.getCurrentUser());
        if (temp == null || temp.isEmpty())
            return -1;
        return Integer.parseInt(temp);
    }
    //*/

    protected abstract void resetPasswordText(boolean animate);
    protected abstract String getPasswordText();
    protected abstract void setPasswordEntryEnabled(boolean enabled);
    protected abstract void setPasswordEntryInputEnabled(boolean enabled);

    // Prevent user from using the PIN/Password entry until scheduled deadline.
    protected void handleAttemptLockout(long elapsedRealtimeDeadline) {
        setPasswordEntryEnabled(false);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                mSecurityMessageDisplay.setMessage(
                        R.string.kg_too_many_failed_attempts_countdown, true, secondsRemaining);
            }

            @Override
            public void onFinish() {
                mSecurityMessageDisplay.setMessage("", false);
                resetState();
            }
        }.start();
    }

    protected void onUserInput() {
        if (mCallback != null) {
            mCallback.userActivity();
        }
        mSecurityMessageDisplay.setMessage("", false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        onUserInput();
        return false;
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public void onPause() {
        if (mPendingLockCheck != null) {
            mPendingLockCheck.cancel(false);
            mPendingLockCheck = null;
        }
    }

    @Override
    public void onResume(int reason) {
        reset();
    }

    @Override
    public KeyguardSecurityCallback getCallback() {
        return mCallback;
    }

    @Override
    public void showPromptReason(int reason) {
        if (reason != PROMPT_REASON_NONE) {
            int promtReasonStringRes = getPromtReasonStringRes(reason);
            if (promtReasonStringRes != 0) {
                mSecurityMessageDisplay.setMessage(promtReasonStringRes,
                        true /* important */);
            }
        }
    }

    @Override
    public void showMessage(String message, int color) {
        mSecurityMessageDisplay.setNextMessageColor(color);
        mSecurityMessageDisplay.setMessage(message, true /* important */);
    }

    protected abstract int getPromtReasonStringRes(int reason);

    // Cause a VIRTUAL_KEY vibration
    public void doHapticKeyClick() {
        if (mEnableHaptics) {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                    | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }
    
    //*/ Modified by freeme Jack 20140314 for, reform keyguard with free me 3.0
    @Override
    public KeyguardUpdateMonitorCallback getKeyguardUpdateMonitorCallback() {
        return null;
    }
    
    //*/modified by shijiachen 20150727 for customize keyguard
    @Override
    public void notificationVisibleChange(boolean isVisible) {
    }
    
    @Override
    public void onWallpaperChanged(Drawable  drawable) {
    }
    //*/
}

