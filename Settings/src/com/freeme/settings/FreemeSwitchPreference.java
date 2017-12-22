package com.freeme.settings;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.util.Log;
/**
 * Created by luyangjie on 6/15/17.
 */

public class FreemeSwitchPreference extends SwitchPreference {


    public FreemeSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public FreemeSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FreemeSwitchPreference(Context context) {
        super(context);
    }

    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        Switch v = (Switch) view.findViewById(com.android.internal.R.id.switchWidget);
        if (v != null) {
            v.setClickable(true);
        }
        return view;
    }

    @Override
    protected void onClick() {
        //super.onClick();
    }


}
