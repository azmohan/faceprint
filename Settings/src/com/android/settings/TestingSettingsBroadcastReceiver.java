package com.android.settings;

import static com.android.internal.telephony.TelephonyIntents.SECRET_CODE_ACTION;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;

//*/Add by Jiangshouting 2015.12.29 for setting code transplant
import android.content.ComponentName;
import android.net.Uri;
//import com.mediatek.common.featureoption.FeatureOption;
import android.os.SystemProperties;
import android.util.Log;

//*/


public class TestingSettingsBroadcastReceiver extends BroadcastReceiver {
    //*/Added by Jiangshouting 2015.12.29 for setting code transplant
    // process *#6801#*
    Uri softwareVersionUri = Uri.parse("android_secret_code://6801");
    // process *#6802#*
    Uri hardwareVersionUri = Uri.parse("android_secret_code://6802");
    // process *#6803#*
    Uri engineerUri = Uri.parse("android_secret_code://6803");
    // process *#6804#*
    Uri factorySettingsUri = Uri.parse("android_secret_code://6804");
    // process *#6805#*
    Uri automaticTestUri = Uri.parse("android_secret_code://6805");
    // process *#6809#*
    Uri restoreFactorySettingsUri = Uri.parse("android_secret_code://6809");
    // process *#6819#
    Uri imeiInfoUri = Uri.parse("android_secret_code://6819");  
    //merge from A97 by tyd lin 20130306 for koobee style2
    //*/ add for hardware info settings display  2012-03-28,ZhouLiwei.
    // process *#6812#*
    Uri hardwareInfoUri = Uri.parse("android_secret_code://6812");
    //*/

    //*/ add for software info settings display  2012-04-9,ZhouLiwei.
    // process *#6810#*
    Uri softwareInfoUri = Uri.parse("android_secret_code://6810");
    //*/
    //*/
    public TestingSettingsBroadcastReceiver() {
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        /*if (intent.getAction().equals(SECRET_CODE_ACTION)) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setClass(context, TestingSettings.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }*/
        try
        {
        Log.v("FactoryMode", "getIntent success before if");
        if (intent.getAction().equals(SECRET_CODE_ACTION)) {
            Uri uri = intent.getData();
            Log.v("FactoryMode", "getIntent success in if");
            if (uri.equals(engineerUri) /*|| uri.equals(factorySettingsUri) */|| uri.equals(automaticTestUri)) {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setComponent(new ComponentName("com.mediatek.engineermode", "com.mediatek.engineermode.EngineerMode"));
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            } else if (uri.equals(softwareVersionUri) || uri.equals(hardwareVersionUri)) {
                //*/Modify by tyd lin 20130124 for :[tyd00429744] *#680n# cmds are not working.*/
                Intent i = new Intent("android.settings.DEVICE_INFO_SETTINGS");
                //*/
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            } else if (uri.equals(restoreFactorySettingsUri)) {
                //*/Modify by tyd lin 20130124 for :[tyd00429744] *#680n# cmds are not working.*/
                Intent i = new Intent("android.settings.MASTER_CLEAR");
                //*/
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
              /*/
            else if (uri.equals(imeiInfoUri) && SystemProperties.get("ro.fo_imei_write_support").equals("1")) {
                Log.v("*#6819#", "------send-----in-----");   
                Intent i = new Intent(Intent.ACTION_MAIN);
              i.setComponent(new ComponentName("com.mediatek.engineermode", "com.mediatek.engineermode.IMEI"));
              i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              context.startActivity(i); 
            }
             //*/
            //merge from A97 by tyd lin 20130306 for koobee style2
            //*/ add for hardware info settings display  2012-03-28,ZhouLiwei.
            else if (uri.equals(hardwareInfoUri)) {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setComponent(new ComponentName("com.mediatek.factorymode", "com.mediatek.factorymode.hardwareInfo.HardwareInfoActivity"));
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i); 
            }
            //*/
           //*/ add for software info settings display  2012-04-09,ZhouLiwei.
            else if (uri.equals(softwareInfoUri)) {
                Log.v("*#6810#", "------send-----in-----");
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setComponent(new ComponentName("com.mediatek.factorymode", "com.mediatek.factorymode.softwareInfo.SoftwareInfoActivity"));
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i); 
            }
            //*/
          //end merge*/
            //*/ Kelven add the FactoryMode 20120704
            else if (uri.equals(factorySettingsUri)) {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setComponent(new ComponentName("com.mediatek.factorymode", "com.mediatek.factorymode.FactoryMode"));
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i); 
            }
            //*/
        }
      }
      catch(Exception e) {
      Log.e("FactoryMode", "Package exception.");
      }
    }
}
