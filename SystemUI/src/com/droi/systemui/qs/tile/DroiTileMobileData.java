package com.droi.systemui.qs.tile;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.PhoneConstants;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.MobileDataController;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;
import com.mediatek.systemui.qs.tiles.ext.QsIconWrapper;
import com.mediatek.systemui.statusbar.extcb.BehaviorSet;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.PluginFactory;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import com.mediatek.systemui.statusbar.util.FeatureOptions;

//*/freeme.zhangshaopiao,20170904.added for wireless toggle
import android.os.SystemProperties;
//*/

/**
 * M: Mobile Data Connection Tile.
 */
public class DroiTileMobileData extends QSTile<QSTile.SignalState> {
    private static final boolean DEBUG = true;

    //*/ Added by tyd hanhao for data switcher 2015-08-22
    private static final String MOBILE_DATA_CHANGED = "com.android.systemui.qstile.MOBILE_DATA";
    //*/
    
    //*/ freeme, gouzhouping, 20161226, for cancel show SIMSwitcherDialog when airplane enable.
    private boolean isAirplaneEnabled;
    //*/
    
    private static final int QS_MOBILE_DISABLE = R.drawable.ic_qs_mobile_off;
    private static final int QS_MOBILE_ENABLE = R.drawable.ic_qs_mobile_white;

    private static final int DATA_DISCONNECT = 0;
    private static final int DATA_CONNECT = 1;
    private static final int AIRPLANE_DATA_CONNECT = 2;
    private static final int DATA_CONNECT_DISABLE = 3;
    private static final int DATA_RADIO_OFF = 4;

    private final NetworkController mController;
    private final MobileDataController mDataController;

    private int mDataConnectionState = DATA_DISCONNECT;
    private int mDataStateIconId = QS_MOBILE_DISABLE;
    private final IconIdWrapper mDataStateIconIdWrapper = new IconIdWrapper();
    private final Icon mDataStateIcon = new QsIconWrapper(mDataStateIconIdWrapper);

    private final MobileDataSignalCallback mCallback = new MobileDataSignalCallback();

