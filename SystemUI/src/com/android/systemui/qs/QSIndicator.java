package com.android.systemui.qs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by connorlin on 15-8-15.
 */
public class QSIndicator extends View {
    private static QSIndicator mQSIndicator;
    private Paint mPaint;
    private Paint mFocusPaint;

    private int mIndicatorSpace = 20;
    private int mIndicatorRadius = 6;
    private int mPadding = mIndicatorRadius;

    private int mCount = 0;
    private int mIndex = 0;
    private int mViewWidth = 0;
    private int mViewHeight = 0;

    public QSIndicator(Context context) {
        this(context, null);
    }

    public QSIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QSIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mContext = context;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Style.FILL);
        mPaint.setColor(0x26FFFFFF/*Color.DKGRAY*/);

        mFocusPaint = new Paint();
        mFocusPaint.setAntiAlias(true);
        mFocusPaint.setStyle(Style.FILL);
        mFocusPaint.setColor(0xFFD9D9D9/*Color.WHITE*/);
    }

    public static QSIndicator getInstance(Context context) {
        if (mQSIndicator == null) {
            mQSIndicator = new QSIndicator(context);
        }
        return mQSIndicator;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        for (int i = 0; i < mCount; i++) {
            canvas.drawCircle((getWidth() - mViewWidth) / 2 + i * (mIndicatorSpace + mIndicatorRadius),
                    (getHeight() - mViewHeight) / 2 + mPadding,
                    mIndicatorRadius, i == mIndex ? mFocusPaint : mPaint);
        }
    }

    public void setCount(int count) {
        setCount(count, 0);
    }

    public void setCount(int count, int index) {
        mCount = count;
        mIndex = index;
        mViewWidth = count * mIndicatorRadius * 2 + (count - 1) * mIndicatorSpace;
        mViewHeight = mIndicatorRadius * 2;
        invalidate();
    }

    public void setCurrentIndex(int index) {
        mIndex = index;
        invalidate();
    }

}