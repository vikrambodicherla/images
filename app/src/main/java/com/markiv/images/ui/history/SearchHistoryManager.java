package com.markiv.images.ui.history;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

/**
 * @author vikrambd
 * @since 2/1/15
 */
public class SearchHistoryManager {
    private static final String KEY_SEARCH_TERM_SET = "searchTermsSet";
    private static final String SEPERATOR = "☢";
    private SharedPreferences mSharedPreferences;

    public SearchHistoryManager(Context context) {
        mSharedPreferences = context.getSharedPreferences("searchHistory", Context.MODE_PRIVATE);
    }

    public List<String> getSearchHistory(){
        final String searchHistoryList = mSharedPreferences.getString(KEY_SEARCH_TERM_SET, null);
        if(TextUtils.isEmpty(searchHistoryList)){
            return Collections.unmodifiableList(Collections.EMPTY_LIST);
        }
        else {
            return Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(searchHistoryList.split(SEPERATOR))));
        }
    }

    public void recordSearch(String query){
        if(TextUtils.isEmpty(query)){
            return;
        }

        if(query.contains(SEPERATOR)){
            throw new RuntimeException("The search query cannot have the seperator " + SEPERATOR);
        }

        String searchHistoryList = mSharedPreferences.getString(KEY_SEARCH_TERM_SET, null);
        if(TextUtils.isEmpty(searchHistoryList)){
            searchHistoryList = query;
        }
        else {
            final HashSet<String> searchHistoryAsMap = new HashSet<String>(Arrays.asList(searchHistoryList.split(SEPERATOR)));
            if(searchHistoryAsMap.contains(query)){
               return;
            }

            searchHistoryList = searchHistoryList + SEPERATOR + query;
        }

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
            mSharedPreferences.edit().putString(KEY_SEARCH_TERM_SET, searchHistoryList).apply();
        }
        else {
            mSharedPreferences.edit().putString(KEY_SEARCH_TERM_SET, searchHistoryList).commit();
        }
    }
}
