/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui;

import android.animation.ArgbEvaluator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;

//*/ Modified begin by droi xupeng 20160427, make battery icon can be horizontal
import java.io.File;
import java.io.FileInputStream;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
//*/
import com.android.systemui.statusbar.policy.BatteryController;
import com.mediatek.systemui.statusbar.util.BatteryHelper;

public class BatteryMeterView extends View implements DemoMode,
        BatteryController.BatteryStateChangeCallback {
    public static final String TAG = BatteryMeterView.class.getSimpleName();
    public static final String ACTION_LEVEL_TEST = "com.android.systemui.BATTERY_LEVEL_TEST";
    public static final String SHOW_PERCENT_SETTING = "status_bar_show_battery_percent";

    private static final boolean SINGLE_DIGIT_PERCENT = false;

    private static final int FULL = 96;

    private static final float BOLT_LEVEL_THRESHOLD = 0.3f;  // opaque bolt below this fraction

    //*/ Modified begin by droi xupeng 20160428, make battery icon can be horizontal
    private static final boolean SHOW_HORIZONTAL = true;
    private float mBatteryPadding = 0f;
    private final RectF mClipFrame = new RectF();
    private final RectF mBatteryFrame = new RectF();
    private float mDensity = 1f;
    private int mFramePaintWidth = 2;
    public static final int EMPTY = 4;
    public static final float SUBPIXEL = 0.4f;  // inset rects for softer edges
    private final Paint mButtonPaint;
    //*/

    private final int[] mColors;

    private boolean mShowPercent;
    private float mButtonHeightFraction;
    private float mSubpixelSmoothingLeft;
    private float mSubpixelSmoothingRight;
    private final Paint mFramePaint, mBatteryPaint, mWarningTextPaint, mTextPaint, mBoltPaint;
    private float mTextHeight, mWarningTextHeight;
    private int mIconTint = Color.WHITE;

    private int mHeight;
    private int mWidth;
    private String mWarningString;
    private final int mCriticalLevel;
    private int mChargeColor;
    private final float[] mBoltPoints;
    private final Path mBoltPath = new Path();

    private final RectF mFrame = new RectF();
    private final RectF mButtonFrame = new RectF();
    private final RectF mBoltFrame = new RectF();

    private final Path mShapePath = new Path();
    /// M:Optimize the draw methord of battery path
    private final Path mShapePathUnion = new Path();
    private final Path mClipPath = new Path();
    private final Path mTextPath = new Path();

    private BatteryController mBatteryController;
    private boolean mPowerSaveEnabled;

    private int mDarkModeBackgroundColor;
    private int mDarkModeFillColor;

    private int mLightModeBackgroundColor;
    private int mLightModeFillColor;

    private BatteryTracker mTracker = new BatteryTracker();
    private final SettingObserver mSettingObserver = new SettingObserver();

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        //*/ Modified begin by droi xupeng 20160428, make battery icon can be horizontal
        mContext = context;
        //*/
        final Resources res = context.getResources();
        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView,
                defStyle, 0);
        final int frameColor = atts.getColor(R.styleable.BatteryMeterView_frameColor,
                context.getColor(R.color.batterymeter_frame_color));
        TypedArray levels = res.obtainTypedArray(R.array.batterymeter_color_levels);
        TypedArray colors = res.obtainTypedArray(R.array.batterymeter_color_values);

        final int N = levels.length();
        mColors = new int[2*N];
        for (int i=0; i<N; i++) {
            mColors[2*i] = levels.getInt(i, 0);
            mColors[2*i+1] = colors.getColor(i, 0);
        }
        levels.recycle();
        colors.recycle();
        atts.recycle();
        updateShowPercent();
        mWarningString = context.getString(R.string.battery_meter_very_low_overlay_symbol);
        mCriticalLevel = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_criticalBatteryWarningLevel);
        mButtonHeightFraction = context.getResources().getFraction(
                R.fraction.battery_button_height_fraction, 1, 1);
        mSubpixelSmoothingLeft = context.getResources().getFraction(
                R.fraction.battery_subpixel_smoothing_left, 1, 1);
        mSubpixelSmoothingRight = context.getResources().getFraction(
                R.fraction.battery_subpixel_smoothing_right, 1, 1);

        //*/Modified begin by droi xupeng 20160428, make battery icon can be horizontal
        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setColor(frameColor);
        mFramePaint.setDither(true);
        mFramePaint.setAntiAlias(true);
        mFramePaint.setStrokeWidth(mFramePaintWidth);
        mFramePaint.setStyle(Paint.Style.STROKE);
        mFramePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));

        mBatteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBatteryPaint.setDither(true);
        mBatteryPaint.setStyle(Paint.Style.FILL);

        mButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mButtonPaint.setColor(res.getColor(R.color.batterymeter_frame_color));
        mButtonPaint.setDither(true);
        mButtonPaint.setStrokeWidth(2);
        mButtonPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mButtonPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(0xFFFFFFFF);
        Typeface font = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        mTextPaint.setTypeface(font);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mWarningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWarningTextPaint.setColor(mColors[1]);
        font = Typeface.create("sans-serif", Typeface.BOLD);
        mWarningTextPaint.setTypeface(font);
        mWarningTextPaint.setTextAlign(Paint.Align.CENTER);

        mChargeColor = context.getColor(R.color.batterymeter_charge_color);

        mBoltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBoltPaint.setColor(context.getColor(R.color.batterymeter_bolt_color));
        mBoltPoints = loadBoltPoints(res);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        //*/

        mDarkModeBackgroundColor =
                context.getColor(R.color.dark_mode_icon_color_dual_tone_background);
        mDarkModeFillColor = context.getColor(R.color.dark_mode_icon_color_dual_tone_fill);
        mLightModeBackgroundColor =
                context.getColor(R.color.light_mode_icon_color_dual_tone_background);
        mLightModeFillColor = context.getColor(R.color.light_mode_icon_color_dual_tone_fill);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ACTION_LEVEL_TEST);
        final Intent sticky = getContext().registerReceiver(mTracker, filter);
        if (sticky != null) {
            // preload the battery level
            mTracker.onReceive(getContext(), sticky);
        }
        mBatteryController.addStateChangedCallback(this);
        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(SHOW_PERCENT_SETTING), false, mSettingObserver);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        getContext().unregisterReceiver(mTracker);
        mBatteryController.removeStateChangedCallback(this);
        getContext().getContentResolver().unregisterContentObserver(mSettingObserver);
    }

    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        mPowerSaveEnabled = mBatteryController.isPowerSave();
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        // TODO: Use this callback instead of own broadcast receiver.
    }

    @Override
    public void onPowerSaveChanged() {
        mPowerSaveEnabled = mBatteryController.isPowerSave();
        invalidate();
    }

    private static float[] loadBoltPoints(Resources res) {
        final int[] pts = res.getIntArray(R.array.batterymeter_bolt_points);
        int maxX = 0, maxY = 0;
        for (int i = 0; i < pts.length; i += 2) {
            maxX = Math.max(maxX, pts[i]);
            maxY = Math.max(maxY, pts[i + 1]);
        }
        final float[] ptsF = new float[pts.length];
        for (int i = 0; i < pts.length; i += 2) {
            ptsF[i] = (float)pts[i] / maxX;
            ptsF[i + 1] = (float)pts[i + 1] / maxY;
        }
        return ptsF;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mHeight = h;
        mWidth = w;
        //*/ Modified begin by droi xupeng 20160428, make battery icon can be horizontal
        mWarningTextPaint.setTextSize(h * 0.4f);
        //*/
        mWarningTextHeight = -mWarningTextPaint.getFontMetrics().ascent;
    }

    private void updateShowPercent() {
        mShowPercent = false && 0 != Settings.System.getInt(getContext().getContentResolver(),
                SHOW_PERCENT_SETTING, 0);
    }

    private int getColorForLevel(int percent) {

        // If we are in power save mode, always use the normal color.
        if (mPowerSaveEnabled) {
            return mColors[mColors.length-1];
        }
        int thresh, color = 0;
        for (int i=0; i<mColors.length; i+=2) {
            thresh = mColors[i];
            color = mColors[i+1];
            /*/ Modified begin by droi xupeng 20160428, make battery icon can be horizontal
            if (percent <= thresh) return (color == mIconTint  && color == mColors[mColors.length-1]) ? color : mIconTint;
            //*/
            //*/freeme.gejun, show red when low power
            if (percent <= thresh) {

                // Respect tinting for "normal" level
                if (i == mColors.length-2) {
                    return mIconTint;
                } else {
                    return color;
                }
            }
            //*/
        }

        return color;
    }

    public void setDarkIntensity(float darkIntensity) {
        int backgroundColor = getBackgroundColor(darkIntensity);
        int fillColor = getFillColor(darkIntensity);
        mIconTint = fillColor;
        /*/ freeme Jack 20160824, adjust icon tint for light Immersion statusbar
        mFramePaint.setColor(backgroundColor);
        /*/
        mFramePaint.setColor(fillColor);
        //*/
        mBoltPaint.setColor(fillColor);

        //*/ freeme Jack 20160824, adjust icon tint for light Immersion statusbar
        mButtonPaint.setColor(fillColor);
        mBatteryPaint.setColor(fillColor);
        mWarningTextPaint.setColor(fillColor);
        mTextPaint.setColor(fillColor);
        //*/

        mChargeColor = fillColor;
        invalidate();
    }

    private int getBackgroundColor(float darkIntensity) {
        return getColorForDarkIntensity(
                darkIntensity, mLightModeBackgroundColor, mDarkModeBackgroundColor);
    }

    private int getFillColor(float darkIntensity) {
        return getColorForDarkIntensity(
                darkIntensity, mLightModeFillColor, mDarkModeFillColor);
    }

    private int getColorForDarkIntensity(float darkIntensity, int lightColor, int darkColor) {
        return (int) ArgbEvaluator.getInstance().evaluate(darkIntensity, lightColor, darkColor);
    }

    @Override
    public void draw(Canvas c) {
        //*/ Modified begin by droi xupeng 20160428, make battery icon can be horizontal
        if(SHOW_HORIZONTAL) {
            c.rotate(-90, mWidth / 2, mHeight / 2);
        }
        //*/

        BatteryTracker tracker = mDemoMode ? mDemoTracker : mTracker;
        final int level = tracker.level;
        /// M: Support "Battery Protection".
        final boolean mChargingProtection =
            tracker.plugged && BatteryHelper.isPlugForProtection(tracker.status, tracker.level);
        //*/ freeme.shanjibing, 20160705. add log for battery anim
        Log.i(TAG,"mChargingProtection="+mChargingProtection);
        //*/
        if (level == BatteryTracker.UNKNOWN_LEVEL) return;

        float drawFrac = (float) level / 100f;

        //*/ Modified begin by droi xupeng 20160428, make battery icon can be horizontal
        if(mChargingProtection) {
            if(mSweepFrac != 0) {
                drawFrac = (float)((mSweepFrac - FRAC_INTERVAL) / 100f);
            } else {
                mTmpSweepFrac = mSweepFrac = level;
            }
        } else {
            mSweepFrac = 0;
            mTmpSweepFrac = 0;
        }
        //*/

        final int pt = getPaddingTop();
        final int pl = getPaddingLeft();
        final int pr = getPaddingRight();
        final int pb = getPaddingBottom();
        int height = mHeight - pt - pb; // M: Support "Wireless Charging"
        final int width = mWidth - pl - pr;
        /// M: Support "Wireless Charging". @{
        if (mChargingProtection && BatteryHelper.isWirelessCharging(tracker.plugType)) {
            height = (int) ((mHeight - pt - pb) * 0.95);
        }
        /// M: Support "Wireless Charging". @}

        //*/ Modified begin by droi xupeng 20160427, make battery icon can be horizontal
        final int buttonHeight = (int) (height * 0.10f);

        if(SHOW_HORIZONTAL) {
            mFrame.set(width * 0.27f, 0, width * 0.67f, height);
            mFrame.offset(pl, pt);

            mButtonFrame.set(
                    mFrame.left + width * 0.1f,
                    mFrame.top + 1,
                    mFrame.right - width * 0.1f,
                    mFrame.top + buttonHeight /* + 5 cover frame border of intersecting area*/);
        } else {
            mFrame.set(0, 0, width, height);
            mFrame.offset(pl, pt);

            // button-frame: area above the battery body
            mButtonFrame.set(
                    mFrame.left + Math.round(width * 0.25f),
                    mFrame.top,
                    mFrame.right - Math.round(width * 0.25f),
                    mFrame.top + buttonHeight + 5);
        }
        mButtonFrame.top += SUBPIXEL;
        mButtonFrame.left += SUBPIXEL;
        mButtonFrame.right -= SUBPIXEL;

        // frame: battery body area
        mFrame.top += buttonHeight;
        mFrame.left += SUBPIXEL;
        mFrame.top += SUBPIXEL;
        mFrame.right -= SUBPIXEL;
        mFrame.bottom -= SUBPIXEL;

        mBatteryPadding = width * 0.05f;
        mBatteryFrame.set(
                mFrame.left + mBatteryPadding,
                mFrame.top + mBatteryPadding,
                mFrame.right - mBatteryPadding,
                mFrame.bottom- mBatteryPadding);

        // first, draw the battery shape
        c.drawRoundRect(mFrame, 3, 3, mFramePaint);

        // set the battery charging color
        /*/freeme.gejun change charge color to green for xlj
        mBatteryPaint.setColor(tracker.plugged ? mChargeColor : getColorForLevel(level));
        /*/
        mBatteryPaint.setColor(tracker.plugged ? mContext.getColor(R.color.batterymeter_charge_color) : getColorForLevel(level));
        //*/

        if (!mChargingProtection) {
            if (level >= FULL) {
                drawFrac = 1f;
            } else if (level <= EMPTY) {
                drawFrac = 0f;
            }
        }
        final float levelTop = drawFrac == 1f ? mButtonFrame.top
                : (mFrame.top + (mFrame.height() * (1f - drawFrac)));

        c.drawRoundRect(mButtonFrame, 2f, 2f, mButtonPaint);

        mClipFrame.set(mBatteryFrame);
        mClipFrame.top += (mBatteryFrame.height() * (1f - drawFrac));

        c.save(Canvas.CLIP_SAVE_FLAG);
        c.clipRect(mClipFrame);
        c.drawRoundRect(mBatteryFrame, 2, 2, mBatteryPaint);
        c.restore();
        if(SHOW_HORIZONTAL) {
            c.rotate(90, mWidth / 2, mHeight / 2);
        }

        /// M: Support "Battery Protection".
        if (mChargingProtection) {
            if(mIsQuickCharging) {
                // define the bolt shape
                float bl, bt, br, bb;
                if (SHOW_HORIZONTAL) {
                    bl = mBatteryFrame.left + mBatteryFrame.width() * 0.1f;
                    bt = mBatteryFrame.top + mBatteryFrame.height() * 0.3f;
                    br = mBatteryFrame.right - mBatteryFrame.width() * 0.05f;
                    bb = mBatteryFrame.bottom - mBatteryFrame.height() * 0.3f;
                } else {
                    bl = mFrame.left + mFrame.width() / 4.5f;
                    bt = mFrame.top + mFrame.height() / 6f;
                    br = mFrame.right - mFrame.width() / 7f;
                    bb = mFrame.bottom - mFrame.height() / 10f;
                }
                if (mBoltFrame.left != bl || mBoltFrame.top != bt
                        || mBoltFrame.right != br || mBoltFrame.bottom != bb) {
                    mBoltFrame.set(bl, bt, br, bb);
                    mBoltPath.reset();
                    mBoltPath.moveTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                    for (int i = 2; i < mBoltPoints.length; i += 2) {
                        mBoltPath.lineTo(
                                mBoltFrame.left + mBoltPoints[i] * mBoltFrame.width(),
                                mBoltFrame.top + mBoltPoints[i + 1] * mBoltFrame.height());
                    }
                    mBoltPath.lineTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                }
                c.drawPath(mBoltPath, mBoltPaint);
            }

            /// M: Support "Wireless Charging". @{
            if (BatteryHelper.isWirelessCharging(tracker.plugType)) {
                c.drawLine(mFrame.left,
                            mHeight,
                            mFrame.right,
                            mHeight,
                            mBatteryPaint);
            }
            /// M: Support "Wireless Charging". @}
        }

        // compute percentage text
        boolean pctOpaque = false;
        float pctX = 0, pctY = 0;
        String pctText = null;
        if (!tracker.plugged && level > mCriticalLevel && mShowPercent) {
            mTextPaint.setColor(getColorForLevel(level));
            mTextPaint.setTextSize(height *
                    (SINGLE_DIGIT_PERCENT ? 0.75f
                            : (tracker.level == 100 ? 0.38f : 0.5f)));
            mTextHeight = -mTextPaint.getFontMetrics().ascent;
            pctText = String.valueOf(SINGLE_DIGIT_PERCENT ? (level/10) : level);
            pctX = mWidth * 0.5f;
            pctY = (mHeight + mTextHeight) * 0.47f;
            pctOpaque = levelTop > pctY;
            if (!pctOpaque) {
                mTextPath.reset();
                mTextPaint.getTextPath(pctText, 0, pctText.length(), pctX, pctY, mTextPath);
                // cut the percentage text out of the overall shape
                mShapePath.op(mTextPath, Path.Op.DIFFERENCE);
            }
        }

        if(mChargingProtection && drawFrac != 1f) {
            mLevel = level;
            drawChargingAnim(level);
        }
        /*
        // draw the battery shape background
        c.drawPath(mShapePath, mFramePaint);

        // draw the battery shape, clipped to charging level
        mFrame.top = levelTop;
        mClipPath.reset();
        mClipPath.addRect(mFrame,  Path.Direction.CCW);
        /// M:Optimize the draw methord of battery path @{
        mShapePathUnion.reset();
        mShapePathUnion.op(mShapePath, Path.Op.UNION);
        mShapePathUnion.op(mClipPath, Path.Op.INTERSECT);
        /// @}
        c.drawPath(mShapePathUnion, mBatteryPaint);
        */
        //*/
        if (!tracker.plugged) {
            if (level <= mCriticalLevel) {
                // draw the warning text
            	//*/ freeme, gouzhouping, 20160928, for show red exclamation point.
            	mWarningTextPaint.setColor(mColors[1]);
            	//*/
                final float x = mWidth * 0.5f;
                final float y = (mHeight + mWarningTextHeight) * 0.48f;
                c.drawText(mWarningString, x, y, mWarningTextPaint);
            } else if (pctOpaque) {
                // draw the percentage text
                c.drawText(pctText, pctX, pctY, mTextPaint);
            }
        }
    }

    //*/ Modified begin by droi xupeng 20160428, make battery icon can be horizontal
    private final int FRAC_FULL = 100;
    private final int FRAC_INTERVAL = 10;
    private int mSweepFrac = 0;
    private int mTmpSweepFrac = 0;
    private int mLevel = 0;
    private Handler mHandler = new BatteryHandler();

    private static final int MSG_CHARGING_ANIM = 0;
    private static final int MSG_CHARGING_TYPE = 1;
    private static final long CHARGING_DELAY_NORMAL = 1000;
    private static final long CHARGING_DELAY_QUICK = 300;

    private class BatteryHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CHARGING_ANIM:
                    drawChargingAnim(mLevel);
                    break;

                case MSG_CHARGING_TYPE:
                    Toast.makeText(mContext, R.string.charging_normal_tip, Toast.LENGTH_LONG)
                         .show();
                    mHandler.removeMessages(MSG_CHARGING_TYPE);
                    break;

                default:
                    break;
            }
        }
    }

    private void drawChargingAnim(int level) {
        mHandler.removeMessages(MSG_CHARGING_ANIM);
        mSweepFrac += 1;
        if (mSweepFrac >= FRAC_FULL + FRAC_INTERVAL) {
            mTmpSweepFrac = mSweepFrac = level;
        }

        if (mSweepFrac - mTmpSweepFrac >= FRAC_INTERVAL
                || mSweepFrac == (FRAC_FULL + FRAC_INTERVAL - 1)) {
            mTmpSweepFrac = mSweepFrac;
            postInvalidate();
        } else {
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_CHARGING_ANIM;
            if (mIsQuickCharging) {
                mHandler.sendMessageDelayed(msg, CHARGING_DELAY_QUICK / FRAC_INTERVAL);
            } else {
                mHandler.sendMessageDelayed(msg, CHARGING_DELAY_NORMAL / FRAC_INTERVAL);
            }
        }
    }
    //*/

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private boolean mDemoMode;
    private BatteryTracker mDemoTracker = new BatteryTracker();

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (!mDemoMode && command.equals(COMMAND_ENTER)) {
            mDemoMode = true;
            mDemoTracker.level = mTracker.level;
            mDemoTracker.plugged = mTracker.plugged;
        } else if (mDemoMode && command.equals(COMMAND_EXIT)) {
            mDemoMode = false;
            postInvalidate();
        } else if (mDemoMode && command.equals(COMMAND_BATTERY)) {
            String level = args.getString("level");
            String plugged = args.getString("plugged");
            if (level != null) {
                mDemoTracker.level = Math.min(Math.max(Integer.parseInt(level), 0), 100);
            }
            if (plugged != null) {
                mDemoTracker.plugged = Boolean.parseBoolean(plugged);
            }
            postInvalidate();
        }
    }

    private boolean mIsQuickCharging = false;
    private boolean mIsNormalCharging = true;
    private boolean mOldChargeState = mIsNormalCharging;
    private static final int CHARGING_TYPE_USB = 0;
    private static final int CHARGING_TYPE_AC = 1;
    private static final int CHARGING_TYPE_QUICK = 2;
    private static final int CHARGING_TYPE_WIRELESS = 3;
    private int mTipsType = -1;
    private int mTmpTipType = -1;
    private Context mContext = null;

    private static boolean isQuickCharging() {
        boolean ret = false;
        File f = new File("/sys/bus/platform/devices/battery/Pump_Express");
        if (f.exists()) {
            try {
                FileInputStream fis = new FileInputStream(f);
                int length = fis.available();
                byte[] buffer = new byte[length];
                fis.read(buffer);
                String res = new String(buffer);
                fis.close();
                if (res != null && res.startsWith("1")) {
                    ret = true;
                }
            } catch (Exception e) {
            }
        }

        return ret;
    }

    private static boolean isNormalCharging() {
        boolean ret = false;
        File f = new File("/sys/devices/platform/battery_meter/FG_Current");
        if (f.exists()) {
            try {
                FileInputStream fis = new FileInputStream(f);
                int length = fis.available();
                byte[] buffer = new byte[length];
                fis.read(buffer);
                String res = new String(buffer);
                fis.close();
                if (res != null && res.startsWith("-")) {
                    ret = true;
                }
            } catch (Exception e) {
            }
        }

        return ret;
    }
    //*/

    private final class BatteryTracker extends BroadcastReceiver {
        public static final int UNKNOWN_LEVEL = -1;

        // current battery status
        int level = UNKNOWN_LEVEL;
        String percentStr;
        int plugType;
        boolean plugged;
        int health;
        int status;
        String technology;
        int voltage;
        int temperature;
        boolean testmode = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                if (testmode && ! intent.getBooleanExtra("testmode", false)) return;

                level = (int)(100f
                        * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                        / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));

                plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                plugged = plugType != 0;
                health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH,
                        BatteryManager.BATTERY_HEALTH_UNKNOWN);
                status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN);
                technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
                voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                //*/ freeme.shanjibing, 20160705. add log for battery anim
                Log.i(TAG,"plugType="+plugType+" status="+status+" level="+level);
                //*/
                setContentDescription(
                        context.getString(R.string.accessibility_battery_level, level));
                postInvalidate();

                //*/ Modified begin by droi xupeng 20160428, make battery icon can be horizontal
                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    mIsQuickCharging = isQuickCharging();
                } else {
                    mIsQuickCharging = false;
                    mIsNormalCharging = true;
                    mOldChargeState = mIsNormalCharging;
                    mTmpTipType = -1;
                }
                //*/
            } else if (action.equals(ACTION_LEVEL_TEST)) {
                testmode = true;
                post(new Runnable() {
                    int curLevel = 0;
                    int incr = 1;
                    int saveLevel = level;
                    int savePlugged = plugType;
                    Intent dummy = new Intent(Intent.ACTION_BATTERY_CHANGED);
                    @Override
                    public void run() {
                        if (curLevel < 0) {
                            testmode = false;
                            dummy.putExtra("level", saveLevel);
                            dummy.putExtra("plugged", savePlugged);
                            dummy.putExtra("testmode", false);
                        } else {
                            dummy.putExtra("level", curLevel);
                            dummy.putExtra("plugged", incr > 0 ? BatteryManager.BATTERY_PLUGGED_AC
                                    : 0);
                            dummy.putExtra("testmode", true);
                        }
                        getContext().sendBroadcast(dummy);

                        if (!testmode) return;

                        curLevel += incr;
                        if (curLevel == 100) {
                            incr *= -1;
                        }
                        postDelayed(this, 200);
                    }
                });
            }
        }
    }

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            updateShowPercent();
            postInvalidate();
        }
    }
}
