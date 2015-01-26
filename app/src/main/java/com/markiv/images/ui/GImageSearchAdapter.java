package com.markiv.images.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.markiv.images.data.GImageSearchSession;
import com.markiv.images.data.model.GISearchResult;

/**
* @author vikrambd
* @since 1/25/15
*/
class GImageSearchAdapter extends BaseAdapter {
    private Context mContext;
    private GImageSearchSession mSearchSession;

    public GImageSearchAdapter(Context context, GImageSearchSession searchSession) {
        mContext = context;
        mSearchSession = searchSession;
    }

    @Override
    public int getCount() {
        return 100;
    }

    @Override
    public Object getItem(int position) {
        return mSearchSession.blockingGetSearchResult(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView == null) {
            textView = new TextView(mContext);
        }
        else {
            textView = (TextView) convertView;
        }
        GISearchResult result = (GISearchResult) getItem(position);
        textView.setText(result.getTitle());
        return textView;
    }


}
