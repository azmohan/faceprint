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

package com.android.settings.deviceinfo;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.ArrayUtils;
import com.android.settings.InstrumentedPreferenceActivity;
import com.android.settings.R;
import com.android.settings.Utils;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;


import java.lang.ref.WeakReference;

//*/ freeme.xupeng, 20161221. Add two serial number
import android.os.AsyncResult;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;
import com.mediatek.NvRAMAgent.NvRAMAgent;
//*/

/**
 * Display the following information
 * # Battery Strength  : TODO
 * # Uptime
 * # Awake Time
 * # XMPP/buzz/tickle status : TODO
 *
 */
public class Status extends InstrumentedPreferenceActivity {

    private static final String KEY_BATTERY_STATUS = "battery_status";
    private static final String KEY_BATTERY_LEVEL = "battery_level";
    private static final String KEY_IP_ADDRESS = "wifi_ip_address";
    private static final String KEY_WIFI_MAC_ADDRESS = "wifi_mac_address";
    private static final String KEY_BT_ADDRESS = "bt_address";
    private static final String KEY_SERIAL_NUMBER = "serial_number";
    private static final String KEY_WIMAX_MAC_ADDRESS = "wimax_mac_address";
    private static final String KEY_SIM_STATUS = "sim_status";
    private static final String KEY_IMEI_INFO = "imei_info";

    // Broadcasts to listen to for connectivity changes.
    private static final String[] CONNECTIVITY_INTENTS = {
            BluetoothAdapter.ACTION_STATE_CHANGED,
            ConnectivityManager.CONNECTIVITY_ACTION,
            WifiManager.LINK_CONFIGURATION_CHANGED_ACTION,
            WifiManager.NETWORK_STATE_CHANGED_ACTION,
    };

    private static final int EVENT_UPDATE_STATS = 500;

    private static final int EVENT_UPDATE_CONNECTIVITY = 600;

    private ConnectivityManager mCM;
    private WifiManager mWifiManager;

    private Resources mRes;

    private String mUnknown;
    private String mUnavailable;

    private Preference mUptime;
    private Preference mBatteryStatus;
    private Preference mBatteryLevel;
    private Preference mBtAddress;
    private Preference mIpAddress;
    private Preference mWifiMacAddress;
    private Preference mWimaxMacAddress;

    private Handler mHandler;

    private ISettingsMiscExt mExt;

    private static class MyHandler extends Handler {
        private WeakReference<Status> mStatus;

