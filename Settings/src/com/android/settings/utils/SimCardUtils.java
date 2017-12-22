package com.android.settings.utils;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
/**
 * Add this class for SIM support.
 */
public class SimCardUtils {

    public static final String TAG = "ProviderSimCardUtils";
    private static final String ACCOUNT_TYPE_POSTFIX = " Account";
    public static TelephonyManager sTelephonyManager;

    /**
     * M: Structure function.
     * @param context context
     */
    public SimCardUtils(Context context) {
        sTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    /**
     * M: add for mark SIM type.
     */
    public interface SimType {
        String SIM_TYPE_SIM_TAG = "SIM";
        int SIM_TYPE_SIM = 0;

        String SIM_TYPE_USIM_TAG = "USIM";
        int SIM_TYPE_USIM = 1;

        // UIM
        int SIM_TYPE_UIM = 2;
        int SIM_TYPE_CSIM = 3;
        String SIM_TYPE_UIM_TAG = "RUIM";
        // UIM
        // UICC TYPE
        String SIM_TYPE_CSIM_TAG = "CSIM";
        // UICC TYPE
    }

    /**
     * M: [Gemini+] all possible icc card type are put in this array. it's a map
     * of SIM_TYPE => SIM_TYPE_TAG like SIM_TYPE_SIM => "SIM"
     */
    private static final SparseArray<String> SIM_TYPE_ARRAY = new SparseArray<String>();
    static {
        SIM_TYPE_ARRAY.put(SimType.SIM_TYPE_SIM, SimType.SIM_TYPE_SIM_TAG);
        SIM_TYPE_ARRAY.put(SimType.SIM_TYPE_USIM, SimType.SIM_TYPE_USIM_TAG);
        SIM_TYPE_ARRAY.put(SimType.SIM_TYPE_UIM, SimType.SIM_TYPE_UIM_TAG);
        SIM_TYPE_ARRAY.put(SimType.SIM_TYPE_CSIM, SimType.SIM_TYPE_CSIM_TAG);
    }

    /**
     * M: check whether the SIM is inserted.
     * @param slotId the slot id of SIM.
     * @return whether the SIM is inserted.
     */
    public static boolean isSimInserted(int slotId,Context context) {
        boolean isSimInsert = false;
        if(sTelephonyManager == null){
            sTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        }
        if (sTelephonyManager != null) {
            if (isGeminiSupport()) {
                isSimInsert = sTelephonyManager.hasIccCard(slotId);
            } else {
                isSimInsert = sTelephonyManager.hasIccCard(0);
            }
        }

        return isSimInsert;
    }

    /**
     * [Gemini] whether Gemini feature enabled on this device.
     * @return ture if allowed to enable
     */
    public static boolean isGeminiSupport() {
        return TelephonyManager.getDefault().getSimCount() > 1;
    }



    /**
     * M: [Gemini+] get the readable sim account type, like "SIM Account".
     *
     * @param simType
     * the integer sim type
     * @return the string like "SIM Account"
     */
    public static String getSimAccountType(int simType) {
        return SIM_TYPE_ARRAY.get(simType) + ACCOUNT_TYPE_POSTFIX;
    }

    /**
     * M: [Gemini+]SIM account type is a string like "USIM Account".
     *
     * @param accountType the account type
     * @return whether the account is sim account
     */
    public static boolean isSimAccount(String accountType) {
        for (int i = 0; i < SIM_TYPE_ARRAY.size(); i++) {
            int simType = SIM_TYPE_ARRAY.keyAt(i);
            if (TextUtils.equals(getSimAccountType(simType), accountType)) {
                return true;
            }
        }
        Log.d(TAG, "account " + accountType + " is not SIM account");
        return false;
    }
}

