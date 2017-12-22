package com.freeme.settings.face;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.freeme.util.FreemeFeature;

public final class FaceprintSettingsDashboard extends SettingsPreferenceFragment implements Indexable {
    private static final String TAG = "FaceprintSettingsD";

    public static class FaceprintActivityDashboard extends SettingsActivity { /* empty */ }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        gotoFaceprintSettings();
    }

    private void gotoFaceprintSettings() {
        if(!hasFaceprintSupport(getContext())){
            Log.v(TAG, "Don't support faceprint!!");
            return;
        }
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", FaceprintSettings.class.getName());
        getContext().startActivity(intent);
        getActivity().finish();
    }

    public static boolean hasFaceprintSupport(Context context) {
        if (FreemeFeature.getLocalInt("config.faceprint.ui.entrypos", 0) != 1) {
            return false;
        }
        Intent intent = new Intent();
        intent.setComponent(FaceprintSettings.FACEPRINT_SETTINGS);
        return isIntentAvailable(context, intent);
    }

    public static boolean isIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.GET_ACTIVITIES);
        return list.size() > 0;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.FINGERPRINT;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                    final Resources res = context.getResources();
                    if (hasFaceprintSupport(context)) {
                        SearchIndexableRaw data = new SearchIndexableRaw(context);
                        data.title = res.getString(R.string.security_settings_faceprint_preference_title);
                        data.screenTitle = res.getString(R.string.security_settings_faceprint_preference_title);
                        result.add(data);
                    }
                    return result;
                }
            };
}
