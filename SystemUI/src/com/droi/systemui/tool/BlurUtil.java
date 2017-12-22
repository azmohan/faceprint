package com.droi.systemui.tool;

import java.lang.reflect.Method;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class BlurUtil {

    private static final String TAG = "BlurUtil";

    private Context mContext;
    private Display mDisplay;
    private Matrix mDisplayMatrix;
    private DisplayMetrics mDisplayMetrics;

    public BlurUtil() {}
    
    public BlurUtil(Context context) {
        mContext = context;
        initDisplayParams();
    }
    
    public void initDisplayParams() {
        WindowManager mWindowManager = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE));
        mDisplayMatrix = new Matrix();
        mDisplay = mWindowManager.getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();
        mDisplay.getMetrics(mDisplayMetrics);
    }

    public Bitmap takeScreenShot() {
        Bitmap bmp = null;

        mDisplay.getMetrics(mDisplayMetrics);
        float[] dims = {mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels};
        float degrees = getDegreesForRotation(mDisplay.getRotation());
        Log.i(TAG, "takeScreenshot dims = " + dims[0] + "," + dims[1] + " of " + degrees);

        boolean requiresRotation = (degrees > 0);
        if (requiresRotation) {
            // Get the dimensions of the device in its native orientation
            mDisplayMatrix.reset();
            mDisplayMatrix.preRotate(-degrees);
            mDisplayMatrix.mapPoints(dims);
            dims[0] = Math.abs(dims[0]);
            dims[1] = Math.abs(dims[1]);
            Log.i(TAG, "takeScreenshot reqRotate, dims = " + dims[0] + "," + dims[1]);
        }

        try {
            Class<?> demo = Class.forName("android.view.SurfaceControl");
            Method method = demo.getMethod("screenshot", new Class[]{int.class, int.class});
            bmp = (Bitmap) method.invoke(null, (int) dims[0], (int) dims[1]);
            Log.i(TAG, "takescreenshot bmp = " + bmp);

            if (bmp == null) {
                return null;
            }

            if (requiresRotation) {
                // Rotate the screenshot to the current orientation
                Bitmap ss = Bitmap.createBitmap(mDisplayMetrics.widthPixels,
                        mDisplayMetrics.heightPixels, Bitmap.Config.ARGB_4444);
                Canvas c = new Canvas(ss);
                c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
                c.rotate(degrees);
                c.translate(-dims[0] / 2, -dims[1] / 2);
                c.drawBitmap(bmp, 0, 0, null);
                c.setBitmap(null);
                // Recycle the previous bitmap
                bmp.recycle();
                bmp = null;
                bmp = ss;
                Log.i(TAG, "takescreenshot requiresRotation bmp = " + bmp);
            }

            bmp.setHasAlpha(false);
            bmp.prepareToDraw();
        } catch (Exception e) {
            Log.i(TAG, "Exception = " + e);
            e.printStackTrace();
        }

        bmp = small(bmp);

        return bmp;
    }
 
    private float getDegreesForRotation(int value) {
        switch (value) {
            case Surface.ROTATION_90:
                return 360f - 90f;
            case Surface.ROTATION_180:
                return 360f - 180f;
            case Surface.ROTATION_270:
                return 360f - 270f; 
        }

        return 0f;
    }

    private static Bitmap small(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale(0.15f, 0.15f);

        Bitmap resizeBmp = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);

        return resizeBmp;
    }

    public Bitmap blurBitmap() {
        return blurBitmap(takeScreenShot(), mContext);
    }
    
    public Bitmap blurBitmap(Bitmap bitmap, Context context) {
        //Let's create an empty bitmap with the same size of the bitmap we want to blur
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_4444);
        
        //Instantiate a new Renderscript    
        RenderScript rs = RenderScript.create(context);

        //Create an Intrinsic Blur Script using the Renderscript    
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));    

        //Create the Allocations (in/out) with the Renderscript and the in/out bitmaps    
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
        Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);

        //Set the radius of the blur
        blurScript.setRadius(18f);

        //Perform the Renderscript
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);

        //Copy the final bitmap created by the out Allocation to the outBitmap
        allOut.copyTo(outBitmap);

        //recycle the original bitmap
        bitmap.recycle();

        //After finishing everything, we destroy the Renderscript.
        rs.destroy();

        return outBitmap;
    }

}