        public MyHandler(Status activity) {
            mStatus = new WeakReference<Status>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Status status = mStatus.get();
            if (status == null) {
                return;
            }

            switch (msg.what) {
                case EVENT_UPDATE_STATS:
                    status.updateTimes();
                    sendEmptyMessageDelayed(EVENT_UPDATE_STATS, 1000);
                    break;

                case EVENT_UPDATE_CONNECTIVITY:
                    status.updateConnectivity();
                    break;
            }
        }
    }

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBatteryLevel.setSummary(Utils.getBatteryPercentage(intent));
                mBatteryStatus.setSummary(Utils.getBatteryStatus(getResources(), intent));
            }
        }
    };

    private IntentFilter mConnectivityIntentFilter;
    private final BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ArrayUtils.contains(CONNECTIVITY_INTENTS, action)) {
                mHandler.sendEmptyMessage(EVENT_UPDATE_CONNECTIVITY);
            }
        }
    };

    private boolean hasBluetooth() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    private boolean hasWimax() {
        return  mCM.getNetworkInfo(ConnectivityManager.TYPE_WIMAX) != null;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mExt = UtilsExt.getMiscPlugin(this);

        mHandler = new MyHandler(this);

        mCM = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        addPreferencesFromResource(R.xml.device_info_status);
        mBatteryLevel = findPreference(KEY_BATTERY_LEVEL);
        mBatteryStatus = findPreference(KEY_BATTERY_STATUS);
        mBtAddress = findPreference(KEY_BT_ADDRESS);
        mWifiMacAddress = findPreference(KEY_WIFI_MAC_ADDRESS);
        mWimaxMacAddress = findPreference(KEY_WIMAX_MAC_ADDRESS);
        mIpAddress = findPreference(KEY_IP_ADDRESS);

        ///M: feature replace sim to uim
        changeSimTitle();

        mRes = getResources();
        mUnknown = mRes.getString(R.string.device_info_default);
        mUnavailable = mRes.getString(R.string.status_unavailable);

        // Note - missing in zaku build, be careful later...
        mUptime = findPreference("up_time");

        if (!hasBluetooth()) {
            getPreferenceScreen().removePreference(mBtAddress);
            mBtAddress = null;
        }

        if (!hasWimax()) {
            getPreferenceScreen().removePreference(mWimaxMacAddress);
            mWimaxMacAddress = null;
        }

        mConnectivityIntentFilter = new IntentFilter();
        for (String intent: CONNECTIVITY_INTENTS) {
             mConnectivityIntentFilter.addAction(intent);
        }

        updateConnectivity();

        //*/ freeme.xupeng, 20161221. Add two serial number
        if (SystemProperties.get("ro.freeme.dual_serial").equals("1")) {
            getMainBoardNumber();
        } else {
            String serial = Build.SERIAL;
            if (serial != null && !serial.equals("")) {
                setSummaryText(KEY_SERIAL_NUMBER, serial);
            } else {
                removePreferenceFromScreen(KEY_SERIAL_NUMBER);
            }
        }
        //*/
        //*/ freeme.xupeng, 20170615. remove sim status for XLJ
        if (com.droi.feature.FeatureOption.FREEME_XLJ_VERSION) {
            removePreferenceFromScreen(KEY_SIM_STATUS);
        }
        //*/

        //Remove SimStatus and Imei for Secondary user as it access Phone b/19165700
        if (Utils.isWifiOnly(this) || UserHandle.myUserId() != UserHandle.USER_OWNER) {
            removePreferenceFromScreen(KEY_SIM_STATUS);
            removePreferenceFromScreen(KEY_IMEI_INFO);
        }

        // Make every pref on this screen copy its data to the clipboard on longpress.
        // Super convenient for capturing the IMEI, MAC addr, serial, etc.
        getListView().setOnItemLongClickListener(
            new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view,
                        int position, long id) {
                    ListAdapter listAdapter = (ListAdapter) parent.getAdapter();
                    Preference pref = (Preference) listAdapter.getItem(position);

                    ClipboardManager cm = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setText(pref.getSummary());
                    Toast.makeText(
                        Status.this,
                        com.android.internal.R.string.text_copied,
                        Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVICEINFO_STATUS;
    }

    /**
     * for replace SIM to UIM.
     */
    private void changeSimTitle() {
        findPreference(KEY_SIM_STATUS).setTitle(
                mExt.customizeSimDisplayString(
                        findPreference(KEY_SIM_STATUS).getTitle().toString(),
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID));
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mConnectivityReceiver, mConnectivityIntentFilter,
                         android.Manifest.permission.CHANGE_NETWORK_STATE, null);
        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mHandler.sendEmptyMessage(EVENT_UPDATE_STATS);
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(mBatteryInfoReceiver);
        unregisterReceiver(mConnectivityReceiver);
        mHandler.removeMessages(EVENT_UPDATE_STATS);
    }

    /**
     * Removes the specified preference, if it exists.
     * @param key the key for the Preference item
     */
    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    /**
     * @param preference The key for the Preference item
     * @param property The system property to fetch
     * @param alt The default value, if the property doesn't exist
     */
    private void setSummary(String preference, String property, String alt) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property, alt));
        } catch (RuntimeException e) {

        }
    }

    private void setSummaryText(String preference, String text) {
            if (TextUtils.isEmpty(text)) {
               text = mUnknown;
             }
             // some preferences may be missing
             if (findPreference(preference) != null) {
                 findPreference(preference).setSummary(text);
             }
    }

    private void setWimaxStatus() {
        if (mWimaxMacAddress != null) {
            String macAddress = SystemProperties.get("net.wimax.mac.address", mUnavailable);
            mWimaxMacAddress.setSummary(macAddress);
        }
    }

    private void setWifiStatus() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        mWifiMacAddress.setSummary(!TextUtils.isEmpty(macAddress) ? macAddress : mUnavailable);
    }

    private void setIpAddressStatus() {
        String ipAddress = Utils.getDefaultIpAddresses(this.mCM);
        if (ipAddress != null) {
            mIpAddress.setSummary(ipAddress);
        } else {
            mIpAddress.setSummary(mUnavailable);
        }
    }

    private void setBtStatus() {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth != null && mBtAddress != null) {
            String address = bluetooth.isEnabled() ? bluetooth.getAddress() : null;
            if (!TextUtils.isEmpty(address)) {
               // Convert the address to lowercase for consistency with the wifi MAC address.
                mBtAddress.setSummary(address.toLowerCase());
            } else {
                mBtAddress.setSummary(mUnavailable);
            }
        }
    }

    void updateConnectivity() {
        setWimaxStatus();
        setWifiStatus();
        setBtStatus();
        setIpAddressStatus();
    }

    void updateTimes() {
        long at = SystemClock.uptimeMillis() / 1000;
        long ut = SystemClock.elapsedRealtime() / 1000;

        if (ut == 0) {
            ut = 1;
        }

        mUptime.setSummary(convert(ut));
    }

    private String pad(int n) {
        if (n >= 10) {
            return String.valueOf(n);
        } else {
            return "0" + String.valueOf(n);
        }
    }

    private String convert(long t) {
        int s = (int)(t % 60);
        int m = (int)((t / 60) % 60);
        int h = (int)((t / 3600));

        return h + ":" + pad(m) + ":" + pad(s);
    }

    //*/ freeme.xupeng, 20161221. Add two serial number
    Handler barcodeHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    String[] result = (String[]) ar.result;
                    String rawDate = "";
                    String mainBoard = "";
                    if (result != null && result.length > 0) {
                        rawDate = result[0];
                    }
                    if (rawDate.length() > 0) {
                        int start = rawDate.indexOf("\"");
                        int end = rawDate.lastIndexOf("\"");
                        if (end > start + 1) {
                            mainBoard = rawDate.substring(start + 1, end);
                            if (mainBoard.contains(" ")) {
                                mainBoard = mainBoard.substring(0, mainBoard.indexOf(" "));
                            }
                        }
                        Log.d("NVRAM-j", "rawDate = " + rawDate + ", main board:{" + mainBoard + "}");
                    }
                    String[] productInfo = getproductInfo();
                    String serial = "";
                    if (productInfo[1] != null && !productInfo[1].equals("")) {
                        serial = productInfo[1];
                    } else if (!mainBoard.equals("")) {
                        serial = mainBoard;
                    } else if (productInfo[0] != null && !productInfo[0].equals("")) {
                        serial = productInfo[0];
                    }
                    if (!serial.equals("")) {
                        setSummaryText(KEY_SERIAL_NUMBER, serial);
                    } else {
                        removePreferenceFromScreen(KEY_SERIAL_NUMBER);
                    }
                    break;
            }
        }
    };

    public static boolean isSupported(String featureKey) {
        return "1".equals(SystemProperties.get(featureKey));
    }

    private void getMainBoardNumber() {
        final String FK_MTK_C2K_SUPPORT = "ro.mtk_c2k_support";
        final String FK_MTK_SVLTE_SUPPORT = "ro.mtk_svlte_support";
        final String FK_SRLTE_SUPPORT = "ro.mtk_srlte_support";
        final String FK_EVDO_DT_SUPPORT = "ro.evdo_dt_support";

        Phone mPhone = PhoneFactory.getDefaultPhone();
        if (isSupported(FK_MTK_C2K_SUPPORT)) {
            if ((isSupported(FK_MTK_SVLTE_SUPPORT)
                    || isSupported(FK_SRLTE_SUPPORT))
                    && mPhone instanceof LteDcPhoneProxy) {
                mPhone = ((LteDcPhoneProxy) mPhone).getLtePhone();
            }
            if (isSupported(FK_EVDO_DT_SUPPORT)
                    && mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2);
            }
        }
        mPhone.invokeOemRilRequestStrings(new String[]{"AT+EGMR=0,5", "+EGMR"}, barcodeHandler.obtainMessage(100));
    }

    private String[] getproductInfo() {
        String FILED_ID = "/data/nvram/APCFG/APRDEB/PRODUCT_INFO";
        int PRODUCT_INFO_SIZE = 1024;
        int BARCODE_DIGITS = 16;
        int SERIALNOMER_DIGITS = 16;
        int SERIALNOMER_OFFSET = 64 + 4 * 10 + 12;
        NvRAMAgent agent = NvRAMAgent.Stub.asInterface(ServiceManager.getService("NvRAMAgent"));
        byte[] buff = null;
        try {
            buff = agent.readFileByName(FILED_ID);// read buffer from nvram
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        String[] productInfo = new String[2];
        if (buff != null) {
            if (PRODUCT_INFO_SIZE == buff.length) {
                byte[] bBarcode = new byte[BARCODE_DIGITS];
                byte[] bSerialno = new byte[SERIALNOMER_DIGITS];
                System.arraycopy(buff, 0, bBarcode, 0, BARCODE_DIGITS);
                System.arraycopy(buff, SERIALNOMER_OFFSET, bSerialno, 0, SERIALNOMER_DIGITS);
                if (isNumberOrLetter(bBarcode)) {
                    productInfo[0] = new String(bBarcode);
                } else {
                    productInfo[0] = Build.SERIAL;
                }
                if (isNumberOrLetter(bSerialno)) {
                    productInfo[1] = new String(bSerialno);
                } else {
                    productInfo[1] = "";
                }
                StringBuilder sb = new StringBuilder();
                for (int i=0;i<BARCODE_DIGITS;i++) {
                    sb.append(bBarcode[i]).append("-");
                }
                sb.append("\n");
                for (int i=0;i<SERIALNOMER_DIGITS;i++) {
                    sb.append(bSerialno[i]).append("-");
                }
                Log.d("NVRAM-j", "bBarcode + bSerialno: " + sb.toString());
            } else {
                Log.e("NVRAM-j", FILED_ID + " size is " + buff.length);
            }
        } else {
            Log.e("NVRAM-j", "buff is bull");
        }
        return productInfo;
    }

    private boolean isNumberOrLetter(byte[] src) {
        boolean ret = false;
        int size = src.length;
        for (int i = 0; i < size; i++) {
            if ((src[i] >= 48 && src[i] <= 57) || (src[i] >= 65 && src[i] <= 70)
                    || (src[i] >= 97 && src[i] <= 102)) {
                ret = true;
                break;
            }
        }
        return ret;
    }
    //*/
}
