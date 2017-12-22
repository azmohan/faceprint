package com.droi.systemui.qs.order;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.widget.ImageView;

import android.provider.Settings;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.droi.systemui.qs.order.entry.QSTileItem;

import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.statusbar.extcb.PluginFactory;
import com.mediatek.systemui.statusbar.util.SIMHelper;


public class QSTileOrderManager implements SharedPreferenceKeys {

    private static final String TAG = "QSTileOrderManager";
    private static final boolean DEBUG = true;

    private static QSTileOrderManager manager = null;

    Context mContext;
    SharedPreferences mPrefs;
    SharedPreferences.Editor mEditor;

    Resources res;
    IQuickSettingsPlugin quickSettingsPlugin;

    private String[] mDefaultList;
    private String[] mOrderedList;
    private String[] mOthersList;

    private ArrayList<QSTileItem> mAllItemList;
    private ArrayList<QSTileItem> mUserItemList;
    private ArrayList<QSTileItem> mOtherItemList;
    
    private int N = 0;
    private boolean sortItemRemoved = false;

    //*/ freeme.shanjibing, 20160707. modify for qstileorder
    private static final String FLOATTASK = "floattask";
    //*/

    private QSTileOrderManager(Context context) {
        this.mContext = context;
        init();
    }

    private void init() {
        mPrefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mEditor = mPrefs.edit();

        res = mContext.getResources();
        quickSettingsPlugin = PluginFactory.getQuickSettingsPlugin(mContext);

        mAllItemList = new ArrayList<QSTileItem>();
        mUserItemList = new ArrayList<QSTileItem>();
        mOtherItemList = new ArrayList<QSTileItem>();
    }

    public synchronized static QSTileOrderManager getInstance(Context context) {
        if(null == manager) {
            manager = new QSTileOrderManager(context); 
        }

        return manager;
    }

    public void registerPreferencesListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        mPrefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterPreferencesListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        mPrefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public String[] getDefaultArrays() {
        return mDefaultList;
    }

    public String[] getOrderedArrays() {
        return mOrderedList;
    }

    public String[] getOthersArrays() {
        return mOthersList;
    }

    public ArrayList<QSTileItem> getOnShowingQSTile() {

        return mUserItemList;
    }
    public ArrayList<QSTileItem> getOthersQSTile() {
        return mOtherItemList;
    }

    public ArrayList<QSTileItem> getAllQSTile() {
        return mAllItemList;
    }

    public boolean isShowBrightnessController() {
        //*/ Modified by tyd hanhao, show BrighenessController forever 2015-08-07
        //return mPrefs.getBoolean(PREF_KEY_SHOW_BRIGHTNESS, true);
        return true;
    }

    public void setShowBrightnessController(boolean show) {
        mEditor.putBoolean(PREF_KEY_SHOW_BRIGHTNESS, show);
        mEditor.commit();
    }

    public boolean isInitialized() {
        return mPrefs.getBoolean("initialized", false);
    }

    public void createItemEntryList() {

        int i = 0;
        mUserItemList.clear();
        if(null != mOrderedList) {
            for(String tileSpec : mOrderedList) {
                try {
				    /*/ freeme, gouzhouping. 20160817, for optimization order activity.
					mUserItemList.add(createItem(tileSpec,i++));
					/*/
                	if(!tileSpec.equals("more")) {
                		mUserItemList.add(createItem(tileSpec,i++));
                	} else {
                		N--;
                		continue;
                	}
					//*/
                } catch (Throwable t) {
                    Log.d(TAG, "Error creating tile for spec: " + tileSpec, t);
                }
            }
        }

        mOtherItemList.clear();

        if(null != mOthersList) {
            for(String tileSpec : mOthersList) {
                try {
                    mOtherItemList.add(createItem(tileSpec,i++));
                } catch (Throwable t) {
                    Log.d(TAG, "Error creating tile for spec: " + tileSpec, t);
                }
            }
        }

        mAllItemList.clear();
        mAllItemList.addAll(mUserItemList);
        mAllItemList.addAll(mOtherItemList);

    }

