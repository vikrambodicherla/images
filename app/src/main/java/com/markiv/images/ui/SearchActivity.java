package com.markiv.images.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.markiv.images.R;
import com.markiv.images.data.GISSession;
import com.markiv.images.ui.history.SearchHistoryManager;

public class SearchActivity extends ActionBarActivity {
    private ViewFlipperManager mViewSwitcherManager;

    private GISSession mGISSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mViewSwitcherManager = new ViewFlipperManager();

        if(!handleIfSearchIntent(getIntent())) {
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(!handleIfSearchIntent(intent)){
            finish();
        }
    }

    private boolean handleIfSearchIntent(Intent intent){
        if(intent != null && Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            if(TextUtils.isEmpty(query)){
                return false;
            }

            //Setup the UI
            final ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle(query);
            actionBar.setDisplayHomeAsUpEnabled(true);

            //Search
            search(query);
            return true;
        }
        else {
            return false;
        }
    }

    private void search(String query){
        new SearchHistoryManager(this).recordSearch(query);
        if(mGISSession == null || !query.equals(mGISSession.getQuery())){
            if(mGISSession != null){
                mGISSession.persist();
                mGISSession.kill();
            }

            //TODO Optimize, make smaller pages on a smaller device - less memory or smaller screen size
            mGISSession = GISSession.newSession(query, 8);
            mViewSwitcherManager.setGridAdapter(new GImageSearchAdapter(this, mGISSession, mViewSwitcherManager));
        }

        //Preemptively set this with a delay, let the adapter unset it
        //TODO Hacky
        mViewSwitcherManager.showMessageDelayed(String.format(getResources().getString(R.string.no_search_results), query), 1000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_search, menu);

        final MenuItem searchItem = menu.findItem(R.id.ic_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(((SearchManager) getSystemService(Context.SEARCH_SERVICE)).getSearchableInfo(getComponentName()));

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    class ViewFlipperManager {
        private GridView mScrollView;
        private TextView mMessagesTextView;

        private ViewFlipper mViewFlipper;

        ViewFlipperManager() {
            mViewFlipper = (ViewFlipper) findViewById(R.id.search_switcher);

            mScrollView = (GridView) findViewById(R.id.search_grid);
            mMessagesTextView = (TextView) findViewById(R.id.search_message);
        }

        public void setGridAdapter(ListAdapter adapter){
            mScrollView.setAdapter(adapter);
        }

        public void showGrid(){
            mViewFlipper.setDisplayedChild(1);
        }

        public void showMessage(String message){
            mViewFlipper.setDisplayedChild(0);
            mMessagesTextView.setText(message);
        }

        public void showMessage(int stringResId){
            showMessage(getResources().getString(stringResId));
        }

        public void showMessageDelayed(final String message, long delay){
            mViewFlipper.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showMessage(message);
                }
            }, delay);
        }
    }
}