package com.droi.systemui.qs.order.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.droi.systemui.qs.order.DataTools;
import com.droi.systemui.qs.order.adapter.DragAdapter;

public class DragGrid extends GridView {

    // clicked X position
	public int downX;
	// clicekd Y position
	public int downY;
	// clicked X offset
	public int windowX;
	// clicked Y offset
	public int windowY;
	// X in Screen
	private int win_view_x;
	// Y in Screen
	private int win_view_y;
	// X offset when draging
	int dragOffsetX;
	// Y offset when draging
	int dragOffsetY;
	// position when longClicked
	public int dragPosition;
	// position when end move
	private int dropPosition;
	// position when start move
	private int startPosition;
	// position which is moveing
	private int holdPosition;
	
	// item's height width
	private int itemHeight;
	private int itemWidth;
	
	// the Item's view in draging
	private View dragImageView = null;
	
	// the Item's view in longClick
	private ViewGroup dragItemView = null;
	
	private WindowManager windowManager = null;
	private WindowManager.LayoutParams windowParams = null;
	
	private int nColumns = 5;
	private int remainder;
	private int mSortItemPos = -1;

	private boolean isMoving = false;

	private double dragScale = 1.2D;
	private Vibrator mVibrator;
	
	// gap in every item
	private int mHorizontalSpacing = 15;
	private int mVerticalSpacing = 15;

	private String LastAnimationID;
	
	public DragGrid(Context context) {
		super(context);
		init(context);
	}

