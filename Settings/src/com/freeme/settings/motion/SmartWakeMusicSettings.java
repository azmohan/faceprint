package com.freeme.settings.motion;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import com.android.settings.R;

public class SmartWakeMusicSettings extends PreferenceActivity implements Preference.OnPreferenceClickListener {
    private static final String KEY_START_FRONT_MUSIC_SETTING = "start_front_music_setting";
    private static final String KEY_START_OR_STOP_MUSIC_SETTING = "start_or_stop_music_setting";
    private static final String KEY_START_NEXT_MUSIC_SETTING = "start_next_music_setting";

    private Preference mFrontMuisc;
    private Preference mStartOrStopMuisc;
    private Preference mNextMuisc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.music_settings);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        mFrontMuisc = findPreference(KEY_START_FRONT_MUSIC_SETTING);
        mStartOrStopMuisc = findPreference(KEY_START_OR_STOP_MUSIC_SETTING);
        mNextMuisc =  findPreference(KEY_START_NEXT_MUSIC_SETTING);
        mFrontMuisc.setOnPreferenceClickListener(this);
        mStartOrStopMuisc.setOnPreferenceClickListener(this);
        mNextMuisc.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String action;
        String control;
        String name = getResources().getString(R.string.control_music_mode_title);
        Intent data = new Intent();
        if (preference == mFrontMuisc) {
            action = "musicprev";
            control = getResources().getString(R.string.prev_music);
        } else if (preference == mStartOrStopMuisc) {
            action = "musicstartpause";
            control = getResources().getString(R.string.start_or_pause_music);
        } else if (preference == mNextMuisc) {
            action = "musicnext";
            control = getResources().getString(R.string.next_music);
        } else {
            return false;
        }
        data.putExtra("ControlData", name + "  " + control);
        data.putExtra("ActionData", "mediacontrol" + ";" + control + ";" + action);
        setResult(SmartWakeGestureSettings.TYPE_MEIDA_CONTROL, data);

        finish();

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
