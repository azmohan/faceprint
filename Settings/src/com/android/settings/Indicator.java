package com.android.settings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class Indicator extends View {
    
    Paint mDotPaint = null;
    private int mDotNum = 0;
    private int mCurrentPage = 0;

    public Indicator(Context context) {
        this(context, null);
    }
    
    public Indicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDotPaint = new Paint();
        mDotPaint.setAntiAlias(true);
        mDotPaint.setColor(R.color.guide_dot_normal);
    }
    
    @Override
    public void onDraw(Canvas canvas){
        int radius = 10;
        int cx = radius;
        int cy = 30;
        for (int i = 0; i < mDotNum; i++) {
            if(i == mCurrentPage){
                mDotPaint.setColor(Color.parseColor("#a8a8a8"));
                //mDotPaint.setColor(R.color.guide_dot_selected);
            }else {
                mDotPaint.setColor(Color.parseColor("#cfcfcf"));
                //mDotPaint.setColor(R.color.guide_dot_normal);
            }
            canvas.drawCircle(cx, cy, radius, mDotPaint);
            cx += 40;
        }
        
        
    }
    
    public void setDotNum(int num){
        mDotNum = num;
    }
    
    public void setCurrNum(int curr){
        mCurrentPage = curr;
        invalidate();
    }

}
