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

package com.android.systemui.keyguard;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardService;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.systemui.SystemUIApplication;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class KeyguardService extends Service {
    static final String TAG = "KeyguardService";
    static final String PERMISSION = android.Manifest.permission.CONTROL_KEYGUARD;

    private KeyguardViewMediator mKeyguardViewMediator;

    @Override
    public void onCreate() {
        ((SystemUIApplication) getApplication()).startServicesIfNeeded();
        mKeyguardViewMediator =
                ((SystemUIApplication) getApplication()).getComponent(KeyguardViewMediator.class);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    void checkPermission() {
        // Avoid deadlock by avoiding calling back into the system process.
        if (Binder.getCallingUid() == Process.SYSTEM_UID) return;

        // Otherwise,explicitly check for caller permission ...
        if (getBaseContext().checkCallingOrSelfPermission(PERMISSION) != PERMISSION_GRANTED) {
            Log.w(TAG, "Caller needs permission '" + PERMISSION + "' to call " + Debug.getCaller());
            throw new SecurityException("Access denied to process: " + Binder.getCallingPid()
                    + ", must have permission " + PERMISSION);
        }
    }

    private final IKeyguardService.Stub mBinder = new IKeyguardService.Stub() {

        @Override // Binder interface
        public void addStateMonitorCallback(IKeyguardStateCallback callback) {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"addStateMonitorCallback System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.addStateMonitorCallback(callback);
        }

        @Override // Binder interface
        public void verifyUnlock(IKeyguardExitCallback callback) {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"verifyUnlock System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.verifyUnlock(callback);
        }

        @Override // Binder interface
        public void keyguardDone(boolean authenticated, boolean wakeup) {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"keyguardDone System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            // TODO: Remove wakeup
            mKeyguardViewMediator.keyguardDone(authenticated);
        }

        @Override // Binder interface
        public void setOccluded(boolean isOccluded) {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"setOccluded System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.setOccluded(isOccluded);
        }

        @Override // Binder interface
        public void dismiss() {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"dismiss System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.dismiss();
        }

        @Override // Binder interface
        public void onDreamingStarted() {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"onDreamingStarted System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.onDreamingStarted();
        }

        @Override // Binder interface
        public void onDreamingStopped() {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"onDreamingStopped System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.onDreamingStopped();
        }

        @Override // Binder interface
        public void onStartedGoingToSleep(int reason) {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"onStartedGoingToSleep System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.onStartedGoingToSleep(reason);
        }

        @Override // Binder interface
        public void onFinishedGoingToSleep(int reason) {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"onFinishedGoingToSleep System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.onFinishedGoingToSleep(reason);
        }

        @Override // Binder interface
        public void onStartedWakingUp() {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"onStartedWakingUp System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.onStartedWakingUp();
        }

        @Override // Binder interface
        public void onScreenTurningOn(IKeyguardDrawnCallback callback) {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"onScreenTurningOn System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.onScreenTurningOn(callback);
        }

        @Override // Binder interface
        public void onScreenTurnedOn() {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"onScreenTurnedOn System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.onScreenTurnedOn();
        }

        @Override // Binder interface
        public void onScreenTurnedOff() {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"onScreenTurnedOff System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.onScreenTurnedOff();
        }

        @Override // Binder interface
        public void setKeyguardEnabled(boolean enabled) {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"setKeyguardEnabled System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.setKeyguardEnabled(enabled);
        }

        @Override // Binder interface
        public void onSystemReady() {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"onSystemReady System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.onSystemReady();
        }

        @Override // Binder interface
        public void doKeyguardTimeout(Bundle options) {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"doKeyguardTimeout System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.doKeyguardTimeout(options);
        }

        @Override // Binder interface
        public void setCurrentUser(int userId) {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"setCurrentUser System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.setCurrentUser(userId);
        }

        @Override
        public void onBootCompleted() {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"onBootCompleted System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.onBootCompleted();
        }

        @Override
        public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"startKeyguardExitAnimation System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.startKeyguardExitAnimation(startTime, fadeoutDuration);
        }

        @Override
        public void onActivityDrawn() {
            // freeme.shanjibing, 20160608. add log for keyguard
            Log.i(TAG,"onActivityDrawn System.currentTimeMillis()="+System.currentTimeMillis());
            checkPermission();
            mKeyguardViewMediator.onActivityDrawn();
        }
    };
}

