
package com.droi.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.telephony.PhoneConstants;
import com.android.systemui.R;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.PlmnCarrierListener;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import com.mediatek.systemui.statusbar.util.SIMHelper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static com.android.systemui.BatteryMeterView.SHOW_PERCENT_SETTING;

//*/ freeme.zhiwei.zhang, 20160818. NavigationBar Show/Hide.
import com.droi.feature.FeatureOption;
//*/

public class DroiIconStateController implements Tunable {
    private static final String TAG = "DroiIconStateController";
    private static final boolean DEBUG = true;

    private static final String CARRIRT = "carrier";
    private static final String NETWORK_SPEED = "networkspeed";
    private static final String BATTERY_PERC = "batteryperc";

    private static final String SHOW_STATUSBAR_DATA_USAGE = "show_statusbar_data_usage";
    private static final String DATA_USE_ACTION = "com.tydtech.action.statusbar_data_use";

    private long traffic_data = 0;

    // The file path
    public final String DEV_FILE = "/proc/self/net/dev";

    // Ethernet
    final String ETHLINE = "eth0";
    String[] ethData = {
            "0", "0", "0", "0", "0", "0", "0", "0",
            "0", "0", "0", "0", "0", "0", "0", "0"
    };

    // Wifi
    final String WIFILINE = "wlan0";
    String[] wifiData = {
            "0", "0", "0", "0", "0", "0", "0", "0",
            "0", "0", "0", "0", "0", "0", "0", "0"
    };

    // Gprs
    final String GPRSLINE0 = "ccemni0"; // ccemni2
    String[] gprsData0 = {
            "0", "0", "0", "0", "0", "0", "0", "0",
            "0", "0", "0", "0", "0", "0", "0", "0"
    };

    final String GPRSLINE1 = "ccemni1";
    String[] gprsData1 = {
            "0", "0", "0", "0", "0", "0", "0", "0",
            "0", "0", "0", "0", "0", "0", "0", "0"
    };

    final String GPRSLINE_0 = "ccmni0";
    String[] gprsData_0 = {
            "0", "0", "0", "0", "0", "0", "0", "0",
            "0", "0", "0", "0", "0", "0", "0", "0"
    };

    final String GPRSLINE_1 = "ccmni1";
    String[] gprsData_1 = {
            "0", "0", "0", "0", "0", "0", "0", "0",
            "0", "0", "0", "0", "0", "0", "0", "0"
    };

    // Save old data
    String[] data = {
            "0", "0", "0", "0", "0", "0", "0", "0",
            "0", "0", "0", "0", "0", "0", "0", "0",
            "0", "0", "0", "0", "0", "0", "0", "0"
    };

    private Context mContext;
    private PhoneStatusBar mPhoneStatusBar;
    private SharedPreferences mSp;
    private Handler mHandler = new NetWorkHandler();
    private Thread mThread = null;
    public int refreshInterval = 3;
    private int mBarState;

    private NetworkControllerImpl mNetworkController;

    private TextView mCarrier;
    private TextView mKeyguardCarrier;
    private TextView mStatusbarNetworkSpeed;

    private NotificationPanelView mNotificationPanel;
    private View mNavbarScrim;
    private TextView mExpandedCarrier;
    private TextView mDataUsage;

    private boolean mNetworkFlag = false;

    private boolean mBlockNetworkSpeed = true;
    private boolean mBlockBatteryPercent = true;
    private boolean mBlockCarrier = true;

