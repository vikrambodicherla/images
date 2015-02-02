package com.markiv.images.ui.history;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.markiv.images.R;

/**
 * @author vikrambd
 * @since 2/1/15
 */
public class AutoCompleteSearchHistoryAdapter extends BaseAdapter implements Filterable {
    private final Context mContext;
    private final Set<String> mSearchHistory;
    private final List<String> qualifiedQueriesFromHistory = new ArrayList<>();

    public AutoCompleteSearchHistoryAdapter(Context context) {
        mContext = context;
        mSearchHistory = new SearchHistoryManager(context).getSearchHistory();
    }

    @Override
    public int getCount() {
        return qualifiedQueriesFromHistory.size();
    }

    @Override
    public Object getItem(int position) {
        return qualifiedQueriesFromHistory.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView searchResultsView;
        if(convertView == null){
            searchResultsView = (TextView) LayoutInflater.from(mContext).inflate(R.layout.actionbar_search_history_item, parent, false);
        }
        else {
            searchResultsView = (TextView) convertView;
        }

        searchResultsView.setText((String)getItem(position));
        return searchResultsView;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                Iterator<String> seachHistoryIterator = mSearchHistory.iterator();
                List<String> stringList = new ArrayList<String>();
                while (seachHistoryIterator.hasNext()){
                    final String query = seachHistoryIterator.next();
                    if(query.contains(constraint)){
                        stringList.add(query);
                    }
                }

                FilterResults results = new FilterResults();
                results.count = stringList.size();
                results.values = stringList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                qualifiedQueriesFromHistory.clear();
                qualifiedQueriesFromHistory.addAll((List<String>) (results.values));
                notifyDataSetChanged();
            }
        };
    }
}
