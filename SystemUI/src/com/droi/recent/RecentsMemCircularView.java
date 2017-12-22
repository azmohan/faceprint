package com.droi.recent;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.util.Log;

public class RecentsMemCircularView extends View {
    
    private Paint mPaint;
    private RectF mOvals;
    private int mSweep = 0;
    private int mTotal = 0;
    private boolean mdesc = false;
    private boolean mInc = false;
    
    private static final float SWEEP_INC = 3;
    
    public RecentsMemCircularView(Context context) {
        super(context);
    }
    
    public RecentsMemCircularView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
        
    public RecentsMemCircularView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        
        mPaint.setColor(Color.GRAY);
        mPaint.setAlpha(85);
        canvas.drawArc(mOvals, -90, 360, false, mPaint);
        
        mPaint.setColor(Color.GREEN);
        mPaint.setAlpha(125);
        canvas.drawArc(mOvals, -90, mSweep, false, mPaint);
        
        if(mdesc) {
            drawAnimDesc();
        }
        
        if(mInc) {
            drawAnimInc();
        }
    }

    public void setSweep(int sweep, int height) {
        int magrin = 5;
        mTotal = sweep;
        
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(4);
        mOvals = new RectF(magrin, magrin, height - magrin, height - magrin);
        mInc = true;
        invalidate();
    }

    public void upateTotal(int sweep) {
        mTotal = sweep;
    }

    public void drawAnim() {
        mdesc = true;
        invalidate();
    }
    
    private void drawAnimDesc() {
        mSweep -= SWEEP_INC;
        if (mSweep <= 0) {
            mSweep = 0;
        }
        
        if(mSweep != 0) {
            invalidate();
        } else {
            mdesc = false;
            mInc = true;
            invalidate();
        }
    }
    
    private void drawAnimInc() {
        mSweep += SWEEP_INC;
        if (mSweep > mTotal) {
            mSweep = mTotal;
        }
        
        if(mSweep != mTotal) {
            invalidate();
        } else {
            mInc = false;
        }
    }
}