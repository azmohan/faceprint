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
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
//*/ freeme,gouzhouping. 20160624, for remove floattask
import android.os.SystemProperties;
//*/

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.tiles.AirplaneModeTile;
import com.android.systemui.qs.tiles.BluetoothTile;
import com.android.systemui.qs.tiles.CastTile;
import com.android.systemui.qs.tiles.CellularTile;
import com.android.systemui.qs.tiles.ColorInversionTile;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.qs.tiles.FlashlightTile;
import com.android.systemui.qs.tiles.HotspotTile;
import com.android.systemui.qs.tiles.IntentTile;
import com.android.systemui.qs.tiles.LocationTile;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.qs.tiles.WifiTile;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import com.droi.systemui.qs.tile.DroiTileAudioProfile;
import com.droi.systemui.qs.tile.DroiTileFloatTask;
import com.droi.systemui.qs.tile.DroiTileLockScreen;
import com.droi.systemui.qs.tile.DroiTileMobileData;
import com.droi.systemui.qs.tile.DroiTileMore;
import com.droi.systemui.qs.tile.DroiTileOptimization;
import com.droi.systemui.qs.tile.DroiTilePowerSaving;
import com.droi.systemui.qs.tile.DroiTileReboot;
import com.droi.systemui.qs.tile.DroiTileScreenRecorder;
import com.droi.systemui.qs.tile.DroiTileShutdown;
import com.droi.systemui.qs.tile.DroiTileSleep;
import com.droi.systemui.qs.tile.DroiTileSuperShot;

/// M: add plugin in quicksetting @{
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
/// add plugin in quicksetting @}

/// M: Add extra tiles in quicksetting @{
import com.mediatek.systemui.qs.tiles.AudioProfileTile;
import com.mediatek.systemui.qs.tiles.HotKnotTile;
import com.mediatek.systemui.qs.tiles.ext.ApnSettingsTile;
import com.mediatek.systemui.qs.tiles.ext.DualSimSettingsTile;
import com.mediatek.systemui.qs.tiles.ext.MobileDataTile;
import com.mediatek.systemui.qs.tiles.ext.SimDataConnectionTile;
import com.mediatek.systemui.statusbar.extcb.PluginFactory;
import com.mediatek.systemui.statusbar.policy.AudioProfileController;
import com.mediatek.systemui.statusbar.policy.HotKnotController;
import com.mediatek.systemui.statusbar.util.SIMHelper;
// /@}

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Platform implementation of the quick settings tile host **/
public class QSTileHost implements QSTile.Host, Tunable {
    private static final String TAG = "QSTileHost";
    private static final boolean DEBUG = true;//Log.isLoggable(TAG, Log.DEBUG);

    protected static final String TILES_SETTING = "sysui_qs_tiles";
    //*/ freeme,gouzhouping. 20160624, for remove floattask
    public static final boolean FREEME_FLOATTASK_SUPPORT = SystemProperties.get("ro.freeme_floattask").equals("1"); 
    //*/
    
    //*/ Added by tyd hanhao for Customized QSTile 2015-06-25
    private final boolean mCustomized;
    SharedPreferences mPrefs;
    SharedPreferences.Editor mEditor;
    //*/

    private final Context mContext;
    private final PhoneStatusBar mStatusBar;
    private final LinkedHashMap<String, QSTile<?>> mTiles = new LinkedHashMap<>();
    protected final ArrayList<String> mTileSpecs = new ArrayList<>();
    private final BluetoothController mBluetooth;
    private final LocationController mLocation;
    private final RotationLockController mRotation;
    private final NetworkController mNetwork;
    private final ZenModeController mZen;
    private final HotspotController mHotspot;
    private final CastController mCast;
    private final Looper mLooper;
    private final FlashlightController mFlashlight;
    private final UserSwitcherController mUserSwitcherController;
    private final KeyguardMonitor mKeyguard;
    private final SecurityController mSecurity;
    /// M: Add extra tiles in quicksetting @{
    // add HotKnot in quicksetting
    private final HotKnotController mHotKnot;
    // add AudioProfile in quicksetting
    private final AudioProfileController mAudioProfile;
    // /@}
    private Callback mCallback;