    private boolean mSim1Inserted, mSim2Inserted;
    private String mCustomizedCarrierStr = "";
    private String[] mRealCarrierStr = {"", ""};

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (DATA_USE_ACTION.equals(action)) {
                handleDatauseChanged(intent);
            } else if(ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                handleConnectivityChanged();
            }
        }
    };

    private OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
            if ("RefreshTime".equals(key)) {
                refreshInterval = sp.getInt("RefreshTime", refreshInterval) * 1000;
            } else if ("CustomCarrierLabel".equals(key)) {
                mCustomizedCarrierStr = sp.getString("CustomCarrierLabel", "");
                refreshCarrierText();
            }
        }
    };

    private ContentObserver mNavigationBarShowHideObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            final boolean navMin = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.FREEME_NAVIGATIONBAR_IS_MIN, 0) == 1;
            if(null != mNavbarScrim) {
                mNavbarScrim.post(new Runnable() {

                    @Override
                    public void run() {
                        mNavbarScrim.setVisibility(navMin ? View.GONE : View.VISIBLE);
                    }
                });
            }
        }
    };

    private ContentObserver mDataUsageObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateShowDataUsage();
        }
    };

    private PlmnCarrierListener mPlmnCarrierListener = new PlmnCarrierListener() {

        @Override
        public void updatePlmnCarrier(int slotId, boolean isSimInserted, boolean isHasSimService,
                String[] networkNames) {
            Log.d(TAG, "updateCarrierLabel, slotId=" + slotId + " isSimInserted=" + isSimInserted + " isHasSimService=" + isHasSimService + " networkNames=" + networkNames[slotId]);

            for(int i = 0 ; i < networkNames.length; i++) {
                Log.d(TAG, "" + i + ". " + networkNames[i]);
            }

            Resources res = mContext.getResources();
            String[] mNetworkNames = null;
            if(!isSimInserted) {
                mNetworkNames = new String[]{res.getString(com.android.internal.R.string.lockscreen_missing_sim_message_short),
                                         res.getString(com.android.internal.R.string.lockscreen_missing_sim_message_short)};
            } else if (!isHasSimService) {
                mNetworkNames = new String[]{res.getString(com.android.internal.R.string.lockscreen_carrier_default),
                                         res.getString(com.android.internal.R.string.lockscreen_carrier_default)};
            } else {
                mNetworkNames = networkNames;
            }

            // if two SIM were insertrd,show SIM1.
            if(slotId == PhoneConstants.SIM_ID_1) {
                mRealCarrierStr[0] = isSimInserted && isHasSimService ? networkNames[0] : mNetworkNames[0];
                mSim1Inserted = isSimInserted;
                refreshCarrierText();
                return;
            }

            if(slotId == PhoneConstants.SIM_ID_2) {
                mRealCarrierStr[1] = isSimInserted && isHasSimService ? networkNames[1] : mNetworkNames[1];
                mSim2Inserted = isSimInserted;
                refreshCarrierText();
            }
        }
    };

    class NetWorkHandler extends Handler {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case 1:
                    if (mNetworkFlag) {
                        float speed = (float) traffic_data / (1024 * 3);
                        float speed_dec = speed;
                        if(speed >= 1000) {
                            speed_dec = (float) (Math.round(speed * 10 / 1024)) / 10;
                            String speedStr = String.format("%.2fM/s", speed_dec);
                            mStatusbarNetworkSpeed.setText(speedStr);
                        } else {
                            speed_dec = (float) (Math.round(speed * 10)) / 10;
                        	if (speed_dec >= 100) {
                                mStatusbarNetworkSpeed.setText((int)speed_dec + "K/s");
                        	} else if (speed_dec >= 10) {
                                String speedStr = String.format("%.1fK/s", speed_dec);
                                mStatusbarNetworkSpeed.setText(speedStr);
                        	} else {
                        		String speedStr = String.format("%.2fK/s", speed_dec);
                                mStatusbarNetworkSpeed.setText(speedStr);
                        	}
                            
                        }
                    } else {
                        mStatusbarNetworkSpeed.setText("");
                    }
                    break;
                default:
                    break;
            }
        }
    };

    class UpdateTask implements Runnable {
        @Override
        public void run() {
            while (!mThread.currentThread().isInterrupted()) {
                refreshNetWorkSpeed();
                SystemClock.sleep(refreshInterval);
            }
        }
    }

    public DroiIconStateController(Context context, View statusBarWindow, View statusBar, View keyguardStatusBar,
            PhoneStatusBar phoneStatusBar) {
        mContext = context;
        mPhoneStatusBar = phoneStatusBar;

        mCarrier = (TextView)statusBar.findViewById(R.id.statusbar_carrier_label);
        mKeyguardCarrier = (TextView) keyguardStatusBar.findViewById(R.id.statusbar_carrier_label);
        mStatusbarNetworkSpeed = (TextView)statusBar.findViewById(R.id.statusbar_network_speed);

        mNotificationPanel =     (NotificationPanelView) statusBarWindow.findViewById(R.id.notification_panel);
        if(null != mNotificationPanel) {
            mNavbarScrim = mNotificationPanel.findViewById(R.id.droi_navbar_scrim);
            mDataUsage = (TextView)mNotificationPanel.findViewById(R.id.data_use);
            mExpandedCarrier = (TextView)mNotificationPanel.findViewById(R.id.carrier_label);
        }
    }

    public void systemuiReady() {

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(DATA_USE_ACTION);
        mContext.registerReceiver(mBroadcastReceiver, filter);

        mSp = mContext.getSharedPreferences("QuickSettings", Context.MODE_PRIVATE);
        mSp.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

        mNetworkController = mPhoneStatusBar.getNetworkController();
        mNetworkController.addPlmnCarrierListener(mPlmnCarrierListener);

        mCustomizedCarrierStr = mSp.getString("CustomCarrierLabel", "");
        refreshInterval = mSp.getInt("RefreshTime", refreshInterval) * 1000;

        TunerService.get(mContext).addTunable(this, StatusBarIconController.ICON_BLACKLIST);


        mContext.getContentResolver().unregisterContentObserver(mNavigationBarShowHideObserver);
        if (FeatureOption.FREEME_NAVIGATIONBAR_MIN) {
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.FREEME_NAVIGATIONBAR_IS_MIN),
                    true, mNavigationBarShowHideObserver);
            mNavigationBarShowHideObserver.onChange(true);
        }

        mContext.getContentResolver().unregisterContentObserver(mDataUsageObserver);
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(SHOW_STATUSBAR_DATA_USAGE),
                true, mDataUsageObserver);
        mDataUsageObserver.onChange(true);

        //refresh views carrier-batteryprec-networkspeed after systemReady
        updateShowStatusbarCarrier();
        updateShowStatusbarNetworkSpeed();
        Settings.System.putInt(mContext.getContentResolver(),
                SHOW_PERCENT_SETTING, mBlockBatteryPercent ? 0 : 1);
    }

    public void onConfigurationChanged() {
        Configuration config = mContext.getResources().getConfiguration();

        if(config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mNavbarScrim.setVisibility(View.GONE);
        } else if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (FeatureOption.FREEME_NAVIGATIONBAR_MIN) {
                boolean navMin = Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.FREEME_NAVIGATIONBAR_IS_MIN, 0) == 1;
                mNavbarScrim.setVisibility(mBarState != StatusBarState.SHADE ? View.GONE : navMin ? View.GONE : View.VISIBLE);
            }
        }
    }

    public void setStatusBarState(int newState) {
        if (mBarState == newState) {
            return;
        }

        mBarState = newState;

        if (FeatureOption.FREEME_NAVIGATIONBAR_MIN) {
            boolean navMin = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.FREEME_NAVIGATIONBAR_IS_MIN, 0) == 1;
            mNavbarScrim.setVisibility(mBarState != StatusBarState.SHADE ? View.GONE : navMin ? View.GONE : View.VISIBLE);
        }
        //*/ freeme, gouzhouping, 20160926, for removal the navigationbar space when no navigationbar.
        else {
            try {
                mNavbarScrim.setVisibility(android.view.WindowManagerGlobal.getWindowManagerService().hasNavigationBar()
                        ? View.VISIBLE : View.GONE);
            }catch (RemoteException ex) {
                
            }
        }
        //*/
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (!StatusBarIconController.ICON_BLACKLIST.equals(key)) {
            return;
        }
        ArraySet<String> blockList = StatusBarIconController.getIconBlacklist(newValue);

        boolean blockNetworkSpeed = blockList.contains(NETWORK_SPEED);
        if(mBlockNetworkSpeed != blockNetworkSpeed) {
            mBlockNetworkSpeed = blockNetworkSpeed;
            updateShowStatusbarNetworkSpeed();
        }

        boolean blockBatteryPercent = blockList.contains(BATTERY_PERC);
        if(mBlockBatteryPercent != blockBatteryPercent) {
            mBlockBatteryPercent = blockBatteryPercent;
            Settings.System.putInt(mContext.getContentResolver(),
                    SHOW_PERCENT_SETTING, mBlockBatteryPercent ? 0: 1);
        }

        boolean blockCarrier = blockList.contains(CARRIRT);
        if(mBlockCarrier != blockCarrier) {
            mBlockCarrier = blockCarrier;
            updateShowStatusbarCarrier();
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager mConnectivityManager = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (mNetworkInfo != null) {
            return mNetworkInfo.isAvailable();
        }
        return false;
    }


    void readDataDev() {
        FileReader fr = null;
        try {
            fr = new FileReader(DEV_FILE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        BufferedReader bufr = new BufferedReader(fr, 500);
        String line;
        String[] data_temp;
        String[] netData;
        int k, j;

        try {
            while ((line = bufr.readLine()) != null) {
                data_temp = line.trim().split(":");
                if (line.contains(ETHLINE)) {
                    netData = data_temp[1].trim().split(" ");
                    for (k = 0, j = 0; k < netData.length; k++) {
                        if (netData[k].length() > 0) {
                            ethData[j] = netData[k];
                            j++;
                        }
                    }
                } else if (line.contains(GPRSLINE0)) {
                    netData = data_temp[1].trim().split(" ");
                    for (k = 0, j = 0; k < netData.length; k++) {
                        if (netData[k].length() > 0) {
                            gprsData0[j] = netData[k];
                            j++;
                        }
                    }
                } else if (line.contains(GPRSLINE1)) {
                    netData = data_temp[1].trim().split(" ");
                    for (k = 0, j = 0; k < netData.length; k++) {
                        if (netData[k].length() > 0) {
                            gprsData1[j] = netData[k];
                            j++;
                        }
                    }
                } else if (line.contains(GPRSLINE_0)) {
                    netData = data_temp[1].trim().split(" ");
                    for (k = 0, j = 0; k < netData.length; k++) {
                        if (netData[k].length() > 0) {
                            gprsData_0[j] = netData[k];
                            j++;
                        }
                    }
                } else if (line.contains(GPRSLINE_1)) {
                    netData = data_temp[1].trim().split(" ");
                    for (k = 0, j = 0; k < netData.length; k++) {
                        if (netData[k].length() > 0) {
                            gprsData_1[j] = netData[k];
                            j++;
                        }
                    }
                } else if (line.contains(WIFILINE)) {
                    netData = data_temp[1].trim().split(" ");
                    for (k = 0, j = 0; k < netData.length; k++) {
                        if (netData[k].length() > 0) {
                            wifiData[j] = netData[k];
                            j++;
                        }
                    }
                }
            }
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    void refreshNetWorkSpeed() {
        readDataDev();

        long[] delta = new long[24];
        delta[0] = Long.parseLong(ethData[0]) - Long.parseLong(data[0]);
        delta[1] = Long.parseLong(ethData[1]) - Long.parseLong(data[1]);
        delta[2] = Long.parseLong(ethData[8]) - Long.parseLong(data[2]);
        delta[3] = Long.parseLong(ethData[9]) - Long.parseLong(data[3]);

        delta[4] = Long.parseLong(gprsData0[0]) - Long.parseLong(data[4]);
        delta[5] = Long.parseLong(gprsData0[1]) - Long.parseLong(data[5]);
        delta[6] = Long.parseLong(gprsData0[8]) - Long.parseLong(data[6]);
        delta[7] = Long.parseLong(gprsData0[9]) - Long.parseLong(data[7]);

        delta[8] = Long.parseLong(gprsData1[0]) - Long.parseLong(data[8]);
        delta[9] = Long.parseLong(gprsData1[1]) - Long.parseLong(data[9]);
        delta[10] = Long.parseLong(gprsData1[8]) - Long.parseLong(data[10]);
        delta[11] = Long.parseLong(gprsData1[9]) - Long.parseLong(data[11]);

        delta[12] = Long.parseLong(gprsData_0[0]) - Long.parseLong(data[12]);
        delta[13] = Long.parseLong(gprsData_0[1]) - Long.parseLong(data[13]);
        delta[14] = Long.parseLong(gprsData_0[8]) - Long.parseLong(data[14]);
        delta[15] = Long.parseLong(gprsData_0[9]) - Long.parseLong(data[15]);

        delta[16] = Long.parseLong(gprsData_1[0]) - Long.parseLong(data[16]);
        delta[17] = Long.parseLong(gprsData_1[1]) - Long.parseLong(data[17]);
        delta[18] = Long.parseLong(gprsData_1[8]) - Long.parseLong(data[18]);
        delta[19] = Long.parseLong(gprsData_1[9]) - Long.parseLong(data[19]);

        delta[20] = Long.parseLong(wifiData[0]) - Long.parseLong(data[20]);
        delta[21] = Long.parseLong(wifiData[1]) - Long.parseLong(data[21]);
        delta[22] = Long.parseLong(wifiData[8]) - Long.parseLong(data[22]);
        delta[23] = Long.parseLong(wifiData[9]) - Long.parseLong(data[23]);

        data[0] = ethData[0];
        data[1] = ethData[1];
        data[2] = ethData[8];
        data[3] = ethData[9];

        data[4] = gprsData0[0];
        data[5] = gprsData0[1];
        data[6] = gprsData0[8];
        data[7] = gprsData0[9];

        data[8] = gprsData1[0];
        data[9] = gprsData1[1];
        data[10] = gprsData1[8];
        data[11] = gprsData1[9];

        data[12] = gprsData_0[0];
        data[13] = gprsData_0[1];
        data[14] = gprsData_0[8];
        data[15] = gprsData_0[9];

        data[16] = gprsData_1[0];
        data[17] = gprsData_1[1];
        data[18] = gprsData_1[8];
        data[19] = gprsData_1[9];

        data[20] = wifiData[0];
        data[21] = wifiData[1];
        data[22] = wifiData[8];
        data[23] = wifiData[9];

        traffic_data = delta[0] + delta[4] + delta[8] + delta[12] + delta[16] + delta[20];
        if(traffic_data < 0) {
            traffic_data = 0;
        }
        Message msg = mHandler.obtainMessage();
        msg.what = 1;
        //msg.arg1 = traffic_data;
        mHandler.sendMessage(msg);
    }

    /*-------------------------network speed ------------------------------------------------*/
    private void handleConnectivityChanged() {
        if (isNetworkConnected()) {
            mNetworkFlag = true;
            if (!mBlockNetworkSpeed && mThread == null) {
                updateNetworkSpeed();
            }
        } else {
            mNetworkFlag = false;
            if (mThread != null && mThread.isAlive()) {
                mThread.interrupt();
                mThread = null;
            }
            mStatusbarNetworkSpeed.setText("");
        }
    }

    private void updateNetworkSpeed() {
        if (mThread != null && mThread.isAlive()) {
            mThread.interrupt();
            mThread = null;
        }
        mThread = new Thread(new UpdateTask(), "NetworkSpeed");
        mThread.start();
    }

    private void updateShowStatusbarNetworkSpeed() {
        ViewGroup.LayoutParams lp = mStatusbarNetworkSpeed.getLayoutParams();
        lp.height = mBlockNetworkSpeed ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.width = mBlockNetworkSpeed ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
        mStatusbarNetworkSpeed.setLayoutParams(lp);

        if(!mBlockNetworkSpeed) {
            if(isNetworkConnected()) {
                mNetworkFlag = true;
                updateNetworkSpeed();
            } else {
                mNetworkFlag = false;
                mStatusbarNetworkSpeed.setText("0.00K/s");
            }
        } else {
            if(mThread != null && mThread.isAlive()) {
                mThread.interrupt();
                mThread = null;
            }
            mStatusbarNetworkSpeed.setText("");
        }
    }

    /*-------------------------carrier & expanded carrier------------------------------------------------*/
    private void refreshCarrierText() {
        if(null != mCarrier) {
            mCarrier.post(new Runnable() {

                @Override
                public void run() {
                    if(TextUtils.isEmpty(mCustomizedCarrierStr)) {
                        mCarrier.setText(mSim1Inserted ? mRealCarrierStr[0] : mSim2Inserted ? mRealCarrierStr[1] : mRealCarrierStr[0]);
                    } else {
                        mCarrier.setText(mCustomizedCarrierStr);
                    }
                }
            });
        }

        if(null != mKeyguardCarrier) {
            mKeyguardCarrier.post(new Runnable() {

                @Override
                public void run() {
                    if(TextUtils.isEmpty(mCustomizedCarrierStr)) {
                        mKeyguardCarrier.setText(mSim1Inserted ? mRealCarrierStr[0] : mSim2Inserted ? mRealCarrierStr[1] : mRealCarrierStr[0]);
                    } else {
                        mKeyguardCarrier.setText(mCustomizedCarrierStr);
                    }
                }
            });
        }

        if(null != mExpandedCarrier) {
            mExpandedCarrier.post(new Runnable() {

                @Override
                public void run() {
                    StringBuffer buffer = new StringBuffer();
                    if(mSim1Inserted) {
                        buffer.append(mRealCarrierStr[0]);

                        if(mSim2Inserted) {
                            buffer.append(" | " + mRealCarrierStr[1]);
                        }
                    } else if (mSim2Inserted) {
                        buffer.append(mRealCarrierStr[1]);
                    } else {
                        // Default 'No Service'
                        buffer.append(mRealCarrierStr[0]);
                    }
                    mExpandedCarrier.setText(buffer.toString());
                }
            });
        }
    }

    private void updateShowStatusbarCarrier() {
        if(null != mCarrier) {
            ViewGroup.LayoutParams lp = mCarrier.getLayoutParams();
            lp.height = mBlockCarrier ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.width = mBlockCarrier ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;

            mCarrier.setLayoutParams(lp);
        }

        if(null != mKeyguardCarrier) {
            ViewGroup.LayoutParams lp = mKeyguardCarrier.getLayoutParams();
            lp.height = mBlockCarrier ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.width = mBlockCarrier ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;

            mKeyguardCarrier.setLayoutParams(lp);
        }
    }

    /*------------------------- data use ------------------------------------------------*/
    private void updateShowDataUsage() {
        final boolean show = Settings.System.getInt(mContext.getContentResolver(),
                SHOW_STATUSBAR_DATA_USAGE, 0) != 0;
        ViewGroup.LayoutParams lp = mDataUsage.getLayoutParams();
        lp.height = show ? LinearLayout.LayoutParams.WRAP_CONTENT : 0;
        mDataUsage.setLayoutParams(lp);
    }

    private void handleDatauseChanged(Intent intent) {
        StringBuffer info = new StringBuffer();

        String dataUsage = "";
        String todayUse, monthRemain, monthPackage;
        String strSeparator = mContext.getResources().getString(R.string.status_bar_network_name_separator);
        boolean isExceed = false;

        if(SIMHelper.isSimInsertedBySlot(mContext, PhoneConstants.SIM_ID_1)) {
            isExceed = intent.getBooleanExtra("month_outnumber1", false);
            todayUse = intent.getStringExtra("today_used1");
            monthRemain = intent.getStringExtra("month_remain1");
            monthPackage = intent.getStringExtra("traffic_package1");

            if(isExceed) {
                dataUsage = "" + mContext.getResources().getString(
                    R.string.statusbar_data_use_exceed_new,
                    todayUse, monthRemain);
            } else {
                dataUsage = "" + mContext.getResources().getString(
                    R.string.statusbar_data_use_normal_new,
                    todayUse, monthRemain);
            }

            info.append(dataUsage);

            if(DEBUG) {
                Log.d(TAG, "-->card1 data usage:" + dataUsage);
            }
        }

        if(SIMHelper.isSimInsertedBySlot(mContext, PhoneConstants.SIM_ID_2)) {
            isExceed = intent.getBooleanExtra("month_outnumber2", false);
            todayUse = intent.getStringExtra("today_used2");
            monthRemain = intent.getStringExtra("month_remain2");
            monthPackage = intent.getStringExtra("traffic_package2");

            if(isExceed) {
                dataUsage ="" + mContext.getResources().getString(
                    R.string.statusbar_data_use_exceed_new,
                    todayUse, monthRemain);
            } else {
                dataUsage = "" + mContext.getResources().getString(
                    R.string.statusbar_data_use_normal_new,
                    todayUse, monthRemain);
            }

            if(DEBUG) {
                Log.d(TAG, "-->card2 data usage:" + dataUsage);
            }

            if(info.length() > 0 && !TextUtils.isEmpty(dataUsage)) {
                info.append(" " + strSeparator + " ");
            }
            info.append(dataUsage);
        }

        final String str = info.toString();
        if(null != mDataUsage) {
            mDataUsage.post(new Runnable() {

                @Override
                public void run() {
                    mDataUsage.setText(str);
                }
            });
        }
    }
}
