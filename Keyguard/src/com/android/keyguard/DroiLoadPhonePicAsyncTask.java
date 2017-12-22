package com.android.keyguard;

import java.io.InputStream;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.View;

/*
 * add by huangyiquan 20141121
 * params:1.View object
 * 		  2.personId for phone lookup
 * 		  3.width for show
 * 	      4.height for show
 *        5.default pic for show
*/
public class DroiLoadPhonePicAsyncTask extends AsyncTask<Object,Void,Void>{

	private static final String TAG = "LoadPhonePicAsyncTask";
	private static final boolean DEBUG = true;
	private Context mContext;
	private Bitmap personIcon = null;
	private View localView = null;
	private long personId = -1L;
	private int mWidth = 100;
	private int mHeight = 100;
	private Drawable mDefault = null;
	
	protected void onPostExecute(Void result) {
		// TODO Auto-generated method stub
		if(localView != null && personIcon != null){
			localView.setBackgroundDrawable(new BitmapDrawable(personIcon));
		}
		//clearDrawable();
	}

	@Override
	protected void onCancelled() {
		// TODO Auto-generated method stub
		super.onCancelled();
		//clearDrawable();
	}

	@Override
	protected void onPreExecute() {
		// TODO Auto-generated method stub
		super.onPreExecute();
	}

	@Override
	protected Void doInBackground(Object... params) {
		// TODO Auto-generated method stub
		localView = (View)params[0];
		personId = (Long)params[1];
		mWidth = (Integer) params[2];
		mHeight = (Integer) params[3];
		mDefault = (Drawable) params[4];
		mContext = localView.getContext();
		Uri personUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
				personId);
		if (personUri != null) {
			InputStream inputStream = null;
			try {
				inputStream = Contacts.openContactPhotoInputStream(
						mContext.getContentResolver(), personUri);
			} catch (Exception e) {
				Log.e(TAG, "Error opening photo input stream", e);
			}
			if (inputStream != null) {
				Drawable photo = Drawable.createFromStream(inputStream,
						personUri.toString());
				personIcon = getPhotoIconWhenAppropriate(photo);
				return null;
			}
		}
		personIcon = getPhotoIconWhenAppropriate(mDefault);
		return null;
	}
	
	private Bitmap getPhotoIconWhenAppropriate(Drawable photo) {
		if(photo == null)
			return null;
	     if (!(photo instanceof BitmapDrawable)) {
	         return null;
	     }
	     Bitmap orgBitmap = ((BitmapDrawable) photo).getBitmap();
	     int orgWidth = orgBitmap.getWidth();
	     int orgHeight = orgBitmap.getHeight();
	     float ratio = Math.max((mWidth*1.0f/orgWidth), (mHeight*1.0f/orgHeight));
	     mWidth = (int) (orgWidth*ratio);
	     mHeight = (int) (orgHeight*ratio);
	     return Bitmap.createScaledBitmap(orgBitmap, mWidth, mHeight, true);
	 }
	
	private void clearDrawable(){
		if(personIcon != null){
			personIcon.recycle();
			personIcon = null;
		}
		if(mDefault != null){
			mDefault.setCallback(null);
			mDefault = null;
		}
	}
	
}