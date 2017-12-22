package com.droi.suspension;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;

public class FreemeFloatView extends LinearLayout {

    private static final int ACTION_TOUCH_UP    = 1;
    private static final int ACTION_TOUCH_DOWN  = 2;

    private static int sStatusBarHeight;

    private final WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;

    private final ViewConfiguration mViewConfiguration;

    private int mDesignedHeight;
    private int mDesignedWidth;

    private TextView mView;
    private Drawable mBackground;

    private float mTouchMoveRawX;
    private float mTouchRawX;
    private float mTouchX;
    private float mTouchMoveRawY;
    private float mTouchRawY;
    private float mTouchY;

    public FreemeFloatView(Context context) {
        super(context);
        mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        mViewConfiguration = ViewConfiguration.get(context);

        inflate(context, R.layout.freeme_float_window_view, this);
        mView = (TextView) findViewById(R.id.tv_start_cm);

        final Resources r = getResources();
        mDesignedWidth = r.getDimensionPixelOffset(R.dimen.freeme_float_window_width);
        mDesignedHeight = r.getDimensionPixelOffset(R.dimen.freeme_float_window_height);
        mBackground = r.getDrawable(R.drawable.freeme_background_float_window);
    }

    public void setLayoutParams(WindowManager.LayoutParams lp) {
        mWindowParams = lp;
    }

    @Override
    public WindowManager.LayoutParams getLayoutParams() {
        return mWindowParams;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateAlongScreenEdge();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                setBackground(ACTION_TOUCH_DOWN);
                mTouchX = event.getX();
                mTouchY = event.getY();
                mTouchRawX = event.getRawX();
                mTouchRawY = event.getRawY() - getStatusBarHeight();
                mTouchMoveRawX = mTouchRawX;
                mTouchMoveRawY = mTouchRawY;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                mTouchMoveRawX = event.getRawX();
                mTouchMoveRawY = event.getRawY() - getStatusBarHeight();
                update();
                break;
            }
            case MotionEvent.ACTION_UP: {
                setBackground(ACTION_TOUCH_UP);
                if (Math.abs(mTouchRawX - mTouchMoveRawX) < mViewConfiguration.getScaledTouchSlop() &&
                        Math.abs(mTouchRawY - mTouchMoveRawY) < mViewConfiguration.getScaledTouchSlop()) {
                    FreemeFloatManager.doBack(getContext());
                } else {
                    updateAlongScreenEdge();
                }
                break;
            }
            default:
                break;
        }
        return true;
    }

    public int getDesignedWidth() {
        return mDesignedWidth;
    }

    public int getDesignedHeight() {
        return mDesignedHeight;
    }

    private float getStatusBarHeight() {
        if (sStatusBarHeight == 0) {
            try {
                Class<?> dimenClass = Class.forName("com.android.internal.R$dimen");
                int resid = dimenClass.getField("status_bar_height").getInt(null);
                sStatusBarHeight = getResources().getDimensionPixelSize(resid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sStatusBarHeight;
    }

    private void setBackground(int action) {
        if (mView == null)
            return;

        if (mBackground.isStateful()) {
            switch (action) {
                case ACTION_TOUCH_UP:
                    mBackground.setState(new int[] {
                            0
                        }
                    );
                    break;
                case ACTION_TOUCH_DOWN:
                    mBackground.setState(new int[] {
                            android.R.attr.state_pressed
                        }
                    );
                    break;
            }
        }
        mView.setBackground(mBackground);
    }

    private void update() {
        mWindowParams.x = (int)(mTouchMoveRawX - mTouchX);
        mWindowParams.y = (int)(mTouchMoveRawY - mTouchY);
        mWindowManager.updateViewLayout(this, mWindowParams);
    }

    private void updateAlongScreenEdge() {
        DisplayMetrics dm = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(dm);

        int x = dm.widthPixels;
        if (mWindowParams.x <= (x - getWidth()) / 2) {
            x = 0;
        }
        mWindowParams.x = x;

        mWindowManager.updateViewLayout(this, mWindowParams);
    }
}
