package com.android.keyguard;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

public class VisitorModeUtil {
    private static final String TAG = "VistorModeUtil";
    public static final String VISTOR_MODE_STATE = "tydtech_vistor_mode_state";
    public static final String VISTOR_MODE_STATE_EXTRA = "state";
    
    public static final String VISTOR_MODE_OFF = "com.tydtech.ACTION_VISITOR_MODE_OFF";
    public static final String VISTOR_MODE_ON = "com.tydtech.ACTION_VISITOR_MODE_ON";
    
    public static final String VISTOR_MODE_PASSWORD_STATE = "tydtech_vistor_mode_password_state";
    public static final String VISTOR_MODE_PASSWORD_STATE_NONE = "tydtech_vistor_mode_password_state_none";
    public static final String VISTOR_MODE_PASSWORD_STATE_PASSWORD = "tydtech_vistor_mode_state_password";
    public static final String VISTOR_MODE_PASSWORD_STATE_PATTERN = "tydtech_vistor_mode_state_pattern";
    
    public static final String VISTOR_MODE_PATTERN = "tydtech_vistor_mode_pattern";
    public static final String VISTOR_MODE_PASSWORD = "tydtech_vistor_mode_password";
    //add by huangyiquan 20141028 for save the length of password
    public static final String VISTOR_MODE_PASSWORD_LENGTH = "vistor_mode_password_length";
    //end
    public static String getVistorSettingsValue(Context context, String key) {
        String value = "";
        try {
            value = Settings.System.getString(context.getContentResolver(), key);
        } catch (Exception e) {
            Log.e(TAG, " getVistorSettingsValue Exception " + e.toString());
        }
        return value;
    }

    public static boolean getVistorModelState(Context context) {
        try {
            int model = Settings.System.getInt(context.getContentResolver(), VISTOR_MODE_STATE);
            return model == 1;
        } catch (Exception e) {
            Log.e(TAG, " getVistorModelState Exception " + e.toString());
        }
        return false;
    }

    public static void putVistorModelState(Context context, int model) {
        Settings.System.putInt(context.getContentResolver(), VISTOR_MODE_STATE, model);
    }
    
    public static String getVistorModelType(Context context) {
        return Settings.System.getString(context.getContentResolver(), VISTOR_MODE_PASSWORD_STATE);
    }

    public static String getMd5Value(String sSecret) {
        try {
            MessageDigest bmd5 = MessageDigest.getInstance("MD5");
            bmd5.update(sSecret.getBytes());
            StringBuffer buf = new StringBuffer();
            byte[] b = bmd5.digest();
            for (int offset = 0x0; offset < b.length; offset = offset + 0x1) {
                int i = b[offset];
                if (i < 0) {
                    i = i + 0x100;
                }
                if (i < 0x10) {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
