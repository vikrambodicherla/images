package com.markiv.images.ui;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.ViewFlipper;

import com.markiv.images.R;
import com.markiv.images.ui.history.SearchHistoryAdapter;
import com.markiv.images.ui.history.SearchHistoryManager;

public class MainActivity extends ActionBarActivity {
    private ListView mHistoryListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHistoryListView = (ListView) findViewById(R.id.main_history);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SearchHistoryManager searchHistoryManager = new SearchHistoryManager(this);
        if(searchHistoryManager.getSearchHistory().size() > 0){
            ((ViewFlipper) findViewById(R.id.main_switcher)).setDisplayedChild(1);
            mHistoryListView.setAdapter(new SearchHistoryAdapter(this, searchHistoryManager));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);

        final MenuItem searchItem = menu.findItem(R.id.ic_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(((SearchManager) getSystemService(Context.SEARCH_SERVICE)).getSearchableInfo(new ComponentName(this, SearchActivity.class)));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                MenuItemCompat.collapseActionView(searchItem);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        return true;
    }
}
