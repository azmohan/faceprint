package com.droi.systemui.qs.order.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.android.systemui.R;
import com.droi.systemui.qs.order.entry.QSTileItem;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.ImageView;

public class DragAdapter extends BaseAdapter {

    private final static String TAG = "DragAdapter";

    private Context mContext = null;

    private boolean showDraggedItem = false;

    private HashMap<String, Integer> mOrderRecord = new HashMap<String, Integer>();
    
    public List<QSTileItem> mTileList = null;

    private int holdPosition = -1;
    private int sortItemPos = 0;

    private TextView item_text = null;
    private ImageView item_icon = null;

    private ArrayList<OrderChangeListener> mListeners = new ArrayList<OrderChangeListener>();

    public interface OrderChangeListener {
        void onOrderChanged();
    }

    public void addOrderChangedListener(OrderChangeListener listener) {
        mListeners.add(listener);
    } 

    public DragAdapter(Context context, List<QSTileItem> tileList) {
        this.mContext = context;
        this.mTileList = tileList;
        
        sortItemPos = context.getResources().getInteger(R.integer.config_qs_sort_item_position);
        for(QSTileItem item : mTileList) {
            Log.d(TAG, "-->" + item);
        }
    }

    @Override
    public int getCount() {
        return mTileList == null ? 0 : mTileList.size();
    }

    @Override
    public QSTileItem getItem(int position) {
        if (mTileList != null && mTileList.size() > 0) {
            return mTileList.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.droi_qs_tile_order_item, null);
        item_text = (TextView) view.findViewById(R.id.text_item);
        item_icon = (ImageView) view.findViewById(R.id.icon_new);

        QSTileItem tile = getItem(position);
        String spec = tile.getTileSpec();
        String label = tile.getTileLabel();
        int iconId = tile.getIconId();
        
        item_text.setText(label);
        item_icon.setImageResource(iconId);

        /*/ freeme, gouzhouping. 20160817, for optimization order activity.
        if (19 == position){
		/*/
		if(18 == position){
		//*/
            view.setEnabled(false);
        }

        if (position == holdPosition && !showDraggedItem) {
            view.setVisibility(View.INVISIBLE);
        }

        return view;
    }

    public void exchange(int dragPostion, int dropPostion) {
        holdPosition = dropPostion;
        QSTileItem dragItem = getItem(dragPostion);
        Log.d(TAG, "startPostion=" + dragPostion + ";endPosition=" + dropPostion);
        if (dragPostion < dropPostion) {
            mTileList.add(dropPostion + 1, dragItem);
            mTileList.remove(dragPostion);
        } else {
            mTileList.add(dropPostion, dragItem);
            mTileList.remove(dragPostion + 1);
        }

        notifyDataSetChanged();

        for(OrderChangeListener listener : mListeners) {
            listener.onOrderChanged();
        }

        /*/
        for(QSTileItem tile : mTileList) {
            Log.d(TAG , tile.toString());
        }
        //*/
    }

    public void exchangeExceptMore(int holdPostion, int morePostion) {
        QSTileItem holdItem = getItem(holdPostion);
        if (holdPostion < morePostion) {
            mTileList.add(morePostion + 1, holdItem);
            mTileList.remove(holdPostion);
        } else {
            mTileList.add(morePostion, holdItem);
            mTileList.remove(holdPostion + 1);
        }

        //notifyDataSetChanged();

        /*/
        for(QSTileItem tile : mTileList) {
            Log.d(TAG , "after exchange:" + tile.toString());
        }
        //*/
    }

    /** This is called when save ordered QSTile into share_pref */
    public List<QSTileItem> getQSTileList() {
        return mTileList;
    }

    public void setListDate(List<QSTileItem> list) {
        mTileList = list;
        notifyDataSetChanged();
    }
    
    public void updateListDate(int index, QSTileItem newItem) {
        if(index < 0 || index >= mTileList.size()) {
            return;
        }
        
        mTileList.set(index, newItem);
        notifyDataSetChanged();
    }

    public void setShowDropItem(boolean show) {
        showDraggedItem = show;
    }

}
