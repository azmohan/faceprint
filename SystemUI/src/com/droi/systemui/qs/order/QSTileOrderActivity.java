package com.droi.systemui.qs.order;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import com.android.systemui.R;
import com.android.systemui.tuner.TunerService;
import com.droi.systemui.qs.order.adapter.DragAdapter;
import com.droi.systemui.qs.order.entry.QSTileItem;
import com.droi.systemui.qs.order.views.DragGrid;

import java.util.ArrayList;

import static com.android.systemui.statusbar.phone.PhoneStatusBar.ACTION_QSTILE_ORDER_CHANGED;

public class QSTileOrderActivity extends Activity implements SharedPreferenceKeys,OnItemClickListener {

    private static final String TAG = "QSTileOrderActivity";

    private ActionBar mActionBar;

    private QSTileOrderManager sOrderManager;

    private DragGrid gridView;
    DragAdapter dragAdapter;

    ArrayList<QSTileItem> allList = new ArrayList<QSTileItem>();
    ArrayList<QSTileItem> actualList = new ArrayList<QSTileItem>();

    private MyQSOrderChangeListener mListener;

    private Switch mBrightnessSwitcher;

    class MyQSOrderChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {

        private Context mContext;
        public MyQSOrderChangeListener(Context context){
            mContext = context;
        };

        @Override
        public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
            Log.d(TAG, "SharedPreferences changed...");
            Intent intent = new Intent(ACTION_QSTILE_ORDER_CHANGED);
            intent.putExtra("showBrightnessView", sOrderManager.isShowBrightnessController());
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN ,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.droi_qs_tile_order_main);
        mActionBar = getActionBar();
        mActionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.notification_header_bg));
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        sOrderManager = QSTileOrderManager.getInstance(this);

        init();

        mListener = new MyQSOrderChangeListener(this);
        sOrderManager.registerPreferencesListener(mListener);
    }
    
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.droi_qstile_reset_menu, menu);
        return true;
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.action_reset:
                // reset
                sOrderManager.saveData(null, true);
                new Handler().postDelayed(new Runnable() {

                    public void run() {
                        refreshViews();
                    }
                }, 100L);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();

        sOrderManager.saveData(dragAdapter.getQSTileList(), false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        sOrderManager.unregisterPreferencesListener(mListener);
    }

    private void init(){

        sOrderManager.loadData();
        allList = sOrderManager.getAllQSTile();
        actualList = sOrderManager.getOnShowingQSTile();

        gridView = (DragGrid)findViewById(R.id.userGridView);
        mBrightnessSwitcher = (Switch)findViewById(R.id.brightness_category_switch);

        // when all tiles are show in QSPanel, we need init this in the actual order.
        //dragAdapter = new DragAdapter(this, actualList);
        dragAdapter = new DragAdapter(this, allList);

        gridView.setAdapter(dragAdapter);
        gridView.setOnItemClickListener(this);

        dragAdapter.addOrderChangedListener(new DragAdapter.OrderChangeListener() {

            public void onOrderChanged() {
                sOrderManager.saveData(dragAdapter.getQSTileList(), false);
            }
        });
        
        findViewById(R.id.line_2).setVisibility(View.GONE);
        findViewById(R.id.brigheness_toggle).setVisibility(View.GONE);

        mBrightnessSwitcher.setChecked(sOrderManager.isShowBrightnessController());
        mBrightnessSwitcher.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                    sOrderManager.setShowBrightnessController(arg1);
            }
        });
    }

    private void refreshViews() {
        allList = sOrderManager.getAllQSTile();

        dragAdapter.setListDate(allList);
        mBrightnessSwitcher.setChecked(sOrderManager.isShowBrightnessController());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, final int position,long id) {
    }

}
