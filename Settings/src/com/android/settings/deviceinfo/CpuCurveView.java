package com.android.settings.deviceinfo;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class CpuCurveView extends View{

	private int maxPercent = 100;
	private int minPercent = 0;
	private int pointsNum = 10;
	
	private int mWidth = 0;
	private int mHeight = 0;
	
	private int minWidth = 40;
	private int minHeight = 50;
	
	private int mStrokeWidth = 3;
	private int padding = 2;
	
	private int lineColor = Color.BLUE;
	private int rectColor = Color.GRAY;
	
	ArrayList<Float> points = new ArrayList<Float>(pointsNum+1);
	
	Paint mPaint = new Paint();

	public CpuCurveView(Context context) {
		super(context);
		init();
	}
	
	public CpuCurveView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	private void init(){
		mPaint.setAntiAlias(true);
		mPaint.setStrokeWidth(mStrokeWidth);
		mPaint.setStyle(Paint.Style.FILL);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		int size = points.size();
		float yStep = (mHeight*1.0f - padding*2)/(maxPercent - minPercent);
		float xStep = mWidth*1.0f/pointsNum;
		float ySpace = mHeight*1.0f/pointsNum;
		//draw background
		//mPaint.setColor(Color.GREEN);
		//canvas.drawRect(0, 0, mWidth, mHeight, mPaint);
		
		mPaint.setColor(lineColor);
		mPaint.setStrokeWidth(mStrokeWidth*2);
		canvas.drawLine(0, 0, 0, mHeight, mPaint);
		canvas.drawLine(0, mHeight, mWidth, mHeight, mPaint);
		
		/*/
		mPaint.setStrokeWidth(mStrokeWidth*1.0f/4);
		mPaint.setColor(rectColor);
		for (int i = 0; i <= pointsNum; i++) {
			canvas.drawLine(xStep*i, 0, xStep*i, mHeight, mPaint);
			canvas.drawLine(0, ySpace*i, mWidth, ySpace*i, mPaint);
		}
		//*/
		
		mPaint.setColor(lineColor);
		mPaint.setStrokeWidth(mStrokeWidth);
		for (int i = 0; i < size - 1 ; i++) {
			canvas.drawLine(xStep*i, (maxPercent - points.get(i))*yStep + padding, xStep*(i+1), (maxPercent - points.get(i+1))*yStep + padding, mPaint);
			canvas.drawPoint(xStep*i, (maxPercent - points.get(i))*yStep + padding, mPaint);
		}
		super.onDraw(canvas);
	}
	
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);
		int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);
		
		mWidth = Math.max(minWidth, widthSpecSize);
		mHeight = Math.max(minHeight, heightSpecSize);
        setMeasuredDimension(mWidth, mHeight);
	}
	
	public int getMaxPercent() {
		return maxPercent;
	}

	public void setMaxPercent(int maxPercent) {
		this.maxPercent = maxPercent;
	}

	public int getMinPercent() {
		return minPercent;
	}

	public void setMinPercent(int minPercent) {
		this.minPercent = minPercent;
	}
	
	public void setPoint(float perfcent){
		perfcent = Math.min(maxPercent, Math.max(perfcent, minPercent));
		
		int size = points.size();
		if (size >= pointsNum+1){
			@SuppressWarnings("unchecked")
			ArrayList<Float> tempPoints = (ArrayList<Float>) points.clone();
			points.clear();
 			for (int i = 0; i < pointsNum; i++) {
				points.add(i, tempPoints.get(i+1));
			}
 			points.add(pointsNum, perfcent);
		}else {
			points.add(perfcent);
		}
		postInvalidate();
	}

	public int getMinWidth() {
		return minWidth;
	}

	public void setMinWidth(int minWidth) {
		this.minWidth = minWidth;
	}

	public int getMinHeight() {
		return minHeight;
	}

	public void setMinHeight(int minHeight) {
		this.minHeight = minHeight;
	}

	public int getStrokeWidth() {
		return mStrokeWidth;
	}

	public void setStrokeWidth(int mStrokeWidth) {
		this.mStrokeWidth = mStrokeWidth;
	}

	public int getLineColor() {
		return lineColor;
	}

	public void setLineColor(int lineColor) {
		this.lineColor = lineColor;
	}

	public int getRectColor() {
		return rectColor;
	}

	public void setRectColor(int rectColor) {
		this.rectColor = rectColor;
	}
}
