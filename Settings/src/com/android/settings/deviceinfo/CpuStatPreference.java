package com.android.settings.deviceinfo;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.android.settings.deviceinfo.CpuStat.CpuInfo;

import android.content.Context;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.android.settings.R;

public class CpuStatPreference extends Preference {

	private LinearLayout mCpuStatParent;
	private LinearLayout mCpuStatParentLineTwo;
	private int numCore = CpuManager.getNumCores();
	private CpuStat mCpuStat = new CpuStat();
	private ArrayList<CpuCurveView> CpuCurveViews;

	Handler mHandler = new Handler();
	Timer mTimer;
	TimerTask mTimerTask;

	public CpuStatPreference(Context context) {
		super(context);
		setLayoutResource(R.layout.cpu_stat_preference);
	}

	public CpuStatPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayoutResource(R.layout.cpu_stat_preference);
	}

	public CpuStatPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setLayoutResource(R.layout.cpu_stat_preference);
	}


	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		mCpuStatParent = (LinearLayout) view.findViewById(R.id.cpu_stat_parent_first_line);
		mCpuStatParentLineTwo = (LinearLayout) view.findViewById(R.id.cpu_stat_parent_second_line);
		CpuCurveViews = new ArrayList<CpuCurveView>();
		int numCorePerLine = numCore;
		if(numCore > 4){
			mCpuStatParentLineTwo.setVisibility(View.VISIBLE);
			numCorePerLine = numCore%2==1 ? numCore/2+1 : numCore/2;
		}
		for (int i = 0; i < numCorePerLine; i++) {
			LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.weight = 1;
			int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getContext().getResources().getDisplayMetrics());
			params.setMargins(margin, margin, margin, margin);
			CpuCurveView cpuCurveView = new CpuCurveView(getContext());
			cpuCurveView.setLineColor(0xff58BBED); 
			cpuCurveView.setBackgroundResource(R.drawable.cpu_frep_background);
			mCpuStatParent.addView(cpuCurveView,params);
			CpuCurveViews.add(cpuCurveView);
		}
		if(numCore > 4){
			for (int i = 0; i < numCore - numCorePerLine; i++) {
				LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.weight = 1;
				int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getContext().getResources().getDisplayMetrics());
				params.setMargins(margin, margin, margin, margin);
				CpuCurveView cpuCurveView = new CpuCurveView(getContext());
				cpuCurveView.setLineColor(0xff58BBED); 
				cpuCurveView.setBackgroundResource(R.drawable.cpu_frep_background);
				mCpuStatParentLineTwo.addView(cpuCurveView,params);
				CpuCurveViews.add(cpuCurveView);
			}			
		}
		if (com.mediatek.settings.FeatureOption.FREEME_CPU_INFO_FAKE) {
			mCpuStatParent.setVisibility(View.GONE);
			mCpuStatParentLineTwo.setVisibility(View.GONE);
		}
	}

	public void startDrawCpuStat() {
		Log.e("Greg", "startDrawCpuStat");
		
		mTimerTask = new TimerTask() {

			@Override
			public void run() {
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						ArrayList<CpuInfo> mCpuInfos = mCpuStat.getCpuInfoList();
						final int size = Math.min(mCpuInfos.size(), numCore);
						for (int i = 0; i < size; i++) {
							if (mCpuStatParent != null) {
								CpuCurveView cpuCurveView = CpuCurveViews.get(i);
								CpuInfo cpuInfo = mCpuInfos.get(i);
								if (cpuCurveView != null) {
									cpuCurveView.setPoint(cpuInfo.getUsage());
								}
							}
						}
						if (mCpuInfos.size() < numCore) {
							for (int i = numCore - mCpuInfos.size(); i < numCore; i++) {
								if (mCpuStatParent != null) {
									CpuCurveView cpuCurveView = (CpuCurveView) mCpuStatParent.getChildAt(i);
									if (cpuCurveView != null) {
										cpuCurveView.setPoint(0);
									}
								}
							}
						}
						/*/
						StringBuffer buffer = new StringBuffer();
						for (int i = 0; i < mCpuInfos.size(); i++) {
							buffer.append(mCpuInfos.get(i).getUsage());
							buffer.append(",");
						}
						Log.e("Greg", "buffer : " + buffer);
						//*/
					}
				});
			}
		};
		mTimer = new Timer();
		mTimer.schedule(mTimerTask, 1000, 1000);
	}

	public void stopDrawCpuStat() {
		Log.e("Greg", "stopDrawCpuStat");
		if (mTimer != null) {
			mTimer.cancel();
		}
		mTimer = null;
		mTimerTask = null;
	}
}