    public QSTileHost(Context context, PhoneStatusBar statusBar,
            BluetoothController bluetooth, LocationController location,
            RotationLockController rotation, NetworkController network,
            ZenModeController zen, HotspotController hotspot,
            CastController cast, FlashlightController flashlight,
            UserSwitcherController userSwitcher, KeyguardMonitor keyguard,
            SecurityController security,
            /// M: Add extra tiles in quicksetting @{
            // add HotKnot in quicksetting
            HotKnotController hotknot,
            // add AudioProfile in quicksetting
            AudioProfileController audioprofile) {
            // /@}
        mContext = context;
        mStatusBar = statusBar;
        mBluetooth = bluetooth;
        mLocation = location;
        mRotation = rotation;
        mNetwork = network;
        mZen = zen;
        mHotspot = hotspot;
        mCast = cast;
        mFlashlight = flashlight;
        mUserSwitcherController = userSwitcher;
        mKeyguard = keyguard;
        mSecurity = security;
        /// M: Add extra tiles in quicksetting @{
        // add HotKnot in quicksetting
        mHotKnot = hotknot;
        // add AudioProfile in quicksetting
        mAudioProfile = audioprofile;
        // /@}
        final HandlerThread ht = new HandlerThread(QSTileHost.class.getSimpleName(),
                Process.THREAD_PRIORITY_BACKGROUND);
        ht.start();
        mLooper = ht.getLooper();

        //*/ Added by tyd hanhao for Customized QSTile 2015-06-25
        mCustomized = mContext.getResources().getBoolean(R.bool.config_customized_qs_tile);
        mPrefs = context.getSharedPreferences("QuickSettingsOrderedTile", Context.MODE_PRIVATE);
        mEditor = mPrefs.edit();
        mGuidanceInitialized = isGuidanceInitialized();
        //*/
        TunerService.get(mContext).addTunable(this, TILES_SETTING);
    }

    public void destroy() {
        TunerService.get(mContext).removeTunable(this);
    }

    @Override
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public Collection<QSTile<?>> getTiles() {
        return mTiles.values();
    }

    @Override
    public void startActivityDismissingKeyguard(final Intent intent) {
        mStatusBar.postStartActivityDismissingKeyguard(intent, 0);
    }

    @Override
    public void startActivityDismissingKeyguard(PendingIntent intent) {
        mStatusBar.postStartActivityDismissingKeyguard(intent);
    }

    @Override
    public void warn(String message, Throwable t) {
        // already logged
    }

    @Override
    public void collapsePanels() {
        mStatusBar.postAnimateCollapsePanels();
    }

