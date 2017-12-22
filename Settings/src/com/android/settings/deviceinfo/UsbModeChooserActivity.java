/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.annotation.Nullable;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.CheckBox;
import android.widget.Switch;

/**
 * UI for the USB chooser dialog.
 *
 */
public class UsbModeChooserActivity extends Activity {

    public static final int[] DEFAULT_MODES = {
        /*/ freeme.biantao, 20160613. Adb chooser. [Exchange]
        UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_NONE,
        UsbBackend.MODE_POWER_SOURCE | UsbBackend.MODE_DATA_NONE,
        //*/
        UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MTP,
        UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_PTP,
        UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MIDI,
        //*/ freeme.biantao, 20160613. Adb chooser. [Exchange]
        UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_NONE,
        UsbBackend.MODE_POWER_SOURCE | UsbBackend.MODE_DATA_NONE,
        //*/
        /// M: Add for Built-in CD-ROM and USB Mass Storage @{
        UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MASS_STORAGE,
        UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_BICR
        /// M: @}
    };

    private UsbBackend mBackend;
    private AlertDialog mDialog;
    private LayoutInflater mLayoutInflater;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mLayoutInflater = LayoutInflater.from(this);

        mDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.usb_use)
                .setView(R.layout.usb_dialog_container)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                })
                /*/ freeme.biantao, 20160612. Adb chooser.
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                //*/
                .create();
        mDialog.show();

        LinearLayout container = (LinearLayout) mDialog.findViewById(R.id.container);

        mBackend = new UsbBackend(this);
        int current = mBackend.getCurrentMode();
        for (int i = 0; i < DEFAULT_MODES.length; i++) {
            if (mBackend.isModeSupported(DEFAULT_MODES[i])) {
                inflateOption(DEFAULT_MODES[i], current == DEFAULT_MODES[i], container);
            }
        }
        //*/ freeme.biantao, 20160612. Adb chooser.
        inflateAdbSwitch(container);
        inflateAskAgain(container);
        // ---
        mUsbBroadcastReceiver = new UsbBroadcastReceiver(this);
        mUsbBroadcastReceiver.register();
        //*/
    }

    private void inflateOption(final int mode, boolean selected, LinearLayout container) {
        /*/ freeme.biantao, 20160612. Adb chooser.
        View v = mLayoutInflater.inflate(R.layout.radio_with_summary, container, false);
        /*/
        View v = mLayoutInflater.inflate(R.layout.freeme_radio_with_summary, container, false);
        //*/

        ((TextView) v.findViewById(android.R.id.title)).setText(getTitle(mode));
        ((TextView) v.findViewById(android.R.id.summary)).setText(getSummary(mode));

        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ActivityManager.isUserAMonkey()) {
                    mBackend.setMode(mode);
                }
                mDialog.dismiss();
                finish();
            }
        });
        ((Checkable) v).setChecked(selected);
        container.addView(v);
    }

    private static int getSummary(int mode) {
        switch (mode) {
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_NONE:
                return R.string.usb_use_charging_only_desc;
            case UsbBackend.MODE_POWER_SOURCE | UsbBackend.MODE_DATA_NONE:
                return R.string.usb_use_power_only_desc;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MTP:
                return R.string.usb_use_file_transfers_desc;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_PTP:
                return R.string.usb_use_photo_transfers_desc;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MIDI:
                return R.string.usb_use_MIDI_desc;
            /// M: Add for Built-in CD-ROM and USB Mass Storage @{
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MASS_STORAGE:
                return R.string.usb_ums_summary;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_BICR:
                return R.string.usb_bicr_summary;
            /// M: @}
        }
        return 0;
    }

    private static int getTitle(int mode) {
        switch (mode) {
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_NONE:
                return R.string.usb_use_charging_only;
            case UsbBackend.MODE_POWER_SOURCE | UsbBackend.MODE_DATA_NONE:
                return R.string.usb_use_power_only;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MTP:
                return R.string.usb_use_file_transfers;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_PTP:
                return R.string.usb_use_photo_transfers;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MIDI:
                return R.string.usb_use_MIDI;
            /// M: Add for Built-in CD-ROM and USB Mass Storage @{
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MASS_STORAGE:
                return R.string.usb_use_mass_storage;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_BICR:
                return R.string.usb_use_built_in_cd_rom;
            /// M: @}
        }
        return 0;
    }

    //*/ freeme.biantao, 20160612. Adb chooser.
    private AlertDialog mAdbDialog;
    private AdbInteraction mAdbInteraction;
    private boolean mAdbEnabled;

    private void inflateAdbSwitch(LinearLayout container){
        View v = mLayoutInflater.inflate(R.layout.freeme_adb_enable, container, false);

        ((TextView) v.findViewById(android.R.id.title)).setText(R.string.enable_adb);
        ((TextView) v.findViewById(android.R.id.summary)).setText(R.string.enable_adb_summary);

        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Checkable) v).toggle();
            }
        });

        ContentResolver r = getContentResolver();
        mAdbEnabled = Settings.Global.getInt(r, Settings.Global.ADB_ENABLED, 0) == 1;
        mAdbInteraction = new AdbInteraction(v);
        mAdbInteraction.setChecked(mAdbEnabled, false);

        container.addView(v);
    }

    private class AdbInteraction implements CompoundButton.OnCheckedChangeListener,
            DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

        private Checkable mParent;
        private Switch mSwitch;

        public AdbInteraction(View parent) {
            mParent = (Checkable) parent;
            mSwitch = (Switch) parent.findViewById(R.id.adb_enable);
            mSwitch.setOnCheckedChangeListener(this);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                if (mAdbDialog != null && mAdbDialog.isShowing()) {
                    mAdbDialog.dismiss();
                    mAdbDialog = null;
                }
                setChecked(true, false); // fake, just for sync state
                mAdbDialog = new AlertDialog.Builder(UsbModeChooserActivity.this)
                        .setMessage(R.string.adb_warning_message)
                        .setTitle(R.string.adb_warning_title)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNegativeButton(android.R.string.no, this)
                        .setOnDismissListener(this)
                        .show();
            } else {
                mAdbEnabled = false;
                setChecked(mAdbEnabled, false);
                Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_ENABLED, 0);
            }
        }

        public void setChecked(boolean checked, boolean fromUser) {
            if (mParent.isChecked() != checked) {
                if (fromUser) {
                    mParent.setChecked(checked);
                } else {
                    mSwitch.setOnCheckedChangeListener(null);
                    mParent.setChecked(checked);
                    mSwitch.setOnCheckedChangeListener(this);
                }
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            setChecked(mAdbEnabled, false);
            mAdbDialog = null;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE: {
                    ContentResolver r = getContentResolver();
                    Settings.Global.putInt(r, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
                    Settings.Global.putInt(r, Settings.Global.ADB_ENABLED, 1);
                    mAdbEnabled = true;
                    break;
                }
                default: {
                    mAdbEnabled = false;
                    break;
                }
            }
            setChecked(mAdbEnabled, false);
            mAdbDialog = null;
        }
    }

    // ---
    private static final String KEY_USB_ASK_AGAIN = "persist.sys.usb.ask_again";

    private void inflateAskAgain(LinearLayout container) {
        View v = mLayoutInflater.inflate(R.layout.freeme_adb_askagain, container, false);

        CheckBox checkBox = (CheckBox) v.findViewById(R.id.adb_not_askagain);
        checkBox.setChecked(!"yes".equals(SystemProperties.get(KEY_USB_ASK_AGAIN, "yes")));
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SystemProperties.set(KEY_USB_ASK_AGAIN, isChecked ? "no" : "yes");
            }
        });

        container.addView(v);
    }

    // ---
    // The non-persistent property which stores the current USB settings.
    private UsbBroadcastReceiver mUsbBroadcastReceiver;

    private class UsbBroadcastReceiver extends BroadcastReceiver {
        private Context mContext;
        private boolean mHasRegisted;

        public UsbBroadcastReceiver(Context context) {
            mContext = context;
        }

        @Override
        public void onReceive(Context content, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case UsbManager.ACTION_USB_STATE: {
                    boolean isUsbHwDisconnected = intent.getBooleanExtra("USB_HW_DISCONNECTED", false);
                    if (isUsbHwDisconnected) {
                        finish();
                    }
                    break;
                }
                case Intent.ACTION_BATTERY_CHANGED: {
                    int pluggedType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                    if (pluggedType != BatteryManager.BATTERY_PLUGGED_USB) {
                        finish();
                    }
                    break;
                }
            }
        }

        public void onFilter(IntentFilter filter) {
            filter.addAction(UsbManager.ACTION_USB_STATE);
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        }

        public final void register() {
            if (!mHasRegisted) {
                IntentFilter filter = new IntentFilter();
                onFilter(filter);
                mContext.registerReceiver(this, filter);

                mHasRegisted = true;
            }
        }

        public final void unregister() {
            if (mHasRegisted) {
                mContext.unregisterReceiver(this);

                mHasRegisted = false;
            }
        }
    };

    @Override
    protected void onDestroy() {
        mUsbBroadcastReceiver.unregister();
        super.onDestroy();
    }
    //*/
}
