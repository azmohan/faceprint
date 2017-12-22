package com.droi.recent;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;

public class DroiHorizontalScrollView extends HorizontalScrollView {
	protected boolean mIsBeingDragged = false;
	private int mActivePointerId = INVALID_POINTER;
	private static final int INVALID_POINTER = -1;
	private int mLastMotionX;
	private int mTouchSlop;
	private int mMinimumVelocity;
	private int mMaximumVelocity;
	private int mCurrentPage = 0;
	private static final int PAGE_WIDTH = 400;
	private int SCREEN_WIDTH;
	private VelocityTracker mVelocityTracker;
	
	public DroiHorizontalScrollView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init(context);
	}

	public DroiHorizontalScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init(context);
	}

	public DroiHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		init(context);
	}

	public DroiHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		// TODO Auto-generated constructor stub
		init(context);
	}
	
	private void init(Context context){
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mCurrentPage = 0;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        SCREEN_WIDTH = displayMetrics.widthPixels;
	}
	
    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }
	
    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }
	
    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            return true;
        }
        
        switch (action & MotionEvent.ACTION_MASK) {
	        case MotionEvent.ACTION_MOVE: {
	        	 final int activePointerId = mActivePointerId;
	             if (activePointerId == INVALID_POINTER) {
	                 // If we don't have a valid id, the touch down wasn't on content.
	                 break;
	             }
	
	             final int pointerIndex = ev.findPointerIndex(activePointerId);
	             if (pointerIndex == -1) {
	                 break;
	             }
	
	             final int x = (int) ev.getX(pointerIndex);
	             final int xDiff = (int) Math.abs(x - mLastMotionX);
	             if (xDiff > mTouchSlop) {
	                 mIsBeingDragged = true;
	                 mLastMotionX = x;
	                    initVelocityTrackerIfNotExists();
	                    mVelocityTracker.addMovement(ev);
	             }
	             break;
	        }
	        
	        case MotionEvent.ACTION_DOWN: {
	        	final int x = (int) ev.getX();
                if (!inChild((int) x, (int) ev.getY())) {
                    mIsBeingDragged = false;
                    recycleVelocityTracker();
                    break;
                }
                mLastMotionX = x;
                mActivePointerId = ev.getPointerId(0);
                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
	        	break;
	        }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:{
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
            	break;
            }
	        case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = ev.getActionIndex();
                mLastMotionX = (int) ev.getX(index);
                mActivePointerId = ev.getPointerId(index);
	        	break;
	        }
	        case MotionEvent.ACTION_POINTER_UP:{
	        	mLastMotionX = (int) ev.getX(ev.findPointerIndex(mActivePointerId));
	        	break;
	        }
        }
        return super.onInterceptTouchEvent(ev);
    }
	
    private boolean inChild(int x, int y) {
        if (getChildCount() > 0) {
            final int scrollX = getScrollX();
            final View child = getChildAt(0);
            return !(y < child.getTop()
                    || y >= child.getBottom()
                    || x < child.getLeft() - scrollX
                    || x >= child.getRight() - scrollX);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
    	// TODO Auto-generated method stub
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(ev);
        
    	final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
	        case MotionEvent.ACTION_DOWN: {
                if (getChildCount() == 0) {
                    return false;
                }
                mLastMotionX = (int) ev.getX();
                mActivePointerId = ev.getPointerId(0);
	        	break;
	        }
	        case MotionEvent.ACTION_MOVE:{
	        	final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
	        	if (activePointerIndex == -1) {
	        		break;
	        	}
                final int x = (int) ev.getX(activePointerIndex);
                int deltaX = mLastMotionX - x;
                if (!mIsBeingDragged && Math.abs(deltaX) > mTouchSlop) {
                	mIsBeingDragged = true;
                }
                if (mIsBeingDragged) {
                	mLastMotionX = x;
                }
	        	break;
	        }
	        case MotionEvent.ACTION_UP:
	        case MotionEvent.ACTION_CANCEL:{
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int initialVelocity = (int) velocityTracker.getXVelocity(mActivePointerId);
	        	if (mIsBeingDragged) {
	        		int newScrollX = getNewScrollX();
	        		//if ((Math.abs(initialVelocity) > mMinimumVelocity) == false) {
	        			//smoothScrollTo(newScrollX, getScrollY());
	        		//}
	        		recycleVelocityTracker();
                    mActivePointerId = INVALID_POINTER;

                    mIsBeingDragged = false;
					//recentOnTouchEventOfUp();
					//return true;
	        	}
	        	break;
	        }
	        case MotionEvent.ACTION_POINTER_UP:{
	            final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		        final int pointerId = ev.getPointerId(pointerIndex);
		        if (pointerId == mActivePointerId) {
		            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
		            mLastMotionX = (int) ev.getX(newPointerIndex);
		            mActivePointerId = ev.getPointerId(newPointerIndex);
		            if (mVelocityTracker != null) {
		                mVelocityTracker.clear();
		            }
		        }
	        	break;
	        }
        }
    	return super.onTouchEvent(ev);
    }
    
    private int getNewScrollX(){
    	int result = 0;
    	if(getRecentsCount() > 1){
    		int pageIndex = getNearestPage();
    		View pageView = getPageAt(pageIndex);
    		if(pageView != null){
    			int pageCenter = pageView.getLeft() + PAGE_WIDTH / 2;
    			int screenCenter = getScrollX() + SCREEN_WIDTH / 2;
    			return getScrollX() - (screenCenter - pageCenter);
    		}
    	}else if(getRecentsCount() == 1){
    		return  - (SCREEN_WIDTH - PAGE_WIDTH) / 2;
    	}
    	return result;
    }
    
    private int getNearestPage(){
    	int leftedge = getScrollX();
        int rightedge = PAGE_WIDTH + getScrollX();
        
        int leftIndex = getPageIndexForScrollX(leftedge);
        int rightIndex = getPageIndexForScrollX(rightedge);
        if (leftIndex == rightIndex) {
            return leftIndex;
        }
        int nearestIndex = 0;
        View leftPage = getPageAt(leftIndex);
        View rightPage = getPageAt(rightIndex);
        
        if ((leftPage != null) && (rightPage != null)) {
        	int leftPageCenter = leftPage.getLeft() + PAGE_WIDTH / 2;
        	int rightPageCenter = rightPage.getLeft() + PAGE_WIDTH / 2;
        	int currentCenter = getScrollX() + (SCREEN_WIDTH / 2);
            int ldx = Math.abs((currentCenter - leftPageCenter));
            int rdx = Math.abs((rightPageCenter - currentCenter));
            if(ldx < rdx){
            	nearestIndex = leftIndex;
            }else{
            	nearestIndex = rightIndex;
            }
        }
        return nearestIndex;
    }
    
    private int getPageIndexForScrollX(int scrollX){
    	int pageCount = getRecentsCount();
    	int slot = getSlotForScrollX(scrollX);
    	if(getRecentsCount() > 1){
            return slot % pageCount;
    	}
    	return slot;
    }
    
    private int getSlotForScrollX(int scrollX){
    	if(getRecentsCount() > 1){
    		if(scrollX < 0){
    			return (Math.abs(scrollX) - 1) / PAGE_WIDTH;
    		}else{
    			return scrollX / PAGE_WIDTH;
    		}
    	}else if(getRecentsCount() == 1){
    		return 0;
    	}
    	return 0;
    }
    
    private View getPageAt(int index){
    	if(getChildCount() > 0){
    		ViewGroup child = (ViewGroup) getChildAt(0);
    		if(child != null){
    			return child.getChildAt(index);
    		}
    	}
    	return null;
    }
    
    private int getRecentsCount(){
    	int count = 0;
    	ViewGroup child = (ViewGroup) getChildAt(0);
    	if(child != null){
    		count = child.getChildCount();
    	}
    	return count;
    }

	/*
	@Override
	public void fling(int velocityX) {
		if (getChildCount() > 0) {
			int width = getWidth() - mPaddingRight - mPaddingLeft;
			int right = getChildAt(0).getWidth();

			int min = getMinScrollRangeX();
			int max = getMaxScrollRangeX();
			getScroller().fling(mScrollX, mScrollY, velocityX, 0, min,
					max, 0, 0, width / 2, 0);

			final boolean movingRight = velocityX > 0;

			View currentFocused = findFocus();
			View newFocused = findFocusableViewInMyBounds(movingRight,
					getScroller().getFinalX(), currentFocused);

			if (newFocused == null) {
				newFocused = this;
			}

			if (newFocused != currentFocused) {
				newFocused.requestFocus(movingRight ? View.FOCUS_RIGHT : View.FOCUS_LEFT);
			}

			postInvalidateOnAnimation();
		}
	}*/

	private int getMinScrollRangeX() {
		return -160;
	}

	private int getMaxScrollRangeX() {
		int scrollRange = 0;
		if (getChildCount() > 0) {
			View child = getChildAt(0);
			scrollRange = Math.max(0,
					child.getWidth() - (getWidth() - getPaddingLeft() - getPaddingRight()));
		}

		return scrollRange + 160;
	}

	/*
	private void recentOnTouchEventOfUp() {
		int scrollX = getScrollX();
		int scrollY = getScrollY();

		if (scrollX > getMaxScrollRangeX() || scrollX < getMinScrollRangeX()) {
			if (getScroller().springBack(mScrollX, scrollY, getMinScrollRangeX(),
					getMaxScrollRangeX(), 0, 0)) {
				postInvalidateOnAnimation();
				return;
			}
		}

		int finalX = scrollX;
		int num = scrollX / 400;
		int edge = scrollX % 400;
		if (scrollX < 0) {
			finalX = -160;
		} else {
			if (edge < 240) {
				finalX = 400*num - 160;
			} else {
				finalX = 400*num + 240;
			}
		}
		overScrollBy(finalX - scrollX, 0, scrollX, scrollY, getMaxScrollRangeX()
				,0 , 0, 0, true);
	}*/
}
