package com.freeme.settings.motion;

import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.R;

public class SmartWakeGestureSettings extends PreferenceActivity implements Preference.OnPreferenceClickListener {
    private static final String[] KEY_SETTING_MAP = {
            "screen_wakeup_up_enabled", "screen_wakeup_down_enabled",
            "screen_wakeup_left_enabled", "screen_wakeup_right_enabled",
            "screen_wakeup_c_enabled", "screen_wakeup_m_enabled",
            "screen_wakeup_e_enabled", "screen_wakeup_o_enabled",
            "screen_wakeup_w_enabled", "screen_wakeup_v_enabled",
            "screen_wakeup_s_enabled", "screen_wakeup_z_enabled",
            "screen_wakeup_rarrow_enabled", "screen_wakeup_tarrow_enabled"};
    private static final Preference[] KEY_PREFERENCE_MAP = new Preference[KEY_SETTING_MAP.length];

    public static final String[] KEY_ENABLED_SETTING_MAP = {
            Settings.System.FREEME_SCREEN_WAKEUP_UP_ENABLED, Settings.System.FREEME_SCREEN_WAKEUP_DWON_ENABLED,
            Settings.System.FREEME_SCREEN_WAKEUP_LEFT_ENABLED, Settings.System.FREEME_SCREEN_WAKEUP_RIGHT_ENABLED,
            Settings.System.FREEME_SCREEN_WAKEUP_C_ENABLED, Settings.System.FREEME_SCREEN_WAKEUP_M_ENABLED,
            Settings.System.FREEME_SCREEN_WAKEUP_E_ENABLED, Settings.System.FREEME_SCREEN_WAKEUP_O_ENABLED,
            Settings.System.FREEME_SCREEN_WAKEUP_W_ENABLED, Settings.System.FREEME_SCREEN_WAKEUP_V_ENABLED,
            Settings.System.FREEME_SCREEN_WAKEUP_S_ENABLED, Settings.System.FREEME_SCREEN_WAKEUP_Z_ENABLED,
            Settings.System.FREEME_SCREEN_WAKEUP_RARROW_ENABLED, Settings.System.FREEME_SCREEN_WAKEUP_TARROW_ENABLED};
    public static final String[] KEY_ACTION_SETTING_MAP = {
            Settings.System.FREEME_SCREEN_ACTION_UP_SETTING, Settings.System.FREEME_SCREEN_ACTION_DOWN_SETTING,
            Settings.System.FREEME_SCREEN_ACTION_LEFT_SETTING, Settings.System.FREEME_SCREEN_ACTION_RIGHT_SETTING,
            Settings.System.FREEME_SCREEN_ACTION_C_SETTING, Settings.System.FREEME_SCREEN_ACTION_M_SETTING,
            Settings.System.FREEME_SCREEN_ACTION_E_SETTING, Settings.System.FREEME_SCREEN_ACTION_O_SETTING,
            Settings.System.FREEME_SCREEN_ACTION_W_SETTING, Settings.System.FREEME_SCREEN_ACTION_V_SETTING,
            Settings.System.FREEME_SCREEN_ACTION_S_SETTING, Settings.System.FREEME_SCREEN_ACTION_Z_SETTING,
            Settings.System.FREEME_SCREEN_ACTION_RARROW_SETTING, Settings.System.FREEME_SCREEN_ACTION_TARROW_SETTING};
    public static final int TYPE_STARTUP_CALL = 0;
    public static final int TYPE_STARTUP_MMS = 1;
    public static final int TYPE_STARTUP_APP = 2;
    public static final int TYPE_MEIDA_CONTROL = 3;
    public static final int TYPE_UNLOCK = 4;

    private Context mContext = null;
    private ContentResolver mContentResolver = null;
    private boolean mSmartWakeEnable = false;

