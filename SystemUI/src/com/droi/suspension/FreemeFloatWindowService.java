package com.droi.suspension;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

public class FreemeFloatWindowService extends Service {

    private final Handler mHandler = new Handler();

    private Timer mTimer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (!FreemeFloatManager.isRunning()) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                FreemeFloatManager.addWindowDefault(getApplicationContext());
                            }
                        });
                    }
                }
            }, 0, 200);
        }
        return super.onStartCommand(intent, START_FLAG_REDELIVERY | START_FLAG_RETRY, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
        mTimer = null;

        mHandler.removeCallbacksAndMessages(null);
    }
}
