package com.android.settings;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import java.util.ArrayList;
import java.util.List;

import com.android.settings.TrustAgentUtils.TrustAgentComponentInfo;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.mediatek.HobbyDB.CustomHobbyService;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.widget.LockPatternUtils;
import android.os.UserHandle;
import android.os.UserManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.service.trust.TrustAgentService;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.provider.SearchIndexableData;
import android.provider.SearchIndexableResource;
import android.security.KeyStore;
import com.mediatek.settings.ext.ISettingsMiscExt;
import android.app.ActivityManagerNative;

//*/Add by Jiangshouting 2016.01.04 for setting code transplant
import com.android.internal.logging.MetricsLogger;
//*/

//*/freeme.chenhanyuan. 20160607. Lock screen message function useless
import android.preference.Preference.OnPreferenceClickListener;
//*/

public class LockScreenAndPassword extends SettingsPreferenceFragment 
        implements OnPreferenceChangeListener, Indexable{
    
 // Lock Settings
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";
    private static final String KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING =
            "biometric_weak_improve_matching";
    private static final String KEY_BIOMETRIC_WEAK_LIVELINESS = "biometric_weak_liveliness";
    private static final String KEY_LOCK_ENABLED = "lockenabled";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    private static final String KEY_DEVICE_ADMIN_CATEGORY = "device_admin_category";
    private static final String KEY_LOCK_AFTER_TIMEOUT = "lock_after_timeout";
    private static final String KEY_OWNER_INFO_SETTINGS = "owner_info_settings";
    private static final String KEY_ADVANCED_SECURITY = "advanced_security";
    private static final String KEY_MANAGE_TRUST_AGENTS = "manage_trust_agents"; 
    private static final String KEY_TRUST_AGENT = "trust_agent";
    private static final String KEY_POWER_INSTANTLY_LOCKS = "power_button_instantly_locks";
    
 // Misc Settings
    private static final String KEY_SIM_LOCK = "sim_lock";
    private static final String KEY_SHOW_PASSWORD = "show_password";

    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST = 124;
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF = 125;
    private static final int CHANGE_TRUST_AGENT_SETTINGS = 126;
    
    private LockPatternUtils mLockPatternUtils;
    private ListPreference mLockAfter;
    private boolean mIsPrimary;
    
    private SwitchPreference mPowerButtonInstantlyLocks;
    private SwitchPreference mBiometricWeakLiveliness;
    private SwitchPreference mVisiblePattern;
    private SwitchPreference mShowPassword;
    
    //*/add by tyd wangalei 2015 10 12 for second menu sort
    private String SIM_LOCK_SETTINGS="sim_lock_settings";
    //*/
    
 // These switch preferences need special handling since they're not all stored in Settings.
    private static final String SWITCH_PREFERENCE_KEYS[] = { KEY_LOCK_AFTER_TIMEOUT,
            KEY_LOCK_ENABLED, KEY_VISIBLE_PATTERN, KEY_BIOMETRIC_WEAK_LIVELINESS,
            KEY_POWER_INSTANTLY_LOCKS, KEY_SHOW_PASSWORD,  };
    
  //*/Added by tyd Greg 2014-09-17,for visitor mode
    private static final String KEY_VISITOR_MODE_SETTINGS = "key_visitor_mode_settings";
    private Preference mVisitorModePreference;
    //*/
    
    private static final Intent TRUST_AGENT_INTENT =
            new Intent(TrustAgentService.SERVICE_INTERFACE);
    private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";
 // Only allow one trust agent on the platform.
    private static final boolean ONLY_ONE_TRUST_AGENT = true;
    private Intent mTrustAgentClickIntent;
    private DevicePolicyManager mDPM;
    private ISettingsMiscExt mExt;
    
    private SubscriptionManager mSubscriptionManager;
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;

    private static int mLockScreenUserId;

    //*/freeme.chenhanyuan. 20160607. Lock screen message function useless
    private Preference ownerInfoPref;
    private static final int MY_USER_ID = UserHandle.myUserId();
    //*/
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        mLockPatternUtils = new LockPatternUtils(getActivity());
        mExt = (ISettingsMiscExt) UtilsExt.getMiscPlugin(getActivity());
        mSubscriptionManager = SubscriptionManager.from(getActivity());
        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        if (savedInstanceState != null
                && savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
            mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
        }

        mLockScreenUserId = Utils.getEffectiveUserId(getActivity());
    }
    
    @Override
    public void onResume() {
        super.onResume();
     // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();
        
        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (mBiometricWeakLiveliness != null) {
            mBiometricWeakLiveliness.setChecked(
                    lockPatternUtils.isBiometricWeakLivelinessEnabled(mLockScreenUserId));
        }
        if (mVisiblePattern != null) {
            mVisiblePattern.setChecked(
                lockPatternUtils.isVisiblePatternEnabled(mLockScreenUserId));
        }
        if (mPowerButtonInstantlyLocks != null) {
            mPowerButtonInstantlyLocks.setChecked(
                lockPatternUtils.getPowerButtonInstantlyLocks(mLockScreenUserId));
        }

        if (mShowPassword != null) {
            mShowPassword.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.TEXT_SHOW_PASSWORD, 1) != 0);
        }
        //*/freeme.chenhanyuan. 201606022. add for Lock screen message.
        updateOwnerInfo();
        //*/
    }

    //*/freeme.chenhanyuan. 20160607. Lock screen message function useless
    public void updateOwnerInfo() {
        if (ownerInfoPref != null) {
            ownerInfoPref.setSummary(mLockPatternUtils.isOwnerInfoEnabled(MY_USER_ID)
                    ? mLockPatternUtils.getOwnerInfo(MY_USER_ID)
                    : getString(R.string.owner_info_settings_summary));
        }
    }
    //*/
    
    /**
     * Important!
     *
     * Don't forget to update the SecuritySearchIndexProvider if you are doing any change in the
     * logic or adding/removing preferences here.
     */
    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.security_settings);
        root = getPreferenceScreen();

        // Add options for lock/unlock screen
        final int resid = getResIdForLockUnlockScreen(getActivity(), mLockPatternUtils);

        //*/Modified by tyd Greg 2014-09-17,for visitor mode
        if (FeatureOption.TYD_VISITOR_MODE_SUPPORT) {
            if (Settings.System.getInt(getContentResolver(), "tydtech_vistor_mode_state", 0x0) != 0x1) {
                addPreferencesFromResource(resid);
            }
        }else {
            addPreferencesFromResource(resid);
        }
        /*/
        addPreferencesFromResource(resid);
        //*/

        // Add options for device encryption
        mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;

        if (!mIsPrimary) {
            // Rename owner info settings
            //*/freeme.chenhanyuan. 20160607. Lock screen message function useless
            ownerInfoPref = findPreference(KEY_OWNER_INFO_SETTINGS);
            /*/
            Preference ownerInfoPref = findPreference(KEY_OWNER_INFO_SETTINGS);
            //*/
            if (ownerInfoPref != null) {
                if (UserManager.get(getActivity()).isLinkedUser()) {
                    ownerInfoPref.setTitle(R.string.profile_info_settings_title);
                } else {
                    ownerInfoPref.setTitle(R.string.user_info_settings_title);
                }
            }
        }
        //*/freeme.chenhanyuan. 20160607. Lock screen message function useless
        else {
            ownerInfoPref = findPreference(KEY_OWNER_INFO_SETTINGS);
            if (ownerInfoPref != null) {
                ownerInfoPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        OwnerInfoSettings.show(LockScreenAndPassword.this);
                        return true;
                    }
                });
            }
        }
        //*/

        ///M: only emmc project support the feature
        boolean isEMMC = FeatureOption.MTK_EMMC_SUPPORT && !FeatureOption.MTK_CACHE_MERGE_SUPPORT;
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean isLowRAM = am.isLowRamDevice();
        if (isEMMC && !isLowRAM && mIsPrimary) {
            if (LockPatternUtils.isDeviceEncryptionEnabled()) {
                // The device is currently encrypted.
                addPreferencesFromResource(R.xml.security_settings_encrypted);
            } else {
                // This device supports encryption but isn't encrypted.
                addPreferencesFromResource(R.xml.security_settings_unencrypted);
            }
        }

        // Trust Agent preferences
        PreferenceGroup securityCategory = (PreferenceGroup)
                root.findPreference(KEY_SECURITY_CATEGORY);
        if (securityCategory != null) {
            final boolean hasSecurity = mLockPatternUtils.isSecure(mLockScreenUserId);
            ArrayList<TrustAgentComponentInfo> agents =
                    getActiveTrustAgents(getPackageManager(), mLockPatternUtils);
            for (int i = 0; i < agents.size(); i++) {
                final TrustAgentComponentInfo agent = agents.get(i);
                Preference trustAgentPreference =
                        new Preference(securityCategory.getContext());
                trustAgentPreference.setKey(KEY_TRUST_AGENT);
                trustAgentPreference.setTitle(agent.title);
                trustAgentPreference.setSummary(agent.summary);
                // Create intent for this preference.
                Intent intent = new Intent();
                intent.setComponent(agent.componentName);
                intent.setAction(Intent.ACTION_MAIN);
                trustAgentPreference.setIntent(intent);
                // Add preference to the settings menu.
                securityCategory.addPreference(trustAgentPreference);
                if (!hasSecurity) {
                    trustAgentPreference.setEnabled(false);
                    trustAgentPreference.setSummary(R.string.disabled_because_no_backup_security);
                }
            }
        }

        // lock after preference
        mLockAfter = (ListPreference) root.findPreference(KEY_LOCK_AFTER_TIMEOUT);
        if (mLockAfter != null) {
            setupLockAfterPreference();
            updateLockAfterPreferenceSummary();
        }

        // biometric weak liveliness
        mBiometricWeakLiveliness =
                (SwitchPreference) root.findPreference(KEY_BIOMETRIC_WEAK_LIVELINESS);

        // visible pattern
        mVisiblePattern = (SwitchPreference) root.findPreference(KEY_VISIBLE_PATTERN);

        // lock instantly on power key press
        mPowerButtonInstantlyLocks = (SwitchPreference) root.findPreference(
                KEY_POWER_INSTANTLY_LOCKS);
        Preference trustAgentPreference = root.findPreference(KEY_TRUST_AGENT);
        if (mPowerButtonInstantlyLocks != null &&
                trustAgentPreference != null &&
                trustAgentPreference.getTitle().length() > 0) {
            mPowerButtonInstantlyLocks.setSummary(getString(
                    R.string.lockpattern_settings_power_button_instantly_locks_summary,
                    trustAgentPreference.getTitle()));
        }

        // don't display visible pattern if biometric and backup is not pattern
        ///M: Add for voice unlock.
        if ((resid == R.xml.security_settings_biometric_weak ||
                resid == R.xml.security_settings_voice_weak) &&
                mLockPatternUtils.getKeyguardStoredPasswordQuality(mLockScreenUserId) !=
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
            if (securityCategory != null && mVisiblePattern != null) {
                securityCategory.removePreference(root.findPreference(KEY_VISIBLE_PATTERN));
            }
        }

        // Append the rest of the settings
        addPreferencesFromResource(R.xml.security_settings_misc_2);

        ///M: feature replace sim to uim
        changeSimTitle();

        // Do not display SIM lock for devices without an Icc card
        TelephonyManager tm = TelephonyManager.getDefault();
        if (!mIsPrimary || !isSimIccReady() || Utils.isWifiOnly(getActivity())) {
            root.removePreference(root.findPreference(KEY_SIM_LOCK));
        } else {
            // Disable SIM lock if there is no ready SIM card.
            root.findPreference(KEY_SIM_LOCK).setEnabled(isSimReady());
        }

        // Show password
        mShowPassword = (SwitchPreference) root.findPreference(KEY_SHOW_PASSWORD);

        // The above preferences come and go based on security state, so we need to update
        // the index. This call is expected to be fairly cheap, but we may want to do something
        // smarter in the future.
        Index.getInstance(getActivity())
                .updateFromClassNameResource(SecuritySettings.class.getName(), true, true);

        for (int i = 0; i < SWITCH_PREFERENCE_KEYS.length; i++) {
            final Preference pref = findPreference(SWITCH_PREFERENCE_KEYS[i]);
            if (pref != null) pref.setOnPreferenceChangeListener(this);
        }

        //*/Added by tyd Greg 2014-09-17,for visitor mode
        mVisitorModePreference = findPreference(KEY_VISITOR_MODE_SETTINGS);
        if (FeatureOption.TYD_VISITOR_MODE_SUPPORT) {
            if (Settings.System.getInt(getContentResolver(), "tydtech_vistor_mode_state", 0x0) == 0x1) {
                if (mVisitorModePreference != null){
                    getPreferenceScreen().removePreference(mVisitorModePreference);
                }
            }
        }else {
            if (mVisitorModePreference != null) {
                getPreferenceScreen().removePreference(mVisitorModePreference);
            }
        }
        //*/
        
        return root;
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        boolean result = true;
        final String key = preference.getKey();
        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (KEY_LOCK_AFTER_TIMEOUT.equals(key)) {
            int timeout = Integer.parseInt((String) value);
            try {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, timeout);
            } catch (NumberFormatException e) {
                Log.e("SecuritySettings", "could not persist lockAfter timeout setting", e);
            }
            updateLockAfterPreferenceSummary();
        } else if (KEY_LOCK_ENABLED.equals(key)) {
            lockPatternUtils.setLockPatternEnabled((Boolean) value , mLockScreenUserId);
        } else if (KEY_VISIBLE_PATTERN.equals(key)) {
            lockPatternUtils.setVisiblePatternEnabled((Boolean) value,Utils.getEffectiveUserId(getActivity()));
        } else  if (KEY_BIOMETRIC_WEAK_LIVELINESS.equals(key)) {
            if ((Boolean) value) {
                lockPatternUtils.setBiometricWeakLivelinessEnabled(true , mLockScreenUserId);
            } else {
                // In this case the user has just unchecked the checkbox, but this action requires
                // them to confirm their password.  We need to re-check the checkbox until
                // they've confirmed their password
                mBiometricWeakLiveliness.setChecked(true);
                ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(this.getActivity(), this);
                if (!helper.launchConfirmationActivity(
                                CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF, null, false)) {
                    // If this returns false, it means no password confirmation is required, so
                    // go ahead and uncheck it here.
                    // Note: currently a backup is required for biometric_weak so this code path
                    // can't be reached, but is here in case things change in the future
                    lockPatternUtils.setBiometricWeakLivelinessEnabled(false , mLockScreenUserId);
                    mBiometricWeakLiveliness.setChecked(false);
                }
            }
        } else if (KEY_POWER_INSTANTLY_LOCKS.equals(key)) {
            mLockPatternUtils.setPowerButtonInstantlyLocks((Boolean) value , mLockScreenUserId);
        } else if (KEY_SHOW_PASSWORD.equals(key)) {
            Settings.System.putInt(getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD,
                    ((Boolean) value) ? 1 : 0);
        } 
        return result;
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
        } else if (KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING.equals(key)) {
            ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(this.getActivity(), this);
            if (!helper.launchConfirmationActivity(
                    CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST, null, false)) {
                // If this returns false, it means no password confirmation is required, so
                // go ahead and start improve.
                // Note: currently a backup is required for biometric_weak so this code path
                // can't be reached, but is here in case things change in the future
                startBiometricWeakImprove();
            }
        } else if (KEY_TRUST_AGENT.equals(key)) {
            ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(this.getActivity(), this);
            mTrustAgentClickIntent = preference.getIntent();
            if (!helper.launchConfirmationActivity(CHANGE_TRUST_AGENT_SETTINGS, null, false) &&
                    mTrustAgentClickIntent != null) {
                // If this returns false, it means no password confirmation is required.
                startActivity(mTrustAgentClickIntent);
                mTrustAgentClickIntent = null;
            }
        }else if(SIM_LOCK_SETTINGS.equals(key)){
        	//*/add by tyd wangalei 2015 10  12 for second menu sort
            CustomHobbyService mService=new CustomHobbyService(this.getActivity());
            if(mService.isExistData(R.string.lock_pwd_title, R.string.sim_lock_settings_category)){
        		mService.update(R.string.lock_pwd_title, R.string.sim_lock_settings_category);
        	}else{
        		mService.insert(R.string.lock_pwd_title, R.string.sim_lock_settings_category,"com.android.settings.IccLockSettings", 1,"com.android.settings");
        	}
            return super.onPreferenceTreeClick(preferenceScreen, preference);
            //*/
        }else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }
    
    private static int getResIdForLockUnlockScreen(Context context,
            LockPatternUtils lockPatternUtils) {
        int resid = 0;
        if (!lockPatternUtils.isSecure(mLockScreenUserId)) {
            // if there are multiple users, disable "None" setting
            UserManager mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
            List<UserInfo> users = mUm.getUsers(true);
            final boolean singleUser = users.size() == 1;

            if (singleUser && lockPatternUtils.isLockScreenDisabled(mLockScreenUserId)) {
                resid = R.xml.security_settings_lockscreen;
            } else {
                resid = R.xml.security_settings_chooser;
            }
        } else if (lockPatternUtils.usingBiometricWeak() &&
                lockPatternUtils.isBiometricWeakInstalled(mLockScreenUserId)) {
            resid = R.xml.security_settings_biometric_weak;
        } else if (lockPatternUtils.usingVoiceWeak()) {
            resid = R.xml.security_settings_voice_weak;
        } else {
            switch (lockPatternUtils.getKeyguardStoredPasswordQuality(mLockScreenUserId)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    resid = R.xml.security_settings_pattern;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    resid = R.xml.security_settings_pin;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    resid = R.xml.security_settings_password;
                    break;
            }
        }
        return resid;
    }
    
    private static ArrayList<TrustAgentComponentInfo> getActiveTrustAgents(
            PackageManager pm, LockPatternUtils utils) {
        ArrayList<TrustAgentComponentInfo> result = new ArrayList<TrustAgentComponentInfo>();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(TRUST_AGENT_INTENT,
                PackageManager.GET_META_DATA);
        List<ComponentName> enabledTrustAgents = utils.getEnabledTrustAgents(mLockScreenUserId);
        if (enabledTrustAgents != null && !enabledTrustAgents.isEmpty()) {
            for (int i = 0; i < resolveInfos.size(); i++) {
                ResolveInfo resolveInfo = resolveInfos.get(i);
                if (resolveInfo.serviceInfo == null) continue;
                if (!TrustAgentUtils.checkProvidePermission(resolveInfo, pm)) continue;
                TrustAgentComponentInfo trustAgentComponentInfo =
                        TrustAgentUtils.getSettingsComponent(pm, resolveInfo);
                if (trustAgentComponentInfo.componentName == null ||
                        !enabledTrustAgents.contains(
                                TrustAgentUtils.getComponentName(resolveInfo)) ||
                        TextUtils.isEmpty(trustAgentComponentInfo.title)) continue;
                result.add(trustAgentComponentInfo);
                if (ONLY_ONE_TRUST_AGENT) break;
            }
        }
        return result;
    }
    
    private void setupLockAfterPreference() {
        // Compatible with pre-Froyo
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        mLockAfter.setValue(String.valueOf(currentTimeout));
        mLockAfter.setOnPreferenceChangeListener(this);
        final long adminTimeout = (mDPM != null ? mDPM.getMaximumTimeToLock(null) : 0);
        final long displayTimeout = Math.max(0,
                Settings.System.getInt(getContentResolver(), SCREEN_OFF_TIMEOUT, 0));
        if (adminTimeout > 0) {
            // This setting is a slave to display timeout when a device policy is enforced.
            // As such, maxLockTimeout = adminTimeout - displayTimeout.
            // If there isn't enough time, shows "immediately" setting.
            disableUnusableTimeouts(Math.max(0, adminTimeout - displayTimeout));
        }
    }
    
    private void disableUnusableTimeouts(long maxTimeout) {
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            mLockAfter.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            mLockAfter.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            final int userPreference = Integer.valueOf(mLockAfter.getValue());
            if (userPreference <= maxTimeout) {
                mLockAfter.setValue(String.valueOf(userPreference));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        mLockAfter.setEnabled(revisedEntries.size() > 0);
    }
    
    private void updateLockAfterPreferenceSummary() {
        // Update summary message with current value
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (currentTimeout >= timeout) {
                best = i;
            }
        }

        Preference preference = getPreferenceScreen().findPreference(KEY_TRUST_AGENT);
        if (preference != null && preference.getTitle().length() > 0) {
            mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary_with_exception,
                    entries[best], preference.getTitle()));
        } else {
            mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary, entries[best]));
        }
        mLockAfter.setValue(String.valueOf(currentTimeout));
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mTrustAgentClickIntent != null) {
            outState.putParcelable(TRUST_AGENT_CLICK_INTENT, mTrustAgentClickIntent);
        }
    }
    
    /**
     * M: Replace SIM to SIM/UIM.
     */
    private void changeSimTitle() {
        findPreference(KEY_SIM_LOCK).setTitle(
                mExt.customizeSimDisplayString(
                        findPreference(KEY_SIM_LOCK).getTitle().toString(),
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID));
        findPreference("sim_lock_settings").setTitle(
                mExt.customizeSimDisplayString(
                        findPreference("sim_lock_settings").getTitle().toString(),
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID));
    }
    
    /* Return true if a there is a Slot that has Icc.
     */
    private boolean isSimIccReady() {
        TelephonyManager tm = TelephonyManager.getDefault();
        final List<SubscriptionInfo> subInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList();

        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (tm.hasIccCard(subInfo.getSimSlotIndex())) {
                    return true;
                }
            }
        }

        return false;
    }
    
    /* Return true if a SIM is ready for locking.
     * TODO: consider adding to TelephonyManager or SubscritpionManasger.
     */
    private boolean isSimReady() {
        int simState = TelephonyManager.SIM_STATE_UNKNOWN;
        final List<SubscriptionInfo> subInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                simState = TelephonyManager.getDefault().getSimState(subInfo.getSimSlotIndex());
                if((simState != TelephonyManager.SIM_STATE_ABSENT) &&
                            (simState != TelephonyManager.SIM_STATE_UNKNOWN)){
                    return true;
                }
            }
        }
        return false;
    }
    
    public void startBiometricWeakImprove(){
        Intent intent = new Intent();
        intent.setClassName("com.android.facelock", "com.android.facelock.AddToSetup");
        startActivity(intent);
    }
    
    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST &&
                resultCode == Activity.RESULT_OK) {
            startBiometricWeakImprove();
            return;
        } else if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF &&
                resultCode == Activity.RESULT_OK) {
            final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
            lockPatternUtils.setBiometricWeakLivelinessEnabled(false , mLockScreenUserId);
            // Setting the mBiometricWeakLiveliness checked value to false is handled when onResume
            // is called by grabbing the value from lockPatternUtils.  We can't set it here
            // because mBiometricWeakLiveliness could be null
            return;
        } else if (requestCode == CHANGE_TRUST_AGENT_SETTINGS && resultCode == Activity.RESULT_OK) {
            if (mTrustAgentClickIntent != null) {
                startActivity(mTrustAgentClickIntent);
                mTrustAgentClickIntent = null;
            }
            return;
        }
        createPreferenceHierarchy();
    }
    
    /**
     * For Search. Please keep it in sync when updating "createPreferenceHierarchy()"
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new SecuritySearchIndexProvider();

    private static class SecuritySearchIndexProvider extends BaseSearchIndexProvider {

        boolean mIsPrimary;

        public SecuritySearchIndexProvider() {
            super();

            mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {

            List<SearchIndexableResource> result = new ArrayList<SearchIndexableResource>();

            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            // Add options for lock/unlock screen
            int resId = getResIdForLockUnlockScreen(context, lockPatternUtils);

            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = resId;
            result.add(sir);

            if (mIsPrimary) {
                DevicePolicyManager dpm = (DevicePolicyManager)
                        context.getSystemService(Context.DEVICE_POLICY_SERVICE);

                switch (dpm.getStorageEncryptionStatus()) {
                    case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE:
                        // The device is currently encrypted.
                        resId = R.xml.security_settings_encrypted;
                        break;
                    case DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE:
                        // This device supports encryption but isn't encrypted.
                        resId = R.xml.security_settings_unencrypted;
                        break;
                }

                sir = new SearchIndexableResource(context);
                sir.xmlResId = resId;
                result.add(sir);
            }

            // Append the rest of the settings
            sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.security_settings_misc_2;
            result.add(sir);

            return result;
        }

        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
            final Resources res = context.getResources();

            final String screenTitle = res.getString(R.string.lock_pwd_title);

            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = screenTitle;
            data.screenTitle = screenTitle;
            result.add(data);

            if (!mIsPrimary) {
                int resId = (UserManager.get(context).isLinkedUser()) ?
                        R.string.profile_info_settings_title : R.string.user_info_settings_title;

                data = new SearchIndexableRaw(context);
                data.title = res.getString(resId);
                data.screenTitle = screenTitle;
                result.add(data);
            }

            // Credential storage
            final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);

            if (!um.hasUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS)) {
                KeyStore keyStore = KeyStore.getInstance();

                final int storageSummaryRes = keyStore.isHardwareBacked() ?
                        R.string.credential_storage_type_hardware :
                        R.string.credential_storage_type_software;

                data = new SearchIndexableRaw(context);
                data.title = res.getString(storageSummaryRes);
                data.screenTitle = screenTitle;
                result.add(data);
            }

            // Advanced
            final LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            if (lockPatternUtils.isSecure(mLockScreenUserId)) {
                ArrayList<TrustAgentComponentInfo> agents =
                        getActiveTrustAgents(context.getPackageManager(), lockPatternUtils);
                for (int i = 0; i < agents.size(); i++) {
                    final TrustAgentComponentInfo agent = agents.get(i);
                    data = new SearchIndexableRaw(context);
                    data.title = agent.title;
                    data.screenTitle = screenTitle;
                    result.add(data);
                }
            }
            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final List<String> keys = new ArrayList<String>();

            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            // Add options for lock/unlock screen
            int resId = getResIdForLockUnlockScreen(context, lockPatternUtils);

            // don't display visible pattern if biometric and backup is not pattern
            if (resId == R.xml.security_settings_biometric_weak &&
                    lockPatternUtils.getKeyguardStoredPasswordQuality(mLockScreenUserId) !=
                            DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
                keys.add(KEY_VISIBLE_PATTERN);
            }

            // Do not display SIM lock for devices without an Icc card
            if (!mIsPrimary
                    || SubscriptionManager.from(context).getActiveSubscriptionInfoCount() == 0
                    || Utils.isWifiOnly(context)) {
                keys.add(KEY_SIM_LOCK);
            }

            // TrustAgent settings disappear when the user has no primary security.
            if (!lockPatternUtils.isSecure(mLockScreenUserId)) {
                keys.add(KEY_TRUST_AGENT);
            }

            return keys;
        }
    }

    //*/Added by Jiangshouting 2016.01.04 for setting code transplant
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.LOCK_SCREEN_AND_PASSWORD;
    }
    //*/

}
