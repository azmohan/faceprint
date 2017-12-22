/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.DroiOnDismissAction;
import com.android.keyguard.KeyguardSecurityCallback;
import com.android.keyguard.KeyguardSecurityView;
import com.android.keyguard.KeyguardStatusView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.DejankUtils;
import com.android.systemui.EventLogConstants;
import com.android.systemui.EventLogTags;
import com.android.systemui.R;
import com.android.systemui.qs.QSContainer;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.phone.PhoneStatusBar.KeyguardPanelViewCallback;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.stack.StackStateAnimator;
import com.mediatek.keyguard.Clock.ClockView;
import com.freeme.keyguard.ICustomCallback;
import com.freeme.keyguard.ICustomLockscreenView;
import com.freeme.keyguard.OnUpdateMonitorCallback;
import com.android.keyguard.BatteryStatus;
import android.os.BatteryManager;


//*/added by Droi  shijiachen 20150720 for customize keyguard
//*/

public class NotificationPanelView extends PanelView implements
        ExpandableView.OnHeightChangedListener, ObservableScrollView.Listener,
        View.OnClickListener, NotificationStackScrollLayout.OnOverscrollTopChangedListener,
        KeyguardAffordanceHelper.Callback, NotificationStackScrollLayout.OnEmptySpaceClickListener,
        HeadsUpManager.OnHeadsUpChangedListener,SensorEventListener {

    private static final boolean DEBUG = false;
    private static final String TAG = "NotificationPanelView";

    // Cap and total height of Roboto font. Needs to be adjusted when font for the big clock is
    // changed.
    private static final int CAP_HEIGHT = 1456;
    private static final int FONT_HEIGHT = 2163;

    private static final float HEADER_RUBBERBAND_FACTOR = 2.05f;
    private static final float LOCK_ICON_ACTIVE_SCALE = 1.2f;

    private static final String COUNTER_PANEL_OPEN = "panel_open";
    private static final String COUNTER_PANEL_OPEN_QS = "panel_open_qs";
    private static final String COUNTER_PANEL_OPEN_PEEK = "panel_open_peek";

    private static final Rect mDummyDirtyRect = new Rect(0, 0, 1, 1);

    public static final long DOZE_ANIMATION_DURATION = 700;

    private KeyguardAffordanceHelper mAfforanceHelper;
    private StatusBarHeaderView mHeader;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    private KeyguardStatusBarView mKeyguardStatusBar;
    private QSContainer mQsContainer;
    private QSPanel mQsPanel;
    private KeyguardStatusView mKeyguardStatusView;
    private ObservableScrollView mScrollView;
    private TextView mClockView;
    private View mReserveNotificationSpace;
    private View mQsNavbarScrim;
    private NotificationsQuickSettingsContainer mNotificationContainerParent;
    private NotificationStackScrollLayout mNotificationStackScroller;
    private int mNotificationTopPadding;
    private boolean mAnimateNextTopPaddingChange;

    private int mTrackingPointer;
    private VelocityTracker mVelocityTracker;
    private boolean mQsTracking;

    private FrameLayout mBlurFrame;

    /**
     * Handles launching the secure camera properly even when other applications may be using the
     * camera hardware.
     */
    private SecureCameraLaunchManager mSecureCameraLaunchManager;

    /**
     * If set, the ongoing touch gesture might both trigger the expansion in {@link PanelView} and
     * the expansion for quick settings.
     */
    private boolean mConflictingQsExpansionGesture;

    /**
     * Whether we are currently handling a motion gesture in #onInterceptTouchEvent, but haven't
     * intercepted yet.
     */
    private boolean mIntercepting;
    private boolean mPanelExpanded;
    private boolean mQsExpanded;
    private boolean mQsExpandedWhenExpandingStarted;
    private boolean mQsFullyExpanded;
    private boolean mKeyguardShowing;
    private boolean mDozing;
    private boolean mDozingOnDown;
    private int mStatusBarState;
    private float mInitialHeightOnTouch;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private float mLastTouchX;
    private float mLastTouchY;
    private float mQsExpansionHeight;
    //*/add by shijiachen 20150721 for customize keyguard
    private float mLastQsExpansionHeight;
    //*/
    private int mQsMinExpansionHeight;
    private int mQsMaxExpansionHeight;
    private int mQsPeekHeight;
    private boolean mStackScrollerOverscrolling;
    private boolean mQsExpansionFromOverscroll;
    private float mLastOverscroll;
    private boolean mQsExpansionEnabled = true;
    private ValueAnimator mQsExpansionAnimator;
    private FlingAnimationUtils mFlingAnimationUtils;
    private int mStatusBarMinHeight;
    private boolean mUnlockIconActive;
    private int mNotificationsHeaderCollideDistance;
    private int mUnlockMoveDistance;
    private float mEmptyDragAmount;

    private Interpolator mFastOutSlowInInterpolator;
    private Interpolator mFastOutLinearInterpolator;
    private Interpolator mDozeAnimationInterpolator;
    private ObjectAnimator mClockAnimator;
    private int mClockAnimationTarget = -1;
    private int mTopPaddingAdjustment;
    private KeyguardClockPositionAlgorithm mClockPositionAlgorithm =
            new KeyguardClockPositionAlgorithm();
    private KeyguardClockPositionAlgorithm.Result mClockPositionResult =
            new KeyguardClockPositionAlgorithm.Result();
    private boolean mIsExpanding;

    private boolean mBlockTouches;
    private int mNotificationScrimWaitDistance;
    // Used for two finger gesture as well as accessibility shortcut to QS.
    private boolean mQsExpandImmediate;
    private boolean mTwoFingerQsExpandPossible;

    /**
     * If we are in a panel collapsing motion, we reset scrollY of our scroll view but still
     * need to take this into account in our panel height calculation.
     */
    private int mScrollYOverride = -1;
    private boolean mQsAnimatorExpand;
    private boolean mIsLaunchTransitionFinished;
    private boolean mIsLaunchTransitionRunning;
    private Runnable mLaunchAnimationEndRunnable;
    private boolean mOnlyAffordanceInThisMotion;
    private boolean mKeyguardStatusViewAnimating;
    private boolean mHeaderAnimating;
    private ObjectAnimator mQsContainerAnimator;
    private ValueAnimator mQsSizeChangeAnimator;

    private boolean mShadeEmpty;

    private boolean mQsScrimEnabled = true;
    private boolean mLastAnnouncementWasQuickSettings;
    private boolean mQsTouchAboveFalsingThreshold;
    private int mQsFalsingThreshold;

    private float mKeyguardStatusBarAnimateAlpha = 1f;
    private int mOldLayoutDirection;
    private HeadsUpTouchHelper mHeadsUpTouchHelper;
    private boolean mIsExpansionFromHeadsUp;
    private boolean mListenForHeadsUp;
    private int mNavigationBarBottomHeight;
    private boolean mExpandingFromHeadsUp;
    private boolean mCollapsedOnDown;
    private int mPositionMinSideMargin;
    private int mLastOrientation = -1;
    private boolean mClosingWithAlphaFadeOut;
    private boolean mHeadsUpAnimatingAway;
    //*/ Added by droi hanhao for customized 2016-01-27
    private NotificationData mNotificationData;
    //*/
    
    //*/ Added by droi hanhao for show expandedCarrier & datausage 2016-01-19
    private TextView mDataUsage;
    private TextView mExpandedCarrier;
    //*/
    
  //*/added by Droi shijiachen 2015-7-24 for customize keyguard
    private DroiKeyguardManager mDroiKeyguardManager;
    private DroiKeyguardManager.LockscreenPackageInfo mLockscreenPackageInfo;
    private ViewGroup mKeyguardView;
    private KeyguardUpdateMonitorCallback mMonitorCallback = null;
    private KeyguardPanelViewCallback mKeyguardPanelViewCallback = null;
    private float mNotificationExpandHeight = 0;
    private LockPatternUtils mLockPatternUtils;
    private boolean  mIsKeyguardExist = false;
    private boolean mIsShowNotificationScrim = false;
    private boolean mIsShowNotification = true;
    private boolean mIsScreenOn = true;
    private ActivityStarter mActivityStarter;
    private ImageView mBlurImage;
    private ScrimController mScrimController;
    private AsyncTask<Void, Void, Bitmap> mBlurTask = null;
    private boolean  mIsNotificationTracking = false;
    private boolean  mAttemptToUnlock = false;
    private boolean  mCanInShadeLock = false;
    private boolean  mIsExpandNotificationScrolledToBottom = false;
    private boolean  mIsRegisterDistanceSensor = false;
    private SensorManager mSensorManager;
    private PowerManager mPm;
    private BlurRun mBlurRun;

    //*/ Added by droi hanhao for customized PowerSave 2016-01-27
    private boolean mSuperPowerSaveEnabled = false;
    //*/
    //*/freeme.zhangshaopiao,20170822,add swipe left and right action
    private boolean mIsAction = false;
    //*/

    /// M: For customize clock
    private ClockView mMtkClockView;

    /// M: A1 support
    private static boolean bA1Support =
            SystemProperties.get("ro.mtk_a1_feature").equals("1");

    private Runnable mHeadsUpExistenceChangedRunnable = new Runnable() {
        @Override
        public void run() {
            mHeadsUpAnimatingAway = false;
            notifyBarPanelExpansionChanged();
        }
    };

    /** Interpolator to be used for animations that respond directly to a touch */
    private final Interpolator mTouchResponseInterpolator =
            new PathInterpolator(0.3f, 0f, 0.1f, 1f);

    public NotificationPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(!DEBUG);
    }

    public void setStatusBar(PhoneStatusBar bar) {
        mStatusBar = bar;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHeader = (StatusBarHeaderView) findViewById(R.id.header);
        mHeader.setOnClickListener(this);
        mKeyguardStatusBar = (KeyguardStatusBarView) findViewById(R.id.keyguard_header);
        mKeyguardStatusView = (KeyguardStatusView) findViewById(R.id.keyguard_status_view);
        mQsContainer = (QSContainer) findViewById(R.id.quick_settings_container);
        mQsPanel = (QSPanel) findViewById(R.id.quick_settings_panel);

        if (bA1Support) {
            mClockView = (TextView) findViewById(R.id.clock_view);
        } else {
            mMtkClockView = (ClockView) findViewById(R.id.clock_view);
        }
        
        //*/ Added by droi hanhao for show extend carrier,data
        mExpandedCarrier = (TextView)findViewById(R.id.carrier_label);
        mDataUsage = (TextView)findViewById(R.id.data_use);
        //*/
        
        mScrollView = (ObservableScrollView) findViewById(R.id.scroll_view);
        mScrollView.setListener(this);
        mScrollView.setFocusable(false);
        mReserveNotificationSpace = findViewById(R.id.reserve_notification_space);
        mNotificationContainerParent = (NotificationsQuickSettingsContainer)
                findViewById(R.id.notification_container_parent);
        mNotificationStackScroller = (NotificationStackScrollLayout)
                findViewById(R.id.notification_stack_scroller);
        mNotificationStackScroller.setOnHeightChangedListener(this);
        mNotificationStackScroller.setOverscrollTopChangedListener(this);
        mNotificationStackScroller.setOnEmptySpaceClickListener(this);
        mNotificationStackScroller.setScrollView(mScrollView);
        mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(getContext(),
                android.R.interpolator.fast_out_slow_in);
        mFastOutLinearInterpolator = AnimationUtils.loadInterpolator(getContext(),
                android.R.interpolator.fast_out_linear_in);
        mDozeAnimationInterpolator = AnimationUtils.loadInterpolator(getContext(),
                android.R.interpolator.linear_out_slow_in);
        mKeyguardBottomArea = (KeyguardBottomAreaView) findViewById(R.id.keyguard_bottom_area);
        mQsNavbarScrim = findViewById(R.id.qs_navbar_scrim);
        mAfforanceHelper = new KeyguardAffordanceHelper(this, getContext());
        mSecureCameraLaunchManager =
                new SecureCameraLaunchManager(getContext(), mKeyguardBottomArea);
      //*/added by shijiachen 20150908 for distance unlock
        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        //*/
        mLastOrientation = getResources().getConfiguration().orientation;
		//*/Added by Droi shijiachen 2015-7-20 for  customize keyguard 
        mPm = (PowerManager)getContext().getSystemService(Context.POWER_SERVICE);
        mDroiKeyguardManager = new DroiKeyguardManager(getContext());
        mLockPatternUtils = mDroiKeyguardManager.getLockPatternUtils();
	    if(!mLockPatternUtils.isLockScreenDisabled(ActivityManager.getCurrentUser())&& isUserSetComplete()){
	    	prepareKeyguardView();	
	       	mStatusBarState = StatusBarState.KEYGUARD;
	     }else{
	    	 mStatusBarState = StatusBarState.SHADE;
	     }
		//*/
        // recompute internal state when qspanel height changes
        mQsContainer.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                final int height = bottom - top;
                final int oldHeight = oldBottom - oldTop;
                if (height != oldHeight) {
                    onScrollChanged();
                }
            }
        });

        mBlurFrame = (FrameLayout)findViewById(R.id.blur_background);
    }

    @Override
    protected void loadDimens() {
        super.loadDimens();
        mNotificationTopPadding = getResources().getDimensionPixelSize(
                R.dimen.notifications_top_padding);
        mFlingAnimationUtils = new FlingAnimationUtils(getContext(), 0.4f);
        mStatusBarMinHeight = getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.status_bar_height);
        mQsPeekHeight = getResources().getDimensionPixelSize(R.dimen.qs_peek_height);
        mNotificationsHeaderCollideDistance =
                getResources().getDimensionPixelSize(R.dimen.header_notifications_collide_distance);
        mUnlockMoveDistance = getResources().getDimensionPixelOffset(R.dimen.unlock_move_distance);
        mClockPositionAlgorithm.loadDimens(getResources());
        mNotificationScrimWaitDistance =
                getResources().getDimensionPixelSize(R.dimen.notification_scrim_wait_distance);
        mQsFalsingThreshold = getResources().getDimensionPixelSize(
                R.dimen.qs_falsing_threshold);
        mPositionMinSideMargin = getResources().getDimensionPixelSize(
                R.dimen.notification_panel_min_side_margin);
    }
  //*/ Added by droi hanhao for customized 2016-01-27
    public void setNotificationData(NotificationData data){
        mNotificationData = data;
    }
    //*/
    //*/ added by Droi shijiachen 2015-7-20 for custom keyguard
    public void setActivityStarter(ActivityStarter activityStarter) {
        mActivityStarter = activityStarter;
    }
	
	//*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
    public void clearKeyguardCallback(View v){
        if (v instanceof KeyguardSecurityView){
            ((KeyguardSecurityView)v).setKeyguardCallback(null);
        }else if(v instanceof ICustomLockscreenView){
            ((ICustomLockscreenView)v).setCallback(null);
        }
    }
	//*/
	
	/*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
    private KeyguardSecurityView prepareKeyguardView(){
	/*/
	private void prepareKeyguardView(){
	//*/
       Log.i(TAG, "prepareKeyguardView");
       mLockscreenPackageInfo = mDroiKeyguardManager.getLockscreenPackageInfo();
       Log.i(TAG, "layoutId:" +  mLockscreenPackageInfo.layoutId);
           if(mKeyguardView == null){
               Log.i(TAG, "mKeyguardView == null");
               mKeyguardView = new FrameLayout(getContext());
               mKeyguardView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        	addView(mKeyguardView,0);
    	}
        if(mLockscreenPackageInfo.layoutId > 0){
        	mIsDefaultLockscreen = false;	
               Log.i(TAG, "reset mKeyguardView");
               View old =  getKeyguardView();              
               /*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
               if(old != null){
                   old.setKeyguardCallback(null);
                   Log.i(TAG, "clean callback");
               }
			   /*/
			   clearKeyguardCallback(old);
			   //*/
            mKeyguardView.removeAllViews();
        	View v = null;
        	try {
            	v  = mLockscreenPackageInfo.inflate(mKeyguardView, false);	
            	if(mLockscreenPackageInfo.configShowKeyguardStatusInfo){
            		mKeyguardStatusView.setVisibility(View.VISIBLE);
                	if(mLockscreenPackageInfo.configShowClock){
                		mKeyguardStatusView.getClockView().setVisibility(View.VISIBLE);
                	}else{
                		mKeyguardStatusView.getClockView().setVisibility(View.GONE);
                	}
                	if(mLockscreenPackageInfo.configShowDate){
                		mKeyguardStatusView.getDateView().setVisibility(View.VISIBLE);
                	}else {
                		mKeyguardStatusView.getDateView().setVisibility(View.GONE);
    				}
                	if(mLockscreenPackageInfo.configShowAlarm){
                		mKeyguardStatusView.getAlarmView().setVisibility(View.VISIBLE);
                	}else {
                		mKeyguardStatusView.getAlarmView().setVisibility(View.GONE);
    				}
                	if(mLockscreenPackageInfo.configShowOwnerInfo){
                		mKeyguardStatusView.getOwnerView().setVisibility(View.VISIBLE);
                	}else {
                		mKeyguardStatusView.getOwnerView().setVisibility(View.GONE);
    				}
            	}else{
            		mKeyguardStatusView.setVisibility(View.GONE);
            	}
                //*/freeme.zhangshaopiao,20170814,modify IsUseDefault to ture for  swipe left and right action
            	mKeyguardBottomArea.setIsUseDefaultLockscreen(SystemProperties.get("ro.freeme.xlj_jingdong").equals("1") ? true:false);
            	/*/
            	mKeyguardBottomArea.setIsUseDefaultLockscreen(false);
            	//*/
			} catch (Exception e) {
				mKeyguardBottomArea.setVisibility(View.VISIBLE);
				Log.d("shijc", "prepareKeyguardView exception: " + e);
				e.printStackTrace();
				mDroiKeyguardManager.resetDefaultLockscreen();
				mLockscreenPackageInfo = mDroiKeyguardManager.getLockscreenPackageInfo();
				mKeyguardBottomArea.setIsUseDefaultLockscreen(true);
			}
        	
        	if(v != null){
        		        Log.d("shijc", "set keyguard success: " + v);
            			Log.d("shijc", "prepareKeyguardView getChildCount: " + mKeyguardView.getChildCount() );
				//*/	freeme, gouzhouping, 20161025, for custom keyguard new interface	
	            if (v instanceof KeyguardSecurityView) {
                    KeyguardSecurityView view = null;
			    //*/
                    try {
	                	view = (KeyguardSecurityView)(v);
	                	view.setKeyguardCallback(mKeyguardCallback);
	                	view.setLockPatternUtils(mLockPatternUtils);
	                	mMonitorCallback = view.getKeyguardUpdateMonitorCallback();
	                	if(mMonitorCallback != null){
	                        KeyguardUpdateMonitor.getInstance(getContext()).registerCallback(mMonitorCallback);	
	                	}
                        setDisableWindowHideFlag(mLockscreenPackageInfo.configUseWindowInKeyguard);
                    } catch (Exception e) {
                        mIsDefaultLockscreen = true;
                            Log.d("shijc", "prepareKeyguardView exception: " + e);
                            e.printStackTrace();
                    }
                //*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
                } else if (v instanceof ICustomLockscreenView) {
                    try {
                        ICustomLockscreenView  dlsv = (ICustomLockscreenView)(v);
                        dlsv.setCallback(mDroiLockscreenSecurityCallback);
                        mListener = dlsv.onGetUpdateMonitorCallback();
                        if(mListener != null){
                            registerUpdateMonitorCallback();
                        }
                        setDisableWindowHideFlag(mLockscreenPackageInfo.configUseWindowInKeyguard);
                    } catch (Exception e) {
                        mIsDefaultLockscreen = true;
                        e.printStackTrace();
                    }
                }
				//*/
                mKeyguardView.addView(v,0);
                mIsKeyguardExist = true;
            }else{
            	resetDefaultLockscreen();
            }
        }else{
        	resetDefaultLockscreen();
        }
        mIsShowNotificationScrim = false;
        if(mKeyguardPanelViewCallback != null){
        	mKeyguardPanelViewCallback.onLockscreenChanged(mIsDefaultLockscreen);
        }
        mClockPositionAlgorithm.setIsDefaultLockscreen(mIsDefaultLockscreen);
		/*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
        return view;
		/*/
    }

    //*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
    private void unRegisterUpdateMonitorCallback() {
        if(mKeyguardUpdateMonitorCallback !=null){
            KeyguardUpdateMonitor.getInstance(getContext()).removeCallback(mKeyguardUpdateMonitorCallback);
        }
    }

    private OnUpdateMonitorCallback mListener;
    private ICustomCallback mDroiLockscreenSecurityCallback = new ICustomCallback() {
        @Override
        public void unLock(boolean securityVerified) {
            if (mLockscreenPackageInfo != null && mLockscreenPackageInfo.configUseWindowInKeyguard) {
                setDisableWindowHideFlag(false);
            }
            if (!mIsNotificationTracking) {
                mAttemptToUnlock = true;
                if (mKeyguardView != null) {
                    mKeyguardView.setFocusable(false);
                }
                resetLockscreen();
                handleUnlock(securityVerified);
            }
        }

       @Override
       public void dismiss(boolean securityVerified){
       }

       @Override
       public void dismiss(boolean securityVerified,Intent intent){
            //*/ freeme.shanjibing, 20161222. modify for xiaoying
            if (mLockscreenPackageInfo != null && mLockscreenPackageInfo.configUseWindowInKeyguard) {
                setDisableWindowHideFlag(false);
            }
            if (!mIsNotificationTracking) {
                mAttemptToUnlock = true;
                if (mKeyguardView != null) {
                    mKeyguardView.setFocusable(false);
                }

                resetLockscreen();
                if (mDroiKeyguardManager.isCameraIntent(intent)) {
                    mDroiKeyguardManager.startCamera(mActivityStarter);
                    mStatusBar.startLaunchTransitionTimeout();
                } else if (mDroiKeyguardManager.isXiaoYing(intent)) {
                    Log.i("shanjibing","isXiaoYing...");
                    mActivityStarter.startActivity(intent, false);
                } else {
                    mActivityStarter.startActivity(intent, false);
                }
                
            }
            //*/
       }

       @Override
       public void dismiss(boolean securityVerified,Runnable runOnDismiss){
       }

       @Override
       public void userActivity(){
       }

       @Override
       public void reportUnlockAttempt(boolean success, int timeoutMs){
       }

       @Override
       public void reset(){
       }

       @Override
       public void showBackupSecurity(){
       }
    };

    private KeyguardUpdateMonitorCallback mKeyguardUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onRefreshBatteryInfo(BatteryStatus status) {
            super.onRefreshBatteryInfo(status);
            final boolean isChargingOrFull = status.status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status.status == BatteryManager.BATTERY_STATUS_FULL;
            final boolean powerPluggedIn = status.isPluggedIn()
                    && isChargingOrFull;
            final int level = status.level;
            if (mListener != null) {
                mListener.onRefreshBatteryInfo(isChargingOrFull, level,
                        powerPluggedIn);
            }
        };

        @Override
        public void onTimeChanged() {
            super.onTimeChanged();
            if (mListener != null) {
                mListener.onTimeChanged();
            }
        }

        @Override    
        public void onScreenTurnedOn() { 
            super.onScreenTurnedOn();
            if (mListener != null) {
                mListener.onScreenTurnedOn();
            }
        }

        @Override
        public void onScreenTurnedOff() { 
            super.onScreenTurnedOff();
            if (mListener != null) {
                mListener.onScreenTurnedOff();
            }
        }

        @Override
        public void onUnreadUpdate(int type, int count) {
            super.onUnreadUpdate(type, count); 
            if (mListener != null) {
                mListener.onUnreadUpdate(type,count);
            }
        }

    };
	
    private void registerUpdateMonitorCallback() {
        if(mKeyguardUpdateMonitorCallback != null){
            KeyguardUpdateMonitor.getInstance(getContext()).registerCallback(mKeyguardUpdateMonitorCallback);
        }
    }
    //*/
    public void notifyLockscreenReset(){
        View view = getKeyguardView();
        if (view != null && (view instanceof KeyguardSecurityView)){
            KeyguardSecurityView v = (KeyguardSecurityView)view;       
		    v. reset();
		}else if(view != null && (view instanceof ICustomLockscreenView)){
            Log.i("shanjibing","notifyLockscreenReset");
            ICustomLockscreenView v = (ICustomLockscreenView)view;       
            v. onReset();
        }
    }
	
	//*/added by shijiachen 20151026,check is user set up finished
    private boolean isUserSetComplete(){
    	return  Settings.Secure.getInt(getContext().getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 0) == 1;
    }
    //*/
    
    private void resetDefaultLockscreen(){
    	mKeyguardStatusView.setVisibility(View.VISIBLE);
    	mKeyguardBottomArea.setVisibility(View.VISIBLE);	
    	//*/freeme.gouzhouping, 20161025, for show time and date in default lockScreen when change from third lockscreen to default screen. 
    	mKeyguardStatusView.getClockView().setVisibility(View.VISIBLE);
    	mKeyguardStatusView.getDateView().setVisibility(View.VISIBLE);
    	mKeyguardStatusView.getAlarmView().setVisibility(View.VISIBLE);
    	mKeyguardStatusView.getOwnerView().setVisibility(View.VISIBLE);
    	//*/
    	mIsDefaultLockscreen = true;
    	mKeyguardBottomArea.setIsUseDefaultLockscreen(true);
    	android.provider.Settings.System.putInt(getContext().getContentResolver(), "disable_lockscreen_window_force_hide", 0);
    }
    
    public void setKeyguardPanelViewCallback(KeyguardPanelViewCallback cb){
    	mKeyguardPanelViewCallback = cb;
        //*/ Added by tyd hanhao for disable active_state(click twice to open a notification) in normal keyguard 2015-11-26 
        mKeyguardPanelViewCallback.onLockscreenChanged(mIsDefaultLockscreen);
        //*/
    }
    

    public void onBackPressed(){
    	Log.i("shijc", "onBackPressed in npv, state:" + mStatusBarState);
    	mAttemptToUnlock = false;
        //*/added by shijiachen, 20151109 for unlock keyguard with action & fingerprint
    	if(mStatusBarState == StatusBarState.KEYGUARD){
    		mIsNotificationTracking  = false;
    		Log.d("shijc", "mIsNotificationTracking is false");
    	}
    	if(mIsHeaderVisible){
//    		mIsNotificationTracking = false;
    	}else{
    		mKeyguardStatusBar.setAlpha(1f);
//    		mNotificationStackScroller.setNotificationVisible(View.GONE);	
    	}
    }

    private class BlurRun implements Runnable{
    	private boolean isForce;
    	
    	BlurRun(boolean isForce){
    		this.isForce = isForce;
    	}
    	
		@Override
		public void run() {
			createBlurImage(isForce);
		}
    }

   private  KeyguardSecurityCallback mKeyguardCallback = new KeyguardSecurityCallback(){
	   
       @Override
       public void userActivity() {
    	   if(mPm != null){
    		   mPm.userActivity(SystemClock.uptimeMillis(), false);   
    	   }
       }

      @Override
      public boolean isVerifyUnlockOnly() {
          return false;
      }
      
      @Override
      public void reportUnlockAttempt(boolean success, int timeoutMs) {
      }
      
      @Override
      public void showBackupSecurity() {
      }
      @Override
      public void reset() {
      }
      
      @Override
      public boolean hasOnDismissAction() {
          return false;
      }
      
      @Override
      public void setOnDismissAction(DroiOnDismissAction action) {
      }

       @Override
       public void dismiss(boolean securityVerified) {
	       //*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
	       if (mLockscreenPackageInfo != null && mLockscreenPackageInfo.configUseWindowInKeyguard) {
	           setDisableWindowHideFlag(false);
		   }
	       if (!mIsNotificationTracking) {
		   //*/
                mAttemptToUnlock = true;
                if (mKeyguardView != null) {
                    mKeyguardView.setFocusable(false);
                }
                resetLockscreen();
                handleUnlock(securityVerified);
            }
       }
       
       @Override
       public void dismiss(boolean securityVerified,Runnable run) {
    	   Log.i("shijc", "dismiss keyguard");
//    	   handleUnlock(false);
            if (mLockscreenPackageInfo != null && mLockscreenPackageInfo.configUseWindowInKeyguard) {
                setDisableWindowHideFlag(false);
            }
            if (!mIsNotificationTracking) {
                mAttemptToUnlock = true;
                if (mKeyguardView != null) {
                    mKeyguardView.setFocusable(false);
                }
                resetLockscreen();
                mActivityStarter.startRun(run, false);
            }
       }
       
       @Override
       public void dismiss(boolean securityVerified,Intent  intent) {
            Log.i("shijc", "dismiss keyguard intent" + intent.getAction());
            if (mLockscreenPackageInfo != null && mLockscreenPackageInfo.configUseWindowInKeyguard) {
                setDisableWindowHideFlag(false);
            }
            if (!mIsNotificationTracking) {
                mAttemptToUnlock = true;
                if (mKeyguardView != null) {
                    mKeyguardView.setFocusable(false);
                }

                resetLockscreen();
                if (mDroiKeyguardManager.isCameraIntent(intent)) {
                    mDroiKeyguardManager.startCamera(mActivityStarter);
                    // mSecureCameraLaunchManager.startSecureCameraLaunch();
                    mStatusBar.startLaunchTransitionTimeout();
                } else {
                    Log.i("shijc","phone");
                    mActivityStarter.startActivity(intent, false);
                }
                // handleUnlock(false);
            }
       }
    };
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        final float[] values = event.values;
        final int action = SensorManager.mapGesSensorDataToWindow((int) values[0]);
        switch (action) {
            case SensorManager.GESTURE_LEFT:
            case SensorManager.GESTURE_RIGHT:
            case SensorManager.GESTURE_UP:
            case SensorManager.GESTURE_DOWN:
            	Log.i("shijc", "onSensorChanged");
            	handleDistanceUnlock();
                break;
            default:
                break;
        }
    }
    
    private void handleDistanceUnlock() {
        mAttemptToUnlock = true;
        if (mKeyguardView != null) {
            mKeyguardView.setFocusable(false);
        }

        resetLockscreen();
        handleUnlock(false);
    }
    
    public void registerGestureSensorListener(){
    	Log.i("shijc", "registerGestureSensorListener");
    	boolean smartUnlock = getGestureSets();
        if(smartUnlock && !mIsRegisterDistanceSensor && mSensorManager != null){
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_GESTURE),
                    SensorManager.SENSOR_DELAY_NORMAL);
            mIsRegisterDistanceSensor = true;
            //gesture icon show
            Intent intent = new Intent("com.freeme.action.FLOAT_GESTURE");
            intent.putExtra("isFloatGestureOn", true);
            intent.putExtra("isFromKeyguard", true);
            getContext().sendBroadcast(intent);
        }
    }
    
    public void unRegisterGestureSensorListener(){
    	Log.i("shijc", "unRegisterGestureSensorListener");
        if(mIsRegisterDistanceSensor && mSensorManager != null){
            mSensorManager.unregisterListener(this);
            mIsRegisterDistanceSensor = false;
            //gesture icon remove
            Intent intent = new Intent("com.freeme.action.FLOAT_GESTURE");
            intent.putExtra("isFloatGestureOn", false);
            intent.putExtra("isFromKeyguard", true);
            getContext().sendBroadcast(intent);
        }
    }
    
    private boolean getGestureSets(){
    	return Settings.System.getBoolbit(getContext().getContentResolver(),
                Settings.System.FREEME_GESTURE_SETS,
                Settings.System.FREEME_GESTURE_LOCKSCR_UNLOCK,
                false);
    }
    
    public void setShowNotification(boolean isShow){
    	mIsShowNotification = isShow;
    }
    
    public void cleanUp(){
    	
    }
    
    public void setScrimController(ScrimController scrimController){
    	mScrimController = scrimController;
    }

    public void resetLockscreen(){
        if (mLockPatternUtils.isSecure(ActivityManager.getCurrentUser())) {
		    /*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
			KeyguardSecurityView v = getKeyguardView();
            Log.d("shijc", "KeyguardSecurityView reset " + (v != null));
            if (v != null) {
                v.reset();
            }
		    /*/
            View view = getKeyguardView();
            if (view != null && (view instanceof KeyguardSecurityView)){
                KeyguardSecurityView v = (KeyguardSecurityView)view;
                v.reset();
            }else if(view != null && (view instanceof ICustomLockscreenView)){
                ICustomLockscreenView v = (ICustomLockscreenView)view;
                v.onReset();
            }
			//*/
        }
    }
    
    public void notifyWallpaperChanged(Drawable drawable){
	    /*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
		KeyguardSecurityView v = getKeyguardView();
    	if(v != null){
    		Drawable wp = drawable == null ?mDroiKeyguardManager.requestWallpaper():drawable;
    		v.onWallpaperChanged(wp);
    	}
	    /*/
        View view = getKeyguardView();
        if (view != null && (view instanceof KeyguardSecurityView)){
            KeyguardSecurityView v = (KeyguardSecurityView)view;
			Drawable wp = drawable == null ?mDroiKeyguardManager.requestWallpaper():drawable;
			v.onWallpaperChanged(wp);
		}else if (view != null && (view instanceof ICustomLockscreenView)){
            ICustomLockscreenView v = (ICustomLockscreenView)view;
            Drawable wp = drawable == null ?mDroiKeyguardManager.requestWallpaper():drawable;
            v.onWallpaperChanged(wp);
        }
		//*/
    }
    
    private void handleUnlock(boolean auth){
    	if(!mLockPatternUtils.isSecure(ActivityManager.getCurrentUser())){
    		mKeyguardStatusView.setVisibility(View.GONE);
			//*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
            View view = getKeyguardView();
            if (view != null && (view instanceof KeyguardSecurityView)){
                KeyguardSecurityView v = (KeyguardSecurityView)view;
			//*/
    	 		v.onPause();
    	 		v.setKeyguardCallback(null);
    	 		if(mMonitorCallback != null){
    	 			KeyguardUpdateMonitor.getInstance(getContext()).removeCallback(mMonitorCallback);
    	 		}
    	 		mKeyguardView.removeAllViews();
    	 		Log.i("shijc", "keyguard unlock");
			//*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
    	 	}else if(view != null && (view instanceof ICustomLockscreenView)){
                ICustomLockscreenView v = (ICustomLockscreenView)view;
                v.onPause();
                v.setCallback(null);
                unRegisterUpdateMonitorCallback();
                if(mListener != null){
                    unRegisterUpdateMonitorCallback();
                }
                mKeyguardView.removeAllViews();
            }
			//*/
    		removeKeyguard();
    	}
    	mIsKeyguardExist = false;
 	   if(mKeyguardPanelViewCallback != null){
 		   mKeyguardPanelViewCallback.onDismiss(auth);
 	   }
    }
    private void removeKeyguard(){
         /*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
         View view = getKeyguardView();
            if (view != null && (view instanceof KeyguardSecurityView)){
                KeyguardSecurityView v = (KeyguardSecurityView)view;
               v.onPause();
               v.setKeyguardCallback(null);
               if(mMonitorCallback != null){
                   KeyguardUpdateMonitor.getInstance(getContext()).removeCallback(mMonitorCallback);
               }
               mKeyguardView.removeAllViews();
              Log.i("shijc", "removeKeyguard");
           }
         /*/
           View view = getKeyguardView();
            if (view != null && (view instanceof KeyguardSecurityView)){
                KeyguardSecurityView v = (KeyguardSecurityView)view;
                v.onPause();
                v.setKeyguardCallback(null);
                if(mMonitorCallback != null){
                    KeyguardUpdateMonitor.getInstance(getContext()).removeCallback(mMonitorCallback);
                }
                mKeyguardView.removeAllViews();
                Log.i("shijc", "keyguard unlock");
            }else if(view != null && (view instanceof ICustomLockscreenView)){
                ICustomLockscreenView v = (ICustomLockscreenView)view;
                v.onPause();
                v.setCallback(null);
                unRegisterUpdateMonitorCallback();
                if(mListener != null){
                    mListener = null;
                }
                mKeyguardView.removeAllViews();
            }
        mIsKeyguardExist = false;
        boolean resetMagicTrackMode = mDroiKeyguardManager.resetMagicTrackMode(getContext());
        Log.i("shijc", "resetMagicTrackMode = " + resetMagicTrackMode);
    }
    
    public void hideNotification(){
    	mNotificationStackScroller.setNotificationVisible(View.GONE);
    }
    
    public void setBlurBackground(boolean isBlur){
    	if(isBlur){
    		runBlur(true, false);
    	}else{
    		disableBlurBackground(false);
    	}
    }
	//*/

    public void updateResources() {
        int panelWidth = getResources().getDimensionPixelSize(R.dimen.notification_panel_width);
        int panelGravity = getResources().getInteger(R.integer.notification_panel_layout_gravity);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mHeader.getLayoutParams();
        if (lp.width != panelWidth) {
            lp.width = panelWidth;
            lp.gravity = panelGravity;
            mHeader.setLayoutParams(lp);
            mHeader.post(mUpdateHeader);
        }

        lp = (FrameLayout.LayoutParams) mNotificationStackScroller.getLayoutParams();
        if (lp.width != panelWidth) {
            lp.width = panelWidth;
            lp.gravity = panelGravity;
            mNotificationStackScroller.setLayoutParams(lp);
        }

        lp = (FrameLayout.LayoutParams) mScrollView.getLayoutParams();
        if (lp.width != panelWidth) {
            lp.width = panelWidth;
            lp.gravity = panelGravity;
            mScrollView.setLayoutParams(lp);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        
     // modify by shijiachen 2015-7-24  for customize keyguard
        if(mKeyguardStatusView.getVisibility() != View.GONE){ 
            if(!mIsHeaderVisible){
             // Update Clock Pivot
                mKeyguardStatusView.setPivotX(getWidth() / 2);
                /// M: We use "getHeight()" instead of "getTextSize()" since the ClockView
                /// is implemented by our own and does not have "getTextSize()".
                /// Our own ClockView will display AM/PM by default.
                if (bA1Support) {
                    mKeyguardStatusView.setPivotY(
                            (FONT_HEIGHT - CAP_HEIGHT) / 2048f * mClockView.getTextSize());
                } else {
                    mKeyguardStatusView.setPivotY(
                            (FONT_HEIGHT - CAP_HEIGHT) / 2048f * mMtkClockView.getHeight());
                }
            }else{
                mKeyguardStatusView.setVisibility(View.GONE);
            }
        
        }

        // Calculate quick setting heights.
        int oldMaxHeight = mQsMaxExpansionHeight;
        mQsMinExpansionHeight = mKeyguardShowing ? 0 : mHeader.getCollapsedHeight() + mQsPeekHeight;
        mQsMaxExpansionHeight = mHeader.getExpandedHeight() + mQsContainer.getDesiredHeight();
        positionClockAndNotifications();
        if (mQsExpanded && mQsFullyExpanded) {
            mQsExpansionHeight = mQsMaxExpansionHeight;
            requestScrollerTopPaddingUpdate(false /* animate */);
            requestPanelHeightUpdate();

            // Size has changed, start an animation.
            if (mQsMaxExpansionHeight != oldMaxHeight) {
                startQsSizeChangeAnimation(oldMaxHeight, mQsMaxExpansionHeight);
            }
        } else if (!mQsExpanded) {
            setQsExpansion(mQsMinExpansionHeight + mLastOverscroll);
        }
		
		         updateStackHeight(getExpandedHeight());
      
        updateHeader();
        mNotificationStackScroller.updateIsSmallScreen(
                mHeader.getCollapsedHeight() + mQsPeekHeight);

        // If we are running a size change animation, the animation takes care of the height of
        // the container. However, if we are not animating, we always need to make the QS container
        // the desired height so when closing the QS detail, it stays smaller after the size change
        // animation is finished but the detail view is still being animated away (this animation
        // takes longer than the size change animation).
        if (mQsSizeChangeAnimator == null) {
            mQsContainer.setHeightOverride(mQsContainer.getDesiredHeight());
        }
        updateMaxHeadsUpTranslation();
    }

    @Override
    public void onAttachedToWindow() {
        mSecureCameraLaunchManager.create();
    }

    @Override
    public void onDetachedFromWindow() {
        mSecureCameraLaunchManager.destroy();
    }

    private void startQsSizeChangeAnimation(int oldHeight, final int newHeight) {
        if (mQsSizeChangeAnimator != null) {
            oldHeight = (int) mQsSizeChangeAnimator.getAnimatedValue();
            mQsSizeChangeAnimator.cancel();
        }
        mQsSizeChangeAnimator = ValueAnimator.ofInt(oldHeight, newHeight);
        mQsSizeChangeAnimator.setDuration(300);
        mQsSizeChangeAnimator.setInterpolator(mFastOutSlowInInterpolator);
        mQsSizeChangeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                requestScrollerTopPaddingUpdate(false /* animate */);
                requestPanelHeightUpdate();
                int height = (int) mQsSizeChangeAnimator.getAnimatedValue();
                mQsContainer.setHeightOverride(height - mHeader.getExpandedHeight());
            }
        });
        mQsSizeChangeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mQsSizeChangeAnimator = null;
            }
        });
        mQsSizeChangeAnimator.start();
    }

    /**
     * Positions the clock and notifications dynamically depending on how many notifications are
     * showing.
     */
    private void positionClockAndNotifications() {
        boolean animate = mNotificationStackScroller.isAddOrRemoveAnimationPending();
        int stackScrollerPadding;
        //*/modified Droi  by shijiachen 20150721
        if(mIsDefaultLockscreen){
        	if (mStatusBarState != StatusBarState.KEYGUARD) {
                int bottom = mHeader.getCollapsedHeight();
                stackScrollerPadding = mStatusBarState == StatusBarState.SHADE
                        ? bottom + mQsPeekHeight + mNotificationTopPadding
                        : mKeyguardStatusBar.getHeight() + mNotificationTopPadding;
                mTopPaddingAdjustment = 0;
            } else {
                mClockPositionAlgorithm.setup(
                        mStatusBar.getMaxKeyguardNotifications(),
                        getMaxPanelHeight(),
                        getExpandedHeight(),
                        mNotificationStackScroller.getNotGoneChildCount(),
                        getHeight(),
                        mKeyguardStatusView.getHeight(),
                        mEmptyDragAmount);
                mClockPositionAlgorithm.run(mClockPositionResult);
                    if (animate || mClockAnimator != null) {
                        startClockAnimation(mClockPositionResult.clockY);
                    } else {
                        mKeyguardStatusView.setY(mClockPositionResult.clockY);
                    }
                    updateClock(mClockPositionResult.clockAlpha, mClockPositionResult.clockScale);
                    
                    stackScrollerPadding = mClockPositionResult.stackScrollerPadding;
                    mTopPaddingAdjustment = mClockPositionResult.stackScrollerPaddingAdjustment;
              	}
    		mNotificationStackScroller.setIntrinsicPadding(stackScrollerPadding);	
    		requestScrollerTopPaddingUpdate(animate);	
        }else{
            int bottom = mHeader.getCollapsedHeight();
            stackScrollerPadding = bottom + mQsPeekHeight + mNotificationTopPadding;
            mTopPaddingAdjustment = 0;
            if(mStatusBarState == StatusBarState.KEYGUARD){
            	if(mLockscreenPackageInfo.configShowStatusView){
                    mClockPositionAlgorithm.setup(
                            mStatusBar.getMaxKeyguardNotifications(),
                            getMaxPanelHeight(),
                            getExpandedHeight(),
                            mNotificationStackScroller.getNotGoneChildCount(),
                            getHeight(),
                            mKeyguardStatusView.getHeight(),
                            mEmptyDragAmount);
                    	mClockPositionAlgorithm.run(mClockPositionResult);
                        if (animate || mClockAnimator != null) {
                            startClockAnimation(mClockPositionResult.clockY);
                        } else {
                            mKeyguardStatusView.setY(mClockPositionResult.clockY);
                        }
                        updateClock(mClockPositionResult.clockAlpha, mClockPositionResult.clockScale);
//                        stackScrollerPadding = mClockPositionResult.stackScrollerPadding;
            	}
            }
            if(mStatusBarState != StatusBarState.SHADE){
            	mNotificationStackScroller.setIntrinsicPadding(mKeyguardStatusBar.getHeight());	
            }else{
            	mNotificationStackScroller.setIntrinsicPadding(stackScrollerPadding);	
        		requestScrollerTopPaddingUpdate(animate);
            }
    			
//    		requestScrollerTopPaddingUpdate(animate);	
        }
    }

    private void startClockAnimation(int y) {
        if (mClockAnimationTarget == y) {
            return;
        }
        mClockAnimationTarget = y;
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);
                if (mClockAnimator != null) {
                    mClockAnimator.removeAllListeners();
                    mClockAnimator.cancel();
                }
                mClockAnimator = ObjectAnimator
                        .ofFloat(mKeyguardStatusView, View.Y, mClockAnimationTarget);
                mClockAnimator.setInterpolator(mFastOutSlowInInterpolator);
                mClockAnimator.setDuration(StackStateAnimator.ANIMATION_DURATION_STANDARD);
                mClockAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mClockAnimator = null;
                        mClockAnimationTarget = -1;
                    }
                });
                mClockAnimator.start();
                return true;
            }
        });
    }

    private void updateClock(float alpha, float scale) {
        if (!mKeyguardStatusViewAnimating) {
            mKeyguardStatusView.setAlpha(alpha);
        }
        mKeyguardStatusView.setScaleX(scale);
        mKeyguardStatusView.setScaleY(scale);
    }

    public void animateToFullShade(long delay) {
        mAnimateNextTopPaddingChange = true;
        mNotificationStackScroller.goToFullShade(delay);
        requestLayout();
    }

    public void setQsExpansionEnabled(boolean qsExpansionEnabled) {
        mQsExpansionEnabled = qsExpansionEnabled;
        mHeader.setClickable(qsExpansionEnabled);
        //*/ Added by tyd hanhao for disable QSExpansion, when in SuperPowerSavingMode 2015-08-29
        mHeader.updateClickable(qsExpansionEnabled);
        //*/
    }

    //*/ Added by tyd hanhao for disable QSExpansion, when in SuperPowerSavingMode 2015-08-29
    public void setInSuperPowerSaveMode(boolean modeOpen) {
    
        mSuperPowerSaveEnabled = modeOpen;
        setQsExpansionEnabled(!modeOpen);
    }
    //*/

    @Override
    public void resetViews() {
        mHasBlured = false;
        mIsLaunchTransitionFinished = false;
        mBlockTouches = false;
        mUnlockIconActive = false;
        mAfforanceHelper.reset(true);
        closeQs();
        mStatusBar.dismissPopups();
        mNotificationStackScroller.setOverScrollAmount(0f, true /* onTop */, false /* animate */,
                true /* cancelAnimators */);
        mNotificationStackScroller.resetScrollPosition();
        //*/ Added by droi hanhao 2016-02-24
        setBackgroundColor(getResources().getColor(com.android.internal.R.color.transparent));
        //*/
    }

    public void closeQs() {
        cancelQsAnimation();
        setQsExpansion(mQsMinExpansionHeight);
    }

    public void animateCloseQs() {
        if (mQsExpansionAnimator != null) {
            if (!mQsAnimatorExpand) {
                return;
            }
            float height = mQsExpansionHeight;
            mQsExpansionAnimator.cancel();
            setQsExpansion(height);
        }
        flingSettings(0 /* vel */, false);
    }

    public void openQs() {
        cancelQsAnimation();
        if (mQsExpansionEnabled) {
            setQsExpansion(mQsMaxExpansionHeight);
        }
    }

    public void expandWithQs() {
        if (mQsExpansionEnabled) {
            mQsExpandImmediate = true;
        }
        expand();
    }

    @Override
    public void fling(float vel, boolean expand) {
        GestureRecorder gr = ((PhoneStatusBarView) mBar).mBar.getGestureRecorder();
        if (gr != null) {
            gr.tag("fling " + ((vel > 0) ? "open" : "closed"), "notifications,v=" + vel);
        }
        super.fling(vel, expand);
    }

    @Override
    protected void flingToHeight(float vel, boolean expand, float target,
            float collapseSpeedUpFactor, boolean expandBecauseOfFalsing) {
        mHeadsUpTouchHelper.notifyFling(!expand);
        setClosingWithAlphaFadeout(!expand && getFadeoutAlpha() == 1.0f);
        super.flingToHeight(vel, expand, target, collapseSpeedUpFactor, expandBecauseOfFalsing);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.getText().add(getKeyguardOrLockScreenString());
            mLastAnnouncementWasQuickSettings = false;
            return true;
        }
        return super.dispatchPopulateAccessibilityEventInternal(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mBlockTouches) {
            return false;
        }
        initDownStates(event);
        if (mHeadsUpTouchHelper.onInterceptTouchEvent(event)) {
            mIsExpansionFromHeadsUp = true;
            MetricsLogger.count(mContext, COUNTER_PANEL_OPEN, 1);
            MetricsLogger.count(mContext, COUNTER_PANEL_OPEN_PEEK, 1);
            return true;
        }
        if (!isFullyCollapsed() && onQsIntercept(event)) {
            return true;
        }
        return super.onInterceptTouchEvent(event);
    }

    private boolean onQsIntercept(MotionEvent event) {
        int pointerIndex = event.findPointerIndex(mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            mTrackingPointer = event.getPointerId(pointerIndex);
        }
        final float x = event.getX(pointerIndex);
        final float y = event.getY(pointerIndex);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mIntercepting = true;
                mInitialTouchY = y;
                mInitialTouchX = x;
                initVelocityTracker();
                trackMovement(event);
                if (shouldQuickSettingsIntercept(mInitialTouchX, mInitialTouchY, 0)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (mQsExpansionAnimator != null) {
                    onQsExpansionStarted();
                    mInitialHeightOnTouch = mQsExpansionHeight;
                    mQsTracking = true;
                    mIntercepting = false;
                    mNotificationStackScroller.removeLongPressCallback();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                final int upPointer = event.getPointerId(event.getActionIndex());
                if (mTrackingPointer == upPointer) {
                    // gesture is ongoing, find a new pointer to track
                    final int newIndex = event.getPointerId(0) != upPointer ? 0 : 1;
                    mTrackingPointer = event.getPointerId(newIndex);
                    mInitialTouchX = event.getX(newIndex);
                    mInitialTouchY = event.getY(newIndex);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                final float h = y - mInitialTouchY;
                trackMovement(event);
                if (mQsTracking) {

                    // Already tracking because onOverscrolled was called. We need to update here
                    // so we don't stop for a frame until the next touch event gets handled in
                    // onTouchEvent.
                    setQsExpansion(h + mInitialHeightOnTouch);
                    trackMovement(event);
                    mIntercepting = false;
                    return true;
                }
                if (Math.abs(h) > mTouchSlop && Math.abs(h) > Math.abs(x - mInitialTouchX)
                        && shouldQuickSettingsIntercept(mInitialTouchX, mInitialTouchY, h)) {
                    mQsTracking = true;
                    onQsExpansionStarted();
                    notifyExpandingFinished();
                    mInitialHeightOnTouch = mQsExpansionHeight;
                    mInitialTouchY = y;
                    mInitialTouchX = x;
                    mIntercepting = false;
                    mNotificationStackScroller.removeLongPressCallback();
                    return true;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                trackMovement(event);
                if (mQsTracking) {
                    flingQsWithCurrentVelocity(y,
                            event.getActionMasked() == MotionEvent.ACTION_CANCEL);
                    mQsTracking = false;
                }
                mIntercepting = false;
                break;
        }
        return false;
    }

    @Override
    protected boolean isInContentBounds(float x, float y) {
        float stackScrollerX = mNotificationStackScroller.getX();
        return !mNotificationStackScroller.isBelowLastNotification(x - stackScrollerX, y)
                && stackScrollerX < x && x < stackScrollerX + mNotificationStackScroller.getWidth();
    }

    private void initDownStates(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mOnlyAffordanceInThisMotion = false;
            mQsTouchAboveFalsingThreshold = mQsFullyExpanded;
            mDozingOnDown = isDozing();
            mCollapsedOnDown = isFullyCollapsed();
            mListenForHeadsUp = mCollapsedOnDown && mHeadsUpManager.hasPinnedHeadsUp();
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        // Block request when interacting with the scroll view so we can still intercept the
        // scrolling when QS is expanded.
        if (mScrollView.isHandlingTouchEvent()) {
            return;
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    private void flingQsWithCurrentVelocity(float y, boolean isCancelMotionEvent) {
        float vel = getCurrentVelocity();
        final boolean expandsQs = flingExpandsQs(vel);
        if (expandsQs) {
            logQsSwipeDown(y);
        }
        flingSettings(vel, expandsQs && !isCancelMotionEvent);
    }

    private void logQsSwipeDown(float y) {
        float vel = getCurrentVelocity();
        final int gesture = mStatusBarState == StatusBarState.KEYGUARD
                ? EventLogConstants.SYSUI_LOCKSCREEN_GESTURE_SWIPE_DOWN_QS
                : EventLogConstants.SYSUI_SHADE_GESTURE_SWIPE_DOWN_QS;
        EventLogTags.writeSysuiLockscreenGesture(
                gesture,
                (int) ((y - mInitialTouchY) / mStatusBar.getDisplayDensity()),
                (int) (vel / mStatusBar.getDisplayDensity()));
    }

    private boolean flingExpandsQs(float vel) {
        if (isBelowFalsingThreshold()) {
            return false;
        }
        if (Math.abs(vel) < mFlingAnimationUtils.getMinVelocityPxPerSecond()) {
            return getQsExpansionFraction() > 0.5f;
        } else {
            return vel > 0;
        }
    }

    private boolean isBelowFalsingThreshold() {
        return !mQsTouchAboveFalsingThreshold && mStatusBarState == StatusBarState.KEYGUARD;
    }

    private float getQsExpansionFraction() {
        return Math.min(1f, (mQsExpansionHeight - mQsMinExpansionHeight)
                / (getTempQsMaxExpansion() - mQsMinExpansionHeight));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //*/freeme.zhangshaopiao,20170822,add swipe left and right action
        if(event.getAction() == MotionEvent.ACTION_DOWN) mIsAction = false;
        //*/
        if (mBlockTouches) {
            return false;
        }
        initDownStates(event);
        if (mListenForHeadsUp && !mHeadsUpTouchHelper.isTrackingHeadsUp()
                && mHeadsUpTouchHelper.onInterceptTouchEvent(event)) {
            mIsExpansionFromHeadsUp = true;
            MetricsLogger.count(mContext, COUNTER_PANEL_OPEN_PEEK, 1);
        }
        if ((!mIsExpanding || mHintAnimationRunning)
                && !mQsExpanded
                && mStatusBar.getBarState() != StatusBarState.SHADE) {
            mAfforanceHelper.onTouchEvent(event);
        }
        //*/freeme.zhangshaopiao,20170814,add swipe left and right action
        if(SystemProperties.get("ro.freeme.xlj_jingdong").equals("1") && !mIsAction && getKeyguardView() != null && getKeyguardView() instanceof KeyguardSecurityView){
            getKeyguardView().onTouchEvent(event);
        }
        //*/
        if (mOnlyAffordanceInThisMotion) {
            return true;
        }
        mHeadsUpTouchHelper.onTouchEvent(event);
        if (!mHeadsUpTouchHelper.isTrackingHeadsUp() && handleQsTouch(event)) {
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && isFullyCollapsed()) {
            MetricsLogger.count(mContext, COUNTER_PANEL_OPEN, 1);
            updateVerticalPanelPosition(event.getX());
        }
        super.onTouchEvent(event);
        return true;
    }

    private boolean handleQsTouch(MotionEvent event) {
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN && getExpandedFraction() == 1f
                && mStatusBar.getBarState() != StatusBarState.KEYGUARD && !mQsExpanded
                && mQsExpansionEnabled) {

            // Down in the empty area while fully expanded - go to QS.
            mQsTracking = true;
            mConflictingQsExpansionGesture = true;
            onQsExpansionStarted();
            mInitialHeightOnTouch = mQsExpansionHeight;
            mInitialTouchY = event.getX();
            mInitialTouchX = event.getY();
        }
        if (!isFullyCollapsed()) {
            handleQsDown(event);
        }
        if (!mQsExpandImmediate && mQsTracking) {
            onQsTouch(event);
            if (!mConflictingQsExpansionGesture) {
                return true;
            }
        }
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mConflictingQsExpansionGesture = false;
        }
        if (action == MotionEvent.ACTION_DOWN && isFullyCollapsed()
                && mQsExpansionEnabled) {
            mTwoFingerQsExpandPossible = true;
        }
        if (mTwoFingerQsExpandPossible && isOpenQsEvent(event)
                /*/ freeme, gouzhouping, 20161129, for remove statusbar slide when click it and power off.
                && event.getY(event.getActionIndex()) < mStatusBarMinHeight) {
                /*/
                && event.getY(event.getActionIndex()) > mStatusBarMinHeight && !mKeyguardShowing ) {
                //*/
            MetricsLogger.count(mContext, COUNTER_PANEL_OPEN_QS, 1);
            mQsExpandImmediate = true;
            requestPanelHeightUpdate();

            // Normally, we start listening when the panel is expanded, but here we need to start
            // earlier so the state is already up to date when dragging down.
            setListening(true);
        }
        return false;
    }

    private boolean isInQsArea(float x, float y) {
        //*/ Modified by Droi shijiachen 20150730 for gesture when the notification is shown
        boolean ret = false;
        if(mIsDefaultLockscreen){
            ret = (x >= mScrollView.getX() && x <= mScrollView.getX() + mScrollView.getWidth()) &&
                    (y <= mNotificationStackScroller.getBottomMostNotificationBottom()
                    || y <= mQsContainer.getY() + mQsContainer.getHeight());
        }else{
            ret = true;
        }
        return ret;
        //*/
    }

    private boolean isOpenQsEvent(MotionEvent event) {
        final int pointerCount = event.getPointerCount();
        final int action = event.getActionMasked();

        final boolean twoFingerDrag = action == MotionEvent.ACTION_POINTER_DOWN
                && pointerCount == 2;

        final boolean stylusButtonClickDrag = action == MotionEvent.ACTION_DOWN
                && (event.isButtonPressed(MotionEvent.BUTTON_STYLUS_PRIMARY)
                        || event.isButtonPressed(MotionEvent.BUTTON_STYLUS_SECONDARY));

        final boolean mouseButtonClickDrag = action == MotionEvent.ACTION_DOWN
                && (event.isButtonPressed(MotionEvent.BUTTON_SECONDARY)
                        || event.isButtonPressed(MotionEvent.BUTTON_TERTIARY));

        //*/ Modified by droi hanhao for customized force expanded when drag from top, 2016-01-08
        return mForceExpandedQS || twoFingerDrag || stylusButtonClickDrag || mouseButtonClickDrag;
        //*/
    }

    private void handleQsDown(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                && shouldQuickSettingsIntercept(event.getX(), event.getY(), -1)) {
            mQsTracking = true;
            onQsExpansionStarted();
            mInitialHeightOnTouch = mQsExpansionHeight;
            mInitialTouchY = event.getX();
            mInitialTouchX = event.getY();

            // If we interrupt an expansion gesture here, make sure to update the state correctly.
            notifyExpandingFinished();
        }
    }

    @Override
    protected boolean flingExpands(float vel, float vectorVel, float x, float y) {
        boolean expands = super.flingExpands(vel, vectorVel, x, y);

        // If we are already running a QS expansion, make sure that we keep the panel open.
        if (mQsExpansionAnimator != null) {
            expands = true;
        }
        return expands;
    }

    @Override
    protected boolean hasConflictingGestures() {
        return mStatusBar.getBarState() != StatusBarState.SHADE;
    }

    @Override
    protected boolean shouldGestureIgnoreXTouchSlop(float x, float y) {
        return !mAfforanceHelper.isOnAffordanceIcon(x, y);
    }

    private void onQsTouch(MotionEvent event) {
        int pointerIndex = event.findPointerIndex(mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            mTrackingPointer = event.getPointerId(pointerIndex);
        }
        final float y = event.getY(pointerIndex);
        final float x = event.getX(pointerIndex);
        final float h = y - mInitialTouchY;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mQsTracking = true;
                mInitialTouchY = y;
                mInitialTouchX = x;
                onQsExpansionStarted();
                mInitialHeightOnTouch = mQsExpansionHeight;
                initVelocityTracker();
                trackMovement(event);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                final int upPointer = event.getPointerId(event.getActionIndex());
                if (mTrackingPointer == upPointer) {
                    // gesture is ongoing, find a new pointer to track
                    final int newIndex = event.getPointerId(0) != upPointer ? 0 : 1;
                    final float newY = event.getY(newIndex);
                    final float newX = event.getX(newIndex);
                    mTrackingPointer = event.getPointerId(newIndex);
                    mInitialHeightOnTouch = mQsExpansionHeight;
                    mInitialTouchY = newY;
                    mInitialTouchX = newX;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                setQsExpansion(h + mInitialHeightOnTouch);
                if (h >= getFalsingThreshold()) {
                    mQsTouchAboveFalsingThreshold = true;
                }
                trackMovement(event);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mQsTracking = false;
                mTrackingPointer = -1;
                trackMovement(event);
                float fraction = getQsExpansionFraction();
                if ((fraction != 0f || y >= mInitialTouchY)
                        && (fraction != 1f || y <= mInitialTouchY)) {
                    flingQsWithCurrentVelocity(y,
                            event.getActionMasked() == MotionEvent.ACTION_CANCEL);
                } else {
                    logQsSwipeDown(y);
                    mScrollYOverride = -1;
                }
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
        }
    }

    private int getFalsingThreshold() {
        float factor = mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f;
        return (int) (mQsFalsingThreshold * factor);
    }

    @Override
    public void onOverscrolled(float lastTouchX, float lastTouchY, int amount) {
        if (mIntercepting && shouldQuickSettingsIntercept(lastTouchX, lastTouchY,
                -1 /* yDiff: Not relevant here */)) {
            mQsTracking = true;
            onQsExpansionStarted(amount);
            mInitialHeightOnTouch = mQsExpansionHeight;
            mInitialTouchY = mLastTouchY;
            mInitialTouchX = mLastTouchX;
        }
    }

    @Override
    public void onOverscrollTopChanged(float amount, boolean isRubberbanded) {
        cancelQsAnimation();
        if (!mQsExpansionEnabled) {
            amount = 0f;
        }
        float rounded = amount >= 1f ? amount : 0f;
        mStackScrollerOverscrolling = rounded != 0f && isRubberbanded;
        mQsExpansionFromOverscroll = rounded != 0f;
        mLastOverscroll = rounded;
        updateQsState();
        setQsExpansion(mQsMinExpansionHeight + rounded);
    }

    @Override
    public void flingTopOverscroll(float velocity, boolean open) {
        mLastOverscroll = 0f;
        setQsExpansion(mQsExpansionHeight);
        flingSettings(!mQsExpansionEnabled && open ? 0f : velocity, open && mQsExpansionEnabled,
                new Runnable() {
                    @Override
                    public void run() {
                        mStackScrollerOverscrolling = false;
                        mQsExpansionFromOverscroll = false;
                        updateQsState();
                    }
                }, false /* isClick */);
    }

    private void onQsExpansionStarted() {
        onQsExpansionStarted(0);
    }

    private void onQsExpansionStarted(int overscrollAmount) {
        cancelQsAnimation();
        cancelHeightAnimator();

        // Reset scroll position and apply that position to the expanded height.
        float height = mQsExpansionHeight - mScrollView.getScrollY() - overscrollAmount;
        if (mScrollView.getScrollY() != 0) {
            mScrollYOverride = mScrollView.getScrollY();
        }
        mScrollView.scrollTo(0, 0);
        setQsExpansion(height);
        requestPanelHeightUpdate();
    }

    private void setQsExpanded(boolean expanded) {
        boolean changed = mQsExpanded != expanded;
        if (changed) {
            mQsExpanded = expanded;
            updateQsState();
            requestPanelHeightUpdate();
            mNotificationStackScroller.setInterceptDelegateEnabled(expanded);
            mStatusBar.setQsExpanded(expanded);
            mQsPanel.setExpanded(expanded);
            mNotificationContainerParent.setQsExpanded(expanded);
        }
    }
    

    public void setBarState(int statusBarState, boolean keyguardFadingAway,
            boolean goingToFullShade) {
        int oldState = mStatusBarState;
        boolean keyguardShowing = statusBarState == StatusBarState.KEYGUARD;
        //*/ add by Droi  shijiachen 2015-07-24 for click notification unlock;
        Log.d(TAG, "setBarState  statusBarState:"+statusBarState+",keyguardFadingAway:"+keyguardFadingAway+",goingToFullShade:"+goingToFullShade);
        mLastKeyguardStatus = mStatusBarState;
        if (statusBarState ==  StatusBarState.SHADE){
            mIsNotificationTracking = false;
            removeKeyguard();

            if (mNotificationStackScroller != null){
                mNotificationStackScroller.setDismissViewVisible(View.VISIBLE);
            }
            mKeyguardStatusView.setVisibility(View.GONE);

            //*/added by shijiachen 20150908 for distance unlock
            unRegisterGestureSensorListener();
            //*/

            mBlurFrame.setVisibility(View.VISIBLE);

            if(keyguardFadingAway){
            }else{
                if(mScrimController != null){
                    mScrimController.setEnable(true);
                }
            }
     	    if(mBlurTask != null){
     		    mBlurTask.cancel(true);
     	    }
          
     	    if (mBlurImage != null&&mKeyguardView != null){
     		    mBlurImage.setBackground(null);
     		    mKeyguardView.removeView(mBlurImage);
     	    }
		    setDisableWindowHideFlag(false);
        }else{
            Log.d("shijc","keyguard mIsKeyguardExist:" + mIsKeyguardExist);
            if(statusBarState == StatusBarState.KEYGUARD){
            	mIsNotificationTracking = false;
                if(mBlurFrame != null){
                	mBlurFrame.setBackgroundResource(0);
                }
                mBlurFrame.setVisibility(View.GONE);
                mIsShowNotificationScrim = false;
                setBackgroundColor(getResources().getColor(com.android.internal.R.color.transparent));
                if(getKeyguardView() == null && !mLockPatternUtils.isLockScreenDisabled(ActivityManager.getCurrentUser())){
                    prepareKeyguardView();
                }
                if(!mIsDefaultLockscreen){
                    mNotificationStackScroller.setNotificationVisible(View.GONE);
                }
            }else{ 
                mIsNotificationTracking = true;
            }
        }
        //*/
        
        //*/ modified by  Droi shijiachen 2015-07-24 for cutstomize keyguard
        
       //setKeyguardStatusViewVisibility(statusBarState, keyguardFadingAway, goingToFullShade);
       // setKeyguardBottomAreaVisibility(statusBarState, goingToFullShade);
      	if(mIsDefaultLockscreen || mLockscreenPackageInfo.configShowKeyguardStatusInfo){
            // &&(mKeyguardStatusView.getClockView().getVisibility() != View.GONE || mKeyguardStatusView.getDateView().getVisibility() != View.GONE)){
            setKeyguardStatusViewVisibility(statusBarState, keyguardFadingAway, goingToFullShade);  
        }
            setKeyguardBottomAreaVisibility(statusBarState, goingToFullShade);  
        //*/

        mStatusBarState = statusBarState;
        mKeyguardShowing = keyguardShowing;

        if (goingToFullShade || (oldState == StatusBarState.KEYGUARD
                && statusBarState == StatusBarState.SHADE_LOCKED)) {
            animateKeyguardStatusBarOut();
            animateHeaderSlidingIn();
        } else if (oldState == StatusBarState.SHADE_LOCKED
                && statusBarState == StatusBarState.KEYGUARD) {
            animateKeyguardStatusBarIn(StackStateAnimator.ANIMATION_DURATION_STANDARD);
            animateHeaderSlidingOut();
        } else {
            mKeyguardStatusBar.setAlpha(1f);
            mKeyguardStatusBar.setVisibility(keyguardShowing ? View.VISIBLE : View.INVISIBLE);
            if (keyguardShowing && oldState != mStatusBarState) {
                mKeyguardBottomArea.updateLeftAffordance();
                mAfforanceHelper.updatePreviews();
            }
        }
        if (keyguardShowing) {
            updateDozingVisibilities(false /* animate */);
        }
        resetVerticalPanelPosition();
        updateQsState();
    }

    private final Runnable mAnimateKeyguardStatusViewInvisibleEndRunnable = new Runnable() {
        @Override
        public void run() {
            mKeyguardStatusViewAnimating = false;
            mKeyguardStatusView.setVisibility(View.GONE);
        }
    };

    private final Runnable mAnimateKeyguardStatusViewVisibleEndRunnable = new Runnable() {
        @Override
        public void run() {
            mKeyguardStatusViewAnimating = false;
        }
    };

    private final Animator.AnimatorListener mAnimateHeaderSlidingInListener
            = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mHeaderAnimating = false;
            mQsContainerAnimator = null;
            mQsContainer.removeOnLayoutChangeListener(mQsContainerAnimatorUpdater);
        }
    };

    private final OnLayoutChangeListener mQsContainerAnimatorUpdater
            = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                int oldTop, int oldRight, int oldBottom) {
            int oldHeight = oldBottom - oldTop;
            int height = bottom - top;
            if (height != oldHeight && mQsContainerAnimator != null) {
                PropertyValuesHolder[] values = mQsContainerAnimator.getValues();
                float newEndValue = mHeader.getCollapsedHeight() + mQsPeekHeight - height - top;
                float newStartValue = -height - top;
                values[0].setFloatValues(newStartValue, newEndValue);
                mQsContainerAnimator.setCurrentPlayTime(mQsContainerAnimator.getCurrentPlayTime());
            }
        }
    };

    private final ViewTreeObserver.OnPreDrawListener mStartHeaderSlidingIn
            = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            getViewTreeObserver().removeOnPreDrawListener(this);
            long delay = mStatusBarState == StatusBarState.SHADE_LOCKED
                    ? 0
                    : mStatusBar.calculateGoingToFullShadeDelay();
            mHeader.setTranslationY(-mHeader.getCollapsedHeight() - mQsPeekHeight);
            mHeader.animate()
                    .translationY(0f)
                    .setStartDelay(delay)
                    .setDuration(StackStateAnimator.ANIMATION_DURATION_GO_TO_FULL_SHADE)
                    .setInterpolator(mFastOutSlowInInterpolator)
                    .start();
            mQsContainer.setY(-mQsContainer.getHeight());
            mQsContainerAnimator = ObjectAnimator.ofFloat(mQsContainer, View.TRANSLATION_Y,
                    mQsContainer.getTranslationY(),
                    mHeader.getCollapsedHeight() + mQsPeekHeight - mQsContainer.getHeight()
                            - mQsContainer.getTop());
            mQsContainerAnimator.setStartDelay(delay);
            mQsContainerAnimator.setDuration(StackStateAnimator.ANIMATION_DURATION_GO_TO_FULL_SHADE);
            mQsContainerAnimator.setInterpolator(mFastOutSlowInInterpolator);
            mQsContainerAnimator.addListener(mAnimateHeaderSlidingInListener);
            mQsContainerAnimator.start();
            mQsContainer.addOnLayoutChangeListener(mQsContainerAnimatorUpdater);
            return true;
        }
    };

    private void animateHeaderSlidingIn() {
        // If the QS is already expanded we don't need to slide in the header as it's already
        // visible.
        if (!mQsExpanded) {
            mHeaderAnimating = true;
            getViewTreeObserver().addOnPreDrawListener(mStartHeaderSlidingIn);
        }
    }

    private void animateHeaderSlidingOut() {
        mHeaderAnimating = true;
        mHeader.animate().y(-mHeader.getHeight())
                .setStartDelay(0)
                .setDuration(StackStateAnimator.ANIMATION_DURATION_STANDARD)
                .setInterpolator(mFastOutSlowInInterpolator)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mHeader.animate().setListener(null);
                        mHeaderAnimating = false;
                        updateQsState();
                    }
                })
                .start();
        mQsContainer.animate()
                .y(-mQsContainer.getHeight())
                .setStartDelay(0)
                .setDuration(StackStateAnimator.ANIMATION_DURATION_STANDARD)
                .setInterpolator(mFastOutSlowInInterpolator)
                .start();
    }

    private final Runnable mAnimateKeyguardStatusBarInvisibleEndRunnable = new Runnable() {
        @Override
        public void run() {
            mKeyguardStatusBar.setVisibility(View.INVISIBLE);
            mKeyguardStatusBar.setAlpha(1f);
            mKeyguardStatusBarAnimateAlpha = 1f;
        }
    };

    private void animateKeyguardStatusBarOut() {
        ValueAnimator anim = ValueAnimator.ofFloat(mKeyguardStatusBar.getAlpha(), 0f);
        anim.addUpdateListener(mStatusBarAnimateAlphaListener);
        anim.setStartDelay(mStatusBar.isKeyguardFadingAway()
                ? mStatusBar.getKeyguardFadingAwayDelay()
                : 0);
        anim.setDuration(mStatusBar.isKeyguardFadingAway()
                ? mStatusBar.getKeyguardFadingAwayDuration() / 2
                : StackStateAnimator.ANIMATION_DURATION_STANDARD);
        anim.setInterpolator(mDozeAnimationInterpolator);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimateKeyguardStatusBarInvisibleEndRunnable.run();
            }
        });
        anim.start();
    }

    private final ValueAnimator.AnimatorUpdateListener mStatusBarAnimateAlphaListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mKeyguardStatusBarAnimateAlpha = (float) animation.getAnimatedValue();
            updateHeaderKeyguardAlpha();
        }
    };

    private void animateKeyguardStatusBarIn(long duration) {
        mKeyguardStatusBar.setVisibility(View.VISIBLE);
        mKeyguardStatusBar.setAlpha(0f);
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.addUpdateListener(mStatusBarAnimateAlphaListener);
        anim.setDuration(duration);
        anim.setInterpolator(mDozeAnimationInterpolator);
        anim.start();
    }

    private final Runnable mAnimateKeyguardBottomAreaInvisibleEndRunnable = new Runnable() {
        @Override
        public void run() {
            mKeyguardBottomArea.setVisibility(View.GONE);
        }
    };

    private void setKeyguardBottomAreaVisibility(int statusBarState,
            boolean goingToFullShade) {
        if (goingToFullShade) {
            mKeyguardBottomArea.animate().cancel();
            mKeyguardBottomArea.animate()
                    .alpha(0f)
                    .setStartDelay(mStatusBar.getKeyguardFadingAwayDelay())
                    .setDuration(mStatusBar.getKeyguardFadingAwayDuration() / 2)
                    .setInterpolator(PhoneStatusBar.ALPHA_OUT)
                    .withEndAction(mAnimateKeyguardBottomAreaInvisibleEndRunnable)
                    .start();
        }
        //*/ Modified by tyd hanhao for not show keyguardbuttonarea when in shade_locked mode 2015-10-15
        else if (statusBarState == StatusBarState.KEYGUARD
                /*|| statusBarState == StatusBarState.SHADE_LOCKED*/) {
            mKeyguardBottomArea.animate().cancel();
            if (!mDozing) {
                mKeyguardBottomArea.setVisibility(View.VISIBLE);
            }
            mKeyguardBottomArea.setAlpha(1f);
        }
        //*/
        else {
            mKeyguardBottomArea.animate().cancel();
            mKeyguardBottomArea.setVisibility(View.GONE);
            mKeyguardBottomArea.setAlpha(1f);
        }
    }

    private void setKeyguardStatusViewVisibility(int statusBarState, boolean keyguardFadingAway,
            boolean goingToFullShade) {
        if ((!keyguardFadingAway && mStatusBarState == StatusBarState.KEYGUARD
                && statusBarState != StatusBarState.KEYGUARD) || goingToFullShade) {
            mKeyguardStatusView.animate().cancel();
            mKeyguardStatusViewAnimating = true;
            mKeyguardStatusView.animate()
                    .alpha(0f)
                    .setStartDelay(0)
                    .setDuration(160)
                    .setInterpolator(PhoneStatusBar.ALPHA_OUT)
                    .withEndAction(mAnimateKeyguardStatusViewInvisibleEndRunnable);
            if (keyguardFadingAway) {
                mKeyguardStatusView.animate()
                        .setStartDelay(mStatusBar.getKeyguardFadingAwayDelay())
                        .setDuration(mStatusBar.getKeyguardFadingAwayDuration()/2)
                        .start();
            }
        } else if (mStatusBarState == StatusBarState.SHADE_LOCKED
                && statusBarState == StatusBarState.KEYGUARD) {
            if((!mIsDefaultLockscreen && !mIsHeaderVisible) || mIsDefaultLockscreen){
            mKeyguardStatusView.animate().cancel();
            mKeyguardStatusView.setVisibility(View.VISIBLE);
            mKeyguardStatusViewAnimating = true;
            mKeyguardStatusView.setAlpha(0f);
            mKeyguardStatusView.animate()
                    .alpha(1f)
                    .setStartDelay(0)
                    .setDuration(320)
                    .setInterpolator(PhoneStatusBar.ALPHA_IN)
                    .withEndAction(mAnimateKeyguardStatusViewVisibleEndRunnable);
            }
        } else if (statusBarState == StatusBarState.KEYGUARD) {
            if((!mIsDefaultLockscreen && !mIsHeaderVisible) || mIsDefaultLockscreen){
            mKeyguardStatusView.animate().cancel();
            mKeyguardStatusViewAnimating = false;
            mKeyguardStatusView.setVisibility(View.VISIBLE);
            mKeyguardStatusView.setAlpha(1f);
            }
        } else {
            mKeyguardStatusView.animate().cancel();
            mKeyguardStatusViewAnimating = false;
            mKeyguardStatusView.setVisibility(View.GONE);
            mKeyguardStatusView.setAlpha(1f);
        }
    }

    private void updateQsState() {
        boolean expandVisually = mQsExpanded || mStackScrollerOverscrolling || mHeaderAnimating;
        mHeader.setVisibility((mQsExpanded || !mKeyguardShowing || mHeaderAnimating)
                ? View.VISIBLE
                : View.INVISIBLE);
        mHeader.setExpanded((mKeyguardShowing && !mHeaderAnimating)
                || (mQsExpanded && !mStackScrollerOverscrolling));
        mNotificationStackScroller.setScrollingEnabled(
                mStatusBarState != StatusBarState.KEYGUARD && (!mQsExpanded
                        || mQsExpansionFromOverscroll));
        mQsPanel.setVisibility(expandVisually ? View.VISIBLE : View.INVISIBLE);
        mQsContainer.setVisibility(
                mKeyguardShowing && !expandVisually ? View.INVISIBLE : View.VISIBLE);
        mScrollView.setTouchEnabled(mQsExpanded);
        updateEmptyShadeView();
        mQsNavbarScrim.setVisibility(mStatusBarState == StatusBarState.SHADE && mQsExpanded
                && !mStackScrollerOverscrolling && mQsScrimEnabled
                        ? View.VISIBLE
                        : View.INVISIBLE);
        if (mKeyguardUserSwitcher != null && mQsExpanded && !mStackScrollerOverscrolling) {
            mKeyguardUserSwitcher.hideIfNotSimple(true /* animate */);
        }
        
        //   Added by droi hanhao for show expandedCarrier & datausage 2016-01-19
        if(null != mExpandedCarrier) {
            mExpandedCarrier.setVisibility(expandVisually ? View.VISIBLE : View.GONE);
        }

        if(null != mDataUsage) {
            mDataUsage.setVisibility(expandVisually ? View.VISIBLE : View.GONE);
        }

    }

    private void setQsExpansion(float height) {
        //*/ Added by tyd hanhao for unknown error,tyd00583658 2015-11-27
        if(0 == mQsMinExpansionHeight && 0 == mQsMaxExpansionHeight) {
            Log.d(TAG, "this log can only be just expanded the status_bar for the first time to see... [tyd00583658]");
            mQsMinExpansionHeight = 1;
            mQsMaxExpansionHeight = 2;
        }
        //*/
        height = Math.min(Math.max(height, mQsMinExpansionHeight), mQsMaxExpansionHeight);
        //*/ Added by tyd hanhao for disable QSExpansion, when in SuperPowerSavingMode 2015-08-29
        if(mSuperPowerSaveEnabled) {
            height = mQsMinExpansionHeight;
        }
        //*/
        mQsFullyExpanded = height == mQsMaxExpansionHeight;
        if (height > mQsMinExpansionHeight && !mQsExpanded && !mStackScrollerOverscrolling) {
            setQsExpanded(true);
        } else if (height <= mQsMinExpansionHeight && mQsExpanded) {
            setQsExpanded(false);
            if (mLastAnnouncementWasQuickSettings && !mTracking && !isCollapsing()) {
                announceForAccessibility(getKeyguardOrLockScreenString());
                mLastAnnouncementWasQuickSettings = false;
            }
        }
        mQsExpansionHeight = height;
        mHeader.setExpansion(getHeaderExpansionFraction());
        setQsTranslation(height);
        requestScrollerTopPaddingUpdate(false /* animate */);
        updateNotificationScrim(height);
        if (mKeyguardShowing) {
            updateHeaderKeyguard();
        }
        if (mStatusBarState == StatusBarState.SHADE_LOCKED
                || mStatusBarState == StatusBarState.KEYGUARD) {
            updateKeyguardBottomAreaAlpha();
        }
        if (mStatusBarState == StatusBarState.SHADE && mQsExpanded
                && !mStackScrollerOverscrolling && mQsScrimEnabled) {
            mQsNavbarScrim.setAlpha(getQsExpansionFraction());
        }
        
        //*/ Added by droi hanhao for show expandedCarrier & datausage 2016-01-19 
        if(false && mQsExpanded && !mStackScrollerOverscrolling) {
            if(null != mExpandedCarrier) {
                mExpandedCarrier.setAlpha(getQsExpansionFraction());
            }
            
            if(null != mDataUsage) {
                mDataUsage.setAlpha(getQsExpansionFraction());
            }
        }
        //*/

        // Upon initialisation when we are not layouted yet we don't want to announce that we are
        // fully expanded, hence the != 0.0f check.
        if (height != 0.0f && mQsFullyExpanded && !mLastAnnouncementWasQuickSettings) {
            announceForAccessibility(getContext().getString(
                    R.string.accessibility_desc_quick_settings));
            mLastAnnouncementWasQuickSettings = true;
        }

        //*/ Added by tyd hanhao for customized clear notification button 2015-10-15
        boolean showDismissView = false;
        if(null != mNotificationData) {
            showDismissView = mNotificationData.hasActiveClearableNotifications();
        }

        if(mStatusBarState == StatusBarState.KEYGUARD) {
            if(mIsDefaultLockscreen) {
                //keyguard default lockscreen mode
                showDismissView = false;
            } else {
                if(height <= mQsMinExpansionHeight ) {
                    //keyguard normal lockscreen mode, but not show nssLayout
                    showDismissView = false;
                }
            }
        }

        mNotificationStackScroller.updateDismissView(showDismissView);
        //*/
        
        if (DEBUG) {
            invalidate();
        }
    }

    private String getKeyguardOrLockScreenString() {
        if (mStatusBarState == StatusBarState.KEYGUARD) {
            return getContext().getString(R.string.accessibility_desc_lock_screen);
        } else {
            return getContext().getString(R.string.accessibility_desc_notification_shade);
        }
    }

    private void updateNotificationScrim(float height) {
        int startDistance = mQsMinExpansionHeight + mNotificationScrimWaitDistance;
        float progress = (height - startDistance) / (mQsMaxExpansionHeight - startDistance);
        progress = Math.max(0.0f, Math.min(progress, 1.0f));
    }

    private float getHeaderExpansionFraction() {
        if (!mKeyguardShowing) {
            return getQsExpansionFraction();
        } else {
            return 1f;
        }
    }

    private void setQsTranslation(float height) {
        if (!mHeaderAnimating) {
            mQsContainer.setY(height - mQsContainer.getDesiredHeight() + getHeaderTranslation());
        }
        if (mKeyguardShowing && !mHeaderAnimating) {
            mHeader.setY(interpolate(getQsExpansionFraction(), -mHeader.getHeight(), 0));
        }
		//*/modified by shijiachen 20150727 for customize keyguard
        if(mStatusBarState == StatusBarState.SHADE){
        	handleQsTranslationInShade(height);
        }else if(mStatusBarState == StatusBarState.KEYGUARD){
        	handleQsTranslationInKeyguard(height);
        }else{
        	handleQsTranslationInShadeLock(height);
        }
		//*/
    }
    //*/added by shijiachen 20150821,handle top notification status
    private void handleQsTranslationInKeyguard(float height){
        if(height > 0){
        	mIsNotificationTracking = true;
        	if(null != mBlurFrame&& height > 0 &&! mIsShowNotificationScrim) {
				mIsShowNotificationScrim = true;
				mBlurFrame.setVisibility(View.VISIBLE);
                mBlurFrame.setBackgroundColor(getResources().getColor(R.color.droi_notification_bg_primary));
            }
        	if(!mIsHeaderVisible){
                final boolean show = Settings.Secure.getIntForUser(getContext().getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS,
                        1,
                        UserHandle.USER_CURRENT) != 0;
        		if(!mIsDefaultLockscreen){
        			if(show){
        				mNotificationStackScroller.setNotificationVisible(View.VISIBLE);        		
        			}
        			mNotificationStackScroller.setNotificationOverflowViewVisible(View.GONE);
        			if(mNotificationData != null){
            			mNotificationStackScroller.updateDismissView(mNotificationData.hasActiveClearableNotifications());	
            		}
        		}
        			runBlur(true, false);
    		    	if(mShowBlurBackground && mBlurImage != null&&mKeyguardView != null&&mBlurImage.getParent() == null){
    		    		mKeyguardView.addView(mBlurImage,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));	
    		    	}
            		if(mLockscreenPackageInfo!=null && mLockscreenPackageInfo.configShowKeyguardStatusInfo){
            			mKeyguardStatusView.setVisibility(View.GONE);
            		}	
            		notifyNotificationVisible(true);
        	}
        	/*/ freeme.chenming, 20160706. fixed anr bug for pull statusbar in keyguard. tyd00610326
        	if((int)height == mQsMaxExpansionHeight){
        	/*/
        	if(((int)height == mQsMaxExpansionHeight) && !mCanInShadeLock){
        	//*/
        		mCanInShadeLock = true;
        		mIsExpandNotificationScrolledToBottom = mNotificationStackScroller.isScrolledToBottom();
        		if(mNotificationData != null ){
        			mNotificationStackScroller.updateDismissView(mNotificationData.hasActiveClearableNotifications());	
        		}
        		Log.i("shijc", "mCanInShadeLock:" + mCanInShadeLock+",mIsExpandNotificationScrolledToBottom:" + mIsExpandNotificationScrolledToBottom);
        	}
        	mIsHeaderVisible = true;
        }else{
        	//*/ freeme.chenming, 20160706. fixed anr bug for pull statusbar in keyguard. tyd00610326
        	mCanInShadeLock = false;
        	//*/
        	mIsNotificationTracking = false;
        	mIsShowNotificationScrim = false;
        	boolean isInShadeLocked = false;
         	if(mIsHeaderVisible && !mAttemptToUnlock&&!mIsDefaultLockscreen){
         		isInShadeLocked = handleNotificationTouchTop();
         	}
         	if(!isInShadeLocked){
         		disableBlurBackground(false);
         		if(mIsDefaultLockscreen){
             		mBlurFrame.setVisibility(View.GONE);
             	}else{
             		if(!mIsScreenOn && mBlurFrame != null){
        				mBlurFrame.setVisibility(View.GONE);
        			}	
             	}
         	}
         	mIsHeaderVisible = false;
         }
    }

    private void runBlur(boolean isDelay,final boolean isForce){
        /*/ freeme.shanjibing, 20160621. optimization
    	if(isDelay){
    		mBlurRun = new BlurRun(isForce);
    		postDelayed(mBlurRun, 100);
    	}else{
    		createBlurImage(isForce);
    	}
        //*/
    }
    
    private boolean handleNotificationTouchTop(){
    			boolean inShadeLocked = false;
// 			Log.i("shijc", "handleNotificationTouchTop, -------> state:" + mStatusBarState + ",mCanInShadeLock:" + mCanInShadeLock);
// 			Log.i("shijc", "isScrolledToBottom :" + mNotificationStackScroller.isScrolledToBottom() + ",isScrolledToTop:" + mNotificationStackScroller.isScrolledToTop() );
 				if(mIsExpandNotificationScrolledToBottom||mIsInstantCollapse||( !mCanInShadeLock && mStatusBarState == StatusBarState.KEYGUARD)){
     				mNotificationStackScroller.setNotificationVisible(View.GONE);	
     				if(mLockscreenPackageInfo!=null && mLockscreenPackageInfo.configShowKeyguardStatusInfo){
             			mKeyguardStatusView.setVisibility(View.VISIBLE);
             		}
                    if(null != mBlurFrame) {
                   	    mBlurFrame.setVisibility(View.GONE);
                    }
                    disableBlurBackground(false);
                    notifyNotificationVisible(false);
                    mIsInstantCollapse = false;
     			}else{
     				mIsNotificationTracking = true;
     				inShadeLocked = true;
     				mKeyguardPanelViewCallback.onTrackNotification();        		
     				mNotificationStackScroller.setNotificationOverflowViewVisible(View.GONE);
     				mNotificationStackScroller.setAlpha(1);
     			}	
 				mCanInShadeLock = false;
 				return inShadeLocked;
    }
    public boolean isHeadVisible(){
    	return mIsHeaderVisible;
    }
    
    private void handleQsTranslationInShade(float height){
        if(null == mBlurFrame) {
            return;
        }
    	 if(height > 0){
            /*/ freeme.gouzhouping, 20160809, for removal the blur background when alarm in notification panel.
            if(mHeadsUpManager.hasPinnedHeadsUp()) {
            /*/
            if(mHeadsUpManager.hasPinnedHeadsUp() && !isFullyExpanded()) {
            //*/
                mIsShowNotificationScrim = false;
                mBlurFrame.setVisibility(View.GONE);
            } else 
           	if(!mIsShowNotificationScrim) {
   				   mIsShowNotificationScrim = true;
   				   mBlurFrame.setVisibility(View.VISIBLE);
                   mBlurFrame.setBackgroundColor(getResources().getColor(R.color.droi_notification_bg_primary));
            }
     	  }else{
               if(mIsShowNotificationScrim) {
               	   mIsShowNotificationScrim = false;
               	   mBlurFrame.setVisibility(View.GONE);
               }
     	  }
//    	 Log.d("shijc", "call  handleQsTranslationInShade,height:" + height);
    }
    
    private void notifyNotificationVisible(boolean visible){
	    /*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
		KeyguardSecurityView v = getKeyguardView();
	    if(v != null){
		    v.notificationVisibleChange(visible);
	    }
	    /*/ 
    	View view = getKeyguardView();
        if (view != null && (view instanceof KeyguardSecurityView)){
            KeyguardSecurityView v = (KeyguardSecurityView)view;
		    v.notificationVisibleChange(visible);
	    }else if (view != null && (view instanceof ICustomLockscreenView)) {
            ICustomLockscreenView v = (ICustomLockscreenView)view;
		    v.notificationVisibleChange(visible);
		}
		//*/
	}    
    private void handleQsTranslationInShadeLock(float height){
    	if(null != mBlurFrame &&! mIsShowNotificationScrim) {
			mIsShowNotificationScrim = true;
		    mBlurFrame.setVisibility(View.VISIBLE);
		    mBlurFrame.setBackgroundColor(getResources().getColor(R.color.droi_notification_bg_primary));
		    if(mBlurImage == null){
		    	runBlur(false, false);
		    }
		    notifyNotificationVisible(true);
		}
    	if(height > 0){
    		mIsHeaderVisible = true;
    	}else{
    		if(mIsHeaderVisible){
    			  notifyNotificationVisible(false);
    		}
    		mIsHeaderVisible = false;
    	}
    	Log.d("shijc", "call  handleQsTranslationInShadeLock,height:" + height);
    }
    
    public boolean isAnimatingExpandHeight(){
    	return mIsAnimatingExpandHeight;
    }
    
    public void resetQsTranslation(){
    	mIsNotificationTracking = false;
    	mIsHeaderVisible = false;
    	mIsShowNotificationScrim = false;
    	if(mBlurFrame != null){
    		mBlurFrame.setVisibility(View.GONE);	
    	}
    	disableBlurBackground(false);
		mNotificationStackScroller.setNotificationVisible(View.GONE);        		
		mNotificationStackScroller.setNotificationOverflowViewVisible(View.GONE);
    	notifyNotificationVisible(false);
    }
    
    private void disableBlurBackground(boolean destory){
            if(mBlurImage != null){
            	if(mKeyguardView != null){
            		mKeyguardView.removeView(mBlurImage);	
            	}
            	if(destory){
            		mBlurImage.setBackground(null);
                	mBlurImage = null;            		
            	}
            }
            if(mStatusBarState != StatusBarState.SHADE){
	            if(mLockscreenPackageInfo!=null && mLockscreenPackageInfo.configShowKeyguardStatusInfo){
	    			mKeyguardStatusView.setVisibility(View.VISIBLE);
	    		}
            }
    }
    
    public void resetPanelBarStatus(){
//    	mBar.resetPanelStates();
    	mBar.panelExpansionChanged(this, 1, true);
    }
    
    public void showBlurBackground(boolean show){
    	Log.d("shijc", "call  showBlurBackground,show:" + show);
    	if(show){
    		   if(!mIsShowNotificationScrim){
    			   mIsShowNotificationScrim = true;
            	   mBlurFrame.setVisibility(View.VISIBLE);
            	   runBlur(false, true);
    		    	if(mShowBlurBackground && mKeyguardView != null&&mBlurImage != null&&mBlurImage.getParent() == null){
    		    		mKeyguardView.addView(mBlurImage,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));	
    		    	}
    		    	mBlurFrame.setBackgroundColor(getResources().getColor(R.color.droi_notification_bg_primary));   
    		   }
    	}else{
        	   mIsShowNotificationScrim = false;
        	   mBlurFrame.setVisibility(View.GONE);
    	}
    }
    
    public boolean isNotificationTracking(){
    	return mIsNotificationTracking;
    }
    
    //*/

    private float calculateQsTopPadding() {
        if (mKeyguardShowing
                && (mQsExpandImmediate || mIsExpanding && mQsExpandedWhenExpandingStarted)) {

            // Either QS pushes the notifications down when fully expanded, or QS is fully above the
            // notifications (mostly on tablets). maxNotifications denotes the normal top padding
            // on Keyguard, maxQs denotes the top padding from the quick settings panel. We need to
            // take the maximum and linearly interpolate with the panel expansion for a nice motion.
            int maxNotifications = mClockPositionResult.stackScrollerPadding
                    - mClockPositionResult.stackScrollerPaddingAdjustment
                    - mNotificationTopPadding;
            int maxQs = getTempQsMaxExpansion();
            int max = mStatusBarState == StatusBarState.KEYGUARD
                    ? Math.max(maxNotifications, maxQs)
                    : maxQs;
            return (int) interpolate(getExpandedFraction(),
                    mQsMinExpansionHeight, max);
        } else if (mQsSizeChangeAnimator != null) {
            return (int) mQsSizeChangeAnimator.getAnimatedValue();
        } else if (mKeyguardShowing && mScrollYOverride == -1) {

            // We can only do the smoother transition on Keyguard when we also are not collapsing
            // from a scrolled quick settings.
            return interpolate(getQsExpansionFraction(),
                    mNotificationStackScroller.getIntrinsicPadding() - mNotificationTopPadding,
                    mQsMaxExpansionHeight);
        } else {
            return mQsExpansionHeight;
        }
    }

    private void requestScrollerTopPaddingUpdate(boolean animate) {
        mNotificationStackScroller.updateTopPadding(calculateQsTopPadding(),
                mScrollView.getScrollY(),
                mAnimateNextTopPaddingChange || animate,
                mKeyguardShowing
                        && (mQsExpandImmediate || mIsExpanding && mQsExpandedWhenExpandingStarted));
        mAnimateNextTopPaddingChange = false;
    }

    private void trackMovement(MotionEvent event) {
        if (mVelocityTracker != null) mVelocityTracker.addMovement(event);
        mLastTouchX = event.getX();
        mLastTouchY = event.getY();
    }

    private void initVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
        }
        mVelocityTracker = VelocityTracker.obtain();
    }

    private float getCurrentVelocity() {
        if (mVelocityTracker == null) {
            return 0;
        }
        mVelocityTracker.computeCurrentVelocity(1000);
        return mVelocityTracker.getYVelocity();
    }

    private void cancelQsAnimation() {
        if (mQsExpansionAnimator != null) {
            mQsExpansionAnimator.cancel();
        }
    }

    private void flingSettings(float vel, boolean expand) {
        flingSettings(vel, expand, null, false /* isClick */);
    }

    private void flingSettings(float vel, boolean expand, final Runnable onFinishRunnable,
            boolean isClick) {
        float target = expand ? mQsMaxExpansionHeight : mQsMinExpansionHeight;
        if (target == mQsExpansionHeight) {
            mScrollYOverride = -1;
            if (onFinishRunnable != null) {
                onFinishRunnable.run();
            }
            return;
        }
        boolean belowFalsingThreshold = isBelowFalsingThreshold();
        if (belowFalsingThreshold) {
            vel = 0;
        }
        mScrollView.setBlockFlinging(true);
        ValueAnimator animator = ValueAnimator.ofFloat(mQsExpansionHeight, target);
        if (isClick) {
            animator.setInterpolator(mTouchResponseInterpolator);
            animator.setDuration(368);
        } else {
            mFlingAnimationUtils.apply(animator, mQsExpansionHeight, target, vel);
        }
        if (belowFalsingThreshold) {
            animator.setDuration(350);
        }
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setQsExpansion((Float) animation.getAnimatedValue());
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mScrollView.setBlockFlinging(false);
                mScrollYOverride = -1;
                mQsExpansionAnimator = null;
                if (onFinishRunnable != null) {
                    onFinishRunnable.run();
                }
            }
        });
        animator.start();
        mQsExpansionAnimator = animator;
        mQsAnimatorExpand = expand;
    }

    /**
     * @return Whether we should intercept a gesture to open Quick Settings.
     */
    private boolean shouldQuickSettingsIntercept(float x, float y, float yDiff) {
        if (!mQsExpansionEnabled || mCollapsedOnDown) {
            return false;
        }
        View header = mKeyguardShowing ? mKeyguardStatusBar : mHeader;
        boolean onHeader = x >= header.getX() && x <= header.getX() + header.getWidth()
                && y >= header.getTop() && y <= header.getBottom();

        //*/ Added by Linguanrong for expand qs at once
        if(!mKeyguardShowing && mNotificationStackScroller.isScrolledToBottom()) {
            //*/ Modified by tyd hanhao for tyd00589842 2015-12-19
            return onHeader || (mQsExpanded && null != mNotificationData && mNotificationData.getActiveNotifications().size() != 0 && yDiff < 0);
            //*/
        } else
        //*/
        if (mQsExpanded) {
            return onHeader || (mScrollView.isScrolledToBottom() && yDiff < 0) && isInQsArea(x, y);
        } else {
            return onHeader;
        }
    }

    @Override
    protected boolean isScrolledToBottom() {
        if (!isInSettings()) {
            return mStatusBar.getBarState() == StatusBarState.KEYGUARD
                    || mNotificationStackScroller.isScrolledToBottom();
        } else {
            return mScrollView.isScrolledToBottom();
        }
    }

    @Override
    protected int getMaxPanelHeight() {
        int min = mStatusBarMinHeight;
        if (mStatusBar.getBarState() != StatusBarState.KEYGUARD
                && mNotificationStackScroller.getNotGoneChildCount() == 0) {
            int minHeight = (int) ((mQsMinExpansionHeight + getOverExpansionAmount())
                    * HEADER_RUBBERBAND_FACTOR);
            min = Math.max(min, minHeight);
        }
        int maxHeight;
        if (mQsExpandImmediate || mQsExpanded || mIsExpanding && mQsExpandedWhenExpandingStarted) {
            maxHeight = calculatePanelHeightQsExpanded();
        } else {
            maxHeight = calculatePanelHeightShade();
        }
        maxHeight = Math.max(maxHeight, min);
        return maxHeight;
    }

    private boolean isInSettings() {
        return mQsExpanded;
    }

    @Override
    protected void onHeightUpdated(float expandedHeight) {
        //*/ added by Droi shijiachen for customize keyguard
    	if(mStatusBarState == StatusBarState.KEYGUARD && expandedHeight == 0 ){
            mIsNotificationTracking = false;
    	}
    	//*/
        if (!mQsExpanded || mQsExpandImmediate || mIsExpanding && mQsExpandedWhenExpandingStarted) {
            positionClockAndNotifications();
        }
        if (mQsExpandImmediate || mQsExpanded && !mQsTracking && mQsExpansionAnimator == null
                && !mQsExpansionFromOverscroll) {
            float t;
            if (mKeyguardShowing) {

                // On Keyguard, interpolate the QS expansion linearly to the panel expansion
                t = expandedHeight / getMaxPanelHeight();
            } else {

                // In Shade, interpolate linearly such that QS is closed whenever panel height is
                // minimum QS expansion + minStackHeight
                float panelHeightQsCollapsed = mNotificationStackScroller.getIntrinsicPadding()
                        + mNotificationStackScroller.getMinStackHeight();
                float panelHeightQsExpanded = calculatePanelHeightQsExpanded();
                t = (expandedHeight - panelHeightQsCollapsed)
                        / (panelHeightQsExpanded - panelHeightQsCollapsed);
            }
            setQsExpansion(mQsMinExpansionHeight
                    + t * (getTempQsMaxExpansion() - mQsMinExpansionHeight));
        }
        updateStackHeight(expandedHeight);
        updateHeader();
        updateUnlockIcon();
        //*/modify by Droi shijiachen for customize keyguard
        if(mIsDefaultLockscreen){
            updateNotificationTranslucency();
        }else{
            updateNotificationTranslucency(expandedHeight);
        }
        //updateNotificationTranslucency();
        //*/
        updatePanelExpanded();
        mNotificationStackScroller.setShadeExpanded(!isFullyCollapsed());
        if (DEBUG) {
            invalidate();
        }
    }
    //*/added by Droid shijiachen for customize keyguard
    private void updateNotificationTranslucency(float expandedHeight){
        float alpha = 1;
       if(expandedHeight < 100){
             mNotificationStackScroller.setAlpha(0.5f);
       }else{
             mNotificationStackScroller.setAlpha(1f);
       }
   }
    //*/

    private void updatePanelExpanded() {
        boolean isExpanded = !isFullyCollapsed();
        if (mPanelExpanded != isExpanded) {
            mHeadsUpManager.setIsExpanded(isExpanded);
            mStatusBar.setPanelExpanded(isExpanded);
            mPanelExpanded = isExpanded;
        }
    }

    /**
     * @return a temporary override of {@link #mQsMaxExpansionHeight}, which is needed when
     *         collapsing QS / the panel when QS was scrolled
     */
    private int getTempQsMaxExpansion() {
        int qsTempMaxExpansion = mQsMaxExpansionHeight;
        if (mScrollYOverride != -1) {
            qsTempMaxExpansion -= mScrollYOverride;
        }
        return qsTempMaxExpansion;
    }

    private int calculatePanelHeightShade() {
        int emptyBottomMargin = mNotificationStackScroller.getEmptyBottomMargin();
        int maxHeight = mNotificationStackScroller.getHeight() - emptyBottomMargin
                - mTopPaddingAdjustment;
        maxHeight += mNotificationStackScroller.getTopPaddingOverflow();
        return maxHeight;
    }

    private int calculatePanelHeightQsExpanded() {
        float notificationHeight = mNotificationStackScroller.getHeight()
                - mNotificationStackScroller.getEmptyBottomMargin()
                - mNotificationStackScroller.getTopPadding();

        // When only empty shade view is visible in QS collapsed state, simulate that we would have
        // it in expanded QS state as well so we don't run into troubles when fading the view in/out
        // and expanding/collapsing the whole panel from/to quick settings.
        if (mNotificationStackScroller.getNotGoneChildCount() == 0
                && mShadeEmpty) {
            notificationHeight = mNotificationStackScroller.getEmptyShadeViewHeight()
                    + mNotificationStackScroller.getBottomStackPeekSize()
                    + mNotificationStackScroller.getCollapseSecondCardPadding();
        }
        int maxQsHeight = mQsMaxExpansionHeight;

        // If an animation is changing the size of the QS panel, take the animated value.
        if (mQsSizeChangeAnimator != null) {
            maxQsHeight = (int) mQsSizeChangeAnimator.getAnimatedValue();
        }
        float totalHeight = Math.max(
                maxQsHeight + mNotificationStackScroller.getNotificationTopPadding(),
                mStatusBarState == StatusBarState.KEYGUARD
                        ? mClockPositionResult.stackScrollerPadding - mTopPaddingAdjustment
                        : 0)
                + notificationHeight;
        if (totalHeight > mNotificationStackScroller.getHeight()) {
            float fullyCollapsedHeight = maxQsHeight
                    + mNotificationStackScroller.getMinStackHeight()
                    + mNotificationStackScroller.getNotificationTopPadding()
                    - getScrollViewScrollY();
            totalHeight = Math.max(fullyCollapsedHeight, mNotificationStackScroller.getHeight());
        }
        return (int) totalHeight;
    }

    private int getScrollViewScrollY() {
        if (mScrollYOverride != -1 && !mQsTracking) {
            return mScrollYOverride;
        } else {
            return mScrollView.getScrollY();
        }
    }
    private void updateNotificationTranslucency() {
        float alpha = 1f;
        if (mClosingWithAlphaFadeOut && !mExpandingFromHeadsUp && !mHeadsUpManager.hasPinnedHeadsUp()) {
            alpha = getFadeoutAlpha();
        }
        mNotificationStackScroller.setAlpha(alpha);
    }

    private float getFadeoutAlpha() {
        float alpha = (getNotificationsTopY() + mNotificationStackScroller.getItemHeight())
                / (mQsMinExpansionHeight + mNotificationStackScroller.getBottomStackPeekSize()
                - mNotificationStackScroller.getCollapseSecondCardPadding());
        alpha = Math.max(0, Math.min(alpha, 1));
        alpha = (float) Math.pow(alpha, 0.75);
        return alpha;
    }

    @Override
    protected float getOverExpansionAmount() {
        return mNotificationStackScroller.getCurrentOverScrollAmount(true /* top */);
    }

    @Override
    protected float getOverExpansionPixels() {
        return mNotificationStackScroller.getCurrentOverScrolledPixels(true /* top */);
    }

    private void updateUnlockIcon() {
        if (mStatusBar.getBarState() == StatusBarState.KEYGUARD
                || mStatusBar.getBarState() == StatusBarState.SHADE_LOCKED) {
            boolean active = getMaxPanelHeight() - getExpandedHeight() > mUnlockMoveDistance;
            KeyguardAffordanceView lockIcon = mKeyguardBottomArea.getLockIcon();
            if (active && !mUnlockIconActive && mTracking) {
                lockIcon.setImageAlpha(1.0f, true, 150, mFastOutLinearInterpolator, null);
                lockIcon.setImageScale(LOCK_ICON_ACTIVE_SCALE, true, 150,
                        mFastOutLinearInterpolator);
            } else if (!active && mUnlockIconActive && mTracking) {
                lockIcon.setImageAlpha(lockIcon.getRestingAlpha(), true /* animate */,
                        150, mFastOutLinearInterpolator, null);
                lockIcon.setImageScale(1.0f, true, 150,
                        mFastOutLinearInterpolator);
            }
            mUnlockIconActive = active;
        }
    }

    /**
     * Hides the header when notifications are colliding with it.
     */
    private void updateHeader() {
        if (mStatusBar.getBarState() == StatusBarState.KEYGUARD) {
            updateHeaderKeyguard();
        } else {
            updateHeaderShade();
        }

    }

    private void updateHeaderShade() {
        if (!mHeaderAnimating) {
            mHeader.setTranslationY(getHeaderTranslation());
        }
        setQsTranslation(mQsExpansionHeight);
    }

    private float getHeaderTranslation() {
        if (mStatusBar.getBarState() == StatusBarState.KEYGUARD) {
            return 0;
        }
        if (mNotificationStackScroller.getNotGoneChildCount() == 0) {
            if (mExpandedHeight / HEADER_RUBBERBAND_FACTOR >= mQsMinExpansionHeight) {
                return 0;
            } else {
                return mExpandedHeight / HEADER_RUBBERBAND_FACTOR - mQsMinExpansionHeight;
            }
        }
        float stackTranslation = mNotificationStackScroller.getStackTranslation();
        float translation = stackTranslation / HEADER_RUBBERBAND_FACTOR;
        if (mHeadsUpManager.hasPinnedHeadsUp() || mIsExpansionFromHeadsUp) {
            translation = mNotificationStackScroller.getTopPadding() + stackTranslation
                    - mNotificationTopPadding - mQsMinExpansionHeight;
        }
        return Math.min(0, translation);
    }

    /**
     * @return the alpha to be used to fade out the contents on Keyguard (status bar, bottom area)
     *         during swiping up
     */
    private float getKeyguardContentsAlpha() {
        float alpha;
        if (mStatusBar.getBarState() == StatusBarState.KEYGUARD) {

            // When on Keyguard, we hide the header as soon as the top card of the notification
            // stack scroller is close enough (collision distance) to the bottom of the header.
            alpha = getNotificationsTopY()
                    /
                    (mKeyguardStatusBar.getHeight() + mNotificationsHeaderCollideDistance);
        } else {

            // In SHADE_LOCKED, the top card is already really close to the header. Hide it as
            // soon as we start translating the stack.
            alpha = getNotificationsTopY() / mKeyguardStatusBar.getHeight();
        }
        alpha = MathUtils.constrain(alpha, 0, 1);
        alpha = (float) Math.pow(alpha, 0.75);
        return alpha;
    }

    private void updateHeaderKeyguardAlpha() {
        //*/ modify by Droi fenglei 2016-01-21 for customize keyguard
        float alphaQsExpansion = 1 - Math.min(1, getQsExpansionFraction() * 2);
        if(mIsDefaultLockscreen){
            mKeyguardStatusBar.setAlpha(Math.min(getKeyguardContentsAlpha(), alphaQsExpansion)
                    * mKeyguardStatusBarAnimateAlpha);
        }
        if(!mIsDefaultLockscreen){
            if(!mIsHeaderVisible){
                mKeyguardStatusBar.setAlpha(1);
            }else{
                float headAlpha = (getNotificationsTopY() -mKeyguardStatusBar.getHeight() )/mKeyguardStatusBar.getHeight();
                mKeyguardStatusBar.setAlpha(1 - headAlpha);
            }
            if(mIsHeaderVisible){
                if(mIsExpandNotificationScrolledToBottom||( !mCanInShadeLock && mStatusBarState == StatusBarState.KEYGUARD)){
                    mNotificationStackScroller.setAlpha(getKeyguardContentsAlpha());
                 }else{
                    mNotificationStackScroller.setAlpha(1f);
                }           
            }else{
                if(!mIsNotificationTracking && mNotificationStackScroller.isScrolledToBottom()){
                    mNotificationStackScroller.setNotificationVisible(View.GONE);   
                }
            }
        }
        //*/
        mKeyguardStatusBar.setVisibility(mKeyguardStatusBar.getAlpha() != 0f
                && !mDozing ? VISIBLE : INVISIBLE);
    }

    private void updateHeaderKeyguard() {
        updateHeaderKeyguardAlpha();
        setQsTranslation(mQsExpansionHeight);
    }

    private void updateKeyguardBottomAreaAlpha() {
        float alpha = Math.min(getKeyguardContentsAlpha(), 1 - getQsExpansionFraction());
        mKeyguardBottomArea.setAlpha(alpha);
        mKeyguardBottomArea.setImportantForAccessibility(alpha == 0f
                ? IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                : IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }

    private float getNotificationsTopY() {
        if (mNotificationStackScroller.getNotGoneChildCount() == 0) {
            return getExpandedHeight();
        }
        return mNotificationStackScroller.getNotificationsTopY();
    }

    @Override
    protected void onExpandingStarted() {
        super.onExpandingStarted();
        if(null != mBlurUtil && !mHasBlured ) {
            if(mStatusBarState == StatusBarState.SHADE && mShowBlurBackground){
                runBgBlurInAnimation();
            }
            mHasBlured = true;
        }
        mNotificationStackScroller.onExpansionStarted();
        mIsExpanding = true;
        mQsExpandedWhenExpandingStarted = mQsFullyExpanded;
        if (mQsExpanded) {
            onQsExpansionStarted();
        }
    }

    //*/ Added by tyd hanhao for show blur background smoothly, 2015-12-03
    private void runBgBlurInAnimation() {
        long start = System.currentTimeMillis();
        Bitmap bmp = mBlurUtil.blurBitmap();
        Log.i("connor", "wast time = " + (System.currentTimeMillis() - start) + " ms");
        setBackground(new BitmapDrawable(getResources(), bmp));
    }
    //*/
    
    private void createBlurImage(final boolean force){
	    	if(mBlurTask!=null){
	    		mBlurTask.cancel(true);
	    	}
	    	mBlurTask = new AsyncTask<Void, Void, Bitmap>(){
	    		@Override
	    			protected void onPreExecute() {
	    				super.onPreExecute();
	    				
	    			}	
				@Override
				protected Bitmap doInBackground(Void... arg0) {
					if(isCancelled()){
						return null;
					}
					Bitmap b = null;
					b = mBlurUtil.blurBitmap();
					return b;
				}
				@Override	
				protected void onPostExecute(Bitmap result) {
					if(result != null){
						boolean isNeedAdd = false;
						if(mBlurImage == null){
							isNeedAdd = true;
							mBlurImage = new ImageView(getContext());	
						}
						if(mShowBlurBackground && isNeedAdd && mKeyguardView != null&&(mIsHeaderVisible||force)&&mBlurImage.getParent() == null){
							mKeyguardView.addView(mBlurImage,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));							
						}
						if(mBlurImage.getDrawable()!= null){
							Bitmap bitmap = ((BitmapDrawable)mBlurImage.getDrawable()).getBitmap();
							if(bitmap != null && !bitmap.isRecycled()){
								bitmap.recycle();
							}
						}
						mBlurImage.setBackground(new BitmapDrawable(getResources(), result));
					}
					mBlurTask = null;
					super.onPostExecute(result);
				}
	    	};
	    	mBlurTask.execute();
    }

    @Override
    protected void onExpandingFinished() {
        super.onExpandingFinished();
        mNotificationStackScroller.onExpansionStopped();
        mHeadsUpManager.onExpandingFinished();
        mIsExpanding = false;
        mScrollYOverride = -1;
        if (isFullyCollapsed()) {
            DejankUtils.postAfterTraversal(new Runnable() {
                @Override
                public void run() {
                    setListening(false);
                }
            });

            // Workaround b/22639032: Make sure we invalidate something because else RenderThread
            // thinks we are actually drawing a frame put in reality we don't, so RT doesn't go
            // ahead with rendering and we jank.
            postOnAnimation(new Runnable() {
                @Override
                public void run() {
                    getParent().invalidateChild(NotificationPanelView.this, mDummyDirtyRect);
                }
            });
        } else {
            setListening(true);
        }
        mQsExpandImmediate = false;
        mTwoFingerQsExpandPossible = false;
        mIsExpansionFromHeadsUp = false;
        mNotificationStackScroller.setTrackingHeadsUp(false);
        mExpandingFromHeadsUp = false;
        setPanelScrimMinFraction(0.0f);
    }

    private void setListening(boolean listening) {
        mHeader.setListening(listening);
        mKeyguardStatusBar.setListening(listening);
        mQsPanel.setListening(listening);
        //*/ Added by tyd hanhao for customized qsTile 2015-06-23
        // show Brightness below QSTiles
        mQsContainer.setListening(listening);
        //*/
    }

    @Override
    public void instantExpand() {
        super.instantExpand();
        setListening(true);
    }

    @Override
    protected void setOverExpansion(float overExpansion, boolean isPixels) {
        if (mConflictingQsExpansionGesture || mQsExpandImmediate) {
            return;
        }
        if (mStatusBar.getBarState() != StatusBarState.KEYGUARD) {
            mNotificationStackScroller.setOnHeightChangedListener(null);
            if (isPixels) {
                mNotificationStackScroller.setOverScrolledPixels(
                        overExpansion, true /* onTop */, false /* animate */);
            } else {
                mNotificationStackScroller.setOverScrollAmount(
                        overExpansion, true /* onTop */, false /* animate */);
            }
            mNotificationStackScroller.setOnHeightChangedListener(this);
        }
    }

    @Override
    protected void onTrackingStarted() {
        super.onTrackingStarted();
        if (mQsFullyExpanded) {
            mQsExpandImmediate = true;
        }
        if (mStatusBar.getBarState() == StatusBarState.KEYGUARD
                || mStatusBar.getBarState() == StatusBarState.SHADE_LOCKED) {
            mAfforanceHelper.animateHideLeftRightIcon();
        }
        mNotificationStackScroller.onPanelTrackingStarted();
    }

    @Override
    protected void onTrackingStopped(boolean expand) {
        super.onTrackingStopped(expand);
        if (expand) {
            mNotificationStackScroller.setOverScrolledPixels(
                    0.0f, true /* onTop */, true /* animate */);
        }
        mNotificationStackScroller.onPanelTrackingStopped();
        if (expand && (mStatusBar.getBarState() == StatusBarState.KEYGUARD
                || mStatusBar.getBarState() == StatusBarState.SHADE_LOCKED)) {
            if (!mHintAnimationRunning) {
                mAfforanceHelper.reset(true);
            }
        }
        if (!expand && (mStatusBar.getBarState() == StatusBarState.KEYGUARD
                || mStatusBar.getBarState() == StatusBarState.SHADE_LOCKED)) {
            KeyguardAffordanceView lockIcon = mKeyguardBottomArea.getLockIcon();
            lockIcon.setImageAlpha(0.0f, true, 100, mFastOutLinearInterpolator, null);
            lockIcon.setImageScale(2.0f, true, 100, mFastOutLinearInterpolator);
        }
    }

    @Override
    public void onHeightChanged(ExpandableView view, boolean needsAnimation) {

        // Block update if we are in quick settings and just the top padding changed
        // (i.e. view == null).
        if (view == null && mQsExpanded) {
            return;
        }
        requestPanelHeightUpdate();
    }

    @Override
    public void onReset(ExpandableView view) {
    }

    @Override
    public void onScrollChanged() {
        if (mQsExpanded) {
            requestScrollerTopPaddingUpdate(false /* animate */);
            requestPanelHeightUpdate();
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mAfforanceHelper.onConfigurationChanged();
        if (newConfig.orientation != mLastOrientation) {
            resetVerticalPanelPosition();
        }
        mLastOrientation = newConfig.orientation;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        mNavigationBarBottomHeight = insets.getSystemWindowInsetBottom();
        updateMaxHeadsUpTranslation();
        return insets;
    }

    private void updateMaxHeadsUpTranslation() {
        mNotificationStackScroller.setHeadsUpBoundaries(getHeight(), mNavigationBarBottomHeight);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        if (layoutDirection != mOldLayoutDirection) {
            mAfforanceHelper.onRtlPropertiesChanged();
            mOldLayoutDirection = layoutDirection;
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mHeader) {
            onQsExpansionStarted();
            if (mQsExpanded) {
                flingSettings(0 /* vel */, false /* expand */, null, true /* isClick */);
            } else if (mQsExpansionEnabled) {
                EventLogTags.writeSysuiLockscreenGesture(
                        EventLogConstants.SYSUI_TAP_TO_OPEN_QS,
                        0, 0);
                flingSettings(0 /* vel */, true /* expand */, null, true /* isClick */);
            }
        }
    }

    @Override
    public void onAnimationToSideStarted(boolean rightPage, float translation, float vel) {
        boolean start = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? rightPage : !rightPage;
        mIsLaunchTransitionRunning = true;
        mLaunchAnimationEndRunnable = null;
        float displayDensity = mStatusBar.getDisplayDensity();
        int lengthDp = Math.abs((int) (translation / displayDensity));
        int velocityDp = Math.abs((int) (vel / displayDensity));
        if (start) {
            EventLogTags.writeSysuiLockscreenGesture(
                    EventLogConstants.SYSUI_LOCKSCREEN_GESTURE_SWIPE_DIALER, lengthDp, velocityDp);
            mKeyguardBottomArea.launchLeftAffordance();
        } else {
            EventLogTags.writeSysuiLockscreenGesture(
                    EventLogConstants.SYSUI_LOCKSCREEN_GESTURE_SWIPE_CAMERA, lengthDp, velocityDp);
            mSecureCameraLaunchManager.startSecureCameraLaunch();
        }
        mStatusBar.startLaunchTransitionTimeout();
        mBlockTouches = true;
        //*/freeme.zhangshaopiao,20170822,add swipe left and right action
        mIsAction = true;
        //*/
    }

    @Override
    public void onAnimationToSideEnded() {
        mIsLaunchTransitionRunning = false;
        mIsLaunchTransitionFinished = true;
        if (mLaunchAnimationEndRunnable != null) {
            mLaunchAnimationEndRunnable.run();
            mLaunchAnimationEndRunnable = null;
        }
    }

    @Override
    protected void startUnlockHintAnimation() {
      //*/modified by shijiachen 20150728 for customized keyguard
        if(mIsAnimatingExpandHeight){
        super.startUnlockHintAnimation();
        startHighlightIconAnimation(getCenterIcon());
        }
        //*/
    }

    /**
     * Starts the highlight (making it fully opaque) animation on an icon.
     */
    private void startHighlightIconAnimation(final KeyguardAffordanceView icon) {
        icon.setImageAlpha(1.0f, true, KeyguardAffordanceHelper.HINT_PHASE1_DURATION,
                mFastOutSlowInInterpolator, new Runnable() {
                    @Override
                    public void run() {
                        icon.setImageAlpha(icon.getRestingAlpha(),
                                true /* animate */, KeyguardAffordanceHelper.HINT_PHASE1_DURATION,
                                mFastOutSlowInInterpolator, null);
                    }
                });
    }

    @Override
    public float getMaxTranslationDistance() {
        return (float) Math.hypot(getWidth(), getHeight());
    }

    @Override
    public void onSwipingStarted(boolean rightIcon) {
        boolean camera = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? !rightIcon
                : rightIcon;
        if (camera) {
            mSecureCameraLaunchManager.onSwipingStarted();
            mKeyguardBottomArea.bindCameraPrewarmService();
        }
        requestDisallowInterceptTouchEvent(true);
        mOnlyAffordanceInThisMotion = true;
        mQsTracking = false;
    }

    @Override
    public void onSwipingAborted() {
        mKeyguardBottomArea.unbindCameraPrewarmService(false /* launched */);
    }

    @Override
    public void onIconClicked(boolean rightIcon) {
        if (mHintAnimationRunning) {
            return;
        }
        mHintAnimationRunning = true;
        mAfforanceHelper.startHintAnimation(rightIcon, new Runnable() {
            @Override
            public void run() {
                mHintAnimationRunning = false;
                mStatusBar.onHintFinished();
            }
        });
        rightIcon = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? !rightIcon : rightIcon;
        if (rightIcon) {
            mStatusBar.onCameraHintStarted();
        } else {
            if (mKeyguardBottomArea.isLeftVoiceAssist()) {
                mStatusBar.onVoiceAssistHintStarted();
            } else {
                mStatusBar.onPhoneHintStarted();
            }
        }
    }

    @Override
    public KeyguardAffordanceView getLeftIcon() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL
                ? mKeyguardBottomArea.getRightView()
                : mKeyguardBottomArea.getLeftView();
    }

    @Override
    public KeyguardAffordanceView getCenterIcon() {
        return mKeyguardBottomArea.getLockIcon();
    }

    @Override
    public KeyguardAffordanceView getRightIcon() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL
                ? mKeyguardBottomArea.getLeftView()
                : mKeyguardBottomArea.getRightView();
    }

    @Override
    public View getLeftPreview() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL
                ? mKeyguardBottomArea.getRightPreview()
                : mKeyguardBottomArea.getLeftPreview();
    }

    @Override
    public View getRightPreview() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL
                ? mKeyguardBottomArea.getLeftPreview()
                : mKeyguardBottomArea.getRightPreview();
    }

    @Override
    public float getAffordanceFalsingFactor() {
        return mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f;
    }
    public boolean isDefaultLockscreen(){
        return mIsDefaultLockscreen;
    }

    @Override
    protected float getPeekHeight() {
        if (mNotificationStackScroller.getNotGoneChildCount() > 0) {
            return mNotificationStackScroller.getPeekHeight();
        } else {
            return mQsMinExpansionHeight * HEADER_RUBBERBAND_FACTOR;
        }
    }

    @Override
    protected float getCannedFlingDurationFactor() {
        if (mQsExpanded) {
            return 0.7f;
        } else {
            return 0.6f;
        }
    }

    @Override
    protected boolean fullyExpandedClearAllVisible() {
        return mNotificationStackScroller.isDismissViewNotGone()
                && mNotificationStackScroller.isScrolledToBottom() && !mQsExpandImmediate;
    }

    @Override
    protected boolean isClearAllVisible() {
        return mNotificationStackScroller.isDismissViewVisible();
    }

    @Override
    protected int getClearAllHeight() {
        return mNotificationStackScroller.getDismissViewHeight();
    }

    @Override
    protected boolean isTrackingBlocked() {
        return mConflictingQsExpansionGesture && mQsExpanded;
    }

    public void notifyVisibleChildrenChanged() {
        if (mNotificationStackScroller.getNotGoneChildCount() != 0) {
            mReserveNotificationSpace.setVisibility(View.VISIBLE);
        } else {
            mReserveNotificationSpace.setVisibility(View.GONE);
        }
    }

    public boolean isQsExpanded() {
        return mQsExpanded;
    }

    public boolean isQsDetailShowing() {
        return mQsPanel.isShowingDetail();
    }

    public void closeQsDetail() {
        mQsPanel.closeDetail();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public boolean isLaunchTransitionFinished() {
        return mIsLaunchTransitionFinished;
    }

    public boolean isLaunchTransitionRunning() {
        return mIsLaunchTransitionRunning;
    }

    public void setLaunchTransitionEndRunnable(Runnable r) {
        mLaunchAnimationEndRunnable = r;
    }

    public void setEmptyDragAmount(float amount) {
        float factor = 0.8f;
        if (mNotificationStackScroller.getNotGoneChildCount() > 0) {
            factor = 0.4f;
        } else if (!mStatusBar.hasActiveNotifications()) {
            factor = 0.4f;
        }
        mEmptyDragAmount = amount * factor;
        positionClockAndNotifications();
    }

    private static float interpolate(float t, float start, float end) {
        return (1 - t) * start + t * end;
    }

    public void setDozing(boolean dozing, boolean animate) {
        if (dozing == mDozing) return;
        mDozing = dozing;
        if (mStatusBarState == StatusBarState.KEYGUARD) {
            updateDozingVisibilities(animate);
        }
    }

    private void updateDozingVisibilities(boolean animate) {
        if (mDozing) {
            mKeyguardStatusBar.setVisibility(View.INVISIBLE);
            mKeyguardBottomArea.setVisibility(View.INVISIBLE);
        } else {
            mKeyguardBottomArea.setVisibility(View.VISIBLE);
            mKeyguardStatusBar.setVisibility(View.VISIBLE);
            if (animate) {
                animateKeyguardStatusBarIn(DOZE_ANIMATION_DURATION);
                mKeyguardBottomArea.startFinishDozeAnimation();
            }
        }
    }

    @Override
    public boolean isDozing() {
        return mDozing;
    }

    public void setShadeEmpty(boolean shadeEmpty) {
        mShadeEmpty = shadeEmpty;
        updateEmptyShadeView();
    }

    private void updateEmptyShadeView() {
        //*/ Modified by tyd hanhao show 'No nofification' ingore QS's expanded state 2015-08-07
        // Hide "No notifications" in QS.
        mNotificationStackScroller.updateEmptyShadeView(mShadeEmpty /*&& !mQsExpanded*/);
        //*/
    }

    public void setQsScrimEnabled(boolean qsScrimEnabled) {
        boolean changed = mQsScrimEnabled != qsScrimEnabled;
        mQsScrimEnabled = qsScrimEnabled;
        if (changed) {
            updateQsState();
        }
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        mKeyguardUserSwitcher = keyguardUserSwitcher;
    }

    private final Runnable mUpdateHeader = new Runnable() {
        @Override
        public void run() {
            mHeader.updateEverything();
        }
    };

    public void onScreenTurningOn() {
        mKeyguardStatusView.refreshTime(); 
    }
    public void onScreenTurnedOn(){
      //*/ added by Droi fenglei 2015-07-14 for customize keyguard
        //mKeyguardStatusView.refreshTime(); 
        Log.d(TAG, "onScreenTurnedOn +");
        mIsScreenOn = true;
        if(mKeyguardStatusView != null && mKeyguardStatusView.getVisibility() != View.GONE){
            mKeyguardStatusView.refreshTime();  
        }
        if(!mLockPatternUtils.isLockScreenDisabled(ActivityManager.getCurrentUser()) && mStatusBarState == StatusBarState.KEYGUARD){
	    	if(mIsDefaultLockscreen ){
	    		mKeyguardStatusView.setVisibility(View.VISIBLE);
	    	}else{
			    //*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
	    	    View view = getKeyguardView();
                if (view != null && (view instanceof KeyguardSecurityView)){
                    KeyguardSecurityView v = (KeyguardSecurityView)view;
				    v.onResume(KeyguardSecurityView.SCREEN_ON);
				    if(mLockscreenPackageInfo != null && mLockscreenPackageInfo.configShowKeyguardStatusInfo){
		    	        mKeyguardStatusView.setVisibility(View.VISIBLE);
		    	    }
		    	}else if(view != null && (view instanceof ICustomLockscreenView)){
                    ICustomLockscreenView v = (ICustomLockscreenView)view;
                    //v.onResume(KeyguardSecurityView.SCREEN_ON);
				//*/	
                    if(mLockscreenPackageInfo != null && mLockscreenPackageInfo.configShowKeyguardStatusInfo){
                        mKeyguardStatusView.setVisibility(View.VISIBLE);
                    }
                }else if( isUserSetComplete()){
		    		prepareKeyguardView();
		    	}
		    	//mUnreadHelper.refresh();
	    	}
    	}
        //*/added by shijiachen 20150908 for distance unlock
    	if( mStatusBarState == StatusBarState.KEYGUARD){
    		registerGestureSensorListener();            	
    	}
        //*/
        Log.d(TAG, "onScreenTurnedOn -");
    }
    
    public void onScreenTurnedOff(){
        Log.d("shijc", "onScreenTurnedOff +");
        mIsNotificationTracking = false;
        mCanInShadeLock = false;
        mIsAnimatingExpandHeight = false;
    	mIsShowNotificationScrim = false;
    	if(mBlurRun != null){
    		removeCallbacks(mBlurRun);
    	}
    	if(mStatusBarState != StatusBarState.SHADE){
    		expand();
    	 	if(mBlurFrame != null){
    	 		mBlurFrame.setVisibility(View.GONE);
    	 	}
           mKeyguardStatusView.setVisibility(View.GONE);
    	}
		mNotificationStackScroller.setNotificationOverflowViewVisible(View.GONE);
    	mIsScreenOn = false;
        if(!mLockPatternUtils.isLockScreenDisabled(ActivityManager.getCurrentUser())){
            if(!mIsDefaultLockscreen){
			    //*/ freeme, gouzhouping, 20161025, for custom keyguard new interface
                View view = getKeyguardView();
                if (view != null && (view instanceof KeyguardSecurityView)){
                    KeyguardSecurityView v = (KeyguardSecurityView)view;
				    v.reset();
					v.onPause();
                }else if(view != null && (view instanceof ICustomLockscreenView)){
                    ICustomLockscreenView v = (ICustomLockscreenView)view;
                    v.onReset();
                    v.onPause();
				//*/	
                }else if(mStatusBarState != StatusBarState.SHADE && isUserSetComplete()){
                   // mIsKeyguardExist = false;
                    prepareKeyguardView();
                }
            }
        }
        //*/added by shijiachen 20150908 for distance unlock
        unRegisterGestureSensorListener();
        //*/
    	disableBlurBackground(true);
        mAttemptToUnlock = false;
        Log.d("shijc", "onScreenTurnedOff -");
    }
	
    /*/ freeme, gouzhouping, 20161025, for custom keyguard new interface 
    private KeyguardSecurityView getKeyguardView(){
        KeyguardSecurityView v = null;
	/*/
    private View getKeyguardView(){
        View v = null;
	//*/	
        if(mKeyguardView != null && mKeyguardView.getChildCount()>0){
            try {
                 v = mKeyguardView.getChildAt(0);
            } catch (Exception e) {
                if(DEBUG){
                    Log.d(TAG, "getKeyguardView exception: " + e);
                    e.printStackTrace();
                }
            }
        }
        return v;
    }
    
    public DroiKeyguardManager getDroiKeyguardManager() {
		return mDroiKeyguardManager;
	}
    
    //*/

    @Override
    public void onEmptySpaceClicked(float x, float y) {
        onEmptySpaceClick(x);
    }

    protected boolean onMiddleClicked() {
        switch (mStatusBar.getBarState()) {
            case StatusBarState.KEYGUARD:
                if (!mDozingOnDown) {
                    EventLogTags.writeSysuiLockscreenGesture(
                            EventLogConstants.SYSUI_LOCKSCREEN_GESTURE_TAP_UNLOCK_HINT,
                            0 /* lengthDp - N/A */, 0 /* velocityDp - N/A */);
                    startUnlockHintAnimation();
                }
                return true;
            case StatusBarState.SHADE_LOCKED:
                if (!mQsExpanded) {
                    mStatusBar.goToKeyguard();
                }
                return true;
            case StatusBarState.SHADE:

                // This gets called in the middle of the touch handling, where the state is still
                // that we are tracking the panel. Collapse the panel after this is done.
                post(mPostCollapseRunnable);
                return false;
            default:
                return true;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (DEBUG) {
            Paint p = new Paint();
            p.setColor(Color.RED);
            p.setStrokeWidth(2);
            p.setStyle(Paint.Style.STROKE);
            canvas.drawLine(0, getMaxPanelHeight(), getWidth(), getMaxPanelHeight(), p);
            p.setColor(Color.BLUE);
            canvas.drawLine(0, getExpandedHeight(), getWidth(), getExpandedHeight(), p);
            p.setColor(Color.GREEN);
            canvas.drawLine(0, calculatePanelHeightQsExpanded(), getWidth(),
                    calculatePanelHeightQsExpanded(), p);
            p.setColor(Color.YELLOW);
            canvas.drawLine(0, calculatePanelHeightShade(), getWidth(),
                    calculatePanelHeightShade(), p);
            p.setColor(Color.MAGENTA);
            canvas.drawLine(0, calculateQsTopPadding(), getWidth(),
                    calculateQsTopPadding(), p);
            p.setColor(Color.CYAN);
            canvas.drawLine(0, mNotificationStackScroller.getTopPadding(), getWidth(),
                    mNotificationStackScroller.getTopPadding(), p);
        }
    }

    @Override
    public void onHeadsUpPinnedModeChanged(final boolean inPinnedMode) {
        if (inPinnedMode) {
            mHeadsUpExistenceChangedRunnable.run();
            updateNotificationTranslucency();
        } else {
            mHeadsUpAnimatingAway = true;
            mNotificationStackScroller.runAfterAnimationFinished(
                    mHeadsUpExistenceChangedRunnable);
        }
    }

    @Override
    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
        //*/ freeme.shanjibing, 20160628. cancel this anim
        //mNotificationStackScroller.generateHeadsUpAnimation(headsUp, true);
        //*/
    }

    @Override
    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
    }

    @Override
    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean isHeadsUp) {
        //*/ freeme.shanjibing, 20160625. cancel this anim
        //mNotificationStackScroller.generateHeadsUpAnimation(entry.row, isHeadsUp);
        //*/
    }

    @Override
    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        super.setHeadsUpManager(headsUpManager);
        mHeadsUpTouchHelper = new HeadsUpTouchHelper(headsUpManager, mNotificationStackScroller,
                this);
    }

    public void setTrackingHeadsUp(boolean tracking) {
        if (tracking) {
            mNotificationStackScroller.setTrackingHeadsUp(true);
            mExpandingFromHeadsUp = true;
        }
        // otherwise we update the state when the expansion is finished
    }
	
	private void setDisableWindowHideFlag(boolean disable){
    	if(disable){
       	  if( android.provider.Settings.System.getInt(getContext().getContentResolver(), "disable_lockscreen_window_force_hide",0) != 1){
      		 android.provider.Settings.System.putInt(getContext().getContentResolver(), "disable_lockscreen_window_force_hide", 1);
      	  }
    	}else{
    		if( android.provider.Settings.System.getInt(getContext().getContentResolver(), "disable_lockscreen_window_force_hide",0) != 0){
         		 android.provider.Settings.System.putInt(getContext().getContentResolver(), "disable_lockscreen_window_force_hide", 0);
         	  }
    	}
    }

    @Override
    protected void onClosingFinished() {
        super.onClosingFinished();
        resetVerticalPanelPosition();
        setClosingWithAlphaFadeout(false);
    }

    private void setClosingWithAlphaFadeout(boolean closing) {
        mClosingWithAlphaFadeOut = closing;
        mNotificationStackScroller.forceNoOverlappingRendering(closing);
    }

    /**
     * Updates the vertical position of the panel so it is positioned closer to the touch
     * responsible for opening the panel.
     *
     * @param x the x-coordinate the touch event
     */
    private void updateVerticalPanelPosition(float x) {
        // M: Fix the display wrong issue when the width = 0 unexpected.
        // Step: set screen lock as none and reboot then
        if (mNotificationStackScroller.getWidth() <= 0
                || mNotificationStackScroller.getWidth() * 1.75f > getWidth()) {
            resetVerticalPanelPosition();
            return;
        }
        float leftMost = mPositionMinSideMargin + mNotificationStackScroller.getWidth() / 2;
        float rightMost = getWidth() - mPositionMinSideMargin
                - mNotificationStackScroller.getWidth() / 2;
        if (Math.abs(x - getWidth() / 2) < mNotificationStackScroller.getWidth() / 4) {
            x = getWidth() / 2;
        }
        x = Math.min(rightMost, Math.max(leftMost, x));
        setVerticalPanelTranslation(x -
                (mNotificationStackScroller.getLeft() + mNotificationStackScroller.getWidth() / 2));
     }

    private void resetVerticalPanelPosition() {
        setVerticalPanelTranslation(0f);
    }

    private void setVerticalPanelTranslation(float translation) {
        mNotificationStackScroller.setTranslationX(translation);
        mScrollView.setTranslationX(translation);
        mHeader.setTranslationX(translation);
    }

    private void updateStackHeight(float stackHeight) {
        mNotificationStackScroller.setStackHeight(stackHeight);
        updateKeyguardBottomAreaAlpha();
    }

    public void setPanelScrimMinFraction(float minFraction) {
        mBar.panelScrimMinFractionChanged(minFraction);
    }

    public void clearNotificattonEffects() {
        mStatusBar.clearNotificationEffects();
    }

    protected boolean isPanelVisibleBecauseOfHeadsUp() {
        return mHeadsUpManager.hasPinnedHeadsUp() || mHeadsUpAnimatingAway;
    }

}