	public DragGrid(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public DragGrid(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public void init(Context context){
	    mSortItemPos = context.getResources().getInteger(R.integer.config_qs_sort_item_position);
	    
		mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		mHorizontalSpacing = DataTools.dip2px(context, mHorizontalSpacing);
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
			downX = (int) ev.getX();
			downY = (int) ev.getY();
			windowX = (int) ev.getX();
			windowY = (int) ev.getY();
			setOnItemClickListener(ev);
		}
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {

		boolean bool = true;
		if (dragImageView != null && dragPosition != AdapterView.INVALID_POSITION) {

			bool = super.onTouchEvent(ev);
			int x = (int) ev.getX();
			int y = (int) ev.getY();
			
			switch (ev.getAction()) {
			    case MotionEvent.ACTION_DOWN:
				    downX = (int) ev.getX();
				    windowX = (int) ev.getX();
				    downY = (int) ev.getY();
				    windowY = (int) ev.getY();
				    break;
			    case MotionEvent.ACTION_MOVE:
				    onDrag(x, y ,(int) ev.getRawX() , (int) ev.getRawY());
				    if (!isMoving){
				        onMove(x, y);
				    }
				    break;
			    case MotionEvent.ACTION_UP:
				    stopDrag();
				    onDrop(x, y);
				    break;

			    default:
			        break;
			}
		}
		return super.onTouchEvent(ev);
	}
	
	private void onDrag(int x, int y , int rawx , int rawy) {
		if (dragImageView != null) {
			windowParams.alpha = 0.6f;
			windowParams.x = rawx - win_view_x;
			windowParams.y = rawy - win_view_y;
			windowManager.updateViewLayout(dragImageView, windowParams);
		}
	}

	/** after UP Event */
	private void onDrop(final int x, final int y) {
	    if(isMoving) {
            new Handler().postDelayed(new Runnable(){
                public void run() {
                    onDrop(x, y);
                }
            }, 100L);

            return;
	    }

	    int tempPostion = pointToPosition(x, y); // change the (x,y) -->Item's Position
	    dropPosition = tempPostion;
	    DragAdapter mDragAdapter = (DragAdapter) getAdapter();
	    mDragAdapter.setShowDropItem(true); // show the Item whick is dragged
	    mDragAdapter.notifyDataSetChanged();
        requestDisallowInterceptTouchEvent(false);
	}
	
	/** longClick */
	public void setOnItemClickListener(final MotionEvent ev) {
		setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				int x = (int) ev.getX();
				int y = (int) ev.getY();

				startPosition = position;// here start moving
				dragPosition = position;
				
				if (startPosition == mSortItemPos) {// should not long click "Sort" button
					return false;
				}
				
				ViewGroup dragViewGroup = (ViewGroup) getChildAt(dragPosition - getFirstVisiblePosition());
				TextView dragTextView = (TextView)dragViewGroup.findViewById(R.id.text_item);
				dragTextView.setSelected(true);
				dragTextView.setEnabled(false);
				itemHeight = dragViewGroup.getHeight();
				itemWidth = dragViewGroup.getWidth();

				if (dragPosition != AdapterView.INVALID_POSITION) {
					win_view_x = windowX - dragViewGroup.getLeft();
					win_view_y = windowY - dragViewGroup.getTop();
					dragOffsetX = (int) (ev.getRawX() - x);
					dragOffsetY = (int) (ev.getRawY() - y);
					dragItemView = dragViewGroup;
					dragViewGroup.destroyDrawingCache();
					dragViewGroup.setDrawingCacheEnabled(true);
					Bitmap dragBitmap = Bitmap.createBitmap(dragViewGroup.getDrawingCache());
					mVibrator.vibrate(50);
					startDrag(dragBitmap, (int)ev.getRawX(), (int)ev.getRawY());
					hideDropItem();
					dragViewGroup.setVisibility(View.INVISIBLE);
					isMoving = false;
					requestDisallowInterceptTouchEvent(true);
					return true;
				}
				return false;
			}
		});
	}

	public void startDrag(Bitmap dragBitmap, int x, int y) {
		stopDrag();
		
		windowParams = new WindowManager.LayoutParams();
		windowParams.gravity = Gravity.TOP | Gravity.LEFT;
		windowParams.x = x - win_view_x;
		windowParams.y = y  - win_view_y;

		windowParams.width = (int) (dragScale * dragBitmap.getWidth());
		windowParams.height = (int) (dragScale * dragBitmap.getHeight());
		this.windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		this.windowParams.format = PixelFormat.TRANSLUCENT;
		this.windowParams.windowAnimations = 0;
		ImageView iv = new ImageView(getContext());
		iv.setImageBitmap(dragBitmap);
		windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		windowManager.addView(iv, windowParams);
		dragImageView = iv;
	}

	private void stopDrag() {
		if (dragImageView != null) {
			windowManager.removeView(dragImageView);
			dragImageView = null;
		}
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,MeasureSpec.AT_MOST);
		super.onMeasure(widthMeasureSpec, expandSpec);
	}

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        /*
        final String tip = getResources().getString(R.string.qs_order_sort_tip);
        
        float h = getHeight();
        float w = getWidth();

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        
        paint.setColor(getResources().getColor(com.android.internal.R.color.black));
        paint.setAlpha(56);
        canvas.drawRect(0, 0 , w , h/2, paint);
        
        paint.setColor(Color.WHITE);
        paint.setTextSize(getResources().getDimensionPixelSize(R.dimen.qs_tile_text_size));
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setAlpha(255);

        Rect rectTip = new Rect();
        paint.getTextBounds(tip, 0, tip.length(), rectTip);

        int tipHeigth = rectTip.bottom - rectTip.top;
        float tipX = getLeft() + getResources().getDimensionPixelSize(R.dimen.qs_tile_text_size);
        float tipY = h/2 + tipHeigth;
        
        canvas.drawText(tip, tipX , tipY , paint);
        */

    }

	private void hideDropItem() {
		((DragAdapter) getAdapter()).setShowDropItem(false);
	}
	
	public Animation getMoveAnimation(float toXValue, float toYValue) {
		TranslateAnimation mTranslateAnimation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0F,
				Animation.RELATIVE_TO_SELF,toXValue, 
				Animation.RELATIVE_TO_SELF, 0.0F,
				Animation.RELATIVE_TO_SELF, toYValue);
		mTranslateAnimation.setFillAfter(true);
		mTranslateAnimation.setDuration(200L);
		return mTranslateAnimation;
	}
	 
	public void onMove(int x, int y) {

		int dPosition = pointToPosition(x, y);

		if (dPosition > -1 && dPosition != mSortItemPos) {

		    dropPosition = dPosition;
		    if (dragPosition != startPosition){
		        dragPosition = startPosition;
		    }
		    int movecount;

		    if ((dragPosition == startPosition) || (dragPosition != dropPosition)){
		        movecount = dropPosition - dragPosition;
		    }else{
		        movecount = 0;
		    }

		    if(0 == movecount) {
                return;
		    }
		    
		    int movecount_abs = Math.abs(movecount);
			if (dPosition != dragPosition) {
				ViewGroup dragGroup = (ViewGroup) getChildAt(dragPosition);
				dragGroup.setVisibility(View.INVISIBLE);
				float to_x = 1;
				float to_y;
				float x_value = ((float) mHorizontalSpacing / (float) itemWidth) + 1.0f;
				float y_value = ((float) mVerticalSpacing / (float) itemHeight) + 1.0f;
				Log.d("x_value", "x_value = " + x_value);

                DragAdapter mDragAdapter = (DragAdapter) getAdapter();

				for (int i = 0; i < movecount_abs; i++) {
					 to_x = x_value;
					 to_y = y_value;

					if (movecount > 0) { // drag andr drop back
						holdPosition = dragPosition + i + 1;

                        /*/ freeme, gouzhouping. 20160817, for optimization in order activity.
						if(19 == holdPosition) {
						/*/
						if(18 == holdPosition) {
						//*/
                            continue;
						} else if (holdPosition - 1 == mSortItemPos) {
                            to_x = (nColumns - 1 - 1) * x_value;
                            to_y = - y_value;
                            mDragAdapter.exchangeExceptMore(holdPosition, mSortItemPos);
						} else if (dragPosition / nColumns == holdPosition / nColumns) { // at same line
							to_x = - y_value;
							to_y = 0;
						} else if (holdPosition % nColumns == 0) { // at different line's column 0
							to_x = (nColumns - 1) * x_value;
							to_y = - y_value;
						} else {
							to_x = - x_value;
							to_y = 0;
						}
					} else { // drag and drop forward
						holdPosition = dragPosition - i - 1;

						/*/ freeme, gouzhouping. 20160817, for optimization in order activity.
						if(19 == holdPosition) {
						/*/
						if(18 == holdPosition) {
						//*/
                            continue;
						} else if (holdPosition + 1 == mSortItemPos){
                            to_x = (nColumns - 1 - 1) * x_value * (-1);
                            to_y = y_value;
                            mDragAdapter.exchangeExceptMore(holdPosition, 19);
						} else if (dragPosition / nColumns == holdPosition / nColumns) {
							to_x = x_value;
							to_y = 0;
						} else if((holdPosition + 1) % nColumns == 0){
							to_x = (nColumns - 1) * x_value * (-1);
							to_y = y_value;
						}else{
							to_x = x_value;
							to_y = 0;
						}
					}

                    ViewGroup moveViewGroup = (ViewGroup) getChildAt(holdPosition);
                    Animation moveAnimation = getMoveAnimation(to_x, to_y);
                    moveViewGroup.startAnimation(moveAnimation);
                    if (holdPosition == dropPosition) {
                        LastAnimationID = moveAnimation.toString();
                    }
                    moveAnimation.setAnimationListener(new AnimationListener() {
                    
                        @Override
                        public void onAnimationStart(Animation animation) {
                            isMoving = true;
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if (animation.toString().equalsIgnoreCase(LastAnimationID)) {
                                DragAdapter mDragAdapter = (DragAdapter) getAdapter();
                                mDragAdapter.exchange(startPosition,dropPosition);
                                startPosition = dropPosition;
                                dragPosition = dropPosition;
                                isMoving = false;
                            }
                        }
                    });
				}
			}
		}
	}
}