    private QSTileItem createItem(String spec, int ordered){
        String label = "";
        int iconId = -1;
        if (spec.equals("wifi")) {
            label = res.getString(R.string.quick_settings_wifi_label);
            iconId = R.drawable.droi_ic_qs_wlan_off;
        } else if (spec.equals("bt")) {
            label = res.getString(R.string.quick_settings_bluetooth_label);
            iconId = R.drawable.droi_ic_qs_bt_off;
        } else if (spec.equals("cell")) {
            label = res.getString(R.string.data_usage);
            iconId = R.drawable.droi_ic_qs_data_usage_normal;
        } else if (spec.equals("airplane")) {
            label = res.getString(R.string.airplane_mode);
            iconId = R.drawable.droi_ic_qs_tile_airplane_normal;
        } else if (spec.equals("rotation")) {
            label = res.getString(R.string.quick_settings_rotation_unlocked_label);
            iconId = R.drawable.droi_ic_qs_rotation_off;
        } else if (spec.equals("flashlight")) {
            label = res.getString(R.string.quick_settings_flashlight_label);
            iconId = R.drawable.droi_ic_qs_flashlight_off;
        } else if (spec.equals("location")) {
            label = res.getString(R.string.quick_settings_location_label);
            iconId = R.drawable.droi_ic_qs_gps_off;
        } else if (spec.equals("hotspot")) {
            label = res.getString(R.string.quick_settings_hotspot_label);
        } else if (spec.equals("hotknot") && SIMHelper.isMtkHotKnotSupport()){
            label = res.getString(R.string.quick_settings_hotknot_label);
            iconId = R.drawable.droi_ic_qs_hotknot_off;
        } else if (spec.equals("audioprofile") && SIMHelper.isMtkAudioProfilesSupport()) {
            label = res.getString(R.string.droi_qs_profile_normal);
            iconId = R.drawable.droi_ic_qs_normal_dark;
        } else if (spec.equals("dataconnection") && !SIMHelper.isWifiOnlyDevice()) {
            label = res.getString(R.string.mobile);
            iconId = R.drawable.droi_ic_qs_mobile_off;
        } else if (spec.equals("more")) { //*/ Modified by tyd hanhao, not show 'Sort' button
            //label = res.getString(R.string.quick_setting_more_label);
            //iconId = R.drawable.ic_qs_more_normal;
        } else if (spec.equals("lockscreen")) {
            label = res.getString(R.string.droi_qs_lockscreen_label);
            iconId = R.drawable.droi_ic_qs_lockscreen_normal;
        } else if (spec.equals("shutdown")) {
            label = res.getString(R.string.droi_qs_shutdown_label);
            iconId = R.drawable.droi_ic_qs_shutdown_normal;
        } else if (spec.equals("reboot")) {
            label = res.getString(R.string.droi_qs_reboot_label);
            iconId = R.drawable.droi_ic_qs_reboot_normal;
        } else if (spec.equals("sleep")) {
            label = res.getString(R.string.droi_qs_sleep_label);
            iconId = R.drawable.droi_ic_qs_timeout_off;
        } else if (spec.equals("supershot")) {
            label = res.getString(R.string.droi_qs_supershot_label);
            iconId = R.drawable.droi_ic_qs_supershot_normal;
        }  else if (spec.equals("screenrecorder")) {
            label = res.getString(R.string.droi_qs_screenrecording_label);
            iconId = R.drawable.droi_ic_qs_screenrecorder_normal;
        }  /*else if (spec.equals("floattask")) {
            label = res.getString(R.string.droi_qs_floattask_label);
            iconId = R.drawable.droi_ic_qs_floattask_off;
        } */else if (spec.equals("oneclear")) {
            label = res.getString(R.string.droi_qs_oneclear_label);
            iconId = R.drawable.droi_ic_qs_optimization_normal;
        } else if (spec.equals("powersaving")) {
            label = res.getString(R.string.droi_qs_powersaving_label);
            iconId = R.drawable.droi_ic_qs_powersaving_off;
        } else {
            throw new IllegalArgumentException("Bad tile spec: " + spec);
        }

        return new QSTileItem(spec, label, iconId, ordered);
    }

    public void loadData() {
        //*/ freeme.shanjibing, 20160707. modify for qstileorder
        loadData(false);
        /*/
        loadData(true);
        //*/
    }

