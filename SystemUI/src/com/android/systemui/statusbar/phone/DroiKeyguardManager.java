package com.android.systemui.statusbar.phone;



import java.io.File;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.WallpaperManager;
import com.android.internal.widget.LockPatternUtils;
import com.mediatek.keyguard.PowerOffAlarm.PowerOffAlarmManager ;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.statusbar.policy.PreviewInflater;
import android.app.ActivityManager;

public class DroiKeyguardManager {
    private static final Intent SECURE_CAMERA_INTENT =
            new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE)
                    .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    private static final Intent INSECURE_CAMERA_INTENT =
            new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
	private static final String TAG = "DroiKeyguardManager";
    private static final boolean DEBUG = true;

    private static final String LOCKSCREEN_DEFAULT = "android";
    private static final String LOCKSCREEN_DEFAULT_ALT = "com.android.keyguard";
    private static final String LOCKSCREEN_CUSTOM_ID = "lockscreen_custom_layout";
    private static final String LOCKSCREEN_CUSTOM_BG = "lockscreen_custom_bg";
    
    
    private Context mContext;
    private String mLockScreenPackageName = null;
    private LockscreenPackageInfo mLockscreenPackageInfo;
    private LockPatternUtils mLockPatternUtils;
    private WallpaperCallback mWallpaperCallback = null;
    private Drawable mWallpaperDrawable = null;
    private boolean isDefaultLockscreen;
    private WallpaperManager mWm;
    
    public DroiKeyguardManager(Context context){
    	this.mContext = context;
    	mWm = WallpaperManager.getInstance(context);
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(com.freeme.content.FreemeIntent.ACTION_LOCKSCREEN_WALLPAPER_CHANGED);
    	mContext.registerReceiver(mBroadcastReceiver, filter);
    	mLockPatternUtils = new LockPatternUtils(context);
    }
    
    public LockPatternUtils getLockPatternUtils(){
    	return mLockPatternUtils;
    }
    
    public boolean isCameraIntent(Intent intent){
    	if(intent == null){
    		return false;
    	}
    	String action = intent.getAction();
    	if(action!= null){
    		return action.contains(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE)
			||action.contains(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
    	}else{
    		return false;
    	}
    }

    public static final String ACTION_SEND_MESSAGE_TO_LAUNCHER = "com.freeme.os.home.lockscreen_to_launcher";

    //*/ freeme.shanjibing, 20161222. modify for xiaoying
    public boolean isXiaoYing(Intent intent){
        if(intent == null){
            return false;
        }
        boolean moveToOSPage = intent.getBooleanExtra("move_to_os_page", false);
        return moveToOSPage;
    }
    //*/
    
    public void startCamera(ActivityStarter activityStarter){
    	Intent intent = getCameraIntent();
        boolean wouldLaunchResolverActivity = PreviewInflater.wouldLaunchResolverActivity(
                mContext, intent, ActivityManager.getCurrentUser());
        if (intent == SECURE_CAMERA_INTENT && !wouldLaunchResolverActivity) {
            if (DEBUG) Log.d(TAG, "launchCamera() - launch SECURE_CAMERA_INTENT") ;
            ///M: [ALPS01844520] Insecure Camera may be resumed if no CLEAR_TOP attribute.
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        } else {
            if (DEBUG) Log.d(TAG, "launchCamera() - launch INSECURE_CAMERA_INTENT") ;
            // We need to delay starting the activity because ResolverActivity finishes itself if
            // launched behind lockscreen.
            activityStarter.startActivity(intent, false /* dismissShade */);
        }
    }
    
    private Intent getCameraIntent() {
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(mContext);
        boolean currentUserHasTrust = updateMonitor.getUserHasTrust(
                ActivityManager.getCurrentUser());
        return mLockPatternUtils.isSecure(ActivityManager.getCurrentUser()) && !currentUserHasTrust
                ? SECURE_CAMERA_INTENT : INSECURE_CAMERA_INTENT;
    }
    
    public interface WallpaperCallback{
    	public void onWallpaperChanged(Drawable drawable);
    }
    
    public void setWallpaperCallback(WallpaperCallback cb) {
    	this.mWallpaperCallback = cb;
	}
			
    public boolean isDefaultLockScreen() {
        String result = getLockscreenPackage();
        return LOCKSCREEN_DEFAULT.equals(result);
    }
    
    public boolean resetMagicTrackMode(Context context){
        int systemMagicTrackMode = Settings.System.getInt(context.getContentResolver(), Settings.System.FREEME_MAGIC_TRACK_MODE,0);
        int backupMagicTrackMode = Settings.System.getInt(context.getContentResolver(), "tyd_magic_track_mode_back_up",0);
        
        if(backupMagicTrackMode != systemMagicTrackMode){
            Settings.System.putInt(context.getContentResolver(), Settings.System.FREEME_MAGIC_TRACK_MODE,backupMagicTrackMode);
            return true;
        }
        
        return false;
    }
    	
    public String getLockscreenPackage() {
       String result = Settings.System.getString(mContext.getContentResolver(), Settings.System.FREEME_LOCKSCREEN_PACKAGE);
        if(DEBUG){
        	Log.d(TAG, "getLockscreenPackage: " + result );	
        }
        return android.text.TextUtils.isEmpty(result) ? LOCKSCREEN_DEFAULT : result; 
       /* return "com.freeme.lockscreen.iphone";*/
    }
    
    public void resetDefaultLockscreen() {
        Settings.System.putString(mContext.getContentResolver(), Settings.System.FREEME_LOCKSCREEN_PACKAGE, LOCKSCREEN_DEFAULT);
    }
	
    public boolean isLockscreenChanged(){
    	if(mLockscreenPackageInfo == null){
    		return true;
    	}
    	String lastPackageString =  mLockscreenPackageInfo.packageName;
    	return !lastPackageString.equals(getLockscreenPackage());
    }
	
    public LockscreenPackageInfo getLockscreenPackageInfo() {
    	
        mLockScreenPackageName = getLockscreenPackage();
        Log.d(TAG, "getLockscreenPackageInfo. mLockScreenPackageName: " + mLockScreenPackageName );
        if (mLockscreenPackageInfo != null && mLockscreenPackageInfo.packageName.equals(mLockScreenPackageName)){
        	return mLockscreenPackageInfo;
        }
        
        int layoutId = 0;
        int viewId = 0;

//        if (android.text.TextUtils.isEmpty(mLockScreenPackageName) 
//                || LOCKSCREEN_DEFAULT.equals(mLockScreenPackageName)
//                || LOCKSCREEN_DEFAULT_ALT.equals(mLockScreenPackageName))
//            return new LockscreenPackageInfo(mContext, LOCKSCREEN_DEFAULT_ALT, layoutId, viewId);
        Context themeContext = null;
        try {
            themeContext = mContext.createPackageContext(
            		mLockScreenPackageName,
            Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
    
            layoutId = themeContext.getResources().getIdentifier(LOCKSCREEN_CUSTOM_ID, "layout", mLockScreenPackageName);
            viewId = themeContext.getResources().getIdentifier(LOCKSCREEN_CUSTOM_ID, "id", mLockScreenPackageName);
        
            mLockscreenPackageInfo = new LockscreenPackageInfo(themeContext, mLockScreenPackageName, layoutId, viewId);
            
            int tempId = themeContext.getResources().getIdentifier("config_show_status_view", "bool", mLockScreenPackageName);
            if (tempId !=0 )
                mLockscreenPackageInfo.configShowStatusView = themeContext.getResources().getBoolean(tempId);
            
            tempId = themeContext.getResources().getIdentifier("config_show_weather_view", "bool", mLockScreenPackageName);
            if (tempId !=0 )
                mLockscreenPackageInfo.configShowWeatherView = themeContext.getResources().getBoolean(tempId);
            
            tempId = themeContext.getResources().getIdentifier("config_show_eca_view", "bool", mLockScreenPackageName);
            if (tempId !=0 )
                mLockscreenPackageInfo.configShowEcaView = themeContext.getResources().getBoolean(tempId);
            
            tempId = themeContext.getResources().getIdentifier("config_keyguard_background_transparent", "bool", mLockScreenPackageName);
            if (tempId !=0 ){
            	mLockscreenPackageInfo.configKeyguardBackgroundTransparent = themeContext.getResources().getBoolean(tempId);
            }
            
            tempId = themeContext.getResources().getIdentifier("config_keyguard_show_status_info", "bool", mLockScreenPackageName);
            if (tempId !=0 ){
            	mLockscreenPackageInfo.configShowKeyguardStatusInfo = themeContext.getResources().getBoolean(tempId);
            }
            
            tempId = themeContext.getResources().getIdentifier("config_use_window_in_keyguard", "bool", mLockScreenPackageName);
            if (tempId !=0 ){
            	mLockscreenPackageInfo.configUseWindowInKeyguard = themeContext.getResources().getBoolean(tempId);
            }
            
            if(mLockscreenPackageInfo.configShowKeyguardStatusInfo){
            	 tempId = themeContext.getResources().getIdentifier("config_show_date", "bool", mLockScreenPackageName);
                 if (tempId !=0 )
                     mLockscreenPackageInfo.configShowDate = themeContext.getResources().getBoolean(tempId);
                 
                 tempId = themeContext.getResources().getIdentifier("config_show_alarm", "bool", mLockScreenPackageName);
                 if (tempId !=0 )
                     mLockscreenPackageInfo.configShowAlarm = themeContext.getResources().getBoolean(tempId);
                 
                 tempId = themeContext.getResources().getIdentifier("config_show_clock", "bool", mLockScreenPackageName);
                 if (tempId !=0 )
                     mLockscreenPackageInfo.configShowClock = themeContext.getResources().getBoolean(tempId);
                 
                 tempId = themeContext.getResources().getIdentifier("config_show_owner_info", "bool", mLockScreenPackageName);
                 if (tempId !=0 )
                     mLockscreenPackageInfo.configShowOwnerInfo = themeContext.getResources().getBoolean(tempId);
            }
            
            tempId = themeContext.getResources().getIdentifier("config_show_notification_in_keyguard", "bool", mLockScreenPackageName);
            if (tempId !=0 ){
            	mLockscreenPackageInfo.configShowNotificationInKeyguard = themeContext.getResources().getBoolean(tempId);
            }
            
            if(DEBUG){
            	 Log.d(TAG, "layoutId:  " + layoutId );
            	 Log.d(TAG, "configShowStatusView:  " + mLockscreenPackageInfo.configShowStatusView );
            	 Log.d(TAG, "configShowWeatherView:  " + mLockscreenPackageInfo.configShowWeatherView );
            	 Log.d(TAG, "configShowEcaView:  " + mLockscreenPackageInfo.configShowEcaView );
            	 Log.d(TAG, "configKeyguardBackgroundTransparent:  " + mLockscreenPackageInfo.configKeyguardBackgroundTransparent );
            	 Log.d(TAG, "configShowStatusInfo:  " + mLockscreenPackageInfo.configShowKeyguardStatusInfo );
            	 Log.d(TAG, "configShowDate:  " + mLockscreenPackageInfo.configShowDate );
            	 Log.d(TAG, "configShowAlarm:  " + mLockscreenPackageInfo.configShowAlarm );
            	 Log.d(TAG, "configShowClock:  " + mLockscreenPackageInfo.configShowClock );
            	 Log.d(TAG, "configShowOwnerInfo:  " + mLockscreenPackageInfo.configShowOwnerInfo );
            	 Log.d(TAG, "configShowNotificationInKeyguard:  " + mLockscreenPackageInfo.configShowNotificationInKeyguard );
            	 Log.d(TAG, "configUseWindowInKeyguard:  " + mLockscreenPackageInfo.configUseWindowInKeyguard );
            }
        } catch (Exception e) {
//        	layoutId = 0;
//        	viewId = 0;
            resetDefaultLockscreen();
            if(DEBUG){
          	  	Log.d(TAG, "getLockscreenPackageInfo error ");
            	e.printStackTrace();
          }
            return new LockscreenPackageInfo(mContext, LOCKSCREEN_DEFAULT_ALT, layoutId, viewId);
        }
    
        return mLockscreenPackageInfo;
    }
	
	
    protected class LockscreenPackageInfo {
        Context context;
        String packageName;
        int layoutId;
        int viewId;
        boolean configShowStatusView;
        boolean configShowWeatherView;
        boolean configShowEcaView;
        boolean configKeyguardBackgroundTransparent;
        boolean configShowKeyguardStatusInfo = true;
        boolean configShowDate  = true;
        boolean configShowAlarm  = true;
        boolean configShowClock  = true;
        boolean configShowOwnerInfo  = true;
        boolean configShowNotificationInKeyguard  = true;
        boolean configUseWindowInKeyguard  = false;

        LockscreenPackageInfo(Context _context, String _packageName, int _layoutId, int _viewId) {
            context = _context;
            packageName = _packageName;
            layoutId = _layoutId;
            viewId = _viewId;
        }
        
        View inflate(ViewGroup root, boolean attachToRoot) {
            if(DEBUG){
          	  	Log.d(TAG, "inflate keyguard,layoutId: " + layoutId + ",root:" + root);
          }
            return ((LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, root, attachToRoot);
        }
    }
    
    //*/ added by freeme Jack 20120710 for: lock screen wallpaper feature
    private static final File LOCKSCREEN_WALLPAPER_FILE = new File("/data/system/users/all/lockwallpaper");
    public Drawable loadLockscreenWallpaper() {

        Drawable dr = null;

        ParcelFileDescriptor fd = null;
        try {
            fd = ParcelFileDescriptor.open(LOCKSCREEN_WALLPAPER_FILE,
                ParcelFileDescriptor.MODE_READ_ONLY);
                
            //<--
            BitmapFactory.Options options = new BitmapFactory.Options();
            // Enable PQ support for all static lockscreen wallpaper bitmap decoding
            options.inPostProc = true;
            options.inPostProcFlag = 1;
            //-->
            
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options);
            dr = new BitmapDrawable(mContext.getResources(), bitmap);

        } catch (Exception e) {
            Log.i(TAG, "Load lock screen wallpaper Exception");
            
            dr = mContext.getResources().getDrawable(com.android.internal.R.drawable.default_wallpaper_lockscreen);
            
        } finally {
            if (null != fd) {
                try {
                    fd.close();
                } catch (Exception e) {
                    Log.i(TAG, "fd close IOException");
                }
            }
        }
        
        return dr;
    }
    //*/
    
  //*/ added by Droi shijiachen 20150731 for: lock screen wallpaper feature
    
    public boolean isTransparentWallpaper(){
    	boolean ret = false;
    	if(mLockscreenPackageInfo != null){
    		if (mLockscreenPackageInfo.configKeyguardBackgroundTransparent&&mLockscreenPackageInfo.layoutId != 0 ) {
    			ret = true;
    		}
    	}
    	return ret;
    }
    
    public Drawable requestWallpaper(){
    	Drawable dr = null;
    	mLockscreenPackageInfo = getLockscreenPackageInfo();
        if (!mLockscreenPackageInfo.configKeyguardBackgroundTransparent||mLockscreenPackageInfo.layoutId == 0 ) {
        	if(mWallpaperDrawable == null){
        		dr = mWallpaperDrawable = mWm.getLockDrawable();
        	}else{
        		dr = mWallpaperDrawable;
        	}
        }else{
        	dr = mWallpaperDrawable = null;
        }
        return dr;
    }

    private void onLockscreenWallpaperChanged() {
    	mWallpaperDrawable = mWm.getLockDrawable();
        if(mWallpaperCallback != null){
        	mWallpaperCallback.onWallpaperChanged(mWallpaperDrawable);
        }
    }
    
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent intent) {
			String action = intent.getAction();
			 if (com.freeme.content.FreemeIntent.ACTION_LOCKSCREEN_WALLPAPER_CHANGED.equals(action)) {
				 	onLockscreenWallpaperChanged();
	          }
		}
    };
    
}