    private Switch mSwitch;
    private TextView mTitle;
    private LinearLayout mSwitchPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.smart_wake_gesture_settings);
        setContentView(R.layout.smart_wake_gesture_settings);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);

        mContext = this;
        mContentResolver = mContext.getContentResolver();

        mSmartWakeEnable = Settings.System.getInt(mContentResolver, Settings.System.FREEME_SCREEN_GESTURE_WAKEUP_ENABLED, 0) == 1;

        for (int i = 0; i <  KEY_SETTING_MAP.length; i++) {
            KEY_PREFERENCE_MAP[i] =  findPreference(KEY_SETTING_MAP[i]);
            KEY_PREFERENCE_MAP[i].setOnPreferenceClickListener(this);
        }

        mSwitch = (Switch) findViewById(R.id.my_switch);
        mSwitch.setChecked(mSmartWakeEnable);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                mTitle.setText(mSwitch.isChecked() ? mSwitch.getTextOn() : mSwitch.getTextOff());
                mSmartWakeEnable = arg1;
                Settings.System.putInt(mContentResolver, Settings.System.FREEME_SCREEN_GESTURE_WAKEUP_ENABLED, mSmartWakeEnable ? 1 : 0);
                setAllPreferenceEnable(mSmartWakeEnable);
            }
        });

        mTitle = (TextView) findViewById(R.id.title);
        mTitle.setText(mSmartWakeEnable ? mSwitch.getTextOn() : mSwitch.getTextOff());

        mSwitchPreference = (LinearLayout) findViewById(R.id.layout_switch_preference);
        mSwitchPreference.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwitch.setChecked(!mSwitch.isChecked());
            }
        });
        setAllPreferenceEnable(mSmartWakeEnable);
    }

    @Override
    public void onResume() {
        super.onResume();

        for (int i = 0; i < KEY_PREFERENCE_MAP.length; i++) {
            String summary = "";
            String actionAll = android.provider.Settings.System.getString(mContentResolver, KEY_ACTION_SETTING_MAP[i]);
            if (actionAll != null) {
                String[] actionItem = actionAll.split(";");
                if (actionItem[0].equals("startupcall")) {
                    summary =  getResources().getString(R.string.take_phone_mode_title) + "  " + actionItem[1];
                } else if (actionItem[0].equals("startupmms")) {
                    summary = getResources().getString(R.string.send_sms_mode_title);
                } else if (actionItem[0].equals("startupapp")) {
                    summary = getResources().getString(R.string.open_app_mode_title) + "  " + filterDefaultName(actionItem[1]);
                } else if (actionItem[0].equals("mediacontrol")) {
                    summary = getResources().getString(R.string.control_music_mode_title) + "  " + filterDefaultName(actionItem[1]);
                } else if (actionItem[0].equals("unlock")) {
                    summary = getResources().getString(R.string.unlock_screen_mode_title);
                }
            }

            KEY_PREFERENCE_MAP[i].setSummary(summary);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Intent intent = new Intent(mContext, SmartWakeSelectSettings.class);
        for (int i = 0; i < KEY_PREFERENCE_MAP.length; i++) {
            if (preference == KEY_PREFERENCE_MAP[i]) {
                intent.putExtra("type", i);
            }
        }
        startActivityForResult(intent, 0);
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            KEY_PREFERENCE_MAP[resultCode].setSummary(data.getExtras().getString("ControlData"));
            android.provider.Settings.System.putString(mContentResolver, KEY_ACTION_SETTING_MAP[resultCode],
                    data.getExtras().getString("ActionData"));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setAllPreferenceEnable(boolean enable) {
        for (int i = 0; i < KEY_PREFERENCE_MAP.length; i++) {
            KEY_PREFERENCE_MAP[i].setEnabled(enable);
        }
    }

    private String filterDefaultName(String name) {
        if (name.equals("appweixin")) {
            return getResources().getString(R.string.default_setting_weixin);
        } else if (name.equals("appqq")) {
            return getResources().getString(R.string.default_setting_qq);
        } else if (name.equals("appbrowser")) {
            return getResources().getString(R.string.default_setting_browser);
        } else if (name.equals("appcall")) {
            return getResources().getString(R.string.default_setting_call);
        } else if (name.equals("appmusic")) {
            return getResources().getString(R.string.default_setting_music);
        } else if (name.equals("musicstartpause")) {
            return getResources().getString(R.string.start_or_pause_music);
        } else if (name.equals("musicprev")) {
            return getResources().getString(R.string.prev_music);
        } else if (name.equals("musicnext")) {
            return getResources().getString(R.string.next_music);
        } else {
            return name;
        }
    }
}
