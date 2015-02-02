package com.markiv.images.ui.history;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.markiv.images.R;

/**
 * @author vikrambd
 * @since 2/1/15
 */
public class SearchHistoryAdapter extends BaseAdapter {
    private final Context mContext;

    //TODO Lists and Sets. Search history needs to be a List everywhere! Sorry!
    private final List<String> mSearchHistory;

    public SearchHistoryAdapter(Context context, SearchHistoryManager searchHistoryManager) {
        mContext = context;
        mSearchHistory = new ArrayList<>(searchHistoryManager.getSearchHistory());
    }

    @Override
    public int getCount() {
        return mSearchHistory.size();
    }

    @Override
    public Object getItem(int position) {
        return mSearchHistory.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView searchHistoryView;
        if(convertView == null){
            searchHistoryView = (TextView) LayoutInflater.from(mContext).inflate(R.layout.search_history_item, parent, false);
        }
        else {
            searchHistoryView = (TextView) convertView;
        }

        searchHistoryView.setText((String)getItem(position));
        return searchHistoryView;
    }
}