    //*/ Added by tyd hanhao for data switcher 2015-08-22
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MOBILE_DATA_CHANGED)) {
                final boolean dataOn = intent.getBooleanExtra("DATA_CHANGED", false);
                Log.d(TAG, "receive from settings dataOn = " + dataOn);
                new Handler().postDelayed(new Runnable() {
                
                    @Override
                    public void run() {
                        mDataController.updateMobileDtatState(dataOn);
                        refreshState();
                    }
                }, 3000);
                refreshState();
            }
        }
    };
    //*/

    /**
     * Constructs a new MobileDataTile instance with Host.
     * @param host A Host object.
     */
    public DroiTileMobileData(Host host) {
        super(host);
        mController = host.getNetworkController();
        mDataController = mController.getMobileDataController();
        
        //*/ Added by tyd hanhao for data switcher 2015-08-22
        IntentFilter filter = new IntentFilter();
        filter.addAction(MOBILE_DATA_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
        //*/
        
        if (DEBUG) {
            Log.d(TAG, "create MobileDataTile");
        }
    }

    @Override
    public void setListening(boolean listening) {
        if (DEBUG) {
            Log.d(TAG, "setListening = " + listening);
        }
        if (listening) {
            mController.addSignalCallback(mCallback);
        } else {
            mController.removeSignalCallback(mCallback);
        }
    }

    @Override
    protected SignalState newTileState() {
        return new SignalState();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_PANEL;
    }

    @Override
    protected void handleClick() {
        //*/ Modified by tyd hanhao for data switcher 2015-08-22
        /*/freeme.gejun, always show dialog when double sim
        if(isMultSimInserted() && !TelephonyManager.from(mContext).getDataEnabled()) {
        /*/
        if (isMultSimInserted() && !isAirplaneEnabled && !FeatureOptions.MTK_CMCC_SUPPORT) {
            //*/
            handleShowSIMSwitcherDialog();
        } else {
            handleDataSwitch();
        }

        //*/freeme.zhangshaopiao,20170904.added for wireless toggle
        if (SystemProperties.get("ro.freeme.xlj_jingdong").equals("1")){
            getHost().getContext().sendBroadcast(new Intent("wirelessSettings.update"));
        }
        //*/
    }

    //*/ Modified by tyd hanhao for data switcher 2015-08-22
    private void handleDataSwitch() {
        if (mDataController.isMobileDataSupported()) {
            if(mState.enabled){
                mDataController.setMobileDataEnabled(!mState.connected);
            } else if(mState.defaultDataSimEmpty){
                int defaultSubId = SubscriptionManager.getDefaultSubId();
                SubscriptionManager sbMgr = SubscriptionManager.from(mContext);
                sbMgr.setDefaultDataSubId(defaultSubId);
                mDataController.setMobileDataEnabled(true);
            }
        }
    }
    //*/

    //*/ Added by tyd hanhao for data switcher 2015-08-22
    private void handleShowSIMSwitcherDialog() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.sim.SimDialogActivity"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("dialog_type", 4);
        mHost.startActivityDismissingKeyguard(intent);
    }
    //*/
    
    @Override
    protected void handleUpdateState(QSTile.SignalState state, Object arg) {
        if (DEBUG) {
            Log.d(TAG, "handleUpdateState arg=" + arg);
        }

        state.visible = mController.hasMobileDataFeature();
        if (!state.visible) {
            return;
        }

        CallbackInfo cb = (CallbackInfo) arg;

        if (cb == null) {
            cb = mCallback.mInfo;
        }

        final boolean enabled = mDataController.isMobileDataSupported()
                && !cb.noSim && !cb.airplaneModeEnabled && isDefaultDataSimRadioOn();
        final boolean dataConnected = enabled && mDataController.isMobileDataEnabled()
                && (cb.mobileSignalIconId > 0);
        final boolean dataNotConnected = (cb.mobileSignalIconId > 0) && (cb.enabledDesc == null);

        //*/ freeme, gouzhouping, 20161226, for cancel show SIMSwitcherDialog when airplane enable.
        isAirplaneEnabled = cb.airplaneModeEnabled;
        //*/
        
        //*/freeme.gejun, 20160705 DefaultDataSim Empty
        final boolean defaultDataSimEmpty = mDataController.isMobileDataSupported()
                && !cb.noSim && !cb.airplaneModeEnabled && SubscriptionManager.getDefaultDataSubId() < 0;
        state.defaultDataSimEmpty = defaultDataSimEmpty;
        //*/
        state.enabled = enabled;
        state.connected = dataConnected;
        state.activityIn = cb.enabled && cb.activityIn;
        state.activityOut = cb.enabled && cb.activityOut;
        state.filter = true;
        state.selected = dataConnected;

        if (!state.enabled) {
            mDataConnectionState = DATA_CONNECT_DISABLE;
            mDataStateIconId = QS_MOBILE_DISABLE;
        } else if (dataConnected) {
            mDataConnectionState = DATA_CONNECT;
            mDataStateIconId = QS_MOBILE_ENABLE;
        } else if (dataNotConnected) {
            mDataConnectionState = DATA_DISCONNECT;
            mDataStateIconId = QS_MOBILE_DISABLE;
        } else {
            mDataConnectionState = DATA_DISCONNECT;
            mDataStateIconId = QS_MOBILE_DISABLE;
        }
        
        //*/ Added by droi hanhao for customized QSTile 2015-06-24
        if(CUSTOMIZED_QSTILE) {
            if(mDataConnectionState == DATA_CONNECT) {
                mDataStateIconId = R.drawable.droi_ic_qs_mobile_on;
            } else {// state disable
                mDataStateIconId = R.drawable.droi_ic_qs_mobile_off;
            }
        }
        //*/
        
        if (PluginFactory.getStatusBarPlugin(mContext).customizeBehaviorSet()
                == BehaviorSet.OP09_BS) {
            state.label = PluginFactory.getQuickSettingsPlugin(mContext)
                    .customizeDataConnectionTile(mDataConnectionState, mDataStateIconIdWrapper,
                            mContext.getString(R.string.mobile));
            state.icon = mDataStateIcon;
        } else {
            state.label = mContext.getString(R.string.mobile);
            state.icon = ResourceIcon.get(mDataStateIconId);
        }

        if (DEBUG) {
            Log.d(TAG, "handleUpdateState state=" + state);
        }
    }

    private final boolean isDefaultDataSimRadioOn() {
        //*/ freeme.xupeng, 20160630. temp solution: remove these code.
        //*/ Added by droi hanhao for tyd00596853 2016-03-16
        // checkDefaultDataSubId();
        //*/
        //*/
        final int subId = SubscriptionManager.getDefaultDataSubId();
        final boolean isRadioOn = subId >= 0 && SIMHelper.isRadioOn(subId);
        if (DEBUG) {
            Log.d(TAG, "isDefaultDataSimRadioOn subId=" + subId + ", isRadioOn=" + isRadioOn);
        }
        return isRadioOn;
    }

    //*/ freeme.xupeng, 20160630. temp solution: remove these code.
    /*/ Added by droi hanhao for tyd00596853 2016-03-16
    private void checkDefaultDataSubId() {
        int defaultSubId = SubscriptionManager.getDefaultSubId();
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubId();
        if(defaultDataSubId < 0 && defaultSubId >= 0) {
            Log.d(TAG, "defaultSubId = " + defaultSubId + ", defaultDataSubId = " + defaultDataSubId);
            SubscriptionManager sbMgr = SubscriptionManager.from(mContext);
            sbMgr.setDefaultDataSubId(defaultSubId);
            Log.d(TAG, "fix default data subId = " + defaultSubId);
        }
    }
    //*/
    //*/

    /** Added by tyd hanhao, 2015-09-08
        * we only need to show switch dialog when mult SIM inserted.
        */
    private final boolean isMultSimInserted() {
        int slotCount = SIMHelper.getSlotCount();

        if(slotCount <= 1) {
            return false;
        }

        // We have two SIM cards at most.
        return SIMHelper.isSimInsertedBySlot(mContext, PhoneConstants.SIM_ID_1) && SIMHelper.isSimInsertedBySlot(mContext, PhoneConstants.SIM_ID_2);
    }

    /**
     * NetworkSignalChanged Callback Info.
     */
    private static final class CallbackInfo {
        public boolean enabled;
        public boolean wifiEnabled;
        public boolean wifiConnected;
        public boolean airplaneModeEnabled;
        public int mobileSignalIconId;
        public int dataTypeIconId;
        public boolean activityIn;
        public boolean activityOut;
        public String enabledDesc;
        public boolean noSim;

        @Override
        public String toString() {
            return new StringBuilder("CallbackInfo[")
                    .append("enabled=").append(enabled)
                    .append(",wifiEnabled=").append(wifiEnabled)
                    .append(",wifiConnected=").append(wifiConnected)
                    .append(",airplaneModeEnabled=").append(airplaneModeEnabled)
                    .append(",mobileSignalIconId=").append(mobileSignalIconId)
                    .append(",dataTypeIconId=").append(dataTypeIconId)
                    .append(",activityIn=").append(activityIn)
                    .append(",activityOut=").append(activityOut)
                    .append(",enabledDesc=").append(enabledDesc)
                    .append(",noSim=").append(noSim)
                    .append(']').toString();
        }
    }

    private final class MobileDataSignalCallback extends SignalCallbackAdapter {
        final CallbackInfo mInfo = new CallbackInfo();

        @Override
        public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon,
                boolean activityIn, boolean activityOut, String description) {
            mInfo.wifiEnabled = enabled;
            mInfo.wifiConnected = qsIcon.visible;
            refreshState(mInfo);
        }

        @Override
        public void setMobileDataIndicators(IconState statusIcon, IconState qsIcon, int statusType,
                int networkIcon, int qsType, boolean activityIn, boolean activityOut,
                /// M: Add for CT6M. add activity icon @{
                int dataActivity,
                int primarySimIcon,
                /// @}
                String typeContentDescription, String description, boolean isWide, int subId) {
            if (qsIcon == null) {
                // Not data sim, don't display.
                return;
            }
            mInfo.enabled = qsIcon.visible;
            mInfo.mobileSignalIconId = qsIcon.icon;
            mInfo.dataTypeIconId = qsType;
            mInfo.activityIn = activityIn;
            mInfo.activityOut = activityOut;
            mInfo.enabledDesc = description;
            if (DEBUG) {
                Log.d(TAG, "setMobileDataIndicators mInfo=" + mInfo);
            }
            refreshState(mInfo);
        }

        @Override
        public void setNoSims(boolean show) {
            mInfo.noSim = show;
            if (mInfo.noSim) {
                // Make sure signal gets cleared out when no sims.
                mInfo.mobileSignalIconId = 0;
                mInfo.dataTypeIconId = 0;
                mInfo.enabled = false;

                if (DEBUG) {
                    Log.d(TAG, "setNoSims noSim=" + show);
                }
            }
            refreshState(mInfo);
        }

        @Override
        public void setIsAirplaneMode(IconState icon) {
            mInfo.airplaneModeEnabled = icon.visible;
            if (mInfo.airplaneModeEnabled) {
                mInfo.mobileSignalIconId = 0;
                mInfo.dataTypeIconId = 0;
                mInfo.enabled = false;
            }
            refreshState(mInfo);
        }

        @Override
        public void setMobileDataEnabled(boolean enabled) {
            refreshState(mInfo);
        }
    };
}
