/*
*
*Add for SIM2 Ringtone by tyd xiaocui 2015-08-05
*/

package com.mediatek.audioprofile;

import android.app.Activity;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.widget.AccountViewAdapter;
import com.mediatek.widget.AccountViewAdapter.AccountElements;

import java.util.ArrayList;
import java.util.List;
import com.android.internal.telephony.PhoneConstants;
import android.provider.Settings;
import android.provider.Settings.System;
import com.android.settings.R;

/**
 * An list fragment to show active subscription.
 *
 */
public class SelectSimSettings extends ListFragment {

    private static final String TAG = "SelectSimSettings";
    private AccountViewAdapter mAdapter;
    private List<SubscriptionInfo> mSubInfoList;
    private List<Integer> mSlotIdList = new ArrayList<Integer>();
    private String  mRingtoneType;
    private String mKey;
    
    private int ringtone_summary[] ={R.string.zzzz_ringtone_sim1_sound_summary,R.string.zzzz_ringtone_sim2_sound_summary};
    private int message_summary[] ={R.string.zzzz_message_sim1_sound_summary,R.string.zzzz_message_sim2_sound_summary};		
 

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Customize title if intent with extra
        String title = getActivity().getIntent().getStringExtra(Intent.EXTRA_TITLE);
        mRingtoneType = getActivity().getIntent().getStringExtra("Ringtone");
        mKey = getActivity().getIntent().getStringExtra("Profile");
        Log.i("xiaocui33","SubSelectSettings mRingtoneType = "+mRingtoneType.toString() + "mKey =" + mKey.toString());
        if (!TextUtils.isEmpty(title)) {
            getActivity().setTitle(title);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSubInfoList = SubscriptionManager.from(getActivity()).getActiveSubscriptionInfoList();
        //Handle SIM Hot plug in/out case, when creating view possible there is no SIM available
        if (mSubInfoList != null && mSubInfoList.size() > 0) {
            setListAdapter(getAdapter(mSubInfoList));
        } else {
            getActivity().finish();
        }
    }

    private ListAdapter getAdapter(List<SubscriptionInfo> subInfoList) {
        if (mAdapter == null) {
            mAdapter = new AccountViewAdapter(getActivity(), getAccountsData(subInfoList));
        } else {
            mAdapter.updateData(getAccountsData(subInfoList));
        }
        return mAdapter;
    }

    private List<AccountElements> getAccountsData(
            List<SubscriptionInfo> subInfoList) {
        List<AccountElements> accounts = new ArrayList<AccountElements>();
		int i = 0;
		if(mRingtoneType.equals("voice")){
	        for (SubscriptionInfo record : subInfoList) {
	            //FIXME Later on mSimIconRes, will be replaced by one integer, not an array
	            accounts.add(new AccountElements(record.getIconTint(), record.getDisplayName().toString(),
	            		getActivity().getString(ringtone_summary[i++])));
	        }
		}else if(mRingtoneType.equals("message")){
	        for (SubscriptionInfo record : subInfoList) {
	            //FIXME Later on mSimIconRes, will be replaced by one integer, not an array
	            accounts.add(new AccountElements(record.getIconTint(), record.getDisplayName().toString(),
	            		getActivity().getString(message_summary[i++])));
	        }
		}

        return accounts;
    }
    

    private  void startActivityEditProfile(int id) {
        Intent ringtoneProfileIntent = new Intent(getActivity(), RingtoneProfile.class);
        Log.i("xiaocui33","DefaultPreference onClick mRingtoneType =" +mRingtoneType);
        ringtoneProfileIntent.putExtra("Profile", mKey);
        ringtoneProfileIntent.putExtra(PhoneConstants.SLOT_KEY, id);
        ringtoneProfileIntent.putExtra("Ringtone",mRingtoneType);
        getActivity().startActivity(ringtoneProfileIntent);
    }

    @Override
    public void onListItemClick(ListView listView, View v, int position, long id) {
        Intent intent = new Intent();
        SubscriptionInfo record = mSubInfoList.get(position);
        long subId = record.getSubscriptionId();
        int slotId = record.getSimSlotIndex();
        Log.d(TAG, "onListItemClick with slotId = " + slotId + " subId = " + subId);
        //intent.putExtra(PhoneConstants.SLOT_KEY, slotId);
       // intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);
        //RingtoneManager.setSimId(subId);
       // Settings.System.putLong(getActivity().getContentResolver(),"SIM_ID", subId );
        startActivityEditProfile(slotId);
        //getActivity().setResult(Activity.RESULT_OK, intent);
        //getActivity().finish();
    }
    
    
    
    
}
