/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.droi.recent;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;

import com.android.systemui.R;
import com.android.systemui.SwipeHelper;
import com.droi.recent.RecentsPanelView.TaskDescriptionAdapter;

import java.lang.Override;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class RecentsHorizontalScrollView extends HorizontalScrollView
        implements SwipeHelper.Callback, RecentsPanelView.RecentsScrollView {
    private static final String TAG = RecentsPanelView.TAG;
    private static final boolean DEBUG = RecentsPanelView.DEBUG;
    private LinearLayout mLinearLayout;
    private TaskDescriptionAdapter mAdapter;
    private RecentsCallback mCallback;
    protected int mLastScrollPosition = -1;
    private SwipeHelper mSwipeHelper;
    private FadedEdgeDrawHelper mFadedEdgeDrawHelper;
    private HashSet<View> mRecycledViews;
    private int mNumItemsInOneScreenful;
    private Runnable mOnScrollListener;
    //*/ Added by tyd hongchang.han 2015.10.21
    private OverScroller mScroller;
    private boolean mLastPositionChagned;
    private boolean mClearFlag;
    private final int RECENT_ITEM_WIDTH ;
    private final int LEFT_OVER_SCROLL ;
	private final int HIDE_PART;
    private final int RECENT_ITEM_CONTENT_WIDTH;
    //*/
    //Added by Droi Sean 2015-12-05 for remeber screen info begin
    private final int SCREEN_WIDTH;
    private final int SCREEN_HEIGHT;
    private int mItemHeight = 0;
    //Added by Droi Sean 2015-12-05 for remeber screen info end

    public RecentsHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        mSwipeHelper = new SwipeHelper(SwipeHelper.Y, this, context);
        mFadedEdgeDrawHelper = FadedEdgeDrawHelper.create(context, attrs, this, false);
        mRecycledViews = new HashSet<View>();
        //*/ Added by tyd hongchang.han 2015.10.21
        mScroller = getScroller();
        //*/ tyd
        //Added by Droi Sean 2015-12-05 for init screen info begin
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        SCREEN_WIDTH = wm.getDefaultDisplay().getWidth();
        SCREEN_HEIGHT = wm.getDefaultDisplay().getHeight();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int statusBarHeight = (int) Math.ceil( 25 * metrics.density);
        int titleHeight = (int)context.getResources().getDimension(R.dimen.droi_app_thumbnail_title_height);
        int thumbnailHeight = (int)context.getResources().getDimension(R.dimen.droi_status_bar_recents_thumbnail_height);
        mItemHeight = (SCREEN_HEIGHT - statusBarHeight - (titleHeight + thumbnailHeight))/2 + titleHeight + thumbnailHeight;
        RECENT_ITEM_CONTENT_WIDTH = (int)context.getResources().getDimension(R.dimen.droi_status_bar_recents_thumbnail_width);
        //Added by Droi Sean 2015-12-05 for init screen info end
        
        RECENT_ITEM_WIDTH = (int)context.getResources().getDimension(R.dimen.recent_item_width);
        LEFT_OVER_SCROLL = (SCREEN_WIDTH-RECENT_ITEM_WIDTH)/2;
        HIDE_PART = RECENT_ITEM_WIDTH - LEFT_OVER_SCROLL;
    }

    public void setMinSwipeAlpha(float minAlpha) {
        mSwipeHelper.setMinSwipeProgress(minAlpha);
    }

    private int scrollPositionOfMostRecent() {
        return mLinearLayout.getWidth() - getWidth();
    }

    private void addToRecycledViews(View v) {
        if (mRecycledViews.size() < mNumItemsInOneScreenful) {
            mRecycledViews.add(v);
        }
    }

    public View findViewForTask(int persistentTaskId) {
        for (int i = 0; i < mLinearLayout.getChildCount(); i++) {
            View v = mLinearLayout.getChildAt(i);
            RecentsPanelView.ViewHolder holder = (RecentsPanelView.ViewHolder) v.getTag();
            if (holder.taskDescription.persistentTaskId == persistentTaskId) {
                return v;
            }
        }
        return null;
    }

    private void update() {
        for (int i = 0; i < mLinearLayout.getChildCount(); i++) {
            View v = mLinearLayout.getChildAt(i);
            addToRecycledViews(v);
            mAdapter.recycleView(v);
        }
        LayoutTransition transitioner = getLayoutTransition();
        setLayoutTransition(null);

        mLinearLayout.removeAllViews();
        Iterator<View> recycledViews = mRecycledViews.iterator();
        for (int i = 0; i < mAdapter.getCount(); i++) {
            View old = null;
            if (recycledViews.hasNext()) {
                old = recycledViews.next(); 
                recycledViews.remove();
                old.setVisibility(VISIBLE);
            }

            final View view = mAdapter.getView(i, old, mLinearLayout);
            //Added by Droi Sean 2015-12-05 for set item height begin
            //LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)view.getLayoutParams();
            //lp.height = mItemHeight;
            //Added by Droi Sean 2015-12-05 for set item height end
            if (mFadedEdgeDrawHelper != null) {
                mFadedEdgeDrawHelper.addViewCallback(view);
            }

            OnTouchListener noOpListener = new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            };

            view.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                	/*/ Deleted by TYD hongchang 2015-10-27
                    mCallback.dismiss();
                    //*/
                }
            });
            // We don't want a click sound when we dimiss recents
            view.setSoundEffectsEnabled(false);

            OnClickListener launchAppListener = new OnClickListener() {
                public void onClick(View v) {
                    mCallback.handleOnClick(view);
                }
            };

            RecentsPanelView.ViewHolder holder = (RecentsPanelView.ViewHolder) view.getTag();
            final View thumbnailView = holder.thumbnailView;
            OnLongClickListener longClickListener = new OnLongClickListener() {
                public boolean onLongClick(View v) {
                    final View anchorView = view.findViewById(R.id.droi_app_description);
                    mCallback.handleLongPress(view, anchorView, thumbnailView);
                    return true;
                }
            };
            thumbnailView.setClickable(true);
            thumbnailView.setOnClickListener(launchAppListener);

            //Modified by Sean 2015-12-11 for disable long click event begin
            //thumbnailView.setOnLongClickListener(longClickListener);
            //Modified by Sean 2015-12-11 for disable long click event end

            // We don't want to dismiss recents if a user clicks on the app title
            // (we also don't want to launch the app either, though, because the
            // app title is a small target and doesn't have great click feedback)
            final View appTitle = view.findViewById(R.id.droi_app_label);
            appTitle.setContentDescription(" ");
            appTitle.setOnTouchListener(noOpListener);
            mLinearLayout.addView(view);
        }
        setLayoutTransition(transitioner);
        //Added by Droi Sean 2015-12-07 for initing the first position begin
        mLastScrollPosition = computeInitScrollX();
        //Added by Droi Sean 2015-12-07 for initing the first position end
        // Scroll to end after initial layout.

        final OnGlobalLayoutListener updateScroll = new OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                	//*/ Added and Modifed by tyd hongchang.han 2015.10.21

                	if (!mClearFlag) {
                        onOverScrolled(mLastScrollPosition, getScrollY(), false, false);
                	}

                    final ViewTreeObserver observer = getViewTreeObserver();

                    if (observer.isAlive()) {
                        observer.removeOnGlobalLayoutListener(this);
                    }
                	//*/ tyd
                }
            };
        getViewTreeObserver().addOnGlobalLayoutListener(updateScroll);
    }

    @Override
    public void removeViewInLayout(final View view) {
        dismissChild(view);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (DEBUG) Log.v(TAG, "onInterceptTouchEvent()");

        final int x = (int)ev.getRawX();
        final int y = (int)ev.getRawY();
        /*
        final int action = ev.getAction();
        if(action == MotionEvent.ACTION_DOWN){
            if(mLinearLayout != null && mLinearLayout.getChildCount() >= 1){
                View child = mLinearLayout.getChildAt(0);
                if(y > child.getBottom() || x < child.getTop()){
                    Log.v(TAG, "Touch outside of Scroll item");
                }
            }
        }

        if(touchedLockIcon(x, y)){
            return false;
        }*/
        return mSwipeHelper.onInterceptTouchEvent(ev) ||
            super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (DEBUG) Log.v(TAG, "onTouchEvent()");

        final int x = (int)ev.getRawX();
        final int y = (int)ev.getRawY();

        /*
        if(touchedLockIcon(x, y)){
            return false;
        }*/

    	//*/ Added by tyd hongchang.han 2015.10.21
    	if (isBeingDragged()) {
    		return super.onTouchEvent(ev);
    	}
    	//*/ tyd
        return mSwipeHelper.onTouchEvent(ev) ||
            super.onTouchEvent(ev);
    }

    //*/ Modified by TYD hongchang.han 2010-10-21
    public boolean canChildBeDismissed(View v) {
    	View childView = v.findViewById(R.id.droi_recent_item);
        if (childView.getTranslationY() > 0) {
        	return false;
        } else {
        	return true;
        }
    }
    //*/ TYD

    @Override
    public boolean isAntiFalsingNeeded() {
        return false;
    }

    @Override
    public float getFalsingThresholdFactor() {
        return 1.0f;
    }

    public void dismissChild(View v) {
        mSwipeHelper.dismissChild(v, 0);
    }

    public void onChildDismissed(View v) {
        addToRecycledViews(v);
        //*/ Added by tyd hongchang.han 2015.10.21
        computeLastPostion(v);
        //onOverScrolled(mLastScrollPosition, getScrollY(), false, false);
        overScrollBy(mLastScrollPosition - getScrollX(), 0,
                getScrollX(), getScrollY(),
                0, 0,
                getMinScrollRangeX(), getMaxScrollRangeX(),
                false);
         //*/ tyd
        mLinearLayout.removeView(v);
        mCallback.handleSwipe(v);
        // Restore the alpha/translation parameters to what they were before swiping
        // (for when these items are recycled)
        View contentView = getChildContentView(v);
        contentView.setAlpha(1f);
        contentView.setTranslationY(0);
    }

    public void onBeginDrag(View v) {
        // We do this so the underlying ScrollView knows that it won't get
        // the chance to intercept events anymore
        requestDisallowInterceptTouchEvent(true);
    }

    public void onDragCancelled(View v) {
    }

    @Override
    public void onChildSnappedBack(View animView) {
    }

    @Override
    public boolean updateSwipeProgress(View animView, boolean dismissable, float swipeProgress) {
        return false;
    }

    public View getChildAtPosition(MotionEvent ev) {
        final float x = ev.getX() + getScrollX();
        final float y = ev.getY() + getScrollY();
        for (int i = 0; i < mLinearLayout.getChildCount(); i++) {
            View item = mLinearLayout.getChildAt(i);
            if (x >= item.getLeft() && x < item.getRight()
                && y >= item.getTop() && y < item.getBottom()) {
                return item;
            }
        }
        return null;
    }

    public View getChildContentView(View v) {
        return v.findViewById(R.id.droi_recent_item);
    }

    @Override
    public void drawFadedEdges(Canvas canvas, int left, int right, int top, int bottom) {
        //Modified by Droi Sean 2015-12-03 for hiding fadededge begin
        /*
        if (mFadedEdgeDrawHelper != null) {
           
            mFadedEdgeDrawHelper.drawCallback(canvas,
                    left, right, top, bottom, getScrollX(), getScrollY(),
                    0, 0,
                    getLeftFadingEdgeStrength(), getRightFadingEdgeStrength(), getPaddingTop());
        }*/
        //Modified by Droi Sean 2015-12-03 for hiding fadededge end
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
       super.onScrollChanged(l, t, oldl, oldt);
       if (mOnScrollListener != null) {
           mOnScrollListener.run();
       }
    }

    public void setOnScrollListener(Runnable listener) {
        mOnScrollListener = listener;
    }

    @Override
    public int getVerticalFadingEdgeLength() {
        if (mFadedEdgeDrawHelper != null) {
            return mFadedEdgeDrawHelper.getVerticalFadingEdgeLength();
        } else {
            return super.getVerticalFadingEdgeLength();
        }
    }

    @Override
    public int getHorizontalFadingEdgeLength() {
        if (mFadedEdgeDrawHelper != null) {
            return mFadedEdgeDrawHelper.getHorizontalFadingEdgeLength();
        } else {
            return super.getHorizontalFadingEdgeLength();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setScrollbarFadingEnabled(true);
        mLinearLayout = (LinearLayout) findViewById(R.id.droi_recents_linear_layout);
        final int leftPadding = getContext().getResources()
            .getDimensionPixelOffset(R.dimen.status_bar_recents_thumbnail_left_margin);
        setOverScrollEffectPadding(leftPadding, 0);
        //Added by Droi Sean 2015-12-07 for initing the first position begin
        mLastScrollPosition = computeInitScrollX();
        //Added by Droi Sean 2015-12-07 for initing the first position end
        // Scroll to end after initial layout.
    }

    @Override
    public void onAttachedToWindow() {
        if (mFadedEdgeDrawHelper != null) {
            mFadedEdgeDrawHelper.onAttachedToWindowCallback(mLinearLayout, isHardwareAccelerated());
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float densityScale = getResources().getDisplayMetrics().density;
        mSwipeHelper.setDensityScale(densityScale);
        float pagingTouchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        mSwipeHelper.setPagingTouchSlop(pagingTouchSlop);
    }

    private void setOverScrollEffectPadding(int leftPadding, int i) {
        // TODO Add to (Vertical)ScrollView
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Skip this work if a transition is running; it sets the scroll values independently
        // and should not have those animated values clobbered by this logic
        LayoutTransition transition = mLinearLayout.getLayoutTransition();
        if (transition != null && transition.isRunning()) {
            return;
        }
        // Keep track of the last visible item in the list so we can restore it
        // to the bottom when the orientation changes.
        //mLastScrollPosition = scrollPositionOfMostRecent();

        // This has to happen post-layout, so run it "in the future"
        post(new Runnable() {
            public void run() {
                // Make sure we're still not clobbering the transition-set values, since this
                // runnable launches asynchronously
                LayoutTransition transition = mLinearLayout.getLayoutTransition();
                if (transition == null || !transition.isRunning()) {
                    scrollTo(mLastScrollPosition, 0);
                }
            }
        });
    }

    @Override
    public void scrollTo ( int x, int y){
        if (mScrollX != x || mScrollY != y) {
            int oldX = mScrollX;
            int oldY = mScrollY;
            mScrollX = x;
            mScrollY = y;
            invalidateParentCaches();
            onScrollChanged(mScrollX, mScrollY, oldX, oldY);
            if (!awakenScrollBars()) {
                postInvalidateOnAnimation();
            }
        }
    }

    public void setAdapter(TaskDescriptionAdapter adapter) {
        mAdapter = adapter;
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            public void onChanged() {
                update();
            }

            public void onInvalidated() {
                update();
            }
        });
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int childWidthMeasureSpec =
                MeasureSpec.makeMeasureSpec(dm.widthPixels, MeasureSpec.AT_MOST);
        int childheightMeasureSpec =
                MeasureSpec.makeMeasureSpec(dm.heightPixels, MeasureSpec.AT_MOST);
        View child = mAdapter.createView(mLinearLayout);
        child.measure(childWidthMeasureSpec, childheightMeasureSpec);
        mNumItemsInOneScreenful =
                (int) FloatMath.ceil(dm.widthPixels / (float) child.getMeasuredWidth());
        addToRecycledViews(child);

        for (int i = 0; i < mNumItemsInOneScreenful - 1; i++) {
            addToRecycledViews(mAdapter.createView(mLinearLayout));
        }
    }

    public int numItemsInOneScreenful() {
        return mNumItemsInOneScreenful;
    }

    @Override
    public void setLayoutTransition(LayoutTransition transition) {
        // The layout transition applies to our embedded LinearLayout
        mLinearLayout.setLayoutTransition(transition);
    }

    public void setCallback(RecentsCallback callback) {
        mCallback = callback;
    }

    //*/ Added by tyd  hongchang.han 2015.10.21.
    private int getMaxScrollRangeX() {
    	return mLinearLayout.getWidth() - getWidth() + LEFT_OVER_SCROLL;
    }

    private int computeInitScrollX(){
        if(mLinearLayout.getChildCount() >= 2) {
        	return (RECENT_ITEM_WIDTH + RECENT_ITEM_WIDTH / 2) - SCREEN_WIDTH / 2;
        }else{
            return -LEFT_OVER_SCROLL;
        }
    }

    private int getMinScrollRangeX() {
    	return -LEFT_OVER_SCROLL;
    }

    @SuppressWarnings({"UnusedParameters"})
    protected boolean overScrollBy(int deltaX, int deltaY,
            int scrollX, int scrollY,
            int scrollRangeX, int scrollRangeY,
            int maxOverScrollX, int maxOverScrollY,
            boolean isTouchEvent) {
        final int overScrollMode = getOverScrollMode();
        final boolean canScrollHorizontal =
                computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        final boolean canScrollVertical =
                computeVerticalScrollRange() > computeVerticalScrollExtent();
        final boolean overScrollHorizontal = overScrollMode == OVER_SCROLL_ALWAYS ||
                (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
        final boolean overScrollVertical = overScrollMode == OVER_SCROLL_ALWAYS ||
                (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);

        int newScrollX = scrollX + deltaX;
        if (!overScrollHorizontal) {
            maxOverScrollX = 0;
        }

        int newScrollY = scrollY + deltaY;
        if (!overScrollVertical) {
            maxOverScrollY = 0;
        }

        scrollRangeX = getMaxScrollRangeX();
        // Clamp values if at the limits and record
        // left overscroll 360px. scrollback 200px. final scroll: 360-200=160px 
        final int left = -maxOverScrollX - 360;
        final int right = maxOverScrollX + scrollRangeX + 260;
        final int top = -maxOverScrollY;
        final int bottom = maxOverScrollY + scrollRangeY;

        boolean clampedX = false;
        if (newScrollX > right) {
            newScrollX = right;
            clampedX = true;
        } else if (newScrollX < left) {
            newScrollX = left;
            clampedX = true;
        }

        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }

        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

        return clampedX || clampedY;
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY,
            boolean clampedX, boolean clampedY) {
        // Treat animating scrolls differently; see #computeScroll() for why.
        if (!mScroller.isFinished()) {
            final int oldX = mScrollX;
            final int oldY = mScrollY;
            mScrollX = scrollX;
            mScrollY = scrollY;
            invalidateParentIfNeeded();
            onScrollChanged(mScrollX, mScrollY, oldX, oldY);
            if (clampedX) {
                mScroller.springBack(mScrollX, mScrollY, getMinScrollRangeX(), getMaxScrollRangeX(), 0, 0);
            }
        } else {
            super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        }

        awakenScrollBars();
    }

    private void computePositionAfterEnter() {
    	int count = mLinearLayout.getChildCount();
    	if (count == 1) {
    		// left over scroll: 160px
    		mLastScrollPosition = -LEFT_OVER_SCROLL;
    	}
    	if (count == 2) {
    		// left over scroll: 160px
    		mLastScrollPosition = -LEFT_OVER_SCROLL;
    	}

    	if (count == 3) {
    		mLastScrollPosition = HIDE_PART;
    	}

    	if (count > 3) {
    		// When in landscape, getMeasuredWidth return error. So compute width with number of card
    		// item width: 400px
    		mLastScrollPosition = count * RECENT_ITEM_WIDTH - RECENT_ITEM_WIDTH*2 - LEFT_OVER_SCROLL;
    	}

    }

    private void computeLastPostion(View v) {
        //Added by TYD Sean 2015-11-26 for updating screen scrollX begin
        int indexOfParent = mLinearLayout.indexOfChild(v);

        int count = mLinearLayout.getChildCount();
        int scrollX = getScrollX();
        if((indexOfParent == count - 1) && getScrollX() > 0  || (v.getLeft() < scrollX) && v.getRight() > scrollX){
            mLastScrollPosition = getScrollX() - v.getWidth();
        }else{
           mLastScrollPosition = getScrollX();
        }
        //Added by TYD Sean 2015-11-26 for updating screen scrollX end
    }

    private ArrayList<View> findViewsInScreen() {
    	
    	ArrayList<View> views = new ArrayList<View>();
    	for (int i=0; i<mLinearLayout.getChildCount(); i++) {
    	    View v = mLinearLayout.getChildAt(i);
    	    if (isChildViewInScreen(v)) {
    	    	Log.d("hongchang", "inscreen: " + i);
    	    	views.add(v);
    	    }
    	}
    	return views;
    }
    
    private boolean isChildViewInScreen(View v) {
    	int childLeft = v.getLeft();
    	int childRight = v.getRight();

    	int shifting = -getScrollX();
    	if ((childLeft + shifting) > -RECENT_ITEM_WIDTH && (childRight + shifting) < (RECENT_ITEM_WIDTH + getWidth())) {
    		return true;
    	}
    	return false;
    }

    public View getChildLockFabView(View v) {
    	return v.findViewById(R.id.droi_lock_app_fab);
    }
    
    public void clearChildView(int index) {
    	dismissChild(mLinearLayout.getChildAt(index));
    }

    public void hideVisibleViews() {
    	mClearFlag = true;
        startClearAnimation();
    }

    private boolean touchedLockIcon(int x, int y){
        final ArrayList<View> views = findViewsInScreen();
        for(View view : views){
            if(view != null) {
                View lockIcon = getChildLockFabView(view);
                Rect rect = getBound(lockIcon);
                if(rect.contains(x, y) && y <= getBottom()){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean pointInside(int x, int y, View v) {
        if(v == null){
            return false;
        }
        final int l = v.getLeft();
        final int r = v.getRight();
        final int t = v.getTop();
        final int b = v.getBottom();
        return x >= l && x < r && y >= t && y < b;
    }

    public static Rect getBound(View view){
        Rect rect = new Rect();
        if(view != null){
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            rect.left = location[0];
            rect.top = location[1];
            rect.right = rect.left + view.getWidth();
            rect.bottom = rect.top + view.getHeight();
        }else{
            rect.top = 0;
            rect.left = 0;
            rect.right = 0;
            rect.bottom = 0;
        }
        return rect;
    }

    private void startClearAnimation(){
        final ArrayList<View> views = findViewsInScreen();
        ValueAnimator animator = ValueAnimator.ofFloat(0, -1050);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                for (View view : views) {
                    if(view != null) {
                        RecentsPanelView.ViewHolder holder = (RecentsPanelView.ViewHolder) view.getTag();
                        if (holder != null && holder.taskDescription != null && holder.taskDescription.mIsLocked == false) {
                            view.setTranslationY(value);
                        }
                    }
                }
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mCallback != null) {
                    mCallback.onClearAllViewsDone();
                }
                for (View view : views) {
                    if(view != null){
                        RecentsPanelView.ViewHolder holder = (RecentsPanelView.ViewHolder)view.getTag();
                        if(holder != null && holder.taskDescription != null && holder.taskDescription.mIsLocked == false) {
                            view.setTranslationY(0);
                            view.setVisibility(View.GONE);
                        }
                    }

                }
                RecentTasksLoader recentTasksLoader = RecentTasksLoader.getInstance(getContext());
                if(recentTasksLoader != null) {
                    recentTasksLoader.cancelPreloadingRecentTasksList();
                }
                mClearFlag = false;//reset flag after clear
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.setDuration(500);
        animator.start();
    }

    public boolean getClearFlag(){
        return mClearFlag;
    }

    public void clearAllViews() {
    	
    }

    public void updateChildLockState(View v) {
    	mCallback.onLockStateChagned(v);
    }

    public void updateCircleViewAlpha(float alpha){
    	mCallback.updateCircleViewAlpha(alpha);
    }
    //*/ tyd
}
