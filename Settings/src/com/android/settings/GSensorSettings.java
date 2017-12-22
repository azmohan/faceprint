package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.TargetView;
import com.freeme.internal.server.INativeMiscService;
import com.mediatek.HobbyDB.CustomHobbyService;

public class GSensorSettings extends Activity implements View.OnClickListener {
    private static final String TAG = "GSensorSettings";

    private class BallEventSource implements SensorEventListener {
        private SensorManager mSensorManager;
        private Sensor mSensor;
        private boolean mRegisted;

        private static final int SAMPLE_SIZE = 6;
        private int mSampleCount;
        private final PointF mSamplePoint;

        BallEventSource(Context context) {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            mSamplePoint = new PointF();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            final float[] values = event.values;

            float px = 10 * (values[0] * values[0]);
            if (values[0] < 0) px = -px;

            float py = 10 * (values[1] * values[1]);
            if (values[1] > 0) py = -py;

            if (mSampleCount == 0) {
                ++mSampleCount;
                mSamplePoint.set(px, py);
            } else if (mSampleCount < SAMPLE_SIZE) {
                ++mSampleCount;
                PointF o = mSamplePoint;
                float spx = (o.x + px) / 2;
                float spy = (o.y + py) / 2;
                mSamplePoint.set(spx, spy);
            } else {
                mSampleCount = 0;
                mTargetView.moveBall(mSamplePoint);
            }
        }
        
        void register() {
            mSampleCount = 0;
            mRegisted = mSensorManager.registerListener(this, mSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

        void unregister() {
            if (mRegisted) {
                mRegisted = false;
                mSensorManager.unregisterListener(this);
            }
        }
    }

    private BallEventSource mBallEventSource;
    
    private TextView mSensorGuide;
    private Button mSensorCalibrate;
    private TargetView mTargetView;

    private static final int ANIMATION_TIME = 50;
    private static final int ANIMATION_COUNT = 40;

    private static final int MSG_CALIBRATE_READY    = 1;
    private static final int MSG_CALIBRATE_UPDATE   = 2;
    private static final int MSG_CALIBRATE_COMPLETE = 3;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CALIBRATE_READY: {
                    mBallEventSource.unregister();

                    mSensorGuide.setText(R.string.gsensor_calibrating);
                    mSensorCalibrate.setEnabled(false);
                    mTargetView.setBallState(TargetView.STATE_STEPPING, ANIMATION_COUNT);

                    removeMessages(MSG_CALIBRATE_READY);
                    sendEmptyMessageDelayed(MSG_CALIBRATE_UPDATE, ANIMATION_TIME);

                    break;
                }

                case MSG_CALIBRATE_UPDATE: {
                    sendEmptyMessageDelayed(
                            mTargetView.stepBall() ? MSG_CALIBRATE_COMPLETE : MSG_CALIBRATE_UPDATE,
                            ANIMATION_TIME);

                    break;
                }

                case MSG_CALIBRATE_COMPLETE: {
                    removeMessages(MSG_CALIBRATE_COMPLETE);

                    final boolean success = runCalibration();

                    mSensorGuide.setText(R.string.gsensor_ready);
                    mSensorCalibrate.setEnabled(true);

                    mTargetView.setBallState(TargetView.STATE_NORMAL, 0);

                    Toast.makeText(GSensorSettings.this,
                            success ? R.string.gsensor_end : R.string.gsensor_end_width_failed,
                            Toast.LENGTH_SHORT).show();

                    mBallEventSource.register();

                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.gsensor);
        
        mSensorGuide = (TextView) findViewById(R.id.guide);

        mSensorCalibrate = (Button) findViewById(R.id.calibrate);
        mSensorCalibrate.setOnClickListener(this);

        mTargetView = (TargetView) findViewById(R.id.target);

        mBallEventSource = new BallEventSource(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        mSensorGuide.setText(R.string.gsensor_ready);
        mSensorCalibrate.setEnabled(true);

        mTargetView.setBallState(TargetView.STATE_NORMAL, 0);

        mBallEventSource.register();
    }
    
    @Override
    protected void onPause() {
        mBallEventSource.unregister();
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }
    
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.calibrate: {
                mHandler.sendEmptyMessage(MSG_CALIBRATE_READY);
                break;
            }
        }
    }
    
    public boolean runCalibration() {
        INativeMiscService nms = INativeMiscService.Impl.getInstance();
        try {
            return nms.runSensorCali(INativeMiscService.CALI_GSENSOR) == 0;
        } catch (RemoteException e) {
            return false;
        }
    }
}
