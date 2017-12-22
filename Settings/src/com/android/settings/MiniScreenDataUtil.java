//*/add by tyd zhanglingzeng for mini screen mode 201409.
package com.android.settings;

import android.content.Context;
import android.database.ContentObserver;
import android.content.ContentResolver;
import android.net.Uri;
import android.database.Cursor;
import android.content.ContentValues;
import android.util.Log;
import android.widget.Toast;
import com.mediatek.settings.FeatureOption;
import android.view.WindowManager;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import java.util.List;


public final class MiniScreenDataUtil{
	private static final String TAG = "MiniScreenDataUtil";
	private static final boolean DEBUGE = true;
	public static final Uri MINI_SCREEN_MODE_URI = Uri.parse("content://com.freeme.minscreen/minimodeswitch");
	private static final String MINI_TB_ITEM_ID = "_id";
	private static final String MINI_TB_ITEM_MINISWITCH = "MODE_SWITCH";
	private static final String MINI_TB_ITEM_NEXT_SWITCH = "NEXT_SWITCH";
	
	public static boolean queryMiniMode(Context context){
		String columns[] = new String[] {MINI_TB_ITEM_ID, MINI_TB_ITEM_MINISWITCH, MINI_TB_ITEM_NEXT_SWITCH}; 
		Uri uri = MINI_SCREEN_MODE_URI;  
		
		ContentResolver contentResolver = null;
		if(context != null){
		    contentResolver =  context.getContentResolver();
		}
		if(contentResolver == null){
		    return false;
		}
	    Cursor cur = contentResolver.query(uri, columns, null, null, null);
		//Cursor cur = context.getContentResolver().query(uri, columns, null, null, null);
		if(cur == null){
			return false;
		}
		
		boolean result = false;
		if (cur.moveToFirst()) {  
			int modeSwitch = -1;
			do {  	
				modeSwitch = cur.getInt(cur.getColumnIndex(MINI_TB_ITEM_MINISWITCH));			
			} while (cur.moveToNext());  
			result = (modeSwitch == 1 ? true : false);
		}
		cur.close();
		return result;
	}

	public static void setMiniModeData(Context context, boolean modeSwitch){
		if(DEBUGE) Log.i(TAG, "setMiniModeData() modeSwitch = " + modeSwitch );
		int modeValue = modeSwitch ? 1 : 0;
		String columns[] = new String[] {MINI_TB_ITEM_ID, MINI_TB_ITEM_MINISWITCH, MINI_TB_ITEM_NEXT_SWITCH}; 
		Uri uri = MINI_SCREEN_MODE_URI;  
		Cursor cur = context.getContentResolver().query(uri, columns, null, null, null);

		if(cur == null){
			ContentValues values = new ContentValues();  
			values.put(MINI_TB_ITEM_MINISWITCH, modeValue); 
			values.put(MINI_TB_ITEM_NEXT_SWITCH, 0);
			context.getContentResolver().insert(MINI_SCREEN_MODE_URI, values); 
		}else {
			if(cur.getCount() == 1){
				updateMiniModeData(context, modeValue, 0);
			}else{
				ContentValues values = new ContentValues();  
				values.put(MINI_TB_ITEM_MINISWITCH, modeValue); 
				values.put(MINI_TB_ITEM_NEXT_SWITCH, 0);
				context.getContentResolver().delete(MINI_SCREEN_MODE_URI, null, null);
				context.getContentResolver().insert(MINI_SCREEN_MODE_URI, values);
			}
		}
		if(cur != null){
			cur.close();
		}
	}

	private static void updateMiniModeData(Context context, int modeValue, int nextMode ){
		ContentValues values = new ContentValues();
		values.put(MINI_TB_ITEM_MINISWITCH, modeValue);
		values.put(MINI_TB_ITEM_NEXT_SWITCH, nextMode);
		context.getContentResolver().update(MINI_SCREEN_MODE_URI, values, null, null);
	}

	public static boolean isMiniScreenServiceRunning(ActivityManager activityManager){
		boolean isServiceRunning = false;
		int defaultNum = 100;
		List<RunningServiceInfo> runServiceList = activityManager.getRunningServices(defaultNum);
		for (RunningServiceInfo serviceinfo : runServiceList){
			//Log.i(TAG, "serviceinfo.process = " + serviceinfo.process);
			if("com.freeme.minscreen".equals(serviceinfo.process)){
				isServiceRunning = true;
				break;
			}
		}
		return isServiceRunning;
	}
	
}
//*/ add end.
