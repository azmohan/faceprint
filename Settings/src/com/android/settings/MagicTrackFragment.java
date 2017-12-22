package com.android.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.internal.launcher.particle.ParticleEnum;
import com.android.internal.launcher.particle.ParticleInfo;

//*/add by tyd zhanglingzeng for mini screen mode 20140913.
import android.widget.Toast;

import com.mediatek.HobbyDB.CustomHobbyService;
import com.mediatek.settings.FeatureOption;
//*/add end.

import android.os.SystemProperties;

public class MagicTrackFragment extends Fragment {
    //*/added by shijiachen 20151020 for magic track back up
    private static final String TYD_MAGIC_TRACK_MODE_BACK_UP = "tyd_magic_track_mode_back_up";
    //*/
    private GridView m_preview = null;
    private PreviewAdapter mPreviewAdapter = null;
    LayoutInflater inflater = null;
    private int mParticleId = ParticleEnum.PARTICLE_EFFECT_ID_NONE;

    public MagicTrackFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //*/add by tyd_wangalei 2015.9.21 for Customs hobby sort
        CustomHobbyService mService=new CustomHobbyService(getActivity());
        if(mService.isExistData(R.string.display_settings, R.string.pref_finger_particle_effects_title)){
			mService.update(R.string.display_settings, R.string.pref_finger_particle_effects_title);
		}else{
			mService.insert(R.string.display_settings, R.string.pref_finger_particle_effects_title, this.getClass().getName(), 1, "");
		}
        //*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mParticleId = Settings.System.getInt(getActivity().getContentResolver(), Settings.System.FREEME_MAGIC_TRACK_MODE,
                ParticleEnum.PARTICLE_EFFECT_ID_NONE);

        View content = inflater.inflate(R.layout.workspacetransition_effect_content, container, false);
        m_preview = (GridView) content.findViewById(R.id.transition_content);
        m_preview.setBackground(null);
        mPreviewAdapter = new PreviewAdapter(getActivity());
        m_preview.setAdapter(mPreviewAdapter);
        m_preview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (mParticleId != arg3) {
                    mParticleId = (int) arg3;
                    Settings.System.putInt(getActivity().getContentResolver(), Settings.System.FREEME_MAGIC_TRACK_MODE,
                            mParticleId);
                    //*/added by shijiachen 20151020 for magic track back up
                    Settings.System.putInt(getActivity().getContentResolver(), TYD_MAGIC_TRACK_MODE_BACK_UP,
                            mParticleId);
                    mPreviewAdapter.notifyDataSetChanged();
                    //*/
                }
            }
        });
        return content;
    }

    public class PreviewAdapter extends BaseAdapter {
        Context mContext;
        LayoutInflater mLayoutInflater;
        ParticleInfo[] mParticleInfos;

        public PreviewAdapter(Context c) {
            mContext = c;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            mParticleInfos = listParticleInfos();
        }

        public int getCount() {
            return mParticleInfos.length;
        }

        public Object getItem(int position) {
            return mParticleInfos[position];
        }

        public long getItemId(int position) {
            return mParticleInfos[position].id;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.workspacetransition_effect_item, null);
            }

            ParticleInfo info = mParticleInfos[position];

            ImageView preview = (ImageView) convertView.findViewById(R.id.preview);
            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            TextView title = (TextView) convertView.findViewById(R.id.title);

            preview.setImageResource(info.texture);
            title.setText(info.name);
            // highlight
            if (info.id == mParticleId) {
                icon.setVisibility(View.VISIBLE);
            } else {
                icon.setVisibility(View.INVISIBLE);
            }

            return convertView;
        }
    }

    public static ParticleInfo[] listParticleInfos() {
        final ParticleInfo[] particleItems = new ParticleInfo[] {
                new ParticleInfo(ParticleEnum.PARTICLE_EFFECT_ID_NONE, R.string.particle_effect_name_none,
                        R.drawable.particle_effect_icon_none),
                new ParticleInfo(ParticleEnum.PARTICLE_EFFECT_ID_RANDOM, R.string.particle_effect_name_random,
                        R.drawable.particle_effect_icon_random),
                /** custom effect add followed */
                new ParticleInfo(ParticleEnum.PARTICLE_EFFECT_ID_HEART, R.string.particle_effect_name_heart,
                        R.drawable.particle_effect_icon_heart),
                new ParticleInfo(ParticleEnum.PARTICLE_EFFECT_ID_STARS, R.string.particle_effect_name_stars,
                        R.drawable.particle_effect_icon_stars),
                new ParticleInfo(ParticleEnum.PARTICLE_EFFECT_ID_HEARTBEAT, R.string.particle_effect_name_heartbeat,
                        R.drawable.particle_effect_icon_heartbeat),
                new ParticleInfo(ParticleEnum.PARTICLE_EFFECT_ID_HALO, R.string.particle_effect_name_halo,
                        R.drawable.particle_effect_icon_halo), };

        return particleItems;
    }
}
