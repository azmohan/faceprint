package com.freeme.settings.motion;

import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.view.MenuItem;
import com.android.settings.R;

public class SmartWakeSelectSettings extends PreferenceActivity implements OnPreferenceClickListener {
    private static final String KEY_SELECT_PHONE_WAKE_SETTING = "select_phone_wake_setting";
    private static final String KEY_SELECT_SMS_WAKE_SETTING = "select_sms_wake_setting";
    private static final String KEY_SELECT_APP_WAKE_SETTING = "select_app_wake_setting";
    private static final String KEY_SELECT_MUSICCONTROL_WAKE_SETTING = "select_musiccontrol_wake_setting";
    private static final String KEY_SELECT_UNLUCKSCREEN_WAKE_SETTING = "select_unlockscreen_wake_setting";

    private Preference mPhoneWakeSetting;
    private Preference mSmsWakeSetting;
    private Preference mAppWakeSetting;
    private Preference mMusicWakeSetting;
    private Preference mUnlockWakeSetting;

    private static final int[] KEY_STRING_ID_MAP = {
            R.string.screen_wakeup_up_enabled, R.string.screen_wakeup_down_enabled,
            R.string.screen_wakeup_left_enabled, R.string.screen_wakeup_right_enabled,
            R.string.screen_wakeup_c_enabled, R.string.screen_wakeup_m_enabled,
            R.string.screen_wakeup_e_enabled, R.string.screen_wakeup_o_enabled,
            R.string.screen_wakeup_w_enabled, R.string.screen_wakeup_v_enabled,
            R.string.screen_wakeup_s_enabled, R.string.screen_wakeup_z_enabled,
            R.string.screen_wakeup_rarrow_enabled, R.string.screen_wakeup_tarrow_enabled};

    private Context mContext = null;
    private ContentResolver mContentResolver = null;
    private boolean mSmartWakeEnable = false;

    private int mType = -1;
    private String mSmartWakeEnableName = null;

