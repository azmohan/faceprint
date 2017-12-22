package com.android.settings.deviceinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.Utils;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.List;
//*/Add by Jiangshouting 2016.01.04 for setting code transplant
import com.android.internal.logging.MetricsLogger;
//*/

public class StorageAndBatterySettings extends SettingsPreferenceFragment 
    implements Indexable{
    
    private String KEY_BATTERY_SETTINGS="battery_settings";
    
    private boolean mBatteryPresent = true;
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                boolean batteryPresent = Utils.isBatteryPresent(intent);

                if (mBatteryPresent != batteryPresent) {
                    mBatteryPresent = batteryPresent;
                }
            }
        }
    };
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        addPreferencesFromResource(R.xml.storage_battery_settings);
    }
    
    private void initPreferences(){
        Preference storageBattery = (Preference) findPreference(KEY_BATTERY_SETTINGS);
        if (!mBatteryPresent) {
            getPreferenceScreen().removePreference(storageBattery);
        }
    }
    
    public void onResume() {
        super.onResume();
        
        getActivity().registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        getActivity().unregisterReceiver(mBatteryInfoReceiver);
    }
    //*/Add by Tyd Jiangshouting 2015.11.19 for Setting search function lacking some data.
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
    new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = context.getResources().getString(R.string.storage_battery_settings);
            data.screenTitle = context.getResources().getString(R.string.storage_battery_settings);
            data.keywords = context.getResources().getString(R.string.storage_battery_settings);
            result.add(data);
 
            return result;
        }
    };
    //*/

    //*/Added by Jiangshouting 2016.01.04 for setting code transplant
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.STORAGEANDBATTERYSETTINGS;
    }
    //*/

}
