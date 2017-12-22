package com.droi.suspension;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;

public class FreemeFloatManager {
    public static final String ACTION_SUSPENSION_TOGGLE = "com.freeme.intent.action.SUSPENSION_TOGGLE";

    private static WindowManager sWindowManager;
    private static WindowManager.LayoutParams sWindowParams;

    private static FreemeFloatView sFreemeFloatView;

    private FreemeFloatManager() {
        // Do not initialize.
    }

    private static WindowManager getWindowManager(Context context) {
        if (sWindowManager == null) {
            sWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }
        return sWindowManager;
    }

    private static void addWindow(Context context, int x, int y) {
        if (sFreemeFloatView == null) {
            WindowManager windowManager = getWindowManager(context);
            DisplayMetrics dm = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(dm);

            final int screenWidth = dm.widthPixels;
            final int screenHeight = dm.heightPixels;

            sFreemeFloatView = new FreemeFloatView(context);

            if (sWindowParams == null) {
                sWindowParams = new WindowManager.LayoutParams();
                sWindowParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                sWindowParams.format = PixelFormat.RGBA_8888;
                sWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
                sWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
                sWindowParams.width = sFreemeFloatView.getDesignedWidth();
                sWindowParams.height = sFreemeFloatView.getDesignedHeight();
            }
            if (x == 0 && y == 0) {
                sWindowParams.x = screenWidth;
                sWindowParams.y = screenHeight / 20;
            } else {
                sWindowParams.x = x;
                if (y > sWindowParams.height / 2)
                    y -= sWindowParams.height / 2;
                sWindowParams.y = y;
            }
            sFreemeFloatView.setLayoutParams(sWindowParams);

            windowManager.addView(sFreemeFloatView, sWindowParams);
        }
    }

    public static void addWindowDefault(Context context) {
        addWindow(context, 0, 0);
    }

    private static void removeWindow(Context context) {
        if (sFreemeFloatView != null) {
            getWindowManager(context).removeView(sFreemeFloatView);
            sFreemeFloatView = null;
        }
    }

    private static final class EmulatedKeyEvent {
        private ExecutorService mExecutor;
        private Instrumentation mInstrument;

        void keepAlive() {
            if (mInstrument == null) {
                mInstrument = new Instrumentation();
            }
            if (mExecutor == null || mExecutor.isShutdown()) {
                mExecutor = Executors.newSingleThreadExecutor();
            }
        }

        void sendKeyDownUpSync(final int key) {
            mExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        mInstrument.sendKeyDownUpSync(key);
                    } catch (Exception e) {
                        Log.e("Exception when doBack", e.toString());
                    }
                }
            });
        }

        void finish() {
            if (mExecutor != null && !mExecutor.isShutdown()) {
                mExecutor.shutdown();
            }
            mExecutor = null;
            mInstrument = null;
        }
    }

    private static final EmulatedKeyEvent sEmulatedKeyEvent = new EmulatedKeyEvent();

    public static void doBack(Context context) {
        sEmulatedKeyEvent.keepAlive();
        sEmulatedKeyEvent.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
    }

    public static boolean isEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                "suspension_default", 0) != 0;
    }

    public static boolean isRunning() {
        return sFreemeFloatView != null;
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, FreemeFloatWindowService.class);
        context.startService(intent);
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, FreemeFloatWindowService.class));
        removeWindow(context);
        sEmulatedKeyEvent.finish();
    }
}
