package com.android.settings.accessibility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.HobbyDB.CustomHobbyService;
import com.mediatek.settings.FeatureOption;
import android.os.SystemProperties;
import com.freeme.internal.server.INativeMiscService;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import android.os.SystemProperties;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.search.BaseSearchIndexProvider;
import android.content.Context;
import java.util.List;
import java.util.ArrayList;
import android.content.res.Resources;
import android.provider.SearchIndexableResource;
import com.android.internal.widget.LockPatternUtils;

//*/Add by Jiangshouting 2016.01.04 for setting code transplant
import com.android.internal.logging.MetricsLogger;
//*/

public class SensorCalibrationFragment extends SettingsPreferenceFragment implements Indexable{
	
	private static final String TAG = "SensorCalibrationFragment";
	
	//*/add by tyd liuyong 20130826 for psensor calibration
    private static final String PSENSOR_CALIBRATION_DATA = "/sys/devices/platform/als_ps/ps_offset";
    private static final String KEY_PSENSOR = "psensor";
    private Preference mPsensorPreference;
    //*/
    
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		addPreferencesFromResource(R.xml.sensor_calibration);

        //*/ freeme.menglingqiang, 20160929. Psensor.
        mPsensorPreference =(Preference) findPreference(KEY_PSENSOR);
        if (!com.droi.feature.FeatureOption.FREEME_HW_SENSOR_PROXIMITY && mPsensorPreference != null){
            getPreferenceScreen().removePreference(mPsensorPreference);
        }
        //*/ 
        /*/ADD  by tyd wangalei 2015.9.22 for Customs hobby sort
        CustomHobbyService mService=new CustomHobbyService(getActivity());
        if(mService.isExistData(R.string.radio_controls_title, R.string.sensor_calibration_title)){
            mService.update(R.string.radio_controls_title, R.string.sensor_calibration_title);
        }else{
            mService.insert(R.string.radio_controls_title, R.string.sensor_calibration_title, this.getClass().getName(), 1, "");
        }
        //*/
        
	}
	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		//*/add by tyd liuyong 20130826 for psensor
        if(preference == mPsensorPreference){
            showCalibrationDialog();        	
        }
        //*/
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
	
	//*/add by tyd liuyong 20130816 for psensor
    private String readFile() {
    	
 	   StringBuilder infos = new StringBuilder();
 		
 		File file = new File(PSENSOR_CALIBRATION_DATA);
 		
 		InputStream inputStream = null;
 		BufferedReader buffer = null;
 		InputStreamReader in = null;
 		try {
 			inputStream = new FileInputStream(file);
 			
 			in = new InputStreamReader(inputStream, "GBK");
 			buffer = new BufferedReader(in);
 			
 			// read line by line.
 			String line = null;
 			while ((line = buffer.readLine()) != null) {
 				infos.append(line);
 			}
 			
 		} catch (FileNotFoundException e) {
 			Log.i(TAG, "readFile() FileNotFoundException" + e.getMessage());
 		} catch (UnsupportedEncodingException e) {
 			Log.i(TAG, "readFile() UnsupportedEncodingException" + e.getMessage());
 		} catch (IOException e) {
 			Log.i(TAG, "readFile() IOException" + e.getMessage());
 		} finally {
            try {
                if (buffer != null) {
                	buffer.close();
                }
                if (inputStream != null) {
                	inputStream.close();
                }
                if (in != null) {
                	in.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }		
 		return infos.toString();
 	}
    
 	private void showCalibrationDialog() {
 		new AlertDialog.Builder(getActivity())
        .setTitle(R.string.psensor_dlg_title)
        .setMessage(R.string.psensor_dlg_note)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	  String mcaldataString = readFile();
              	  Log.i(TAG, "readFile() is " + mcaldataString);	
      			     if( mcaldataString.equals("0")){
      			    	Toast.makeText(getActivity(),
                                getString(R.string.psensor_calibration_fail),
                                Toast.LENGTH_LONG).show(); 
      			     }else{
          		        SystemProperties.set("persist.sys.cal",mcaldataString);
                             //write calibration data to sensor
                             if(runCalibration(3) != -1){          		            			    	 
      			          Toast.makeText(getActivity(),
                                     getString(R.string.psensor_calibration_success),
                                     Toast.LENGTH_LONG).show();
                                 }
      			     }
                }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
	}

	private int runCalibration(int i) {
		android.os.IBinder binder = android.os.ServiceManager.getService("TydNativeMisc");
		INativeMiscService caliutil = INativeMiscService.Stub.asInterface(binder);
		int result = 0;
        try {
        	result = caliutil.runSensorCali(i);
        } catch (Exception e) {
            // ignore
        }
        return result;
	}
	//*/

    //*/Add by Tyd Jiangshouting 2015.11.19 for Setting search function lacking some data.
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
    new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
            final Resources res = context.getResources();

            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.sensor_calibration_title);
            data.screenTitle = res.getString(R.string.sensor_calibration_title);
            data.keywords = res.getString(R.string.sensor_calibration_title);
            result.add(data);
 
            return result;
        }
        
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {

            List<SearchIndexableResource> result = new ArrayList<SearchIndexableResource>();

            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);

            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.sensor_calibration;
            result.add(sir);

            return result;
        }
    };
    //*/
    
    //*/Added by Jiangshouting 2016.01.04 for setting code transplant
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.SENSORCALIBRATION;
    }
    //*/
}
