package com.android.settings;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.settings.Indicator;


public class GuideAlertActivity extends Activity implements View.OnClickListener {
    
    private ViewPager viewPager;
    private GuidePagerAdapter pagerAdapter;
    private List<View> views;
    private ImageButton closeBtn;

    private ImageView indicator;
    private View view_one;
    private View view_second;
    private RelativeLayout relativeLayout;
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.guide_alert_activity);
        indicator = (ImageView)findViewById(R.id.indicator);
        closeBtn = (ImageButton)findViewById(R.id.closebtn);
        closeBtn.setOnClickListener(this);
        relativeLayout = (RelativeLayout) findViewById(R.id.linearlayout);
        Drawable drawable = getResources().getDrawable(R.drawable.guide_1);
        int drawable_height_px = drawable.getIntrinsicHeight();
        ViewGroup.LayoutParams lp = relativeLayout.getLayoutParams();
        lp.height=drawable_height_px;
        lp.width= ViewGroup.LayoutParams.WRAP_CONTENT;
        relativeLayout.setLayoutParams(lp);
        initView();
    }
    public void initView(){
        ImageView img;
        LayoutInflater inflater = LayoutInflater.from(this);
        view_one = inflater.inflate(R.layout.guide_one,null);
        view_second = inflater.inflate(R.layout.guide_second,null);
        views = new ArrayList<View>();


        views.add(view_one);
        views.add(view_second);


        viewPager = (ViewPager)findViewById(R.id.viewpager);
        pagerAdapter = new GuidePagerAdapter(views, this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            
            @Override
            public void onPageSelected(int arg0) {
            }
            
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {

                if(arg0 == 0) {
                    indicator.setBackgroundResource(R.drawable.guide_indator_first);
                }
                if(arg0 == 1){
                    indicator.setBackgroundResource(R.drawable.guide_indator_second);
                }
            }
            
            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });
    }
    
    @Override
    public void onClick(View v) {
        finish();
    }
}