    public void loadData(boolean reset) {

        if(reset || !mPrefs.getBoolean("initialized", false)) {
            String defaultTileList = res.getString(R.string.droi_quick_settings_tiles_default);
            String defaultOnshowList = res.getString(R.string.droi_quick_settings_tiles);
            //*/ freeme.shanjibing, 20160707. modify for qstileorder
            if (!QSTileHost.FREEME_FLOATTASK_SUPPORT) {
                int i = defaultOnshowList.indexOf(FLOATTASK);
                if (i!=-1) {
                    defaultOnshowList = defaultOnshowList.substring(0,i-1)+defaultOnshowList.substring(i
                        +FLOATTASK.length(),defaultOnshowList.length());
                }
            }   
            //*/

            mEditor.putString(PREF_KEY_DEFAULE, defaultTileList);
            mEditor.putString(PREF_KEY_ONSHOW, defaultOnshowList);
            mEditor.putBoolean(PREF_KEY_SHOW_BRIGHTNESS, true);
            mEditor.putBoolean("initialized", true);
            mEditor.commit();

            if(DEBUG){
                if(reset) {
                    Log.d(TAG, "reload Data OK for reset...");
                } else {
                    Log.d(TAG, "initialize Dara OK for first init");
                }
            }
        }

        String defaultList = mPrefs.getString(PREF_KEY_DEFAULE, "");
        String onShowList = mPrefs.getString(PREF_KEY_ONSHOW, "");
        
        N = onShowList.split(",").length;
        
        String sortSpec = onShowList.substring(onShowList.lastIndexOf(',') + 1, onShowList.length());
        if(DEBUG) Log.d(TAG, "sortSpec = " + sortSpec);

        String tempOnShow = onShowList;
        if(sortSpec.equals("more")) {
		    /*/ freeme, gouzhouping. 20160817, for optimization order activity.
			tempOnShow = onShowList.substring(0, onShowList.lastIndexOf(','))
			/*/
            tempOnShow = onShowList.substring(0, onShowList.lastIndexOf(',')) + ",more";
			//*/
            sortItemRemoved = true;
            N--;
        } else {
            sortItemRemoved = false;
        }
        
        if(DEBUG) Log.d(TAG, "tempOnShow = " + tempOnShow);

        mDefaultList = defaultList.split(",");
        mOrderedList = tempOnShow.split(",");

        List<String> tempOrderedList = arraysToList(mOrderedList);
        List<String> tempOthersList = new ArrayList<String>();

        for(int j = 0 ; j < mDefaultList.length; j++) {
            if(tempOrderedList.contains(mDefaultList[j])) {
                continue;
            }
            tempOthersList.add(mDefaultList[j]);
        }

        mOthersList = listToArrays(tempOthersList);

        createItemEntryList();
    }

    public void saveData(List<QSTileItem> orderdList, boolean reset) {

        if(reset) {
            Log.d(TAG, "reset QSTile order");
            loadData(true);
            //*/ Added by droi hanhao for android 6.0+ 
            Settings.Secure.putString(mContext.getContentResolver(), "sysui_qs_tiles", "default");
            //*/
            return;
        }

        String newList = "";
        if(sortItemRemoved) {
            List<QSTileItem> tempList = orderdList.subList(0, N);
			/*/ freeme, gouzhouping, 20160817, for optimization order activity.
			newList = listToString(tempList) + ",more";
			/*/
            newList = listToString(tempList) + "," + arraysToString(mOthersList) + ",more";
			//*/
            if(DEBUG) Log.d(TAG, "after add sortItem:" + newList);
        } else {
            newList = listToString(orderdList);
            if(DEBUG) Log.d(TAG, "saved data:" + newList);
        }
 
        mEditor.putString(PREF_KEY_ONSHOW, newList);
        mEditor.commit();
        
        if(DEBUG)
            Log.d(TAG, "save ordered QSTile " + newList);
        
        //*/ Added by droi hanhao for android 6.0+ 
        Settings.Secure.putString(mContext.getContentResolver(), "sysui_qs_tiles", newList);
        //*/
    }

    public String listToString(List<QSTileItem> list) {
        List<String> specList = new ArrayList<String>();

        for(QSTileItem item : list) {
            String spec = item.getTileSpec();

            specList.add(spec);
        }

        return arraysToString(listToArrays(specList));
    }

    public String[] listToArrays(List<String> list) {
        String[] arrays = list.toArray(new String[0]);

        return arrays;
    }

    public List<String> arraysToList(String[] arrays) {
        List<String> list = new ArrayList<String>();

        for(String tile : arrays) {
            list.add(tile);
        }
        return list;
    }

    public String arraysToString(String[] arrays) {
        String string;

        StringBuffer buffer = new StringBuffer();

        int num = arrays.length;
        for (int i = 0 ; i < num ; i++) {
            if(i != num -1) {
                buffer.append(arrays[i] + ",");
            } else {
                buffer.append(arrays[i]);
            }
        }

        string = buffer.toString();

        return string;
    }

}

