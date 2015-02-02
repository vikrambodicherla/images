package com.markiv.images.ui.history;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author vikrambd
 * @since 2/1/15
 */
public class SearchHistoryManager {
    private static final String KEY_SEARCH_TERM_SET = "searchTermsSet";
    private SharedPreferences mSharedPreferences;

    public SearchHistoryManager(Context context) {
        mSharedPreferences = context.getSharedPreferences("searchHistory", Context.MODE_PRIVATE);
    }

    public Set<String> getSearchHistory(){
        Set<String> searchSet = mSharedPreferences.getStringSet(KEY_SEARCH_TERM_SET, null);
        return searchSet != null ? Collections.unmodifiableSet(searchSet) : Collections.EMPTY_SET;
    }

    public void recordSearch(String query){
        //TODO We are losing search history order!
        Set<String> searchSet = mSharedPreferences.getStringSet(KEY_SEARCH_TERM_SET, null);
        if(searchSet == null){
            searchSet = new HashSet<>();
        }
        searchSet.add(query);
        mSharedPreferences.edit().putStringSet(KEY_SEARCH_TERM_SET, searchSet).commit();
    }
}
