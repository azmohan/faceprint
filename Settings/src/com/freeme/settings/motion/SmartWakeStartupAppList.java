package com.freeme.settings.motion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import com.android.settings.R;

public class SmartWakeStartupAppList extends Activity {

    private LayoutInflater mLayoutInflater;
    private AppListAdapter mAppListAdapter;
    private List<ActivityInfo> mActivityInfoList;
    private PackageManager mPackageManager;

    private final String[] FILTER = new String[]{
            "com.google.android.gms", "com.adobe.flashplayer",
            "com.morpho.app.photocleardemotrial", "com.freeme.locknow",
            "com.mediatek.hotknot.verifier", "com.zhuoyou.freeme",
            "com.zhuoyi.security.lite", "com.zhuoyi.security.service",
            "com.freeme.sharedcenter"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_app_list);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        mAppListAdapter = new AppListAdapter();
        ListView appListView = (ListView) findViewById(R.id.app_list);
        appListView.setAdapter(mAppListAdapter);

        mLayoutInflater = getLayoutInflater();
        mPackageManager = getPackageManager();
        mActivityInfoList = getAppActivityInfoList(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mAppListAdapter != null) {
            if (mAppListAdapter.bitMapList.size() > 0) {
                for (int i = 0; i < mAppListAdapter.bitMapList.size(); i++) {
                    Bitmap bitmap = mAppListAdapter.bitMapList.get(i);
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.isRecycled();
                    }
                }
            }
        }
        mAppListAdapter.bitMapList.clear();

        System.gc();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean filterApp(ActivityInfo activityInfo) {
        for (int i = 0; i < FILTER.length; i++) {
            if (activityInfo.packageName.equals(FILTER[i])) {
                return true;
            }
        }
        return false;
    }

    private List<ActivityInfo> getAppActivityInfoList(Context context) {
        List<ActivityInfo> activtyInfoList = new ArrayList<ActivityInfo>();
        try {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            List<ResolveInfo> allActInfoList = mPackageManager.queryIntentActivities(intent, 0);
            Iterator<ResolveInfo> iterator = allActInfoList.iterator();
            while (iterator.hasNext()) {
                ActivityInfo activityInfo = ((ResolveInfo) iterator.next()).activityInfo;
                if (!filterApp(activityInfo)) {
                    activtyInfoList.add(activityInfo);
                }
            }
            Collections.sort(activtyInfoList, new AppComparator());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return activtyInfoList;
    }

    private class AppListAdapter extends BaseAdapter {
        private ArrayList<Bitmap> bitMapList = null;
        private LruCache<String, Bitmap> mMemoryCache;

        public AppListAdapter() {
            super();
            bitMapList = new ArrayList<Bitmap>();
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory());
            final int cacheSize = maxMemory / 8;
            mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getRowBytes() * bitmap.getHeight();
                }
            };
        }

        @Override
        public int getCount() {
            return mActivityInfoList != null ? mActivityInfoList.size() : 0;
        }

        @Override
        public Object getItem(int arg0) {
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ActivityInfo activityinfo =  mActivityInfoList.get(position);
            final String appName = activityinfo.loadLabel(mPackageManager).toString();
            final String appPackageName = activityinfo.packageName;
            final String appClassNmae = activityinfo.name;

            convertView = mLayoutInflater.inflate(R.layout.list_app_item, parent, false);
            final ImageView appImageView = (ImageView) convertView.findViewById(R.id.app_list_item_img);
            final TextView appNameView = (TextView) convertView.findViewById(R.id.app_list_item_name);

            final Bitmap bitmap = getBitmapFromMemCache(appClassNmae);
            if (bitmap != null) {
                appImageView.setImageBitmap(bitmap);
            } else {
                MyAsyncTask task = new MyAsyncTask(appImageView);
                task.execute(activityinfo, appClassNmae);
            }

            appNameView.setText(appName);
            convertView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    String title = getResources().getString(R.string.open_app_mode_title);
                    Intent data = new Intent();
                    data.putExtra("ControlData", title + "  " + appName);
                    data.putExtra("ActionData", "startupapp" + ";" + appName + ";" + appPackageName + ";" + appClassNmae);
                    setResult(SmartWakeGestureSettings.TYPE_STARTUP_APP, data);
                    finish();
                }
            });

            return convertView;
        }

        private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
            if (getBitmapFromMemCache(key) == null) {
                mMemoryCache.put(key, bitmap);
            }
        }

        private Bitmap getBitmapFromMemCache(String key) {
            return mMemoryCache.get(key);
        }

        class MyAsyncTask extends AsyncTask<Object, String, Bitmap> {
            final ImageView appIco;
            private final WeakReference<ImageView> imageViewWeakReference;

            public MyAsyncTask(ImageView appIco) {
                super();
                imageViewWeakReference = new WeakReference<ImageView>(appIco);
                this.appIco = appIco;
            }

            @Override
            protected Bitmap doInBackground(Object... params) {
                Bitmap bitmap = ((BitmapDrawable) (((ActivityInfo) params[0])
                        .loadIcon(mPackageManager))).getBitmap();
                addBitmapToMemoryCache((String) params[1], bitmap);
                bitMapList.add(bitmap);
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                if (imageViewWeakReference != null && result != null) {
                    final ImageView imageView = imageViewWeakReference.get();
                    if (imageView != null) {
                        appIco.setImageBitmap(result);
                    }
                }
            }
        }
    }

    private class AppComparator implements Comparator<ActivityInfo> {
        private Collator collator = Collator.getInstance();

        @Override
        public int compare(ActivityInfo arg0, ActivityInfo arg1) {
            CollationKey key1 = collator.getCollationKey(arg0.loadLabel(mPackageManager).toString());
            CollationKey key2 = collator.getCollationKey(arg1.loadLabel(mPackageManager).toString());
            return key1.compareTo(key2);
        }
    }
}