    @Override
    public Looper getLooper() {
        return mLooper;
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    @Override
    public BluetoothController getBluetoothController() {
        return mBluetooth;
    }

    @Override
    public LocationController getLocationController() {
        return mLocation;
    }

    @Override
    public RotationLockController getRotationLockController() {
        return mRotation;
    }

    @Override
    public NetworkController getNetworkController() {
        return mNetwork;
    }

    @Override
    public ZenModeController getZenModeController() {
        return mZen;
    }

    @Override
    public HotspotController getHotspotController() {
        return mHotspot;
    }

    @Override
    public CastController getCastController() {
        return mCast;
    }

    @Override
    public FlashlightController getFlashlightController() {
        return mFlashlight;
    }

    @Override
    public KeyguardMonitor getKeyguardMonitor() {
        return mKeyguard;
    }

    public UserSwitcherController getUserSwitcherController() {
        return mUserSwitcherController;
    }

    public SecurityController getSecurityController() {
        return mSecurity;
    }

    /// M: Add extra tiles in quicksetting @{
    // add HotKnot in quicksetting
    @Override
    public HotKnotController getHotKnotController() {
        return mHotKnot;
    }

    // add AudioProfile in quicksetting
    @Override
    public AudioProfileController getAudioProfileController() {
        return mAudioProfile;
    }
    // /@}
    //*/ Added by droi hanhao for customized 2016-01-13
    private static final String GUIDER_STATE = "GuiderState";
    private boolean mGuidanceInitialized = false;
    @Override
    public boolean haveGuidance(String tileSpec) {
        SharedPreferences prefs = mContext.getSharedPreferences(GUIDER_STATE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        return prefs.getBoolean(tileSpec, false);
    }

    @Override
    public void setGuidance(String tileSpec, boolean guided) {
        SharedPreferences prefs = mContext.getSharedPreferences(GUIDER_STATE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(tileSpec, guided);
        editor.commit();
    }

    private boolean isGuidanceInitialized() {
        SharedPreferences prefs = mContext.getSharedPreferences(GUIDER_STATE, Context.MODE_PRIVATE);
        return prefs.getBoolean("Initialized", false);
    }
    private void setGuidanceInitialized() {
        SharedPreferences prefs = mContext.getSharedPreferences(GUIDER_STATE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("Initialized", true);
        editor.commit();
        mGuidanceInitialized = true;
    }
    //*/

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (!TILES_SETTING.equals(key)) {
            return;
        }
        if (DEBUG) Log.d(TAG, "Recreating tiles");
        final List<String> tileSpecs = loadTileSpecs(newValue);
        if (tileSpecs.equals(mTileSpecs)) return;
        for (Map.Entry<String, QSTile<?>> tile : mTiles.entrySet()) {
            if (!tileSpecs.contains(tile.getKey())) {
                if (DEBUG) Log.d(TAG, "Destroying tile: " + tile.getKey());
                tile.getValue().destroy();
            }
        }
        final LinkedHashMap<String, QSTile<?>> newTiles = new LinkedHashMap<>();
        for (String tileSpec : tileSpecs) {
            if (mTiles.containsKey(tileSpec)) {
                newTiles.put(tileSpec, mTiles.get(tileSpec));
            } else {
                if (DEBUG) Log.d(TAG, "Creating tile: " + tileSpec);
                try {
                    newTiles.put(tileSpec, createTile(tileSpec));
                } catch (Throwable t) {
                    Log.w(TAG, "Error creating tile for spec: " + tileSpec, t);
                }
            }
        }
        mTileSpecs.clear();
        mTileSpecs.addAll(tileSpecs);

        //*/ Added by droi hanhao for customized 2016-01-13
        if(!mGuidanceInitialized) {
            if(DEBUG) Log.d(TAG, "Have initialized guidance list");
            setGuidanceInitialized(); 
        }
        //*/
        
        mTiles.clear();
        mTiles.putAll(newTiles);
        if (mCallback != null) {
            mCallback.onTilesChanged();
        }
    }

    protected QSTile<?> createTile(String tileSpec) {
        IQuickSettingsPlugin quickSettingsPlugin = PluginFactory
                .getQuickSettingsPlugin(mContext);

        //*/ Added by droi hanhao for customized 2016-01-13
        if(null != tileSpec && !mGuidanceInitialized) {
            setGuidance(tileSpec, false);
        }
        //*/

        if (tileSpec.equals("wifi")) return new WifiTile(this);
        else if (tileSpec.equals("bt")) return new BluetoothTile(this);
        else if (tileSpec.equals("inversion")) return new ColorInversionTile(this);
        else if (tileSpec.equals("cell")) return new CellularTile(this);
        else if (tileSpec.equals("airplane")) return new AirplaneModeTile(this);
        else if (tileSpec.equals("dnd")) return new DndTile(this);
        else if (tileSpec.equals("rotation")) return new RotationLockTile(this);
        else if (tileSpec.equals("flashlight")) return new FlashlightTile(this);
        else if (tileSpec.equals("location")) return new LocationTile(this);
        else if (tileSpec.equals("cast")) return new CastTile(this);
        else if (tileSpec.equals("hotspot")) return new HotspotTile(this);
        /// M: Add extra tiles in quicksetting @{
        else if (tileSpec.equals("hotknot") && SIMHelper.isMtkHotKnotSupport())
            return new HotKnotTile(this);
        else if (tileSpec.equals("audioprofile") && SIMHelper.isMtkAudioProfilesSupport()) {
            //*/ Modified by droi hanhao for customized 2016-01-13
            return mCustomized ? new DroiTileAudioProfile(this) : new AudioProfileTile(this);
            //*/
        }
        // /@}
        /// M: add DataConnection in quicksetting @{
        else if (tileSpec.equals("dataconnection") && !SIMHelper.isWifiOnlyDevice()) {
            //*/ Modified by droi hanhao for customized 2016-01-13
            return mCustomized ? new DroiTileMobileData(this) : new MobileDataTile(this);
            //*/ Modified by droi hanhao for customized 2016-01-13
        }
        /// M: add DataConnection in quicksetting @}
        /// M: Customize the quick settings tiles for operator. @{
        else if (tileSpec.equals("simdataconnection") && !SIMHelper.isWifiOnlyDevice() &&
                quickSettingsPlugin.customizeAddQSTile(new SimDataConnectionTile(this)) != null) {
            return (SimDataConnectionTile) quickSettingsPlugin.customizeAddQSTile(
                    new SimDataConnectionTile(this));
        } else if (tileSpec.equals("dulsimsettings") && !SIMHelper.isWifiOnlyDevice() &&
                quickSettingsPlugin.customizeAddQSTile(new DualSimSettingsTile(this)) != null) {
            return (DualSimSettingsTile) quickSettingsPlugin.customizeAddQSTile(
                    new DualSimSettingsTile(this));
        } else if (tileSpec.equals("apnsettings") && !SIMHelper.isWifiOnlyDevice() &&
                quickSettingsPlugin.customizeAddQSTile(new ApnSettingsTile(this)) != null) {
            return (ApnSettingsTile) quickSettingsPlugin.customizeAddQSTile(
                    new ApnSettingsTile(this));
        }
        /// @}
        else if (tileSpec.startsWith(IntentTile.PREFIX)) return IntentTile.create(this,tileSpec);
        //*/ Added by droi hanhao for customized 2016-01-13
        else if (tileSpec.equals("screenrecorder") && mCustomized) {
            return new DroiTileScreenRecorder(this);
        } else if (tileSpec.equals("oneclear") && mCustomized) {
            return new DroiTileOptimization(this);
        } else if (tileSpec.equals("supershot") && mCustomized) {
            return new DroiTileSuperShot(this);
        } else if (tileSpec.equals("powersaving") && mCustomized) {
            return new DroiTilePowerSaving(this);
        } else if (tileSpec.equals("lockscreen") && mCustomized) {
            return new DroiTileLockScreen(this);
        } else if (tileSpec.equals("floattask") && mCustomized) {
            return new DroiTileFloatTask(this);
        } else if (tileSpec.equals("sleep") && mCustomized) {
            return new DroiTileSleep(this);
        } else if (tileSpec.equals("reboot") && mCustomized) {
            return new DroiTileReboot(this);
        } else if (tileSpec.equals("shutdown") && mCustomized) {
            return new DroiTileShutdown(this);
        } else if (tileSpec.equals("more") && mCustomized) {
            return new DroiTileMore(this);
        }
        //*/
        else throw new IllegalArgumentException("Bad tile spec: " + tileSpec);
    }

    protected List<String> loadTileSpecs(String tileList) {
        final Resources res = mContext.getResources();
        String defaultTileList = res.getString(R.string.quick_settings_tiles_default);

        // M: Add extra tiles @{
        defaultTileList += "," + res.getString(R.string.quick_settings_tiles_extra);
        // @}
        /// M: Customize the quick settings tile order for operator. @{
        IQuickSettingsPlugin quickSettingsPlugin = PluginFactory.getQuickSettingsPlugin(mContext);
        defaultTileList = quickSettingsPlugin.customizeQuickSettingsTileOrder(defaultTileList);
        /// M: Customize the quick settings tile order for operator. @}
        
        //*/ Added by droi hanhao for customized 2016-01-13
        if(mCustomized) {
            defaultTileList = res.getString(R.string.droi_quick_settings_tiles_default);
        }
        //*/
        Log.d(TAG, "loadTileSpecs() default tile list: " + defaultTileList);
        
        if (tileList == null) {
            tileList = res.getString(R.string.quick_settings_tiles);
            if (DEBUG) Log.d(TAG, "Loaded tile specs from config: " + tileList);
        } else {
            if (DEBUG) Log.d(TAG, "Loaded tile specs from setting: " + tileList);
        }
        final ArrayList<String> tiles = new ArrayList<String>();
        boolean addedDefault = false;
        for (String tile : tileList.split(",")) {
            tile = tile.trim();
            if (tile.isEmpty()) continue;
            if (tile.equals("default")) {
                if (!addedDefault) {
                    tiles.addAll(Arrays.asList(defaultTileList.split(",")));
                    addedDefault = true;
                }
            } else {
                tiles.add(tile);
            }
        }
        //*/ freeme,gouzhouping. 20160624, for remove floattask
        if (!FREEME_FLOATTASK_SUPPORT) {
            tiles.remove("floattask");
        }
        //*/
        return tiles;
    }
}
