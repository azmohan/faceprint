package com.android.settings;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;



public final class TargetView extends View {

    private static final float TARGET_RING_RADIUS_OUTER = 13/16f;
    private static final float TARGET_RING_RADIUS_MIDLE =  9/16f;
    private static final float TARGET_RING_RADIUS_INNER =  5/16f;
    private static final float TARGET_AXLE_RADIUS       = 15/16f;

    private final Paint mPaint;
    private final PointF mCenter;

    private float mRingRadiusOuter;
    private float mRingRadiusMidle;
    private float mRingRadiusInner;
    private float mAxleRadius;

    /// Ball

    private static final float BALL_RADIUS = 2/16f;

    private Drawable mBallIcon;
    private final Paint mBallStepPaint;
    private float mBallRadius;

    private final PointF mBallPosition;

    private int mStepCount;
    private float mStepDistanceX;
    private float mStepDistanceY;

    public static final int STATE_NORMAL   = 0;
    public static final int STATE_STEPPING = 1;
    private int mState;

    public TargetView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        
        int colorForeground;
        
        final Resources.Theme theme = context.getTheme();

        TypedArray a = theme.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.Theme, defStyleAttr, 0);
        colorForeground = a.getColor(
                com.android.internal.R.styleable.Theme_colorForeground, -1);
        a.recycle();
        
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2);
        mPaint.setColor(colorForeground);

        mCenter = new PointF();

        /// Ball

        a = context.obtainStyledAttributes(attrs,
                new int[] { android.R.attr.icon }, defStyleAttr, 0);
        mBallIcon = a.getDrawable(0);
        a.recycle();

        mBallStepPaint = new Paint(mPaint);
        mBallStepPaint.setColor(Color.GREEN);

        mBallPosition = new PointF();

        mState = STATE_NORMAL;

        updateBallDrawableState();
    }

    public TargetView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        final float cx = w / 2f;
        final float cy = h / 2f;
        mCenter.set(cx, cy);
        final float radius = Math.min(cx, cy);
        mRingRadiusOuter = TARGET_RING_RADIUS_OUTER * radius;
        mRingRadiusMidle = TARGET_RING_RADIUS_MIDLE * radius;
        mRingRadiusInner = TARGET_RING_RADIUS_INNER * radius;
        mAxleRadius = TARGET_AXLE_RADIUS * radius;

        /// Ball
        mBallRadius = BALL_RADIUS * radius;
        if (oldw == 0 && oldh == 0) {
            mBallPosition.set(cx, cy);
        } else {
            mBallPosition.offset((w-oldw)/2f, (h-oldh)/2f);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        final PointF center = mCenter;
        final float cx = center.x;
        final float cy = center.y;
        final float axleRadius = mAxleRadius;

        final Paint paint = mPaint;
        canvas.save();
        canvas.translate(cx, cy);
        canvas.drawCircle(0, 0, mRingRadiusOuter, paint);  // outer
        canvas.drawCircle(0, 0, mRingRadiusMidle, paint);  // middle
        canvas.drawCircle(0, 0, mRingRadiusInner, paint);  // inner
        canvas.drawLine(-axleRadius,           0, axleRadius,          0, paint); // h
        canvas.drawLine(          0, -axleRadius,          0, axleRadius, paint); // v
        canvas.restore();

        /// Ball
        canvas.save();
        switch (mState) {
            case STATE_STEPPING:
                float r = (float) Math.sqrt(
                        Math.pow(mStepDistanceX * mStepCount, 2) + Math.pow(mStepDistanceY * mStepCount, 2));
                canvas.drawCircle(cx, cy, r + mBallRadius, mBallStepPaint);

                // fall-through
            default:
                mBallIcon.setBounds(mapBallBounds(mBallPosition, mBallRadius));
                mBallIcon.draw(canvas);
                break;
        }
        canvas.restore();
    }

    /// Ball

    private void updateBallDrawableState() {
        if (mBallIcon != null && mBallIcon.isStateful()) {
            final int[] states = (mState == STATE_STEPPING)
                    ? new int[] { android.R.attr.state_pressed }
                    : new int[] { 0 };
            mBallIcon.setState(states);
        }
    }

    public void setBallState(int state, int arg) {
        mState = state;
        switch (state) {
            case STATE_STEPPING:
                mStepCount = arg;
                mStepDistanceX = (mBallPosition.x - mCenter.x) / mStepCount;
                mStepDistanceY = (mBallPosition.y - mCenter.y) / mStepCount;
                break;
            default:
                break;
        }
        updateBallDrawableState();
    }

    public void moveBall(PointF offset) {
        float ox = offset.x;
        float oy = offset.y;
        float or = (float) Math.sqrt(ox * ox + oy * oy);
        if (or > mRingRadiusOuter) {
            ox = (ox * mRingRadiusOuter) / or;
            oy = (oy * mRingRadiusOuter) / or;
        }
        mBallPosition.set(mCenter);
        mBallPosition.offset(ox, oy);

        invalidate();
    }

    public boolean stepBall() {
        if (mState == STATE_STEPPING) {
            --mStepCount;
            if (mStepCount >= 0) {
                moveBall(new PointF(mStepDistanceX * mStepCount, mStepDistanceY * mStepCount));
                return false;
            }
        }
        return true;
    }

    private static Rect mapBallBounds(PointF position, float radius) {
        return new Rect(
                (int) (position.x - radius),
                (int) (position.y - radius),
                (int) (position.x + radius),
                (int) (position.y + radius)
        );
    }
}
