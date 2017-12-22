
package com.droi.systemui.qs.tile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.mediatek.audioprofile.AudioProfileManager;
import com.mediatek.audioprofile.AudioProfileManager.Scenario;
import com.mediatek.common.audioprofile.AudioProfileListener;
import com.mediatek.systemui.statusbar.util.SIMHelper;

public class DroiTileAudioProfile extends QSTile<QSTile.BooleanState> {
    private static final String TAG = "DroiTileAudioProfile";
    private static final boolean DBG = true;

    private static final int CHANGE_PROFILE = 9000;
    
    private static final boolean ENABLE_AUDIO_PROFILE =
            SIMHelper.isMtkAudioProfilesSupport();

    private boolean mUpdating = false;

    private AudioProfileManager mProfileManager;

    private Scenario mCurrentScenario;
    private int mAudioString = R.string.audio_profile;
    private int mAudioState = R.drawable.ic_qs_custom_on;

    public DroiTileAudioProfile(Host host) {
        super(host);
        setAudioProfileUpdates(true);
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
    }

    @Override
    protected void handleClick() {
        Message msg = mHandler.obtainMessage(CHANGE_PROFILE);
        mHandler.sendMessage(msg);
    }
    
    @Override
    protected void handleLongClick() {
        Intent intent = new Intent();
        //*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
        intent.setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$EditProfileActivity"));
        /*/
        intent.setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$AudioProfileSettingsActivity"));
      //*/
        mHost.startActivityDismissingKeyguard(intent);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = mContext.getString(mAudioString);
        state.visible = true;
        state.selected = true;
        state.icon = ResourceIcon.get(mAudioState);
    }

    private void updateAudioProfile(String key) {
        if (key == null) {
            return;
        }
        if (DBG) {
            Log.i(TAG, "updateAudioProfile called, selected profile is: " + key);
        }
        if (ENABLE_AUDIO_PROFILE) {
            mProfileManager.setActiveProfile(key);
        }
        if (DBG) {
            Log.d(TAG, "updateAudioProfile called, setActiveProfile is: " + key);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_PANEL;
    }

    private AudioProfileListener mAudioProfileListenr = new AudioProfileListener() {
        @Override
        public void onProfileChanged(String profileKey) {
            if (ENABLE_AUDIO_PROFILE) {
                if (profileKey != null) {
                    if (!mUpdating) {
                        /// M: AudioProfile is no ready, so skip update
                        Log.d(TAG, "onProfileChanged !mUpdating");
                        return;
                    }
                    Scenario senario = AudioProfileManager.getScenario(profileKey);
                    if (DBG) {
                        Log.d(TAG, "onProfileChanged onReceive called, profile type is: " +
                                senario);
                    }
                    if (senario != null) {
                        updateProfileView(senario);
                    }
                }
            }
        }
    };

    private void updateProfileView(Scenario scenario) {
        if (DBG) {
            Log.d(TAG, "updateProfileView before");
        }
        loadEnabledProfileResource(scenario);
    }

    private void loadEnabledProfileResource(Scenario scenario) {
        if (DBG) {
            Log.d(TAG, "loadEnabledProfileResource called, profile is: " + scenario);
        }
        mCurrentScenario = scenario;
        int audioState;
        switch (scenario) {
        case GENERAL:
            mAudioString = R.string.droi_qs_profile_normal;
            mAudioState = R.drawable.droi_ic_qs_profile_normal;
            break;
        case MEETING:
            mAudioString = R.string.droi_qs_profile_meeting;
            mAudioState = R.drawable.droi_ic_qs_profile_meeting;
            break;
        case OUTDOOR:
            mAudioString = R.string.droi_qs_profile_outdoor;
            mAudioState = R.drawable.droi_ic_qs_profile_outdoor;
            break;
        case SILENT:
            mAudioString = R.string.droi_qs_profile_silent;
            mAudioState = R.drawable.droi_ic_qs_profile_silent;
            break;
        case CUSTOM:
        default:
            mAudioString = R.string.audio_profile;
            mAudioState = R.drawable.ic_qs_custom_on;
            break;
        }
        refreshState();
    }

    public void setAudioProfileUpdates(boolean update) {
        if (update != mUpdating) {
            if (ENABLE_AUDIO_PROFILE) {
                mProfileManager = (AudioProfileManager) mContext.getSystemService(
                    Context.AUDIO_PROFILE_SERVICE);
                mProfileManager.listenAudioProfie(mAudioProfileListenr,
                    AudioProfileListener.LISTEN_PROFILE_CHANGE);
            }
            mUpdating = update;
        } else {
            if (ENABLE_AUDIO_PROFILE) {
                mProfileManager.listenAudioProfie(
                    mAudioProfileListenr, AudioProfileListener.STOP_LISTEN);
            }
        }
    }

    private void changeAudioProfile(String key) {
        if (key == null) {
            return;
        }
        if (DBG) {
            Log.i(TAG, "changeAudioProfile called, selected profile is: " + key);
        }
        if (ENABLE_AUDIO_PROFILE) {
            mProfileManager.setActiveProfile(key);
        }
        if (DBG) {
            Log.d(TAG, "changeAudioProfile called, setActiveProfile is: " + key);
        }
    }

    private Scenario getNextProfile() {
        switch(mCurrentScenario) {
            case MEETING:
           /*/Modified by tyd xiaocui 2015-09-15 for 6.0 style audioprofile
                return Scenario.OUTDOOR;
            case OUTDOOR:
           //*/
                return Scenario.GENERAL;
            case SILENT:
                return Scenario.MEETING;
            case GENERAL:
            case CUSTOM:
            default:
                // There diden't care customized profile
                return Scenario.SILENT;
            }
    }
    
    private void handleChangeToNext() {
        if (ENABLE_AUDIO_PROFILE) {
            Scenario scenario;
            String key;
            
            scenario = getNextProfile();
            key = AudioProfileManager.getProfileKey(scenario);
            
            if (DBG) {
                Log.d(TAG, "handleChangeCurrentProfile: next->" + key);
            }

            changeAudioProfile(key);
        }
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case CHANGE_PROFILE:
                handleChangeToNext();
                break;
            default:
                break;
            }
        }
    };
}
