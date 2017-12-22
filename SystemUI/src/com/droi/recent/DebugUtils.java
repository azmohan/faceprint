package com.droi.recent;

import android.os.Build;
import android.util.Log;

public class DebugUtils{

    private static final String TAG = "droiRecents";

    public static final boolean DEBUG = "userdebug".equals(Build.TYPE);

    public static void i(String tag, String msg){
        Log.i(TAG, tag + " " + msg);
    }

    public static void v(String tag, String msg){
        Log.v(TAG, tag + " " + msg);
    }

    public static void e(String tag, String msg){
        Log.e(TAG, tag + " " + msg);
    }

    public static void w(String tag, String msg){
        Log.w(TAG, tag + " " + msg);
    }

    public static void d(String tag, String msg){
        Log.d(TAG, tag + " " + msg);
    }
}