    private SwitchPreference mOnOff;
    private PreferenceScreen mParentPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.smart_wake_select_settings);

        mType = getIntent().getExtras().getInt("type", -1);
        if (mType == -1) {
            finish();
            return;
        }
        mSmartWakeEnableName = getSmartWakeEnableName(mType);
        if (mSmartWakeEnableName == null) {
            finish();
            return;
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);

        int titleResId = getSmartWakePreferenceTitle(mType);
        if (titleResId != -1) {
            setTitle(titleResId);
        }

        mContext = this;
        mContentResolver = mContext.getContentResolver();

        mPhoneWakeSetting = findPreference(KEY_SELECT_PHONE_WAKE_SETTING);
        mSmsWakeSetting = findPreference(KEY_SELECT_SMS_WAKE_SETTING);
        mAppWakeSetting = findPreference(KEY_SELECT_APP_WAKE_SETTING);
        mMusicWakeSetting = findPreference(KEY_SELECT_MUSICCONTROL_WAKE_SETTING);
        mUnlockWakeSetting = findPreference(KEY_SELECT_UNLUCKSCREEN_WAKE_SETTING);
        mPhoneWakeSetting.setOnPreferenceClickListener(this);
        mSmsWakeSetting.setOnPreferenceClickListener(this);
        mAppWakeSetting.setOnPreferenceClickListener(this);
        mMusicWakeSetting.setOnPreferenceClickListener(this);
        mUnlockWakeSetting.setOnPreferenceClickListener(this);

        mSmartWakeEnable = Settings.System.getInt(mContentResolver, mSmartWakeEnableName, 0) == 1;

        mOnOff = new SwitchPreference(mContext);
        mOnOff.setLayoutResource(R.layout.freeme_switch_preference);
        mOnOff.setOnPreferenceClickListener(this);
        mOnOff.setOrder(0);
        mOnOff.setSwitchTextOn(R.string.motion_on);
        mOnOff.setSwitchTextOff(R.string.motion_off);
        mOnOff.setChecked(mSmartWakeEnable);
        mOnOff.setTitle(mSmartWakeEnable ? R.string.motion_on : R.string.motion_off);

        mParentPreference = (PreferenceScreen) findPreference("smart_wakeup_settings_parent");
        mParentPreference.addPreference(mOnOff);

        if (!"1".equals(android.os.SystemProperties.get("ro.freeme.music_support")))
        mParentPreference.removePreference(mMusicWakeSetting);

        setAllCustomPreferenceEnable(mSmartWakeEnable);
    }

    @Override
    public void onResume() {
        super.onResume();

        String name = getSmartWakeActionName(mType);
        if (android.provider.Settings.System.getString(mContentResolver, name) == null) {
            mPhoneWakeSetting.setSummary("");
            mSmsWakeSetting.setSummary("");
            mAppWakeSetting.setSummary("");
            mMusicWakeSetting.setSummary("");
            mUnlockWakeSetting.setSummary("");
        } else {
            String actionAll = android.provider.Settings.System.getString(mContentResolver, name);
            String[] actionItem = actionAll.split(";");
            if (actionItem[0].equals("startupcall")) {
                mPhoneWakeSetting.setSummary(actionItem[1]);
            } else if (actionItem[0].equals("startupmms")) {
                mSmsWakeSetting.setSummary(getResources().getString(R.string.smart_wake_action_state_on));
            } else if (actionItem[0].equals("stratupapp")) {
                mAppWakeSetting.setSummary(filterDefaultName(actionItem[1]));
            } else if (actionItem[0].equals("mediacontrol")) {
                mMusicWakeSetting.setSummary(filterDefaultName(actionItem[1]));
            } else if (actionItem[0].equals("unlock")) {
                mUnlockWakeSetting.setSummary(getResources().getString(R.string.smart_wake_action_state_on));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            if (resultCode == SmartWakeGestureSettings.TYPE_MEIDA_CONTROL || resultCode == SmartWakeGestureSettings.TYPE_STARTUP_APP) {
                setResult(getIntent().getExtras().getInt("type"), data);
                finish();
            } else {
                if (backContact(data) == null) {
                    return;
                } else {
                    Intent mdata = new Intent();
                    mdata.putExtra("ControlData", getResources().getString(R.string.take_phone_mode_title) + "  "
                            + backContact(data)[0]);
                    mdata.putExtra("ActionData", "startupcall" + ";"
                            + backContact(data)[0] + ";" + backContact(data)[1]);
                    setResult(getIntent().getExtras().getInt("type"), mdata);
                    finish();
                }
            }
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

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mPhoneWakeSetting) {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, 0);
        } else if (preference == mSmsWakeSetting) {
            Intent data = new Intent();
            data.putExtra("ControlData", getResources().getString(R.string.send_sms_mode_title));
            data.putExtra("ActionData", "startupmms" + ";");
            setResult(getIntent().getExtras().getInt("type"), data);
            finish();
        } else if (preference == mAppWakeSetting) {
            Intent intent = new Intent(this, SmartWakeStartupAppList.class);
            startActivityForResult(intent, 0);
        } else if (preference == mMusicWakeSetting) {
            Intent intent = new Intent(this, SmartWakeMusicSettings.class);
            startActivityForResult(intent, 0);
        } else if (preference == mUnlockWakeSetting) {
            Intent data = new Intent();
            data.putExtra("ControlData", getResources().getString(R.string.unlock_screen_mode_title));
            data.putExtra("ActionData", "unlock" + ";");
            setResult(getIntent().getExtras().getInt("type"), data);
            finish();
        } else if (preference == mOnOff) {
            mSmartWakeEnable = mOnOff.isChecked();
            mOnOff.setTitle(mSmartWakeEnable ? mOnOff.getSwitchTextOn() : mOnOff.getSwitchTextOff() );
            Settings.System.putInt(mContentResolver, mSmartWakeEnableName, mSmartWakeEnable ? 1 : 0);
            setAllCustomPreferenceEnable(mSmartWakeEnable);
        }
        return false;
    }

    private void setAllCustomPreferenceEnable(boolean enable) {
        mPhoneWakeSetting.setEnabled(enable);
        mSmsWakeSetting.setEnabled(enable);
        mAppWakeSetting.setEnabled(enable);
        mMusicWakeSetting.setEnabled(enable);
        mUnlockWakeSetting.setEnabled(enable);
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

    private static final String getSmartWakeEnableName(int type) {
        if (type >= 0 && type < SmartWakeGestureSettings.KEY_ENABLED_SETTING_MAP.length) {
            return SmartWakeGestureSettings.KEY_ENABLED_SETTING_MAP[type];
        }
        return null;
    }

    private static final int getSmartWakePreferenceTitle(int type) {
        if (type >= 0 && type < KEY_STRING_ID_MAP.length) {
            return KEY_STRING_ID_MAP[type];
        }
        return -1;
    }

    private static final String getSmartWakeActionName(int type) {
        if (type >= 0 && type < SmartWakeGestureSettings.KEY_ACTION_SETTING_MAP.length) {
            return SmartWakeGestureSettings.KEY_ACTION_SETTING_MAP[type];
        }
        return "";
    }

    private String[] backContact(Intent intent) {
        Uri contactData = intent.getData();
        if (contactData == null) {
            return null;
        }
        Cursor cursor = null;
        Cursor phone = null;
        try {
            ContentResolver cr = getContentResolver();
            cursor = cr.query(contactData, null, null, null, null);
            cursor.moveToFirst();
            String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            phone = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                    null,
                    null);
            String number = null;
            while (phone.moveToNext()) {
                number = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            }

            String[] contactInfArray = new String[2];
            contactInfArray[0] = contactName + "/" + number;
            contactInfArray[1] = number;
            return contactInfArray;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (phone != null) {
                phone.close();
            }
        }
    }
}
